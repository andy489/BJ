/* ── bj-play-render.js ────────────────────────────────────────────────────
   DOM renderer for AJAX game state updates.
   Maps GameStateDto JSON → DOM mutations.
   Works alongside the existing Thymeleaf SSR; only runs after a fetch response.
──────────────────────────────────────────────────────────────────────────── */

var BJ_RENDER = (function () {

    /* Choice constants (mirror GameUtil.java) */
    var C = {
        CHIP_OPS:   0, SURRENDER: 1, SPLIT: 2, DOUBLE_DOWN: 3,
        DD_YES: 6, DD_NO: 7, STAND: 8, HIT: 9, DEAL: 10,
        EVEN_YES: 11, EVEN_NO: 12, INS_YES: 13, INS_NO: 15,
        AUTO_FINALIZE: 19, AUTO_PLAY: 20, SPLIT_DD_ADVANCE: 21
    }

    var ERR = { INSUFFICIENT_FUNDS: 0, INVALID_BET: 1, LOW_BET: 2, HIGH_BET: 3 }

    var RANK_LABEL = { 1:'A',2:'2',3:'3',4:'4',5:'5',6:'6',7:'7',8:'8',9:'9',10:'10',11:'J',12:'Q',13:'K' }
    var SUIT_SYM   = { 0:'♣',1:'♦',2:'♥',3:'♠' }
    var IMAGE_BASE = './../images/playing_card/'
    var DISP_BASE  = 7

    function fmt(bd) {
        var n = parseFloat(bd) || 0
        return '£' + n.toLocaleString('en-GB', {minimumFractionDigits:2, maximumFractionDigits:2})
    }

    function cardLabel(c) {
        return (RANK_LABEL[c.rank] || '?') + (SUIT_SYM[c.suit] || '?')
    }

    function cardImg(c, cls) {
        var img = document.createElement('img')
        img.src = IMAGE_BASE + c.rank + '-' + c.suit + '.png'
        img.alt = ''
        img.className = 'play-card' + (cls ? ' ' + cls : '')
        return img
    }

    function backImg(cls) {
        var img = document.createElement('img')
        img.src = IMAGE_BASE + 'back-2.png'
        img.alt = ''
        img.className = 'play-card' + (cls ? ' ' + cls : '')
        return img
    }

    /* ── Card displacement helpers ── */
    function parity(n) { return n % 2 }
    function dBase(n)  { return DISP_BASE - Math.floor(n / 2) }

    /* ── Render dealer cards ── */
    function renderDealerCards(state) {
        var box = document.querySelector('.dealer-cards .dealer-cards-box')
        if (!box) return

        var wrapper = box.querySelector('.play-card-wrapper')
        if (!wrapper) {
            wrapper = document.createElement('div')
            wrapper.className = 'play-card-wrapper'
            box.insertBefore(wrapper, box.querySelector('.score-box'))
        }
        wrapper.innerHTML = ''

        var cards = state.dealerCards || []
        if (cards.length === 1) {
            wrapper.className = 'play-card-wrapper'
            // Thymeleaf uses dealerCardsOdd = (size+1)%2, which counts the hidden card,
            // so 1 visible card → (1+1)%2 = 0 (even). Classes are d-0-6, d-0-7.
            var par = (cards.length + 1) % 2
            wrapper.appendChild(cardImg(cards[0], 'd-' + par + '-6'))
            wrapper.appendChild(backImg('d-' + par + '-7'))
        } else {
            wrapper.className = 'play-card-wrapper'
            var par = parity(cards.length)
            var base = dBase(cards.length)
            cards.forEach(function(c, i) {
                wrapper.appendChild(cardImg(c, 'd-' + par + '-' + (base + i)))
            })
        }

        var scoreEl = box.querySelector('.curr-result-box')
        if (scoreEl && state.dealerScore) scoreEl.textContent = state.dealerScore
    }

    /* ── Render player cards (non-split) ── */
    function renderPlayerCards(state) {
        var cards = state.playerCards || []
        var doubleDown = state.doubleDown

        var normalBox = document.querySelector('.player-cards-box')
        var splitWrapper = document.querySelector('.split-hands-wrapper')

        var inSplit = state.splitActive || (state.splitHands && state.splitHands.length > 0)

        if (inSplit) {
            if (normalBox) normalBox.style.display = 'none'
            renderSplitHands(state)
        } else {
            if (splitWrapper) splitWrapper.style.display = 'none'
            if (!normalBox) {
                normalBox = document.createElement('div')
                normalBox.className = 'player-cards-box'
                document.querySelector('.player-cards').appendChild(normalBox)
            }
            normalBox.style.display = ''

            var wrapper = normalBox.querySelector('.play-card-wrapper, .double-down-wrapper')
            if (!wrapper) {
                wrapper = document.createElement('div')
                wrapper.className = 'play-card-wrapper'
                normalBox.insertBefore(wrapper, normalBox.querySelector('.score-box'))
            }
            wrapper.innerHTML = ''

            if (!doubleDown) {
                wrapper.className = 'play-card-wrapper'
                var par = parity(cards.length)
                var base = dBase(cards.length)
                cards.forEach(function(c, i) {
                    wrapper.appendChild(cardImg(c, 'd-' + par + '-' + (base + i)))
                })
            } else {
                wrapper.className = 'play-card-wrapper double-down-wrapper'
                var upright = cards.slice(0, cards.length - 1)
                var ddCard  = cards[cards.length - 1]
                var par = parity(upright.length)
                var base = dBase(upright.length)
                upright.forEach(function(c, i) {
                    wrapper.appendChild(cardImg(c, 'd-' + par + '-' + (base + i)))
                })
                wrapper.appendChild(cardImg(ddCard, 'double-down-card'))
            }

            var scoreEl = normalBox.querySelector('.curr-result-box')
            if (scoreEl && state.playerScore) scoreEl.textContent = state.playerScore
        }
    }

    /* ── Render split hands ── */
    function renderSplitHands(state) {
        var splitHands = state.splitHands || []
        var container  = document.querySelector('.player-cards')
        if (!container) return

        var existing = container.querySelector('.split-hands-wrapper')
        if (!existing) {
            existing = document.createElement('div')
            existing.className = 'split-hands-wrapper'
            container.appendChild(existing)
        }
        existing.style.display = ''
        existing.innerHTML = ''

        var activeIdx    = state.activeSplitHandIndex || 0
        var ddFlags      = state.splitDoubleDownFlags || []
        var scores       = state.splitScores || []
        var multipliers  = state.splitHandMultipliers || []

        splitHands.forEach(function(hand, idx) {
            var box = document.createElement('div')
            box.className = 'split-hand-box' + (idx === activeIdx ? ' active-hand' : '')
            box.setAttribute('data-hand-index', idx)

            var label = document.createElement('div')
            label.className = 'split-hand-label'
            label.textContent = 'Hand ' + (splitHands.length - idx)
            box.appendChild(label)

            var isDD = state.doubleDown && idx === activeIdx
                    || (idx !== activeIdx && ddFlags[idx])

            var hSize = hand.length
            var par   = parity(hSize)
            var base  = dBase(hSize)

            var wrapper = document.createElement('div')
            wrapper.className = isDD ? 'play-card-wrapper double-down-wrapper' : 'play-card-wrapper'

            var upright = isDD ? hand.slice(0, hand.length - 1) : hand
            upright.forEach(function(c, i) {
                wrapper.appendChild(cardImg(c, 'd-' + parity(upright.length) + '-' + (dBase(upright.length) + i)))
            })
            if (isDD && hand.length > 0) {
                wrapper.appendChild(cardImg(hand[hand.length - 1], 'double-down-card'))
            }
            box.appendChild(wrapper)

            var scoreBox = document.createElement('div')
            scoreBox.className = 'score-box'
            var scoreSpan = document.createElement('div')
            scoreSpan.className = 'curr-result-box'
            scoreSpan.textContent = scores[idx] || ''
            scoreBox.appendChild(scoreSpan)
            box.appendChild(scoreBox)

            existing.appendChild(box)
        })
    }

    /* ── Render scores ── */
    function renderScores(state) {
        // dealer score rendered in renderDealerCards
        // player score rendered in renderPlayerCards
    }

    /* ── Render action buttons ── */
    function renderButtons(state) {
        var avail = state.availableChoices || []

        setEnabled('#btn-surrender-trigger', avail.includes(C.SURRENDER))
        setEnabled('#btn-split-trigger',    avail.includes(C.SPLIT))
        setEnabled('[action="/play/double-down"] button[type="submit"]', avail.includes(C.DOUBLE_DOWN))
        setEnabled('#btn-stand-trigger',    avail.includes(C.STAND))
        setEnabled('#btn-hit-trigger',      avail.includes(C.HIT))

        var hasDeal = avail.includes(C.DEAL)
        var dealBtn = document.querySelector('.btn-deal')
        if (dealBtn) {
            if (hasDeal) dealBtn.classList.remove('disabled')
            else         dealBtn.classList.add('disabled')
        }

        var chipBox = document.querySelector('.chip-box-wrapper')
        if (chipBox) {
            chipBox.classList.toggle('d-none', !avail.includes(C.CHIP_OPS))
        }

        var hasAutoPlay  = avail.includes(C.AUTO_PLAY)
        var hasAutoFin   = avail.includes(C.AUTO_FINALIZE)
        var autoBtn = document.querySelector('.btn-auto-play')
        if (autoBtn) {
            if (hasAutoPlay || hasAutoFin) autoBtn.classList.remove('disabled')
            else                           autoBtn.classList.add('disabled')
        }
        var autoForm = autoBtn && autoBtn.closest('form')
        if (autoForm) {
            autoForm.action = hasAutoPlay ? '/play/auto-play' : '/play/auto-finalize'
        }

        // Update GAME ID display (second .game-metadata span — first is username)
        var allMeta = document.querySelectorAll('.game-metadata')
        var hashEl = allMeta.length >= 2 ? allMeta[1] : null
        if (hashEl && state.hash) {
            hashEl.textContent = state.hash
            if (state.hash === 'NO ID') hashEl.classList.add('gray')
            else                        hashEl.classList.remove('gray')
        }
    }

    function setEnabled(selector, enabled) {
        var el = document.querySelector(selector)
        if (!el) return
        if (enabled) el.classList.remove('disabled')
        else         el.classList.add('disabled')
    }

    /* ── Render server-side modals (even money, insurance, double-down confirm, errors) ── */
    function renderModals(state) {
        var avail    = state.availableChoices || []
        var errCodes = state.errCodeList || []

        var overlay = document.querySelector('.modal-overlay')

        var showEven = avail.includes(C.EVEN_YES)
        var showIns  = avail.includes(C.INS_NO)
        var showDD   = avail.includes(C.DD_NO)
        var showErr  = errCodes.length > 0

        toggleModal('.choice-modal-wrapper:has(form[action="/play/even"])',  showEven)
        toggleModal('.choice-modal-wrapper:has(form[action="/play/insurance"])', showIns)
        toggleModal('.choice-modal-wrapper:has(form[action="/play/dd-confirm"])', showDD)

        // insurance YES button visibility
        var insYesCol = document.querySelector('.choice-modal-wrapper:has(form[action="/play/insurance"]) .col-4:first-of-type')
        if (insYesCol) insYesCol.classList.toggle('d-none', !avail.includes(C.INS_YES))

        // error modal
        var errModal = document.querySelector('.err-modal-wrapper:not(#no-funds-modal):not(#low-bet-modal)')
        if (errModal) {
            if (showErr) {
                errModal.classList.remove('d-none')
                errModal.classList.toggle('err-modal-wrapper-dep-link', errCodes.includes(ERR.INSUFFICIENT_FUNDS))
                // update error text
                var titleEls = errModal.querySelectorAll('.modal-title')
                titleEls.forEach(function(el) { el.textContent = '' })
                var firstTitle = errModal.querySelector('.modal-title')
                if (firstTitle) {
                    firstTitle.textContent = errCodeText(errCodes[0])
                }
                // deposit button visibility
                var depBox = errModal.querySelectorAll('.choice-box')[1]
                if (depBox) depBox.classList.toggle('d-none', !errCodes.includes(ERR.INSUFFICIENT_FUNDS))
            } else {
                errModal.classList.add('d-none')
            }
        }

        var anyActive = showEven || showIns || showDD || showErr
        if (overlay) overlay.classList.toggle('active', anyActive)
    }

    function errCodeText(code) {
        var msgs = {
            0: 'INSUFFICIENT FUNDS!',
            1: 'INVALID BET!',
            2: 'BET TOO LOW! Minimum bet is £10.00.',
            3: 'BET TOO HIGH! Maximum bet is £1,000.00.'
        }
        return msgs[code] || 'ERROR!'
    }

    function toggleModal(selector, show) {
        var el = document.querySelector(selector)
        if (!el) return
        if (show) el.classList.remove('d-none')
        else      el.classList.add('d-none')
    }

    /* ── Render wallet ── */
    function renderWallet(state) {
        var w = state.wallet
        if (!w) return

        // balance
        var balEl = document.querySelector('.balance')
        if (balEl) balEl.textContent = fmt(w.balance)

        // main bet circle
        var mainCircleAmt = document.querySelector('#bet-circle-main .bet-circle-amount.curr-bet')
        if (mainCircleAmt) mainCircleAmt.textContent = fmt(w.currentBet)

        // total stake
        var totalStake = parseFloat(w.currentBet||0) + parseFloat(w.perfectPairsBet||0)
                       + parseFloat(w.twentyOneThreeBet||0) + parseFloat(w.dealerPerfectPairsBet||0)
        var tsEl = document.querySelector('.total-stake')
        if (tsEl) tsEl.textContent = fmt(totalStake)

        // hidden bet input
        var betInput = document.querySelector('.curr-bet-value')
        if (betInput) betInput.value = parseFloat(w.currentBet) || 0

        // last win
        var lwEl = document.querySelector('.last-bet-box-value span, .balance-box-value ~ .cash-stat .last-bet-box-value')
        // Target "Last Win" display by structure — 2nd cash-stat
        var cashStats = document.querySelectorAll('.cash-stat')
        if (cashStats[1]) {
            var lwVal = cashStats[1].querySelector('.last-bet-box-value span')
            if (lwVal) lwVal.textContent = fmt(w.lastWin)
        }
        // Last Bet — 3rd cash-stat
        if (cashStats[2]) {
            var lbVal = cashStats[2].querySelector('.last-bet-box-value span')
            if (lbVal) {
                var lastTotal = parseFloat(w.lastTotalBet) || parseFloat(w.lastBet) || 0
                lbVal.textContent = fmt(lastTotal)
            }
        }

        // side bet circles (server-committed amounts during hand)
        if (state.dealt) {
            updateSideCircleDisplay('pp',  parseFloat(w.perfectPairsBet)       || 0)
            updateSideCircleDisplay('213', parseFloat(w.twentyOneThreeBet)      || 0)
            updateSideCircleDisplay('dpp', parseFloat(w.dealerPerfectPairsBet) || 0)
        }

        // bet circles wrapper lock state
        var bcw = document.querySelector('.bet-circles-wrapper')
        if (bcw) {
            var avail = state.availableChoices || []
            bcw.classList.toggle('bet-circles-wrapper--locked', !avail.includes(0))
            bcw.classList.toggle('bet-circles-wrapper--in-play', state.dealt)
        }

        // deal button re-check after wallet update — skip when server already granted DEAL
        var serverGrantsDeal = (state.availableChoices || []).includes(C.DEAL)
        if (!serverGrantsDeal && typeof refreshDealButton === 'function') refreshDealButton()
    }

    /* ── Render result overlay ── */
    function renderResultOverlay(state) {
        var overlay = document.getElementById('result-overlay')

        if (state.finalized) {
            if (!overlay) {
                overlay = document.createElement('div')
                overlay.id = 'result-overlay'
                overlay.className = 'result-overlay'
                overlay.style.visibility = 'hidden'
                var cardsWrapper = document.getElementById('cardsWrapper')
                var gameWrapper  = cardsWrapper && cardsWrapper.querySelector('.game-wrapper')
                if (gameWrapper) gameWrapper.appendChild(overlay)
            }

            // Set result label
            overlay.innerHTML = ''
            var m = parseFloat(state.handMultiplier) || 0
            var span = document.createElement('span')
            if (m > 1.0) {
                span.className = 'result-overlay-label result-overlay--win'
                span.textContent = 'WIN'
            } else if (m === 1.0) {
                span.className = 'result-overlay-label result-overlay--push'
                span.textContent = 'PUSH'
            } else {
                span.className = 'result-overlay-label result-overlay--loss'
                span.textContent = 'LOSS'
            }
            overlay.appendChild(span)

            // Show and auto-dismiss
            var fadeDuration = 600
            var displayMs    = (typeof BJ_RESULT_DISPLAY_MS !== 'undefined') ? BJ_RESULT_DISPLAY_MS : 2500
            setTimeout(function() {
                overlay.style.visibility = ''
                var displayDelay = displayMs - fadeDuration
                if (displayDelay < 0) displayDelay = 0
                setTimeout(function() {
                    overlay.classList.add('result-overlay--fade-out')
                    setTimeout(function() {
                        overlay.remove()
                        // Reset stale finalized state so Clear/Repeat work client-side again
                        if (typeof BJ_FINALIZED   !== 'undefined') BJ_FINALIZED   = false
                        if (typeof BJ_LAST_CHOICE !== 'undefined') BJ_LAST_CHOICE = -1
                    }, fadeDuration)
                }, displayDelay)
            }, 50)
        } else {
            if (overlay) overlay.remove()
        }
    }

    /* ── Render bet history ── */
    function renderBetHistory(state) {
        if (!state.betHistory) return

        var panelWrapper = document.querySelector('.history-panel-wrapper')
        if (!panelWrapper) return

        var panel = document.getElementById('historyPanel')
        if (!panel) return

        if (state.betHistory.length === 0) {
            panel.innerHTML = '<div class="history-empty">No hands played yet</div>'
            return
        }

        var list = document.createElement('div')
        list.className = 'history-cards-list'

        state.betHistory.forEach(function(h, idx) {
            var sign = h.resultSign
            var rowClass = sign > 0 ? 'hist-win' : (sign === 0 ? 'hist-push' : 'hist-loss')
            var row = document.createElement('div')
            row.className = 'hist-row ' + rowClass

            // header
            var header = document.createElement('div')
            header.className = 'hist-row-header'
            header.innerHTML =
                '<span class="hist-row-num">' + (idx + 1) + '</span>' +
                '<span class="hist-row-time">' + (h.finalizedTime ? formatHistTime(h.finalizedTime) : '-') + '</span>' +
                '<span class="hist-row-amounts">' +
                    '<span>' + fmt(h.totalBet) + '</span>' +
                    '<span class="hist-arrow">→</span>' +
                    '<span>' + fmt(h.returnAmount) + '</span>' +
                '</span>' +
                (sign > 0  ? '<span class="hist-badge hist-badge-win">Win</span>'  : '') +
                (sign === 0 ? '<span class="hist-badge hist-badge-push">Push</span>' : '') +
                (sign < 0  ? '<span class="hist-badge hist-badge-loss">Loss</span>' : '')
            row.appendChild(header)

            // cards
            var cardsDiv = document.createElement('div')
            cardsDiv.className = 'hist-row-cards'
            if (h.splitHandViews && h.splitHandViews.length > 0) {
                h.splitHandViews.forEach(function(sh) {
                    var g = document.createElement('div')
                    g.className = 'hist-hand-group'
                    g.innerHTML = '<span class="hist-hand-label">Hand ' + sh.handNumber + ':</span>' +
                        sh.cardLabels.map(function(c) {
                            return '<span class="' + histCardClass(c) + '">' + escHtml(c) + '</span>'
                        }).join('') +
                        '<span class="hist-badge ' + shResultBadgeClass(sh) + '">' + sh.resultLabel + '</span>'
                    cardsDiv.appendChild(g)
                })
                var dg = document.createElement('div')
                dg.className = 'hist-hand-group'
                dg.innerHTML = '<span class="hist-hand-label">Dealer:</span>' +
                    (h.dealerCardLabels || []).map(function(c) {
                        return '<span class="' + histCardClass(c) + '">' + escHtml(c) + '</span>'
                    }).join('')
                cardsDiv.appendChild(dg)
            } else {
                var pg = document.createElement('div')
                pg.className = 'hist-hand-group'
                pg.innerHTML = '<span class="hist-hand-label">You:</span>' +
                    (h.playerCardLabels || []).map(function(c) {
                        return '<span class="' + histCardClass(c) + '">' + escHtml(c) + '</span>'
                    }).join('')
                cardsDiv.appendChild(pg)
                var dg = document.createElement('div')
                dg.className = 'hist-hand-group'
                dg.innerHTML = '<span class="hist-hand-label">Dealer:</span>' +
                    (h.dealerCardLabels || []).map(function(c) {
                        return '<span class="' + histCardClass(c) + '">' + escHtml(c) + '</span>'
                    }).join('')
                cardsDiv.appendChild(dg)
            }
            row.appendChild(cardsDiv)

            // actions
            var actDiv = document.createElement('div')
            actDiv.className = 'hist-row-actions'
            ;(h.actionLabels || []).forEach(function(a) {
                var s = document.createElement('span')
                s.className = 'hist-action-tag'
                s.textContent = a
                actDiv.appendChild(s)
            })
            if (h.doubleDown)  actDiv.innerHTML += '<span class="hist-tag hist-tag-dd">DD</span>'
            if (h.split)       actDiv.innerHTML += '<span class="hist-tag hist-tag-split">Split</span>'
            if (h.insurance)   actDiv.innerHTML += '<span class="hist-tag hist-tag-ins">Ins</span>'
            row.appendChild(actDiv)

            // side bets
            var ppBet  = parseFloat(h.ppBet)  || 0
            var t3Bet  = parseFloat(h.t3Bet)  || 0
            var dppBet = parseFloat(h.dppBet) || 0
            if (ppBet > 0 || t3Bet > 0 || dppBet > 0) {
                var sbDiv = document.createElement('div')
                sbDiv.className = 'hist-row-sidebets'
                sbDiv.innerHTML = buildSideBetHtml('PP', h.ppBet, h.ppWin) +
                                  buildSideBetHtml('21+3', h.t3Bet, h.t3Win) +
                                  buildSideBetHtml('DPP', h.dppBet, h.dppWin)
                var gross = (parseFloat(h.ppWin)||0) + (parseFloat(h.t3Win)||0) + (parseFloat(h.dppWin)||0)
                if (gross > 0) {
                    sbDiv.innerHTML += '<span class="hist-sidebet-net hist-sidebet-net--win">gross ' + fmt(gross) + '</span>'
                }
                row.appendChild(sbDiv)
            }

            // initial deal
            if (h.initialPlayerCardLabels && h.initialPlayerCardLabels.length > 0) {
                var dealDiv = document.createElement('div')
                dealDiv.className = 'hist-row-initial-deal'
                dealDiv.innerHTML = '<span class="hist-hand-label">Deal:</span>' +
                    h.initialPlayerCardLabels.map(function(c) {
                        return '<span class="' + histCardClass(c) + '">' + escHtml(c) + '</span>'
                    }).join('') +
                    '<span class="hist-sidebet-label">vs</span>' +
                    (h.initialDealerCardLabels || []).map(function(c) {
                        return '<span class="' + histCardClass(c) + ' hist-card-dealer">' + escHtml(c) + '</span>'
                    }).join('')
                row.appendChild(dealDiv)
            }

            list.appendChild(row)
        })

        panel.innerHTML = ''
        panel.appendChild(list)
    }

    function buildSideBetHtml(label, bet, win) {
        var b = parseFloat(bet) || 0
        var w = parseFloat(win) || 0
        if (b <= 0) return ''
        return '<span class="hist-sidebet-label">' + label + '</span>' +
               '<span class="hist-sidebet-stake">' + fmt(b) + '</span>' +
               (w > 0
                   ? '<span class="hist-sidebet-win">+' + fmt(w) + '</span>'
                   : '<span class="hist-sidebet-loss">-' + fmt(b) + '</span>')
    }

    function shResultBadgeClass(sh) {
        if (sh.multiplier > 1.0) return 'hist-badge-win'
        if (sh.multiplier === 1.0) return 'hist-badge-push'
        return 'hist-badge-loss'
    }

    function histCardClass(c) {
        return (c.indexOf('♥') !== -1 || c.indexOf('♦') !== -1) ? 'hist-card hist-card-red' : 'hist-card'
    }

    function escHtml(s) {
        return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')
    }

    function formatHistTime(dt) {
        if (!dt) return '-'
        // dt is a pre-formatted string from BetHistoryEntryDto ("HH:mm dd/MM/yy")
        // or an ISO LocalDateTime array [year,month,day,hour,min,...]
        if (Array.isArray(dt)) {
            var d = new Date(dt[0], dt[1]-1, dt[2], dt[3]||0, dt[4]||0)
            return ('0'+d.getHours()).slice(-2) + ':' + ('0'+d.getMinutes()).slice(-2) +
                   ' ' + d.getDate() + ' ' + ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'][d.getMonth()]
        }
        // Already a formatted string — return as-is
        return dt
    }

    /* ── Apply layout helpers (match inline script behavior) ── */
    function runLayoutHelpers() {
        adaptSplitHandOverlap()
        positionDDCards()
        scaleTable()
    }

    function scaleTable() {
        var wrapper = document.getElementById('cardsWrapper')
        if (!wrapper) return
        var vw = Math.min(window.innerWidth, document.documentElement.clientWidth)
        var scale = Math.min(1, (vw - 16) / 900)
        wrapper.style.marginBottom = ''
        wrapper.style.setProperty('--scale', scale)
        if (scale < 1) {
            var naturalH = wrapper.offsetHeight
            wrapper.style.marginBottom = ((scale - 1) * naturalH) + 'px'
        }
    }

    function adaptSplitHandOverlap() {
        var MIN_STRIP_PX = 14
        Array.from(document.querySelectorAll('.split-hand-box')).forEach(function(box) {
            var uprights = Array.from(box.querySelectorAll('.play-card:not(.double-down-card)'))
            if (uprights.length < 2) return
            var cardW = uprights[0].offsetWidth
            var boxW  = box.offsetWidth - 8
            var n     = uprights.length
            var needed  = (cardW * n - boxW) / (n - 1)
            var maxOvlp = cardW - MIN_STRIP_PX
            var overlap = Math.min(Math.max(needed, 0), maxOvlp)
            for (var i = 1; i < uprights.length; i++) {
                uprights[i].style.marginLeft = (-overlap) + 'px'
            }
        })
    }

    function positionDDCards() {
        Array.from(document.querySelectorAll('.double-down-card')).forEach(function(ddCard) {
            var wrapper  = ddCard.closest('.double-down-wrapper')
            if (!wrapper) return
            var uprights = Array.from(wrapper.querySelectorAll('.play-card:not(.double-down-card)'))
            if (!uprights.length) return
            var cardH = ddCard.offsetHeight
            var cardW = ddCard.offsetWidth
            var inSplitBox = !!ddCard.closest('.split-hand-box')
            var centerX, groupBottom
            if (inSplitBox) {
                var lefts  = uprights.map(function(c) { return c.offsetLeft })
                var rights = uprights.map(function(c) { return c.offsetLeft + c.offsetWidth })
                centerX     = (Math.min.apply(null,lefts) + Math.max.apply(null,rights)) / 2
                groupBottom = Math.max.apply(null, uprights.map(function(c) { return c.offsetTop + c.offsetHeight }))
            } else {
                centerX     = wrapper.offsetWidth / 2
                groupBottom = Math.max.apply(null, uprights.map(function(c) { return c.offsetTop + c.offsetHeight }))
            }
            ddCard.style.left = (centerX - cardW / 2) + 'px'
            ddCard.style.top  = (groupBottom - cardH / 2 - cardW / 2) + 'px'
        })
    }

    /* ── Handle split-dd-advance auto-submit ── */
    function handleSplitDdAdvance(state) {
        var avail = state.availableChoices || []
        if (avail.includes(C.SPLIT_DD_ADVANCE)) {
            var sddForm = document.querySelector('form[action="/play/split-dd-advance"]')
            if (sddForm) {
                setTimeout(function() {
                    BJ_AJAX.submitForm(sddForm)
                }, 700)
            }
        }
    }

    /* ── Main entry point ── */
    function applyState(state) {
        if (state.redirectUrl) {
            window.location.href = state.redirectUrl
            return
        }

        // Update global JS vars used by bj-play.js handlers
        if (typeof BJ_GAME_DEALT    !== 'undefined') BJ_GAME_DEALT    = state.dealt
        if (typeof BJ_FINALIZED     !== 'undefined') BJ_FINALIZED     = state.finalized
        if (typeof BJ_SPLIT_ACTIVE  !== 'undefined') BJ_SPLIT_ACTIVE  = state.splitActive
        if (typeof BJ_SPLIT_DD      !== 'undefined') BJ_SPLIT_DD      = state.splitActive && state.doubleDown
        if (typeof BJ_HAND_MULTI    !== 'undefined') BJ_HAND_MULTI    = state.handMultiplier
        if (state.wallet) {
            if (typeof BJ_LAST_BET     !== 'undefined') BJ_LAST_BET     = state.wallet.lastBet
            if (typeof BJ_LAST_PP_BET  !== 'undefined') BJ_LAST_PP_BET  = state.wallet.lastPpBet
            if (typeof BJ_LAST_T3_BET  !== 'undefined') BJ_LAST_T3_BET  = state.wallet.lastT3Bet
            if (typeof BJ_LAST_DPP_BET !== 'undefined') BJ_LAST_DPP_BET = state.wallet.lastDppBet
            if (typeof BJ_PP_BET       !== 'undefined') BJ_PP_BET       = state.wallet.perfectPairsBet
            if (typeof BJ_213_BET      !== 'undefined') BJ_213_BET      = state.wallet.twentyOneThreeBet
            if (typeof BJ_DPP_BET      !== 'undefined') BJ_DPP_BET      = state.wallet.dealerPerfectPairsBet
        }
        if (state.dealt && state.playerCards) {
            var cards = state.playerCards
            var hard  = 0
            var soft  = false
            // Compute client-side hard score for bj-play.js confirmations
            var left=0, right=0
            cards.forEach(function(c) {
                if (c.rank === 1) { left+=1; right = right+11 <= 21 ? right+11 : right+1 }
                else if (c.rank >= 10) { left+=10; right+=10 }
                else { left+=c.rank; right+=c.rank }
            })
            if (right > 21) right = left
            hard = left
            soft = left !== right && right <= 21
            if (typeof BJ_PLAYER_HARD    !== 'undefined') BJ_PLAYER_HARD    = hard
            if (typeof BJ_PLAYER_IS_SOFT !== 'undefined') BJ_PLAYER_IS_SOFT = soft
        }
        if (state.dealt && state.dealerCards && state.dealerCards.length > 0) {
            var r = state.dealerCards[0].rank
            if (typeof BJ_DEALER_NOMINAL !== 'undefined')
                BJ_DEALER_NOMINAL = r === 1 ? 11 : r >= 10 ? 10 : r
        }
        if (state.dealt && state.playerCards && state.playerCards.length === 2) {
            var r = state.playerCards[0].rank
            if (typeof BJ_PAIR_NOMINAL !== 'undefined')
                BJ_PAIR_NOMINAL = r === 1 ? 11 : r >= 10 ? 10 : r
        }
        // lastChoice
        if (typeof BJ_LAST_CHOICE !== 'undefined') {
            var avail = state.availableChoices || []
            // We don't have takenChoices in GameStateDto; derive lastChoice from context
            // Keep existing BJ_LAST_CHOICE — it's only used for card animation on initial SSR
        }

        renderDealerCards(state)
        renderPlayerCards(state)
        renderButtons(state)
        renderModals(state)
        renderWallet(state)
        renderResultOverlay(state)
        if (state.betHistory) renderBetHistory(state)

        setTimeout(function() {
            runLayoutHelpers()
            handleSplitDdAdvance(state)
        }, 50)
    }

    return { applyState: applyState }

})()
