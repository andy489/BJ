package com.casino.blackjack.service.gamelogic.util;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class Util {
    public static final int DISPLACEMENT_BASE = 7;

    public static final int SOFT = 0;
    public static final int HARD = 1;

    public static final int SOFT_PIVOT = 9;

    public static final int BJ_CNT = 21;
    public static final int BJ_CARDS_CNT = 2;

    public static final String BJ_DISPLAY_CNT = "BJ";
    public static final String DISPLAY_BUST_CNT = "BUST";

    public static final int ONE_CARD = 1;

    public static final Integer CHIP_OPERATIONS = -1;
    public static final Integer SURRENDER = 0;
    public static final Integer SPLIT = 1;
    public static final Integer DOUBLE_DOWN = 2;
    public static final Integer STAND = 3;
    public static final Integer HIT = 4;
    public static final Integer DEAL = 5;
    public static final Integer EVEN_MONEY_YES = 6;
    public static final Integer EVEN_MONEY_NO = 7;
    public static final Integer INSURANCE_YES = 8;
    public static final Integer INSURANCE_NO = 9;

    public static final Integer CLUBS_SUIT = 0;
    public static final Integer DIAMONDS_SUIT = 1;
    public static final Integer HEARTS_SUIT = 2;
    public static final Integer SPADES_SUIT = 3;

    public static final Integer ACE_RANK = 1;
    public static final Integer TWO_RANK = 2;
    public static final Integer THREE_RANK = 3;
    public static final Integer FOUR_RANK = 4;
    public static final Integer FIVE_RANK = 5;
    public static final Integer SIX_RANK = 6;
    public static final Integer SEVEN_RANK = 7;
    public static final Integer EIGHT_RANK = 8;
    public static final Integer NINE_RANK = 9;
    public static final Integer TEN_RANK = 10;
    public static final Integer JAKE_RANK = 11;
    public static final Integer QUEEN_RANK = 12;
    public static final Integer KING_RANK = 13;

    public static final String NO_ID_STR = "NO ID";

    public static final Double ZERO_MULTI = 0.0d;
    public static final Double SURRENDER_MULTI = 0.5d;
    public static final Double PUSH_MULTI = 1.0d;
    public static final Double INSURANCE_MULTIPLIER = 1.5d;
    public static final Double DOUBLE_MULTI = 2.0d;
    public static final Double BJ_MULTI = 2.5d;

    public static final String SCORE_SEPARATOR = "/";

    public static final String NO_CURR_GAME_ERR = "NO CURR GAME ERROR";
    public static final String NO_TAKEN_CHOICES = "NO TAKEN CHOICES YET";
    public static final String NO_WALLET_FOUND = "NO WALLET FOUND";

    public static final Integer DEALER_CARDS_PROP_IND = 0;
    public static final Integer PLAYER_CARDS_PROP_IND = 1;
    public static final Integer AVAILABLE_CHOICES_CARDS_PROP_IND = 2;
    public static final Integer TAKEN_CHOICES_PROP_IND = 3;
    public static final Integer ERR_CODE_PROP_IND = 4;

    public static final Integer DEALER_THRESHOLD_17 = 17;

    public static final BigDecimal MIN_BET = new BigDecimal("10.00");
    public static final BigDecimal MAX_BET = new BigDecimal("1000.00");

    public static final Integer ERR_CODE_INSUFFICIENT_FUNDS = 1;
    public static final Integer ERR_CODE_INVALID_BET = 2;
    public static final Integer ERR_CODE_LOW_BET = 3;
    public static final Integer ERR_CODE_HIGH_BET = 4;

    public static final String ERR_MSG_INSUFFICIENT_FUNDS = "INSUFFICIENT FUNDS! " +
            "А deposit is required in order to place bet.";
    public static final String ERR_MSG_INVALID_BET = "INVALID BET! " +
            "Invalid bet format.";
    public static final String ERR_MSG_LOW_BET = "LOW BET! " +
            "Bet less than {MIN_BET} or exceeding {MAX BET} is not allowed.";
    public static final String ERR_MSG_HIGH_BET = "HIGH BET! " +
            "Bet less than {MIN_BET} or exceeding {MAX_BET} is not allowed.";

    public static final Map<Integer, String> ERROR_MAP = new HashMap<>();
    static {
        ERROR_MAP.put(ERR_CODE_INSUFFICIENT_FUNDS, ERR_MSG_INSUFFICIENT_FUNDS);
        ERROR_MAP.put(ERR_CODE_INVALID_BET, ERR_MSG_INVALID_BET);
        ERROR_MAP.put(ERR_CODE_LOW_BET, ERR_MSG_LOW_BET);
        ERROR_MAP.put(ERR_CODE_HIGH_BET, ERR_MSG_HIGH_BET);
    }
}
