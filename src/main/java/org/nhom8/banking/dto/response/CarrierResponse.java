package org.nhom8.banking.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CarrierResponse {
    private String id;
    private String name;
}
