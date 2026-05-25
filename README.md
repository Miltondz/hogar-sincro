# Hogar Sincro 🏠

**Hogar Sincro** es una app Android nativa para gestión inteligente de hogar: gastos, compras, despensa e inventario, con sincronización en la nube y escaneo por IA.

Arquitectura **Local-First**: Room SQLite on-device + sincronización bidireccional con Neon Serverless PostgreSQL. Funciona sin conexión a internet.

---

## Características

### 1. Gastos del Hogar
- Registro de gastos variables y recurrentes (alquiler, servicios).
- Categorías: *Alquiler*, *Servicio*, *Alimentos*, *Diverso*.
- Selector de **quién pagó** por gasto (miembros del hogar dinámicos).
- Eliminación con acción **Deshacer** (4 segundos antes de confirmar borrado).

### 2. Lista de Compras
- Artículos pendientes y comprados con sección separada visual.
- **Modo carrito activo**: presupuesto de sesión, totales en tiempo real.
- Al cerrar carrito, genera gasto agregado (sin duplicar por ítem).

### 3. Despensa y Stock (Inventario)
- Stock **binario**: `0 = AGOTADO` (rojo) / `>0 = TENGO` (verde). Sin tasas de consumo ni alertas numéricas.
- Toggle TENGO/AGOTADO por ítem — un toque.
- Escaneo de alacena por foto: diálogo de confirmación con checkboxes antes de importar.
- Comparador de precios por comercio.

### 4. Escáner Inteligente (Gemini AI)
- Escaneo multimodal de recibos, alacena y precios vía **Gemini API**.
- Modelo configurable desde UI: `gemini-2.5-flash` (default), `gemini-1.5-flash`.
- **Confirmación antes de importar**: diálogo con desglose de productos antes de persistir datos de ticket.

### 5. Sincronización (Neon PostgreSQL)
- Sync bidireccional con **Neon Serverless PostgreSQL** (`sa-east-1`).
- `DeletionTracker` previene reaparición de ítems eliminados tras sync.
- Badge **Offline** en barra superior cuando Neon no está disponible.
- Panel de estado de conexión, conteo de registros y configuración de hogar.

### 6. Companion Web
- Interfaz Next.js (`web-companion/`) con las mismas funciones: gastos, compras, despensa, escáner y sincronización.
- Mismas reglas de stock binario. Modelo Gemini configurable y persistido en localStorage.

---

## Arquitectura

```
ui/screens/        → Composables (una pantalla por tab)
ui/HomeViewModel   → Estado maestro: expenses, inventory, shopping, scanner, sync
data/Repository    → Acceso unificado + orquestación de sync
data/Daos.kt       → 4 Room DAOs
data/AppDatabase   → Singleton Room
data/NeonDatabaseHelper → Queries JDBC a Neon PostgreSQL
data/GeminiScannerService → Llamadas multimodal a Gemini REST API
data/DeletionTracker → Log de eliminaciones (SharedPreferences) para sync correcto
```

**Flujo de datos**: Room (SQLite local) → StateFlow → Compose UI. Sync con Neon en background via coroutines. Snackbar events via Channel.

---

## Schema Neon PostgreSQL

Tablas mirror de entidades Room 1:1. DDL corre en primera conexión.

| Tabla | Campos clave |
|---|---|
| `expenses` | `id`, `title`, `amount`, `category`, `timestamp`, `is_recurring`, `recurring_due_date`, `paid_by` |
| `inventory_items` | `id`, `name`, `current_stock`, `min_stock_alert`, `unit`, `depletion_rate_per_day`, `best_store`, `best_price`, `last_updated` |
| `shopping_items` | `id`, `product_name`, `quantity_to_buy`, `unit`, `estimated_price`, `is_bought`, `target_store` |
| `sync_settings` | `id`, `active_user`, `household_code`, `is_sync_enabled`, `last_sync_timestamp`, `members`, `gemini_model` |

`id` es `SERIAL PRIMARY KEY` en Neon, `autoGenerate = true` en Room. Cuidado con mapeo de IDs en sync.

---

## Build

```bash
# Debug APK
./gradlew :app:assembleDebug

# Tests (Robolectric, JVM)
./gradlew :app:testDebugUnitTest

# Test clase individual
./gradlew :app:testDebugUnitTest --tests "com.example.ExampleRobolectricTest"

# Clean
./gradlew :app:clean
```

Windows: usar `gradlew.bat` o `.\gradlew`.

**JAVA_HOME requerido**: `C:/Program Files/Android/openjdk/jdk-21.0.8`

---

## Configuración local

Crear `.env` en raíz del proyecto (no sube a git):

```env
GEMINI_API_KEY=tu_clave_aqui
NEON_DATABASE_URL=jdbc:postgresql://...
```

Para el companion web, crear `web-companion/.env.local`:

```env
GEMINI_API_KEY=tu_clave_aqui
DATABASE_URL=postgres://...
```

---

## Stack Tecnológico

| Capa | Tecnología |
|---|---|
| Lenguaje | Kotlin (Android) / JavaScript (web) |
| UI | Jetpack Compose + Material Design 3 |
| Async | Kotlin Coroutines + StateFlow |
| DB local | Room (SQLite) con KSP |
| DB nube | Neon Serverless PostgreSQL (JDBC) |
| IA | Gemini API (multimodal REST) |
| Web | Next.js App Router |
| Tests | Robolectric (SDK 36) |
