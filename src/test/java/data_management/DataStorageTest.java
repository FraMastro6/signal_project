package data_management;

import static org.junit.jupiter.api.Assertions.*;

import com.alerts.*;
import com.data_management.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

class DataStorageTest {
    private DataStorage storage;
    private AlertGenerator alertGenerator;
    private static final long T1 = 1_000_000L;
    private static final long T2 = T1 + 60_000L;
    private static final long T3 = T2 + 60_000L;
    private static final long T4 = T3 + 60_000L;
    private static final int PATIENT_ID = 2;



    @BeforeEach
    void setUp() {
        storage = DataStorage.getInstance();
        storage.clearAllData();
        alertGenerator = new AlertGenerator(storage);
    }

    //check substring
    private boolean hasAlert(String conditionSubstring) {
        return alertGenerator.getAlerts().stream()
                .anyMatch(a -> a.getCondition().toLowerCase()
                        .contains(conditionSubstring.toLowerCase()));
    }
    private Patient getPatient() {
        return storage.getAllPatients().stream()
                .filter(p -> p.getPatientId() == PATIENT_ID)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Patient not found"));
    }
    @Test
    @DisplayName("DataStorage is a singleton, getInstance returns same instance")
    void testDataStorageIsSingleton() {
        DataStorage instance1 = DataStorage.getInstance();
        DataStorage instance2 = DataStorage.getInstance();

        assertNotNull(instance1);
        assertSame(instance1, instance2, "Should return the same singleton instance");
    }
    @Test
    @DisplayName("Add patient data creates new patient if not exists")
    void testAddPatientDataCreatesNewPatient() {
        storage.addPatientData(PATIENT_ID, 120.5, "systolic", T1);

        List<Patient> patients = storage.getAllPatients();
        assertEquals(1, patients.size());
        assertEquals(PATIENT_ID, patients.get(0).getPatientId());
    }
    @Test
    @DisplayName("Add multiple records to same patient")
    void testAddMultipleRecordsToSamePatient() {
        storage.addPatientData(PATIENT_ID, 120.5, "systolic", T1);
        storage.addPatientData(PATIENT_ID, 121.0, "systolic", T2);
        storage.addPatientData(PATIENT_ID, 122.5, "systolic", T3);

        List<PatientRecord> records = storage.getRecords(PATIENT_ID, 0, Long.MAX_VALUE);
        assertEquals(3, records.size());
    }
    @Test
    @DisplayName("Add records for multiple different patients")
    void testAddRecordsForMultiplePatients() {
        storage.addPatientData(1, 120.5, "systolic", T1);
        storage.addPatientData(2, 80.0, "diastolic", T1);
        storage.addPatientData(3, 98.5, "saturation", T1);

        List<Patient> patients = storage.getAllPatients();
        assertEquals(3, patients.size());
    }
    @Test
    @DisplayName("Add different record types for same patient")
    void testDifferentRecordTypesForSamePatient() {
        storage.addPatientData(PATIENT_ID, 120.5, "systolic", T1);
        storage.addPatientData(PATIENT_ID, 80.0, "diastolic", T1);
        storage.addPatientData(PATIENT_ID, 98.5, "saturation", T1);
        storage.addPatientData(PATIENT_ID, 72.0, "heartRate", T1);

        List<PatientRecord> records = storage.getRecords(PATIENT_ID, 0, Long.MAX_VALUE);
        assertEquals(4, records.size());
    }
    @Test
    @DisplayName("Patient.getRecords correctly filters by time window boundaries")
    void testPatientGetRecordsTimeRange() {
        Patient patient = new Patient(1);
        patient.addRecord(100.0, "HeartRate", 1000L);
        patient.addRecord(105.0, "HeartRate", 2000L);
        patient.addRecord(110.0, "HeartRate", 3000L);
        patient.addRecord(115.0, "HeartRate", 4000L);
        patient.addRecord(120.0, "HeartRate", 5000L);

        List<PatientRecord> records = patient.getRecords(2000L, 4000L);

        assertEquals(3, records.size());
        assertEquals(2000L, records.get(0).getTimestamp());
        assertEquals(4000L, records.get(2).getTimestamp());
    }
    @Test
    @DisplayName("Get records returns empty list when patient doesn't exist")
    void testGetRecordsForNonExistentPatient() {
        List<PatientRecord> records = storage.getRecords(999, 0, Long.MAX_VALUE);

        assertNotNull(records);
        assertTrue(records.isEmpty());
    }
    @Test
    @DisplayName("Get records returns empty list when no records in time range")
    void testGetRecordsNoRecordsInTimeRange() {
        storage.addPatientData(PATIENT_ID, 100.0, "systolic", T1);
        storage.addPatientData(PATIENT_ID, 110.0, "systolic", T2);

        List<PatientRecord> records = storage.getRecords(PATIENT_ID, T3, T4);
        assertTrue(records.isEmpty());
    }


    @Test
    @DisplayName("FileDataReader correctly parses valid data lines from a directory")
    void testFileDataReader(@TempDir Path tempDir) throws IOException {
        // 1. Create a temporary file with simulated simulator output
        Path file = tempDir.resolve("simulated_data.txt");
        List<String> lines = Arrays.asList(
                "Patient ID: 3, Timestamp: 1700000000000, Label: HeartRate, Data: 75.0",
                "Patient ID: 3, Timestamp: 1700000001000, Label: SystolicBloodPressure, Data: 120.0",
                "should be skipped by the reader!!!!!!!!!!!!!!!!!!!!!!!!!!!!!",
                "Patient ID: 4, Timestamp: 1700000002000, Label: Saturation, Data: 98.0"
        );
        Files.write(file, lines);

        DataStorage tempStorage = DataStorage.getInstance();
        FileDataReader reader = new com.data_management.FileDataReader(tempDir.toString());

        reader.readData(tempStorage);

        List<PatientRecord> patient3Records = tempStorage.getRecords(3, 1690000000000L, 1710000000000L);
        assertEquals(2, patient3Records.size());
        assertEquals("HeartRate", patient3Records.get(0).getRecordType());
        assertEquals(75.0, patient3Records.get(0).getMeasurementValue());

        List<PatientRecord> patient4Records = tempStorage.getRecords(4, 1690000000000L, 1710000000000L);
        assertEquals(1, patient4Records.size());
    }
    @Test
    @DisplayName("FileDataReader handles empty directory")
    void testFileDataReaderEmptyDirectory(@TempDir Path tempDir) throws IOException {
        DataStorage tempStorage = DataStorage.getInstance();
        FileDataReader reader = new FileDataReader(tempDir.toString());

        reader.readData(tempStorage);

        List<Patient> patients = tempStorage.getAllPatients();
        assertTrue(patients.isEmpty());
    }

}
