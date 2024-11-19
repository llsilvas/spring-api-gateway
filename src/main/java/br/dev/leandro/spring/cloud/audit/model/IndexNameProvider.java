package br.dev.leandro.spring.cloud.audit.model;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component("indexNameProvider")
public class IndexNameProvider {

    private static final String INDEX_PREFIX = "audit_logs_";
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MM_yyyy");

    public static String getIndexName() {
        return INDEX_PREFIX + LocalDateTime.now().format(dateTimeFormatter);
    }
}
