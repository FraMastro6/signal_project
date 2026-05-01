package data_management;

import static org.junit.jupiter.api.Assertions.*;

import com.alerts.AlertGenerator;
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
        storage = new DataStorage();
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
    @DisplayName("triggers on three increasing systolic readings each >10 mmHg apart")
    void increasingSystolicTrend() {
        storage.addPatientData(2, 110, "systolic", T1);
        storage.addPatientData(2, 121, "systolic", T2); // +11
        storage.addPatientData(2, 132, "systolic", T3); // +11

        alertGenerator.evaluateData(getPatient());

        assertTrue(hasAlert("trend"), "Expected a pressure trend alert");
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

        DataStorage tempStorage = new DataStorage();
        FileDataReader reader = new com.data_management.FileDataReader(tempDir.toString());

        reader.readData(tempStorage);

        List<PatientRecord> patient3Records = tempStorage.getRecords(3, 1690000000000L, 1710000000000L);
        assertEquals(2, patient3Records.size());
        assertEquals("HeartRate", patient3Records.get(0).getRecordType());
        assertEquals(75.0, patient3Records.get(0).getMeasurementValue());

        List<PatientRecord> patient4Records = tempStorage.getRecords(4, 1690000000000L, 1710000000000L);
        assertEquals(1, patient4Records.size());
    }

    // blood pressure trend
    @Test
    @DisplayName("triggers on three decreasing systolic readings each >10 apart")
    void decreasingSystolicTrend() {
        storage.addPatientData(PATIENT_ID, 150, "systolic", T1);
        storage.addPatientData(PATIENT_ID, 139, "systolic", T2); // -11
        storage.addPatientData(PATIENT_ID, 128, "systolic", T3); // -11

        alertGenerator.evaluateData(getPatient());

        assertTrue(hasAlert("trend"));
    }

    @Test
    @DisplayName("does NOT trigger trend when change is exactly 10")
    void noTrendAtExactlyTen() {
        storage.addPatientData(PATIENT_ID, 110, "systolic", T1);
        storage.addPatientData(PATIENT_ID, 120, "systolic", T2); // +10, not >10
        storage.addPatientData(PATIENT_ID, 130, "systolic", T3); // +10

        alertGenerator.evaluateData(getPatient());

        assertFalse(hasAlert("trend"));
    }

    @Test
    @DisplayName("triggers diastolic trend alert")
    void increasingDiastolicTrend() {
        storage.addPatientData(PATIENT_ID, 70, "diastolic", T1);
        storage.addPatientData(PATIENT_ID, 81, "diastolic", T2); // +11
        storage.addPatientData(PATIENT_ID, 92, "diastolic", T3); // +11

        alertGenerator.evaluateData(getPatient());

        assertTrue(hasAlert("trend"));
    }

    //  blood pressure critical threshold

    @Test
    @DisplayName("triggers when systolic exceeds 180")
    void systolicTooHigh() {
        storage.addPatientData(PATIENT_ID, 181, "systolic", T1);
        alertGenerator.evaluateData(getPatient());
        assertTrue(hasAlert("systolic"));
    }

    @Test
    @DisplayName("triggers when systolic drops below 90")
    void systolicTooLow() {
        storage.addPatientData(PATIENT_ID, 89, "systolic", T1);
        alertGenerator.evaluateData(getPatient());
        assertTrue(hasAlert("systolic"));
    }

    @Test
    @DisplayName("triggers when diastolic exceeds 120")
    void diastolicTooHigh() {
        storage.addPatientData(PATIENT_ID, 121, "diastolic", T1);
        alertGenerator.evaluateData(getPatient());
        assertTrue(hasAlert("diastolic"));
    }

    @Test
    @DisplayName("triggers when diastolic drops below 60")
    void diastolicTooLow() {
        storage.addPatientData(PATIENT_ID, 59, "diastolic", T1);
        alertGenerator.evaluateData(getPatient());
        assertTrue(hasAlert("diastolic"));
    }

    @Test
    @DisplayName("does NOT trigger threshold alert for normal systolic")
    void normalSystolicNoAlert() {
        storage.addPatientData(PATIENT_ID, 120, "systolic", T1);
        alertGenerator.evaluateData(getPatient());
        assertFalse(hasAlert("systolic"));
    }

    //  saturation

    @Test
    @DisplayName("does NOT trigger low saturation at exactly 92%")
    void saturationBoundary() {
        storage.addPatientData(PATIENT_ID, 92, "saturation", T1);
        alertGenerator.evaluateData(getPatient());
        assertFalse(hasAlert("saturation"));
    }

    @Test
    @DisplayName("triggers rapid drop when >=5% drop within 10 minutes")
    void rapidSaturationDrop() {
        storage.addPatientData(PATIENT_ID, 97, "saturation", T1);
        storage.addPatientData(PATIENT_ID, 92, "saturation", T1 + 5 * 60_000L); // 5 min later, -5%

        alertGenerator.evaluateData(getPatient());

        assertTrue(hasAlert("Rapid Saturation Drop Alert"));
    }

    @Test
    @DisplayName("does NOT trigger rapid drop when >10 minutes apart")
    void rapidDropOutsideWindow() {
        storage.addPatientData(PATIENT_ID, 97, "saturation", T1);
        storage.addPatientData(PATIENT_ID, 92, "saturation", T1 + 11 * 60_000L); // 11 min later

        alertGenerator.evaluateData(getPatient());

        assertFalse(hasAlert("rapid"));
    }

    @Test
    @DisplayName("does NOT trigger rapid drop when drop is less than 5%")
    void smallDropNoAlert() {
        storage.addPatientData(PATIENT_ID, 97, "saturation", T1);
        storage.addPatientData(PATIENT_ID, 94, "saturation", T1 + 60_000L); // only 3%

        alertGenerator.evaluateData(getPatient());

        assertFalse(hasAlert("rapid"));

    }
    @Test
    @DisplayName("triggers when saturation <92%")
    void testLowSaturationTriggersAlert() {
        storage.addPatientData(PATIENT_ID, 90, "saturation", T1);
        alertGenerator.evaluateData(getPatient());
        assertTrue(hasAlert("saturation"));

    }
    //  hypotensive hypoxemia

    @Test
    @DisplayName("triggers hypoxemia alert when systolic <90 AND saturation <92")
    void hypotensiveHypoxemia() {
        storage.addPatientData(PATIENT_ID, 85, "systolic",   T1);
        storage.addPatientData(PATIENT_ID, 88, "saturation", T1);

        alertGenerator.evaluateData(getPatient());

        assertTrue(hasAlert("hypoxemia"));
    }

    @Test
    @DisplayName("does NOT trigger hypoxemia when only systolic is low")
    void hypoxemiaOnlySystolic() {
        storage.addPatientData(PATIENT_ID, 85, "systolic",   T1);
        storage.addPatientData(PATIENT_ID, 95, "saturation", T1);

        alertGenerator.evaluateData(getPatient());

        assertFalse(hasAlert("hypoxemia"));
    }

    @Test
    @DisplayName("does NOT trigger hypoxemia when only saturation is low")
    void hypoxemiaOnlySaturation() {
        storage.addPatientData(PATIENT_ID, 110, "systolic",  T1);
        storage.addPatientData(PATIENT_ID, 88,  "saturation", T1);

        alertGenerator.evaluateData(getPatient());

        assertFalse(hasAlert("hypoxemia"));
    }

    // ECG

    @Test
    @DisplayName("triggers ECG alert when a value exceeds 1.5x the window average")
    void ecgSpikeDetected() {
        // avg = (10+10+30)/3 = 16.67, spike 30 > 16.67*1.5 = 25 → alert
        storage.addPatientData(PATIENT_ID, 10, "ECG", T1);
        storage.addPatientData(PATIENT_ID, 10, "ECG", T2);
        storage.addPatientData(PATIENT_ID, 30, "ECG", T3);

        alertGenerator.evaluateData(getPatient());

        assertTrue(hasAlert("ecg"));
    }

    @Test
    @DisplayName("does NOT trigger ECG alert for uniform readings")
    void uniformEcgNoAlert() {
        storage.addPatientData(PATIENT_ID, 10, "ECG", T1);
        storage.addPatientData(PATIENT_ID, 10, "ECG", T2);
        storage.addPatientData(PATIENT_ID, 10, "ECG", T3);

        alertGenerator.evaluateData(getPatient());

        assertFalse(hasAlert("ecg"));
    }

    // triggered alert (button press)

    @Test
    @DisplayName("triggers manual alert when Alert record value is 1.0")
    void buttonPressTriggered() {
        storage.addPatientData(PATIENT_ID, 1.0, "Alert", T1);
        alertGenerator.evaluateData(getPatient());
        assertTrue(hasAlert("triggered") || hasAlert("manual"));
    }

    @Test
    @DisplayName("does NOT trigger manual alert when Alert record value is 0.0 (resolved)")
    void buttonPressResolved() {
        storage.addPatientData(PATIENT_ID, 0.0, "Alert", T1);
        alertGenerator.evaluateData(getPatient());
        assertFalse(hasAlert("triggered") || hasAlert("manual"));
    }

    //  no alerts for normal data

    @Test
    @DisplayName("no alerts for completely normal patient data")
    void noAlertsForNormalData() {
        storage.addPatientData(PATIENT_ID, 120, "systolic",   T1);
        storage.addPatientData(PATIENT_ID, 80,  "diastolic",  T1);
        storage.addPatientData(PATIENT_ID, 98,  "saturation", T1);

        alertGenerator.evaluateData(getPatient());

        assertTrue(alertGenerator.getAlerts().isEmpty());
    }

}
