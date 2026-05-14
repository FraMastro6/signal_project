package com.data_management;

import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

/**
 * A WebSocket client that connects to a WebSocket server and receives
 * real-time patient data, parsing and storing it in {@link DataStorage}.
 */
public class WebSocketClient implements DataReader{
    private final String server;

    public WebSocketClient(String server) {
        this.server = server;
    }


    @Override
    public void readData(DataStorage dataStorage) throws IOException {
        try{
            URI uri = new URI(server);
            InnerClient client = new InnerClient(uri, dataStorage);
            boolean connected = client.connectBlocking(3, TimeUnit.SECONDS);
            if (!connected) {
                throw new IOException("Could not connect to WebSocket server: " + server);
            }
        }
        catch (URISyntaxException e){
            throw new IOException("Invalid WebSocket URI: " + server, e);
        } catch (InterruptedException e) {
            throw new IOException("Connection interrupted while connecting", e);
        }

    }


    private static class InnerClient extends org.java_websocket.client.WebSocketClient {
        private final DataStorage dataStorage;

        InnerClient(URI serverUri, DataStorage dataStorage) {
            super(serverUri);
            this.dataStorage = dataStorage;
        }

        /**
         * Called once when the WebSocket connection is established.
         */
        @Override
        public void onOpen(ServerHandshake handshake) {
            System.out.println("WebSocket connected to: " + getURI()
                    + " (status: " + handshake.getHttpStatus() + ")");
        }

        /**
         * Called every time the server sends a message.
         *
         * Malformed messages are logged and skipped.
         */
        @Override
        public void onMessage(String message) {
            try {
                parseAndStore(message);
            } catch (Exception e) {
                System.err.println("Skipping malformed message: '"
                        + message + "' — " + e.getMessage());
            }
        }

        /**
         * Called when the connection is closed (by either side).
         *
         * @param code   WebSocket close code
         * @param reason close reason
         * @param remote true if the server initiated the close
         */
        @Override
        public void onClose(int code, String reason, boolean remote) {
            System.out.println("WebSocket closed."
                    + " Code: " + code
                    + ", Reason: " + (reason.isEmpty() ? "none" : reason)
                    + ", Closed by: " + (remote ? "server" : "client"));

//            // Abnormal closure — attempt reconnection
//            if (code != 1000) {
//                System.out.println("Abnormal closure detected. Attempting reconnect...");
//                reconnect();
//            }
        }

        /**
         * Called on any WebSocket-level error.
         * Logs the error, onClose will be called immediately after.
         */
        @Override
        public void onError(Exception e) {
            System.err.println("WebSocket error: " + e.getMessage());
        }

        /**
         * Parses one message line and stores the record in DataStorage.
         *
         * Format: "patientId,timestamp,label,value"
         *
         * @param message raw message string from the server
         * @throws IllegalArgumentException if the format is wrong
         * @throws NumberFormatException    if numeric fields cannot be parsed
         */
        private void parseAndStore(String message) {
            if (message == null || message.isBlank()) {
                throw new IllegalArgumentException("Empty message");
            }

            String[] parts = message.split(",", 4);
            if (parts.length != 4) {
                throw new IllegalArgumentException("Expected 4 comma-separated fields, got " + parts.length);
            }

            int    patientId = Integer.parseInt(parts[0].trim());
            long   timestamp = Long.parseLong(parts[1].trim());
            String label     = parts[2].trim();
            double value     = Double.parseDouble(parts[3].trim());

            if (label.isEmpty()) {
                throw new IllegalArgumentException("Label field is empty");
            }

            dataStorage.addPatientData(patientId, value, label, timestamp);
        }


    }
}
