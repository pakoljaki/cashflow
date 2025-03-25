package com.akosgyongyosi.cashflow.mapper;

import com.akosgyongyosi.cashflow.dto.CashflowPlanDTO;
import com.akosgyongyosi.cashflow.dto.PlanLineItemDTO;
import com.akosgyongyosi.cashflow.entity.CashflowPlan;
import com.akosgyongyosi.cashflow.entity.PlanLineItem;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring") 
public interface CashflowPlanMapper {

    CashflowPlanMapper INSTANCE = Mappers.getMapper(CashflowPlanMapper.class);

    CashflowPlanDTO toDTO(CashflowPlan plan);

    CashflowPlan toEntity(CashflowPlanDTO dto);

    PlanLineItemDTO toDTO(PlanLineItem item);

    PlanLineItem toEntity(PlanLineItemDTO dto);

    List<PlanLineItemDTO> toDTO(List<PlanLineItem> items);
    List<PlanLineItem> toEntity(List<PlanLineItemDTO> items);
}
