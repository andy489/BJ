# Blackjack

## Overview

A comprehensive Spring Boot web application that simulates a Blackjack casino game with complete user management, credit
card processing, email verification, and internationalization support. The application follows MVC architecture with
Spring Security for authentication and authorization.

## Core Features

### Game Features
- **Full Blackjack Game Logic** - Complete implementation with standard Blackjack rules including:
  - Hit, Stand, Double Down, Split, Surrender 
  - Insurance and Even Money options 
  - Dealer plays until soft 17 
  - Natural Blackjack payouts (2.5:1)
  - Insurance pays 2:1 
- **Betting System** - Minimum bet 10.00, maximum 1,000.00 
- **Game History** - Track all played games with bet history 
- **Persistent Game State** - Incomplete games are saved and can be resumed

### User Management
- **Registration System** - New user registration with validation 
- **Email Verification** - Account activation via email confirmation 
- **Password Reset** - Forgot password functionality with email reset links 
- **Profile Management** - User profiles with personal information 
- **Role-Based Access** - REGULAR, MODERATOR, ADMIN roles

### Payment System
- **Credit Card Registration** - Store multiple credit cards (max 3 per user)
- **Deposit Functionality** - Add funds to user wallet
- **Card Verification** - Luhn algorithm validation, expiration date checking
- **Supported Cards** - Visa, Mastercard, American Express, Discover, Troy

### Security Features
- **Spring Security** - Comprehensive security configuration
- **Remember Me** - Persistent login functionality
- **CSRF Protection** - Enabled for all forms
- **Password Encoding** - BCrypt password hashing
- **reCAPTCHA Integration** - Google reCAPTCHA v2 for form protection

### Internationalization
- **Multi-Language Support** - English and Bulgarian (български)
- **Locale Switching** - Cookie-based language preferences
- **Dynamic Content** - All user-facing text can be internationalized

### Email System
- **Registration Emails** - Account activation links
- **Password Reset Emails** - Secure password reset tokens
- **HTML Email Templates** - Responsive email designs using Thymeleaf
- **MailHog Integration** - Email testing during development

## Technical Stack
### Backend
- **Framework**: Spring Boot 3.4.5 
- **Security**: Spring Security 6.x 
- **Database**: MySQL with Hibernate JPA 
- **Migration**: Liquibase for database version control 
- **Email**: JavaMailSender with SMTP support 
- **Validation**: Hibernate Validator with custom annotations 
- **Template Engine**: Thymeleaf 
- **HTTP Client**: RestTemplate and WebClient

### Frontend
- **Templating**: Thymeleaf with Spring Security integration
- **CSS Framework**: Custom CSS with responsive design
- **JavaScript**: Client-side form validation and AJAX calls
- **reCAPTCHA**: Google reCAPTCHA integration

### Build & Deployment
- **Build Tool**: Gradle
- **Containerization**: Docker support for MySQL and MailHog
- **Java Version**: 25

## Architecture Highlights

### Design Patterns
- **MVC Pattern** — Clear separation of concerns (Controllers, Services, Repositories)
- **Post-Redirect-Get (PRG)** — Every game action is a POST that mutates state, followed by a redirect to GET `/play` which re-hydrates and renders the updated game
- **Dependency Injection** — Spring IoC container management
- **DTO Pattern** — Data transfer objects between layers; `Game` (in-memory DTO) is separate from `GameEntity` (JPA entity)
- **Repository Pattern** — Data access abstraction with Spring Data JPA
- **Chain of Responsibility Pattern** — Game logic and wallet/persistence operations are handled by an ordered chain of single-purpose processors (see below)

---

### Game Engine — Chain of Responsibility

The entire game loop is driven by a **Chain of Responsibility** implemented in the `processor` package. The central class is `GameStateProcessorChain`, which holds an ordered list of all processors and on each request walks through them until the first one whose `canProcess()` returns `true` — that processor handles the request exclusively and the chain stops.

#### GameContext

Every processor receives a `GameContext` record containing all data and infrastructure it could need:

```
GameContext
  ├── game          — the in-memory Game DTO (cards, choices, multipliers)
  ├── gameEntity    — the JPA entity currently persisted in last_games
  ├── walletEntity  — the player's wallet JPA entity
  ├── lastGameRepo  — repository for last_games table
  ├── pastGameRepo  — repository for played_games table
  ├── walletRepo    — repository for wallets table
  ├── betHistoryService
  ├── basicStrategy — advises whether a double-down follows basic strategy
  ├── clock         — injectable LocalDateTimeProvider (enables test determinism)
  └── om            — Jackson ObjectMapper (Game ↔ JSON serialization)
```

Pure-logic processors only use `game`; wallet-aware processors also read/write `walletEntity`, `walletRepo`, `lastGameRepo`, etc.

#### Processor ordering in GameStateProcessorChain

