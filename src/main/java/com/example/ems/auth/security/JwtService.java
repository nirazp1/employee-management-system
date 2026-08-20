package com.example.ems.auth.security;

import com.example.ems.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String CLAIM_AUTHORITIES = "authorities";
    private static final String CLAIM_TOKEN_TYPE = "type";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private final JwtConfig jwtConfig;

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UserPrincipal principal) {
        return buildToken(principal, jwtConfig.getExpirationMs(), TOKEN_TYPE_ACCESS);
    }

    public String generateRefreshToken(UserPrincipal principal) {
        return buildToken(principal, jwtConfig.getRefreshExpirationMs(), TOKEN_TYPE_REFRESH);
    }

    // Access and refresh tokens share this exact same builder, just with a different lifetime
    // and a "type" claim - I baked the type into the token itself (rather than, say, using two
    // different signing keys) so a single /refresh endpoint can reject an access token that
    // someone mistakenly (or deliberately) tries to use in place of a refresh token.
    private String buildToken(UserPrincipal principal, long expirationMs, String tokenType) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        // Baking the roles into the token itself (rather than looking them up fresh on every
        // request) is what makes this "stateless" - the filter never has to hit the DB just
        // to authorize a request, only to load full user details when it needs them.
        List<String> authorities = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return Jwts.builder()
                .subject(principal.getUsername())
                .claim(CLAIM_AUTHORITIES, authorities)
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .claim("uid", principal.getId().toString())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey())
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isRefreshToken(String token) {
        return TOKEN_TYPE_REFRESH.equals(parseClaims(token).get(CLAIM_TOKEN_TYPE, String.class));
    }

    public boolean isTokenValid(String token, String expectedUsername) {
        try {
            Claims claims = parseClaims(token);
            return claims.getSubject().equals(expectedUsername) && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getAccessTokenExpirationMs() {
        return jwtConfig.getExpirationMs();
    }
}
