# Hogar Sincro 🏠✨

**Hogar Sincro** es una aplicación móvil nativa para Android diseñada para la gestión inteligente, integral y coordinada de un hogar moderno. Permite la administración en tiempo real de gastos recurrentes, listas de compras dinámicas, inventario/despensa de alimentos y escaneo inteligente de recibos mediante Inteligencia Artificial.

La aplicación adopta un modelo de arquitectura **Local-First**, utilizando almacenamiento local persistente capaz de sincronizarse bidireccionalmente en tiempo real con una base de datos serverless en la nube, garantizando usabilidad incluso sin conectividad a internet.

---

## 🎨 Características Principales

### 1. Gastos del Hogar (Expenses)
- Registro detallado de consumos ordinarios y costos fijos.
- Configuración de gastos recurrentes (por ejemplo, servicios básicos y alquileres) con fechas estimadas de cobro ("Día 05 de cada mes").
- Clasificación intuitiva por categorías (*Alquiler*, *Servicio*, *Alimentos*, *Diverso*) e identificador de quién efectuó el pago.
- Sincronización robusta en la nube.

### 2. Lista de Compras (Shopping List)
- Registro instantáneo de artículos pendientes de adquisición.
- Configuración de cantidades, unidades de medida y estimación de costos por producto.
- Marcación en tiempo real de artículos adquiridos con filtros visuales de alta legibilidad.
- Seguimiento inteligente del supermercado o comercio objetivo para optimizar las rutas de compra.

### 3. Despensa y Stock (Inventory)
- Monitoreo del almacenamiento de provisiones del hogar en tiempo real.
- Alertas inteligentes basadas en límites mínimos de stock y tasas de consumo promedio diarias.
- **Predicción de Agotamiento**: Estimación dinámica de días útiles restantes de cada suministro antes de requerir reabastecimiento (`daysUntilDepletion` optimizado).
- Comparador visual de precios en comercios locales históricos para asegurar el costo más eficiente.

### 4. Escáner Inteligente (Gemini AI Scanner)
- Escaneo fotográfico de recibos físicos e imágenes digitales integrando los modelos multimodales **Gemini API**.
- Extracción autónoma de productos, precios individuales de tickets de compra, supermercados y fechas, convirtiendo capturas visuales en registros de datos estructurados automáticamente en cuestión de segundos.

### 5. Sincronización Avanzada (Neon Database Sync)
- Sincronización bidireccional integrada directamente con **Neon Serverless PostgreSQL**.
- **Gestión Inteligente de Eliminación**: Uso de un módulo especializado de seguimiento (`DeletionTracker`) basado en SharedPreferences que rastrea los registros descartados de manera local para asegurar su eliminación en el servidor web de Neon PostgreSQL, evitando la reaparición de ítems eliminados.
- Verificador de estado y configuración en la nube con indicadores visuales de conectividad.

### 6. Portal Web de Acompañamiento
- Interfaz integrada de simulación de escritorio para la visualización global y el análisis estadístico de la información del hogar en una pantalla ampliada, sincronizada con el estado local.

---

## 💻 Arquitectura de Datos y Base de Datos Neon Cloud

La base de datos central en la nube de **Hogar Sincro** corre bajo infraestructura **Neon PostgreSQL Serverless**, lo cual proporciona auto-escalado instantáneo y poolers de conexiones optimizados con un esquema dinámico validado.

### 🌐 Detalles de Conectividad Real
- **Database Engine**: PostgreSQL Serverless (AWS Region: `sa-east-1`)
- **Connection URL / Host**: `ep-blue-water-aco8ck54-pooler.sa-east-1.aws.neon.tech`
- **Auth Provider**: `https://ep-blue-water-aco8ck54.neonauth.sa-east-1.aws.neon.tech`
- **JWKS Endpoint**: `https://ep-blue-water-aco8ck54.neonauth.sa-east-1.aws.neon.tech/neondb/auth/.well-known/jwks.json`

### 📊 Modelos del Esquema de Base de Datos

Las tablas de **Neon PostgreSQL** se configuran automáticamente en la inicialización de la conexión mediante sentencias DDL estrictas. Los tipos de datos definidos en la replicación remota corresponden uno a uno con las entidades locales de la base de datos **Room**:

#### 1. Tabla de Gastos (`expenses`)
| Campo | Tipo SQL | Descripción |
| :--- | :--- | :--- |
| `id` | `SERIAL PRIMARY KEY` | Identificador único autogenerado |
| `title` | `TEXT NOT NULL` | Nombre o concepto del gasto |
| `amount` | `DOUBLE PRECISION NOT NULL` | Monto pagado / adeudado |
| `category` | `TEXT NOT NULL` | Categoría (*Alquiler*, *Servicio*, etc.) |
| `timestamp` | `BIGINT NOT NULL` | Estampa de tiempo UNIX de creación |
| `is_recurring` | `BOOLEAN NOT NULL DEFAULT FALSE` | Flag de cobro recurrente mensual |
| `recurring_due_date`| `TEXT` | Detalles de fecha de cobro recurrente |
| `paid_by` | `TEXT NOT NULL DEFAULT 'Milton'` | Nombre del miembro del hogar que pagó |

