package org.example.ETL.config;

import org.example.DB.read.DbReadService;
import org.example.DB.util.DbCleanupService;
import org.example.DB.write.DbWriteService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DbBeansConfig {

    @Bean
    public DbCleanupService dbCleanupService() {
        return new DbCleanupService();
    }

    @Bean
    public DbReadService dbReadService() {
        return new DbReadService();
    }

    @Bean
    public DbWriteService dbWriteService(DbReadService readService) {
        return new DbWriteService(readService);
    }
}