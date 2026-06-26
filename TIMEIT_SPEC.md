# TimeIt — Android Game Specification
**Version:** 1.0  
**Date:** 2026-06-25  
**Status:** Ready for implementation

---

## 1. Project Overview

TimeIt is a 2–4 player Android party game built around human time perception. Players are shown a target duration (e.g. 7.62 seconds) and must tap the screen when they believe that duration has elapsed. The player whose tap is closest to the target wins the round. First to win a configurable number of rounds wins the match.

---

## 2. Tech Stack

| Layer | Choice | Notes |
|---|---|---|
| Language | Kotlin | Google's official Android language |
| UI Framework | Jetpack Compose | Modern declarative UI; handles animations and touch |
| IDE | Android Studio Quail 1 | Already installed |
| Backend / DB | Supabase | PostgreSQL + Realtime WebSocket for online mode |
| Supabase SDK | `supabase-kt` (supabase-community) | Kotlin Multiplatform client; modules: `postgrest-kt`, `realtime-kt`, `auth-kt` |
| HTTP Client | Ktor (`ktor-client-android`) | Required peer dependency of supabase-kt |
| Serialization | KotlinX Serialization | Default for supabase-kt data models |
| Timing | `System.nanoTime()` | Nanosecond precision; immune to wall-clock drift |
| Version Control | Git | Repo already created and pulled locally |

### Key Gradle dependencies (app/build.gradle.kts)
```kotlin
implementation(platform("io.github.jan-tennert.supabase:bom:VERSION"))
implementation("io.github.jan-tennert.supabase:postgrest-kt")
implementation("io.github.jan-tennert.supabase:realtime-kt")
implementation("io.github.jan-tennert.supabase:auth-kt")
implementation("io.ktor:ktor-client-android:KTOR_VERSION")
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:VERSION")
```

### AndroidManifest.xml additions
```xml
<uses-permission android:name="android.permission.INTERNET" />
```

---

## 3. Game Modes

### 3.1 Pass-and-Play (local, single device)
- 2–4 players share one physical Android device
- Players take turns sequentially — after each tap, the phone is handed to the next player
- No network connection required
- All timing, state, and results managed entirely on-device

### 3.2 Online Multiplayer (separate devices, internet)
- Each player uses their own Android device
- Players join a shared session via a 6-character alphanumeric room code
- The host device generates the target time and broadcasts the game start
- Each player's tap result is submitted to Supabase; results are synced via Realtime
- **Clock sync note:** Do NOT compare raw `System.nanoTime()` across devices — clocks differ per device. Each device records elapsed time from the moment it receives the server-broadcast "GO" signal. Delta is calculated locally and submitted as a float.

> **Future consideration (not in scope for v1):** Simultaneous corner-tapping on a single device (one corner per player). Defer until after online mode is stable.

---

## 4. Game Rules

### 4.1 Win condition
- First player to win **N rounds** wins the match
- Default: **N = 3**
- N is configurable at game creation (options: 1, 3, 5, 7, 10)
- This config lives in a `GameConfig` data class and is designed to be extended for future game modes

### 4.2 Round winner
- The player with the **smallest absolute delta** (|player_time – target_time|) wins the round
- Ties: both players win the round (both increment their win count)

### 4.3 Target time
- Random float: **1.00 – 15.99 seconds**, precision to hundredths (2 decimal places)
- Generated using `kotlin.random.Random.nextDouble(1.0, 16.0)` rounded to 2 decimal places
- Target is displayed to all players **before** the countdown, then **hidden** during the tap phase

---

## 5. Screen Flow

```
SplashScreen
    └── HomeScreen
            ├── [Pass and Play] ──► LobbyScreen (local)
            │                            └── GameScreen ──► ResultsScreen ──► (next round or WinnerScreen)
            └── [Play Online]  ──► OnlineMenuScreen
                                        ├── [Host Game] ──► WaitingRoomScreen ──► GameScreen ──► ResultsScreen
                                        └── [Join Game] ──► JoinRoomScreen ──► WaitingRoomScreen ──► GameScreen
```

---

## 6. Screen Specifications

### 6.1 HomeScreen
- App name / logo
- Two primary buttons: "Pass and Play" | "Play Online"
- Settings icon (top right): configure default win target

### 6.2 LobbyScreen (local)
- Input fields for player names (2–4 players)
- "Add Player" button (up to 4)
- Win target picker (default 3)
- "Start Game" button

### 6.3 OnlineMenuScreen
- "Host a Game" button — creates room, shows room code
- "Join a Game" button — text field for room code entry

