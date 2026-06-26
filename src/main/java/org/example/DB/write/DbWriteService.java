package org.example.DB.write;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.DB.read.DbReadService;
import org.example.ETL.entities.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Repository
public class DbWriteService {

    @PersistenceContext
    private EntityManager em;

    private final DbReadService readService;

    public DbWriteService(DbReadService readService) {
        this.readService = readService;
    }

    // ===== Machine =====
    @Transactional
    public Machine getOrCreateMachine(String name) {
        return readService.findMachineByName(name).orElseGet(() -> {
            Machine m = new Machine(name);
            em.persist(m);
            return m;
        });
    }

    // ===== Device =====
    @Transactional
    public Device getOrCreateDevice(int machineId, String deviceName) {
        return readService.findDevice(machineId, deviceName).orElseGet(() -> {
            Machine m = em.find(Machine.class, machineId);
            Device d = new Device();
            d.setDeviceName(deviceName);
            d.setMachine(m);
            em.persist(d);
            return d;
        });
    }

    // ===== DataItem =====
    @Transactional
    public DataItem getOrCreateDataItem(Device device, String dataItemId, String category) {
        return readService.findDataItem(device.getId(), dataItemId).orElseGet(() -> {
            DataItem di = new DataItem(device, dataItemId, category);
            em.persist(di);
            return di;
        });
    }

    // ===== Unit =====
    @Transactional
    public Unit getOrCreateUnit(String shortName, String fullName) {
        return readService.findUnitByShortName(shortName).map(u -> {
            u.setFullName(fullName); // Обновляем имя, если нашли
            return u;
        }).orElseGet(() -> {
            Unit u = new Unit();
            u.setShortName(shortName);
            u.setFullName(fullName);
            em.persist(u);
            return u;
        });
    }

    // ===== Indicator =====
    /**
     * Сохраняет индикатор и возвращает сохраненный объект.
     * Возвращаемое значение важно для отправки событий в EventBus.
     */
    @Transactional
    public Indicator saveIndicator(DataItem dataItem, Unit unit, OffsetDateTime time, Double value, String textValue) {
        Indicator ind = new Indicator();
        ind.setDataItem(dataItem);
        ind.setUnit(unit);
        ind.setTime(time);
        ind.setValue(value);
        ind.setTextValue(textValue);
        em.persist(ind);
        return ind;
    }
}