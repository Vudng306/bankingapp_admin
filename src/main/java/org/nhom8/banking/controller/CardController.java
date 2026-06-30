package org.nhom8.banking.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.nhom8.banking.common.ApiResponse;
import org.nhom8.banking.dto.request.CreateCardRequest;
import org.nhom8.banking.dto.request.SetCardLimitRequest;
import org.nhom8.banking.dto.response.CardResponse;
import org.nhom8.banking.security.CustomUserDetails;
import org.nhom8.banking.service.CardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    /**
     * Danh sách thẻ ảo của user.
     * GET /cards
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CardResponse>>> list(
            @AuthenticationPrincipal CustomUserDetails user) {

        return ResponseEntity.ok(
                ApiResponse.ok("Danh sách thẻ", cardService.getCards(user.getId()))
        );
    }

    /**
     * Phát hành thẻ ảo mới cho tài khoản.
     * Sinh số thẻ 16 chữ số và ngày hết hạn tự động.
     * POST /cards
     * Body: { accountId }
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CardResponse>> create(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody CreateCardRequest request) {

        CardResponse data = cardService.createCard(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Tạo thẻ ảo thành công", data));
    }

    /**
     * Khóa / Mở khóa thẻ (toggle).
     * PUT /cards/{id}/lock
     */
    @PutMapping("/{id}/lock")
    public ResponseEntity<ApiResponse<CardResponse>> toggleLock(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id) {

        CardResponse data = cardService.toggleLock(user.getId(), id);
        String msg = "LOCKED".equals(data.getStatus()) ? "Đã khóa thẻ" : "Đã mở khóa thẻ";
        return ResponseEntity.ok(ApiResponse.ok(msg, data));
    }

    /**
     * Thiết lập hạn mức giao dịch hàng ngày.
     * Gửi dailyLimit = null để bỏ hạn mức.
     * PUT /cards/{id}/limit
     * Body: { dailyLimit }
     */
    @PutMapping("/{id}/limit")
    public ResponseEntity<ApiResponse<CardResponse>> setLimit(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id,
            @Valid @RequestBody SetCardLimitRequest request) {

        CardResponse data = cardService.setLimit(user.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật hạn mức thành công", data));
    }
}
