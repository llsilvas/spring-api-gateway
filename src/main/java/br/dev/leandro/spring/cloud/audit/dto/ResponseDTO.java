package br.dev.leandro.spring.cloud.audit.dto;

import java.util.Map;

public record ResponseDTO(
        Integer responseStatus,
        Map<String, String> responseHeaders,
        String responseBody
) {
}
