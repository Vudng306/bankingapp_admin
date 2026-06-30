package org.nhom8.banking.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.nhom8.banking.common.ApiResponse;
import org.nhom8.banking.dto.request.RegisterDeviceRequest;
import org.nhom8.banking.dto.response.DeviceResponse;
import org.nhom8.banking.security.CustomUserDetails;
import org.nhom8.banking.service.DeviceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    /**
     * Đăng ký hoặc cập nhật thiết bị sau khi đăng nhập.
     * App Android gọi ngay sau khi nhận JWT, truyền FCM token mới nhất.
     *
     * POST /devices
     * Body: { "deviceId": "...", "deviceName": "Samsung S24", "pushToken": "fcm-token..." }
     */
    @PostMapping
    public ResponseEntity<ApiResponse<DeviceResponse>> register(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody RegisterDeviceRequest request) {

        DeviceResponse data = deviceService.registerOrUpdate(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Thiết bị đã được đăng ký", data));
    }

    /**
     * Danh sách thiết bị đang đăng ký (kể cả đã deactivate).
     *
     * GET /devices
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<DeviceResponse>>> list(
            @AuthenticationPrincipal CustomUserDetails user) {

        return ResponseEntity.ok(ApiResponse.ok(deviceService.list(user.getId())));
    }

    /**
     * Vô hiệu hóa thiết bị — xóa push token, không nhận notification nữa.
     * Gọi khi user đăng xuất trên thiết bị đó.
     *
     * DELETE /devices/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> unregister(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id) {

        deviceService.unregister(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Thiết bị đã được hủy đăng ký"));
    }
}
