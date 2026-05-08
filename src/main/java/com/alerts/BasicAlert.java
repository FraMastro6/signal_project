package com.alerts;

// Represents an alert
public class BasicAlert implements Alert{
    private int patientId;
    private String condition;
    private long timestamp;

    public BasicAlert(int patientId, String condition, long timestamp) {
        this.patientId = patientId;
        this.condition = condition;
        this.timestamp = timestamp;
    }

    public int getPatientId() {
        return patientId;
    }

    public String getCondition() {
        return condition;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
