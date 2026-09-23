package io.github.hyungsoobae.dividendcalendar.fetch.massive;

import io.github.hyungsoobae.dividendcalendar.dividend.Dividend;
import io.github.hyungsoobae.dividendcalendar.fetch.DividendSource;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Massive 배당 API 구현체. 외부 호출과 정규화만 담당하고 데이터 판단은 하지 않는다.
 */
@Component
class MassiveDividendSource implements DividendSource {

    private static final Logger log = LoggerFactory.getLogger(MassiveDividendSource.class);
    private static final String DIVIDENDS_PATH = "/stocks/v1/dividends";

    private final RestClient restClient;
    private final RetryTemplate retryTemplate;
    private final MassiveProperties properties;
    private final String allowedHost;

    MassiveDividendSource(RestClient massiveRestClient,
                          RetryTemplate massiveRetryTemplate,
                          MassiveProperties properties) {
        this.restClient = massiveRestClient;
        this.retryTemplate = massiveRetryTemplate;
        this.properties = properties;
        this.allowedHost = URI.create(properties.baseUrl()).getHost();
    }

    @Override
    public List<Dividend> fetchByTicker(String ticker) {
        List<Dividend> collected = new ArrayList<>();
        URI uri = firstPageUri(ticker);
        int page = 0;

        while (uri != null) {
            page++;
            if (page > properties.maxPages()) {
                throw new MassiveApiException(
                        "페이지 수 상한을 넘었습니다: ticker=%s, maxPages=%d".formatted(ticker, properties.maxPages()));
            }
            MassiveDividendResponse response = request(uri, ticker);
            collected.addAll(toDividends(response, ticker));
            uri = nextPageUri(response.nextUrl(), ticker);
        }

        log.info("배당 수집 완료: ticker={}, 건수={}, 페이지={}", ticker, collected.size(), page);
        return collected;
    }

    private MassiveDividendResponse request(URI uri, String ticker) {
        MassiveDividendResponse response;
        try {
            response = retryTemplate.invoke(
                    () -> restClient.get().uri(uri).retrieve().body(MassiveDividendResponse.class));
        } catch (RestClientException e) {
            // 예외 메시지에 URL이 담길 수 있어 그대로 전파하지 않고 감싼다
            throw new MassiveApiException("배당 API 호출에 실패했습니다: ticker=" + ticker, e);
        }

        if (response == null || response.results() == null) {
            throw new MassiveApiException("배당 API 응답 본문이 비어 있습니다: ticker=" + ticker);
        }
        return response;
    }

    /**
     * 레코드 하나가 잘못됐다고 종목 전체 수집을 실패시키지 않는다.
     * 건너뛴 사실은 WARN으로 남겨 나중에 확인할 수 있게 한다.
     */
    private List<Dividend> toDividends(MassiveDividendResponse response, String ticker) {
        List<Dividend> dividends = new ArrayList<>();
        for (MassiveDividend record : response.results()) {
            try {
                dividends.add(MassiveDividendMapper.toDividend(record));
            } catch (IllegalArgumentException e) {
                log.warn("배당 레코드를 건너뜁니다: ticker={}, id={}, 사유={}", ticker, record.id(), e.getMessage());
            }
        }
        return dividends;
    }

    private URI firstPageUri(String ticker) {
        return UriComponentsBuilder.fromUriString(properties.baseUrl())
                .path(DIVIDENDS_PATH)
                .queryParam("ticker", ticker)
                .queryParam("limit", properties.pageLimit())
                .encode()
                .build()
                .toUri();
    }

    /**
     * next_url은 외부가 준 주소다. 여기로 Authorization 헤더가 전송되므로 호스트를 확인한다.
     * cursor 값이 이미 인코딩되어 있어 다시 인코딩하지 않는다.
     */
    private URI nextPageUri(String nextUrl, String ticker) {
        if (nextUrl == null || nextUrl.isBlank()) {
            return null;
        }

        URI uri = UriComponentsBuilder.fromUriString(nextUrl).build(true).toUri();
        if (!allowedHost.equalsIgnoreCase(uri.getHost())) {
            throw new MassiveApiException(
                    "허용되지 않은 next_url 호스트입니다: ticker=%s, host=%s".formatted(ticker, uri.getHost()));
        }
        return uri;
    }
}