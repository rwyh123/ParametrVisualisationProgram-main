package org.example.ETL.service;

import org.example.ETL.util.Snapshot;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

public class MtconnectReaderService {

    private final String mtconnectUrl;
    private final RestTemplate restTemplate;

    // последнее удачное тело XML
    private volatile String lastXml;

    // последний снапшот (успех/ошибка)
    private volatile Snapshot<String> lastSnapshot;

    public MtconnectReaderService(String mtconnectUrl,
                                  long connectTimeoutMs,
                                  long readTimeoutMs) {
        this.mtconnectUrl = mtconnectUrl;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) connectTimeoutMs);
        factory.setReadTimeout((int) readTimeoutMs);

        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Периодически опрашивает MTConnect.
     * Интервал можно настроить через mtconnect.poll-ms, по умолчанию 3000 мс.
     */
    @Scheduled(fixedDelayString = "${mtconnect.poll-ms:3000}")
    public void poll() {
        try {
            ResponseEntity<String> resp = restTemplate.getForEntity(mtconnectUrl, String.class);
            int status = resp.getStatusCode().value();
            String body = resp.getBody();
            Instant now = Instant.now();

            if (resp.getStatusCode().is2xxSuccessful() && body != null && !body.isBlank()) {
                lastXml = body;
                lastSnapshot = Snapshot.ok(body, status, now);
                // при желании можно включить лог:
                // System.out.println("[reader] OK " + status + " @" + now);
            } else {
                lastSnapshot = Snapshot.error(
                        "Non-OK HTTP status: " + status,
                        status,
                        now
                );
                // lastXml оставляем как старый удачный
                // System.out.println("[reader] ERROR HTTP " + status + " @" + now);
            }
        } catch (Exception e) {
            Instant now = Instant.now();
            lastSnapshot = Snapshot.error(
                    "Exception: " + e.getClass().getSimpleName() + " - " + e.getMessage(),
                    0,
                    now
            );
            // lastXml тоже не трогаем — остаётся последний удачный
            // System.out.println("[reader] EXCEPTION @" + now + " : " + e);
        }
    }

    /** Для dry-run/preview/Main, которые просто хотят XML. */
    public String getLastXml() {
        return lastXml;
    }

    /** Для MtconnectIngestService.runIngest(). */
    public Snapshot<String> getLastSnapshot() {
        return lastSnapshot;
    }
}
