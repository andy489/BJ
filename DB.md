# Database Architecture — Push365 Casino

Schema is managed exclusively by **Liquibase** (`src/main/resources/db/changelog/`).
JPA `ddl-auto` is `none` — the ORM never touches DDL.

---

## Table Overview

| Table | Purpose |
|-------|---------|
| `users` | Registered accounts |
| `roles` | Authorization roles (enum values) |
| `users_roles` | Many-to-many join between users and roles |
| `wallets` | Balance, active bets, and last-round win/bet tracking |
| `credit_cards` | Registered payment cards (up to 3 per user) |
| `last_games` | In-progress game state as structured columns |
| `played_games` | Completed game archive (immutable after finalization) |
| `bet_history` | One row per completed hand — financial summary for the history panel |
| `activation_tokens` | Email verification tokens (60-min expiry) |
| `reset_pass_tokens` | Password-reset tokens (30-min expiry) |

---

## Entity Relationship Diagram

```
users ──────────────────────────────────────────────────────┐
  │  (one-to-one)                                            │
  ├──► wallets            balance, bets, last-win tracking   │
  │                                                          │
  ├──► last_games         current in-flight hand             │
  │      └──────────────────────────────────────────────────►│ owner FK
  │                                                          │
  ├──► played_games       finished hand archive              │
  │      └── one row per completed hand ─────────────────────►│ owner FK
  │            │
  │            └──► bet_history  (game_hash → played_games.hash)
  │                   └──────────────────────────────────────►│ user FK
  │
  ├──► credit_cards       up to 3 cards per user
  │
  ├──► activation_tokens  one per user (unique constraint)
  └──► reset_pass_tokens  one per user (unique constraint)

users ◄──────── users_roles ──────────► roles
```

---

## Table Details

### `users`

Core account record. `is_active` is `false` until the activation email link is clicked.
`my_wallet_id` is a foreign key back to `wallets` — the circular reference (`users ↔ wallets`) is intentional: each side owns a pointer to the other for fast bidirectional lookup without joins.

| Column | Type | Notes |
|--------|------|-------|
| `id` | BIGINT PK | Auto-increment |
| `username` | VARCHAR(255) UNIQUE NOT NULL | Login identifier |
| `email` | VARCHAR(255) UNIQUE NOT NULL | Used for activation and reset emails |
| `password` | VARCHAR(255) | BCrypt hash |
| `first_name` | VARCHAR(255) | |
| `last_name` | VARCHAR(255) | |
| `birth_date` | DATETIME NOT NULL | Used by `@MinAge` (18+) validator |
| `gender` | ENUM('MALE','FEMALE','UNSPECIFIED') NOT NULL | |
| `is_active` | BIT(1) | `false` until email verified |
| `my_wallet_id` | BIGINT FK → wallets.id UNIQUE | |

---

### `roles`

Lookup table; seeded once by Liquibase data changesets and never modified at runtime.

| Column | Type | Notes |
|--------|------|-------|
| `id` | BIGINT PK | Auto-increment |
| `role` | ENUM('REGULAR','MODERATOR','ADMIN') UNIQUE NOT NULL | |

---

### `users_roles`

Composite-PK join table — no auto-increment ID. Standard Spring Security many-to-many.

| Column | Type | Notes |
|--------|------|-------|
| `user_id` | BIGINT PK FK → users.id | |
| `role_id` | BIGINT PK FK → roles.id | |

---

### `wallets`

One wallet per user (one-to-one). Holds both the real balance and all in-flight bet amounts that have been deducted from the balance but not yet settled.

**Balance and settlement fields**

| Column | Type | Notes |
|--------|------|-------|
| `balance` | DECIMAL(38,2) NOT NULL | Real spendable balance |
| `current_bet` | DECIMAL(38,2) NOT NULL | Running sum of all active bets |

**Active per-round bet slots** — each deducted from `balance` when placed, zeroed on payout:

