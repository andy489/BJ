package com.casino.blackjack.controller;

import com.casino.blackjack.service.BetHistoryService;
import com.casino.blackjack.service.GameService;
import com.casino.blackjack.service.auth.UserService;
import com.casino.blackjack.config.GameProperties;
import com.casino.blackjack.service.gamelogic.dto.Game;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.math.BigDecimal;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.SIDE_BET_MIN;


@Controller
@RequestMapping("/play")
public class PlayController extends BaseController {

    private final GameService gameService;
    private final BetHistoryService betHistoryService;
    private final UserService userService;
    private final int resultDisplayMs;

    public PlayController(GameService gameService,
                          BetHistoryService betHistoryService,
                          UserService userService,
                          GameProperties gameProperties) {
        this.gameService = gameService;
        this.betHistoryService = betHistoryService;
        this.userService = userService;
        this.resultDisplayMs = gameProperties.getResultDisplayMs();
    }

    @GetMapping
    public ModelAndView getTable(ModelAndView mav) {
        Game table = gameService.getTable();
        table.setDealerSecondCard(null);
        mav.addObject("game", table);
        mav.addObject("betHistory", betHistoryService.getLast10(userService.getCurrentLoggedUserId()));
        mav.addObject("resultDisplayMs", resultDisplayMs);
        return super.view("play/bj-play", mav);
    }

    @GetMapping("/baccarat")
    public ModelAndView getBaccaratDemo(ModelAndView mav) {
        return super.view("play/coming-soon", mav);
    }

    @GetMapping("/poker")
    public ModelAndView getPokerDemo(ModelAndView mav) {
        return super.view("play/coming-soon", mav);
    }

    @PostMapping("/deal")
    public ModelAndView deal(@RequestParam(required = false) String betStr,
                             @RequestParam(required = false) String ppBetStr,
                             @RequestParam(required = false) String t3BetStr,
                             @RequestParam(required = false) String dppBetStr) {
        if (ppBetStr != null && !ppBetStr.isBlank() && !ppBetStr.equals("0") && isValidSideBet(ppBetStr)) {
            gameService.placePerfectPairsBet(ppBetStr);
        }
        if (t3BetStr != null && !t3BetStr.isBlank() && !t3BetStr.equals("0") && isValidSideBet(t3BetStr)) {
            gameService.place21_3Bet(t3BetStr);
        }
        if (dppBetStr != null && !dppBetStr.isBlank() && !dppBetStr.equals("0") && isValidSideBet(dppBetStr)) {
            gameService.placeDealerPerfectPairsBet(dppBetStr);
        }
        gameService.deal(betStr);
        return super.redirect("/play");
    }

    @PostMapping("/even")
    public ModelAndView even(@RequestParam Boolean evenMoney) {
        gameService.even(evenMoney);
        return super.redirect("/play");
    }

    @PostMapping("/hit")
    public ModelAndView hit() {
        gameService.hit();
        return super.redirect("/play");
    }

    @PostMapping("/stand")
    public ModelAndView stand() {
        gameService.stand();
        return super.redirect("/play");
    }

    @PostMapping("/surrender")
    public ModelAndView surrender() {
        gameService.surrender();
        return super.redirect("/play");
    }

    @PostMapping("/insurance")
    public ModelAndView insurance(@RequestParam Boolean insurance) {
        gameService.insurance(insurance);
        return super.redirect("/play");
    }

    @PostMapping("/double-down")
    public ModelAndView doubleDown() {
        gameService.doubleDown();
        return super.redirect("/play");
    }

    @PostMapping("/split")
    public ModelAndView split() {
        gameService.split();
        return super.redirect("/play");
    }

    @PostMapping("/auto-finalize")
    public ModelAndView autoFinalize() {
        gameService.autoFinalize();
        return super.redirect("/play");
    }

    @PostMapping("/split-dd-advance")
    public ModelAndView splitDdAdvance() {
        gameService.splitDdAdvance();
        return super.redirect("/play");
    }

    @PostMapping("/auto-play")
    public ModelAndView autoPlay() {
        gameService.autoPlay();
        return super.redirect("/play");
    }

    @PostMapping("/dd-confirm")
    public ModelAndView doubleDownConfirm(@RequestParam Boolean confirm) {
        gameService.ddConfirm(confirm);
        return super.redirect("/play");
    }

    @PostMapping("/repeat-last-bet")
    public ModelAndView repeatLastBet() {
        gameService.repeatLastBet();
        return super.redirect("/play");
    }

    @PostMapping("/clear-bet")
    public ModelAndView clearBet() {
        gameService.clearBet();
        return super.redirect("/play");
    }

    @PostMapping("/accept")
    public ModelAndView accept(@RequestParam Boolean depositRedirect) {
        gameService.accept();

        if(depositRedirect) {
            return super.redirect("/credit-card/deposit");
        }

        return super.redirect("/play");
    }

    @PostMapping("/side-bet/pp")
    public ModelAndView placePerfectPairsBet(@RequestParam(required = false) String betStr) {
        gameService.placePerfectPairsBet(betStr);
        return super.redirect("/play");
    }

    @PostMapping("/side-bet/21-3")
    public ModelAndView place21_3Bet(@RequestParam(required = false) String betStr) {
        gameService.place21_3Bet(betStr);
        return super.redirect("/play");
    }

    @PostMapping("/side-bet/dpp")
    public ModelAndView placeDealerPerfectPairsBet(@RequestParam(required = false) String betStr) {
        gameService.placeDealerPerfectPairsBet(betStr);
        return super.redirect("/play");
    }

    private boolean isValidSideBet(String betStr) {
        try {
            return new BigDecimal(betStr).compareTo(SIDE_BET_MIN) >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
