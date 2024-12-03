package br.dev.leandro.spring.cloud.audit.model;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class IndexNameProvider {

    @Value("${bio.auditoria.prefixo:audit_logs_}")
    private String indexPrefix;

    @Value("${bio.auditoria.date-format:MM_yyyy}")
    private String dateFormat;

    public String getIndexName() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);
        if (indexPrefix == null || indexPrefix.isEmpty()) {
            throw new IllegalStateException("O prefixo do índice (bio.auditoria.prefixo) não está configurado.");
        }
        return indexPrefix + LocalDateTime.now().format(formatter);
    }
}
