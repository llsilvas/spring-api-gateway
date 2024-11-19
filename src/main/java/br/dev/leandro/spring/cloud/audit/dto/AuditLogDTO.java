package br.dev.leandro.spring.cloud.audit.dto;

import java.util.Map;

public record AuditLogDTO(
        String correlationId,
        String criado,
        String method,
        String endpoint,
        String requestBody,
        String clientIp,
        String userAgent,
        Integer responseStatus,
        String responseBody,
        Map<String, String> requestHeaders,
        Map<String, String> responseHeaders,
        Map<String, String> requestParams,
        String tipo
) {
}
