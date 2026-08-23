package com.casino.blackjack.service.simulation;

public enum SimulationStrategy {

    DEALER_MIRROR(
            "Dealer Mirror",
            "Dealer-mirror strategy: hit on any total ≤16, stand on any total ≥17 (incl. soft 17); no splits/DD/insurance/surrender",
            true
    ),
    BASIC_STRATEGY(
            "Basic Strategy",
            "Mathematically optimal strategy (S17, multi-deck): correct soft-hand decisions, double-down on 9/10/11 and soft hands vs weak dealer, pair splits — reduces house edge to ~0.5%",
            true
    ),
    CUSTOM(
            "Custom Strategy",
            "User-defined strategy — not yet implemented.",
            false
    );

    private final String label;
    private final String description;
    private final boolean implemented;

    SimulationStrategy(String label, String description, boolean implemented) {
        this.label = label;
        this.description = description;
        this.implemented = implemented;
    }

    public String getLabel()       { return label; }
    public String getDescription() { return description; }
    public boolean isImplemented() { return implemented; }
}
