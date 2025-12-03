package com.akosgyongyosi.cashflow.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class CategoryUpdateRequestDTO {
    private String categoryName;
    private boolean createNewCategory;
    private String description;
}
