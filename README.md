# Push365 Casino

A full-stack Spring Boot casino web application featuring Blackjack with complete user management, wallet system, email verification, and support for 10 languages. Built as a portfolio project.

---

## Screenshots

### Lobby

| Light — Desktop | Dark — Desktop |
|---|---|
| ![Lobby Light Desktop](assets/screenshots/2026-08/lobby-light-desktop.png) | ![Lobby Dark Desktop](assets/screenshots/2026-08/lobby-dark-desktop.png) |

| Light — Mobile | Dark — Mobile |
|---|---|
| ![Lobby Light Mobile](assets/screenshots/2026-08/lobby-light-mobile.png) | ![Lobby Dark Mobile](assets/screenshots/2026-08/lobby-dark-mobile.png) |

Authenticated lobby (dark):

![Lobby Authenticated Dark Desktop](assets/screenshots/2026-08/lobby-auth-dark-desktop.png)

---

### Login & Register

![Login flow](assets/screenshots/2026-08/login.gif)

| Login Light | Login Dark | Login Dark Mobile |
|---|---|---|
| ![Login Light](assets/screenshots/2026-08/login-light-desktop.png) | ![Login Dark](assets/screenshots/2026-08/login-dark-desktop.png) | ![Login Dark Mobile](assets/screenshots/2026-08/login-dark-mobile.png) |

Settings panel open (language + theme toggle):

![Settings Open](assets/screenshots/2026-08/login-settings-open.png)

![Register flow](assets/screenshots/2026-08/register.gif)

| Register Light | Register Dark | Register Mobile |
|---|---|---|
| ![Register Light](assets/screenshots/2026-08/register-light-desktop.png) | ![Register Dark](assets/screenshots/2026-08/register-dark-desktop.png) | ![Register Light Mobile](assets/screenshots/2026-08/register-light-mobile.png) |

---

### Deposit

![Deposit flow](assets/screenshots/2026-08/deposit.gif)

---

### Gameplay — Blackjack

| Table — Idle | Table — Active |
|---|---|
| ![Table Idle](assets/screenshots/2026-08/play-table-idle.png) | ![Table Active](assets/screenshots/2026-08/play-table-active.png) |

| Double Down | Double Down Win |
|---|---|
| ![Double Down](assets/screenshots/2026-08/play-double-down.png) | ![Double Down Win](assets/screenshots/2026-08/play-double-down-win.png) |

| Split — Active | Split — Ready |
|---|---|
| ![Split Active](assets/screenshots/2026-08/play-split-active.png) | ![Split Ready](assets/screenshots/2026-08/play-split-ready.png) |

| Even Money Offer | Push |
|---|---|
| ![Even Money](assets/screenshots/2026-08/play-bj-even-money.png) | ![Push](assets/screenshots/2026-08/play-bj-push.png) |

Result overlay:

![Result Overlay](assets/screenshots/2026-08/play-result-overlay.png)

#### In-game Modals

| Strategy Chart | Payouts | How to Play |
|---|---|---|
| ![Strategy Modal](assets/screenshots/2026-08/play-strategy-modal.png) | ![Payouts Modal](assets/screenshots/2026-08/play-payouts-modal.png) | ![How to Play Modal](assets/screenshots/2026-08/play-how-to-play-modal.png) |

---

### Hand History Panel

The collapsible **Last 10 Hands** panel shows every completed hand in reverse chronological order — cards dealt, action taken, bet staked, and payout received, colour-coded by outcome (green = win, red = loss, neutral = push).

![Hand History Panel](assets/screenshots/2026-08/play-history-panel.png)

---

### RTP Simulator

The admin **Blackjack RTP Simulator** runs up to 10 million hands in parallel (up to 4 threads) and reports Return-to-Player, house edge, win/loss/push/blackjack counts, and elapsed time. Two strategies are available.

#### Dealer Mirror — ~94.8% RTP

Copies the dealer rule exactly: hit ≤ 16, stand ≥ 17 (including soft 17). No doubles, splits, or surrender. The missing player edge from skipping these moves produces a ~5.5% house advantage.

![Dealer Mirror Simulation](assets/screenshots/2026-08/sim-dealer-mirror.png)

#### Basic Strategy — ~99.7% RTP

Mathematically optimal multi-deck S17 decisions for every player hand vs every dealer upcard — correct soft-hand play, double-downs on 9/10/11, and all pair splits. Reduces the house edge to ~0.5%.

![Basic Strategy Simulation](assets/screenshots/2026-08/sim-basic-strategy.png)

#### Strategy Comparison Modal

Click the ⓘ button next to **Player strategy** to open a side-by-side comparison of both strategies, including typical decision differences and expected RTP.

![Strategy Comparison Modal](assets/screenshots/2026-08/sim-strategy-modal.png)

#### Side Bet RTP — Analytical Exact Combinatorics

Below the simulation results the page shows exact combinatorial RTP calculations for both side bets (Perfect Pairs and 21+3) across infinite, 6-deck, and 8-deck shoes, complete with formula breakdowns.

