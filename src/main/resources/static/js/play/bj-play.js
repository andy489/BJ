const MIN_BET = 10.0
const MAX_BET = 1000.0

const CHIP_S = 2.5
const CHIP_M = 5.0
const CHIP_L = 10.0
const CHIP_XL = 25.0
const CHIP_2XL = 50.0
const CHIP_3XL = 100.0

$(document).ready(function () {
    $('.btn-err-ok').click(function () {
        $('.err-modal-wrapper').addClass("d-none")
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

    /* Hit on 17+ confirm modal (covers both hard 17 and soft 17+, e.g. soft 19 = hard 9 + 10) */
    $('#btn-hit-trigger').click(function () {
        if ($(this).hasClass('disabled')) return
        var effectiveScore = (typeof BJ_PLAYER_IS_SOFT !== 'undefined' && BJ_PLAYER_IS_SOFT)
            ? BJ_PLAYER_HARD + 10
            : BJ_PLAYER_HARD
        if (typeof BJ_PLAYER_HARD !== 'undefined' && effectiveScore >= 17) {
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

    $('.chip-250').click(function () {
        calcChip(CHIP_S, false)
    })

    $('.chip-500').click(function () {
        calcChip(CHIP_M, false)
    })

    $('.chip-1000').click(function () {
        calcChip(CHIP_L, false)
    })

    $('.chip-2500').click(function () {
        calcChip(CHIP_XL, false)
    })

    $('.chip-5000').click(function () {
        calcChip(CHIP_2XL, false)
    })

    $('.chip-10000').click(function () {
        calcChip(CHIP_3XL, false)
    })

    $('.btn-chip-double').click(function () {
        calcChip(null, true)
    })

    /* Allow clear when chips are staged, or when a game has been played (reset after hand) */
    $('.form-clear').on('submit', function (e) {
        var betVal = parseFloat($('.curr-bet-value').val())
        var hasStaged = !isNaN(betVal) && betVal > 0
        if (!hasStaged && !BJ_GAME_DEALT) {
            e.preventDefault()
            return false
        }
    })

    /* Prevent deal submission when bet is below minimum */
    $('.form-deal').on('submit', function (e) {
        var betVal = parseFloat($('.curr-bet-value').val())
        if (isNaN(betVal) || betVal < MIN_BET) {
            e.preventDefault()
            return false
        }
    })

    /* Keep deal button appearance in sync with current bet */
    refreshDealButton()
})

function refreshDealButton() {
    var betVal = parseFloat($('.curr-bet-value').val())
    var dealBtn = $('.btn-deal')
    if (isNaN(betVal) || betVal < MIN_BET) {
        dealBtn.addClass('disabled')
    } else {
        dealBtn.removeClass('disabled')
    }
}

function calcChip(chipValue, doubleChip) {
    let hiddenBetField = $('.curr-bet-value')[0]

    let currBetElem = $('.curr-bet')[0]
    let balanceElem = $('.balance')[0]
    let currBet = currBetElem.innerText
    let currBalance = balanceElem.innerText

    let currency = currBet.match(/[^\d,]/g).join('').trim()
    let amountBet = currBet.replace(/[^0-9]+/g, '')
    let amountBalance = currBalance.replace(/[^0-9]+/g, '')

    amountBet = amountBet.splice(amountBet.length - 2, 0, '.')
    amountBalance = amountBalance.splice(amountBalance.length - 2, 0, '.')

    if (+amountBalance === 0.0) {
        return
    }

    let newBet, newBalance
    if (doubleChip) {
        newBet = +amountBet * 2
        newBalance = +amountBalance - +amountBet
    } else {
        newBet = (+amountBet + chipValue)
        newBalance = (+amountBalance - chipValue)
    }

    if (newBet > MAX_BET) {
        let diff = newBet - MAX_BET
        newBet = MAX_BET
        newBalance += diff
    }

    if (newBalance < 0.0) {
        if (doubleChip) {
            newBet = +amountBet + +amountBalance
        } else {
            let diff = chipValue + newBalance
            newBet = +amountBet + diff
        }
        newBalance = 0.0
    }

    if (newBet < MIN_BET) {
        currBetElem.classList.add('low-bet')
    } else {
        currBetElem.classList.remove('low-bet')
    }

    let newBetStr = newBet.toFixed(2)
    let newBalanceStr = newBalance.toFixed(2)

    let resultBet = newBetStr
    let resultBalance = newBalanceStr

    if (currency.startsWith('$')) {
        currency = currency.replace(/.$/, '')

        for (let i = newBetStr.length - 6; i > 0; i -= 3) {
            resultBet = resultBet.splice(i, 0, ',')
        }

        for (let i = newBalanceStr.length - 6; i > 0; i -= 3) {
            resultBalance = resultBalance.splice(i, 0, ',')
        }

        resultBet = currency + resultBet
        resultBalance = currency + resultBalance
    } else {
        resultBet = resultBet.replace('.', ',')
        resultBet = resultBet + ' ' + currency
        resultBalance = resultBalance + ' ' + currency
    }

    currBetElem.innerText = resultBet
    balanceElem.innerText = resultBalance
    hiddenBetField.value = newBet

    refreshDealButton()
}

String.prototype.splice = function (start, delCount, newSubStr) {
    return this.slice(0, start) + newSubStr + this.slice(start + Math.abs(delCount))
}
