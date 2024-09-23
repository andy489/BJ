package com.casino.blackjack.model.validation.deposit;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CurrencyValidator implements ConstraintValidator<Currency, String> {

    // \d{1,4} matches from one upto four digits.
    // (?:\.\d{1,2})? matches an optional decimal and the following digits; number of digits following must be 1 or 2.
    private static final String CURRENCY_PATTERN = "^(?:10000|[1-9]\\d{1,4})(?:\\.\\d{1,2})?$";

    @Override
    public boolean isValid(String currency, ConstraintValidatorContext context) {
        currency = currency.replaceAll(",", "");
        return currency.matches(CURRENCY_PATTERN);
    }
}
