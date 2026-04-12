package com.cardio_generator.generators;

import com.cardio_generator.outputs.OutputStrategy;

/**
 * Interface for generating patient-related data.
 */
public interface PatientDataGenerator {
    /**
     * Generates data for a specific patient and sends it using the given output strategy.
     * @param patientId ID of the patient
     * @param outputStrategy output strategy
     */
    void generate(int patientId, OutputStrategy outputStrategy);
}
