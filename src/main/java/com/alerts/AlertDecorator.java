package com.alerts;

public class AlertDecorator implements Alert{
    protected final Alert wrappedAlert;

    protected AlertDecorator(Alert wrappedAlert) {
        this.wrappedAlert = wrappedAlert;
    }

    @Override
    public int getPatientId() {
        return wrappedAlert.getPatientId();
    }

    @Override
    public String getCondition() {
        return wrappedAlert.getCondition();
    }

    @Override
    public long getTimestamp() {
    return wrappedAlert.getTimestamp();
    }
}
