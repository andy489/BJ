package com.casino.blackjack.service.gamelogic.util;

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

    public static final Integer DEALER_CARDS_PROP_IND = 0;
    public static final Integer PLAYER_CARDS_PROP_IND = 1;
    public static final Integer AVAILABLE_CHOICES_CARDS_PROP_IND = 2;
    public static final Integer TAKEN_CHOICES_PROP_IND = 3;

    public static final Integer DEALER_THRESHOLD_17 = 17;
}