| Column | Type | Notes |
|--------|------|-------|
| `hand_bet` | DECIMAL(38,2) NOT NULL | Main hand bet |
| `double_bet` | DECIMAL(38,2) NOT NULL | Extra stake placed on double-down |
| `insurance_bet` | DECIMAL(38,2) NOT NULL | Insurance side stake |
| `split_bet` | DECIMAL(38,2) NOT NULL | Split hand second stake |
| `perfect_pairs_bet` | DECIMAL(38,2) NOT NULL | PP side bet |
| `twenty_one_three_bet` | DECIMAL(38,2) NOT NULL | 21+3 side bet |
| `dealer_perfect_pairs_bet` | DECIMAL(38,2) NOT NULL | DPP side bet |

**Last-round win breakdown** — shown in the "Last Win" display box:

| Column | Type | Notes |
|--------|------|-------|
| `last_win` | DECIMAL(38,2) NOT NULL | Combined total return (hand + all side bets) |
| `last_hand_win` | DECIMAL(38,2) NOT NULL | Main hand component of last win |
| `last_pp_win` | DECIMAL(38,2) NOT NULL | PP component |
| `last_t3_win` | DECIMAL(38,2) NOT NULL | 21+3 component |
| `last_dpp_win` | DECIMAL(38,2) NOT NULL | DPP component |

**Last-round bet record** — used by "Repeat Last Bet":

| Column | Type | Notes |
|--------|------|-------|
| `last_bet` | DECIMAL(38,2) NOT NULL | Main hand bet from the previous round |
| `last_total_bet` | DECIMAL(38,2) NOT NULL | Total stake (main + side bets) from previous round |
| `last_pp_bet` | DECIMAL(38,2) NOT NULL | PP side bet from previous round |
| `last_t3_bet` | DECIMAL(38,2) NOT NULL | 21+3 side bet from previous round |
| `last_dpp_bet` | DECIMAL(38,2) NOT NULL | DPP side bet from previous round |

---

### `credit_cards`

Up to 3 cards per user. Used for deposit and cash-out. CVC is stored as-is (dev project — no production PCI scope). Uniqueness enforced on `card_number`.

| Column | Type | Notes |
|--------|------|-------|
| `id` | BIGINT PK | Auto-increment |
| `card_number` | VARCHAR(255) UNIQUE NOT NULL | Luhn-validated on input |
| `card_holder` | VARCHAR(255) NOT NULL | |
| `card_cvc` | VARCHAR(255) NOT NULL | |
| `expired_month` | INT NOT NULL | |
| `expired_year` | INT NOT NULL | Future-date validated on input |
| `owner_id` | BIGINT FK → users.id NOT NULL | |

---

### `last_games`

Exactly one row per user (unique `owner_id`). Holds the full state of the hand currently in progress. When a game is finalized, `GameService` atomically copies this row to `played_games` and zeroes out or re-initializes the relevant fields — the row itself stays and is reused for the next deal.

**Why columns instead of a JSON blob?** Card lists and choice lists are short enough to fit in `VARCHAR(255)` after Jackson serialization; storing them as individual columns makes it possible to query and debug game state directly in SQL without parsing JSON.

**Core game state**

| Column | Type | Notes |
|--------|------|-------|
| `id` | BIGINT PK | Auto-increment |
| `owner_id` | BIGINT FK → users.id UNIQUE | One active game per user |
| `hash` | VARCHAR(255) | UUID identifying this game instance, copied to `played_games` and `bet_history` at finalization |
| `finalized` | BIT(1) | `true` once payout is computed; triggers move to `played_games` on next GET |
| `dealt_time` | DATETIME | When the hand was dealt |
| `player_cards` | VARCHAR(255) | JSON array of card strings, e.g. `["K♠","7♦"]` |
| `dealer_cards` | VARCHAR(255) | JSON array; second card replaced with `null` while hand is live |
| `dealer_second_card` | VARCHAR(63) | Held separately so the view can hide it until dealer's turn |
| `available_choices` | VARCHAR(255) | JSON array of `CHOICE_*` integers the player can currently make |
| `taken_choices` | VARCHAR(255) | JSON array of `CHOICE_*` integers taken so far this hand |
| `err_code_list` | VARCHAR(255) | JSON array of error codes (e.g. insufficient funds) to surface in the UI |
| `hand_multiplier` | DOUBLE | Payout multiplier for the main hand (e.g. 1.5 for BJ, -1 for loss) |
| `insurance_multiplier` | DOUBLE | Payout multiplier for the insurance bet |
| `insurance` | BIT(1) | Whether insurance was taken |
| `double_down` | BIT(1) | Whether a double-down was played |

