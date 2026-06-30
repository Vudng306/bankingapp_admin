package org.nhom8.banking.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationResponse {

    private Long          id;
    private String        title;
    private String        content;
    private String        type;    // TRANSACTION, BALANCE, SYSTEM
    private boolean       read;
    private LocalDateTime createdAt;
}
