package com.casino.blackjack.controller;

import com.casino.blackjack.service.simulation.SimulationResult;
import com.casino.blackjack.service.simulation.SimulationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/admin/simulation")
@PreAuthorize("hasAuthority('ADMIN')")
public class SimulationController extends BaseController {

    private static final long MAX_SPINS = 10_000_000L;
    private static final long MIN_SPINS = 1L;

    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @GetMapping
    public ModelAndView form(ModelAndView mav) {
        return super.view("admin/simulation", mav);
    }

    @PostMapping
    public ModelAndView run(@RequestParam("spins") long spins, ModelAndView mav) {
        if (spins < MIN_SPINS || spins > MAX_SPINS) {
            mav.addObject("error", "Spins must be between 1 and 10,000,000.");
            return super.view("admin/simulation", mav);
        }
        SimulationResult result = simulationService.simulate(spins);
        mav.addObject("result", result);
        return super.view("admin/simulation", mav);
    }
}
