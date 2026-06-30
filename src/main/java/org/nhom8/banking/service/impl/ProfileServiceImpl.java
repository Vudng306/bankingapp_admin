package org.nhom8.banking.service.impl;

import lombok.RequiredArgsConstructor;
import org.nhom8.banking.dto.request.UpdatePinRequest;
import org.nhom8.banking.dto.response.AccountSummaryResponse;
import org.nhom8.banking.dto.response.UserProfileResponse;
import org.nhom8.banking.entity.Account;
import org.nhom8.banking.entity.User;
import org.nhom8.banking.exception.AppException;
import org.nhom8.banking.exception.ErrorCode;
import org.nhom8.banking.repository.AccountRepository;
import org.nhom8.banking.repository.UserRepository;
import org.nhom8.banking.service.ProfileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png");
    private static final long MAX_BYTES = 2L * 1024 * 1024; // 2 MB

    private final UserRepository    userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder   passwordEncoder;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    // ── Profile ───────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        User user = findById(userId);
        List<AccountSummaryResponse> accounts = accountRepository.findByUserId(userId)
                .stream().map(this::toAccountSummary).toList();

        return UserProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus().name())
                .createdAt(user.getCreatedAt())
                .accounts(accounts)
                .build();
    }

    // ── PIN ───────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void updatePin(Long userId, UpdatePinRequest request) {
        User user = findById(userId);

        if (user.getPinHash() != null) {
            // Đã có PIN → bắt buộc xác minh PIN cũ
            if (!StringUtils.hasText(request.getCurrentPin())) {
                throw new AppException(ErrorCode.INVALID_PIN, "Vui lòng nhập PIN hiện tại");
            }
            if (!passwordEncoder.matches(request.getCurrentPin(), user.getPinHash())) {
                throw new AppException(ErrorCode.INVALID_PIN);
            }
        }

        user.setPinHash(passwordEncoder.encode(request.getNewPin()));
        userRepository.save(user);
    }

    // ── Avatar ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public String uploadAvatar(Long userId, MultipartFile file) {
        validateImage(file);

        User user = findById(userId);
        String filename  = userId + "_" + System.currentTimeMillis() + resolveExt(file);
        Path   avatarDir = Paths.get(uploadDir, "avatars");

        try {
            Files.createDirectories(avatarDir);
            Files.copy(file.getInputStream(), avatarDir.resolve(filename),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, "Không thể lưu ảnh đại diện");
        }

        String url = "/uploads/avatars/" + filename;
        user.setAvatarUrl(url);
        userRepository.save(user);
        return url;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "File không được để trống");
        }
        String ct = file.getContentType();
        if (ct == null || !ALLOWED_TYPES.contains(ct)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                    "Chỉ chấp nhận ảnh JPEG hoặc PNG");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                    "Kích thước ảnh không được vượt quá 2 MB");
        }
    }

    private String resolveExt(MultipartFile file) {
        return switch (file.getContentType()) {
            case "image/png"  -> ".png";
            default           -> ".jpg";
        };
    }

    private AccountSummaryResponse toAccountSummary(Account a) {
        return AccountSummaryResponse.builder()
                .id(a.getId())
                .accountNumber(a.getAccountNumber())
                .balance(a.getBalance())
                .currency(a.getCurrency())
                .accountType(a.getAccountType().name())
                .status(a.getStatus().name())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
