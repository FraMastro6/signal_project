package com.alerts.strategies;

import com.alerts.BasicAlert;
import com.alerts.factories.BloodPressureAlertFactory;
import com.data_management.DataStorage;
import com.data_management.Patient;
import com.data_management.PatientRecord;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class BloodPressureStrategy implements AlertStrategy{
    private final BloodPressureAlertFactory factory = new BloodPressureAlertFactory();


    @Override
    public List<BasicAlert> checkAlert(Patient patient, DataStorage dataStorage) {
        List<PatientRecord> records = patient.getPatientRecords();
        Instant instant = Instant.now();
        long timeStampMillis = instant.toEpochMilli();
        List<BasicAlert> triggered = new ArrayList<>();

        // pressure check: Trend Alert
        List<PatientRecord> systolicRecords = records.stream()
                .filter(x -> x.getRecordType().equalsIgnoreCase("systolic"))
                .sorted(Comparator.comparingLong(PatientRecord::getTimestamp))
                .collect(Collectors.toList());

        List<PatientRecord> diastolicRecords = records.stream()
                .filter(x -> x.getRecordType().equalsIgnoreCase("diastolic"))
                .sorted(Comparator.comparingLong(PatientRecord::getTimestamp))
                .collect(Collectors.toList());

        checkPressureTrend(systolicRecords, patient, timeStampMillis, triggered);
        checkPressureTrend(diastolicRecords, patient, timeStampMillis, triggered);

        //pressure check : Critical Threshold Alert
        for(PatientRecord record : systolicRecords) {
            if (record.getMeasurementValue() > 180 || record.getMeasurementValue() < 90) {
                triggered.add(factory.createAlert(patient.getPatientId(), "Critical Systolic Alert", timeStampMillis));

            }
        }
        for (PatientRecord record : diastolicRecords){
            if (record.getMeasurementValue() > 120 || record.getMeasurementValue() < 60) {
                triggered.add(factory.createAlert(patient.getPatientId(), "Critical Diastolic Alert", timeStampMillis));

            }
        }
        return triggered;
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
                                    long timeStampMillis, List<BasicAlert> triggered) {

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
                triggered.add(factory.createAlert(patient.getPatientId(), "Pressure Trend Alert", timeStampMillis));
            }
        }
    }


}
