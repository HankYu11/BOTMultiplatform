# BOT Multiplatform

A cross-platform score calculator app for the popular card game Big Old Two, built with Kotlin Multiplatform and Compose Multiplatform.

## Features

* Score calculator for Big Old Two with support for common rules
* Runs on Android and iOS using a single codebase
* Built entirely with Compose Multiplatform
* MVVM architecture with clean separation of concerns
* Offline-first data sync with optional server integration

## Tech Stack

* Kotlin Multiplatform (KMP)
* Jetpack Compose Multiplatform
* Kotlin Coroutines / Flow
* Room Database (multiplatform)
* Ktor Client (for backend communication)
* Koin (dependency injection)

## Architecture

### Offline-First Data Sync

The app uses an offline-first architecture that allows full functionality without network connectivity while seamlessly syncing data when online.

#### Sync Model: "Sync and Replace"

The app uses a **server as single source of truth** model:

- **Offline**: Data is saved locally with Room-generated IDs and marked as `isDirty`
- **Sync**: Complete game data is uploaded to server, which returns data with server-generated IDs
- **Replace**: Local data is deleted and replaced with server response

This eliminates ID conflicts and keeps the architecture simple.

#### Entity Structure

```kotlin
data class GameEntity(
    val id: Int,              // Room auto-generated, becomes server ID after sync
    val isDirty: Boolean,     // True if local changes need syncing
    val lastSyncedAt: Long?,  // Timestamp of last successful sync
    val createdAt: Long,
)
```

#### Sync Flow

```
┌─────────────────────────────────────────────────────────────┐
│                      User Action                             │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ Online & Eligible│
                    │   for Online?    │
                    └─────────────────┘
                      │           │
                    Yes          No
                      │           │
                      ▼           ▼
              ┌──────────┐  ┌──────────────┐
              │ Try API  │  │ Save Locally │
              │  Call    │  │ (isDirty=false)│
              └──────────┘  └──────────────┘
                   │
           ┌───────┴───────┐
        Success         Failure
           │               │
           ▼               ▼
    ┌────────────┐  ┌──────────────┐
    │Save with   │  │ Save Locally │
    │Server IDs  │  │ (isDirty=true)│
    └────────────┘  └──────────────┘
```

#### Data States

| `isEligibleForOnline` | `canUseOnline()` | Behavior |
|----------------------|------------------|----------|
| `false` | - | Pure offline mode, no sync attempts |
| `true` | `false` | Offline with dirty flag, sync later |
| `true` | `true` | Online-first with offline fallback |

#### Background Sync (Sync and Replace)

The `SyncManager` handles sync with a complete replacement strategy:

1. **Collect**: Gather all local game data (players, rounds, results)
2. **Upload**: Send complete game to server via `POST /game/sync`
3. **Replace**: Delete local data, insert server response with server IDs
4. **Notify**: Emit `gameIdChanged` event if ID changed (for navigation)

```kotlin
// Sync request uses player indices instead of IDs
data class SyncGameRequest(
    val players: List<SyncPlayerRequest>,  // [{ name: "Alice" }, ...]
    val rounds: List<SyncRoundRequest>,    // Results reference playerIndex 0-3
)
```

#### Join Game by ID

When joining a game, the app checks:

1. Server (if online) - authoritative source
2. Local database by `id` - for local or previously-synced games

### Project Structure

```
composeApp/
├── src/
│   ├── commonMain/          # Shared Kotlin code
│   │   ├── data/
│   │   │   ├── database/    # Room entities, DAOs
│   │   │   ├── network/     # Ktor API clients
│   │   │   ├── repository/  # Data repositories
│   │   │   └── sync/        # SyncManager, states
│   │   ├── domain/          # Use cases, models
│   │   └── ui/              # Compose screens, ViewModels
│   ├── androidMain/         # Android-specific code
│   ├── iosMain/             # iOS-specific code
│   ├── commonTest/          # Shared tests
│   └── androidUnitTest/     # Android-specific tests (MockK)
```

## Testing

Run Android unit tests:
```bash
./gradlew :composeApp:testDebugUnitTest
```

Test coverage includes:
- Repository behavior (online/offline modes)
- ViewModel logic
- Domain model validation
- Sync state management
