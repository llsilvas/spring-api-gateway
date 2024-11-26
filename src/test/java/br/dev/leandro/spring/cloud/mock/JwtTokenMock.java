package br.dev.leandro.spring.cloud.mock;

import br.dev.leandro.spring.cloud.jwt.JwtToken;

import java.util.HashMap;
import java.util.Map;

public class JwtTokenMock {

    public static JwtToken getJwtToken() {
        // Valores de exemplo para cada campo
        String sub = "1234567890";

        Map<String, Object> resourceAccess = new HashMap<>();
        resourceAccess.put("resource1", "access1");
        resourceAccess.put("resource2", "access2");

        String iss = "https://issuer.example.com";
        String typ = "Bearer";
        String preferredUsername = "johndoe";
        String aud = "your-audience";
        String acr = "1";
        Integer nbf = 1633024800;

        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("role1", "admin");
        realmAccess.put("role2", "user");

        String azp = "your-azp";
        Long exp = 1633032000L;
        String sessionState = "state12345";
        String jti = "jti-12345";
        String iat = "1633024800";
        String email = "johndoe@example.com";
        String scope = "scope1";
        String sid = "sid-12345";

        Map<String, Object> claims = new HashMap<>();
        claims.put("customClaim1", "value1");
        claims.put("customClaim2", 2);

        // Criação da instância de JwtToken com os valores mockados
        return new JwtToken(
                sub,
                resourceAccess,
                iss,
                typ,
                preferredUsername,
                aud,
                acr,
                nbf,
                realmAccess,
                azp,
                exp,
                sessionState,
                jti,
                iat,
                email,
                scope,
                sid,
                claims
        );
    }
}
