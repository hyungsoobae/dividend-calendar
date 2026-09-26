package io.github.hyungsoobae.dividendcalendar.fetch;

import io.github.hyungsoobae.dividendcalendar.dividend.Dividend;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 수집 동작을 눈으로 확인하기 위한 임시 실행기.
 * 저장 단계에서 스케줄러로 대체한다. fetch-demo 프로파일에서만 동작한다.
 */
@Component
@Profile("fetch-demo")
class FetchDemoRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FetchDemoRunner.class);
    private static final List<String> TICKERS = List.of("AAPL", "O", "META");

    private final DividendSource dividendSource;

    FetchDemoRunner(DividendSource dividendSource) {
        this.dividendSource = dividendSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (String ticker : TICKERS) {
            List<Dividend> dividends = dividendSource.fetchByTicker(ticker);
            log.info("=== {} ({}건) ===", ticker, dividends.size());
            dividends.stream()
                    .sorted((a, b) -> b.exDate().compareTo(a.exDate()))
                    .limit(3)
                    .forEach(d -> log.info("{}", d));
        }
    }
}