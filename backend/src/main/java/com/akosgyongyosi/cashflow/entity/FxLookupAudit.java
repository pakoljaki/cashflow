package com.akosgyongyosi.cashflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "fx_lookup_audit", indexes = {
        @Index(name = "ix_fxlookup_ts", columnList = "ts"),
        @Index(name = "ix_fxlookup_base_quote_reqdate", columnList = "base_currency, quote_currency, requested_date")
})
public class FxLookupAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ts", nullable = false)
    private Instant ts = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "base_currency", length = 3, nullable = false)
    private Currency baseCurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "quote_currency", length = 3, nullable = false)
    private Currency quoteCurrency;

    @Column(name = "requested_date", nullable = false)
    private LocalDate requestedDate;

    @Column(name = "effective_rate_date", nullable = false)
    private LocalDate effectiveRateDate;

    @Column(name = "rate_mid", precision = 18, scale = 8, nullable = false)
    private BigDecimal rateMid;

    @Column(name = "provisional", nullable = false)
    private boolean provisional;

    @Column(name = "warning_codes", length = 256)
    private String warningCodes; 
}
