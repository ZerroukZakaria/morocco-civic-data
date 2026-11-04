package io.github.zerroukzakaria.civicdata.election;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SharesTest {

    @Test
    void roundsToTwoDecimals() {
        assertThat(Shares.percent(1, 3)).isEqualByComparingTo("33.33");
        assertThat(Shares.percent(2, 3)).isEqualByComparingTo("66.67");
    }

    @Test
    void keepsTwoDecimalsForExactShares() {
        assertThat(Shares.percent(1, 8)).hasToString("12.50");
    }

    @Test
    void givesZeroForZeroVotes() {
        assertThat(Shares.percent(0, 100)).hasToString("0.00");
    }

    @Test
    void givesZeroInsteadOfDividingByZero() {
        assertThat(Shares.percent(5, 0)).hasToString("0.00");
    }
}