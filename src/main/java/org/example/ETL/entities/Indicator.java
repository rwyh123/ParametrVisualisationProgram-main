package org.example.ETL.entities;


import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table (name = "indicator")
public class Indicator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column (name = "id")
    private int id;

    @Column (name = "value",  nullable = true)
    private Double value;

    @Column(nullable = false)
    private OffsetDateTime time;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "data_item_id", nullable = false)
    private DataItem dataItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @Column(name = "text_value", length = 255)
    private String textValue;


    public String getTextValue() {
        return textValue;
    }

    public void setTextValue(String textValue) {
        this.textValue = textValue;
    }

    public DataItem getDataItem() { return dataItem; }
    public void setDataItem(DataItem dataItem) { this.dataItem = dataItem; }

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }

    public OffsetDateTime getTime() {
        return time;
    }

    public void setTime(OffsetDateTime time) {
        this.time = time;
    }
}