#### 2. Tabla de Inventario (`inventory_items`)
| Campo | Tipo SQL | Descripción |
| :--- | :--- | :--- |
| `id` | `SERIAL PRIMARY KEY` | Identificador único autogenerado |
| `name` | `TEXT NOT NULL` | Nombre de la provisión / alimento |
| `current_stock` | `DOUBLE PRECISION NOT NULL` | Cantidad actual almacenada |
| `min_stock_alert` | `DOUBLE PRECISION NOT NULL` | Umbral mínimo para disparo de alerta |
| `unit` | `TEXT NOT NULL` | Unidad de medida (`kg`, `litros`, `u`) |
| `depletion_rate_per_day`| `DOUBLE PRECISION NOT NULL` | Consumo promedio diario estimado |
| `best_store` | `TEXT` | Comercio con el mejor precio histórico |
| `best_price` | `DOUBLE PRECISION` | Precio más bajo histórico registrado |
| `second_best_store` | `TEXT` | Comercio alternativo registrado |
| `second_best_price` | `DOUBLE PRECISION` | Segundo mejor precio histórico |
| `last_updated` | `BIGINT NOT NULL` | Estampa de tiempo UNIX de actualización |

#### 3. Tabla de Compras (`shopping_items`)
| Campo | Tipo SQL | Descripción |
| :--- | :--- | :--- |
| `id` | `SERIAL PRIMARY KEY` | Identificador único autogenerado |
| `product_name` | `TEXT NOT NULL` | Nombre del producto a adquirir |
| `quantity_to_buy` | `DOUBLE PRECISION NOT NULL` | Cantidad requerida |
| `unit` | `TEXT NOT NULL DEFAULT 'u'` | Unidad de medida |
| `estimated_price` | `DOUBLE PRECISION NOT NULL DEFAULT 0.0` | Precio estimado del producto |
| `is_bought` | `BOOLEAN NOT NULL DEFAULT FALSE` | Flag de compra realizada / marcado |
| `target_store` | `TEXT` | Comercio preferido para la compra |

---

## 🏗️ Estructura del Repositorio

El código fuente está estructurado de manera modular y limpia en el lenguaje **Kotlin** dentro de la plataforma Android:

```text
/app/src/main/java/com/example/
├── MainActivity.kt               # Actividad principal y barra de navegación adaptativa M3
├── data/                         # Capa de Acceso a Datos (Room, JDBC, Repositorios)
│   ├── Entities.kt               # Definición de Tablas locales (Expense, InventoryItem, ShoppingItem, SyncSettings)
│   ├── Daos.kt                   # Consultas locales de Room Database (SQLite)
│   ├── AppDatabase.kt            # Base de datos centralizada de Room
│   ├── DeletionTracker.kt        # Gestor de eliminaciones locales pendientes de réplica
│   ├── NeonDatabaseHelper.kt     # Consultas JDBC PostgreSQL, sentencias DDL y lógica bidireccional
│   ├── GeminiScannerService.kt   # Integración con el SDK de Gemini API para escaneo de recibos
│   └── Repository.kt             # Origen de datos unificado de la aplicación
└── ui/                           # Capa de UI y Lógica de Presentación (Jetpack Compose, M3 Style)
    ├── HomeViewModel.kt          # Controlador de Estados del Hogar y eventos de sincronización
    └── screens/                  # Pantallas del flujo de navegación
        ├── ExpensesScreen.kt     # Vista de gestión financiera y gastos
        ├── ShoppingScreen.kt     # Vista de checklists de compras del hogar
        ├── InventoryScreen.kt    # Vista de administración de stock de la despensa
        ├── ScannerScreen.kt      # Vista del alimentador inteligente mediante Gemini OCR
        ├── SyncScreen.kt         # Panel de conectividad y Sincronización en la Nube
        └── WebCompanionScreen.kt # Vista Web Companion para monitoreo general en pantallas anchas
```

---

## 🧪 Pruebas Unitarias y Aseguramiento

El repositorio cuenta con una suite estructurada de pruebas en el entorno de desarrollo local mediante **Robolectric**, garantizando robustez antes del empaquetamiento:

- **Suite principal**: `/app/src/test/java/com/example/ExampleUnitTest.kt`
- Para iniciar las pruebas locales, carga el entorno y ejecuta:
  ```bash
  gradle :app:testDebugUnitTest
  ```
  Esto inicia la suite e interactúa con la lógica de verificación de la base de datos PostgreSQL en la nube, asegurando que el controlador, los canales DNS, credenciales de Neon Cloud y la creación del esquema se efectúen correctamente en el host remoto.

---

## 🛠️ Tecnologías Empleadas

- **Lenguaje**: [Kotlin](https://kotlinlang.org/) versión moderna con soporte asíncrono profundo.
- **Lógica Asíncrona**: Kotlin Coroutines & Kotlin Flow para flujos reactivos de datos.
- **UI Framework**: [Jetpack Compose](https://developer.android.com/compose) con componentes Material Design 3 (M3).
- **Base de Datos Local**: [Room DB](https://developer.android.com/training/data-storage/room) con compilador KSP.
- **Base de Datos Remota**: [Neon Serverless PostgreSQL](https://neon.tech/) impulsado por conductores de conexión JDBC.
- **Inteligencia Artificial**: [Gemini Pro / Flash API](https://ai.google.dev/) para el análisis de tickets de compra.
- **Entorno de Compilación**: Gradle (Kotlin DSL con archivos `.gradle.kts`).
