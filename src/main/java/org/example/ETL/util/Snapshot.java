package org.example.ETL.util;

import java.time.Instant;

/**
 * Небольшой иммутабельный контейнер для результата периодического чтения.
 * Хранит:
 *  - момент получения (fetchedAt),
 *  - статус (HTTP-код или ваш внутренний код),
 *  - полезную нагрузку (body) при успехе,
 *  - текст ошибки (error) при неуспехе.
 *
 * @param <T> тип полезной нагрузки (например, String для сырого XML)
 */
public class Snapshot<T> {

    private final boolean ok;
    private final T body;
    private final int statusCode;
    private final String error;
    private final Instant time;

    private Snapshot(boolean ok, T body, int statusCode, String error, Instant time) {
        this.ok = ok;
        this.body = body;
        this.statusCode = statusCode;
        this.error = error;
        this.time = time;
    }

    public static <T> Snapshot<T> ok(T body, int statusCode, Instant time) {
        return new Snapshot<>(true, body, statusCode, null, time);
    }

    public static <T> Snapshot<T> error(String error, int statusCode, Instant time) {
        return new Snapshot<>(false, null, statusCode, error, time);
    }

    public boolean isOk() {
        return ok;
    }

    public T getBody() {
        return body;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getError() {
        return error;
    }

    public Instant getTime() {
        return time;
    }

    @Override
    public String toString() {
        return "Snapshot{" +
                "ok=" + ok +
                ", statusCode=" + statusCode +
                ", time=" + time +
                (ok ? "" : ", error='" + error + '\'') +
                '}';
    }
}