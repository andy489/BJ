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

    $('.btn-chip-double').click(function () {
        calcChip(null, true)
    })
})

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
}

String.prototype.splice = function (start, delCount, newSubStr) {
    return this.slice(0, start) + newSubStr + this.slice(start + Math.abs(delCount))
}