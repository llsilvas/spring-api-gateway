package br.dev.leandro.spring.cloud.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    public JwtToken parseToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Token invalido");
        }

        String token = authorizationHeader.substring("Bearer ".length());

        try {
            DecodedJWT decodedJWT = JWT.decode(token);

            Map<String, Claim> allClaims = decodedJWT.getClaims();

            String sub = getStringClaim(allClaims, "sub");
            Map<String, Object> resourceAccess = getMapClaim(allClaims, "resource_access");
            String iss = getStringClaim(allClaims, "iss");
            String typ = getStringClaim(allClaims, "typ");
            String preferredUsername = getStringClaim(allClaims, "preferred_username");
            String aud = getStringClaim(allClaims, "aud");
            String acr = getStringClaim(allClaims, "acr");
            Integer nbf = getIntegerClaim(allClaims, "nbf");
            Map<String, Object> realmAccess = getMapClaim(allClaims, "realm_access");
            String azp = getStringClaim(allClaims, "azp");
            Long exp = getLongClaim(allClaims, "exp");
            String sessionState = getStringClaim(allClaims, "session_state");
            String jti = getStringClaim(allClaims, "jti");
            String iat = getStringClaim(allClaims, "iat");
            String scope = getStringClaim(allClaims, "scope");
            String sid = getStringClaim(allClaims, "sid");
            String email = getStringClaim(allClaims, "email");

            Map<String, Object> remainingClaims = new HashMap<>();
            for (Map.Entry<String, Claim> entry : allClaims.entrySet()) {
                String key = entry.getKey();
                if (!isFieldInJwtToken(key)) {
                    remainingClaims.put(key, entry.getValue().as(Object.class));
                }
            }

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
                    remainingClaims
            );
        }catch (Exception e) {
            return null;
        }
    }

    private static boolean isFieldInJwtToken(String key) {
        return switch (key) {
            case "sub", "resource_access", "iss", "typ", "preferred_username", "aud", "acr", "nbf", "realm_access",
                 "azp", "exp", "session_state", "jti", "iat", "sid", "scope", "email" -> true;
            default -> false;
        };
    }

    private static String getStringClaim(Map<String, Claim> claims, String key) {
        Claim claim = claims.get(key);
        return (claim != null) ? claim.asString() : null;
    }

    private static Map<String, Object> getMapClaim(Map<String, Claim> claims, String key) {
        Claim claim = claims.get(key);
        return (claim != null) ? claim.asMap() : null;
    }

    private static Integer getIntegerClaim(Map<String, Claim> claims, String key) {
        Claim claim = claims.get(key);
        return (claim != null) ? claim.asInt() : null;
    }

    private static Long getLongClaim(Map<String, Claim> claims, String key) {
        Claim claim = claims.get(key);
        return (claim != null) ? claim.asLong() : null;
    }
}
