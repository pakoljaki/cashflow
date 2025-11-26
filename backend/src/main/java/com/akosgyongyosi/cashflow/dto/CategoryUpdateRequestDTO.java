package com.akosgyongyosi.cashflow.dto;

public class CategoryUpdateRequestDTO {
    private String categoryName;
    private boolean createNewCategory;
    private String description;

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public boolean isCreateNewCategory() {
        return createNewCategory;
    }

    public void setCreateNewCategory(boolean createNewCategory) {
        this.createNewCategory = createNewCategory;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
