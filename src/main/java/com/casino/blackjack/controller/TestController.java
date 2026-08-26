package com.casino.blackjack.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/test")
public class TestController extends BaseController {

    @GetMapping({"/email/registration"})
    public ModelAndView getRegistrationEmail() {

        return super.view("email/registration-welcome");
    }

    @GetMapping({"/email/reset-pass"})
    public ModelAndView getPasswordResetEmail() {

        return super.view("email/reset-pass");
    }

    @GetMapping({"/reset"})
    public ModelAndView getPasswordResetEmail(@RequestParam Boolean send) {

        if (send) {
            ModelAndView modelAndView = new ModelAndView();
            modelAndView.addObject("send", true);

            return super.view("auth/reset-pass-send", modelAndView);
        }

        return super.view("auth/reset-pass-send");
    }

    @GetMapping({"/{statusCode}"})
    public ModelAndView getForbiddenPage(@PathVariable(name = "statusCode") Integer code) {
        return super.view("error/" + code);
    }
}
