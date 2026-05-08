package com.alerts.strategies;

import com.alerts.BasicAlert;
import com.data_management.DataStorage;
import com.data_management.Patient;

import java.util.List;

public interface AlertStrategy {

    /**
     * Evaluates the patient's records and returns any alerts that should be triggered.
     *
     * @param patient     the patient to evaluate
     * @param dataStorage the storage used to retrieve records
     * @return a list of Alert objects to trigger — empty if no conditions are met
     */
    List<BasicAlert> checkAlert(Patient patient, DataStorage dataStorage);
}
