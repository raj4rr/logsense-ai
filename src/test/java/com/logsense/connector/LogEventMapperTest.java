package com.logsense.connector;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class LogEventMapperTest {
 @Test void parsesIsoTimestampAndSeverity() {
  var event = LogEventMapper.fromText("test","2026-11-24T09:00:00Z ERROR request failed traceId=abc",Instant.EPOCH,Map.of());
  assertEquals("ERROR",event.severity()); assertEquals(Instant.parse("2026-11-24T09:00:00Z"),event.timestamp()); assertEquals("abc",event.traceId());
 }
}
