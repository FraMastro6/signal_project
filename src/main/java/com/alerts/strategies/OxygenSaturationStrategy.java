package com.alerts.strategies;

import com.alerts.BasicAlert;
import com.alerts.factories.BloodOxygenAlertFactory;
import com.data_management.DataStorage;
import com.data_management.Patient;
import com.data_management.PatientRecord;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class OxygenSaturationStrategy implements AlertStrategy{
    private final BloodOxygenAlertFactory factory = new BloodOxygenAlertFactory();

    @Override
    public List<BasicAlert> checkAlert(Patient patient, DataStorage dataStorage) {
        List<PatientRecord> records = patient.getPatientRecords();
        Instant instant = Instant.now();
        long timeStampMillis = instant.toEpochMilli();
        List<BasicAlert> triggered = new ArrayList<>();

        List<PatientRecord> saturationRecords = records.stream().filter(x -> x.getRecordType().equalsIgnoreCase("saturation")).sorted(Comparator.comparingLong(PatientRecord::getTimestamp)).collect(Collectors.toList());
        //low saturation
        for (PatientRecord record : saturationRecords){
            if (record.getMeasurementValue()<92){
                triggered.add(factory.createAlert(patient.getPatientId(), "Low saturation Alert", timeStampMillis));

            }
        }

        //Saturation drop
        for(int i=0; i<saturationRecords.size()-1; i++){
            PatientRecord r1 = saturationRecords.get(i);
            PatientRecord r2 = saturationRecords.get(i+1);
            double drop = r1.getMeasurementValue()- r2.getMeasurementValue();
            long timeDifference = (r2.getTimestamp() - r1.getTimestamp())/60000; //difference in milliseconds -> sec -> min
            if (drop>=5 && timeDifference<=10){
                triggered.add(factory.createAlert(patient.getPatientId(), "Rapid Saturation Drop Alert", timeStampMillis));

            }
        }

        return triggered;
    }
}
