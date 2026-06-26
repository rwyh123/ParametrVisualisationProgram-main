package org.example;

import org.example.DB.util.DbCleanupService;
import org.example.ETL.config.*;
import org.example.ETL.util.UnitCatalog; // Добавить импорт
import org.example.ETL.util.tempUtil.UnitSeedConfig;
import org.example.Server.controller.SseController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({
        HibernateConfig.class,
        DbBeansConfig.class,
        UnitSeedConfig.class,
        UnitsBeansConfig.class,
        SchedulingConfig.class,
        IngestConfig.class,
        SseController.class
})
public class Main {

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(Main.class, args);

        // 1. Очистка БД (если включено)
        String clearParam = ctx.getEnvironment().getProperty("app.clear-db-on-start", "false");
        if ("true".equalsIgnoreCase(clearParam)) {
            DbCleanupService cleaner = ctx.getBean(DbCleanupService.class);
            cleaner.cleanDbExceptUnits();
        }

        // 2. ВАЖНО: Загружаем справочник единиц измерения в память
        // Мы делаем это здесь, так как контекст уже полностью поднят и транзакции работают.
        UnitCatalog catalog = ctx.getBean(UnitCatalog.class);
        catalog.loadAll();

        System.out.println("=== SYSTEM STARTED ===");
        System.out.println("UI available at: http://localhost:8080/cnc");
        System.out.println("SSE stream at:   http://localhost:8080/stream");
    }
}