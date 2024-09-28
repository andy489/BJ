package com.casino.blackjack.controller;

import com.casino.blackjack.service.GameService;
import com.casino.blackjack.service.gamelogic.dto.Game;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/play")
public class PlayController extends BaseController {

    private final GameService gameService;

    public PlayController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping
    public ModelAndView sitOnTable() {

        Game game = gameService.sitOnTable();

        ModelAndView mav = new ModelAndView();
        mav.addObject("game", game);

        return super.view("play/bj-play", mav);
    }

    @PostMapping("/deal")
    public ModelAndView deal(ModelAndView modelAndView) {
        gameService.deal();
        return super.redirect("/play");
    }
}
