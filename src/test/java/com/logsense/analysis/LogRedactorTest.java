package com.logsense.analysis;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LogRedactorTest {
 @Test void removesCommonSecretsBeforeModelPrompting() {
  LogRedactor redactor = new LogRedactor();
  String safe = redactor.redact("password=hunter2 Authorization: Bearer abc.def.ghi api_key=secret-value");
  assertFalse(safe.contains("hunter2")); assertFalse(safe.contains("abc.def.ghi")); assertFalse(safe.contains("secret-value"));
  assertTrue(safe.contains("[REDACTED]"));
 }
}
