package com.alerts;

/**
 * Interface representing an alert
 */
public interface Alert {
    int getPatientId();

    String getCondition();

    long getTimestamp();
}
