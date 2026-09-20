package io.github.hyungsoobae.dividendcalendar.dividend;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 배당 한 건. 외부 소스와 무관한 내부 표현이다.
 *
 * @param ticker              티커 (예: AAPL)
 * @param exDate              배당락일. 이 날 이후 매수하면 이번 배당을 받지 못한다
 * @param recordDate          기준일. 주주명부에 등재되어 있어야 하는 날.
 *                            T+1 결제 이후로는 배당락일과 같은 것이 정상이다
 * @param payDate             지급일. 실제로 배당금이 입금되는 날
 * @param declarationDate     선언일. 회사가 배당을 공시한 날. 오래된 건은 없을 수 있다
 * @param amount              배당금. 그 당시 실제로 지급된 금액으로, 액면분할을 반영하지 않는다.
 *                            화면에 보여줄 금액은 이 값을 쓴다
 * @param splitAdjustedAmount 분할 조정 배당금. 과거 금액을 현재 주식 수 기준으로 환산한 값이라
 *                            이력끼리 비교할 때 쓴다. 조정 계수가 확정되기 전인 최신 건은 null이며,
 *                            이때는 이후 분할이 없었으므로 amount와 같다고 본다
 * @param currency            통화 (예: USD)
 * @param frequency           연간 지급 횟수. 0은 비정기(특별/보충/일회성), 1 연간, 2 반기,
 *                            4 분기, 12 월배당 등. 발행사 선언값이 아니라 추론값일 수 있어
 *                            enum으로 매핑하지 않고 정수 그대로 다룬다
 * @param distributionType    배당 유형 (예: recurring)
 */
public record Dividend(
        String ticker,
        LocalDate exDate,
        LocalDate recordDate,
        LocalDate payDate,
        LocalDate declarationDate,
        BigDecimal amount,
        BigDecimal splitAdjustedAmount,
        String currency,
        int frequency,
        String distributionType
) {
}