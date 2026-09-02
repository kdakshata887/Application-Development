package com.examly.springapp.service.impl;

import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.BiometricDevice;
import com.examly.springapp.repository.BiometricDeviceRepository;
import com.examly.springapp.service.BiometricDeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BiometricDeviceServiceImpl implements BiometricDeviceService {

    private final BiometricDeviceRepository biometricDeviceRepository;

    @Override
    public BiometricDevice createDevice(BiometricDevice device) {
        return biometricDeviceRepository.save(device);
    }

    @Override
    public List<BiometricDevice> getAllDevices() {
        return biometricDeviceRepository.findAll();
    }

    @Override
    public BiometricDevice getDeviceById(Long id) {
        return biometricDeviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Biometric device not found with id: " + id));
    }

    @Override
    public BiometricDevice updateDevice(Long id, BiometricDevice update) {
        BiometricDevice device = getDeviceById(id);
        if (update.getDeviceName() != null) device.setDeviceName(update.getDeviceName());
        if (update.getLocation() != null) device.setLocation(update.getLocation());
        if (update.getDeviceType() != null) device.setDeviceType(update.getDeviceType());
        if (update.getLastSync() != null) device.setLastSync(update.getLastSync());
        if (update.getBatteryLevel() != null) device.setBatteryLevel(update.getBatteryLevel());
        if (update.getStatus() != null) device.setStatus(update.getStatus());
        return biometricDeviceRepository.save(device);
    }

    @Override
    public void deleteDevice(Long id) {
        biometricDeviceRepository.delete(getDeviceById(id));
    }
}
