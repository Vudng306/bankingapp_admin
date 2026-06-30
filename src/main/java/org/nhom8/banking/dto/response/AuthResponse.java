package org.nhom8.banking.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {

    private String  token;
    private Long    userId;
    private String  email;
    private String  fullName;
    /** Thời gian hết hạn token tính bằng giây */
    private long    expiresIn;
}
