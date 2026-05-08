package com.alerts.factories;

import com.alerts.BasicAlert;

/**
 * Factory for creating ECG related alerts.
 *
 * Covers abnormal spike detection in ECG windowed readings.
 */
public class ECGAlertFactory extends AlertFactory{
    @Override
    public BasicAlert createAlert(int patientId, String condition, long timestamp) {
        return new BasicAlert(patientId, condition, timestamp);
    }
}
