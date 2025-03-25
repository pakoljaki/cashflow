package com.akosgyongyosi.cashflow.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class AssumptionIdSequence {
    @Id
    private String seqName;
    private Long nextVal;
}