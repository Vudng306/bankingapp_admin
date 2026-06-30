package org.nhom8.banking.common;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class TopupCatalog {

    private TopupCatalog() {}

    @Getter
    public enum Carrier {
        VIETTEL("Viettel"),
        MOBIFONE("MobiFone"),
        VINAPHONE("VinaPhone"),
        VIETNAMOBILE("Vietnamobile"),
        ITEL("Itelecom");

        private final String displayName;

        Carrier(String displayName) {
            this.displayName = displayName;
        }
    }

    public static final List<BigDecimal> FACE_VALUES = List.of(
            new BigDecimal("10000"),
            new BigDecimal("20000"),
            new BigDecimal("50000"),
            new BigDecimal("100000"),
            new BigDecimal("200000"),
            new BigDecimal("500000")
    );

    private static final Set<String> CARRIER_IDS = Arrays.stream(Carrier.values())
            .map(Enum::name)
            .collect(Collectors.toUnmodifiableSet());

    public static boolean isValidCarrier(String carrier) {
        return carrier != null && CARRIER_IDS.contains(carrier.toUpperCase());
    }

    public static boolean isValidFaceValue(BigDecimal amount) {
        return amount != null && FACE_VALUES.stream().anyMatch(fv -> fv.compareTo(amount) == 0);
    }
}
