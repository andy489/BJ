const MIN_BET = 10.0
const MAX_BET = 1000.0
const SIDE_BET_MIN = 5.0
const SIDE_BET_MAX = 25.0

const CHIP_S = 2.5
const CHIP_M = 5.0
const CHIP_L = 10.0
const CHIP_XL = 25.0
const CHIP_2XL = 50.0
const CHIP_3XL = 100.0

/* ── Side bet chip routing ───────────────────────────────────────────────────
   'main' = main bet (default)
   'pp'   = Perfect Pairs
   '213'  = 21+3
   Clicking a side-bet circle selects it; clicking again deselects.
   chipTarget persists across PRG reloads via sessionStorage.
────────────────────────────────────────────────────────────────────────────── */
var chipTarget = sessionStorage.getItem('bj-chip-target') || 'main'

/* Client-side staged side bet amounts (submitted at Deal time) */
var ppStagedBet  = 0.0
var t3StagedBet  = 0.0
var dppStagedBet = 0.0

$(document).ready(function () {
    // Restore chipTarget visual state on page load
    applyChipTargetStyle(chipTarget)

    // Restore staged side bet amounts from sessionStorage (survive PRG reload)
    ppStagedBet  = parseFloat(sessionStorage.getItem('bj-pp-staged')  || '0') || 0.0
    t3StagedBet  = parseFloat(sessionStorage.getItem('bj-t3-staged')  || '0') || 0.0
    dppStagedBet = parseFloat(sessionStorage.getItem('bj-dpp-staged') || '0') || 0.0

    // Sync staged amounts with server-side committed amounts.
    // Server is authoritative: if server reports 0 (post-payout or fresh), clear stale staged.
    // If server has a committed amount and staged is empty, seed from server (mid-hand reload).
    var serverPP  = (typeof BJ_PP_BET  !== 'undefined' && BJ_PP_BET  != null) ? parseFloat(BJ_PP_BET)  : 0
    var serverT3  = (typeof BJ_213_BET !== 'undefined' && BJ_213_BET != null) ? parseFloat(BJ_213_BET) : 0
    var serverDPP = (typeof BJ_DPP_BET !== 'undefined' && BJ_DPP_BET != null) ? parseFloat(BJ_DPP_BET) : 0

    if (serverPP === 0) {
        ppStagedBet = 0.0; sessionStorage.removeItem('bj-pp-staged')
    } else if (ppStagedBet === 0) {
        ppStagedBet = serverPP; sessionStorage.setItem('bj-pp-staged', ppStagedBet)
    }
    if (serverT3 === 0) {
        t3StagedBet = 0.0; sessionStorage.removeItem('bj-t3-staged')
    } else if (t3StagedBet === 0) {
        t3StagedBet = serverT3; sessionStorage.setItem('bj-t3-staged', t3StagedBet)
    }
    if (serverDPP === 0) {
        dppStagedBet = 0.0; sessionStorage.removeItem('bj-dpp-staged')
    } else if (dppStagedBet === 0) {
        dppStagedBet = serverDPP; sessionStorage.setItem('bj-dpp-staged', dppStagedBet)
    }

    updateSideCircleDisplay('pp',  ppStagedBet)
    updateSideCircleDisplay('213', t3StagedBet)
    updateSideCircleDisplay('dpp', dppStagedBet)
    updateTotalStakeDisplay()

    // Apply low-bet class to main circle amount on initial load
    var initBet = parseFloat($('.curr-bet-value').val()) || 0.0
    var mainCurrBet = $('.curr-bet')[0]
    if (mainCurrBet) {
        if (initBet > 0 && initBet < MIN_BET) mainCurrBet.classList.add('low-bet')
        else mainCurrBet.classList.remove('low-bet')
    }

    $('.btn-err-ok').click(function () {
        $('.err-modal-wrapper').addClass("d-none")
        $('.modal-overlay').removeClass('active')
    })

    $('#btn-no-funds-ok').click(function () {
        $('#no-funds-modal').addClass('d-none')
        $('.modal-overlay').removeClass('active')
    })

    /* Low bet — show modal when deal is clicked with bet > 0 but < MIN_BET */
    $('.btn-deal').click(function () {
        if (!$(this).hasClass('disabled')) return
        var betVal = parseFloat($('.curr-bet-value').val()) || 0.0
        if (betVal > 0 && betVal < MIN_BET) {
            $('#low-bet-modal').removeClass('d-none')
            $('.modal-overlay').addClass('active')
        }
    })

    /* Split — show weak-pair warning when pair nominal < dealer nominal */
    $('#btn-split-trigger').click(function () {
        if ($(this).hasClass('disabled')) return
        var pair   = (typeof BJ_PAIR_NOMINAL   !== 'undefined') ? BJ_PAIR_NOMINAL   : 0
        var dealer = (typeof BJ_DEALER_NOMINAL !== 'undefined') ? BJ_DEALER_NOMINAL : 0
        if (pair > 0 && dealer > 0 && pair < dealer) {
            $('#split-pair-value').text(pair)
            $('#split-dealer-value').text(dealer)
            $('#split-weak-pair-modal').removeClass('d-none')
            $('.modal-overlay').addClass('active')
        } else {
            $('#form-split').submit()
        }
    })

    $('#btn-split-weak-yes').click(function () {
        $('#split-weak-pair-modal').addClass('d-none')
        $('.modal-overlay').removeClass('active')
        $('#form-split').submit()
    })

    $('#btn-split-weak-no').click(function () {
        $('#split-weak-pair-modal').addClass('d-none')
        $('.modal-overlay').removeClass('active')
    })

    /* Surrender confirm modal */
    $('#btn-surrender-trigger').click(function () {
        if ($(this).hasClass('disabled')) return
        $('#surrender-confirm-modal').removeClass('d-none')
        $('.modal-overlay').addClass('active')
    })

    $('#btn-surrender-yes').click(function () {
        $('#surrender-confirm-modal').addClass('d-none')
        $('.modal-overlay').removeClass('active')
        $('#form-surrender').submit()
    })

    $('#btn-surrender-no').click(function () {
        $('#surrender-confirm-modal').addClass('d-none')
        $('.modal-overlay').removeClass('active')
    })

    /* Hit on {score} confirm modal */
    $('#btn-hit-trigger').click(function () {
        if ($(this).hasClass('disabled')) return
        var effectiveScore = (typeof BJ_PLAYER_IS_SOFT !== 'undefined' && BJ_PLAYER_IS_SOFT)
            ? BJ_PLAYER_HARD + 10
            : BJ_PLAYER_HARD
        if (typeof BJ_PLAYER_HARD !== 'undefined' && effectiveScore >= 17) {
            $('#hit-score-display').text(effectiveScore)
            $('#hit-hard17-confirm-modal').removeClass('d-none')
            $('.modal-overlay').addClass('active')
        } else {
            $('#form-hit').submit()
        }
    })

    $('#btn-hit-hard17-yes').click(function () {
        $('#hit-hard17-confirm-modal').addClass('d-none')
        $('.modal-overlay').removeClass('active')
        $('#form-hit').submit()
    })

    $('#btn-hit-hard17-no').click(function () {
        $('#hit-hard17-confirm-modal').addClass('d-none')
        $('.modal-overlay').removeClass('active')
    })

    /* Stand on 11 or lower confirm modal */
    $('#btn-stand-trigger').click(function () {
        if ($(this).hasClass('disabled')) return
        if (typeof BJ_PLAYER_HARD !== 'undefined' && BJ_PLAYER_HARD <= 11 && !BJ_PLAYER_IS_SOFT) {
            $('#stand-low-score').text(BJ_PLAYER_HARD)
            $('#stand-low-confirm-modal').removeClass('d-none')
            $('.modal-overlay').addClass('active')
        } else {
            $('#form-stand').submit()
        }
    })

    $('#btn-stand-low-yes').click(function () {
        $('#stand-low-confirm-modal').addClass('d-none')
        $('.modal-overlay').removeClass('active')
        $('#form-stand').submit()
    })

    $('#btn-stand-low-no').click(function () {
        $('#stand-low-confirm-modal').addClass('d-none')
        $('.modal-overlay').removeClass('active')
    })

    /* ── Side bet circles on the table ── */
    $('#bet-circle-pp').click(function () {
        setChipTarget(chipTarget === 'pp' ? 'main' : 'pp')
    })

    $('#bet-circle-213').click(function () {
        setChipTarget(chipTarget === '213' ? 'main' : '213')
    })

    $('#bet-circle-dpp').click(function () {
        setChipTarget(chipTarget === 'dpp' ? 'main' : 'dpp')
    })

    $('#bet-circle-main').click(function () {
        setChipTarget('main')
    })

    /* Chips */
    $('.chip-250').click(function ()  { handleChip(CHIP_S,   false) })
    $('.chip-500').click(function ()  { handleChip(CHIP_M,   false) })
    $('.chip-1000').click(function () { handleChip(CHIP_L,   false) })
    $('.chip-2500').click(function () { handleChip(CHIP_XL,  false) })
    $('.chip-5000').click(function () { handleChip(CHIP_2XL, false) })
    $('.chip-10000').click(function () { handleChip(CHIP_3XL, false) })
    $('.btn-chip-double').click(function () { handleChip(null, true) })

    /* Repeat last bet — restores main hand bet and all side bets from previous hand */
    $('.form-repeat').on('submit', function (e) {
        var lastBet    = (typeof BJ_LAST_BET     !== 'undefined' && BJ_LAST_BET     != null) ? parseFloat(BJ_LAST_BET)     : 0
        var lastPpBet  = (typeof BJ_LAST_PP_BET  !== 'undefined' && BJ_LAST_PP_BET  != null) ? parseFloat(BJ_LAST_PP_BET)  : 0
        var lastT3Bet  = (typeof BJ_LAST_T3_BET  !== 'undefined' && BJ_LAST_T3_BET  != null) ? parseFloat(BJ_LAST_T3_BET)  : 0
        var lastDppBet = (typeof BJ_LAST_DPP_BET !== 'undefined' && BJ_LAST_DPP_BET != null) ? parseFloat(BJ_LAST_DPP_BET) : 0
        if (isNaN(lastBet) || lastBet <= 0) return
        e.preventDefault()
        calcChip(lastBet, false)
        if (!isNaN(lastPpBet)  && lastPpBet  > 0) calcSideChip('pp',  lastPpBet)
        if (!isNaN(lastT3Bet)  && lastT3Bet  > 0) calcSideChip('213', lastT3Bet)
        if (!isNaN(lastDppBet) && lastDppBet > 0) calcSideChip('dpp', lastDppBet)
    })

    /* Clear — clears the selected bet area client-side; POSTs to server when cards are present.
       If the selected target already has nothing to clear, clears ALL bet areas at once. */
    $('.form-clear').on('submit', function (e) {
        // Let the POST through when cards are on the table (active hand) or when the game just finalized
        var hasCards = BJ_GAME_DEALT && !BJ_FINALIZED && BJ_LAST_CHOICE >= 1 && BJ_LAST_CHOICE <= 15
        // Also POST when a completed hand's cards are still visible (post-payout state:
        // dealt=false, finalized=false, but takenChoices non-empty → last_games row still present)
        var hasPostPayoutCards = !BJ_GAME_DEALT && !BJ_FINALIZED && BJ_LAST_CHOICE > 0
        if (hasCards || BJ_FINALIZED || hasPostPayoutCards) return  // let the POST clear server-side state

        e.preventDefault()

        var balanceElem = $('.balance')[0]
        var bal = parseFloat(balanceElem.innerText.replace(/[£,]/g, '')) || 0.0

        // Determine if the currently selected target already has nothing to clear
        var currentTargetAmount = 0.0
        if (chipTarget === 'pp')        currentTargetAmount = ppStagedBet
        else if (chipTarget === '213')  currentTargetAmount = t3StagedBet
        else if (chipTarget === 'dpp')  currentTargetAmount = dppStagedBet
        else                            currentTargetAmount = parseFloat($('.curr-bet-value').val()) || 0.0

        var clearAll = (currentTargetAmount <= 0)

        if (clearAll) {
            // Clear every bet area and return all amounts to balance
            var mainBet = parseFloat($('.curr-bet-value').val()) || 0.0
            bal += mainBet + ppStagedBet + t3StagedBet + dppStagedBet

            ppStagedBet  = 0.0; sessionStorage.setItem('bj-pp-staged',  '0'); updateSideCircleDisplay('pp',  0)
            t3StagedBet  = 0.0; sessionStorage.setItem('bj-t3-staged',  '0'); updateSideCircleDisplay('213', 0)
            dppStagedBet = 0.0; sessionStorage.setItem('bj-dpp-staged', '0'); updateSideCircleDisplay('dpp', 0)

            $('.curr-bet-value').val('0')
            var mainCurrBet = $('.curr-bet')[0]
            mainCurrBet.innerText = '£0.00'
            mainCurrBet.classList.remove('low-bet')
            refreshDealButton()
        } else if (chipTarget === 'pp') {
            bal += ppStagedBet
            ppStagedBet = 0.0
            sessionStorage.setItem('bj-pp-staged', '0')
            updateSideCircleDisplay('pp', 0)
        } else if (chipTarget === '213') {
            bal += t3StagedBet
            t3StagedBet = 0.0
            sessionStorage.setItem('bj-t3-staged', '0')
            updateSideCircleDisplay('213', 0)
        } else if (chipTarget === 'dpp') {
            bal += dppStagedBet
            dppStagedBet = 0.0
            sessionStorage.setItem('bj-dpp-staged', '0')
            updateSideCircleDisplay('dpp', 0)
        } else {
            bal += currentTargetAmount
            $('.curr-bet-value').val('0')
            var mainCurrBet = $('.curr-bet')[0]
            mainCurrBet.innerText = '£0.00'
            mainCurrBet.classList.remove('low-bet')
            refreshDealButton()
        }

        balanceElem.innerText = '£' + bal.toLocaleString('en-GB', {minimumFractionDigits: 2, maximumFractionDigits: 2})
        updateTotalStakeDisplay()
    })

    /* Deal — populate hidden side bet fields and clear sessionStorage */
    $('.form-deal').on('submit', function (e) {
        var betVal = parseFloat($('.curr-bet-value').val())
        if (isNaN(betVal) || betVal < MIN_BET) {
            e.preventDefault()
            return false
        }
        // Populate side bet hidden fields
        $('#deal-pp-bet').val(ppStagedBet > 0 ? ppStagedBet.toFixed(2) : '0')
        $('#deal-213-bet').val(t3StagedBet > 0 ? t3StagedBet.toFixed(2) : '0')
        $('#deal-dpp-bet').val(dppStagedBet > 0 ? dppStagedBet.toFixed(2) : '0')
        // Clear staged state — server now owns these amounts
        sessionStorage.removeItem('bj-pp-staged')
        sessionStorage.removeItem('bj-t3-staged')
        sessionStorage.removeItem('bj-dpp-staged')
        sessionStorage.removeItem('bj-chip-target')
    })

    refreshDealButton()
})

