package com.example.herbalife_clubes.security;

import io.github.cdimascio.dotenv.Dotenv;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    /** Mínimo de bytes del secreto decodificado para HS512 (RFC 7518). */
    static final int HS512_MIN_KEY_BYTES = 64;

    private final Dotenv dotenv = Dotenv.configure()
            .ignoreIfMissing()
            .load();

    private Key key;

    @PostConstruct
    public void initKey() {
        applySecret(resolveSecretFromEnvironment());
    }

    /**
     * Resuelve JWT_SECRET desde variables de entorno o .env local.
     * Visible para tests de ausencia de secreto.
     */
    String resolveSecretFromEnvironment() {
        String secret = System.getenv("JWT_SECRET");

        if (secret == null || secret.isBlank()) {
            try {
                secret = dotenv.get("JWT_SECRET");
            } catch (Exception ignored) {
            }
        }

        return secret;
    }

    /**
     * Valida y aplica el secreto Base64. Falla en inicialización si es inválido o corto.
     * Package-visible para tests unitarios con bytes ficticios.
     */
    void applySecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                "JWT_SECRET no encontrada. "
                + "Configura la variable de entorno JWT_SECRET. "
                + "Genera una clave con: openssl rand -base64 64"
            );
        }

        final byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret.trim());
        } catch (RuntimeException ex) {
            throw new IllegalStateException(
                "JWT_SECRET debe ser Base64 válido.",
                ex
            );
        }

        if (keyBytes.length < HS512_MIN_KEY_BYTES) {
            throw new IllegalStateException(
                "JWT_SECRET debe contener al menos 64 bytes (512 bits) para HS512."
            );
        }

        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    private Key getSignInKey() {
        if (key == null) {
            initKey();
        }
        return key;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000))
                .signWith(getSignInKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}
