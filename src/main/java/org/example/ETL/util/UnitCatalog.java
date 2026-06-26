package org.example.ETL.util;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.example.ETL.entities.Unit;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Кэширует единицы измерения, уже существующие в БД. Ничего не создаёт. */
public class UnitCatalog {

    @PersistenceContext
    private EntityManager em;

    private final Map<String, Unit> byShort = new ConcurrentHashMap<>();
    private final Map<String, Unit> byFull  = new ConcurrentHashMap<>();

    // УДАЛЕНО: @PostConstruct <--- Эту строку нужно убрать!
    @Transactional
    public void loadAll() {
        // Логика остается прежней
        List<Unit> units = em.createQuery("select u from Unit u", Unit.class).getResultList();
        byShort.clear();
        byFull.clear();
        for (Unit u : units) {
            if (u.getShortName() != null && !u.getShortName().isBlank()) {
                byShort.put(u.getShortName(), u);
            }
            if (u.getFullName() != null && !u.getFullName().isBlank()) {
                byFull.put(u.getFullName(), u);
            }
        }
        System.out.println(">>> UnitCatalog loaded: " + units.size() + " units.");
    }

    public Optional<Unit> byShort(String shortName) {
        return Optional.ofNullable(byShort.get(shortName));
    }

    public Optional<Unit> byFull(String fullName) {
        return Optional.ofNullable(byFull.get(fullName));
    }

    /** На случай, если скриптом добавили новые единицы — можно вручную обновить кэш. */
    @Transactional
    public void reload() { loadAll(); }
}
