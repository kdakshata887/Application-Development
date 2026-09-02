package com.examly.springapp.service;

/**
 * Provider interface for biometric attendance integration.
 *
 * Implementations:
 *   - MockBiometricAttendanceProvider — simulates biometric events locally
 *   - DeviceBiometricAttendanceProvider — real device SDK integration (REQUIRES EXTERNAL SETUP)
 *
 * Configure via: app.biometric.provider=mock|device
 *
 * NOTE: DeviceBiometricAttendanceProvider is NOT implemented here because it
 * requires a vendor-specific SDK and hardware. A clean integration point exists
 * so a real implementation can be plugged in without changing other code.
 */
public interface BiometricAttendanceProvider {

    /**
     * Synchronise offline/buffered events from a biometric device.
     *
     * @param deviceId the unique ID of the biometric device
     * @return a status message describing the sync result
     */
    String syncDevice(String deviceId);

    /**
     * Get the health status of a biometric device.
     *
     * @param deviceId the unique ID of the biometric device
     * @return a status string: ONLINE, OFFLINE, ERROR
     */
    String getDeviceHealth(String deviceId);
}
