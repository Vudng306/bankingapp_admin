package org.nhom8.banking.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DeviceResponse {

    private Long id;
    private String deviceId;
    private String deviceName;
    /** true nếu push token đã được đăng ký */
    private boolean pushEnabled;
    private boolean biometricEnabled;
    private boolean active;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}
