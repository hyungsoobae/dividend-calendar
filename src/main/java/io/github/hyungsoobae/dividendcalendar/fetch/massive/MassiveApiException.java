package io.github.hyungsoobae.dividendcalendar.fetch.massive;

/**
 * Massive API 호출이나 응답 처리에 실패했을 때 발생한다.
 * 메시지에는 티커 등 재현에 필요한 정보만 담고, API 키가 포함될 수 있는 URL은 넣지 않는다.
 */
public class MassiveApiException extends RuntimeException {

    public MassiveApiException(String message) {
        super(message);
    }

    public MassiveApiException(String message, Throwable cause) {
        super(message, cause);
    }
}