package com.examly.springapp.service;

import com.examly.springapp.exception.InvalidNameException;
import com.examly.springapp.exception.InvalidPhoneException;

public final class ValidationUtil {

    private ValidationUtil() {}

    public static void validateName(String name) {
        if (name == null || !name.matches("^[A-Za-z ]+$")) {
            throw new InvalidNameException("Name must not contain special characters or numbers");
        }
    }

    public static void validatePhone(String phone) {
        if (phone == null || !phone.matches("^\\d{10}$")) {
            throw new InvalidPhoneException("Phone Number must be exactly 10 digits long");
        }
    }

    public static void validatePeriod(Integer period) {
        if (period == null || period < 1 || period > 8) {
            throw new IllegalArgumentException("Period must be between 1 and 8");
        }
    }
}
