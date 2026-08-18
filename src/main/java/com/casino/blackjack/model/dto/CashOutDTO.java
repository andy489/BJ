package com.casino.blackjack.model.dto;

import com.casino.blackjack.model.validation.deposit.Currency;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class CashOutDTO {

    @NotBlank(message = "{constraint.not.blank}")
    @Currency(message = "{constraint.cashout.sum}")
    private String cashOutSum;
}
