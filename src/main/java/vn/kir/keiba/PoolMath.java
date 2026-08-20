package vn.kir.keiba;

final class PoolMath {
    private PoolMath() { }

    static double decimalOdds(double totalPool, double pairPool, double houseEdge) {
        if (pairPool <= 0.0 || totalPool <= 0.0) return 0.0;
        return totalPool * (1.0 - houseEdge) / pairPool;
    }

    static double decimalOdds(double totalPool, double winningPool, double houseEdge, double minimum, double maximum) {
        double natural = decimalOdds(totalPool, winningPool, houseEdge);
        if (natural <= 0.0) return 0.0;
        double safeMinimum = Math.max(0.0, minimum);
        double safeMaximum = Math.max(safeMinimum, maximum);
        return Math.max(safeMinimum, Math.min(safeMaximum, natural));
    }

    static double payout(double stake, double totalPool, double winningPool, double houseEdge) {
        return stake * decimalOdds(totalPool, winningPool, houseEdge);
    }
}
