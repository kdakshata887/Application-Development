package com.examly.springapp.controller;

import com.examly.springapp.model.BiometricDevice;
import com.examly.springapp.service.BiometricDeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class BiometricDeviceController {

    private final BiometricDeviceService biometricDeviceService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL')")
    public ResponseEntity<BiometricDevice> create(@RequestBody BiometricDevice device) {
        return ResponseEntity.status(HttpStatus.CREATED).body(biometricDeviceService.createDevice(device));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL')")
    public ResponseEntity<List<BiometricDevice>> getAll() {
        return ResponseEntity.ok(biometricDeviceService.getAllDevices());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL')")
    public ResponseEntity<BiometricDevice> getById(@PathVariable Long id) {
        return ResponseEntity.ok(biometricDeviceService.getDeviceById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL')")
    public ResponseEntity<BiometricDevice> update(@PathVariable Long id, @RequestBody BiometricDevice device) {
        return ResponseEntity.ok(biometricDeviceService.updateDevice(id, device));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        biometricDeviceService.deleteDevice(id);
        return ResponseEntity.noContent().build();
    }
}
