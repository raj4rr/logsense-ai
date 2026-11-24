package com.logsense.connector.remote;

import com.logsense.config.LogSenseProperties;
import com.logsense.connector.*;
import com.logsense.connector.file.LocalFileConnector;
import com.logsense.model.*;
import org.springframework.stereotype.Component;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.*;
import java.util.*;

@Component
public class KafkaLogConnector implements LogConnector {
    private final LogSenseProperties p;

    public KafkaLogConnector(LogSenseProperties p) {
        this.p = p;
    }

    public String type() {
        return "KAFKA";
    }

    public boolean supports(String t) {
        return "KAFKA".equalsIgnoreCase(t);
    }

    public List<LogEvent> search(String topic, LogQuery q) {
        var c = p.sources().kafka();
        if (c == null || c.bootstrapServers() == null || c.bootstrapServers().isBlank())
            throw new IllegalStateException("Set KAFKA_BOOTSTRAP_SERVERS");
        if (topic == null || !topic.matches("[A-Za-z0-9._-]{1,249}"))
            throw new SecurityException("Invalid Kafka topic name");
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, c.bootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, (c.groupId() == null ? "logsense-readonly" : c.groupId()) + "-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, q.safeLimit());
        if (c.securityProtocol() != null && !c.securityProtocol().isBlank())
            props.put("security.protocol", c.securityProtocol());
        if (c.saslMechanism() != null && !c.saslMechanism().isBlank()) props.put("sasl.mechanism", c.saslMechanism());
        if (c.saslJaasConfig() != null && !c.saslJaasConfig().isBlank())
            props.put("sasl.jaas.config", c.saslJaasConfig());
        List<LogEvent> out = new ArrayList<>();
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(topic));
            long deadline = System.currentTimeMillis() + 10000;
            while (consumer.assignment().isEmpty() && System.currentTimeMillis() < deadline)
                consumer.poll(Duration.ofMillis(200));
            Set<TopicPartition> partitions = consumer.assignment();
            if (partitions.isEmpty()) return List.of();
            Map<TopicPartition, Long> beginning = consumer.beginningOffsets(partitions), end = consumer.endOffsets(partitions);
            for (TopicPartition tp : partitions) {
                long first = beginning.get(tp), last = end.get(tp);
                consumer.seek(tp, Math.max(first, last - q.safeLimit()));
            }
            deadline = System.currentTimeMillis() + 3500;
            while (out.size() < q.safeLimit() && System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(250));
                if (records.isEmpty() && partitions.stream().allMatch(tp -> consumer.position(tp) >= end.get(tp)))
                    break;
                for (ConsumerRecord<String, String> r : records) {
                    if (r.offset() >= end.getOrDefault(new TopicPartition(r.topic(), r.partition()), 0L)) continue;
                    LogEvent e = LogEventMapper.fromText("kafka:" + topic, r.value(), r.timestamp() < 0 ? Instant.now() : Instant.ofEpochMilli(r.timestamp()), Map.of("partition", r.partition(), "offset", r.offset(), "key", String.valueOf(r.key())));
                    if (LocalFileConnector.matches(e, q)) out.add(e);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Kafka read failed; configure ACLs for read-only topic access", e);
        }
        return out;
    }
}
