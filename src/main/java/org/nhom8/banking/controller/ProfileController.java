package org.nhom8.banking.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.nhom8.banking.common.ApiResponse;
import org.nhom8.banking.dto.request.ChangePasswordRequest;
import org.nhom8.banking.dto.request.UpdatePinRequest;
import org.nhom8.banking.dto.response.UserProfileResponse;
import org.nhom8.banking.security.CustomUserDetails;
import org.nhom8.banking.service.AuthService;
import org.nhom8.banking.service.ProfileService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final AuthService    authService;

    // ── GET /profile ──────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @AuthenticationPrincipal CustomUserDetails user) {

        return ResponseEntity.ok(
                ApiResponse.ok(profileService.getProfile(user.getId())));
    }

    // ── PUT /profile/password ─────────────────────────────────────────────────

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody ChangePasswordRequest request) {

        authService.changePassword(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Đổi mật khẩu thành công"));
    }

    // ── PUT /profile/pin ──────────────────────────────────────────────────────

    /**
     * Thiết lập PIN lần đầu hoặc đổi PIN.
     * - Chưa có PIN: chỉ cần newPin.
     * - Đã có PIN: bắt buộc currentPin + newPin.
     */
    @PutMapping("/pin")
    public ResponseEntity<ApiResponse<Void>> updatePin(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody UpdatePinRequest request) {

        profileService.updatePin(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật PIN thành công"));
    }

    // ── POST /profile/avatar ──────────────────────────────────────────────────

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadAvatar(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam("file") MultipartFile file) {

        String url = profileService.uploadAvatar(user.getId(), file);
        return ResponseEntity.ok(
                ApiResponse.ok("Cập nhật ảnh đại diện thành công",
                        Map.of("avatarUrl", url)));
    }
}
