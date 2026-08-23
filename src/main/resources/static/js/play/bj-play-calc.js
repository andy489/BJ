/**
 * Pure calculation functions for the blackjack bet UI.
 * No DOM, no jQuery, no sessionStorage — fully testable.
 */

const BJ_CALC = (function () {
    const MIN_BET    = 10.0
    const MAX_BET    = 1000.0
    const SIDE_BET_MIN = 5.0
    const SIDE_BET_MAX = 25.0

    /**
     * Calculate the new main bet and balance after adding a chip or doubling.
     *
     * @param {number} currentBet     - current staged main bet
     * @param {number} balance        - current wallet balance shown on page
     * @param {number|null} chipValue - chip denomination to add (null when doubling)
     * @param {boolean} doubleChip    - true = double the current bet instead of adding a chip
     * @returns {{ newBet: number, newBalance: number, noFunds: boolean }}
     */
    function calcChipValues(currentBet, balance, chipValue, doubleChip) {
        if (balance <= 0) {
            return { newBet: currentBet, newBalance: balance, noFunds: true }
        }

        let newBet, newBalance
        if (doubleChip) {
            newBet     = currentBet * 2
            newBalance = balance - currentBet
        } else {
            newBet     = currentBet + chipValue
            newBalance = balance - chipValue
        }

        // Clamp to MAX_BET
        if (newBet > MAX_BET) {
            const diff = newBet - MAX_BET
            newBet     = MAX_BET
            newBalance += diff
        }

        // Clamp to available balance
        if (newBalance < 0) {
            if (doubleChip) {
                newBet = currentBet + balance
            } else {
                const diff = chipValue + newBalance
                newBet = currentBet + diff
            }
            newBalance = 0
        }

        return { newBet, newBalance, noFunds: false }
    }

    /**
     * Calculate the new side bet total and balance after adding a chip.
     *
     * @param {number} currentSideBet - existing staged amount for this side bet
     * @param {number} balance        - current wallet balance shown on page
     * @param {number} chipValue      - chip denomination to add
     * @returns {{ newSideBet: number, newBalance: number, noFunds: boolean, atCap: boolean }}
     */
    function calcSideChipValues(currentSideBet, balance, chipValue) {
        if (balance <= 0) {
            return { newSideBet: currentSideBet, newBalance: balance, noFunds: true, atCap: false }
        }

        const room = SIDE_BET_MAX - currentSideBet
        if (room <= 0) {
            return { newSideBet: currentSideBet, newBalance: balance, noFunds: false, atCap: true }
        }

        const toAdd      = Math.min(chipValue, room, balance)
        const newSideBet = currentSideBet + toAdd
        const newBalance = balance - toAdd

        return { newSideBet, newBalance, noFunds: false, atCap: false }
    }

    /**
     * Calculate total stake across all bet areas.
     *
     * @param {number} handBet  - main hand bet
     * @param {number} ppBet    - Perfect Pairs staged bet
     * @param {number} t3Bet    - 21+3 staged bet
     * @param {number} dppBet   - Dealer Perfect Pairs staged bet
     * @returns {number}
     */
    function calcTotalStake(handBet, ppBet, t3Bet, dppBet) {
        return (handBet || 0) + (ppBet || 0) + (t3Bet || 0) + (dppBet || 0)
    }

    /**
     * Determine if the deal button should be enabled.
     *
     * @param {number} betVal - current staged main bet
     * @returns {boolean} true = enabled, false = disabled
     */
    function isDealEnabled(betVal) {
        return !isNaN(betVal) && betVal >= MIN_BET
    }

    return { calcChipValues, calcSideChipValues, calcTotalStake, isDealEnabled, MIN_BET, MAX_BET, SIDE_BET_MIN, SIDE_BET_MAX }
})()

if (typeof module !== 'undefined' && module.exports) {
    module.exports = BJ_CALC
}
