package org.nhom8.banking.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nhom8.banking.dto.request.QrGenerateRequest;
import org.nhom8.banking.dto.response.QrResponse;
import org.nhom8.banking.entity.Account;
import org.nhom8.banking.entity.User;
import org.nhom8.banking.exception.AppException;
import org.nhom8.banking.exception.ErrorCode;
import org.nhom8.banking.repository.AccountRepository;
import org.nhom8.banking.service.QrService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class QrServiceImpl implements QrService {

    private static final String VIETQR_API_URL = "https://api.vietqr.io/v2/generate";

    private static final RestClient REST_CLIENT = RestClient.create();

    private final AccountRepository accountRepository;

    @Value("${app.qr.bank-bin}")
    private String bankBin;

    @Value("${app.qr.bank-name}")
    private String bankName;

    @Value("${app.qr.city:Ha Noi}")
    private String bankCity;

    @Value("${app.vietqr.client-id:}")
    private String vietQrClientId;

    @Value("${app.vietqr.api-key:}")
    private String vietQrApiKey;

    @Value("${app.vietqr.template:compact}")
    private String vietQrTemplate;

    // ── Public API ────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public QrResponse generate(Long userId, QrGenerateRequest request) {
        Account account = accountRepository.findByIdAndUserId(request.getAccountId(), userId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));

        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new AppException(ErrorCode.ACCOUNT_LOCKED);
        }

        User user = account.getUser();
        String description = (request.getDescription() != null && !request.getDescription().isBlank())
                ? truncate(request.getDescription().trim(), 25)
                : null;

        // Primary: gọi VietQR API nếu đã cấu hình credentials
        if (!vietQrClientId.isBlank() && !vietQrApiKey.isBlank()) {
            try {
                return generateViaVietQrApi(account, user, request.getAmount(), description);
            } catch (Exception e) {
                log.warn("VietQR API lỗi, dùng fallback EMVCo: {}", e.getMessage());
            }
        }

        // Fallback: tự sinh QR theo chuẩn EMVCo
        return generateLocally(account, user, request.getAmount(), description);
    }

    // ── VietQR API ────────────────────────────────────────────────────────────

    private QrResponse generateViaVietQrApi(Account account, User user,
                                            BigDecimal amount, String description) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("accountNo", account.getAccountNumber());
        body.put("acqId", Integer.parseInt(bankBin));
        body.put("accountName", normalizeForVietQr(user.getFullName(), 50));
        if (amount != null) {
            body.put("amount", amount.longValue());
        }
        if (description != null) {
            body.put("addInfo", normalizeForVietQr(description, 25));
        }
        body.put("template", vietQrTemplate);

        JsonNode resp = REST_CLIENT.post()
                .uri(VIETQR_API_URL)
                .header("x-client-id", vietQrClientId)
                .header("x-api-key", vietQrApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        if (resp == null || !"00".equals(resp.path("code").asText())) {
            String desc = resp != null ? resp.path("desc").asText("unknown") : "null response";
            throw new RuntimeException("VietQR trả lỗi: " + desc);
        }

        JsonNode data = resp.path("data");
        String qrCode    = data.path("qrCode").asText();
        String qrDataURL = data.path("qrDataURL").asText(null);
        if (qrDataURL != null && qrDataURL.isBlank()) qrDataURL = null;

        return QrResponse.builder()
                .qrContent(qrCode)
                .qrDataURL(qrDataURL)
                .accountNumber(account.getAccountNumber())
                .accountName(user.getFullName())
                .bankBin(bankBin)
                .bankName(bankName)
                .amount(amount)
                .description(description)
                .build();
    }

    // ── EMVCo fallback ────────────────────────────────────────────────────────

    private QrResponse generateLocally(Account account, User user,
                                       BigDecimal amount, String description) {
        String merchantName = truncate(user.getFullName(), 25);
        String qrContent = buildQrContent(account.getAccountNumber(), merchantName, amount, description);

        return QrResponse.builder()
                .qrContent(qrContent)
                .accountNumber(account.getAccountNumber())
                .accountName(user.getFullName())
                .bankBin(bankBin)
                .bankName(bankName)
                .amount(amount)
                .description(description)
                .build();
    }

    /**
     * Sinh chuỗi QR theo chuẩn VietQR (EMVCo Merchant Presented QR).
     *
     * Cấu trúc:
     *   00 – Payload Format Indicator = "01"
     *   01 – Point of Initiation: "11" (static) | "12" (dynamic, có amount)
     *   38 – Merchant Account Info (VietQR/Napas)
     *        └─ 00: GUID = "A000000727"
     *           01: Napas
     *               └─ 00: Bank BIN (6 chữ số)
     *                  01: Số tài khoản
     *   52 – Merchant Category Code = "0000"
     *   53 – Transaction Currency = "704" (VND)
     *   54 – Transaction Amount (nếu có)
     *   58 – Country Code = "VN"
     *   59 – Merchant Name (≤ 25 ký tự)
     *   60 – Merchant City
     *   62 – Additional Data Field (description ở subtag 08)
     *   63 – CRC-16/CCITT-FALSE (4 hex digits)
     */
    private String buildQrContent(String accountNumber, String merchantName,
                                   BigDecimal amount, String description) {
        String napasInner      = tlv("00", bankBin) + tlv("01", accountNumber);
        String merchantAccInfo = tlv("00", "A000000727") + tlv("01", napasInner);
        String tag38           = tlv("38", merchantAccInfo);

        String tag54 = (amount != null)
                ? tlv("54", amount.toBigInteger().toString())
                : "";

        String tag62 = (description != null)
                ? tlv("62", tlv("08", description))
                : "";

        String body = tlv("00", "01")
                + tlv("01", amount != null ? "12" : "11")
                + tag38
                + tlv("52", "0000")
                + tlv("53", "704")
                + tag54
                + tlv("58", "VN")
                + tlv("59", merchantName)
                + tlv("60", bankCity)
                + tag62
                + "6304";

        return body + crc16(body);
    }

    private static String tlv(String tag, String value) {
        return tag + String.format("%02d", value.length()) + value;
    }

    private static String crc16(String data) {
        int crc = 0xFFFF;
        for (byte b : data.getBytes(StandardCharsets.UTF_8)) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                crc = ((crc & 0x8000) != 0) ? (crc << 1) ^ 0x1021 : crc << 1;
                crc &= 0xFFFF;
            }
        }
        return String.format("%04X", crc);
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    /**
     * Chuẩn hoá chuỗi cho VietQR API: bỏ dấu, chỉ giữ a-z/A-Z/0-9/space, in hoa.
     * VietQR yêu cầu accountName: 5–50 ký tự; addInfo: ≤25 ký tự.
     */
    private static String normalizeForVietQr(String text, int maxLen) {
        String result = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[^a-zA-Z0-9 ]", "")
                .trim()
                .replaceAll("\\s+", " ")
                .toUpperCase();
        return result.length() > maxLen ? result.substring(0, maxLen).trim() : result;
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
