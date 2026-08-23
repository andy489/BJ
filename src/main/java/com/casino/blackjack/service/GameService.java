package com.casino.blackjack.service;

import com.casino.blackjack.model.entity.GameEntity;
import com.casino.blackjack.model.entity.WalletEntity;
import com.casino.blackjack.repo.LastGameRepository;
import com.casino.blackjack.repo.PastGameRepository;
import com.casino.blackjack.repo.WalletRepository;
import com.casino.blackjack.service.auth.UserService;
import com.casino.blackjack.service.gamelogic.dto.Game;
import com.casino.blackjack.service.gamelogic.dto.Wallet;
import com.casino.blackjack.service.gamelogic.processor.DisplayProcessorChain;
import com.casino.blackjack.service.gamelogic.processor.GameContext;
import com.casino.blackjack.service.gamelogic.processor.GameStateProcessorChain;
import com.casino.blackjack.service.gamelogic.rng.CardSource;
import com.casino.blackjack.service.gamelogic.rng.RNG;
import com.casino.blackjack.util.LocalDateTimeProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.casino.blackjack.config.GameProperties;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_CHIP_OPERATIONS;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DEAL;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DOUBLE_DOWN;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DOUBLE_DOWN_NO;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_DOUBLE_DOWN_YES;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_EVEN_MONEY_NO;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_EVEN_MONEY_YES;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_HIT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_INSURANCE_NO;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_INSURANCE_YES;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_INSURANCE_YES_NOT_ENOUGH_MONEY;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_REPEAT_LAST_BET;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_AUTO_FINALIZE;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_AUTO_PLAY;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_SPLIT_DD_ADVANCE;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_REPEAT_LAST_BET_AGAIN;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_SPLIT;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_STAND;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_SURRENDER;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.ERR_CODE_HIGH_BET;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.ERR_CODE_INSUFFICIENT_FUNDS;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.ERR_CODE_INVALID_BET;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.ERR_CODE_LOW_BET;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.MAX_BET;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.MIN_BET;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_PLACE_PERFECT_PAIRS;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_PLACE_21_3;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.CHOICE_PLACE_DEALER_PP;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.NO_CURR_GAME_ERR;
import static com.casino.blackjack.service.gamelogic.util.GameUtil.NO_WALLET_FOUND;

@Service
public class GameService {

    private final LastGameRepository lastGameRepository;
    private final PastGameRepository pastGameRepository;
    private final WalletRepository walletRepository;

    private final UserService userService;
    private final BetHistoryService betHistoryService;

    private final BasicStrategy basicStrategy;
    private final LocalDateTimeProvider localDateTimeProvider;
    private final GameStateProcessorChain processorChain;
    private final DisplayProcessorChain displayProcessorChain;
    private final CardSource cardSource;

    private final ObjectMapper om;
    private final int maxSplits;

    public GameService(LastGameRepository lastGameRepository, PastGameRepository pastGameRepository,
                       WalletRepository walletRepository, UserService userService,
                       BetHistoryService betHistoryService, BasicStrategy basicStrategy,
                       LocalDateTimeProvider localDateTimeProvider,
                       GameStateProcessorChain processorChain, DisplayProcessorChain displayProcessorChain,
                       CardSource cardSource,
                       ObjectMapper om,
                       GameProperties gameProperties) {
        this.lastGameRepository = lastGameRepository;
        this.pastGameRepository = pastGameRepository;
        this.walletRepository = walletRepository;
        this.userService = userService;
        this.betHistoryService = betHistoryService;
        this.basicStrategy = basicStrategy;
        this.localDateTimeProvider = localDateTimeProvider;
        this.processorChain = processorChain;
        this.displayProcessorChain = displayProcessorChain;
        this.cardSource = cardSource;
        this.om = om;
        this.maxSplits = gameProperties.getMaxSplits();
    }

