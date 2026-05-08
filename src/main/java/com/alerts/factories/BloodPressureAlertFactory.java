package com.alerts.factories;

import com.alerts.BasicAlert;

/**
 * Factory for creating blood pressure related alerts.
 *
 * Covers both critical threshold alerts (systolic/diastolic out of range)
 * and trend alerts (consistently rising or falling pressure).
 */
public class BloodPressureAlertFactory extends AlertFactory{

    @Override
    public BasicAlert createAlert(int patientId, String condition, long timestamp) {
        return new BasicAlert(patientId, condition, timestamp);
    }
}
