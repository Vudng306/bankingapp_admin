-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Jun 29, 2026 at 07:40 PM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.0.30

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `banking_db`
--

-- --------------------------------------------------------

--
-- Table structure for table `accounts`
--

CREATE TABLE `accounts` (
  `id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `account_number` varchar(20) NOT NULL,
  `balance` decimal(15,2) NOT NULL DEFAULT 0.00,
  `currency` varchar(3) NOT NULL DEFAULT 'VND',
  `account_type` enum('PAYMENT','SAVINGS') NOT NULL DEFAULT 'PAYMENT',
  `status` enum('ACTIVE','LOCKED') NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `version` bigint(20) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `accounts`
--

INSERT INTO `accounts` (`id`, `user_id`, `account_number`, `balance`, `currency`, `account_type`, `status`, `created_at`, `version`) VALUES
(1, 1, '9704001000000001', 49770000.00, 'VND', 'PAYMENT', 'ACTIVE', '2026-06-28 11:13:54', 5),
(2, 2, '9704001000000002', 30190000.00, 'VND', 'PAYMENT', 'ACTIVE', '2026-06-28 11:13:54', 6),
(3, 3, '9704001402310466', 58990000.00, 'VND', 'PAYMENT', 'ACTIVE', '2026-06-28 17:11:01', 6),
(4, 4, '9704001304820868', 0.00, 'VND', 'PAYMENT', 'ACTIVE', '2026-06-29 14:23:57', 0),
(5, 6, '9704001805473643', 4880000.00, 'VND', 'PAYMENT', 'ACTIVE', '2026-06-29 22:12:50', 3);

-- --------------------------------------------------------

--
-- Table structure for table `cards`
--

CREATE TABLE `cards` (
  `id` bigint(20) NOT NULL,
  `account_id` bigint(20) NOT NULL,
  `card_number` varchar(20) NOT NULL,
  `expiry_date` date NOT NULL,
  `cardholder_name` varchar(100) NOT NULL,
  `status` enum('ACTIVE','LOCKED') NOT NULL DEFAULT 'ACTIVE',
  `daily_limit` decimal(15,2) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `cards`
--

INSERT INTO `cards` (`id`, `account_id`, `card_number`, `expiry_date`, `cardholder_name`, `status`, `daily_limit`, `created_at`) VALUES
(1, 5, '4925845069469082', '2029-06-29', 'TEST DEVICE', 'ACTIVE', NULL, '2026-06-29 22:15:08'),
(2, 5, '4676477829236186', '2029-06-29', 'TEST DEVICE', 'ACTIVE', NULL, '2026-06-29 22:31:30'),
(3, 5, '4527446226776361', '2029-06-29', 'TEST DEVICE', 'ACTIVE', NULL, '2026-06-29 22:31:31'),
(4, 5, '4044800606845288', '2029-06-29', 'TEST DEVICE', 'ACTIVE', NULL, '2026-06-29 22:31:31'),
(5, 5, '4962604388578621', '2029-06-29', 'TEST DEVICE', 'ACTIVE', NULL, '2026-06-29 22:31:32');

-- --------------------------------------------------------

--
-- Table structure for table `devices`
--

CREATE TABLE `devices` (
  `id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `device_name` varchar(100) DEFAULT NULL,
  `device_id` varchar(255) DEFAULT NULL,
  `push_token` varchar(255) DEFAULT NULL,
  `biometric_enabled` tinyint(1) NOT NULL DEFAULT 0,
  `last_login_at` datetime DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `devices`
--

INSERT INTO `devices` (`id`, `user_id`, `device_name`, `device_id`, `push_token`, `biometric_enabled`, `last_login_at`, `is_active`, `created_at`) VALUES
(1, 4, 'Pixel 8 Test', 'test-pixel-001', 'fcm-test-token', 0, '2026-06-29 14:24:26', 1, '2026-06-29 14:24:26'),
(2, 6, 'Test Phone', 'dev-test-abc', 'fcm-abc', 0, '2026-06-29 22:32:31', 1, '2026-06-29 22:13:11'),
(3, 6, 'Laptop', 'dev-laptop-xyz', NULL, 0, '2026-06-29 22:18:06', 0, '2026-06-29 22:18:06');

-- --------------------------------------------------------

--
-- Table structure for table `notifications`
--

CREATE TABLE `notifications` (
  `id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `title` varchar(150) NOT NULL,
  `content` text DEFAULT NULL,
  `type` enum('TRANSACTION','BALANCE','SYSTEM') NOT NULL DEFAULT 'SYSTEM',
  `is_read` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `notifications`
--

INSERT INTO `notifications` (`id`, `user_id`, `title`, `content`, `type`, `is_read`, `created_at`) VALUES
(1, 1, 'Chuyển tiền thành công', 'Bạn đã chuyển 500,000 VND đến TK 9704001000000002', 'TRANSACTION', 1, '2026-06-28 11:13:54'),
(2, 1, 'Chào mừng đến Banking App', 'Tài khoản của bạn đã được kích hoạt thành công.', 'SYSTEM', 1, '2026-06-28 11:13:54'),
(3, 2, 'Nhận tiền thành công', 'Tài khoản của bạn vừa nhận 500,000 VND từ Nguyen Van An', 'TRANSACTION', 0, '2026-06-28 11:13:54'),
(4, 1, 'Chuyển tiền thành công', 'Bạn đã chuyển 100,000 VND đến TK 9704001000000002. Mã GD: TXN2026062882608122', 'TRANSACTION', 1, '2026-06-28 11:20:40'),
(5, 2, 'Nhận tiền thành công', 'Tài khoản của bạn vừa nhận 100,000 VND từ TK 9704001000000001. Mã GD: TXN2026062882608122', 'TRANSACTION', 0, '2026-06-28 11:20:40'),
(6, 1, 'Chuyển tiền thành công', 'Bạn đã chuyển 50,000 VND đến TK 9704001000000002. Mã GD: TXN2026062830221753', 'TRANSACTION', 1, '2026-06-28 12:32:06'),
(7, 2, 'Nhận tiền thành công', 'Tài khoản của bạn vừa nhận 50,000 VND từ TK 9704001000000001. Mã GD: TXN2026062830221753', 'TRANSACTION', 0, '2026-06-28 12:32:06'),
(8, 1, 'Lệnh chuyển tiền đang xử lý', 'Đang xử lý chuyển 100,000 VND đến TK 0123456789 (Nguyen Van B) tại Vietcombank. Mã GD: TXN2026062855826765', 'TRANSACTION', 1, '2026-06-28 12:32:31'),
(9, 1, 'Chuyển tiền liên ngân hàng thành công', 'Giao dịch TXN2026062855826765: đã chuyển 100,000 VND đến TK 0123456789 (Nguyen Van B) tại VCB.', 'TRANSACTION', 1, '2026-06-28 12:32:39'),
(10, 3, 'Chuyển tiền thành công', '-10,000 VND → TK 9704001000000002. Số dư: 90,000 VND. Mã GD: TXN2026062885837772', 'TRANSACTION', 1, '2026-06-28 20:15:48'),
(11, 2, 'Nhận tiền thành công', '+10,000 VND từ TK 9704001402310466. Số dư: 30,160,000 VND. Mã GD: TXN2026062885837772', 'TRANSACTION', 0, '2026-06-28 20:15:48'),
(12, 3, 'Chuyển tiền thành công', '-10,000 VND → TK 9704001000000002. Số dư: 80,000 VND. Mã GD: TXN2026062818985651', 'TRANSACTION', 1, '2026-06-28 21:58:01'),
(13, 2, 'Nhận tiền thành công', '+10,000 VND từ TK 9704001402310466. Số dư: 30,170,000 VND. Mã GD: TXN2026062818985651', 'TRANSACTION', 0, '2026-06-28 21:58:01'),
(14, 3, 'Chuyển tiền thành công', '-10,000 VND → TK 9704001000000002. Số dư: 70,000 VND. Mã GD: TXN2026062880469595', 'TRANSACTION', 1, '2026-06-28 23:00:44'),
(15, 2, 'Nhận tiền thành công', '+10,000 VND từ TK 9704001402310466. Số dư: 30,180,000 VND. Mã GD: TXN2026062880469595', 'TRANSACTION', 0, '2026-06-28 23:00:44'),
(16, 3, 'Chuyển tiền thành công', '-10,000 VND → TK 9704001000000002. Số dư: 60,000 VND. Mã GD: TXN2026062895862397', 'TRANSACTION', 1, '2026-06-28 23:07:55'),
(17, 2, 'Nhận tiền thành công', '+10,000 VND từ TK 9704001402310466. Số dư: 30,190,000 VND. Mã GD: TXN2026062895862397', 'TRANSACTION', 0, '2026-06-28 23:07:55'),
(18, 3, 'Mở sổ tiết kiệm thành công', '-1,000,000 VND. Kỳ hạn 3 tháng (4.50%/năm), đáo hạn 2026-09-28, nhận dự kiến 1,011,250 VND. Số dư TK: 59,000,000 VND. Mã: SAV2026062817830857', 'TRANSACTION', 1, '2026-06-28 23:10:45'),
(19, 3, 'Chuyển tiền thành công', '-10,000 VND → TK 9704001000000001. Số dư: 58,990,000 VND. Mã GD: TXN2026062953726667', 'TRANSACTION', 1, '2026-06-29 12:48:09'),
(20, 1, 'Nhận tiền thành công', '+10,000 VND từ TK 9704001402310466. Số dư: 49,760,000 VND. Mã GD: TXN2026062953726667', 'TRANSACTION', 0, '2026-06-29 12:48:09'),
(21, 6, 'Nạp tiền điện thoại thành công', '-50.000đ → 0987654321 (Viettel). Số dư: 4.950.000đ. Mã GD: TOP202606299621892', 'TRANSACTION', 0, '2026-06-29 22:17:54'),
(22, 6, 'Nạp tiền điện thoại thành công', '-10.000đ → 0912345678 (MobiFone). Số dư: 4.890.000đ. Mã GD: TOP202606292170770', 'TRANSACTION', 0, '2026-06-29 22:30:54'),
(23, 6, 'Chuyển tiền thành công', '-10,000 VND → TK 9704001000000001. Số dư: 4,880,000 VND. Mã GD: TXN2026062948544733', 'TRANSACTION', 0, '2026-06-29 22:32:32'),
(24, 1, 'Nhận tiền thành công', '+10,000 VND từ TK 9704001805473643. Số dư: 49,770,000 VND. Mã GD: TXN2026062948544733', 'TRANSACTION', 0, '2026-06-29 22:32:32');

-- --------------------------------------------------------

--
-- Table structure for table `otp_codes`
--

CREATE TABLE `otp_codes` (
  `id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `code` varchar(10) NOT NULL,
  `purpose` enum('REGISTER','LOGIN','TRANSFER','RESET_PASSWORD') NOT NULL,
  `channel` enum('SMS','EMAIL') NOT NULL,
  `expires_at` datetime NOT NULL,
  `is_used` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `otp_codes`
--

INSERT INTO `otp_codes` (`id`, `user_id`, `code`, `purpose`, `channel`, `expires_at`, `is_used`, `created_at`) VALUES
(1, 1, '932142', 'TRANSFER', 'EMAIL', '2026-06-28 11:22:32', 1, '2026-06-28 11:17:32'),
(2, 1, '704852', 'TRANSFER', 'EMAIL', '2026-06-28 11:22:43', 1, '2026-06-28 11:17:43'),
(3, 1, '798493', 'TRANSFER', 'EMAIL', '2026-06-28 11:25:40', 1, '2026-06-28 11:20:40'),
(4, 1, '417714', 'TRANSFER', 'EMAIL', '2026-06-28 12:35:13', 1, '2026-06-28 12:30:13'),
(5, 1, '419199', 'TRANSFER', 'EMAIL', '2026-06-28 12:36:48', 1, '2026-06-28 12:31:48'),
(6, 1, '910211', 'TRANSFER', 'EMAIL', '2026-06-28 12:37:31', 1, '2026-06-28 12:32:31'),
(7, 1, '152074', 'REGISTER', 'EMAIL', '2026-06-28 12:38:21', 0, '2026-06-28 12:33:21'),
(8, 3, '416489', 'REGISTER', 'EMAIL', '2026-06-28 17:15:55', 1, '2026-06-28 17:10:55'),
(9, 3, '922368', 'TRANSFER', 'EMAIL', '2026-06-28 20:20:40', 1, '2026-06-28 20:15:40'),
(10, 3, '692136', 'TRANSFER', 'EMAIL', '2026-06-28 22:02:54', 1, '2026-06-28 21:57:54'),
(11, 3, '135681', 'TRANSFER', 'EMAIL', '2026-06-28 23:05:41', 1, '2026-06-28 23:00:41'),
(12, 3, '436437', 'TRANSFER', 'EMAIL', '2026-06-28 23:12:51', 1, '2026-06-28 23:07:51'),
(13, 3, '974927', 'TRANSFER', 'EMAIL', '2026-06-29 12:53:05', 1, '2026-06-29 12:48:05'),
(14, 4, '845727', 'REGISTER', 'EMAIL', '2026-06-29 14:28:48', 1, '2026-06-29 14:23:48'),
(15, 5, '523685', 'REGISTER', 'EMAIL', '2026-06-29 22:14:56', 0, '2026-06-29 22:09:56'),
(16, 6, '323037', 'REGISTER', 'EMAIL', '2026-06-29 22:17:50', 1, '2026-06-29 22:12:50'),
(17, 6, '988891', 'TRANSFER', 'EMAIL', '2026-06-29 22:22:30', 1, '2026-06-29 22:17:30'),
(18, 6, '043467', 'TRANSFER', 'EMAIL', '2026-06-29 22:35:44', 1, '2026-06-29 22:30:44'),
(19, 6, '547100', 'TRANSFER', 'EMAIL', '2026-06-29 22:37:31', 1, '2026-06-29 22:32:31');

-- --------------------------------------------------------

--
-- Table structure for table `phone_topups`
--

CREATE TABLE `phone_topups` (
  `id` bigint(20) NOT NULL,
  `transaction_id` bigint(20) NOT NULL,
  `carrier` varchar(20) NOT NULL,
  `phone_number` varchar(15) NOT NULL,
  `face_value` decimal(15,2) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `phone_topups`
--

INSERT INTO `phone_topups` (`id`, `transaction_id`, `carrier`, `phone_number`, `face_value`, `created_at`) VALUES
(1, 13, 'VIETTEL', '0987654321', 50000.00, '2026-06-29 22:17:54'),
(2, 14, 'MOBIFONE', '0912345678', 10000.00, '2026-06-29 22:30:53');

-- --------------------------------------------------------

--
-- Table structure for table `savings`
--

CREATE TABLE `savings` (
  `id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `source_account_id` bigint(20) NOT NULL,
  `principal` decimal(15,2) NOT NULL,
  `interest_rate` decimal(5,2) NOT NULL,
  `term_months` int(11) NOT NULL,
  `start_date` date NOT NULL,
  `maturity_date` date NOT NULL,
  `accrued_interest` decimal(15,2) NOT NULL DEFAULT 0.00,
  `status` enum('ACTIVE','MATURED','WITHDRAWN') NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `savings`
--

INSERT INTO `savings` (`id`, `user_id`, `source_account_id`, `principal`, `interest_rate`, `term_months`, `start_date`, `maturity_date`, `accrued_interest`, `status`, `created_at`) VALUES
(1, 1, 1, 10000000.00, 7.50, 12, '2026-01-01', '2027-01-01', 2054.79, 'ACTIVE', '2026-06-28 11:13:54'),
(2, 3, 3, 1000000.00, 4.50, 3, '2026-06-28', '2026-09-28', 123.29, 'ACTIVE', '2026-06-28 23:10:45');

-- --------------------------------------------------------

--
-- Table structure for table `transactions`
--

CREATE TABLE `transactions` (
  `id` bigint(20) NOT NULL,
  `from_account_id` bigint(20) DEFAULT NULL,
  `to_account_id` bigint(20) DEFAULT NULL,
  `to_external_account` varchar(20) DEFAULT NULL,
  `to_external_account_name` varchar(100) DEFAULT NULL,
  `to_bank_code` varchar(20) DEFAULT NULL,
  `amount` decimal(15,2) NOT NULL,
  `fee` decimal(15,2) NOT NULL DEFAULT 0.00,
  `type` enum('INTERNAL','INTERBANK','TOPUP','SAVINGS_DEPOSIT','SAVINGS_WITHDRAW') NOT NULL,
  `status` enum('PENDING','SUCCESS','FAILED') NOT NULL DEFAULT 'PENDING',
  `description` varchar(255) DEFAULT NULL,
  `reference_code` varchar(30) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `transactions`
--

INSERT INTO `transactions` (`id`, `from_account_id`, `to_account_id`, `to_external_account`, `to_external_account_name`, `to_bank_code`, `amount`, `fee`, `type`, `status`, `description`, `reference_code`, `created_at`) VALUES
(1, 1, 2, NULL, NULL, NULL, 500000.00, 0.00, 'INTERNAL', 'SUCCESS', 'Chia tien an trua', 'REF20260101000001', '2026-06-28 11:13:54'),
(2, 1, 2, NULL, NULL, NULL, 1000000.00, 0.00, 'INTERNAL', 'SUCCESS', 'Tien mua do', 'REF20260115000002', '2026-06-28 11:13:54'),
(3, 2, 1, NULL, NULL, NULL, 200000.00, 0.00, 'INTERNAL', 'SUCCESS', 'Hoan tien taxi', 'REF20260120000003', '2026-06-28 11:13:54'),
(4, 1, 2, NULL, NULL, NULL, 100000.00, 0.00, 'INTERNAL', 'SUCCESS', NULL, 'TXN2026062882608122', '2026-06-28 11:20:40'),
(5, 1, 2, NULL, NULL, NULL, 50000.00, 0.00, 'INTERNAL', 'SUCCESS', 'Test transfer', 'TXN2026062830221753', '2026-06-28 12:32:06'),
(6, 1, NULL, '0123456789', 'Nguyen Van B', 'VCB', 100000.00, 0.00, 'INTERBANK', 'SUCCESS', 'Chuyen tien test | CK den Nguyen Van B - Vietcombank (0123456789)', 'TXN2026062855826765', '2026-06-28 12:32:31'),
(7, 3, 2, NULL, NULL, NULL, 10000.00, 0.00, 'INTERNAL', 'SUCCESS', 'LE ANH TUAN chuyen tien', 'TXN2026062885837772', '2026-06-28 20:15:48'),
(8, 3, 2, NULL, NULL, NULL, 10000.00, 0.00, 'INTERNAL', 'SUCCESS', 'LE ANH TUAN chuyen tien', 'TXN2026062818985651', '2026-06-28 21:58:01'),
(9, 3, 2, NULL, NULL, NULL, 10000.00, 0.00, 'INTERNAL', 'SUCCESS', 'LE ANH TUAN chuyen tien', 'TXN2026062880469595', '2026-06-28 23:00:44'),
(10, 3, 2, NULL, NULL, NULL, 10000.00, 0.00, 'INTERNAL', 'SUCCESS', 'LE ANH TUAN chuyen tien', 'TXN2026062895862397', '2026-06-28 23:07:55'),
(11, 3, NULL, NULL, NULL, NULL, 1000000.00, 0.00, 'SAVINGS_DEPOSIT', 'SUCCESS', 'Gửi tiết kiệm 3 tháng - lãi suất 4.50%/năm', 'SAV2026062817830857', '2026-06-28 23:10:45'),
(12, 3, 1, NULL, NULL, NULL, 10000.00, 0.00, 'INTERNAL', 'SUCCESS', 'LE ANH TUAN chuyen tien', 'TXN2026062953726667', '2026-06-29 12:48:09'),
(13, 5, NULL, '0987654321', 'Viettel', NULL, 50000.00, 0.00, 'TOPUP', 'SUCCESS', 'Nạp 50.000đ → 0987654321 (Viettel)', 'TOP202606299621892', '2026-06-29 22:17:54'),
(14, 5, NULL, '0912345678', 'MobiFone', NULL, 10000.00, 0.00, 'TOPUP', 'SUCCESS', 'Nạp 10.000đ → 0912345678 (MobiFone)', 'TOP202606292170770', '2026-06-29 22:30:53'),
(15, 5, 1, NULL, NULL, NULL, 10000.00, 0.00, 'INTERNAL', 'SUCCESS', 'Regression test', 'TXN2026062948544733', '2026-06-29 22:32:31');

-- --------------------------------------------------------

--
-- Table structure for table `transfer_sessions`
--

CREATE TABLE `transfer_sessions` (
  `id` bigint(20) NOT NULL,
  `confirm_token` varchar(36) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `transfer_type` varchar(20) NOT NULL,
  `from_account_id` bigint(20) NOT NULL,
  `to_account_number` varchar(20) NOT NULL,
  `to_account_name` varchar(100) DEFAULT NULL,
  `to_bank_code` varchar(20) DEFAULT NULL,
  `amount` decimal(15,2) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `expires_at` datetime NOT NULL,
  `used` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `id` bigint(20) NOT NULL,
  `full_name` varchar(100) NOT NULL,
  `email` varchar(150) NOT NULL,
  `phone` varchar(15) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `pin_hash` varchar(255) DEFAULT NULL,
  `avatar_url` varchar(255) DEFAULT NULL,
  `status` enum('ACTIVE','LOCKED') NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`id`, `full_name`, `email`, `phone`, `password_hash`, `pin_hash`, `avatar_url`, `status`, `created_at`, `updated_at`) VALUES
(1, 'Nguyen Van An', 'an@banking.com', '0912345678', '$2a$10$zpn4ier9QvCG//quHpRl.eenSrW5sxofj4p5.Hi37189XmphsNROa', '$2a$10$gWGceeGbKK0keLtDS8LhB.eDUs7z0ZpSoIEo1p/RyrELCZXfjm9x2', NULL, 'ACTIVE', '2026-06-28 11:13:54', '2026-06-28 12:28:23'),
(2, 'Tran Thi Bich', 'bich@banking.com', '0987654321', '$2a$10$mVOaLe5SC6LgTGZd3MOU4.1KtaWx1/do7GlUD/HnXSUUxg7lCd7cu', '$2a$10$jMH09zIhhPshlfXmZX98aeJ9ilu0FTnfCvmLPoB8g1frNaiQSekkq', NULL, 'ACTIVE', '2026-06-28 11:13:54', '2026-06-28 11:13:54'),
(3, 'Lê Anh Tuấn', 'leanhtuan7126@gmail.com', '0904482178', '$2a$10$KJE6NysdIA4T7DBSz2bTXOfgYfmJPAxm1J//6YhReyq4zzkajhvLO', '$2a$10$oTrCYV/06y63tFefTCUGSuIr5HsP7dq4Knn9.ZZloXJDwkydriojC', '/uploads/avatars/3_1782662813464.jpg', 'ACTIVE', '2026-06-28 17:10:55', '2026-06-28 23:06:53'),
(4, 'Test User', 'test_device@example.com', '0901234568', '$2a$10$nPtVhofCeVrEyWPsx4rmU.2AFqDYEHATeLmIA7xbYmyqCakPJR.2O', NULL, NULL, 'ACTIVE', '2026-06-29 14:23:47', '2026-06-29 14:23:57'),
(5, 'Tuan Le', 'tuanle1782745796@example.com', '0912422547', '$2a$10$0R749dQu4K2iyilbRToA3uEVsii.FS1ccd4pTec0BBJLMGmxTl.r.', NULL, NULL, 'LOCKED', '2026-06-29 22:09:56', '2026-06-29 22:09:56'),
(6, 'Test Device', 'test1782745970@example.com', '0912745970', '$2a$10$C0KQebUZM9WhPI8lcwm0IeksvoFVYcdFzzHlSAzTWcUZlZHJyE6UO', '$2a$10$z7RaZ2GH2WkbGIZezA.4..Wiwafcqm3zqNV1O6EQnafP.ZGgCjYii', NULL, 'ACTIVE', '2026-06-29 22:12:50', '2026-06-29 22:17:30');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `accounts`
--
ALTER TABLE `accounts`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `account_number` (`account_number`),
  ADD KEY `idx_accounts_user` (`user_id`);

--
-- Indexes for table `cards`
--
ALTER TABLE `cards`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `card_number` (`card_number`),
  ADD KEY `idx_cards_account` (`account_id`);

--
-- Indexes for table `devices`
--
ALTER TABLE `devices`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_devices_user` (`user_id`);

--
-- Indexes for table `notifications`
--
ALTER TABLE `notifications`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_notif_user` (`user_id`);

--
-- Indexes for table `otp_codes`
--
ALTER TABLE `otp_codes`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_otp_user` (`user_id`);

--
-- Indexes for table `phone_topups`
--
ALTER TABLE `phone_topups`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `transaction_id` (`transaction_id`),
  ADD KEY `idx_topup_tx` (`transaction_id`);

--
-- Indexes for table `savings`
--
ALTER TABLE `savings`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_savings_account` (`source_account_id`),
  ADD KEY `idx_savings_user` (`user_id`);

--
-- Indexes for table `transactions`
--
ALTER TABLE `transactions`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `reference_code` (`reference_code`),
  ADD KEY `idx_tx_from` (`from_account_id`),
  ADD KEY `idx_tx_to` (`to_account_id`),
  ADD KEY `idx_tx_created` (`created_at`),
  ADD KEY `idx_tx_type` (`type`);

--
-- Indexes for table `transfer_sessions`
--
ALTER TABLE `transfer_sessions`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_confirm_token` (`confirm_token`),
  ADD KEY `fk_ts_user` (`user_id`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `email` (`email`),
  ADD UNIQUE KEY `phone` (`phone`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `accounts`
--
ALTER TABLE `accounts`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT for table `cards`
--
ALTER TABLE `cards`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT for table `devices`
--
ALTER TABLE `devices`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `notifications`
--
ALTER TABLE `notifications`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=25;

--
-- AUTO_INCREMENT for table `otp_codes`
--
ALTER TABLE `otp_codes`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=20;

--
-- AUTO_INCREMENT for table `phone_topups`
--
ALTER TABLE `phone_topups`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `savings`
--
ALTER TABLE `savings`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `transactions`
--
ALTER TABLE `transactions`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=16;

--
-- AUTO_INCREMENT for table `transfer_sessions`
--
ALTER TABLE `transfer_sessions`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=15;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `accounts`
--
ALTER TABLE `accounts`
  ADD CONSTRAINT `fk_accounts_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `cards`
--
ALTER TABLE `cards`
  ADD CONSTRAINT `fk_cards_account` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `devices`
--
ALTER TABLE `devices`
  ADD CONSTRAINT `fk_devices_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `notifications`
--
ALTER TABLE `notifications`
  ADD CONSTRAINT `fk_notif_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `otp_codes`
--
ALTER TABLE `otp_codes`
  ADD CONSTRAINT `fk_otp_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `phone_topups`
--
ALTER TABLE `phone_topups`
  ADD CONSTRAINT `fk_topup_tx` FOREIGN KEY (`transaction_id`) REFERENCES `transactions` (`id`);

--
-- Constraints for table `savings`
--
ALTER TABLE `savings`
  ADD CONSTRAINT `fk_savings_account` FOREIGN KEY (`source_account_id`) REFERENCES `accounts` (`id`),
  ADD CONSTRAINT `fk_savings_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `transactions`
--
ALTER TABLE `transactions`
  ADD CONSTRAINT `fk_tx_from` FOREIGN KEY (`from_account_id`) REFERENCES `accounts` (`id`),
  ADD CONSTRAINT `fk_tx_to` FOREIGN KEY (`to_account_id`) REFERENCES `accounts` (`id`);

--
-- Constraints for table `transfer_sessions`
--
ALTER TABLE `transfer_sessions`
  ADD CONSTRAINT `fk_ts_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
