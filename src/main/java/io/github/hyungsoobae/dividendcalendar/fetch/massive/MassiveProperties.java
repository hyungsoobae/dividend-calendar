package io.github.hyungsoobae.dividendcalendar.fetch.massive;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Massive API 연동 설정. 값은 application.yaml의 massive.* 에서 주입된다.
 */
@ConfigurationProperties(prefix = "massive")
record MassiveProperties(
        String baseUrl,
        String apiKey,
        int pageLimit,
        int maxPages,
        Duration connectTimeout,
        Duration readTimeout,
        long maxRetries,
        Duration retryDelay
) {
}