```
── Wallet / persistence processors (run first) ─────────────────────────────────
 1. RepeatLastBetProcessor            CHOICE_REPEAT_LAST_BET
                                      Places the last recorded bet amount onto the wallet.

 2. RepeatLastBetAgainProcessor       CHOICE_REPEAT_LAST_BET_AGAIN
                                      No-op pass-through (already handled this turn).

 3. ClearLastBetProcessor             CHOICE_CLEAR_LAST_BET
                                      Returns the current bet to the player's balance.

 4. InsuranceBetProcessor             CHOICE_INSURANCE_YES
                                      Deducts the insurance half-bet from the wallet,
                                      or flags insufficient funds.

 5. DoubleDownBetProcessor            CHOICE_DOUBLE_DOWN
                                      Checks whether the wallet can cover the additional
                                      bet. Consults BasicStrategy: if correct play,
                                      sets finalized=true and doubles the bet immediately;
                                      if not basic strategy, prompts the player for
                                      confirmation (CHOICE_DOUBLE_NOT_BASIC_STRATEGY).

 6. DoubleDownYesWalletProcessor      CHOICE_DOUBLE_DOWN_YES
                                      Doubles the wallet bet after the player confirms
                                      the non-basic-strategy double.

 7. InsufficientFundsReCheckProcessor CHOICE_INSURANCE_YES_NOT_ENOUGH_MONEY
                                      CHOICE_DOUBLE_DOWN_NOT_ENOUGH_MONEY
                                      Re-checks if the balance now covers the pending
                                      bet; restores choices if affordable.

 8. ErrorPassthroughProcessor         any non-empty errCodeList
                                      Passes errors to the view for rendering, then
                                      clears them from the DB so they don't persist.

 9. FinalizedPayoutProcessor          gameEntity.finalized == true
                                      Moves the game from last_games → played_games,
                                      applies payout multipliers to the wallet,
                                      and saves a BetHistoryEntity record.

── Pure game-logic processors (no DB access) ───────────────────────────────────
10. NotDealtOrFinalizedProcessor      !game.dealt || game.finalized
                                      Resets to the betting phase. If a double-down
                                      was pending, completes it first.

11. DoubleDownConfirmProcessor        CHOICE_DOUBLE_DOWN_YES / CHOICE_DOUBLE_DOWN_NO
                                      YES: deals the double card, dealer plays out,
                                      hand is settled.
                                      NO: cancels the confirm, resumes normal play.

12. SurrenderProcessor                CHOICE_SURRENDER
                                      Finalizes the hand; dealer reveals one card;
                                      player forfeits half the bet (0.5× multiplier).

13. PlayerBlackjackAfterDealProcessor CHOICE_DEAL + player has blackjack
                                      If the dealer's upcard cannot make BJ, pays
                                      3:2 immediately. Otherwise offers even money.

14. EvenMoneyProcessor                CHOICE_EVEN_MONEY_YES / CHOICE_EVEN_MONEY_NO
                                      YES: pays 2:1 regardless of dealer outcome.
                                      NO: plays out — pays BJ only if dealer has no BJ.

15. HitProcessor                      CHOICE_HIT
                                      Deals one card to the player. If 21: dealer
                                      plays out and hand is settled. If bust: finalize
                                      with no payout. Otherwise: offer Stand/Hit again.

16. StandProcessor                    CHOICE_STAND
                                      Dealer plays to soft-17. Compares hands and
                                      sets the hand multiplier accordingly.

17. InsuranceProcessor                CHOICE_INSURANCE_YES / CHOICE_INSURANCE_NO
                                      If dealer has hidden BJ: finalize and pay
                                      insurance (if taken). Otherwise: normal play
                                      continues (Stand / Hit / Double / Split).

18. InitialDealSetupProcessor         canProcess = true (fallthrough, always last)
                                      Runs when no prior processor matched. Sets up
                                      the initial decision choices: Insurance (if
                                      dealer upcard is Ace), Surrender, Stand, Hit,
                                      Double Down, Split (if pair).
```

#### CardSource — injectable card drawing seam

All card drawing goes through a `CardSource` interface, exposed as a Spring `@Bean` in `BlackjackApplication`:

- **`RngCardSource`** — draws from the real random number generator (production)
- **`FixedCardSource`** — drains a pre-loaded `Queue<Card>` in exact order (testing / scenario replay)

The active scenario is controlled by a single line in `BlackjackApplication`:

```java
private static final DeckMode MODE = DeckMode.RANDOM;
```

`DeckMode` is an enum where each value describes a scenario and provides its own `FixedCardSource`. Switching scenarios requires changing only that one line — no comment/uncomment patterns.

#### GameService — thin orchestrator

`GameService` no longer contains any game or wallet logic. Its responsibilities are:

1. Load `GameEntity` and `WalletEntity` from the database
2. Deserialize the JSON blob in `GameEntity` into a `Game` DTO
3. Build a `GameContext` from all loaded data plus infrastructure dependencies
4. Call `processorChain.process(ctx)` — the chain does all the work
5. Save the result and return `game` to the controller

