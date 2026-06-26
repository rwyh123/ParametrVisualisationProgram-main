package org.example.ETL.util.tempUtil;

import jakarta.annotation.PostConstruct;
import org.example.DB.write.DbWriteService;

/**
 * Простая инициализация таблицы unit при старте приложения.
 * Работает идемпотентно: повторный запуск не создаёт дублей.
 */
public class UnitSeeder {

    private final DbWriteService db; // Используем WriteService

    public UnitSeeder(DbWriteService db) {
        this.db = db;
    }

    @PostConstruct
    public void seed() {
        // аналог твоего INSERT ... ON CONFLICT
        seedUnit("°C",  "Celsius");
        seedUnit("deg", "Degree");
        seedUnit("rpm", "Revolutions per minute");
        seedUnit("A",   "Ampere");
        seedUnit("V",   "Volt");
        seedUnit("W",   "Watt");
        seedUnit("N·m", "Newton meter");
        seedUnit("mm",  "Millimeter");
        seedUnit("in",  "Inch");
        seedUnit("%",   "Percent");
        seedUnit("bar", "Bar");
        seedUnit("Pa",  "Pascal");
        seedUnit("m",   "Meter");
        seedUnit("s",   "Second");
        seedUnit("Hz",  "Hertz");
        seedUnit("-",   "No unit");
    }

    private void seedUnit(String shortName, String fullName) {
        // Внутри DbService.getOrCreateUnit:
        // - ищет Unit по shortName
        // - если нет — создаёт
        // - если есть — просто возвращает (можно дополнительно обновлять fullName)
        db.getOrCreateUnit(shortName, fullName);
    }
}