/* ── Side bet target management ─────────────────────────────────────────── */

function setChipTarget(target) {
    chipTarget = target
    sessionStorage.setItem('bj-chip-target', target)
    applyChipTargetStyle(target)
}

function applyChipTargetStyle(target) {
    $('#bet-circle-main').toggleClass('bet-circle--selected', target === 'main')
    $('#bet-circle-pp').toggleClass('bet-circle--selected', target === 'pp')
    $('#bet-circle-213').toggleClass('bet-circle--selected', target === '213')
    $('#bet-circle-dpp').toggleClass('bet-circle--selected', target === 'dpp')
}

function updateSideCircleDisplay(target, amount) {
    var circleId = target === 'pp' ? 'bet-circle-pp' : (target === 'dpp' ? 'bet-circle-dpp' : 'bet-circle-213')
    var circle   = document.getElementById(circleId)
    if (!circle) return

    var amountSpan = circle.querySelector('.bet-circle-amount')
    if (amount > 0) {
        if (!amountSpan) {
            amountSpan = document.createElement('span')
            amountSpan.className = 'bet-circle-amount'
            circle.appendChild(amountSpan)
        }
        amountSpan.textContent = '£' + amount.toLocaleString('en-GB', {minimumFractionDigits: 2, maximumFractionDigits: 2})
        amountSpan.classList.toggle('low-bet', amount < SIDE_BET_MIN)
        circle.classList.add('bet-circle--active')
    } else {
        if (amountSpan) amountSpan.remove()
        circle.classList.remove('bet-circle--active')
    }
}

