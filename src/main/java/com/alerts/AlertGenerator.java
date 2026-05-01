package com.alerts;

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

        // pressure check: Trend Alert
        List<PatientRecord> systolicRecords = records.stream()
                .filter(x -> x.getRecordType().equalsIgnoreCase("systolic"))
                .sorted(Comparator.comparingLong(PatientRecord::getTimestamp))
                .collect(Collectors.toList());

        List<PatientRecord> diastolicRecords = records.stream()
                .filter(x -> x.getRecordType().equalsIgnoreCase("diastolic"))
                .sorted(Comparator.comparingLong(PatientRecord::getTimestamp))
                .collect(Collectors.toList());

        checkPressureTrend(systolicRecords, patient, timeStampMillis);
        checkPressureTrend(diastolicRecords, patient, timeStampMillis);

        //pressure check : Critical Threshold Alert
        for(PatientRecord record : systolicRecords) {
            if (record.getMeasurementValue() > 180 || record.getMeasurementValue() < 90) {
                triggerAlert(new Alert(patient.getPatientId(), "Critical Systolic Alert", timeStampMillis));
            }
        }
        for (PatientRecord record : diastolicRecords){
            if (record.getMeasurementValue() > 120 || record.getMeasurementValue() < 60) {
                triggerAlert(new Alert(patient.getPatientId(), "Critical Diastolic Alert", timeStampMillis));
            }
        }


        //low saturation
        List<PatientRecord> saturationRecords = records.stream().filter(x -> x.getRecordType().equalsIgnoreCase("saturation")).sorted(Comparator.comparingLong(PatientRecord::getTimestamp)).collect(Collectors.toList());
        for (PatientRecord record : saturationRecords){
            if (record.getMeasurementValue()<92){
                triggerAlert(new Alert(patient.getPatientId(), "Low saturation Alert", timeStampMillis));
            }
        }

        //Saturation drop
        for(int i=0; i<saturationRecords.size()-1; i++){
            PatientRecord r1 = saturationRecords.get(i);
            PatientRecord r2 = saturationRecords.get(i+1);
            double drop = r1.getMeasurementValue()- r2.getMeasurementValue();
            long timeDifference = (r2.getTimestamp() - r1.getTimestamp())/60000; //difference in milliseconds -> sec -> min
            if (drop>=5 && timeDifference<=10){
                triggerAlert(new Alert(patient.getPatientId(), "Rapid Saturation Drop Alert", timeStampMillis));
            }
        }

        //Combined Alert: Hypotensive Hypoxemia Alert
        //Maybe to add at critical Threshold check for optimization
        for(PatientRecord record : systolicRecords){
            if(record.getMeasurementValue()<90){
                for (PatientRecord record_ : saturationRecords){
                    if (record_.getMeasurementValue()<92){
                        triggerAlert(new Alert(patient.getPatientId(), "ALERT : Hypotensive Hypoxemia Alert", timeStampMillis));
                    }
                }
            }
        }
        List<PatientRecord> ecgRecords = records.stream()
                .filter(r -> r.getRecordType().equalsIgnoreCase("ECG"))
                .sorted(Comparator.comparingLong(PatientRecord::getTimestamp))
                .collect(Collectors.toList());
        int windowSize = 3;

        for (int i = 0; i <= ecgRecords.size() - windowSize; i++) {

            List<PatientRecord> window = ecgRecords.subList(i, i + windowSize);

            double avg = window.stream()
                    .mapToDouble(PatientRecord::getMeasurementValue)
                    .average()
                    .orElse(0);

            for (PatientRecord record : window) {
                double value = record.getMeasurementValue();

                if (value > avg * 1.5) {
                    triggerAlert(new Alert(patient.getPatientId(), "ECG Abnormal Spike", timeStampMillis));
                }
            }
        }

        // Triggered alert (button press)
        List<PatientRecord> buttonAlerts = records.stream()
                .filter(r -> r.getRecordType().equals("Alert"))
                .collect(Collectors.toList());
        for (PatientRecord record : buttonAlerts) {
            if (record.getMeasurementValue() == 1.0) {
                triggerAlert(new Alert(patient.getPatientId(),
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

    /**
     * Checks for increasing or decreasing pressure trends
     *
     * @param filtered
     * @param patient
     * @param timeStampMillis
     */
    private void checkPressureTrend(List<PatientRecord> filtered,
                                    Patient patient,
                                    long timeStampMillis) {

        for (int i = 0; i < filtered.size() - 2; i++) {

            double v1 = filtered.get(i).getMeasurementValue();
            double v2 = filtered.get(i + 1).getMeasurementValue();
            double v3 = filtered.get(i + 2).getMeasurementValue();

            boolean increasing =
                    (v2 - v1 > 10) &&
                            (v3 - v2 > 10);

            boolean decreasing =
                    (v1 - v2 > 10) &&
                            (v2 - v3 > 10);

            if (increasing || decreasing) {

                triggerAlert(new Alert(
                        patient.getPatientId(),
                        "Pressure Trend Alert",
                        timeStampMillis
                ));
            }
        }
    }

    public List<Alert> getAlerts() {
        return alerts;
    }

}
