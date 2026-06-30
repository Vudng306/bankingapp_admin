package org.nhom8.banking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginRequest {

    /** Email hoặc số điện thoại */
    @NotBlank(message = "Email hoặc số điện thoại không được để trống")
    private String credential;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;

    // ── Device info (tùy chọn) ────────────────────────────────────────────────
    // App Android gửi kèm để ghi nhận thiết bị ngay tại login.
    // Nếu thiếu, vẫn login bình thường — client gọi POST /devices riêng sau đó.

    /** Android: Settings.Secure.ANDROID_ID */
    @Size(max = 255)
    private String deviceId;

    @Size(max = 100)
    private String deviceName;

    /** FCM registration token — nullable nếu chưa cấp quyền thông báo */
    @Size(max = 255)
    private String pushToken;
}
