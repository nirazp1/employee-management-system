package com.example.ems.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.jwt")
@Getter
@Setter
public class JwtConfig {

    private String secret;
    private long expirationMs;
    private long refreshExpirationMs;

    // Failing fast here on purpose: HS256/HS384 signing needs a key of a minimum length to be
    // secure, and a too-short secret would otherwise sit quietly until someone actually tries
    // to sign a token and gets a cryptic library exception. I'd rather the app refuse to start
    // at all than come up with a weak JWT secret in production.
    @PostConstruct
    void validate() {
        if (secret == null || secret.getBytes().length < 32) {
            throw new IllegalStateException("app.jwt.secret must be configured and at least 256 bits (32 bytes) long");
        }
    }
}
