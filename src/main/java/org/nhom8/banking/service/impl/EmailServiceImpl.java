package org.nhom8.banking.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nhom8.banking.entity.OtpCode;
import org.nhom8.banking.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name:Banking App}")
    private String fromName;

    // ── Public API ────────────────────────────────────────────────────────────

    @Override
    @Async("emailExecutor")
    public void sendOtpEmail(String toEmail, String toName,
                              String otpCode, OtpCode.OtpPurpose purpose,
                              int expiryMinutes) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject(subject(purpose));
            helper.setText(htmlBody(toName, otpCode, purpose, expiryMinutes), true);

            mailSender.send(mime);
            log.info("[EMAIL] OTP sent → {} (purpose={})", toEmail, purpose);

        } catch (MailException | MessagingException | java.io.UnsupportedEncodingException e) {
            // Không ném exception — lỗi gửi mail không nên rollback transaction OTP
            log.error("[EMAIL] Failed to send OTP to {} : {}", toEmail, e.getMessage());
        }
    }

    // ── Builders ──────────────────────────────────────────────────────────────

    private String subject(OtpCode.OtpPurpose purpose) {
        return switch (purpose) {
            case REGISTER       -> "[Banking App] Xác thực đăng ký tài khoản";
            case RESET_PASSWORD -> "[Banking App] Đặt lại mật khẩu";
            case LOGIN          -> "[Banking App] Xác thực đăng nhập";
            case TRANSFER       -> "[Banking App] Xác thực giao dịch chuyển tiền";
        };
    }

    private String purposeIntro(OtpCode.OtpPurpose purpose) {
        return switch (purpose) {
            case REGISTER       -> "Bạn vừa đăng ký tài khoản Banking App. Vui lòng dùng mã OTP dưới đây để xác thực.";
            case RESET_PASSWORD -> "Chúng tôi nhận được yêu cầu đặt lại mật khẩu của bạn. Dùng mã OTP dưới đây để tiếp tục.";
            case LOGIN          -> "Yêu cầu đăng nhập từ thiết bị mới. Vui lòng xác thực bằng mã OTP dưới đây.";
            case TRANSFER       -> "Xác thực giao dịch chuyển tiền của bạn bằng mã OTP dưới đây.";
        };
    }

    private String htmlBody(String name, String otp,
                             OtpCode.OtpPurpose purpose, int expiryMinutes) {
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
                <body style="margin:0;padding:0;background:#f0f2f5;font-family:'Segoe UI',Arial,sans-serif">
                <table width="100%%" cellpadding="0" cellspacing="0">
                  <tr><td align="center" style="padding:40px 16px">
                    <table width="560" cellpadding="0" cellspacing="0"
                           style="background:#ffffff;border-radius:12px;overflow:hidden;
                                  box-shadow:0 4px 16px rgba(0,0,0,.10)">

                      <!-- Header -->
                      <tr>
                        <td style="background:linear-gradient(135deg,#1565C0,#1976D2);
                                   padding:32px;text-align:center">
                          <p style="margin:0;font-size:28px;font-weight:800;color:#fff;
                                    letter-spacing:1px">🏦 Banking App</p>
                        </td>
                      </tr>

                      <!-- Body -->
                      <tr>
                        <td style="padding:36px 40px">
                          <p style="margin:0 0 12px;font-size:16px;color:#222">
                            Xin chào <strong>%s</strong>,
                          </p>
                          <p style="margin:0 0 28px;font-size:15px;color:#555;line-height:1.6">
                            %s
                          </p>

                          <!-- OTP box -->
                          <table width="100%%" cellpadding="0" cellspacing="0">
                            <tr>
                              <td align="center"
                                  style="background:#EEF2FF;border:2px dashed #3F51B5;
                                         border-radius:10px;padding:28px 20px">
                                <p style="margin:0 0 6px;font-size:11px;color:#7986CB;
                                          text-transform:uppercase;letter-spacing:2px;
                                          font-weight:600">Mã xác thực OTP</p>
                                <p style="margin:0;font-size:44px;font-weight:900;
                                          color:#1565C0;letter-spacing:14px">%s</p>
                              </td>
                            </tr>
                          </table>

                          <p style="margin:24px 0 6px;font-size:14px;color:#E53935">
                            ⏰ &nbsp;Mã có hiệu lực trong <strong>%d phút</strong>.
                          </p>
                          <p style="margin:0 0 24px;font-size:14px;color:#E53935">
                            🔒 &nbsp;<strong>Không chia sẻ mã này với bất kỳ ai</strong>,
                            kể cả nhân viên ngân hàng.
                          </p>
                          <p style="margin:0;font-size:13px;color:#9E9E9E">
                            Nếu bạn không thực hiện yêu cầu này, hãy bỏ qua email.
                          </p>
                        </td>
                      </tr>

                      <!-- Footer -->
                      <tr>
                        <td style="background:#FAFAFA;padding:18px 40px;
                                   border-top:1px solid #EEEEEE;text-align:center">
                          <p style="margin:0;font-size:12px;color:#BDBDBD">
                            © 2025 Banking App &nbsp;·&nbsp; Email tự động, vui lòng không phản hồi.
                          </p>
                        </td>
                      </tr>

                    </table>
                  </td></tr>
                </table>
                </body>
                </html>
                """.formatted(name, purposeIntro(purpose), otp, expiryMinutes);
    }
}