### 6.4 WaitingRoomScreen
- Displays room code prominently (for host to share)
- List of joined players (updates in real time via Supabase Realtime)
- Host sees "Start Game" button (enabled when ≥ 2 players joined)
- Non-host players see "Waiting for host to start…"

### 6.5 GameScreen
This is the core experience. It has three sub-states:

**State A — Target reveal**
- Large display of target time (e.g. "7.62")
- Subtext: "Remember this time"
- Auto-advances after 3 seconds (or player taps to continue)
- In pass-and-play: also shows "Player N's turn — [Name]"

**State B — Countdown**
- Full-screen animated 3… 2… 1… GO!
- Sound cue on each number and on GO
- Haptic pulse on GO
- Target time disappears at start of countdown

**State C — Tap phase (active timer)**
- Screen is nearly black
- A simple **hourglass animation** plays: sand falls from top chamber to bottom
  - The hourglass is decorative — it does NOT map 1:1 to the target time
  - It runs on a fixed ~12-second visual loop to avoid giving away elapsed time
  - Rendered in Compose Canvas or a Lottie animation asset
- "Tap anywhere to stop" hint text (small, faded, bottom of screen)
- On tap: player's elapsed time is locked via `System.nanoTime()`, screen transitions immediately to State D

**State D — Waiting (online mode only)**
- Shows player's own result: "You tapped at X.XX seconds"
- Shows number of players still tapping
- Auto-advances when all players have tapped or 30-second timeout elapses

### 6.6 ResultsScreen
- Displays all players ranked by delta (smallest first)
- Each row: Player name | Tapped at | Delta (±X.XX s)
- Round winner highlighted
- Running win tally shown for each player
- Two buttons: "Next Round" (host only in online mode) | "End Game"

### 6.7 WinnerScreen
- Shown when a player reaches the win target
- Large celebration display: winner's name
- Buttons: "Play Again" (same players, reset scores) | "Home"

---

## 7. Data Models

### 7.1 Local (in-memory, pass-and-play)
```kotlin
data class GameConfig(
    val winTarget: Int = 3,
    val minTime: Double = 1.0,
    val maxTime: Double = 15.99
)

data class Player(
    val id: String,       // UUID generated locally
    val name: String,
    val wins: Int = 0
)

data class RoundResult(
    val playerId: String,
    val targetTime: Double,
    val playerTime: Double,
    val delta: Double       // playerTime - targetTime (signed)
)

data class GameState(
    val config: GameConfig,
    val players: List<Player>,
    val currentRound: Int,
    val results: List<RoundResult>
)
```

### 7.2 Supabase Schema (online mode)

**Table: `rooms`**
| Column | Type | Notes |
|---|---|---|
| id | uuid | Primary key |
| code | text | 6-char room code, unique |
| host_id | uuid | Player ID of host |
| status | text | `waiting` \| `playing` \| `finished` |
| config | jsonb | Serialized GameConfig |
| target_time | float8 | Set by host at round start |
| current_round | int | 1-indexed |
| created_at | timestamptz | Auto |

**Table: `room_players`**
| Column | Type | Notes |
|---|---|---|
| id | uuid | Primary key |
| room_id | uuid | FK → rooms.id |
| player_name | text | |
| wins | int | Default 0 |
| joined_at | timestamptz | Auto |

**Table: `round_results`**
| Column | Type | Notes |
|---|---|---|
| id | uuid | Primary key |
| room_id | uuid | FK → rooms.id |
| round | int | |
| player_id | uuid | FK → room_players.id |
| player_time | float8 | Elapsed seconds at tap |
| delta | float8 | player_time - target_time |
| submitted_at | timestamptz | Auto |

**Realtime:** Enable on `rooms` and `round_results` tables. Clients subscribe to their room channel to receive live status updates and result submissions.

---

## 8. Project File Structure

