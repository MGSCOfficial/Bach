# Bach 🎼

<p align="center">
  <img src="https://raw.githubusercontent.com/mgsc-personal/Bach/main/app/src/main/res/drawable/bach_logo.svg" alt="Bach Logo" width="120" height="120" onerror="this.style.display='none'"/>
</p>

<p align="center">
  <strong>Gestor clásico de metadatos, carátulas y letras sincronizadas para Android</strong><br>
  <em>by: MGSCOfficial</em>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android_10%2B-green.svg?style=flat-square&logo=android" alt="Android Platform"/>
  <img src="https://img.shields.io/badge/UI-Jetpack_Compose_Material_3-blue.svg?style=flat-square&logo=jetpackcompose" alt="Jetpack Compose"/>
  <img src="https://img.shields.io/badge/Kotlin-2.1.0-purple.svg?style=flat-square&logo=kotlin" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/Theme-Bach_Gold_%26_AMOLED-darkgreen.svg?style=flat-square" alt="Bach Gold & AMOLED"/>
  <img src="https://img.shields.io/badge/License-MIT-orange.svg?style=flat-square" alt="License"/>
</p>

---

## 📌 Descripción

**Bach** es una aplicación moderna y de alto rendimiento para Android desarrollada nativamente en **Kotlin** y **Jetpack Compose**. Diseñada para amantes de la música y coleccionistas audiófilos que requieren organizar, etiquetar, renombrar y embellecer bibliotecas locales de archivos en formatos **MP3** y **M4A / AAC**.

Permite identificar pistas de audio, descargar carátulas en alta definición (`1000x1000`), sincronizar letras en formato `.lrc` e incrustar metadatos definitivos de forma no destructiva con cero fricción visual.

---

## ✨ Características Principales

### 🔍 Identificación y Búsqueda Online Multi-Fuente
* **Búsqueda Paralela**: Consulta simultánea en **iTunes**, **Deezer**, **MusicBrainz**, **TheAudioDB** y **NetEase Cloud Music** (gratuitas), con soporte extensible para **Spotify**, **Last.fm** y **Discogs** con API Keys.
* **Búsqueda Avanzada por Campos**: Filtrado granular por Título, Artista y Álbum.
* **Algoritmo de Coincidencia Inteligente**: Detección automática de *placeholders* (e.g. `Track 3`, `Pista 05`) que recupera la lista completa del álbum y prioriza deterministamente la pista correcta con un 99% de confianza.
* **Previsualización Interactiva**: Modal de comparación que muestra carátula, año, género, pista y disco antes de aplicar cambios al archivo.

### 🎨 Experiencia Visual Premium (Material You & AMOLED)
* **Material You Dinámico**: Paleta de colores adaptativa extraída en tiempo real del fondo de pantalla del sistema (Android 12+).
* **Negro AMOLED Puro (`#000000`)**: Fondo 100% negro azabache libre de superposiciones tonales grises, ideal para máxima autonomía en paneles OLED.
* **Acentos Personalizables**: 6 presets temáticos (Esmeralda, Violeta, Cian, Ámbar, Rosa, Azul) para usuarios que prefieren estilos fijos.

### 📁 Gestión Eficiente de Biblioteca y Carpetas
* **Navegación por Pestañas**: Vistas dedicadas de **Canciones**, **Carpetas** y **Ajustes** con barra de navegación flotante ergonómica.
* **Drilldown de Carpetas**: Inspección de canciones filtradas por carpeta individual con contador en vivo.
* **Ordenamiento Multidimensional**: Reorganiza tu colección por:
  * Fecha de modificación (recientes / antiguos)
  * Título alfabético (A-Z / Z-A)
  * Artista y Álbum
  * Duración y Tamaño en disco
* **Filtros Rápidos**: Acceso instantáneo a canciones *Sin Carátula*, *Sin Letra* o *Incompletas*.

### 📜 Gestor de Letras Sincronizadas (.LRC)
* **Integración con LRCLIB**: Obtención directa de letras con marcas de tiempo precisas (*timestamped*).
* **Opciones Configurables**: Incrustación de la letra directamente en la etiqueta de audio y/o guardado automático como archivo complementario `.lrc`.
* **Protección contra Datos Nulos**: Validación estricta que impide que resultados vacíos o `"null"` sobrescriban metadatos válidos existentes.

### ⚡ Rendimiento Ultrarrápido
* **Motor `FastTagExtractor`**: Análisis de cabeceras ID3v2/MP4 en submilisegundos mediante streaming binario directo sobre Storage Access Framework (SAF), logrando escanear bibliotecas masivas (+2,000 canciones) en pocos segundos.
* **Caché de Miniaturas LRU**: Carga asíncrona fluida de carátulas sin congelar el hilo principal ni agotar la memoria RAM.

---

## 🛠️ Arquitectura y Tecnologías

| Componente | Tecnología |
| :--- | :--- |
| **Lenguaje** | [Kotlin 2.1.0](https://kotlinlang.org/) |
| **Framework UI** | [Jetpack Compose](https://developer.android.com/jetpack/compose) con [Material 3](https://m3.material.io/) |
| **Asincronía & Estado** | Coroutines, Kotlin Flow, StateFlow, ViewModel |
| **Red & APIs** | [OkHttp 4.12](https://square.github.io/okhttp/) (Parallel Deferred Async HTTP Requests) |
| **Carga de Imágenes** | [Coil 2.6](https://coil-kt.github.io/coil/) |
| **Motor de Audio** | `MediaMetadataRetriever` + Parsers Binarios ID3v2/QuickTime |
| **Persistencia** | Android SharedPreferences / DataStore |

---

## 🚀 Compilación e Instalación

### Requisitos Previos
* Android Studio Ladybug / Koala o superior
* JDK 17 (Eclipse Temurin o Microsoft OpenJDK recomendados)
* Dispositivo Android con depuración USB activa (Android 10+, API 29+)

### Pasos de Construcción

1. **Clonar el repositorio**:
   ```bash
   git clone https://github.com/mgsc-personal/Bach.git
   cd Bach
   ```

2. **Ejecutar pruebas unitarias**:
   ```bash
   ./gradlew testDebugUnitTest
   ```

3. **Compilar el APK de depuración**:
   ```bash
   ./gradlew assembleDebug
   ```

4. **Instalar en dispositivo conectado vía ADB**:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 📄 Licencia

Este proyecto está bajo la Licencia [MIT](LICENSE). Puedes utilizarlo, modificarlo y distribuirlo libremente.
