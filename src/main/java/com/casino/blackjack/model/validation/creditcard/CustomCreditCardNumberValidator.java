package com.casino.blackjack.model.validation.creditcard;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CustomCreditCardNumberValidator implements ConstraintValidator<CustomCreditCardNumber, String> {

    private static final int AMERICAN_EXPRESS_LENGTH = 15;
    private static final int STANDARD_LENGTH = 16;

    @Override
    public boolean isValid(String cardNumberRaw, ConstraintValidatorContext context) {

        String onlyDigits = cardNumberRaw.replaceAll("[^0-9]", "");

        // American Express: starts with 34 or 37, 15 digits
        boolean validAmericanExpress = (onlyDigits.startsWith("37") || onlyDigits.startsWith("34"))
                && onlyDigits.length() == AMERICAN_EXPRESS_LENGTH;

        // Visa: starts with "4", 16 digits
        boolean validVisa = onlyDigits.startsWith("4")
                && onlyDigits.length() == STANDARD_LENGTH;

        // Mastercard: starts with 51-55 (traditional) or 2221-2720 (new range), 16 digits
        boolean validMastercard = onlyDigits.length() == STANDARD_LENGTH
                && (isMastercardTraditionalPrefix(onlyDigits) || isMastercardNewRangePrefix(onlyDigits));

        return validVisa || validAmericanExpress || validMastercard;
    }

    private static boolean isMastercardTraditionalPrefix(String digits) {
        if (digits.length() < 2) return false;
        int prefix2 = Integer.parseInt(digits.substring(0, 2));
        return prefix2 >= 51 && prefix2 <= 55;
    }

    private static boolean isMastercardNewRangePrefix(String digits) {
        if (digits.length() < 4) return false;
        int prefix4 = Integer.parseInt(digits.substring(0, 4));
        return prefix4 >= 2221 && prefix4 <= 2720;
    }
}
