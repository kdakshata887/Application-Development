package com.examly.springapp.service.impl;

import com.examly.springapp.service.BiometricAttendanceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Mock biometric attendance provider — simulates device responses locally.
 * No hardware or vendor SDK is required.
 * Active when app.biometric.provider=mock (the default).
 *
 * NOTE (REQUIRES EXTERNAL INTEGRATION):
 * To integrate real biometric hardware, create a DeviceBiometricAttendanceProvider
 * implementing BiometricAttendanceProvider with the vendor SDK,
 * and set app.biometric.provider=device.
 */
@Component
@ConditionalOnProperty(name = "app.biometric.provider", havingValue = "mock", matchIfMissing = true)
@Slf4j
public class MockBiometricAttendanceProvider implements BiometricAttendanceProvider {

    @Override
    public String syncDevice(String deviceId) {
        log.info("[MOCK BIOMETRIC] Sync requested for device: {}", deviceId);
        return "MOCK_SYNC_OK: Device " + deviceId + " synced successfully (0 buffered events)";
    }

    @Override
    public String getDeviceHealth(String deviceId) {
        log.info("[MOCK BIOMETRIC] Health check for device: {}", deviceId);
        return "ONLINE";
    }
}
