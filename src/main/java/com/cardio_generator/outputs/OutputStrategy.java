package com.cardio_generator.outputs;

/**
 * Interface for output strategy
 */
public interface OutputStrategy {
    /**
     * This method will be used to output the patient data in multiple options
     * @param patientId ID of the patient
     * @param timestamp time of the event (UNIX)
     * @param label label type of data
     * @param data data to be outputted
     */
    void output(int patientId, long timestamp, String label, String data);
}
