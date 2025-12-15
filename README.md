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

#### ID Strategy

To avoid conflicts between locally-created and server-created entities:

| ID Type | Range | Description |
|---------|-------|-------------|
| Server IDs | Positive (1, 2, 3, ...) | Assigned by the server |
| Offline IDs | Negative (-1, -2, -3, ...) | Generated locally via `OfflineIdGenerator` |

This ensures offline-created games and rounds never conflict with server data during sync.

#### Dual-ID Pattern

Entities use a dual-ID pattern for sync tracking:

```kotlin
data class GameEntity(
    val id: Int,           // Local primary key (negative if offline-created)
    val serverId: Int?,    // Server's ID (null until synced)
    val isDirty: Boolean,  // True if local changes need syncing
    // ...
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

#### Background Sync

The `SyncManager` handles bidirectional sync:

1. **Push**: Upload dirty local changes to server
   - New games (serverId == null)
   - Modified rounds (isDirty == true)

2. **Pull**: Download new data from server
   - New rounds from other devices
   - Updated player balances

#### Join Game by ID

When joining a game, the app checks multiple sources:

1. Server (if online) - authoritative source
2. Local database by `id` - for locally-created games
3. Local database by `serverId` - for previously-synced games

This allows joining both online and offline-created games.

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
