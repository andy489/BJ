package com.casino.blackjack.service.gamelogic.util;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public class GameUtil {

    public static final Map<String, Integer> CHOICES;
    public static final Map<String, Integer> ERRORS;

    static {
        CHOICES = new LinkedHashMap<>();
        ERRORS = new LinkedHashMap<>();

        Field[] declaredFields = GameUtil.class.getDeclaredFields();

        int i = 0, j = 0;
        for (Field field : declaredFields) {
            if (field.getName().startsWith("CHOICE_")) {
                CHOICES.put(field.getName(), i++);
            }

            if (field.getName().startsWith("ERR_CODE_")) {
                ERRORS.put(field.getName(), j++);
            }
        }
    }

    public static final int DISPLACEMENT_BASE = 7;

    public static final int BJ_CNT = 21;
    public static final int BJ_CARDS_CNT = 2;

    public static final String BJ_DISPLAY_CNT = "BJ";
    public static final String DISPLAY_BUST_CNT = "BUST";

    public static final int ONE_CARD = 1;
    public static final int INITIAL_DEALT_CARD_COUNT = 2;

    public static final Integer CHOICE_CHIP_OPERATIONS = 0;
    public static final Integer CHOICE_SURRENDER = 1;
    public static final Integer CHOICE_SPLIT = 2;
    public static final Integer CHOICE_DOUBLE_DOWN = 3;
    public static final Integer CHOICE_DOUBLE_DOWN_NOT_ENOUGH_MONEY = 4;
    public static final Integer CHOICE_DOUBLE_NOT_BASIC_STRATEGY = 5;
    public static final Integer CHOICE_DOUBLE_DOWN_YES = 6;
    public static final Integer CHOICE_DOUBLE_DOWN_NO = 7;
    public static final Integer CHOICE_STAND = 8;
    public static final Integer CHOICE_HIT = 9;
    public static final Integer CHOICE_DEAL = 10;
    public static final Integer CHOICE_EVEN_MONEY_YES = 11;
    public static final Integer CHOICE_EVEN_MONEY_NO = 12;
    public static final Integer CHOICE_INSURANCE_YES = 13;
    public static final Integer CHOICE_INSURANCE_YES_NOT_ENOUGH_MONEY = 14;
    public static final Integer CHOICE_INSURANCE_NO = 15;
    public static final Integer CHOICE_REPEAT_LAST_BET = 16;
    public static final Integer CHOICE_REPEAT_LAST_BET_AGAIN = 17;
    public static final Integer CHOICE_CLEAR_LAST_BET = 18;

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
    public static final Double INSURANCE_MULTIPLIER = 3.0d;
    public static final Double DOUBLE_MULTI = 2.0d;
    public static final Double BJ_MULTI = 2.5d;

    public static final String SCORE_SEPARATOR = "/";

    public static final String NO_CURR_GAME_ERR = "NO CURR GAME ERR";
    public static final String NO_CURR_WALLET = "NO CURR WALLET ERR";
    public static final String NO_TAKEN_CHOICES = "NO TAKEN CHOICES YET ERR";
    public static final String NO_WALLET_FOUND = "NO WALLET FOUND ERR";

    public static final Integer PROP_IND_DEALER_CARDS = 0;
    public static final Integer PROP_IND_PLAYER_CARDS = 1;
    public static final Integer PROP_IND_AVAILABLE_CHOICES_CARDS = 2;
    public static final Integer PROP_IND_TAKEN_CHOICES = 3;
    public static final Integer PROP_IND_ERR_CODE = 4;
    public static final Integer PROP_IND_DEALER_SECOND_CARD = 5;

    public static final Integer DEALER_THRESHOLD_17 = 17;

    public static final BigDecimal MIN_BET = new BigDecimal("10.00");
    public static final BigDecimal MAX_BET = new BigDecimal("1000.00");

    public static final Integer ERR_CODE_INSUFFICIENT_FUNDS = 0;
    public static final Integer ERR_CODE_INVALID_BET = 1;
    public static final Integer ERR_CODE_LOW_BET = 2;
    public static final Integer ERR_CODE_HIGH_BET = 3;
    public static final Integer NO_LAST_BET = 4;
    public static final Integer CANNOT_REPEAT_LAST_BET = 5;

    public static final String ERR_MSG_INSUFFICIENT_FUNDS = "INSUFFICIENT FUNDS! " +
            "А deposit is required in order to place bet.";
    public static final String ERR_MSG_INVALID_BET = "INVALID BET! " +
            "Invalid bet format.";
    public static final String ERR_MSG_LOW_BET = "LOW BET! " +
            "Bet less than {MIN_BET} or exceeding {MAX BET} is not allowed.";
    public static final String ERR_MSG_HIGH_BET = "HIGH BET! " +
            "Bet less than {MIN_BET} or exceeding {MAX_BET} is not allowed.";

    public static final Integer ELEVEN = 11;
    public static final Integer TEN = 10;
    public static final Integer NINE = 9;

    public static final Map<Integer, String> ERROR_MAP = new LinkedHashMap<>();

    static {
        ERROR_MAP.put(ERR_CODE_INSUFFICIENT_FUNDS, ERR_MSG_INSUFFICIENT_FUNDS);
        ERROR_MAP.put(ERR_CODE_INVALID_BET, ERR_MSG_INVALID_BET);
        ERROR_MAP.put(ERR_CODE_LOW_BET, ERR_MSG_LOW_BET);
        ERROR_MAP.put(ERR_CODE_HIGH_BET, ERR_MSG_HIGH_BET);
    }
}