**Split state** — written only during split hands; `null` otherwise:

| Column | Type | Notes |
|--------|------|-------|
| `split_active` | BIT(1) | `true` while one of the split hands is being played |
| `split_count` | INT | Number of splits performed (max controlled by `GameProperties.maxSplits`) |
| `active_split_hand_index` | INT | Index of the currently active split hand |
| `split_aces` | BIT(1) | `true` when aces were split (restricts further actions) |
| `split_hands` | TEXT | JSON array of card arrays, one per split hand |
| `split_hand_multipliers` | VARCHAR(1023) | JSON array of per-hand multipliers |
| `split_double_down_flags` | VARCHAR(255) | JSON array of booleans, one per split hand |

**Side bet deal snapshot** — captured once at deal, never overwritten:

| Column | Type | Notes |
|--------|------|-------|
| `initial_player_cards` | VARCHAR(255) | First two player cards — needed for PP and 21+3 evaluation at settlement |
| `initial_dealer_up_card` | VARCHAR(255) | Dealer's face-up card — needed for 21+3 evaluation |
| `initial_dealer_cards` | VARCHAR(255) | Both dealer cards — needed for DPP evaluation |

---

### `played_games`

Immutable archive. One row per completed hand. Written once when `last_games.finalized = true` is detected on the next GET. Never updated after creation.

The `hash` is the same UUID used in `last_games` and `bet_history`, making all three rows for the same hand joinable by hash.

| Column | Type | Notes |
|--------|------|-------|
| `id` | BIGINT PK | Auto-increment |
| `hash` | VARCHAR(255) UNIQUE NOT NULL | Game identifier, FK target for `bet_history.game_hash` |
| `owner_id` | BIGINT FK → users.id | Many games per user |
| `player_cards` | VARCHAR(255) | Final player card state (JSON) |
| `dealer_cards` | VARCHAR(255) | Full dealer hand revealed (JSON) |
| `taken_choices` | VARCHAR(255) | Full choice sequence (JSON) |
| `hand_multiplier` | DOUBLE | Final payout multiplier |
| `insurance_multiplier` | DOUBLE | |
| `insurance` | BIT(1) | |
| `double_down` | BIT(1) | |
| `dealt_time` | DATETIME | |
| `finalized_time` | DATETIME | When the hand was settled |
| `split_hands` | TEXT | Split hand card arrays (JSON), null if no split |
| `split_hand_multipliers` | VARCHAR(1023) | Per-split-hand multipliers (JSON) |
| `initial_player_cards` | VARCHAR(255) | For history side-bet replay display |
| `initial_dealer_cards` | VARCHAR(255) | For history side-bet replay display |

---

### `bet_history`

One row per completed hand — financial summary only. This is the table read by `BetHistoryService.getLast10()` (sorted `ORDER BY id DESC`) to populate the "Last 10 Hands" panel. It holds amounts and side-bet breakdown; full card details are fetched via the joined `played_games` row when needed.

| Column | Type | Notes |
|--------|------|-------|
| `id` | BIGINT PK | Auto-increment; natural ordering = chronological |
| `user_id` | BIGINT FK → users.id | |
| `game_hash` | VARCHAR(255) UNIQUE FK → played_games.hash | One-to-one join to the game record |
| `total_bet_amount` | DECIMAL(38,2) NOT NULL | Main hand bet + double bet staked this round |
| `return_amount` | DECIMAL(38,2) NOT NULL | Total returned to wallet (0 on full loss) |
| `double_down` | BIT(1) NOT NULL | |
| `split` | BIT(1) NOT NULL | |
| `pp_bet` | DECIMAL(38,2) NOT NULL | Perfect Pairs bet amount |
| `t3_bet` | DECIMAL(38,2) NOT NULL | 21+3 bet amount |
| `dpp_bet` | DECIMAL(38,2) NOT NULL | Dealer Perfect Pairs bet amount |
| `pp_win` | DECIMAL(38,2) NOT NULL | PP payout (0 if lost) |
| `t3_win` | DECIMAL(38,2) NOT NULL | 21+3 payout |
| `dpp_win` | DECIMAL(38,2) NOT NULL | DPP payout |