```
app/
└── src/main/
    ├── java/com/yourname/timeit/
    │   ├── MainActivity.kt             # Entry point, sets up NavHost
    │   ├── ui/
    │   │   ├── screens/
    │   │   │   ├── HomeScreen.kt
    │   │   │   ├── LobbyScreen.kt
    │   │   │   ├── GameScreen.kt       # Contains all 4 sub-states
    │   │   │   ├── ResultsScreen.kt
    │   │   │   ├── WinnerScreen.kt
    │   │   │   ├── WaitingRoomScreen.kt
    │   │   │   └── JoinRoomScreen.kt
    │   │   ├── components/
    │   │   │   ├── HourglassAnimation.kt   # Canvas-based hourglass
    │   │   │   ├── CountdownDisplay.kt
    │   │   │   └── PlayerResultRow.kt
    │   │   └── theme/
    │   │       ├── Theme.kt
    │   │       ├── Color.kt
    │   │       └── Type.kt
    │   ├── viewmodel/
    │   │   ├── GameViewModel.kt        # Core game state (local)
    │   │   └── OnlineGameViewModel.kt  # Supabase sync logic
    │   ├── data/
    │   │   ├── model/
    │   │   │   ├── GameConfig.kt
    │   │   │   ├── Player.kt
    │   │   │   ├── RoundResult.kt
    │   │   │   └── GameState.kt
    │   │   └── repository/
    │   │       └── SupabaseRepository.kt   # All Supabase calls isolated here
    │   └── util/
    │       ├── TimerUtil.kt            # System.nanoTime() wrappers
    │       └── RoomCodeUtil.kt         # 6-char code generation
    └── res/
        ├── values/strings.xml
        └── raw/                        # Sound effect files (.ogg)
            ├── countdown_beep.ogg
            ├── go.ogg
            └── winner.ogg
```

---

## 9. Timing Implementation Notes

- Use `System.nanoTime()` exclusively for elapsed-time measurement — never `System.currentTimeMillis()`
- Store start time as a `Long` (nanoseconds), compute elapsed as `(System.nanoTime() - startTime) / 1_000_000_000.0` for seconds
- Round all displayed times to 2 decimal places
- In online mode: the "start time" is set the moment the device receives the Supabase Realtime `GO` broadcast event — each device computes its own elapsed time from that local moment

---

## 10. Hourglass Animation Spec

- Rendered using Jetpack Compose `Canvas` API (no external animation library required for v1)
- Two chambers connected by a narrow neck
- Sand particles in the top chamber fall to the bottom over a fixed ~12-second cycle, then reset
- The cycle length is intentionally NOT equal to the max target time (15.99s) — this prevents players from using it as a timer
- Color palette: dark background (#0D0D0D), sand in warm amber (#C8964A), glass outline in soft white (15% opacity)
- On player tap: animation freezes immediately

---

## 11. Build Phases

### Phase 1 — Core mechanic (no UI polish)
- [ ] Project setup: Kotlin + Compose + navigation dependency
- [ ] `GameViewModel` with timer logic using `System.nanoTime()`
- [ ] Single-screen prototype: target shown → countdown → tap → delta displayed
- [ ] Verify timing precision on a physical device

### Phase 2 — Pass-and-Play complete
- [ ] `LobbyScreen` with player name inputs
- [ ] Sequential turn flow in `GameViewModel`
- [ ] `ResultsScreen` with rankings
- [ ] `WinnerScreen` with win-target logic
- [ ] `GameConfig` win target picker

### Phase 3 — Visual polish
- [ ] `HourglassAnimation.kt` (Compose Canvas)
- [ ] Countdown animation (scale + fade)
- [ ] Sound effects (`SoundPool`)
- [ ] Haptic feedback on GO and on tap
- [ ] App theme (dark, minimal)

### Phase 4 — Supabase setup
- [ ] Create Supabase project
- [ ] Create tables: `rooms`, `room_players`, `round_results`
- [ ] Enable Realtime on both tables
- [ ] Add `supabase-kt` dependencies to Gradle
- [ ] `SupabaseRepository.kt` with create/join/submit functions
- [ ] Store Supabase URL and anon key in `local.properties` (never commit to Git)

### Phase 5 — Online multiplayer
- [ ] `OnlineMenuScreen` (host / join)
- [ ] `WaitingRoomScreen` with live player list via Realtime
- [ ] `OnlineGameViewModel` syncing game state via Supabase
- [ ] Result submission and collection
- [ ] Timeout handling (30s max tap window)
- [ ] Disconnect / error states

---

## 12. Out of Scope (v1)

- Simultaneous corner-tapping on a single device (one corner per player) — noted for future consideration
- Accounts / persistent profiles / leaderboards
- Spectator mode
- Custom time ranges
- Sound/haptic settings toggle (add in v2)
- iOS version

---

## 13. Open Questions for Future Versions

1. Should the hourglass animation style or speed be a selectable option (e.g. "no hint" mode = blank screen)?
2. Scoring variants: cumulative delta scoring vs. round-win counting?
3. Maximum round count cap before forced tie-break?
