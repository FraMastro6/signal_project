package com.data_management;

import java.io.IOException;

//TODO Real DataReader implementation
public class MockDataReader implements DataReader{

    @Override
    public void readData(DataStorage dataStorage) throws IOException {
        System.out.println("fake loading data");
        dataStorage.addPatientData(1, 75.0, "HeartRate", 1700000500000L);
        dataStorage.addPatientData(1, 120.0, "BloodPressure", 1700000600000L);

        dataStorage.addPatientData(2, 98.0, "Saturation", 1700000700000L);
    }
}
