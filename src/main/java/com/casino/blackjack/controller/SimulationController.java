package com.casino.blackjack.controller;

import com.casino.blackjack.service.simulation.SideBetRtpCalculator;
import com.casino.blackjack.service.simulation.SimulationResult;
import com.casino.blackjack.service.simulation.SimulationService;
import com.casino.blackjack.service.simulation.SimulationStrategy;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Controller
@RequestMapping("/admin/simulation")
@PreAuthorize("hasAuthority('ADMIN')")
public class SimulationController extends BaseController {

    private static final long   MAX_SPINS   = 10_000_000L;
    private static final long   MIN_SPINS   = 1L;
    private static final double MIN_BET     = 0.10;
    private static final double MAX_BET     = 10_000.0;
    private static final int    MIN_THREADS = 1;
    private static final int    MAX_THREADS = 4;

    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @GetMapping
    public ModelAndView form(ModelAndView mav) {
        mav.addObject("strategies", SimulationStrategy.values());
        mav.addObject("sideBetRtp", SideBetRtpCalculator.calculate());
        return super.view("admin/simulation", mav);
    }

    @PostMapping
    public ModelAndView run(
            @RequestParam("spins") long spins,
            @RequestParam(value = "bet",      defaultValue = "1.0")          double bet,
            @RequestParam(value = "threads",  defaultValue = "1")            int threads,
            @RequestParam(value = "strategy", defaultValue = "DEALER_MIRROR") String strategyKey,
            ModelAndView mav) {

        mav.addObject("strategies", SimulationStrategy.values());

        SimulationStrategy strategy;
        try {
            strategy = SimulationStrategy.valueOf(strategyKey);
        } catch (IllegalArgumentException e) {
            mav.addObject("error", "Unknown strategy.");
            return super.view("admin/simulation", mav);
        }

        if (!strategy.isImplemented()) {
            mav.addObject("error", strategy.getLabel() + " is not yet implemented.");
            return super.view("admin/simulation", mav);
        }

        if (spins < MIN_SPINS || spins > MAX_SPINS) {
            mav.addObject("error", "Hands must be between 1 and 10,000,000.");
            return super.view("admin/simulation", mav);
        }

        double roundedBet = BigDecimal.valueOf(bet).setScale(2, RoundingMode.HALF_UP).doubleValue();
        if (roundedBet < MIN_BET || roundedBet > MAX_BET
                || BigDecimal.valueOf(roundedBet).remainder(BigDecimal.valueOf(0.10)).abs().doubleValue() > 0.001) {
            mav.addObject("error", "Bet must be a multiple of £0.10 between £0.10 and £10,000.");
            return super.view("admin/simulation", mav);
        }

        if (threads < MIN_THREADS || threads > MAX_THREADS) {
            mav.addObject("error", "Threads must be between 1 and 4.");
            return super.view("admin/simulation", mav);
        }

        SimulationResult result = simulationService.simulate(spins, roundedBet, threads, strategy);
        mav.addObject("result", result);
        mav.addObject("bet", roundedBet);
        mav.addObject("threads", threads);
        mav.addObject("spins", spins);
        mav.addObject("selectedStrategy", strategy);
        mav.addObject("sideBetRtp", SideBetRtpCalculator.calculate());
        return super.view("admin/simulation", mav);
    }

    @GetMapping("/baccarat")
    public ModelAndView baccarat(ModelAndView mav) {
        return super.view("admin/simulation-baccarat", mav);
    }

    @GetMapping("/poker")
    public ModelAndView poker(ModelAndView mav) {
        return super.view("admin/simulation-poker", mav);
    }
}
