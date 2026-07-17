# Lupa Free

> Una lupa digital minimalista, ligera y **100 % libre de anuncios** para Android. Sin cuentas, sin telemetría, sin permisos ocultos: solo la cámara, una linterna y un zoom lineal.

---

## Tabla de contenidos

- [Características](#características)
- [Capturas](#capturas)
- [Stack técnico](#stack-técnico)
- [Arquitectura](#arquitectura)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Requisitos](#requisitos)
- [Compilar e instalar](#compilar-e-instalar)
- [Build de release (producción)](#build-de-release-producción)
- [Uso](#uso)
- [Permisos](#permisos)
- [Configuración](#configuración)
- [Decisiones de diseño](#decisiones-de-diseño)
- [Limitaciones conocidas](#limitaciones-conocidas)
- [Roadmap](#roadmap)
- [Contribuir](#contribuir)
- [Licencia](#licencia)
- [Créditos](#créditos)

---

## Características

- 🔍 **Zoom lineal** controlado por un slider flotante y translúcido, con etiqueta en vivo del ratio (1.0x–8.0x según hardware).
- 🔦 **Linterna** (torch) con un toque. El botón se oculta automáticamente en dispositivos sin flash.
- ❄️ **Congelar frame**: captura el frame actual en memoria (sin tocar disco) y lo muestra como overlay. Toque de nuevo para reanudar el preview en vivo.
- 🎯 **Enfoque automático** con tap-to-focus. Toque cualquier punto del preview para enfocar ahí (AF + AE con auto-cancel a los 3 s).
- 📋 **Menú "Más opciones"** (3 puntos) con licencia MIT, acceso a permisos de la app, enlace a GitHub y botón de donación.
- 🌑 **OLED-friendly**: fondo negro puro, estilo *liquid glass* (superficies translúcidas con borde sutil).
- 🔒 **Sin red**: el binario no hace ninguna llamada de red. Verificable inspeccionando el código.
- 🚫 **Sin anuncios, sin trackers, sin SDKs de terceros**.

---

## Capturas

> _Pendiente: añade capturas en `docs/screenshots/` y reemplaza los enlaces._

| Cámara | Congelado | Menú | Licencia |
|:---:|:---:|:---:|:---:|
| _pendiente_ | _pendiente_ | _pendiente_ | _pendiente_ |

---

## Stack técnico

| Capa | Tecnología | Versión |
|---|---|---|
| Lenguaje | Kotlin | 2.2.10 |
| UI | Jetpack Compose (BOM) | 2026.02.01 |
| Material | Material 3 | (via BOM) |
| Cámara | CameraX (`core`, `camera2`, `lifecycle`, `view`) | 1.4.0 |
| Lifecycle | `lifecycle-runtime-ktx`, `lifecycle-viewmodel-compose`, `lifecycle-runtime-compose` | 2.6.1 / 2.8.4 |
| Build | AGP | 9.3.0 |
| SDK | `minSdk` 30, `targetSdk` 36, `compileSdk` 36 | |

Sin `accompanist`, sin Hilt, sin Room, sin Retrofit: el proyecto cabe en ~600 líneas de Kotlin.

---

## Arquitectura

Patrón **MVVM** con un único `ViewModel` y estado expuesto como `StateFlow`. La UI es *stateless*: solo lee estado y dispara intents.

```
┌──────────────────────────────────────────────────────────┐
│  MainActivity                                             │
│    └─ enableEdgeToEdge (SystemBarStyle.dark TRANSPARENT)  │
│    └─ setContent { LupaFreeTheme + MagnifierScreen() }    │
└──────────────────────────────────────────────────────────┘
                          │
                          ▼
┌──────────────────────────────────────────────────────────┐
│  MagnifierScreen (Compose)                                │
│    ├─ Permission gate  (rememberLauncherForActivityResult)│
│    ├─ CameraLayer      (AndroidView + PreviewView)        │
│    ├─ FreezeOverlay    (Image con bitmap en RAM)          │
│    ├─ ZoomLabelAndSlider (label + Slider, glass)          │
│    ├─ TopControlBar    (3 GlassFab: torch, freeze, more)  │
│    ├─ MoreOptionsSheet (ModalBottomSheet)                 │
│    └─ LicenseDialog    (AlertDialog con MIT completo)     │
└──────────────────────────────────────────────────────────┘
                          │
                          ▼
┌──────────────────────────────────────────────────────────┐
│  MagnifierViewModel                                       │
│    ├─ state: StateFlow<MagnifierUiState>                  │
│    ├─ bindCamera(lifecycleOwner, previewView)             │
│    ├─ onZoomChange(Float)                                 │
│    ├─ toggleTorch()                                       │
│    ├─ toggleFreeze()                                      │
│    ├─ focusAt(x, y)                                       │
│    └─ onPermissionResult(Boolean)                         │
└──────────────────────────────────────────────────────────┘
                          │
                          ▼
┌──────────────────────────────────────────────────────────┐
│  CameraX                                                  │
│    ├─ ProcessCameraProvider.bindToLifecycle(...)          │
│    ├─ CameraControl.setLinearZoom(0f..1f)                 │
│    ├─ CameraControl.enableTorch(Boolean)                  │
│    ├─ CameraControl.startFocusAndMetering(action)         │
│    └─ PreviewView.getBitmap()  → ImageBitmap en RAM       │
└──────────────────────────────────────────────────────────┘
```

### Estado (`MagnifierUiState`)

```kotlin
data class MagnifierUiState(
    val zoom: Float = 0f,
    val isTorchOn: Boolean = false,
    val hasFlashUnit: Boolean = false,
    val isFrozen: Boolean = false,
    val frozenImage: ImageBitmap? = null,
    val hasCameraPermission: Boolean = false,
    val minZoomRatio: Float = 1f,
    val maxZoomRatio: Float = 1f,
    val currentZoomRatio: Float = 1f,
    val errorMessage: String? = null,
)
```

### Ciclo de vida de la cámara

- `bindCamera()` llama `ProcessCameraProvider.getInstance(...)` con `addListener` + `ContextCompat.getMainExecutor`.
- `bindToLifecycle(LocalLifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)` deja que CameraX abra/cierre la cámara automáticamente con la actividad.
- `PreviewView.ImplementationMode.COMPATIBLE` (TextureView) es **obligatorio** para que `getBitmap()` funcione al congelar.
- `onCleared()` hace `provider.unbindAll()` y `bitmap.recycle()` para liberar memoria del frame congelado.

### Freeze frame

No se detiene la cámara. Se captura `PreviewView.getBitmap()` (en `Dispatchers.Main`, marcado `@UiThread`) y se compone como `Image` superpuesto al `PreviewView`. Torch, zoom y focus siguen funcionando debajo del frame congelado. Al descongelar, `bitmap.recycle()` y `currentBitmap = null`.

---

## Estructura del proyecto

```
LupaFree/
├── app/
│   ├── build.gradle.kts                 ← Cámara, Compose, lifecycle, icons-extended
│   └── src/main/
│       ├── AndroidManifest.xml          ← CAMERA, FLASHLIGHT, portrait
│       ├── java/com/example/lupafree/
│       │   ├── MainActivity.kt          ← enableEdgeToEdge + host
│       │   └── ui/
│       │       ├── MagnifierViewModel.kt   ← Estado + acciones
│       │       ├── MagnifierScreen.kt      ← UI Compose completa
│       │       └── theme/
│       │           ├── Color.kt
│       │           ├── Theme.kt          ← LupaFreeTheme(darkTheme, dynamicColor)
│       │           └── Type.kt
│       └── res/
│           ├── values/
│           │   ├── colors.xml
│           │   ├── strings.xml          ← Textos UI
│           │   └── themes.xml           ← windowBackground negro
│           ├── drawable/
│           ├── mipmap-*/
│           └── xml/
├── gradle/
│   └── libs.versions.toml               ← Catálogo de versiones
├── build.gradle.kts
├── settings.gradle.kts
├── LICENCE.md                           ← Este archivo
└── README.md                            ← Este archivo
```

---

## Requisitos

- **Android Studio** Ladybug (2024.2.1) o superior (necesario para AGP 9.3).
- **JDK 11** o superior.
- **Android SDK** 36 instalado (con `platforms;android-36` y `build-tools;36.x`).
- **Dispositivo físico o emulador** con Android 11 (API 30) o superior y cámara trasera.

---

## Compilar e instalar

### Desde la línea de comandos

```bash
# Debug APK en app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:assembleDebug

# Instalar en el dispositivo conectado
./gradlew :app:installDebug

# Limpiar build
./gradlew clean
```

### Desde Android Studio

1. `File → Open` → seleccionar la carpeta del proyecto.
2. Esperar a que Gradle termine la sincronización.
3. `Run → Run 'app'` (Shift+F10) con un dispositivo/emulador conectado.

---

## Build de release (producción)

El build de release aplica **R8** (minificación y obfuscación) + **resource shrinking**, y firma el APK con un keystore propio. El resultado es un APK de ~5 MB listo para distribuir.

### 1. Generar el keystore (una sola vez)

```bash
mkdir -p ~/keystores
keytool -genkey -v \
  -keystore ~/keystores/lupafree-release.keystore \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias lupafree \
  -storepass <TU_PASSWORD> \
  -keypass <TU_PASSWORD> \
  -dname "CN=Lupa Free, O=Lupa Free Contributors, L=<TU_CIUDAD>, ST=<TU_ESTADO>, C=<TU_PAIS_ISO2>"
```

> 🔒 **Guarda el keystore y la contraseña en un lugar seguro** (gestor de contraseñas, copia offline cifrada). Si los pierdes no podrás actualizar la app en Play Store — Google te obligará a publicarla como app nueva con un `applicationId` distinto.

### 2. Configurar las credenciales (fuera del repo)

Añade las siguientes variables a `~/.gradle/gradle.properties` (NO al `gradle.properties` del proyecto):

```properties
LUPAFREE_KEYSTORE_PATH=/Users/<tu_usuario>/keystores/lupafree-release.keystore
LUPAFREE_KEYSTORE_PASSWORD=<TU_PASSWORD>
LUPAFREE_KEY_ALIAS=lupafree
LUPAFREE_KEY_PASSWORD=<TU_PASSWORD>
```

El `app/build.gradle.kts` las lee con `providers.gradleProperty(...)` y `System.getenv(...)` como fallback. Así las credenciales nunca se commitean.

### 3. Compilar el APK firmado

```bash
./gradlew :app:assembleRelease
```

Resultado:

| Archivo | Tamaño aprox. | Notas |
|---|---|---|
| `app/build/outputs/apk/release/app-release.apk` | ~5 MB | Firmado con tu keystore, R8 activo |

> Si el keystore no existe, el build produce `app-release-unsigned.apk` (sin firmar, `adb install` lo rechaza). El build NO falla — degrada silenciosamente.

### 4. Instalar en un dispositivo físico

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

### 5. Verificar la firma (opcional)

```bash
$ANDROID_HOME/build-tools/<version>/apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
```

Debe mostrar el DN que pusiste en el paso 1.

### Comparativa de tamaños

| Build | Tamaño | R8 | Recursos | Firmado |
|---|---:|:---:|:---:|:---:|
| `assembleDebug` | ~68 MB | ❌ | ❌ | ❌ (debug key) |
| `assembleRelease` | ~5 MB | ✅ | ✅ | ✅ |

### Subir a Play Store

1. Crea la app en [Google Play Console](https://play.google.com/console).
2. Activa **Play App Signing** (recomendado): Google guarda una copia de seguridad de tu clave y la usa para firmar las actualizaciones que subas.
3. Sube `app-release.apk` o, preferiblemente, genera un **Android App Bundle**:
   ```bash
   ./gradlew :app:bundleRelease
   # Resultado: app/build/outputs/bundle/release/app-release.aab
   ```
   El AAB es el formato que Google prefiere desde agosto de 2021 — genera APKs optimizados por dispositivo en su servidor.

---

## Uso

1. Abre **Lupa Free** desde el lanzador.
2. Acepta el permiso de cámara cuando el sistema lo solicite.
3. Apunta a lo que quieras ampliar.
4. **Desliza el slider** inferior para hacer zoom (1.0x → máximo del sensor).
5. **Toca el icono de rayo** para encender/apagar la linterna.
6. **Toca el icono de pausa** para congelar el frame actual. Toca de nuevo (icono ▶) para reanudar.
7. **Toca cualquier punto** del preview para reenfocar ahí.
8. **Toca los 3 puntos** (esquina superior derecha) para abrir el menú (licencia, permisos, GitHub, donar).

La pantalla se mantiene encendida mientras la app está en primer plano (no requiere `WAKE_LOCK`).

---

## Permisos

| Permiso | Por qué | Obligatorio |
|---|---|---|
| `android.permission.CAMERA` | Renderizar el preview y aplicar zoom/focus | Sí |
| `android.permission.FLASHLIGHT` | Encender la linterna (declarado por el SO en algunos OEM) | Sí |
| `<uses-feature android:name="android.hardware.camera" android:required="true" />` | Filtra dispositivos sin cámara en Play Store | — |

**No solicita** `INTERNET`, `ACCESS_NETWORK_STATE`, ni ningún permiso de almacenamiento: el frame congelado vive exclusivamente en RAM.

---

## Configuración

### Cambiar los enlaces del menú

Edita el objeto `AppLinks` en `app/src/main/java/com/example/lupafree/ui/MagnifierScreen.kt`:

```kotlin
private object AppLinks {
    const val GITHUB = "https://github.com/your-username/lupafree"
    const val DONATE = "https://github.com/sponsors/your-username"
}
```

### Cambiar el autor / año de la licencia MIT

Edita las dos constantes en el mismo archivo:

```kotlin
private const val MIT_LICENSE_YEAR = "2024"
private const val MIT_LICENSE_AUTHOR = "Lupa Free Contributors"
```

El texto completo del diálogo `LicenseDialog` se reconstruye a partir de estas constantes.

### Cambiar el color de fondo OLED

`MagnifierScreen` raíz usa `Color.Black` (`#000000`). Si prefieres un negro menos profundo (que disimule mejor el banding OLED en grises muy oscuros), cambia a `Color(0xFF050505)`.

---

## Decisiones de diseño

- **Sin blur real (RenderEffect)**: requiere API 31+. Se simula el efecto "liquid glass" con superficies translúcidas (`Color.Black.copy(alpha = 0.40f)`) + borde sutil (`Color.White.copy(alpha = 0.20f)`) + `elevation = 0.dp`. Compatible con `minSdk = 30`.
- **`PreviewView.ImplementationMode.COMPATIBLE`** y no `PERFORMANCE`: el primero usa `TextureView` y permite `getBitmap()`; el segundo usa `SurfaceView` que no expone bitmap. La diferencia de rendimiento en dispositivos modernos es despreciable.
- **`PreviewView.ScaleType.FILL_CENTER`**: llena la pantalla recortando bordes, sin letterboxing. Ideal para una lupa.
- **Slider con `valueRange = 0f..1f`**: el `linearZoom` de CameraX es continuo y normalizado. El ratio real (`1.0x..maxZoomRatio`) se calcula en el ViewModel y se muestra como etiqueta.
- **Sin Hilt ni Koin**: el ViewModel se obtiene con `viewModel()` del scope por defecto (la Activity). Para una app tan pequeña, un contenedor de DI sería sobreingeniería.
- **Sin Room/DataStore**: la app no persiste nada entre sesiones (cero estado en disco).
- **Orientación bloqueada a portrait** (`android:screenOrientation="portrait"`): la cámara y la UI están optimizadas para vertical; rotar añadiría complejidad de reinicio de `PreviewView` sin beneficio real.
- **`windowBackground = @android:color/black`** en `themes.xml`: evita el "flash" blanco del splash del sistema al hacer cold start.

---

## Limitaciones conocidas

- **Una sola cámara**: solo la trasera. No hay selector frontal/trasera.
- **Sin captura a archivo**: el freeze es in-memory. Si el sistema mata la app, el frame se pierde. Esto es deliberado (ver "Sin rastreo" arriba).
- **Sin grabación de vídeo**: solo preview en vivo.
- **Sin pinch-to-zoom**: el zoom solo se controla con el slider, por precisión.
- **Sin historial de medidas**: si más adelante se añaden retículas o medición, no hay persistencia.
- **Traducción**: la UI está en español. No hay `values-en/strings.xml` aún.

---

## Roadmap

- [ ] `values-en/strings.xml` con la UI en inglés.
- [ ] Selector de cámara frontal/trasera.
- [ ] Pinch-to-zoom opcional (toggle en el menú).
- [ ] Retícula de medición con calibración por objeto de referencia (tarjeta de crédito, etc.).
- [ ] Captura opcional a archivo (con un interruptor on/off explícito).
- [ ] Tema claro automático cuando el sistema lo pida (override del OLED black).
- [ ] Tests de UI con `createAndroidComposeRule<ComponentActivity>` y un fake `MagnifierViewModel`.

---

## Contribuir

Pull requests bienvenidos. Antes:

1. Asegúrate de que `./gradlew :app:assembleDebug` pasa sin warnings.
2. Mantén la app libre de dependencias que requieran red, anuncios o tracking.
3. Respeta el minimalismo: si una feature requiere más de ~100 líneas de código, plantéalo primero en un *issue*.

---

## Licencia

Este proyecto se distribuye bajo la **Licencia MIT**. Ver [`LICENCE.md`](./LICENCE.md) para el texto completo.

```
MIT License — Copyright (c) 2024 Lupa Free Contributors
```

---

## Créditos

- **CameraX** — AndroidX / Google.
- **Jetpack Compose** y **Material 3** — AndroidX / Google.
- Iconos — [Material Symbols](https://fonts.google.com/icons) (Apache 2.0).

Sin frameworks de UI de terceros, sin assets descargados en runtime, sin servicios externos.
