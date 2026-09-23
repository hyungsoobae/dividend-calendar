package io.github.hyungsoobae.dividendcalendar.fetch.massive;

import java.net.http.HttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(MassiveProperties.class)
class MassiveClientConfig {

    /**
     * API 키를 Authorization 헤더로 보낸다.
     * 쿼리 파라미터로 보내면 I/O 예외 메시지에 요청 URL이 그대로 담겨 키가 로그에 남는다.
     */
    @Bean
    RestClient massiveRestClient(MassiveProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        return RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                .build();
    }

    /**
     * 일시적인 장애(연결 실패, 타임아웃, 5xx)만 재시도한다.
     * 4xx는 재시도해도 결과가 같고, 429(한도 초과)는 짧은 재시도로 풀리지 않아 제외한다.
     */
    @Bean
    RetryTemplate massiveRetryTemplate(MassiveProperties properties) {
        RetryPolicy policy = RetryPolicy.builder()
                .maxRetries(properties.maxRetries())
                .delay(properties.retryDelay())
                .includes(ResourceAccessException.class, HttpServerErrorException.class)
                .build();

        return new RetryTemplate(policy);
    }
}