The `deal()` method is the only exception — it validates the incoming bet string and creates a fresh `Game` before handing off to the chain.

---

### Key Components
#### Configuration Classes
- `SecurityConfig` — Spring Security configuration
- `I18nConfig` — Internationalization setup
- `MailConfig` — Email configuration
- `RecaptchaConfig` — Google reCAPTCHA settings
- `WebConfig` — Web MVC configuration

#### Service Layer
- `GameService` — Thin orchestrator: loads state, builds `GameContext`, delegates to the processor chain
- `GameStateProcessorChain` — Walks the ordered processor list; first match wins
- `BasicStrategy` — Advises whether a double-down is correct basic strategy given the player's hand and dealer upcard
- `UserService` — User management operations
- `CreditCardService` — Payment processing
- `WalletService` — Balance management
- `MailService` — Email communications
- `UserTokenService` — Token generation and validation
- `RecaptchaService` — CAPTCHA verification

#### Custom Validations
- `@UniqueEmail` / `@UniqueUsername` - Duplicate checking 
- `@CustomCreditCardNumber` - Card number validation 
- `@FutureExpirationDate` - Expiry date validation 
- `@MinAge` - Age restriction (18+)
- `@FieldMatch` - Password confirmation matching

### Database Schema
### Core Entities
- `users` - User account information 
- `roles` - User roles for authorization 
- `wallets` - User balances and betting amounts 
- `credit_cards` - Registered credit cards 
- `last_games` - Current incomplete game sessions 
- `played_games` - Game history 
- `bet_history` - Betting records 
- `activation_tokens` - Email verification tokens 
- `reset_pass_tokens` - Password reset tokens

### API Endpoints
### Public Endpoints
- `/` - Home page
- `/auth/login` - Login page
- `/auth/register` - Registration page
- `/auth/reset` - Password reset request
- `/rules` - Game rules
- `/test/**` - Testing endpoints

### Protected Endpoints
- `/play/**` - Gameplay endpoints
- `/credit-card/**` - Credit card management
- `/auth/activation` - Account activation
- `/auth/reset_pass` - Password reset

## Running the Application
### Prerequisites
- Java 25+
- Docker (optional, for MySQL/MailHog)
- MySQL database (if not using Docker)

### Quick Start with Docker
```shell
cd /path/to/BJ
open -a docker

# Start MySQL and MailHog containers
docker-compose up -d

# Run the Spring Boot application
./gradlew bootRun
```

### Access Points
- Application: http://localhost:8080
- MailHog UI: http://localhost:8025 (for viewing emails)
- MySQL: localhost:3306

### Database Client (e.g. Sequel Ace)
Use a **Standard** connection:

| Field    | Value   |
|----------|---------|
| Host     | `127.0.0.1` |
| Username | `root`  |
| Password | *(empty)* |
| Database | `bjdb`  |
| Port     | `3306`  |

### Configuration Properties
Key configuration options in `application.properties`:

```yml
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/bjdb

# Email
mail.host=localhost
mail.port=1025

# reCAPTCHA
google.recaptcha.enabled=true

# Authentication
auth.register.auto-login=false
auth.login.remember-me-key=your-secret-key

# Token expiration
auth.activation-token.expires-after-minutes=60
auth.forgot-password-token.expires-after-minutes=30
```

```shell
docker container rm -f $(docker container ls -aq)
```

### Testing
### Test Accounts
After registration, users need to confirm their email address before logging in (unless auto-login is enabled).

### Email Testing
- Use MailHog UI (port 8025) to view all sent emails
- No real SMTP server required for development

### Game Testing
- Minimum bet: $10
- Maximum bet: $1000
- Register a credit card before depositing funds
- Use test card numbers (follow Luhn algorithm)

### Security Considerations
- Passwords are BCrypt-encoded
- SQL injection prevention using JPA/Hibernate
- CSRF protection enabled
- XSS prevention through Thymeleaf auto-escaping
- Session fixation protection
- Secure remember-me token storage

### Future Enhancements
Potential improvements:

- Multi-table Blackjack support
- Tournament mode
- Leaderboards and achievements
- Mobile-responsive design optimization
- WebSocket for real-time updates
- Caching for improved performance
- API documentation with Swagger/OpenAPI
- Unit and integration test coverage

### Icons

Navbar icons are sourced from [Tabler Icons](https://tabler.io/icons) — an open-source icon library licensed under the MIT License.

| Icon | Tabler name |
|------|-------------|
| Lobby (cocktail glass) | `glass-cocktail` |
| Deposit (credit card) | `credit-card` |
| Cash out (banknote) | `cash-banknote` |
| Play (playing cards) | `cards` |

The Rules (spade) icon is a custom hand-crafted SVG.

### License
This project is for educational purposes. Use responsibly.

