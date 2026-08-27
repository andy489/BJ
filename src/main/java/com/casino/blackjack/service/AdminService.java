package com.casino.blackjack.service;

import com.casino.blackjack.model.entity.GameEntity;
import com.casino.blackjack.model.entity.WalletEntity;
import com.casino.blackjack.repo.BetHistoryRepository;
import com.casino.blackjack.repo.LastGameRepository;
import com.casino.blackjack.repo.PastGameRepository;
import com.casino.blackjack.repo.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AdminService {

    private final BetHistoryRepository betHistoryRepository;
    private final PastGameRepository pastGameRepository;
    private final LastGameRepository lastGameRepository;
    private final WalletRepository walletRepository;

    public AdminService(BetHistoryRepository betHistoryRepository,
                        PastGameRepository pastGameRepository,
                        LastGameRepository lastGameRepository,
                        WalletRepository walletRepository) {
        this.betHistoryRepository = betHistoryRepository;
        this.pastGameRepository = pastGameRepository;
        this.lastGameRepository = lastGameRepository;
        this.walletRepository = walletRepository;
    }

    @Transactional
    public void clearAllHistory() {
        // FK: bet_history.game_hash → played_games.hash (RESTRICT) — must delete child first
        betHistoryRepository.deleteAll();
        pastGameRepository.deleteAll();

        // Reset active games: clear card/choice state but keep the row so the user's session is not broken
        List<GameEntity> activeGames = lastGameRepository.findAll();
        for (GameEntity g : activeGames) {
            g.setHash(null)
             .setPlayerCards(null)
             .setDealerCards(null)
             .setDealerSecondCard(null)
             .setAvailableChoices(null)
             .setTakenChoices(null)
             .setErrCodeList(null)
             .setFinalized(null)
             .setDealtTime(null)
             .setHandMultiplier(null)
             .setInsuranceMultiplier(null)
             .setInsurance(null)
             .setDoubleDown(null)
             .setSplitHands(null)
             .setSplitHandMultipliers(null)
             .setSplitDoubleDownFlags(null)
             .setSplitActive(false)
             .setActiveSplitHandIndex(0)
             .setSplitCount(0)
             .setSplitAces(false)
             .setInitialPlayerCards(null)
             .setInitialDealerUpCard(null)
             .setInitialDealerCards(null);
        }
        lastGameRepository.saveAll(activeGames);

        // Reset last-hand stats on all wallets so the UI shows zeroes after a history clear
        List<WalletEntity> wallets = walletRepository.findAll();
        for (WalletEntity w : wallets) {
            w.setLastBet(BigDecimal.ZERO)
             .setLastTotalBet(BigDecimal.ZERO)
             .setLastWin(BigDecimal.ZERO)
             .setLastHandWin(BigDecimal.ZERO)
             .setLastPpBet(BigDecimal.ZERO)
             .setLastT3Bet(BigDecimal.ZERO)
             .setLastDppBet(BigDecimal.ZERO)
             .setLastPpWin(BigDecimal.ZERO)
             .setLastT3Win(BigDecimal.ZERO)
             .setLastDppWin(BigDecimal.ZERO);
        }
        walletRepository.saveAll(wallets);
    }
}
