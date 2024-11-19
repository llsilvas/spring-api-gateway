package br.dev.leandro.spring.cloud.jwt;

import java.util.Map;

public record JwtToken(
        String sub,
        Map<String, Object> resource_access,
        String iss,
        String typ,
        String preferred_username,
        String aud,
        String acr,
        Integer nbf,
        Map<String, Object> realm_access,
        String azp,
        Long exp,
        String session_state,
        String jti,
        String iat,
        String email,
        String scope,
        String sid,
        Map<String, Object> claims
) {}