![RTP Analytical](assets/screenshots/2026-08/sim-rtp-analytical.png)

---

### Original Design (June 2024)

The first version of the site had a different visual identity before the current casino-dark redesign.

| Lobby | Login | Register |
|---|---|---|
| ![Original Lobby](assets/screenshots/2024-06/01-lobby.png) | ![Original Login](assets/screenshots/2024-06/02-login.gif) | ![Original Register](assets/screenshots/2024-06/03-register.gif) |

| Deposit | Gameplay |
|---|---|
| ![Original Deposit](assets/screenshots/2024-06/04-deposit.gif) | ![Original Gameplay](assets/screenshots/2024-06/05-play.gif) |

---

## Features

### Game
- Hit, Stand, Double Down, Split, Surrender
- Insurance and Even Money options
- Dealer plays to soft 17
- Natural Blackjack pays 3:2 — Insurance pays 2:1
- Basic Strategy advisor with deviation confirmation prompt
- Auto-play button to let the dealer resolve the hand
- Persistent game state — unfinished games resume on next visit
- Collapsible **Last 10 Hands** history panel with cards, actions, bets, and payouts

### RTP Simulator (Admin)
- Simulates up to 10 million hands with configurable bet size (£0.10 – £10,000)
- Multi-threaded execution (1 – 4 threads) using a `FixedThreadPool`
- Thread-safe RNG via `ThreadLocal<MersenneTwister>`
- Strategies: **Dealer Mirror** (~94.5% RTP) and **Basic Strategy** (~99.5% RTP)
- Reports: RTP, total wagered/returned, wins, losses, pushes, blackjacks, elapsed time
- Loading skeleton animation while simulation runs; results fade in on completion

### User Management
- Registration with email verification (activation link, 60 min expiry)
- Forgot password with email reset (30 min expiry)
- Role-based access: REGULAR, MODERATOR, ADMIN
- Profile management

### Wallet & Payments
- Deposit via credit card (Visa, Mastercard, Amex, Discover, Troy)
- Cash-out to registered card
- Luhn algorithm validation + expiration date check
- Up to 3 cards per user

### Security
- Spring Security with BCrypt password hashing
- CSRF protection on all forms
- Google reCAPTCHA v2 on registration and login
- Remember-me persistent login
- Session fixation protection

### UI / UX
- Light ♥ and dark ♠ themes — persisted in localStorage, default light
- Responsive layout — hamburger nav on mobile
- Live weather widget (Sofia, Las Vegas, Monte Carlo, Macao) via OpenWeatherMap
- Navbar settings dropdown: language switcher + theme toggle

### Internationalization
10 languages — switched via cookie, no page reload required:

| Code | Language |
|------|----------|
| `en` | English |
| `bg` | Български (Bulgarian) |
| `de` | Deutsch (German) |
| `el` | Ελληνικά (Greek) |
| `es` | Español (Spanish) |
| `it` | Italiano (Italian) |
| `ja` | 日本語 (Japanese) |
| `pl` | Polski (Polish) |
| `ru` | Русский (Russian) |
| `zh` | 中文 (Chinese) |

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 25 |
| Framework | Spring Boot 3.4.5 |
| Security | Spring Security 6 |
| Template | Thymeleaf |
| Database | MySQL + Hibernate JPA |
| Migrations | Liquibase |
| Email | JavaMailSender + MailHog (dev) |
| HTTP Client | WebClient |
| Validation | Hibernate Validator + custom annotations |
| Build | Gradle |
| Infrastructure | Docker (MySQL + MailHog) |

---

## Architecture

### MVC + Post-Redirect-Get

Every game action is a POST that mutates state, followed by a redirect to `GET /play`. The GET call re-hydrates the game from the database and renders the updated view. This prevents double-submission on refresh.

### Game Engine — Chain of Responsibility

Game logic is implemented as a **Chain of Responsibility** in the `processor` package. `GameStateProcessorChain` holds an ordered list of single-purpose processors. On each request it walks the list; the first processor whose `canProcess()` returns `true` handles the request exclusively and the chain stops.

Every processor receives a `GameContext` record containing the in-memory `Game` DTO, the JPA entities, repositories, `BasicStrategy`, an injectable clock, and the Jackson `ObjectMapper`.

