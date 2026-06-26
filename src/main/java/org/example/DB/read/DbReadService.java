package org.example.DB.read;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.ETL.entities.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public class DbReadService {

    @PersistenceContext
    private EntityManager em;

    // ===== Machine =====
    @Transactional(readOnly = true)
    public Optional<Machine> findMachineByName(String name) {
        List<Machine> list = em.createQuery("select m from Machine m where m.machineName = :name", Machine.class)
                .setParameter("name", name).setMaxResults(1).getResultList();
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    // ===== Device =====
    @Transactional(readOnly = true)
    public List<Device> listDevicesByMachine(int machineId) {
        return em.createQuery("select d from Device d where d.machine.id = :mid", Device.class)
                .setParameter("mid", machineId).getResultList();
    }

    @Transactional(readOnly = true)
    public Optional<Device> findDevice(int machineId, String deviceName) {
        List<Device> list = em.createQuery(
                        "select d from Device d where d.machine.id = :mid and d.deviceName = :name", Device.class)
                .setParameter("mid", machineId)
                .setParameter("name", deviceName)
                .setMaxResults(1)
                .getResultList();
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    // ===== DataItem =====
    @Transactional(readOnly = true)
    public Optional<DataItem> findDataItem(int deviceId, String dataItemId) {
        List<DataItem> list = em.createQuery(
                        "select di from DataItem di where di.device.id = :did and di.dataItemId = :dii", DataItem.class)
                .setParameter("did", deviceId)
                .setParameter("dii", dataItemId)
                .getResultList();
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    // ===== Unit =====
    @Transactional(readOnly = true)
    public Optional<Unit> findUnitByShortName(String shortName) {
        List<Unit> list = em.createQuery("select u from Unit u where u.shortName = :sn", Unit.class)
                .setParameter("sn", shortName).setMaxResults(1).getResultList();
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    // ===== Indicator =====
    @Transactional(readOnly = true)
    public List<Indicator> getLatestIndicators(int deviceId, int limit) {
        return em.createQuery(
                "select i from Indicator i join fetch i.unit join fetch i.dataItem where i.dataItem.device.id = :did order by i.time desc, i.id desc",
                Indicator.class).setParameter("did", deviceId).setMaxResults(limit).getResultList();
    }

    @Transactional(readOnly = true)
    public List<Indicator> getIndicatorsByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        return em.createQuery(
                        "select i from Indicator i " +
                                "join fetch i.unit " +
                                "join fetch i.dataItem " +
                                "where i.id in :ids " +
                                "order by i.time asc", Indicator.class)
                .setParameter("ids", ids)
                .getResultList();
    }
}