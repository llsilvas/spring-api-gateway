package br.dev.leandro.spring.cloud.audit.dto;

import br.dev.leandro.spring.cloud.jwt.JwtToken;

public record AuditLogDTO(
        String correlationId,
        String endpoint,
        RequestDTO request,
        ResponseDTO response,
        JwtToken jwtToken
) {
}
