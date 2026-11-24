package com.logsense.connector.remote;

import com.logsense.config.LogSenseProperties;
import com.logsense.connector.*;
import com.logsense.connector.file.LocalFileConnector;
import com.logsense.model.*;
import org.springframework.stereotype.Component;
import com.github.dockerjava.core.*;
import com.github.dockerjava.httpclient5.*;
import com.github.dockerjava.api.*;
import com.github.dockerjava.api.model.*;

import java.time.*;
import java.util.*;
import java.io.*;

@Component
public class DockerConnector implements LogConnector {
    private final LogSenseProperties p;

    public DockerConnector(LogSenseProperties p) {
        this.p = p;
    }

    public String type() {
        return "DOCKER";
    }

    public boolean supports(String t) {
        return "DOCKER".equalsIgnoreCase(t);
    }

    public List<LogEvent> search(String container, LogQuery q) {
        var c = p.sources().docker();
        if (c == null || !c.enabled()) throw new IllegalStateException("Set DOCKER_ENABLED=true");
        if (container == null || !container.matches("[A-Za-z0-9_.-]{1,128}"))
            throw new SecurityException("Invalid Docker container identifier");
        DockerClientConfig cfg = DefaultDockerClientConfig.createDefaultConfigBuilder().withDockerHost(c.host() == null || c.host().isBlank() ? "unix:///var/run/docker.sock" : c.host()).build();
        try (DockerClient client = DockerClientImpl.getInstance(cfg, new ApacheDockerHttpClient.Builder().dockerHost(cfg.getDockerHost()).build())) {
            var cmd = client.logContainerCmd(container).withStdOut(true).withStdErr(true).withTimestamps(true).withTail(q.safeLimit());
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            cmd.exec(new com.github.dockerjava.core.command.LogContainerResultCallback() {
                @Override
                public void onNext(Frame frame) {
                    try {
                        bytes.write(frame.getPayload());
                    } catch (IOException ignored) {
                    }
                }
            }).awaitCompletion();
            List<LogEvent> out = new ArrayList<>();
            for (String line : bytes.toString(java.nio.charset.StandardCharsets.UTF_8).split("\n")) {
                LogEvent e = LogEventMapper.fromText(container, line, Instant.now(), Map.of("containerId", container));
                if (LocalFileConnector.matches(e, q)) out.add(e);
            }
            return out;
        } catch (Exception e) {
            throw new IllegalStateException("Docker log retrieval failed", e);
        }
    }
}
