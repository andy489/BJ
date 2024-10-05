package com.casino.blackjack.controller;

import com.casino.blackjack.service.GameService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;


@Controller
@RequestMapping("/play")
public class PlayController extends BaseController {

    private final GameService gameService;

    public PlayController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping
    public ModelAndView getTable(ModelAndView mav) {
        mav.addObject("game", gameService.getTable());
        return super.view("play/bj-play", mav);
    }

    @PostMapping("/deal")
    public ModelAndView deal(@RequestParam(required = false) String betStr) {
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
}
