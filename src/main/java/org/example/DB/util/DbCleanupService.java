package org.example.DB.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DbCleanupService {

    @PersistenceContext
    private EntityManager em;

    /**
     * Очищает оперативные данные (измерения, датчики, устройства, станки).
     * Справочник Unit не трогает.
     */
    @Transactional
    public void cleanDbExceptUnits() {
        System.out.println(">>> CLEANING DB (Indicators -> DataItems -> Devices -> Machines)...");
        // Порядок важен из-за Foreign Keys!
        em.createQuery("delete from Indicator").executeUpdate();
        em.createQuery("delete from DataItem").executeUpdate();
        em.createQuery("delete from Device").executeUpdate();
        em.createQuery("delete from Machine").executeUpdate();
        System.out.println(">>> DB CLEANED.");
    }
}