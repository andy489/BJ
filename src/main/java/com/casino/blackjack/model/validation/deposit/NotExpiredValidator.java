package com.casino.blackjack.model.validation.deposit;

import com.casino.blackjack.model.dto.CreditCardDTO;
import com.casino.blackjack.service.CreditCardService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Calendar;
import java.util.Optional;

public class NotExpiredValidator implements ConstraintValidator<NotExpired, String> {

    private final CreditCardService creditCardService;

    public NotExpiredValidator(CreditCardService creditCardService) {

        this.creditCardService = creditCardService;
    }

    @Override
    public boolean isValid(String cardNum, ConstraintValidatorContext context) {

        Optional<CreditCardDTO> byCardNumber = creditCardService.getByCardNumber(cardNum);

        if (byCardNumber.isEmpty()) {
            return false;
        }

        CreditCardDTO creditCardDTO = byCardNumber.get();

        Integer expiredYear = creditCardDTO.getExpiredYear();
        Integer expiredMonth = creditCardDTO.getExpiredMonth();

        return checkCurrentMonthBeforeExpiredMonth(expiredYear, expiredMonth);
    }

    public static boolean checkCurrentMonthBeforeExpiredMonth(Integer expiredYear, Integer expiredMonth) {
        Calendar c = Calendar.getInstance();
        int currentYear = c.get(Calendar.YEAR);
        int currentMonth = c.get(Calendar.MONTH) + 1; // Calendar.MONTH is 0-based; normalise to 1-based

        if (currentYear < expiredYear) {
            return true;
        } else if (currentYear == expiredYear) {
            return currentMonth <= expiredMonth; // valid through end of expiry month
        } else {
            return false;
        }
    }
}
