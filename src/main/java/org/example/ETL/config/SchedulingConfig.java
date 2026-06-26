package org.example.ETL.config;

import org.example.ETL.service.MtconnectReaderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulingConfig {

    @Bean
    public MtconnectReaderService mtconnectReaderService(
            @Value("${mtconnect.url}") String url,
            @Value("${mtconnect.poll-ms}") String pollMs, // Считываем для отладки
            @Value("${mtconnect.http.connect-timeout-ms:2000}") long connectTimeoutMs,
            @Value("${mtconnect.http.read-timeout-ms:4000}") long readTimeoutMs
    ) {
        // Блок отладочного вывода в консоль при старте
        System.out.println("\n=================================================");
        System.out.println(">>> ПРОВЕРКА ПАРАМЕТРОВ MTCONNECT (application.properties) <<<");
        System.out.println("=================================================");
        System.out.println("mtconnect.url                      = " + url);
        System.out.println("mtconnect.poll-ms                  = " + pollMs + " ms");
        System.out.println("mtconnect.http.connect-timeout-ms  = " + connectTimeoutMs + " ms");
        System.out.println("mtconnect.http.read-timeout-ms     = " + readTimeoutMs + " ms");
        System.out.println("=================================================\n");

        return new MtconnectReaderService(url, connectTimeoutMs, readTimeoutMs);
    }
}