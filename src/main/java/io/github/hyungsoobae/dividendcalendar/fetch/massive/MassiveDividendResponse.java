package io.github.hyungsoobae.dividendcalendar.fetch.massive;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Massive 배당 API 응답. 최상위가 배열이 아니라 객체로 감싸여 있다.
 *
 * @param status   OK 등 처리 상태
 * @param requestId 요청 식별자. 장애 문의 시 필요
 * @param results  배당 레코드 목록. 결과가 없어도 빈 배열로 오는지는 미확인
 * @param nextUrl  다음 페이지 주소. 필드명이 아직 확인되지 않았다 (실제 응답으로 검증 필요)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MassiveDividendResponse(
        String status,
        @JsonProperty("request_id") String requestId,
        List<MassiveDividend> results,
        @JsonProperty("next_url") String nextUrl
) {
}