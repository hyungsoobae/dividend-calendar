package io.github.hyungsoobae.dividendcalendar.fetch.massive;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Massive 배당 API의 레코드 하나. 외부 필드명을 그대로 쓴다.
 * 내부 도메인 용어로의 변환은 {@link MassiveDividendMapper}에서만 한다.
 *
 * @param id                         레코드 식별자. 데이터 수정 시 바뀔 수 있어 저장 키로 쓰지 않는다
 * @param splitAdjustedCashAmount    분할 조정 배당금. 조정 계수가 확정되기 전인 최신 건에는 없다
 * @param historicalAdjustmentFactor 조정 계수. 최신 건에는 없다. 현재 사용처는 없지만
 *                                   조정값 계산 근거를 추적할 수 있게 받아둔다
 */
public record MassiveDividend(
        String id,
        String ticker,
        @JsonProperty("ex_dividend_date") LocalDate exDividendDate,
        @JsonProperty("record_date") LocalDate recordDate,
        @JsonProperty("pay_date") LocalDate payDate,
        @JsonProperty("declaration_date") LocalDate declarationDate,
        @JsonProperty("cash_amount") BigDecimal cashAmount,
        @JsonProperty("split_adjusted_cash_amount") BigDecimal splitAdjustedCashAmount,
        @JsonProperty("historical_adjustment_factor") BigDecimal historicalAdjustmentFactor,
        String currency,
        Integer frequency,
        @JsonProperty("distribution_type") String distributionType
) {
}