---

### `activation_tokens`

One row per user (unique `user_id`). Created at registration; deleted by `UserTokenService` after the activation link is clicked or by the scheduled cleanup job after 60 minutes.

| Column | Type | Notes |
|--------|------|-------|
| `id` | BIGINT PK | Auto-increment |
| `user_id` | BIGINT FK → users.id UNIQUE | |
| `token` | VARCHAR(255) | UUID sent in the activation email link |
| `created_at` | DATETIME | Compared against `now()` to enforce 60-min expiry |

---

### `reset_pass_tokens`

Identical structure to `activation_tokens`. One row per user; 30-min expiry; cleaned up by a scheduled job.

| Column | Type | Notes |
|--------|------|-------|
| `id` | BIGINT PK | Auto-increment |
| `user_id` | BIGINT FK → users.id UNIQUE | |
| `token` | VARCHAR(255) | UUID sent in the reset-password email |
| `created_at` | DATETIME | |

---

## Key Design Decisions

### Game state as structured columns, not a single JSON blob

`last_games` uses individual columns rather than one fat JSON document. This allows direct SQL inspection of card state and choices, and makes Liquibase migrations additive — new columns are appended without touching existing data.

### Hash as the cross-table key

Each game gets a UUID (`hash`) at deal time. The same hash appears in `last_games`, `played_games`, and `bet_history`, linking financial records to game detail without requiring a surrogate join table.

### Two-table game lifecycle (`last_games` → `played_games`)

`last_games` is a single mutable row per user — it acts as a cursor for the in-progress hand. On finalization, the row is cloned to `played_games` (immutable archive) and reused for the next deal. This keeps write amplification low and makes "resume on next visit" trivially correct: if a `last_games` row exists for the user and `finalized = false`, the game resumes exactly where it left off.

### Bet slots in `wallets`

Active bet amounts (`hand_bet`, `double_bet`, `insurance_bet`, `split_bet`, side bets) are stored in the wallet row rather than in `last_games`. This means a single atomic wallet update covers both balance deduction and bet tracking — no separate transaction needed to reconcile the two.

### `bet_history` linked to `played_games` by hash (not by BIGINT id)

`bet_history.game_hash` is a FK to `played_games.hash` (a VARCHAR) rather than to `played_games.id`. This means the history entry can be written before the `played_games.id` is known (e.g., in the same transaction) and the relationship is human-readable when debugging.

---

## Liquibase Changelog Summary

| File | Changes |
|------|---------|
| `changelog-tables-v1.0.xml` | Initial schema: all 10 tables, FK constraints, indexes |
| `changelog-tables-v1.1.xml` | Added `wallets.split_bet`; added split columns to `last_games` |
| `changelog-tables-v1.2.xml` | NOT NULL constraints on wallet monetary columns; `bet_history.split`; split columns on `played_games` |
| `changelog-tables-v1.3.xml` | PP and 21+3 side bet columns on `wallets`; initial card snapshot columns on `last_games` |
| `changelog-tables-v1.4.xml` | DPP side bet column on `wallets`; initial dealer cards on `last_games` |
| `changelog-tables-v1.5.xml` | Per-category last-win columns on `wallets` (`last_pp_win`, `last_t3_win`, `last_dpp_win`) |
| `changelog-tables-v1.6.xml` | `wallets.last_total_bet` for betting panel display |
| `changelog-tables-v1.7.xml` | Side bet amounts and wins on `bet_history`; initial card snapshots on `played_games`; last side bet amounts on `wallets`; `last_hand_win` breakdown column |
| `changelog-data-v1.0.xml` | Dev seed data: users, roles, wallets, credit cards |
| `changelog-data-v1.1.xml` | Additional dev seed data |
