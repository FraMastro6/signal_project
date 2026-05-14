package data_management;

import com.alerts.AlertGenerator;
import com.data_management.DataStorage;
import com.data_management.Patient;
import com.data_management.PatientRecord;
import com.data_management.WebSocketClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class WebSocketTest {

    private DataStorage storage;


    @BeforeEach
    void setUp() {
        storage = DataStorage.getInstance();
        storage.clearAllData();
    }
    private boolean hasAlert(AlertGenerator alertGenerator, String conditionSubstring) {
        return alertGenerator.getAlerts().stream()
                .anyMatch(a -> a.getCondition().toLowerCase()
                        .contains(conditionSubstring.toLowerCase()));
    }
    private void parseAndStore(String message, DataStorage ds) {
        if (message == null || message.isBlank())
            throw new IllegalArgumentException("Empty message");

        String[] parts = message.split(",", 4);
        if (parts.length != 4)
            throw new IllegalArgumentException("Expected 4 fields, got " + parts.length);

        int    patientId = Integer.parseInt(parts[0].trim());
        long   timestamp = Long.parseLong(parts[1].trim());
        String label     = parts[2].trim();
        double value     = Double.parseDouble(parts[3].trim());

        if (label.isEmpty())
            throw new IllegalArgumentException("Label is empty");

        ds.addPatientData(patientId, value, label, timestamp);
    }

    @Test
    @DisplayName("parses a valid HeartRate message and stores it correctly")
    void validHeartRateMessage() {
        parseAndStore("1,1700000500000,HeartRate,75.0", storage);

        List<PatientRecord> records = storage.getRecords(1, 0, Long.MAX_VALUE);
        assertEquals(1, records.size());
        PatientRecord r = records.get(0);
        assertEquals(1, r.getPatientId());
        assertEquals("HeartRate", r.getRecordType());
        assertEquals(75.0, r.getMeasurementValue(), 0.001);
        assertEquals(1700000500000L, r.getTimestamp());
    }

    @Test
    @DisplayName("parses a valid BloodPressure message")
    void validBloodPressureMessage() {
        parseAndStore("2,1700000600000,systolic,120.0", storage);

        List<PatientRecord> records = storage.getRecords(2, 0, Long.MAX_VALUE);
        assertEquals(1, records.size());
        assertEquals("systolic", records.get(0).getRecordType());
        assertEquals(120.0, records.get(0).getMeasurementValue(), 0.001);
    }

    @Test
    @DisplayName("parses integer value field correctly")
    void integerValueField() {
        parseAndStore("3,1700000700000,saturation,98", storage);

        assertEquals(98.0,
                storage.getRecords(3, 0, Long.MAX_VALUE).get(0).getMeasurementValue(), 0.001);
    }

    @Test
    @DisplayName("multiple messages for same patient accumulate correctly")
    void multipleMessagesSamePatient() {
        parseAndStore("1,1700000500000,HeartRate,75.0", storage);
        parseAndStore("1,1700000600000,HeartRate,80.0", storage);
        parseAndStore("1,1700000700000,HeartRate,78.0", storage);

        assertEquals(3, storage.getRecords(1, 0, Long.MAX_VALUE).size());
    }

    @Test
    @DisplayName("messages for different patients are stored separately")
    void differentPatientsStoredSeparately() {
        parseAndStore("1,1700000500000,HeartRate,75.0", storage);
        parseAndStore("2,1700000500000,HeartRate,88.0", storage);

        assertEquals(1, storage.getRecords(1, 0, Long.MAX_VALUE).size());
        assertEquals(1, storage.getRecords(2, 0, Long.MAX_VALUE).size());
    }

    @Test
    @DisplayName("parses triggered alert message (value=1.0)")
    void triggeredAlertMessage() {
        parseAndStore("5,1700000500000,Alert,1.0", storage);

        PatientRecord r = storage.getRecords(5, 0, Long.MAX_VALUE).get(0);
        assertEquals("Alert", r.getRecordType());
        assertEquals(1.0, r.getMeasurementValue(), 0.001);
    }

    @Test
    @DisplayName("stored data triggers alert evaluation correctly")
    void dataFlowTriggersAlerts() {
        // simulate receiving a critical reading via WebSocket
        parseAndStore("1,1700000500000,systolic,185.0", storage);

        AlertGenerator alertGenerator = new AlertGenerator(storage);
        Patient patient = storage.getAllPatients().get(0);
        alertGenerator.evaluateData(patient);

        assertFalse(alertGenerator.getAlerts().isEmpty(),
                "Critical systolic reading should trigger an alert");
    }
    @Test
    @DisplayName("readData throws IOException when server is not running")
    void connectionRefused() {
        // port 19999 has nothing listening on it
        WebSocketClient client = new WebSocketClient("ws://localhost:19999");
        assertThrows(IOException.class, () -> client.readData(storage));
    }
    @Test
    @DisplayName("readData throws IOException for an invalid URI")
    void invalidUri() {
        WebSocketClient bad = new WebSocketClient("not a valid uri!!!");
        assertThrows(IOException.class, () -> bad.readData(storage));
    }

    @Test
    @DisplayName("readData throws IOException for wrong protocol")
    void wrongProtocol() {
        WebSocketClient bad = new WebSocketClient("http://localhost:8080");
        assertThrows(IOException.class, () -> bad.readData(storage));
    }

    @Test
    @DisplayName("concurrent messages do not corrupt storage")
    void concurrentMessagesDoNotCorruptStorage() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int id = i;
            new Thread(() -> {
                parseAndStore("1,170000050000" + id + ",HeartRate," + (70.0 + id), storage);
                latch.countDown();
            }).start();
        }

        latch.await(5, TimeUnit.SECONDS);
        // all 10 records should be stored without corruption
        assertEquals(10, storage.getRecords(1, 0, Long.MAX_VALUE).size());
    }

    @Test
    @DisplayName("duplicate message is not stored twice")
    void duplicateMessageNotStored() {
        parseAndStore("1,1700000500000,HeartRate,75.0", storage);
        parseAndStore("1,1700000500000,HeartRate,75.0", storage); // same message

        assertEquals(1, storage.getRecords(1, 0, Long.MAX_VALUE).size());
    }

    //testServer for complete test of the WebSocketClient class
    @Test
    @DisplayName("critical data received via real connection triggers alert")
    void criticalDataTriggersAlertViaRealConnection() throws Exception {
        int port = 18084;
        CountDownLatch serverReady = new CountDownLatch(1);
        CountDownLatch messageSent = new CountDownLatch(1);

        org.java_websocket.server.WebSocketServer testServer =
                new org.java_websocket.server.WebSocketServer(
                        new java.net.InetSocketAddress(port)) {

                    @Override public void onOpen(org.java_websocket.WebSocket conn,
                                                 org.java_websocket.handshake.ClientHandshake h) {
                        conn.send("1,1700000500000,systolic,185.0"); // critical threshold
                        messageSent.countDown();
                    }
                    @Override public void onClose(org.java_websocket.WebSocket c,
                                                  int code, String reason, boolean remote) {}
                    @Override public void onMessage(org.java_websocket.WebSocket c,
                                                    String msg) {}
                    @Override public void onError(org.java_websocket.WebSocket c,
                                                  Exception e) {}
                    @Override public void onStart() { serverReady.countDown(); }
                };
        testServer.setReuseAddr(true);
        testServer.start();
        serverReady.await(3, TimeUnit.SECONDS);

        WebSocketClient client = new WebSocketClient("ws://localhost:" + port);
        client.readData(storage);

        messageSent.await(3, TimeUnit.SECONDS);
        Thread.sleep(300);

        testServer.stop();

        assertFalse(storage.getAllPatients().isEmpty());
        AlertGenerator alertGenerator = new AlertGenerator(storage);
        alertGenerator.evaluateData(storage.getAllPatients().get(0));
        assertFalse(alertGenerator.getAlerts().isEmpty(), "Critical systolic should trigger alert");
        assertTrue(hasAlert(alertGenerator, "systolic"));
    }

    @Test
    @DisplayName("malformed message via real connection is skipped, no data stored")
    void malformedMessageSkippedViaRealConnection() throws Exception {
        int port = 18085;
        CountDownLatch serverReady = new CountDownLatch(1);
        CountDownLatch messageSent = new CountDownLatch(1);

        org.java_websocket.server.WebSocketServer testServer =
                new org.java_websocket.server.WebSocketServer(
                        new java.net.InetSocketAddress(port)) {

                    @Override public void onOpen(org.java_websocket.WebSocket conn,
                                                 org.java_websocket.handshake.ClientHandshake h) {
                        conn.send("BlaBlaBla");
                        conn.send("string to skip");
                        conn.send("1,notATimestamp,HeartRate,75.0");
                        messageSent.countDown();
                    }
                    @Override public void onClose(org.java_websocket.WebSocket c,
                                                  int code, String reason, boolean remote) {}
                    @Override public void onMessage(org.java_websocket.WebSocket c,
                                                    String msg) {}
                    @Override public void onError(org.java_websocket.WebSocket c,
                                                  Exception e) {}
                    @Override public void onStart() { serverReady.countDown(); }
                };

        testServer.setReuseAddr(true);
        testServer.start();
        serverReady.await(3, TimeUnit.SECONDS);

        WebSocketClient client = new WebSocketClient("ws://localhost:" + port);
        client.readData(storage);

        messageSent.await(3, TimeUnit.SECONDS);
        Thread.sleep(300);

        testServer.stop();

        assertTrue(storage.getAllPatients().isEmpty());
    }

}
