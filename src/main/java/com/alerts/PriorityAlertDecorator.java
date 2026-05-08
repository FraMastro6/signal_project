package com.alerts;

public class PriorityAlertDecorator extends AlertDecorator{
    private final int priorityLevel;

    public PriorityAlertDecorator(Alert wrappedAlert, int priorityLevel) {
        super(wrappedAlert);
        this.priorityLevel = priorityLevel;
    }
    @Override
    public String getCondition() {
        return "PRIORITY: " + priorityLevel + " |" + super.getCondition();
    }
}
