# FMP Dividends API 응답 분석 노트

> 2026-09-11, AAPL 실제 호출 결과를 보고 정리. 파싱 코드와 Rule Validator 작성 시 참고.

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
