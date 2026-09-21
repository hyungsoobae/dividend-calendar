package io.github.hyungsoobae.dividendcalendar.fetch.massive;

import io.github.hyungsoobae.dividendcalendar.dividend.Dividend;

/**
 * Massive 응답을 내부 도메인 모델로 변환한다.
 * 외부 필드명과 내부 용어의 매핑은 이 클래스에만 존재한다.
 */
final class MassiveDividendMapper {

    private MassiveDividendMapper() {
    }

    static Dividend toDividend(MassiveDividend source) {
        requireField(source.ticker() != null, "ticker", source);
        requireField(source.exDividendDate() != null, "ex_dividend_date", source);
        requireField(source.cashAmount() != null, "cash_amount", source);
        requireField(source.frequency() != null, "frequency", source);

        return new Dividend(
                source.ticker(),
                source.exDividendDate(),
                source.recordDate(),
                source.payDate(),
                source.declarationDate(),
                source.cashAmount(),
                source.splitAdjustedCashAmount(),
                source.currency(),
                source.frequency(),
                source.distributionType()
        );
    }

    private static void requireField(boolean present, String fieldName, MassiveDividend source) {
        if (!present) {
            throw new IllegalArgumentException(
                    "배당 응답에 필수 필드가 없습니다: field=%s, id=%s".formatted(fieldName, source.id()));
        }
    }
}