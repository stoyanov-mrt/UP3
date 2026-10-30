package com.example.dto.validation;

import com.example.dto.CreateServiceRequestDto;
import com.example.entity.ExecutionMode;
import com.example.entity.Priority;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ConditionalRequestFieldsValidator
        implements ConstraintValidator<ConditionalRequestFields, CreateServiceRequestDto> {

    @Override
    public boolean isValid(CreateServiceRequestDto dto, ConstraintValidatorContext context) {
        if (dto == null) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        boolean valid = true;

        if (dto.executionMode() == ExecutionMode.ON_SITE && isBlank(dto.address())) {
            addViolation(context, "address", "must not be blank when executionMode is ON_SITE");
            valid = false;
        }
        if (dto.priority() == Priority.URGENT && isBlank(dto.urgencyReason())) {
            addViolation(context, "urgencyReason", "must not be blank when priority is URGENT");
            valid = false;
        }
        return valid;
    }

    // Attached to the concrete property, so the API reports it like any other field error
    private static void addViolation(ConstraintValidatorContext context, String field, String message) {
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(field)
                .addConstraintViolation();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
