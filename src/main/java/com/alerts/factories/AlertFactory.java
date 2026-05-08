package com.alerts.factories;

import com.alerts.BasicAlert;

/**
 * Abstract factory defining the Factory Method for creating {@link BasicAlert} objects.
 */
public abstract class AlertFactory {
    /**
     * Factory method — creates an {@link BasicAlert} of the appropriate type.
     * @param patientId the ID of the patient this alert concerns
     * @param condition the condition description
     * @param timestamp when the alert was triggered (ms)
     * @return a new Alert instance
     */
    public abstract BasicAlert createAlert(int patientId, String condition, long timestamp);

}
