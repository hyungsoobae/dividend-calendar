# 배당 API 조사 노트

> 2026-09-14 갱신. 파싱 코드와 Rule Validator 작성 시 참고.

## Massive (구 Polygon.io) — 채택

### 엔드포인트

```
GET https://api.massive.com/stocks/v1/dividends
    ?ticker={ticker}
    &sort=ex_dividend_date.desc
    &limit={n}
    &apiKey={key}
```

- 무료 키로 종목 제한 없이 조회 가능 (FMP에서 막혔던 O 포함)
- 과거 이력과 **선언된 미래 배당**을 함께 반환
- API 키는 쿼리 파라미터. 환경변수로 관리하고 로그에 URL 전체를 찍지 않는다
- 필드별 필터 지원 (`ex_dividend_date.gte` 등)

### 응답 구조

FMP는 최상위가 배열이었지만 Massive는 객체로 감싸여 있다.

```json
{
  "status": "OK",
  "request_id": "...",
  "results": [ { ... } ]
}
```

### 필드

| 필드 | 타입(Java) | 의미 | 주의 |
|---|---|---|---|
| id | String | 레코드 식별자 | upsert 키로 쓰지 않음 (데이터 수정 시 변경 가능) |
| ticker | String | 티커 | |
| ex_dividend_date | LocalDate | 배당락일 | 필드명이 명확해 FMP의 `date`처럼 헷갈리지 않음 |
| record_date | LocalDate | 기준일 | |
| pay_date | LocalDate | 지급일 | |
| declaration_date | LocalDate (nullable) | 선언일 | |
| cash_amount | BigDecimal | 배당금 (분할 미조정) | |
| split_adjusted_cash_amount | BigDecimal (nullable) | 분할 조정 배당금 | **최신 건에는 없음** |
| historical_adjustment_factor | BigDecimal (nullable) | 조정 계수 | 최신 건에는 없음 |
| currency | String | 통화 | market/currency 컬럼 설계와 맞음 |
| frequency | int | 연간 지급 횟수 | 아래 참고 |
| distribution_type | String | recurring 등 | 다른 값은 확인 필요 |

### frequency

```
0   비정기 (특별/보충/일회성)
1   연간        2   반기        3   4개월마다
4   분기        12  월배당      24  격월
52  주간        104 격주        365 일간
```

- DB에는 **정수 그대로** 저장한다. enum으로 강제하면 새 값이 왔을 때 파싱이 깨진다
- 화면 라벨은 매핑 함수로 처리하고, 모르는 값은 "연 N회"로 폴백
- 발행사 선언값이 아니라 **추론값일 수 있다**. 이 값만으로 "배당이 빠졌다" 같은 판정을 하지 않는다
- `0`은 특별배당 판별에 쓸 수 있다. 금액 이상치로 추정하던 것보다 정확

### 주의사항

**1. 최신 레코드에 조정 필드가 없다**

```json
{ "ex_dividend_date": "2026-09-21", "cash_amount": 0.525 }
{ "ex_dividend_date": "2026-06-15", "cash_amount": 0.525,
  "split_adjusted_cash_amount": 0.525, "historical_adjustment_factor": 0.999074 }
```

조정 계수가 아직 확정되지 않아서다. 이력 비교는 다음으로 계산한다.

```
조정 배당금 = split_adjusted_cash_amount ?? cash_amount
```

최신 건은 이후 분할이 없었으므로 원본값과 같다. 나중에 조정값이 채워질 수 있으므로
**저장은 원본 두 필드 그대로, 계산은 조회 시점**에 한다.

**2. 수익률이 없다**

FMP의 `yield`에 해당하는 필드가 없다. 필요하면 주가를 별도 호출해 계산해야 한다.
AI 해설 입력 포함 여부는 M2에서 결정.

### 확인 필요

- [ ] 페이징 방식 (`limit` 상한 초과 시 `next_url` 등) — 최초 수집 시 필요
- [ ] `distribution_type`의 다른 값 (특별배당 종목으로 확인)
- [ ] 무료 플랜 호출 한도 (Polygon 시절 분당 5회, 개명 후 동일한지 미확인)
- [ ] 오래된 데이터의 `declaration_date`가 null인지 빈 문자열인지

---

## FMP — 미채택

무료 플랜은 배당 API의 종목 범위가 제한된다.

- 캘린더 API(`/stable/dividends-calendar`): 90일 조회에 11건, 고유 10종목. 전부 초대형주
- 종목별 이력 API(`/stable/dividends`): O 등에서 `Premium Query Parameter` 오류
- 관심 종목이 응답에 없을 수 있어 서비스에 부적합

아래는 채택 검토 당시의 분석 내용. Massive로 옮긴 뒤에도 액면분할·T+1 등
**데이터 자체의 특성**은 그대로 유효하므로 보존한다.



