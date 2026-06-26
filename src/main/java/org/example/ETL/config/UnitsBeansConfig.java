package org.example.ETL.config;

import org.example.ETL.util.UnitCatalog;
import org.example.ETL.util.UnitResolverExistingOnly;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UnitsBeansConfig {
    @Bean
    public UnitCatalog unitCatalog() {
        return new UnitCatalog();
    }

    @Bean
    public UnitResolverExistingOnly unitResolverExistingOnly(UnitCatalog catalog) {
        return new UnitResolverExistingOnly(catalog);
    }
}
