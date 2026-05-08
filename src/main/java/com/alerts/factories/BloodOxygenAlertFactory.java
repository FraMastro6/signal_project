package com.alerts.factories;

import com.alerts.BasicAlert;

/**
 * Factory for creating blood oxygen (saturation) related alerts.
 *
 * Covers low saturation alerts and rapid saturation drop alerts.
 */
public class BloodOxygenAlertFactory extends AlertFactory{
    @Override
    public BasicAlert createAlert(int patientId, String condition, long timestamp) {
        return new BasicAlert(patientId, condition, timestamp);
    }
}
