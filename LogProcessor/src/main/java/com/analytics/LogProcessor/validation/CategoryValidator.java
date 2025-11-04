package com.analytics.LogProcessor.validation;

import com.analytics.LogProcessor.model.Category;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CategoryValidator implements ConstraintValidator<ValidCategory,String> {

    private boolean allowNull;

    @Override
    public void initialize(ValidCategory constraintAnnotation) {
        this.allowNull = constraintAnnotation.allowNull();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if(value== null) return allowNull;
        return Category.isValid(value);
    }
}
