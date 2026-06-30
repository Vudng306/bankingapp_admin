package org.nhom8.banking.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class UserProfileResponse {

    private Long                        id;
    private String                      fullName;
    private String                      email;
    private String                      phone;
    private String                      avatarUrl;
    private String                      status;
    private LocalDateTime               createdAt;
    private List<AccountSummaryResponse> accounts;
}
