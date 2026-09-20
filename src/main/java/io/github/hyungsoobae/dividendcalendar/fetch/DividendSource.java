package io.github.hyungsoobae.dividendcalendar.fetch;

import io.github.hyungsoobae.dividendcalendar.dividend.Dividend;
import java.util.List;

/**
 * 배당 데이터 공급자. 구현체를 교체할 수 있도록 추상화한다.
 * (예: Massive, 추후 한국 주식용 DART)
 */
public interface DividendSource {

    /**
     * 해당 종목의 배당 이력과 선언된 미래 배당을 함께 반환한다.
     * 정렬 순서는 보장하지 않는다.
     */
    List<Dividend> fetchByTicker(String ticker);
}