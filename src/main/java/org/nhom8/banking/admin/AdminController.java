package org.nhom8.banking.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.nhom8.banking.common.ApiResponse;
import org.nhom8.banking.entity.*;
import org.nhom8.banking.exception.AppException;
import org.nhom8.banking.exception.ErrorCode;
import org.nhom8.banking.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/admin-api")
@RequiredArgsConstructor
public class AdminController {

    private final AdminSessionService adminSessionService;
    private final EntityManager entityManager;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationRepository notificationRepository;
    private final SavingsRepository savingsRepository;
    private final CardRepository cardRepository;
    private final DeviceRepository deviceRepository;
    private final PhoneTopupRepository phoneTopupRepository;
    private final OtpCodeRepository otpCodeRepository;
    private final TransferSessionRepository transferSessionRepository;

    // ───────────────────── Auth ─────────────────────

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@RequestBody Map<String, String> body) {
        Map<String, Object> data = adminSessionService.login(
                body.getOrDefault("username", ""),
                body.getOrDefault("password", "")
        );
        return ResponseEntity.ok(ApiResponse.ok("Đăng nhập admin thành công", data));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader(value = "X-Admin-Token", required = false) String token) {
        adminSessionService.logout(token);
        return ResponseEntity.ok(ApiResponse.ok("Đã đăng xuất admin"));
    }

    // ───────────────────── Dashboard ─────────────────────

    @GetMapping("/summary")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> summary(
            @RequestHeader(value = "X-Admin-Token", required = false) String token) {
        requireAdmin(token);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalUsers", userRepository.count());
        data.put("activeUsers", count("SELECT COUNT(u) FROM User u WHERE u.status = :status", "status", User.UserStatus.ACTIVE));
        data.put("lockedUsers", count("SELECT COUNT(u) FROM User u WHERE u.status = :status", "status", User.UserStatus.LOCKED));
        data.put("totalAccounts", accountRepository.count());
        data.put("lockedAccounts", count("SELECT COUNT(a) FROM Account a WHERE a.status = :status", "status", Account.AccountStatus.LOCKED));
        data.put("totalBalance", scalar("SELECT COALESCE(SUM(a.balance),0) FROM Account a", BigDecimal.class));
        data.put("totalTransactions", transactionRepository.count());
        data.put("successfulTransactions", count("SELECT COUNT(t) FROM Transaction t WHERE t.status = :status", "status", Transaction.TransactionStatus.SUCCESS));
        data.put("pendingTransactions", count("SELECT COUNT(t) FROM Transaction t WHERE t.status = :status", "status", Transaction.TransactionStatus.PENDING));
        data.put("failedTransactions", count("SELECT COUNT(t) FROM Transaction t WHERE t.status = :status", "status", Transaction.TransactionStatus.FAILED));
        data.put("successfulAmount", scalar("SELECT COALESCE(SUM(t.amount),0) FROM Transaction t WHERE t.status = org.nhom8.banking.entity.Transaction.TransactionStatus.SUCCESS", BigDecimal.class));
        data.put("totalSavings", savingsRepository.count());
        data.put("activeSavings", count("SELECT COUNT(s) FROM Savings s WHERE s.status = :status", "status", Savings.SavingsStatus.ACTIVE));
        data.put("totalCards", cardRepository.count());
        data.put("activeCards", count("SELECT COUNT(c) FROM Card c WHERE c.status = :status", "status", Card.CardStatus.ACTIVE));
        data.put("totalDevices", deviceRepository.count());
        data.put("activeDevices", count("SELECT COUNT(d) FROM Device d WHERE d.active = true"));
        data.put("unreadNotifications", count("SELECT COUNT(n) FROM Notification n WHERE n.read = false"));
        data.put("phoneTopups", phoneTopupRepository.count());
        data.put("recentTransactions", transactionRepository
                .findAll(PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::txMap).getContent());
        data.put("transactionTypeBreakdown", transactionTypeBreakdown());
        data.put("monthlyFlow", monthlyFlow());
        data.put("featureMatrix", featureMatrix());

        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/features")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> features(
            @RequestHeader(value = "X-Admin-Token", required = false) String token) {
        requireAdmin(token);
        return ResponseEntity.ok(ApiResponse.ok(featureMatrix()));
    }

    // ───────────────────── Users ─────────────────────

    @GetMapping("/users")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> users(
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) User.UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireAdmin(token);

        QueryParts qp = new QueryParts("User u");
        if (hasText(keyword)) {
            qp.where("(LOWER(u.fullName) LIKE :kw OR LOWER(u.email) LIKE :kw OR LOWER(u.phone) LIKE :kw)");
            qp.param("kw", like(keyword));
        }
        if (status != null) {
            qp.where("u.status = :status");
            qp.param("status", status);
        }

        List<User> list = queryEntities("SELECT u FROM " + qp.fromWhere() + " ORDER BY u.createdAt DESC", User.class, qp.params, page, size);
        long total = countQuery("SELECT COUNT(u) FROM " + qp.fromWhere(), qp.params);
        return ResponseEntity.ok(ApiResponse.ok(pageMap(list.stream().map(this::userMap).toList(), total, page, size)));
    }

    @GetMapping("/users/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> userDetail(
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @PathVariable Long id) {
        requireAdmin(token);
        User user = userRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Map<String, Object> data = new LinkedHashMap<>(userMap(user));
        List<Account> accounts = accountRepository.findByUserId(id);
        List<Long> accountIds = accounts.stream().map(Account::getId).toList();
        data.put("accounts", accounts.stream().map(this::accountMap).toList());
        data.put("cards", cardRepository.findByAccountUserIdOrderByCreatedAtDesc(id).stream().map(this::cardMap).toList());
        data.put("savings", savingsRepository.findByUserIdOrderByCreatedAtDesc(id).stream().map(this::savingsMap).toList());
        data.put("devices", deviceRepository.findByUserIdOrderByLastLoginAtDesc(id).stream().map(this::deviceMap).toList());
        data.put("notifications", notificationRepository.findByUserIdOrderByCreatedAtDesc(id, PageRequest.of(0, 10)).map(this::notificationMap).getContent());
        data.put("transactions", accountIds.isEmpty()
                ? List.of()
                : transactionRepository.findDistinctByFromAccount_IdInOrToAccount_IdIn(accountIds, accountIds,
                        PageRequest.of(0, 15, Sort.by(Sort.Direction.DESC, "createdAt"))).stream().map(this::txMap).toList());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PatchMapping("/users/{id}/status")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateUserStatus(
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        requireAdmin(token);
        User user = userRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        user.setStatus(User.UserStatus.valueOf(required(body, "status")));
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật trạng thái user thành công", userMap(user)));
    }

    // ───────────────────── Accounts ─────────────────────

    @GetMapping("/accounts")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> accounts(
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Account.AccountStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireAdmin(token);

        QueryParts qp = new QueryParts("Account a JOIN a.user u");
        if (hasText(keyword)) {
            qp.where("(LOWER(a.accountNumber) LIKE :kw OR LOWER(u.fullName) LIKE :kw OR LOWER(u.email) LIKE :kw OR LOWER(u.phone) LIKE :kw)");
            qp.param("kw", like(keyword));
        }
        if (status != null) {
            qp.where("a.status = :status");
            qp.param("status", status);
        }
        List<Account> list = queryEntities("SELECT a FROM " + qp.fromWhere() + " ORDER BY a.createdAt DESC", Account.class, qp.params, page, size);
        long total = countQuery("SELECT COUNT(a) FROM " + qp.fromWhere(), qp.params);
        return ResponseEntity.ok(ApiResponse.ok(pageMap(list.stream().map(this::accountMap).toList(), total, page, size)));
    }

    @PatchMapping("/accounts/{id}/status")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateAccountStatus(
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        requireAdmin(token);
        Account account = accountRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));
        account.setStatus(Account.AccountStatus.valueOf(required(body, "status")));
        accountRepository.save(account);
        if (account.getStatus() == Account.AccountStatus.LOCKED) {
            transactionRepository.failPendingByFromAccountId(account.getId());
        }
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật trạng thái tài khoản thành công", accountMap(account)));
    }

    @PatchMapping("/accounts/{id}/balance")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateAccountBalance(
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        requireAdmin(token);
        Account account = accountRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));
        BigDecimal balance = decimal(body.get("balance"));
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Số dư không được âm");
        }
        account.setBalance(balance);
        accountRepository.save(account);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật số dư thành công", accountMap(account)));
    }

    // ───────────────────── Transactions ─────────────────────

    @GetMapping("/transactions")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> transactions(
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Transaction.TransactionType type,
            @RequestParam(required = false) Transaction.TransactionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireAdmin(token);

        QueryParts qp = new QueryParts("Transaction t LEFT JOIN t.fromAccount fa LEFT JOIN fa.user fu LEFT JOIN t.toAccount ta LEFT JOIN ta.user tu");
        if (hasText(keyword)) {
            qp.where("(LOWER(t.referenceCode) LIKE :kw OR LOWER(t.description) LIKE :kw OR LOWER(t.toExternalAccount) LIKE :kw OR LOWER(t.toExternalAccountName) LIKE :kw OR LOWER(fa.accountNumber) LIKE :kw OR LOWER(ta.accountNumber) LIKE :kw OR LOWER(fu.fullName) LIKE :kw OR LOWER(tu.fullName) LIKE :kw)");
            qp.param("kw", like(keyword));
        }
        if (type != null) { qp.where("t.type = :type"); qp.param("type", type); }
        if (status != null) { qp.where("t.status = :status"); qp.param("status", status); }
        if (fromDate != null) { qp.where("t.createdAt >= :fromDate"); qp.param("fromDate", fromDate.atStartOfDay()); }
        if (toDate != null) { qp.where("t.createdAt < :toDate"); qp.param("toDate", toDate.plusDays(1).atStartOfDay()); }

        List<Transaction> list = queryEntities("SELECT t FROM " + qp.fromWhere() + " ORDER BY t.createdAt DESC", Transaction.class, qp.params, page, size);
        long total = countQuery("SELECT COUNT(t) FROM " + qp.fromWhere(), qp.params);
        return ResponseEntity.ok(ApiResponse.ok(pageMap(list.stream().map(this::txMap).toList(), total, page, size)));
    }

    @GetMapping("/transactions/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> transactionDetail(
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @PathVariable Long id) {
        requireAdmin(token);
        Transaction tx = transactionRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));
        return ResponseEntity.ok(ApiResponse.ok(txMap(tx)));
    }

    @PatchMapping("/transactions/{id}/status")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateTransactionStatus(
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        requireAdmin(token);
        Transaction tx = transactionRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));
        tx.setStatus(Transaction.TransactionStatus.valueOf(required(body, "status")));
        transactionRepository.save(tx);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật trạng thái giao dịch thành công", txMap(tx)));
    }

    // ───────────────────── Savings / Cards / Devices ─────────────────────

    @GetMapping("/savings")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> savings(
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Savings.SavingsStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireAdmin(token);
        QueryParts qp = new QueryParts("Savings s JOIN s.user u JOIN s.sourceAccount a");
        if (hasText(keyword)) {
            qp.where("(LOWER(u.fullName) LIKE :kw OR LOWER(u.email) LIKE :kw OR LOWER(a.accountNumber) LIKE :kw)");
            qp.param("kw", like(keyword));
        }
        if (status != null) { qp.where("s.status = :status"); qp.param("status", status); }
        List<Savings> list = queryEntities("SELECT s FROM " + qp.fromWhere() + " ORDER BY s.createdAt DESC", Savings.class, qp.params, page, size);
        long total = countQuery("SELECT COUNT(s) FROM " + qp.fromWhere(), qp.params);
        return ResponseEntity.ok(ApiResponse.ok(pageMap(list.stream().map(this::savingsMap).toList(), total, page, size)));
    }

    @PatchMapping("/savings/{id}/status")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateSavingsStatus(
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        requireAdmin(token);
        Savings savings = savingsRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.SAVINGS_NOT_FOUND));
        savings.setStatus(Savings.SavingsStatus.valueOf(required(body, "status")));
        savingsRepository.save(savings);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật trạng thái sổ tiết kiệm thành công", savingsMap(savings)));
    }

    @GetMapping("/cards")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> cards(
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Card.CardStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireAdmin(token);
        QueryParts qp = new QueryParts("Card c JOIN c.account a JOIN a.user u");
        if (hasText(keyword)) {
            qp.where("(LOWER(c.cardNumber) LIKE :kw OR LOWER(c.cardholderName) LIKE :kw OR LOWER(a.accountNumber) LIKE :kw OR LOWER(u.fullName) LIKE :kw)");
            qp.param("kw", like(keyword));
        }
        if (status != null) { qp.where("c.status = :status"); qp.param("status", status); }
        List<Card> list = queryEntities("SELECT c FROM " + qp.fromWhere() + " ORDER BY c.createdAt DESC", Card.class, qp.params, page, size);
        long total = countQuery("SELECT COUNT(c) FROM " + qp.fromWhere(), qp.params);
        return ResponseEntity.ok(ApiResponse.ok(pageMap(list.stream().map(this::cardMap).toList(), total, page, size)));
    }

    @PatchMapping("/cards/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateCard(
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        requireAdmin(token);
        Card card = cardRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.CARD_NOT_FOUND));
        if (body.containsKey("status")) card.setStatus(Card.CardStatus.valueOf(String.valueOf(body.get("status"))));
        if (body.containsKey("dailyLimit")) card.setDailyLimit(body.get("dailyLimit") == null ? null : decimal(body.get("dailyLimit")));
        cardRepository.save(card);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật thẻ thành công", cardMap(card)));
    }

    @GetMapping("/devices")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> devices(
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireAdmin(token);
        QueryParts qp = new QueryParts("Device d JOIN d.user u");
        if (hasText(keyword)) {
            qp.where("(LOWER(d.deviceName) LIKE :kw OR LOWER(d.deviceId) LIKE :kw OR LOWER(u.fullName) LIKE :kw OR LOWER(u.email) LIKE :kw)");
            qp.param("kw", like(keyword));
        }
        if (active != null) { qp.where("d.active = :active"); qp.param("active", active); }
        List<Device> list = queryEntities("SELECT d FROM " + qp.fromWhere() + " ORDER BY d.lastLoginAt DESC, d.createdAt DESC", Device.class, qp.params, page, size);
        long total = countQuery("SELECT COUNT(d) FROM " + qp.fromWhere(), qp.params);
        return ResponseEntity.ok(ApiResponse.ok(pageMap(list.stream().map(this::deviceMap).toList(), total, page, size)));
    }

    @PatchMapping("/devices/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateDevice(
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        requireAdmin(token);
        Device device = deviceRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy thiết bị"));
        if (body.containsKey("active")) device.setActive(Boolean.parseBoolean(String.valueOf(body.get("active"))));
        if (body.containsKey("biometricEnabled")) device.setBiometricEnabled(Boolean.parseBoolean(String.valueOf(body.get("biometricEnabled"))));
        deviceRepository.save(device);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật thiết bị thành công", deviceMap(device)));
    }

    // ───────────────────── Notifications / Topups / Security logs ─────────────────────

    @GetMapping("/notifications")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> notifications(
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Notification.NotificationType type,
            @RequestParam(required = false) Boolean read,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireAdmin(token);
        QueryParts qp = new QueryParts("Notification n JOIN n.user u");
        if (hasText(keyword)) {
            qp.where("(LOWER(n.title) LIKE :kw OR LOWER(n.content) LIKE :kw OR LOWER(u.fullName) LIKE :kw OR LOWER(u.email) LIKE :kw)");
            qp.param("kw", like(keyword));
        }
        if (type != null) { qp.where("n.type = :type"); qp.param("type", type); }
        if (read != null) { qp.where("n.read = :read"); qp.param("read", read); }
        List<Notification> list = queryEntities("SELECT n FROM " + qp.fromWhere() + " ORDER BY n.createdAt DESC", Notification.class, qp.params, page, size);
        long total = countQuery("SELECT COUNT(n) FROM " + qp.fromWhere(), qp.params);
        return ResponseEntity.ok(ApiResponse.ok(pageMap(list.stream().map(this::notificationMap).toList(), total, page, size)));
    }

    @PostMapping("/notifications")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> createNotification(
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @RequestBody Map<String, Object> body) {
        requireAdmin(token);
        Long userId = Long.valueOf(String.valueOf(body.get("userId")));
        User user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Notification notification = Notification.builder()
                .user(user)
                .title(String.valueOf(body.getOrDefault("title", "Thông báo từ Admin")))
                .content(String.valueOf(body.getOrDefault("content", "")))
                .type(Notification.NotificationType.valueOf(String.valueOf(body.getOrDefault("type", "SYSTEM"))))
                .read(false)
                .build();
        notificationRepository.save(notification);
        return ResponseEntity.ok(ApiResponse.ok("Tạo thông báo thành công", notificationMap(notification)));
    }

    @PatchMapping("/notifications/{id}/read")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateNotificationRead(
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        requireAdmin(token);
        Notification n = notificationRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy thông báo"));
        n.setRead(Boolean.parseBoolean(String.valueOf(body.getOrDefault("read", true))));
        notificationRepository.save(n);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật thông báo thành công", notificationMap(n)));
    }

    @GetMapping("/topups")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> topups(
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireAdmin(token);
        QueryParts qp = new QueryParts("PhoneTopup p JOIN p.transaction t LEFT JOIN t.fromAccount a LEFT JOIN a.user u");
        if (hasText(keyword)) {
            qp.where("(LOWER(p.carrier) LIKE :kw OR LOWER(p.phoneNumber) LIKE :kw OR LOWER(t.referenceCode) LIKE :kw OR LOWER(u.fullName) LIKE :kw)");
            qp.param("kw", like(keyword));
        }
        List<PhoneTopup> list = queryEntities("SELECT p FROM " + qp.fromWhere() + " ORDER BY p.createdAt DESC", PhoneTopup.class, qp.params, page, size);
        long total = countQuery("SELECT COUNT(p) FROM " + qp.fromWhere(), qp.params);
        return ResponseEntity.ok(ApiResponse.ok(pageMap(list.stream().map(this::topupMap).toList(), total, page, size)));
    }

    @GetMapping("/otps")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> otps(
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @RequestParam(required = false) OtpCode.OtpPurpose purpose,
            @RequestParam(required = false) Boolean used,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireAdmin(token);
        QueryParts qp = new QueryParts("OtpCode o JOIN o.user u");
        if (purpose != null) { qp.where("o.purpose = :purpose"); qp.param("purpose", purpose); }
        if (used != null) { qp.where("o.used = :used"); qp.param("used", used); }
        List<OtpCode> list = queryEntities("SELECT o FROM " + qp.fromWhere() + " ORDER BY o.createdAt DESC", OtpCode.class, qp.params, page, size);
        long total = countQuery("SELECT COUNT(o) FROM " + qp.fromWhere(), qp.params);
        return ResponseEntity.ok(ApiResponse.ok(pageMap(list.stream().map(this::otpMap).toList(), total, page, size)));
    }

    @GetMapping("/transfer-sessions")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> transferSessions(
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @RequestParam(required = false) Boolean used,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireAdmin(token);
        QueryParts qp = new QueryParts("TransferSession s JOIN s.user u");
        if (used != null) { qp.where("s.used = :used"); qp.param("used", used); }
        List<TransferSession> list = queryEntities("SELECT s FROM " + qp.fromWhere() + " ORDER BY s.createdAt DESC", TransferSession.class, qp.params, page, size);
        long total = countQuery("SELECT COUNT(s) FROM " + qp.fromWhere(), qp.params);
        return ResponseEntity.ok(ApiResponse.ok(pageMap(list.stream().map(this::transferSessionMap).toList(), total, page, size)));
    }

    // ───────────────────── Helpers: auth / query ─────────────────────

    private void requireAdmin(String token) {
        adminSessionService.requireValid(token);
    }

    private <T> List<T> queryEntities(String jpql, Class<T> type, Map<String, Object> params, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        TypedQuery<T> query = entityManager.createQuery(jpql, type);
        params.forEach(query::setParameter);
        return query.setFirstResult(safePage * safeSize).setMaxResults(safeSize).getResultList();
    }

    private long countQuery(String jpql, Map<String, Object> params) {
        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class);
        params.forEach(query::setParameter);
        return query.getSingleResult();
    }

    private long count(String jpql) {
        return entityManager.createQuery(jpql, Long.class).getSingleResult();
    }

    private long count(String jpql, String name, Object value) {
        return entityManager.createQuery(jpql, Long.class).setParameter(name, value).getSingleResult();
    }

    private <T> T scalar(String jpql, Class<T> type) {
        return entityManager.createQuery(jpql, type).getSingleResult();
    }

    private Map<String, Object> pageMap(List<?> content, long total, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("content", content);
        map.put("page", safePage);
        map.put("size", safeSize);
        map.put("totalElements", total);
        map.put("totalPages", (long) Math.ceil(total / (double) safeSize));
        map.put("hasNext", ((long) (safePage + 1) * safeSize) < total);
        return map;
    }

    private static class QueryParts {
        final String from;
        final List<String> where = new ArrayList<>();
        final Map<String, Object> params = new LinkedHashMap<>();
        QueryParts(String from) { this.from = from; }
        void where(String predicate) { where.add(predicate); }
        void param(String name, Object value) { params.put(name, value); }
        String fromWhere() {
            return where.isEmpty() ? from : from + " WHERE " + String.join(" AND ", where);
        }
    }

    // ───────────────────── Helpers: map entities ─────────────────────

    private Map<String, Object> userMap(User u) {
        List<Account> accounts = accountRepository.findByUserId(u.getId());
        BigDecimal totalBalance = accounts.stream().map(Account::getBalance).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return linked(
                "id", u.getId(),
                "fullName", u.getFullName(),
                "email", u.getEmail(),
                "phone", u.getPhone(),
                "avatarUrl", u.getAvatarUrl(),
                "status", enumName(u.getStatus()),
                "hasPin", u.getPinHash() != null,
                "accountCount", accounts.size(),
                "totalBalance", totalBalance,
                "createdAt", str(u.getCreatedAt()),
                "updatedAt", str(u.getUpdatedAt())
        );
    }

    private Map<String, Object> accountMap(Account a) {
        return linked(
                "id", a.getId(),
                "user", userMini(a.getUser()),
                "accountNumber", a.getAccountNumber(),
                "balance", a.getBalance(),
                "currency", a.getCurrency(),
                "accountType", enumName(a.getAccountType()),
                "status", enumName(a.getStatus()),
                "version", a.getVersion(),
                "createdAt", str(a.getCreatedAt())
        );
    }

    private Map<String, Object> txMap(Transaction t) {
        return linked(
                "id", t.getId(),
                "fromAccount", accountMini(t.getFromAccount()),
                "toAccount", accountMini(t.getToAccount()),
                "toExternalAccount", t.getToExternalAccount(),
                "toExternalAccountName", t.getToExternalAccountName(),
                "toBankCode", t.getToBankCode(),
                "amount", t.getAmount(),
                "fee", t.getFee(),
                "type", enumName(t.getType()),
                "status", enumName(t.getStatus()),
                "description", t.getDescription(),
                "referenceCode", t.getReferenceCode(),
                "createdAt", str(t.getCreatedAt())
        );
    }

    private Map<String, Object> savingsMap(Savings s) {
        return linked(
                "id", s.getId(),
                "user", userMini(s.getUser()),
                "sourceAccount", accountMini(s.getSourceAccount()),
                "principal", s.getPrincipal(),
                "interestRate", s.getInterestRate(),
                "termMonths", s.getTermMonths(),
                "startDate", str(s.getStartDate()),
                "maturityDate", str(s.getMaturityDate()),
                "accruedInterest", s.getAccruedInterest(),
                "status", enumName(s.getStatus()),
                "createdAt", str(s.getCreatedAt())
        );
    }

    private Map<String, Object> cardMap(Card c) {
        return linked(
                "id", c.getId(),
                "account", accountMini(c.getAccount()),
                "cardNumber", maskCard(c.getCardNumber()),
                "rawCardNumber", c.getCardNumber(),
                "expiryDate", str(c.getExpiryDate()),
                "cardholderName", c.getCardholderName(),
                "status", enumName(c.getStatus()),
                "dailyLimit", c.getDailyLimit(),
                "createdAt", str(c.getCreatedAt())
        );
    }

    private Map<String, Object> deviceMap(Device d) {
        return linked(
                "id", d.getId(),
                "user", userMini(d.getUser()),
                "deviceName", d.getDeviceName(),
                "deviceId", d.getDeviceId(),
                "hasPushToken", hasText(d.getPushToken()),
                "biometricEnabled", d.isBiometricEnabled(),
                "lastLoginAt", str(d.getLastLoginAt()),
                "active", d.isActive(),
                "createdAt", str(d.getCreatedAt())
        );
    }

    private Map<String, Object> notificationMap(Notification n) {
        return linked(
                "id", n.getId(),
                "user", userMini(n.getUser()),
                "title", n.getTitle(),
                "content", n.getContent(),
                "type", enumName(n.getType()),
                "read", n.isRead(),
                "createdAt", str(n.getCreatedAt())
        );
    }

    private Map<String, Object> topupMap(PhoneTopup p) {
        return linked(
                "id", p.getId(),
                "transaction", txMap(p.getTransaction()),
                "carrier", p.getCarrier(),
                "phoneNumber", p.getPhoneNumber(),
                "faceValue", p.getFaceValue(),
                "createdAt", str(p.getCreatedAt())
        );
    }

    private Map<String, Object> otpMap(OtpCode o) {
        return linked(
                "id", o.getId(),
                "user", userMini(o.getUser()),
                "code", o.getCode(),
                "purpose", enumName(o.getPurpose()),
                "channel", enumName(o.getChannel()),
                "expiresAt", str(o.getExpiresAt()),
                "used", o.isUsed(),
                "createdAt", str(o.getCreatedAt())
        );
    }

    private Map<String, Object> transferSessionMap(TransferSession s) {
        return linked(
                "id", s.getId(),
                "confirmToken", maskToken(s.getConfirmToken()),
                "user", userMini(s.getUser()),
                "transferType", s.getTransferType(),
                "fromAccountId", s.getFromAccountId(),
                "toAccountNumber", s.getToAccountNumber(),
                "toAccountName", s.getToAccountName(),
                "toBankCode", s.getToBankCode(),
                "amount", s.getAmount(),
                "description", s.getDescription(),
                "expiresAt", str(s.getExpiresAt()),
                "used", s.isUsed(),
                "createdAt", str(s.getCreatedAt())
        );
    }

    private Map<String, Object> userMini(User u) {
        if (u == null) return null;
        return linked("id", u.getId(), "fullName", u.getFullName(), "email", u.getEmail(), "phone", u.getPhone(), "status", enumName(u.getStatus()));
    }

    private Map<String, Object> accountMini(Account a) {
        if (a == null) return null;
        return linked("id", a.getId(), "accountNumber", a.getAccountNumber(), "user", userMini(a.getUser()), "status", enumName(a.getStatus()));
    }

    // ───────────────────── Helpers: reports/features ─────────────────────

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> transactionTypeBreakdown() {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT type, status, COUNT(*) AS tx_count, COALESCE(SUM(amount), 0) AS total_amount
                FROM transactions
                GROUP BY type, status
                ORDER BY type, status
                """).getResultList();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(linked("type", row[0], "status", row[1], "count", row[2], "amount", row[3]));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> monthlyFlow() {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT DATE_FORMAT(created_at, '%Y-%m') AS label,
                       COUNT(*) AS tx_count,
                       COALESCE(SUM(CASE WHEN status = 'SUCCESS' THEN amount ELSE 0 END), 0) AS success_amount,
                       COALESCE(SUM(CASE WHEN status = 'PENDING' THEN amount ELSE 0 END), 0) AS pending_amount,
                       COALESCE(SUM(CASE WHEN status = 'FAILED'  THEN amount ELSE 0 END), 0) AS failed_amount
                FROM transactions
                WHERE created_at >= DATE_SUB(CURRENT_DATE(), INTERVAL 6 MONTH)
                GROUP BY DATE_FORMAT(created_at, '%Y-%m')
                ORDER BY label
                """).getResultList();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(linked("label", row[0], "count", row[1], "successAmount", row[2], "pendingAmount", row[3], "failedAmount", row[4]));
        }
        return result;
    }

    private List<Map<String, Object>> featureMatrix() {
        return List.of(
                feature("Đăng ký / Đăng nhập / Quên mật khẩu", true, "users, otp_codes, devices"),
                feature("Dashboard số dư, giao dịch gần đây, thông báo", true, "accounts, transactions, notifications"),
                feature("Chuyển nội bộ / liên ngân hàng giả lập", true, "transactions, transfer_sessions, accounts"),
                feature("Lịch sử giao dịch, lọc, tìm kiếm, xuất CSV/Print", true, "transactions"),
                feature("Quản lý tài khoản cá nhân, đổi mật khẩu/PIN, avatar", true, "users"),
                feature("QR tài khoản / QR chuyển tiền", true, "accounts + API QR hiện có"),
                feature("Tiết kiệm kỳ hạn, lãi tự động", true, "savings, transactions"),
                feature("Push Notification", true, "devices.push_token, notifications"),
                feature("Báo cáo thu/chi", true, "transactions"),
                feature("Nạp tiền điện thoại", true, "phone_topups, transactions"),
                feature("Quản lý thẻ ảo", true, "cards, accounts"),
                feature("Đăng nhập sinh trắc học", true, "devices.biometric_enabled"),
                feature("Quản lý thiết bị đăng nhập", true, "devices"),
                feature("Chuyển tiền theo lịch", false, "DB hiện tại chưa có bảng scheduled_transfers"),
                feature("Danh bạ người nhận / Beneficiary", false, "DB hiện tại chưa có bảng beneficiaries")
        );
    }

    private Map<String, Object> feature(String name, boolean supportedByDb, String storage) {
        return linked("name", name, "supportedByDb", supportedByDb, "storage", storage);
    }

    // ───────────────────── Helpers: formatting ─────────────────────

    private static boolean hasText(String s) { return s != null && !s.isBlank(); }
    private static String like(String value) { return "%" + value.trim().toLowerCase() + "%"; }
    private static String enumName(Enum<?> e) { return e == null ? null : e.name(); }
    private static String str(Object o) { return o == null ? null : String.valueOf(o); }

    private static String required(Map<String, String> body, String key) {
        String value = body.get(key);
        if (!hasText(value)) throw new AppException(ErrorCode.VALIDATION_ERROR, "Thiếu trường: " + key);
        return value;
    }

    private static BigDecimal decimal(Object raw) {
        if (raw == null || !hasText(String.valueOf(raw))) throw new AppException(ErrorCode.VALIDATION_ERROR, "Giá trị số không hợp lệ");
        return new BigDecimal(String.valueOf(raw));
    }

    private static String maskCard(String card) {
        if (!hasText(card) || card.length() < 8) return card;
        return card.substring(0, 4) + " **** **** " + card.substring(card.length() - 4);
    }

    private static String maskOtp(String code) {
        if (!hasText(code) || code.length() <= 2) return "**";
        return "****" + code.substring(code.length() - 2);
    }

    private static String maskToken(String token) {
        if (!hasText(token) || token.length() <= 8) return "********";
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }

    private static Map<String, Object> linked(Object... args) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < args.length; i += 2) {
            map.put(String.valueOf(args[i]), args[i + 1]);
        }
        return map;
    }
}
