package com.casino.blackjack.service.simulation;

public record SimulationResult(
        long hands,
        double totalWagered,
        double totalReturned,
        double rtp,
        long wins,
        long losses,
        long pushes,
        long blackjacks,
        String strategy
) {
    public static SimulationResult of(long hands, double wagered, double returned,
                                       long wins, long losses, long pushes, long blackjacks,
                                       String strategy) {
        double rtp = wagered > 0 ? returned / wagered * 100.0 : 0.0;
        return new SimulationResult(hands, wagered, returned, rtp, wins, losses, pushes, blackjacks, strategy);
    }
}
