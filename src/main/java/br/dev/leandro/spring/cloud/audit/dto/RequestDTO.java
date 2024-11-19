package br.dev.leandro.spring.cloud.audit.dto;

import java.util.Map;

public record RequestDTO(
        String method,
        String clientIp,
        String userAgent,
        Map<String, String> requestHeaders,
        Map<String, String> requestParams,
        String requestBody
        ) {
}
