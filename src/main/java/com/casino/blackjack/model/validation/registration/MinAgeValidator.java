package com.casino.blackjack.model.validation.registration;

import com.casino.blackjack.util.DateUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.Locale;

public class MinAgeValidator implements ConstraintValidator<MinAge, String> {

    private Integer minAge;

    @Override
    public void initialize(MinAge constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);

        this.minAge = constraintAnnotation.min();
    }

    @Override
    public boolean isValid(String rawData, ConstraintValidatorContext context) {

        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);

        try {
            Date date = formatter.parse(rawData);

            if(date.after(Date.from(Instant.now()))){
                return true; // @CustomPast will be invalid
            }

            return DateUtil.calcYearsBetween(date, LocalDate.now()) >= minAge;
        } catch (ParseException e) {
            return true; // @Pattern will be invalid
        }
    }
}
