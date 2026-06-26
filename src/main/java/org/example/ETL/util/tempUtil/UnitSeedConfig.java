package org.example.ETL.util.tempUtil;

import org.example.DB.write.DbWriteService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UnitSeedConfig {

    @Bean
    public UnitSeeder unitSeeder(DbWriteService dbService) {
        return new UnitSeeder(dbService);
    }
}
