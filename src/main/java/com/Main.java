package com;

import com.cardio_generator.HealthDataSimulator;
import com.data_management.DataStorage;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("DataStorage")) {
            DataStorage.main(new String[]{});
        } else {
            try {
                HealthDataSimulator.main(new String[]{});
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
