package org.example.ETL.config;

import org.example.DB.write.DbWriteService;
import org.example.ETL.service.MtconnectIngestService;
import org.example.ETL.service.MtconnectReaderService;
import org.example.ETL.util.UnitCatalog;
import org.example.ETL.util.UnitResolverExistingOnly;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IngestConfig {
    // ...
    @Bean
    public MtconnectIngestService mtconnectIngestService(
            MtconnectReaderService reader,
            UnitResolverExistingOnly unitResolverExistingOnly,
            DbWriteService dbWriteService, // Принимаем WriteService
            @Value("${mtconnect.machine-name:DemoMachine}") String machineName,
            ApplicationEventPublisher publisher
    ) {
        return new MtconnectIngestService(reader, unitResolverExistingOnly, dbWriteService, machineName, publisher);
    }
}
