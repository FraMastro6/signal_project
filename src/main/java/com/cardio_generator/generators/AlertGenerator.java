package com.cardio_generator.generators;

import java.util.Random;

import com.cardio_generator.outputs.OutputStrategy;

//Added javadoc

/**
 * Generates alert events for patients.
 */
public class AlertGenerator implements PatientDataGenerator {
    //changed constant name to UPPER_SNAKE_CASE and private to encapsulation
    private static final Random RANDOM_GENERATOR = new Random();
    //changed variable name to camelCase
    private boolean[] alertStates; // false = resolved, true = pressed

    /**
     *  Constructor for AlertGenerator
     *  Index 0 unused to allow direct access via 1-based patientId
     *
     * @param patientCount Number of patients
     */
    public AlertGenerator(int patientCount) {
        alertStates = new boolean[patientCount + 1];
    }

    /**
     * Checks the current alert state for the patient and decides what to do next.
     * If an alert is active it may resolve it, otherwise it may randomly trigger a new alert.
     * @param patientId ID of the parient
     * @param outputStrategy the strategy used to output generated alert events
     */
    @Override
    public void generate(int patientId, OutputStrategy outputStrategy) {
        try {
            if (alertStates[patientId]) {
                if (RANDOM_GENERATOR.nextDouble() < 0.9) { // 90% chance to resolve
                    alertStates[patientId] = false;
                    // Output the alert
                    outputStrategy.output(patientId, System.currentTimeMillis(), "Alert", "resolved");
                }
            } else {
                //changed variable name to camelCase (maybe a better name is needed)
                double lambda = 0.1; // Average rate (alerts per period), adjust based on desired frequency
                double p = -Math.expm1(-lambda); // Probability of at least one alert in the period
                boolean alertTriggered = RANDOM_GENERATOR.nextDouble() < p;

                if (alertTriggered) {
                    alertStates[patientId] = true;
                    // Output the alert
                    outputStrategy.output(patientId, System.currentTimeMillis(), "Alert", "triggered");
                }
            }
        } catch (Exception e) {
            System.err.println("An error occurred while generating alert data for patient " + patientId);
            e.printStackTrace();
        }
    }
}