/* ── Chip handler — routes to main or side bet ───────────────────────── */
function handleChip(chipValue, doubleChip) {
    if (chipTarget === 'pp' || chipTarget === '213' || chipTarget === 'dpp') {
        // Side bets don't support double-chip — treat as no-op
        if (doubleChip) return
        calcSideChip(chipTarget, chipValue)
    } else {
        calcChip(chipValue, doubleChip)
    }
}

/* ── Add chip to a side bet slot (client-side staging only) ── */
function calcSideChip(target, chipValue) {
    var balanceElem = $('.balance')[0]
    const balance = parseFloat(balanceElem.innerText.replace(/[£,]/g, '')) || 0.0

    var current = (target === 'pp') ? ppStagedBet : (target === 'dpp' ? dppStagedBet : t3StagedBet)
    const { newSideBet, newBalance, noFunds, atCap } = BJ_CALC.calcSideChipValues(current, balance, chipValue)

    if (noFunds) {
        $('#no-funds-modal').removeClass('d-none')
        $('.modal-overlay').addClass('active')
        return
    }
    if (atCap) return

    if (target === 'pp') {
        ppStagedBet = newSideBet
        sessionStorage.setItem('bj-pp-staged', ppStagedBet)
    } else if (target === 'dpp') {
        dppStagedBet = newSideBet
        sessionStorage.setItem('bj-dpp-staged', dppStagedBet)
    } else {
        t3StagedBet = newSideBet
        sessionStorage.setItem('bj-t3-staged', t3StagedBet)
    }

    balanceElem.innerText = '£' + newBalance.toLocaleString('en-GB', {minimumFractionDigits: 2, maximumFractionDigits: 2})
    updateSideCircleDisplay(target, newSideBet)
    updateTotalStakeDisplay()
}

