package com.casino.blackjack.controller;

import com.casino.blackjack.model.dto.BetHistoryView;
import com.casino.blackjack.model.dto.GameStateDto;
import com.casino.blackjack.service.BetHistoryService;
import com.casino.blackjack.service.GameService;
import com.casino.blackjack.service.GameStateDtoMapper;
import com.casino.blackjack.service.auth.UserService;
import com.casino.blackjack.config.GameProperties;
import com.casino.blackjack.service.gamelogic.dto.Game;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.math.BigDecimal;
import java.util.List;

import static com.casino.blackjack.service.gamelogic.util.GameUtil.SIDE_BET_MIN;


@Controller
@RequestMapping("/play")
public class PlayController extends BaseController {

    private final GameService gameService;
    private final BetHistoryService betHistoryService;
    private final UserService userService;
    private final GameStateDtoMapper mapper;
    private final int resultDisplayMs;

    public PlayController(GameService gameService,
                          BetHistoryService betHistoryService,
                          UserService userService,
                          GameStateDtoMapper mapper,
                          GameProperties gameProperties) {
        this.gameService = gameService;
        this.betHistoryService = betHistoryService;
        this.userService = userService;
        this.mapper = mapper;
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
    public Object deal(@RequestParam(required = false) String betStr,
                       @RequestParam(required = false) String ppBetStr,
                       @RequestParam(required = false) String t3BetStr,
                       @RequestParam(required = false) String dppBetStr,
                       @RequestHeader(value = "Accept", defaultValue = "") String accept) {
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
        if (isJsonRequest(accept)) return jsonState(null);
        return super.redirect("/play");
    }

    @PostMapping("/even")
    public Object even(@RequestParam Boolean evenMoney,
                       @RequestHeader(value = "Accept", defaultValue = "") String accept) {
        gameService.even(evenMoney);
        if (isJsonRequest(accept)) return jsonState(null);
        return super.redirect("/play");
    }

    @PostMapping("/hit")
    public Object hit(@RequestHeader(value = "Accept", defaultValue = "") String accept) {
        gameService.hit();
        if (isJsonRequest(accept)) return jsonState(null);
        return super.redirect("/play");
    }

    @PostMapping("/stand")
    public Object stand(@RequestHeader(value = "Accept", defaultValue = "") String accept) {
        gameService.stand();
        if (isJsonRequest(accept)) return jsonState(null);
        return super.redirect("/play");
    }

    @PostMapping("/surrender")
    public Object surrender(@RequestHeader(value = "Accept", defaultValue = "") String accept) {
        gameService.surrender();
        if (isJsonRequest(accept)) return jsonState(null);
        return super.redirect("/play");
    }

    @PostMapping("/insurance")
    public Object insurance(@RequestParam Boolean insurance,
                            @RequestHeader(value = "Accept", defaultValue = "") String accept) {
        gameService.insurance(insurance);
        if (isJsonRequest(accept)) return jsonState(null);
        return super.redirect("/play");
    }

    @PostMapping("/double-down")
    public Object doubleDown(@RequestHeader(value = "Accept", defaultValue = "") String accept) {
        gameService.doubleDown();
        if (isJsonRequest(accept)) return jsonState(null);
        return super.redirect("/play");
    }

    @PostMapping("/split")
    public Object split(@RequestHeader(value = "Accept", defaultValue = "") String accept) {
        gameService.split();
        if (isJsonRequest(accept)) return jsonState(null);
        return super.redirect("/play");
    }

    @PostMapping("/auto-finalize")
    public Object autoFinalize(@RequestHeader(value = "Accept", defaultValue = "") String accept) {
        gameService.autoFinalize();
        if (isJsonRequest(accept)) return jsonState(null);
        return super.redirect("/play");
    }

    @PostMapping("/split-dd-advance")
    public Object splitDdAdvance(@RequestHeader(value = "Accept", defaultValue = "") String accept) {
        gameService.splitDdAdvance();
        if (isJsonRequest(accept)) return jsonState(null);
        return super.redirect("/play");
    }

    @PostMapping("/auto-play")
    public Object autoPlay(@RequestHeader(value = "Accept", defaultValue = "") String accept) {
        gameService.autoPlay();
        if (isJsonRequest(accept)) return jsonState(null);
        return super.redirect("/play");
    }

    @PostMapping("/dd-confirm")
    public Object doubleDownConfirm(@RequestParam Boolean confirm,
                                    @RequestHeader(value = "Accept", defaultValue = "") String accept) {
        gameService.ddConfirm(confirm);
        if (isJsonRequest(accept)) return jsonState(null);
        return super.redirect("/play");
    }

    @PostMapping("/repeat-last-bet")
    public Object repeatLastBet(@RequestHeader(value = "Accept", defaultValue = "") String accept) {
        gameService.repeatLastBet();
        if (isJsonRequest(accept)) return jsonState(null);
        return super.redirect("/play");
    }

    @PostMapping("/double-bet")
    public Object doubleBet(@RequestHeader(value = "Accept", defaultValue = "") String accept) {
        gameService.doubleBet();
        if (isJsonRequest(accept)) return jsonState(null);
        return super.redirect("/play");
    }

    @PostMapping("/clear-bet")
    public Object clearBet(@RequestHeader(value = "Accept", defaultValue = "") String accept) {
        gameService.clearBet();
        if (isJsonRequest(accept)) return jsonState(null);
        return super.redirect("/play");
    }

    @PostMapping("/accept")
    public Object accept(@RequestParam Boolean depositRedirect,
                         @RequestHeader(value = "Accept", defaultValue = "") String accept) {
        gameService.accept();

        if (isJsonRequest(accept)) {
            GameStateDto state = buildState(null);
            if (depositRedirect) state.setRedirectUrl("/credit-card/deposit");
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(state);
        }

        if (depositRedirect) {
            return super.redirect("/credit-card/deposit");
        }
        return super.redirect("/play");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private boolean isJsonRequest(String accept) {
        return accept.contains(MediaType.APPLICATION_JSON_VALUE);
    }

    @ResponseBody
    private ResponseEntity<GameStateDto> jsonState(List<BetHistoryView> history) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(buildState(history));
    }

    private GameStateDto buildState(List<BetHistoryView> history) {
        Game game = gameService.getTable();
        game.setDealerSecondCard(null);

        List<BetHistoryView> betHistory = history;
        if (betHistory == null && Boolean.TRUE.equals(game.getFinalized())) {
            betHistory = betHistoryService.getLast10(userService.getCurrentLoggedUserId());
        }

        return mapper.map(game, betHistory);
    }

    private boolean isValidSideBet(String betStr) {
        try {
            return new BigDecimal(betStr).compareTo(SIDE_BET_MIN) >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
