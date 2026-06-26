package org.example.ETL.entities;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "device")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column (name = "id")
    private int id;

    @Column(name = "device_name", nullable = false, length = 30) // Обратите внимание на стиль snake_case
    private String deviceName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    // --- УДАЛИТЬ ЭТОТ БЛОК ---
    // @OneToMany(mappedBy = "device", cascade = CascadeType.ALL, orphanRemoval = true)
    // private List<Indicator> indicators;
    // --------------------------

    // --- ВСТАВИТЬ ЭТОТ БЛОК ---
    // Устройство теперь содержит список Метрик (DataItem), а не Индикаторов
    @OneToMany(mappedBy = "device", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DataItem> dataItems;
    // --------------------------

    public Device() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public Machine getMachine() {
        return machine;
    }

    public void setMachine(Machine machine) {
        this.machine = machine;
    }

    // --- ОБНОВИТЬ ГЕТТЕРЫ И СЕТТЕРЫ ---
    public List<DataItem> getDataItems() {
        return dataItems;
    }

    public void setDataItems(List<DataItem> dataItems) {
        this.dataItems = dataItems;
    }
}