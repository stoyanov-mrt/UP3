package com.example.dto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class-level constraint for the conditional request rules:
 * ON_SITE requires a non-blank address, URGENT requires a non-blank urgencyReason.
 */
@Documented
@Constraint(validatedBy = ConditionalRequestFieldsValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConditionalRequestFields {

    String message() default "Conditional request fields are invalid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
