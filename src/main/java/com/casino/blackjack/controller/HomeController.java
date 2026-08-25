package com.casino.blackjack.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class HomeController extends BaseController {


    @GetMapping({"/", "/index"})
    public ModelAndView getIndex() {

        return super.view("index");
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
