package com.akosgyongyosi.cashflow.service.fx;

import com.akosgyongyosi.cashflow.entity.Transaction;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Aspect
@Component
public class TransactionFxEnsureAspect {

    private final FxRateEnsurer fxRateEnsurer;
    private static final Logger log = LoggerFactory.getLogger(TransactionFxEnsureAspect.class);

    public TransactionFxEnsureAspect(FxRateEnsurer fxRateEnsurer) {
        this.fxRateEnsurer = fxRateEnsurer;
    }

@Around("execution(* com.akosgyongyosi.cashflow.repository.TransactionRepository.save*(..))")
    public Object aroundSaves(ProceedingJoinPoint pjp) throws Throwable {
        List<LocalDate> dates = new ArrayList<>();

        for (Object arg : pjp.getArgs()) {
            if (arg instanceof Transaction t && t.getBookingDate() != null) {
                dates.add(t.getBookingDate());
            } else if (arg instanceof Collection<?> coll) {
                for (Object o : coll) {
                    if (o instanceof Transaction t2 && t2.getBookingDate() != null) {
                        dates.add(t2.getBookingDate());
                    }
                }
            } else if (arg != null && arg.getClass().isArray()) {
                Object[] arr = (Object[]) arg;
                for (Object o : arr) {
                    if (o instanceof Transaction t3 && t3.getBookingDate() != null) {
                        dates.add(t3.getBookingDate());
                    }
                }
            }
        }

        Object result = pjp.proceed();

        if (!dates.isEmpty()) {
            LocalDate min = dates.stream().min(LocalDate::compareTo)
                .orElseThrow(() -> new IllegalStateException("Unable to determine min date"));
            LocalDate max = dates.stream().max(LocalDate::compareTo)
                .orElseThrow(() -> new IllegalStateException("Unable to determine max date"));

            Runnable ensureTask = () -> {
                try {
                    if (min.equals(max)) fxRateEnsurer.ensureFor(min);
                    else fxRateEnsurer.ensureForRange(min, max);
                } catch (Exception e) {
                    log.error("FX ensure failed for range {} - {}: {}", min, max, e.getMessage(), e);
                }
            };

            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        ensureTask.run();
                    }
                });
                log.debug("Registered FX ensure for dates {} - {} to run after commit", min, max);
            } else {
                ensureTask.run();
            }
        }

        return result;
    }
}
