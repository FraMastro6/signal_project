package com.data_management;

import java.io.*;
import java.nio.file.*;
import java.util.stream.Stream;


public class FileDataReader implements DataReader {

    private final String filePath;

    public FileDataReader(String filePath) {
        this.filePath = filePath;
    }

    /**
     * Read Data from files in a directory.
     * Rejects malformed lines.
     * Splits the data in its components and store them in dataStorage
     * @param dataStorage the storage where data will be stored
     * @throws IOException
     */
    @Override
    public void readData(DataStorage dataStorage) throws IOException {

        Files.list(Paths.get(filePath))
                .filter(Files::isRegularFile)
                .forEach(file -> {

                    try (Stream<String> lines = Files.lines(file)) {

                        lines.forEach(line -> {

                            try {
                                // skip malformed or header-like lines
                                if (!line.contains("Patient ID")) return;

                                int patientId = Integer.parseInt(
                                        line.split("Patient ID:")[1].split(",")[0].trim()
                                );

                                long timestamp = Long.parseLong(
                                        line.split("Timestamp:")[1].split(",")[0].trim()
                                );

                                String label = line.split("Label:")[1].split(",")[0].trim();

                                double value = Double.parseDouble(
                                        line.split("Data:")[1].trim()
                                );

                                dataStorage.addPatientData(
                                        patientId,
                                        value,
                                        label,
                                        timestamp
                                );

                            } catch (Exception e) {
                                System.out.println("Skipping invalid line: " + line);
                            }
                        });

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}