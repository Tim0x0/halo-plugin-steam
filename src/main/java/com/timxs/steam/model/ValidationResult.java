package com.timxs.steam.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API 验证结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {
    
    private boolean success;
    private int statusCode;
    private String message;
    
    public static ValidationResult success() {
        return new ValidationResult(true, 200, null);
    }
    
    public static ValidationResult error(int statusCode, String message) {
        return new ValidationResult(false, statusCode, message);
    }
}