/* ── Main bet chip calculation ───────────────────────────────────────────── */
function updateTotalStakeDisplay() {
    var handBet = parseFloat($('.curr-bet-value').val()) || 0.0
    var total = BJ_CALC.calcTotalStake(handBet, ppStagedBet, t3StagedBet, dppStagedBet)
    $('.total-stake').text('£' + total.toLocaleString('en-GB', {minimumFractionDigits: 2, maximumFractionDigits: 2}))
}

function refreshDealButton() {
    var betVal = parseFloat($('.curr-bet-value').val())
    var dealBtn = $('.btn-deal')
    if (BJ_CALC.isDealEnabled(betVal)) {
        dealBtn.removeClass('disabled')
    } else {
        dealBtn.addClass('disabled')
    }
}

function calcChip(chipValue, doubleChip) {
    let hiddenBetField = $('.curr-bet-value')[0]
    let currBetElem = $('.curr-bet')[0]
    let balanceElem = $('.balance')[0]

    const currentBet = parseFloat(hiddenBetField.value) || 0.0
    const balance    = parseFloat(balanceElem.innerText.replace(/[£,]/g, '')) || 0.0

    const { newBet, newBalance, noFunds } = BJ_CALC.calcChipValues(currentBet, balance, chipValue, doubleChip)

    if (noFunds) {
        $('#no-funds-modal').removeClass('d-none')
        $('.modal-overlay').addClass('active')
        return
    }

    if (newBet < BJ_CALC.MIN_BET) {
        currBetElem.classList.add('low-bet')
    } else {
        currBetElem.classList.remove('low-bet')
    }

    currBetElem.innerText = '£' + newBet.toLocaleString('en-GB', {minimumFractionDigits: 2, maximumFractionDigits: 2})
    balanceElem.innerText = '£' + newBalance.toLocaleString('en-GB', {minimumFractionDigits: 2, maximumFractionDigits: 2})
    hiddenBetField.value  = newBet

    updateTotalStakeDisplay()
    refreshDealButton()
}
