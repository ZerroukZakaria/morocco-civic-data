package io.github.zerroukzakaria.civicdata.election;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class Shares {

    private Shares() {
    }

    /** votes as a percentage of total, rounded half up to two decimals. Zero when there is no total. */
    static BigDecimal percent(long votes, long total) {
        if (total <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(votes)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }
}