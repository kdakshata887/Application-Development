package com.examly.springapp.service;

import com.examly.springapp.model.BiometricDevice;

import java.util.List;

public interface BiometricDeviceService {
    BiometricDevice createDevice(BiometricDevice device);
    List<BiometricDevice> getAllDevices();
    BiometricDevice getDeviceById(Long id);
    BiometricDevice updateDevice(Long id, BiometricDevice device);
    void deleteDevice(Long id);
}
