# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Build debug APK
./gradlew :app:assembleDebug

# Run unit tests (Robolectric, JVM only)
./gradlew :app:testDebugUnitTest

# Run single test class
./gradlew :app:testDebugUnitTest --tests "com.example.ExampleRobolectricTest"

# Clean
./gradlew :app:clean
```

On Windows use `gradlew.bat` or prefix with `.\`.

## Architecture

**Local-first Android app** (Kotlin + Jetpack Compose + Material 3). Single `MainActivity`, tab-based navigation.

### Layers

```
ui/screens/        → Composable screens (one per feature tab)
ui/HomeViewModel   → Master state: expenses, inventory, shopping, scanner, sync status
ui/MainViewModel   → Tab navigation, spending predictions, store recommendations
data/Repository    → Unified data access, sync orchestration, prepopulation
data/Daos.kt       → 4 Room DAOs: ExpenseDao, InventoryDao, ShoppingDao, SyncSettingsDao
data/AppDatabase   → Room singleton
data/NeonDatabaseHelper → Direct JDBC queries to Neon PostgreSQL (cloud sync)
data/GeminiScannerService → Gemini API multimodal calls for receipt/price/larder scanning
data/DeletionTracker → SharedPreferences-backed deletion log for cloud sync correctness
```

### Data Flow

- Local storage: Room (SQLite) on-device
- Cloud sync: bidirectional with **Neon Serverless PostgreSQL** (`sa-east-1`)
- Sync uses `DeletionTracker` to prevent deleted items reappearing after sync
- Auth: SharedPreferences + household code + member CSV list
- Async: Kotlin Coroutines + StateFlow; ViewModels expose StateFlow, screens collect via `collectAsStateWithLifecycle()`

### Scanner

`GeminiScannerService` calls Gemini multimodal API. `CameraOrGalleryLauncher` is the reusable Compose camera/gallery picker composable; `FileProvider` config is in `res/xml/file_paths.xml`.

### Web Companion

`web-companion/` is a Next.js app. **Read `node_modules/next/dist/docs/` before touching it** — this version has breaking API/convention changes from standard Next.js.

## Key Configs

- `compileSdk = 36`, `minSdk = 26`, Java 11
- KSP (not KAPT) for Room + Moshi code generation
- Robolectric tests run at SDK 36: `@Config(sdk = [36])`
- Screenshot tests via Roborazzi

## Neon PostgreSQL Schema

Tables mirror Room entities 1:1: `expenses`, `inventory_items`, `shopping_items`, `sync_settings`. DDL runs on first connection in `NeonDatabaseHelper`. The `id` fields are `SERIAL PRIMARY KEY` on Neon but `Int` with `autoGenerate = true` in Room — be careful with ID mapping on sync.
