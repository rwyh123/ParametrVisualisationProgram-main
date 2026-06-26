package org.example.Events.AddEvent;

import java.util.List;

public class NewIndicatorsIdsEvent {

    private final List<Integer> indicatorIds;

    public NewIndicatorsIdsEvent(List<Integer> indicatorIds) {
        this.indicatorIds = indicatorIds;
    }

    public List<Integer> getIndicatorIds() {
        return indicatorIds;
    }
}