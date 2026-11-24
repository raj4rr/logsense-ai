package com.logsense.connector;

import com.logsense.model.LogEvent;
import com.logsense.model.LogQuery;
import java.util.List;

public interface LogConnector {
    String type();
    boolean supports(String sourceType);
    List<LogEvent> search(String sourceName, LogQuery query);
}
