package org.nhom8.banking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterDeviceRequest {

    /** ID duy nhất của thiết bị — dùng để upsert (Android: Settings.Secure.ANDROID_ID) */
    @NotBlank(message = "deviceId là bắt buộc")
    @Size(max = 255)
    private String deviceId;

    @Size(max = 100)
    private String deviceName;

    /**
     * FCM registration token — lấy từ FirebaseMessaging.getInstance().getToken() trên Android.
     * Nullable: user có thể đăng ký thiết bị mà chưa cấp quyền thông báo.
     */
    @Size(max = 255)
    private String pushToken;
}
