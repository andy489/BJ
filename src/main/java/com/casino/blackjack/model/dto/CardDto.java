package com.casino.blackjack.model.dto;

public class CardDto {
    private int rank;
    private int suit;

    public CardDto() {}

    public CardDto(int rank, int suit) {
        this.rank = rank;
        this.suit = suit;
    }

    public int getRank() { return rank; }
    public int getSuit() { return suit; }
}
