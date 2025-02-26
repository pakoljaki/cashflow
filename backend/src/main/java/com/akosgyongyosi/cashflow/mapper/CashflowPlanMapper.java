package com.akosgyongyosi.cashflow.mapper;

import com.akosgyongyosi.cashflow.dto.CashflowPlanDTO;
import com.akosgyongyosi.cashflow.dto.PlanLineItemDTO;
import com.akosgyongyosi.cashflow.entity.CashflowPlan;
import com.akosgyongyosi.cashflow.entity.PlanLineItem;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")  // Enables Spring's dependency injection
public interface CashflowPlanMapper {

    CashflowPlanMapper INSTANCE = Mappers.getMapper(CashflowPlanMapper.class);

    // convert cashflowplan entity to DTO
    CashflowPlanDTO toDTO(CashflowPlan plan);

    // convert DTO to entity
    CashflowPlan toEntity(CashflowPlanDTO dto);

    // convert line item entity to DTO
    PlanLineItemDTO toDTO(PlanLineItem item);

    // Convert DTO to entity
    PlanLineItem toEntity(PlanLineItemDTO dto);

    // list conversions (MapStruct handles automatically)
    List<PlanLineItemDTO> toDTO(List<PlanLineItem> items);
    List<PlanLineItem> toEntity(List<PlanLineItemDTO> items);
}
