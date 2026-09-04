package com.casino.blackjack.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class HomeController extends BaseController {

    @Value("${openweathermap.api-key:}")
    private String weatherApiKey;

    @GetMapping({"/", "/index"})
    public ModelAndView getIndex() {
        ModelAndView mv = super.view("index");
        mv.addObject("weatherApiKey", weatherApiKey);
        return mv;
    }

    @GetMapping("/rules")
    public ModelAndView getRules() {

        return super.view("rules");
    }

    @GetMapping("/keep-alive")
    public ResponseEntity<String> keepAlive() {
        return ResponseEntity.ok("ok");
    }

}
