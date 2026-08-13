package com.company.ruanzhu.common.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String[] passwords = {"test123456", "admin123", "123456"};

        System.out.println("=== BCrypt Password Hashes ===");
        for (String password : passwords) {
            String hash = encoder.encode(password);
            System.out.println("Password: " + password);
            System.out.println("Hash: " + hash);
            System.out.println("Verify: " + encoder.matches(password, hash));
            System.out.println();
        }
    }
}
