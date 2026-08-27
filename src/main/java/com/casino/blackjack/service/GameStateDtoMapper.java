package com.casino.blackjack.service;

import com.casino.blackjack.model.dto.BetHistoryEntryDto;
import com.casino.blackjack.model.dto.BetHistoryView;
import com.casino.blackjack.model.dto.CardDto;
import com.casino.blackjack.model.dto.GameStateDto;
import com.casino.blackjack.model.dto.WalletStateDto;
import com.casino.blackjack.service.gamelogic.dto.Card;
import com.casino.blackjack.service.gamelogic.dto.Game;
import com.casino.blackjack.service.gamelogic.dto.Wallet;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Component
public class GameStateDtoMapper {

    public GameStateDto map(Game game, List<BetHistoryView> betHistory) {
        Wallet w = game.getWallet();

        GameStateDto dto = new GameStateDto()
                .setHash(game.getHash())
                .setDealt(Boolean.TRUE.equals(game.getDealt()))
                .setFinalized(Boolean.TRUE.equals(game.getFinalized()))
                .setSplitActive(Boolean.TRUE.equals(game.getSplitActive()))
                .setActiveSplitHandIndex(game.getActiveSplitHandIndex() != null ? game.getActiveSplitHandIndex() : 0)
                .setDoubleDown(Boolean.TRUE.equals(game.getDoubleDown()))
                .setSplitAces(Boolean.TRUE.equals(game.getSplitAces()))
                .setPlayerCards(toCardDtos(game.getPlayerCards()))
                .setDealerCards(toCardDtos(game.getDealerCards()))
                .setSplitHands(toSplitHandDtos(game.getSplitHands()))
                .setSplitHandMultipliers(game.getSplitHandMultipliers() != null ? game.getSplitHandMultipliers() : Collections.emptyList())
                .setSplitDoubleDownFlags(game.getSplitDoubleDownFlags() != null ? game.getSplitDoubleDownFlags() : Collections.emptyList())
                .setPlayerScore(Boolean.TRUE.equals(game.getDealt()) ? game.playerScore() : null)
                .setDealerScore(Boolean.TRUE.equals(game.getDealt()) ? game.dealerScore() : null)
                .setSplitScores(buildSplitScores(game))
                .setAvailableChoices(game.getAvailableChoices() != null ? game.getAvailableChoices() : Collections.emptyList())
                .setErrCodeList(game.getErrCodeList() != null ? game.getErrCodeList() : Collections.emptyList())
                .setHandMultiplier(game.getHandMultiplier())
                .setInsuranceMultiplier(game.getInsuranceMultiplier())
                .setWallet(w != null ? toWalletDto(w) : null);

        if (betHistory != null && !betHistory.isEmpty()) {
            dto.setBetHistory(betHistory.stream().map(BetHistoryEntryDto::new).toList());
        }

        return dto;
    }

    private List<CardDto> toCardDtos(List<Card> cards) {
        if (cards == null) return Collections.emptyList();
        return cards.stream().map(c -> new CardDto(c.getRank(), c.getSuit())).toList();
    }

    private List<List<CardDto>> toSplitHandDtos(List<List<Card>> splitHands) {
        if (splitHands == null) return Collections.emptyList();
        return splitHands.stream().map(this::toCardDtos).toList();
    }

    private List<String> buildSplitScores(Game game) {
        List<List<Card>> splitHands = game.getSplitHands();
        if (splitHands == null || splitHands.isEmpty()) return Collections.emptyList();
        return splitHands.stream().map(game::getScoreForSplitHand).toList();
    }

    private WalletStateDto toWalletDto(Wallet w) {
        return new WalletStateDto()
                .setBalance(nvl(w.getBalance()))
                .setCurrentBet(nvl(w.getCurrentBet()))
                .setLastBet(nvl(w.getLastBet()))
                .setLastWin(nvl(w.getLastWin()))
                .setLastHandWin(nvl(w.getLastHandWin()))
                .setLastPpWin(nvl(w.getLastPpWin()))
                .setLastT3Win(nvl(w.getLastT3Win()))
                .setLastDppWin(nvl(w.getLastDppWin()))
                .setLastTotalBet(nvl(w.getLastTotalBet()))
                .setLastPpBet(nvl(w.getLastPpBet()))
                .setLastT3Bet(nvl(w.getLastT3Bet()))
                .setLastDppBet(nvl(w.getLastDppBet()))
                .setPerfectPairsBet(nvl(w.getPerfectPairsBet()))
                .setTwentyOneThreeBet(nvl(w.getTwentyOneThreeBet()))
                .setDealerPerfectPairsBet(nvl(w.getDealerPerfectPairsBet()))
                .setHandBet(nvl(w.getHandBet()))
                .setDoubleBet(nvl(w.getDoubleBet()))
                .setInsuranceBet(nvl(w.getInsuranceBet()))
                .setSplitBet(nvl(w.getSplitBet()));
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