    public Game getTable() {
        Optional<WalletEntity> currentWalletEntity = extractWallet();
        WalletEntity walletEntity;

        if (currentWalletEntity.isEmpty()) {
            walletEntity = new WalletEntity().setOwner(userService.getCurrentLoggedUser());
            walletRepository.save(walletEntity);
        } else {
            walletEntity = currentWalletEntity.get();
        }

        Optional<GameEntity> currentGameEntity = extractLastGame();

        if (currentGameEntity.isEmpty()) {
            return new Game()
                    .setAvailableChoices(List.of(CHOICE_DEAL, CHOICE_CHIP_OPERATIONS))
                    .setWallet(Wallet.of(walletEntity));
        }

        GameEntity gameEntity = currentGameEntity.get();
        Game game = Game.of(gameEntity, om, walletEntity);

        GameContext ctx = buildContext(game, gameEntity, walletEntity);
        GameContext result = displayProcessorChain.process(ctx);
        return result.game();
    }

    public void deal(String betStr) {
        Optional<GameEntity> currGameEntity = extractLastGame();
        Optional<WalletEntity> currWalletEntity = extractWallet();

        if (currWalletEntity.isEmpty()) {
            throw new IllegalStateException(NO_WALLET_FOUND);
        }

        WalletEntity walletEntity = currWalletEntity.get();
        Wallet wallet = Wallet.of(walletEntity);

        int validBet = validateBet(betStr, wallet);

        if (validBet >= 0) {
            Game game = new Game().addErr(validBet)
                    .setAvailableChoices(List.of(CHOICE_DEAL, CHOICE_CHIP_OPERATIONS))
                    .setWallet(wallet);

            if (currGameEntity.isEmpty()) {
                lastGameRepository.save(GameEntity.of(game, om, userService.getCurrentLoggedUser()));
            } else {
                lastGameRepository.save(GameEntity.map(currGameEntity.get(), game, om));
            }
            return;
        }

        BigDecimal bet = new BigDecimal(betStr);
        wallet.setLastBet(bet);

        Game game = new Game().setCardSource(cardSource)
                .setDealt(true)
                .setHash(RNG.generateGameHash())
                .deal()
                .setDealtTime(localDateTimeProvider.getNow())
                .makeChoice(CHOICE_DEAL);

        GameEntity gameEntity = currGameEntity.isEmpty()
                ? GameEntity.of(game, om, userService.getCurrentLoggedUser())
                : GameEntity.map(currGameEntity.get(), game, om);

        // Persist the initial deal cards for side bet evaluation (survives splits)
        try {
            gameEntity.setInitialPlayerCards(om.writeValueAsString(game.getPlayerCards()));
            // dealer up-card is dealerCards[0] (before hide)
            gameEntity.setInitialDealerUpCard(om.writeValueAsString(game.getDealerCards().get(0)));
            // all dealer initial cards for Dealer Perfect Pairs evaluation
            gameEntity.setInitialDealerCards(om.writeValueAsString(game.getDealerCards()));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        GameContext ctx = buildContext(game, gameEntity, walletEntity);
        processorChain.process(ctx);

        game.setWallet(wallet.placeHandBet(bet)).adjustDealerCardsAfterDeal();

        if (wallet.cannotAffordDouble()) {
            game.removeAvailableChoice(CHOICE_DOUBLE_DOWN);
        }

        lastGameRepository.save(GameEntity.map(gameEntity, game, om));
        Wallet.map(walletEntity, wallet);
        walletRepository.save(walletEntity);
    }

    public void split() {
        choiceNoOption(CHOICE_SPLIT);
    }

    public void placePerfectPairsBet(String betStr) {
        sideBetChoice(CHOICE_PLACE_PERFECT_PAIRS, betStr);
    }

    public void place21_3Bet(String betStr) {
        sideBetChoice(CHOICE_PLACE_21_3, betStr);
    }

    public void placeDealerPerfectPairsBet(String betStr) {
        sideBetChoice(CHOICE_PLACE_DEALER_PP, betStr);
    }

    public void surrender() {
        choiceNoOption(CHOICE_SURRENDER);
    }

    public void stand() {
        choiceNoOption(CHOICE_STAND);
    }

    public void hit() {
        choiceNoOption(CHOICE_HIT);
    }

    public void autoFinalize() {
        choiceNoOption(CHOICE_AUTO_FINALIZE);
    }

    public void autoPlay() {
        choiceNoOption(CHOICE_AUTO_PLAY);
    }

    public void splitDdAdvance() {
        choiceNoOption(CHOICE_SPLIT_DD_ADVANCE);
    }

    public void insurance(Boolean insurance) {
        choiceOption(insurance, CHOICE_INSURANCE_YES, CHOICE_INSURANCE_NO);
    }

    public void doubleDown() {
        choiceNoOption(CHOICE_DOUBLE_DOWN);
    }

    public void ddConfirm(Boolean confirm) {
        choiceOption(confirm, CHOICE_DOUBLE_DOWN_YES, CHOICE_DOUBLE_DOWN_NO);
    }

    public void even(Boolean evenChoice) {
        choiceOption(evenChoice, CHOICE_EVEN_MONEY_YES, CHOICE_EVEN_MONEY_NO);
    }

    public void repeatLastBet() {
        Optional<GameEntity> currGameEntity = extractLastGame();

        Game game;
        if (currGameEntity.isPresent()) {
            Game temp = Game.of(currGameEntity.get(), om);
            if (temp.getTakenChoices().contains(CHOICE_REPEAT_LAST_BET)) {
                game = new Game()
                        .setTakenChoices(temp.getTakenChoices())
                        .makeChoice(CHOICE_REPEAT_LAST_BET_AGAIN)
                        .setAvailableChoices(List.of(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL));
            } else {
                game = new Game()
                        .makeChoice(CHOICE_REPEAT_LAST_BET)
                        .setAvailableChoices(List.of(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL));
            }
        } else {
            game = new Game()
                    .makeChoice(CHOICE_REPEAT_LAST_BET)
                    .setAvailableChoices(List.of(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL));
        }

        GameEntity gameEntity = currGameEntity.isEmpty()
                ? GameEntity.of(game, om, userService.getCurrentLoggedUser())
                : GameEntity.map(currGameEntity.get(), game, om);

        lastGameRepository.save(gameEntity);
    }

    public void clearBet() {
        Optional<GameEntity> currGameEntity = extractLastGame();
        Optional<WalletEntity> currWalletEntity = extractWallet();

        // Delete the game entity so GET /play returns a clean dealt=false state
        currGameEntity.ifPresent(lastGameRepository::delete);

        currWalletEntity.ifPresent(w -> {
            w.setLastBet(BigDecimal.ZERO);
            walletRepository.save(w);
        });
    }

    public void accept() {
        Optional<GameEntity> gameEntity = extractLastGame();

        if (gameEntity.isPresent()) {
            GameEntity currGameEntity = gameEntity.get();
            Game game = Game.of(currGameEntity, om);

            if (game.getLastChoice().equals(CHOICE_INSURANCE_YES_NOT_ENOUGH_MONEY)) {
                game.removeAvailableChoice(CHOICE_DOUBLE_DOWN);
            }

            if (game.getLastChoice().equals(CHOICE_REPEAT_LAST_BET)) {
                game.removeLastChoice();
            }

            game.clearErrors();

            lastGameRepository.save(GameEntity.map(currGameEntity, game, om));
            return;
        }

        throw new IllegalStateException(NO_CURR_GAME_ERR);
    }

    private void choiceNoOption(Integer choice) {
        Optional<GameEntity> gameEntity = extractLastGame();

        if (gameEntity.isPresent()) {
            GameEntity currGameEntity = gameEntity.get();
            Game snapshot = Game.of(currGameEntity, om);
            if (!snapshot.getAvailableChoices().contains(choice)) {
                return;
            }

            WalletEntity walletEntity = extractWallet()
                    .orElseThrow(() -> new IllegalStateException(NO_WALLET_FOUND));

            Game game = snapshot
                    .setCardSource(cardSource)
                    .makeChoice(choice);

            GameContext ctx = buildContext(game, currGameEntity, walletEntity);
            GameContext result = processorChain.process(ctx);

            lastGameRepository.save(GameEntity.map(currGameEntity, result.game(), om));
            return;
        }

        throw new IllegalStateException(NO_CURR_GAME_ERR);
    }

    /**
     * Side bet placement — fires before the deal; no availableChoices guard needed
     * since the processor itself validates the pre-deal state.
     */
    private void sideBetChoice(Integer choice, String betStr) {
        Optional<GameEntity> currGameEntity = extractLastGame();

        WalletEntity walletEntity = extractWallet()
                .orElseThrow(() -> new IllegalStateException(NO_WALLET_FOUND));

        Game game = new Game()
                .makeChoice(choice)
                .setSideBetAmountStr(betStr)
                .setAvailableChoices(List.of(CHOICE_CHIP_OPERATIONS, CHOICE_DEAL));

        GameEntity gameEntity = currGameEntity.isEmpty()
                ? GameEntity.of(game, om, userService.getCurrentLoggedUser())
                : GameEntity.map(currGameEntity.get(), game, om);

        GameContext ctx = buildContext(game, gameEntity, walletEntity);
        processorChain.process(ctx);
    }

    private void choiceOption(Boolean yes, Integer yesChoice, Integer noChoice) {
        Optional<GameEntity> gameEntity = extractLastGame();

        if (gameEntity.isPresent()) {
            GameEntity currGameEntity = gameEntity.get();
            Integer choice = yes ? yesChoice : noChoice;
            Game snapshot = Game.of(currGameEntity, om);
            if (!snapshot.getAvailableChoices().contains(choice)) {
                return;
            }

            WalletEntity walletEntity = extractWallet()
                    .orElseThrow(() -> new IllegalStateException(NO_WALLET_FOUND));

            Game game = snapshot
                    .setCardSource(cardSource)
                    .makeChoice(choice);

            GameContext ctx = buildContext(game, currGameEntity, walletEntity);
            GameContext result = processorChain.process(ctx);

            lastGameRepository.save(GameEntity.map(currGameEntity, result.game(), om));
            return;
        }

        throw new IllegalStateException(NO_CURR_GAME_ERR);
    }

    private GameContext buildContext(Game game, GameEntity gameEntity, WalletEntity walletEntity) {
        return new GameContext(game, gameEntity, walletEntity,
                lastGameRepository, pastGameRepository, walletRepository,
                betHistoryService, basicStrategy, localDateTimeProvider, om, maxSplits);
    }

    private Optional<GameEntity> extractLastGame() {
        return lastGameRepository.findByOwnerId(userService.getCurrentLoggedUserId());
    }

    private Optional<WalletEntity> extractWallet() {
        return walletRepository.findByOwnerId(userService.getCurrentLoggedUserId());
    }

    private int validateBet(String betStr, Wallet wallet) {
        BigDecimal bet;

        try {
            bet = new BigDecimal(betStr);
        } catch (NumberFormatException e) {
            return ERR_CODE_INVALID_BET;
        }

        if (bet.compareTo(MIN_BET) < 0) {
            if (wallet.getBalance().compareTo(MIN_BET) < 0) {
                return ERR_CODE_INSUFFICIENT_FUNDS;
            }
            return ERR_CODE_LOW_BET;
        }

        if (bet.compareTo(MAX_BET) > 0) {
            return ERR_CODE_HIGH_BET;
        }

        if (bet.compareTo(wallet.getBalance()) > 0) {
            return ERR_CODE_INSUFFICIENT_FUNDS;
        }

        return -1;
    }
}
