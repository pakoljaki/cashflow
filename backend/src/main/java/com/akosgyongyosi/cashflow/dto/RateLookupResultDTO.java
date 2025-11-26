package com.akosgyongyosi.cashflow.dto;

import java.math.BigDecimal;
import java.util.List;

public class RateLookupResultDTO {
    private final RateMetaDTO meta;
    private final List<FxWarningDTO> warnings;

    public RateLookupResultDTO(RateMetaDTO meta, List<FxWarningDTO> warnings) {
        this.meta = meta;
        this.warnings = warnings;
    }

    public RateMetaDTO getMeta() {
        return meta;
    }

    public List<FxWarningDTO> getWarnings() {
        return warnings;
    }

    public BigDecimal getRate() {
        return meta.getRate();
    }

    public boolean isProvisional() {
        return meta.isProvisional();
    }
}
