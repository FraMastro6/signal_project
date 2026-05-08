package com.alerts;

import com.alerts.factories.BloodOxygenAlertFactory;
import com.alerts.factories.BloodPressureAlertFactory;
import com.alerts.factories.ECGAlertFactory;
import com.alerts.strategies.AlertStrategy;
import com.alerts.strategies.BloodPressureStrategy;
import com.alerts.strategies.HeartRateStrategy;
import com.alerts.strategies.OxygenSaturationStrategy;
import com.data_management.DataStorage;
import com.data_management.Patient;
import com.data_management.PatientRecord;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Math.abs;

/**
 * The {@code AlertGenerator} class is responsible for monitoring patient data
 * and generating alerts when certain predefined conditions are met. This class
 * relies on a {@link DataStorage} instance to access patient data and evaluate
 * it against specific health criteria.
 */
public class AlertGenerator {
    private DataStorage dataStorage;
    private List<Alert> alerts = new ArrayList<>();
    private final List<AlertStrategy> strategies = new ArrayList<>();


    //Factories
    private final BloodPressureAlertFactory bloodPressureFactory = new BloodPressureAlertFactory();
    private final BloodOxygenAlertFactory bloodOxygenFactory = new BloodOxygenAlertFactory();
    private final ECGAlertFactory ecgFactory = new ECGAlertFactory();

    /**
     * Constructs an {@code AlertGenerator} with a specified {@code DataStorage}.
     * The {@code DataStorage} is used to retrieve patient data that this class
     * will monitor and evaluate.
     *
     * @param dataStorage the data storage system that provides access to patient
     *                    data
     */
    public AlertGenerator(DataStorage dataStorage) {
        this.dataStorage = dataStorage;

        strategies.add(new BloodPressureStrategy());
        strategies.add(new HeartRateStrategy());
        strategies.add(new OxygenSaturationStrategy());
    }

    /**
     * Evaluates the specified patient's data to determine if any alert conditions
     * are met. If a condition is met, an alert is triggered via the
     * {@link #triggerAlert}
     * method. This method should define the specific conditions under which an
     * alert
     * will be triggered.
     *
     * @param patient the patient data to evaluate for alert conditions
     */
    public void evaluateData(Patient patient) {
        List<PatientRecord> records = patient.getPatientRecords();
        Instant instant = Instant.now();
        long timeStampMillis = instant.toEpochMilli();

        for (AlertStrategy strategy : strategies) {
            strategy.checkAlert(patient, dataStorage).forEach(this::triggerAlert);
        }

        //Hypotensive Hypoxemia Alert
        List<PatientRecord> systolicRecords = records.stream()
                .filter(x -> x.getRecordType().equalsIgnoreCase("systolic"))
                .sorted(Comparator.comparingLong(PatientRecord::getTimestamp))
                .collect(Collectors.toList());

        List<PatientRecord> diastolicRecords = records.stream()
                .filter(x -> x.getRecordType().equalsIgnoreCase("diastolic"))
                .sorted(Comparator.comparingLong(PatientRecord::getTimestamp))
                .collect(Collectors.toList());
        List<PatientRecord> saturationRecords = records.stream().filter(x -> x.getRecordType().equalsIgnoreCase("saturation")).sorted(Comparator.comparingLong(PatientRecord::getTimestamp)).collect(Collectors.toList());

        //Combined Alert: Hypotensive Hypoxemia Alert
        //Maybe to add at critical Threshold check for optimization
        for (PatientRecord record : systolicRecords) {
            if (record.getMeasurementValue() < 90) {
                for (PatientRecord record_ : saturationRecords) {
                    if (record_.getMeasurementValue() < 92) {
                        Alert alert = new BasicAlert(patient.getPatientId(), "ALERT : Hypotensive Hypoxemia Alert", timeStampMillis);
                        alert = new PriorityAlertDecorator(alert, 5);
                        alert = new RepeatedAlertDecorator(alert, 3, 60000);
                        triggerAlert(alert);
                    }
                }
            }
        }

        // Triggered alert (button press)
        List<PatientRecord> buttonAlerts = records.stream()
                .filter(r -> r.getRecordType().equals("Alert"))
                .collect(Collectors.toList());
        for (PatientRecord record : buttonAlerts) {
            if (record.getMeasurementValue() == 1.0) {
                triggerAlert(new BasicAlert(patient.getPatientId(),
                        "Manual Alert Button Triggered", timeStampMillis));
            }
        }
    }

    /**
     * Triggers an alert for the monitoring system. This method can be extended to
     * notify medical staff, log the alert, or perform other actions. The method
     * currently assumes that the alert information is fully formed when passed as
     * an argument.
     *
     * @param alert the alert object containing details about the alert condition
     */
    private void triggerAlert(Alert alert) {
        // Implementation might involve logging the alert or notifying staff
        //TODO Full method implementation
        alerts.add(alert);
    }

    public List<Alert> getAlerts() {
        return alerts;
    }

}
