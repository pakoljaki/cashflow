package com.akosgyongyosi.cashflow.service.fx;

import com.akosgyongyosi.cashflow.entity.Transaction;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Aspect
@Component
public class TransactionFxEnsureAspect {

    private final FxRateEnsurer fxRateEnsurer;

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

        if (!dates.isEmpty()) {
            LocalDate min = dates.stream().min(LocalDate::compareTo).get();
            LocalDate max = dates.stream().max(LocalDate::compareTo).get();
            if (min.equals(max)) fxRateEnsurer.ensureFor(min);
            else fxRateEnsurer.ensureForRange(min, max);
        }

        return pjp.proceed();
    }
}
