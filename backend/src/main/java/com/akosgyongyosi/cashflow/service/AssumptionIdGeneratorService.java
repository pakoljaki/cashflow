package com.akosgyongyosi.cashflow.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.akosgyongyosi.cashflow.entity.AssumptionIdSequence;
import com.akosgyongyosi.cashflow.repository.AssumptionIdSequenceRepository;

@Service
public class AssumptionIdGeneratorService {

    private final AssumptionIdSequenceRepository seqRepo;

    public AssumptionIdGeneratorService(AssumptionIdSequenceRepository seqRepo) {
        this.seqRepo = seqRepo;
    }

    @Transactional
    public Long getNextAssumptionId() {
        AssumptionIdSequence seq = seqRepo.findById("ASSUMPTION_ID").orElse(null);

        if (seq == null) {
            seq = new AssumptionIdSequence();
            seq.setSeqName("ASSUMPTION_ID");
            seq.setNextVal(1L);
            seqRepo.save(seq);
        }

        Long val = seq.getNextVal();
        seq.setNextVal(val + 1);
        seqRepo.save(seq);
        return val;
    }

}