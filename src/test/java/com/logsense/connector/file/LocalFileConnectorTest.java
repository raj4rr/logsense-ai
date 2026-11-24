package com.logsense.connector.file;

import com.logsense.config.LogSenseProperties;
import com.logsense.model.LogQuery;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class LocalFileConnectorTest {
 @Test void rejectsPathOutsideAllowedRoot() {
   var p = new LogSenseProperties("ollama", new LogSenseProperties.Security("x",false), new LogSenseProperties.Files(List.of("./sample-logs")), new LogSenseProperties.Limits(500,168));
   var connector = new LocalFileConnector(p);
   assertThrows(SecurityException.class, () -> connector.search("/etc/passwd", new LogQuery("/etc/passwd",null,null,null,null,null,null,10)));
 }
}