## 엔드포인트

```
GET https://financialmodelingprep.com/stable/dividends?symbol={ticker}&apikey={key}
```

- 무료 키로도 AAPL 전체 이력(1987년~)이 반환됨
- 응답은 최신순 정렬된 JSON 배열
- API 키는 쿼리 파라미터로 전달. 코드에서는 환경변수로 관리하고 로그에 URL 전체를 찍지 않는다

## 필드

| 필드 | 타입(Java) | 의미 | 주의 |
|---|---|---|---|
| symbol | String | 티커 | |
| date | LocalDate | 배당락일 (ex-dividend date) | 필드명이 `date`라 헷갈리기 쉬움. 우리 쪽에서는 `exDate`로 매핑 |
| recordDate | LocalDate | 배당 기준일 | |
| paymentDate | LocalDate | 지급일 | |
| declarationDate | LocalDate (nullable) | 배당 선언일 | 과거 데이터에서 빈 문자열 `""`로 오는 경우 있음 |
| dividend | BigDecimal | 당시 실제 배당금 (분할 미조정) | |
| adjDividend | BigDecimal | 액면분할 조정 배당금 | 이력 비교는 이 값으로 |
| yield | BigDecimal | 배당수익률 | 단위가 퍼센트 (0.34 = 0.34%) |
| frequency | String | Quarterly / Irregular 등 | enum 매핑 시 모르는 값이 와도 죽지 않게 처리 |

## 실제 응답에서 발견한 함정

**1. 액면분할 때문에 `dividend`가 크게 흔들린다**

```json
{ "date": "2020-08-07", "adjDividend": 0.205, "dividend": 0.82 }
{ "date": "2020-11-06", "adjDividend": 0.205, "dividend": 0.205 }
```

2020년 8월 4:1 분할 전후로 `dividend`가 0.82 → 0.205로 75% 급락한 것처럼 보이지만 정상이다. `adjDividend`는 동일하다. 2014년 7:1 분할에서도 같은 현상(3.29 → 0.47).
→ **이상치 비교는 반드시 `adjDividend` 기준.**

**2. 최근 데이터는 배당락일 = 기준일**

```json
{ "date": "2024-05-10", "recordDate": "2024-05-13" }
{ "date": "2024-08-12", "recordDate": "2024-08-12" }
```

미국 주식 결제가 2024년 5월 말부터 T+1로 바뀌면서 배당락일과 기준일이 같아졌다. "배당락일 < 기준일" 규칙을 만들면 최근 데이터가 전부 오류로 걸린다.
→ **규칙은 "배당락일 <= 기준일 <= 지급일"로.**

단, `2024-11-08 / 2024-11-11`처럼 T+1 이후인데 날짜가 다른 건도 있다. 데이터 오류인지 실제 사정이 있었는지 확인 필요 (검증 단계에서 다룰 좋은 예시).

**3. 빈 문자열 날짜**

```json
{ "date": "1995-02-13", "declarationDate": "" }
```

Jackson 기본 설정으로 `LocalDate`에 빈 문자열을 넣으면 파싱 에러. null로 변환하도록 설정 필요.

**4. 긴 공백 기간**

1995-11 다음 레코드가 2012-08. 애플이 실제로 배당을 중단했던 기간이라 정상이다. "분기 배당이 빠졌다" 같은 규칙은 이런 케이스를 오탐할 수 있다.

**5. 자잘한 데이터 노이즈**

- 1987-05-11 `dividend: 0.1201` (다른 건들은 0.12 단위)
- 1995-11-21만 `frequency: "Irregular"` (앞뒤는 전부 Quarterly)

치명적이지는 않지만, 외부 데이터가 완벽하지 않다는 증거.

## 샘플 (일부 발췌)

```json
[
  {
    "symbol": "AAPL",
    "date": "2026-08-10",
    "recordDate": "2026-08-10",
    "paymentDate": "2026-08-13",
    "declarationDate": "2026-07-30",
    "adjDividend": 0.27,
    "dividend": 0.27,
    "yield": 0.3438655680269902,
    "frequency": "Quarterly"
  },
  {
    "symbol": "AAPL",
    "date": "2020-08-07",
    "recordDate": "2020-08-10",
    "paymentDate": "2020-08-13",
    "declarationDate": "2020-07-30",
    "adjDividend": 0.205,
    "dividend": 0.82,
    "yield": 0.7155071550715508,
    "frequency": "Quarterly"
  },
  {
    "symbol": "AAPL",
    "date": "1995-02-13",
    "recordDate": "1995-02-17",
    "paymentDate": "1995-03-10",
    "declarationDate": "",
    "adjDividend": 0.00107143,
    "dividend": 0.12,
    "yield": 1.0971302767324578,
    "frequency": "Quarterly"
  }
]
```
