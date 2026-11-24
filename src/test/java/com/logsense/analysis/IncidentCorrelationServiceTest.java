package com.logsense.analysis;
import com.logsense.model.LogEvent;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class IncidentCorrelationServiceTest {
 @Test void groupsCrossServiceEventsByTraceId() {
  var service = new IncidentCorrelationService(new LogRedactor());
  var events = java.util.List.of(
   new LogEvent("a","payment",null,"INFO",Instant.parse("2026-11-24T10:00:00Z"),"accepted",null,"trace-1",null,null,null,Map.of()),
   new LogEvent("b","ledger",null,"ERROR",Instant.parse("2026-11-24T10:00:02Z"),"database timeout",null,"trace-1",null,null,null,Map.of()));
  var result = service.correlate(events);
  assertEquals(1, ((java.util.List<?>) result.get("correlationGroups")).size());
 }
}