```
── Wallet / persistence processors ──────────────────────────────────────────
 1. RepeatLastBetProcessor            CHOICE_REPEAT_LAST_BET
 2. RepeatLastBetAgainProcessor       CHOICE_REPEAT_LAST_BET_AGAIN
 3. ClearLastBetProcessor             CHOICE_CLEAR_LAST_BET
 4. InsuranceBetProcessor             CHOICE_INSURANCE_YES
 5. DoubleDownBetProcessor            CHOICE_DOUBLE_DOWN
 6. DoubleDownYesWalletProcessor      CHOICE_DOUBLE_DOWN_YES
 7. SplitBetProcessor                 CHOICE_SPLIT
 8. InsufficientFundsReCheckProcessor CHOICE_INSURANCE_YES_NOT_ENOUGH_MONEY
                                      CHOICE_DOUBLE_DOWN_NOT_ENOUGH_MONEY
 9. ErrorPassthroughProcessor         any non-empty errCodeList
10. FinalizedPayoutProcessor          gameEntity.finalized == true

── Pure game-logic processors ───────────────────────────────────────────────
11. NotDealtOrFinalizedProcessor      !game.dealt || game.finalized
12. AutoFinalizeProcessor             CHOICE_AUTO_FINALIZE
13. DoubleDownConfirmProcessor        CHOICE_DOUBLE_DOWN_YES / CHOICE_DOUBLE_DOWN_NO
14. SurrenderProcessor                CHOICE_SURRENDER
15. PlayerBlackjackAfterDealProcessor CHOICE_DEAL + player has blackjack
16. EvenMoneyProcessor                CHOICE_EVEN_MONEY_YES / CHOICE_EVEN_MONEY_NO
17. HitProcessor                      CHOICE_HIT
18. StandProcessor                    CHOICE_STAND
19. InsuranceProcessor                CHOICE_INSURANCE_YES / CHOICE_INSURANCE_NO
20. InitialDealSetupProcessor         fallthrough — always last
```

### Card Source — Injectable RNG Seam

All card drawing goes through a `CardSource` interface:

- **`RngCardSource`** — production: real random shuffle
- **`FixedCardSource`** — testing: drains a pre-loaded queue in exact order

The active source is controlled by `game.deck-scenario` in `application.yml`. Set to `RANDOM` in production; change to any `DeckScenario` enum value for deterministic scenario testing.

### GameService — Thin Orchestrator

`GameService` only: loads entities, deserializes the JSON game blob, builds `GameContext`, calls `processorChain.process(ctx)`, saves the result, returns `game` to the controller.

### Key Custom Validations

| Annotation | Purpose |
|-----------|---------|
| `@UniqueEmail` / `@UniqueUsername` | DB-level duplicate check at validation time |
| `@CustomCreditCardNumber` | Luhn algorithm |
| `@FutureExpirationDate` | Card expiry |
| `@MinAge` | 18+ age gate |
| `@FieldMatch` | Password confirmation |

### Database Schema

| Table | Purpose |
|-------|---------|
| `users` | Accounts |
| `roles` | Authorization roles |
| `wallets` | Balances and current bet |
| `credit_cards` | Registered cards |
| `last_games` | Active (in-progress) game state as JSON |
| `played_games` | Completed game history |
| `bet_history` | Per-game bet records |
| `activation_tokens` | Email verification tokens |
| `reset_pass_tokens` | Password reset tokens |

---

## Quick Start

### Prerequisites

- Java 25+
- Docker (for MySQL + MailHog)

### Run

```bash
# Clone and enter the project
cd BJ

# Start MySQL (port 3306) and MailHog (SMTP 1025 / UI 8025)
docker-compose up -d

# Start the application
./gradlew bootRun
```

Open `http://localhost:8080`.

### Dev credentials

| Field | Value |
|-------|-------|
| Username | `pesho` |
| Password | `1234` |

### Access points

| Service | URL |
|---------|-----|
| Application | http://localhost:8080 |
| MailHog UI | http://localhost:8025 |
| MySQL | localhost:3306 / db `bjdb` / user `root` / no password |

### Key configuration (`.env` or `application.yml`)

```properties
google.recaptcha.enabled=false      # disable reCAPTCHA during local dev
game.deck-scenario=RANDOM           # change to a DeckScenario enum value for fixed hands
auth.activation-token.expires-after-minutes=60
auth.forgot-password-token.expires-after-minutes=30
```

---

## Endpoints

| Visibility | Path | Description |
|-----------|------|-------------|
| Public | `/` | Lobby |
| Public | `/auth/login` | Login |
| Public | `/auth/register` | Registration |
| Public | `/auth/reset` | Password reset request |
| Public | `/rules` | Game rules |
| Protected | `/play/**` | Blackjack gameplay |
| Protected | `/credit-card/**` | Deposit / cash-out |
| Protected | `/auth/activation` | Account activation |
| Protected | `/auth/reset_pass` | Password reset |
| Admin | `/admin/simulation` | RTP Simulator |

---

## Icons

Navbar SVG icons are from [Tabler Icons](https://tabler.io/icons) (MIT License).

| Icon | Tabler name |
|------|-------------|
| Lobby (cocktail glass) | `glass-cocktail` |
| Deposit (credit card) | `credit-card` |
| Cash out (banknote) | `cash-banknote` |
| Play (playing cards) | `cards` |

Game decision button icons — the Auto Play button composes two Tabler icons: `rotate-clockwise` (circular arrow) + `player-play` (filled triangle).

The Rules (spade) icon is a custom SVG.

---

## License

MIT License. Educational portfolio project. Use responsibly.
