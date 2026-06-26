package org.example.ETL.entities;

import com.fasterxml.jackson.annotation.JsonIgnore; // <--- ДОБАВИТЬ ИМПОРТ
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(
        name = "data_item",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_device_data_item",
                columnNames = {"device_id", "data_item_id"}
        )
)
public class DataItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_item_id", nullable = false)
    private String dataItemId;

    @Column(name = "category")
    private String category;

    // ВАЖНО: Добавьте @JsonIgnore здесь, чтобы не тянуть всю историю в JSON
    @JsonIgnore
    @OneToMany(mappedBy = "dataItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Indicator> indicators;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    @JsonIgnore // Устройство тоже можно игнорировать в JSON для легкости
    private Device device;

    public DataItem() {}

    public DataItem(Device device, String dataItemId, String category) {
        this.device = device;
        this.dataItemId = dataItemId;
        this.category = category;
    }

    // --- ГЕТТЕРЫ (ОБЯЗАТЕЛЬНО НУЖНЫ ДЛЯ JSON) ---

    public Long getId() {
        return id;
    }

    public String getDataItemId() { // <--- БЕЗ ЭТОГО ПОЛЯ ГРАФИКИ НЕ БУДУТ РАБОТАТЬ
        return dataItemId;
    }

    public String getCategory() {
        return category;
    }

    // ... сеттеры ...
    public void setDataItemId(String dataItemId) { this.dataItemId = dataItemId; }
    public void setCategory(String category) { this.category = category; }
    public void setDevice(Device device) { this.device = device; }
    public void setIndicators(List<Indicator> indicators) { this.indicators = indicators; }
    public List<Indicator> getIndicators() { return indicators; }
}