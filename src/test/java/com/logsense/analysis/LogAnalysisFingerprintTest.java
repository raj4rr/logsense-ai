package com.logsense.analysis;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LogAnalysisFingerprintTest {
 @Test void normalizesVariableIdsAndNumbers() {
  String a=ErrorFingerprint.of("Payment order 123 failed trace 550e8400-e29b-41d4-a716-446655440000");
  String b=ErrorFingerprint.of("Payment order 999 failed trace 550e8400-e29b-41d4-a716-446655440000");
  assertEquals(a,b);
 }
}
