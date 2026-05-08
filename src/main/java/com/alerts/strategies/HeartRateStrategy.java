package com.alerts.strategies;

import com.alerts.BasicAlert;
import com.alerts.factories.ECGAlertFactory;
import com.data_management.DataStorage;
import com.data_management.Patient;
import com.data_management.PatientRecord;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class HeartRateStrategy implements AlertStrategy{
    private final ECGAlertFactory factory = new ECGAlertFactory();

    @Override
    public List<BasicAlert> checkAlert(Patient patient, DataStorage dataStorage) {
        List<PatientRecord> records = patient.getPatientRecords();
        Instant instant = Instant.now();
        long timeStampMillis = instant.toEpochMilli();
        List<BasicAlert> triggered = new ArrayList<>();


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
                    triggered.add(factory.createAlert(patient.getPatientId(), "ECG Abnormal Spike", timeStampMillis));

                }
            }
        }


        return triggered;
    }
}
