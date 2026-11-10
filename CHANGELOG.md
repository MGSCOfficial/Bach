# Changelog — Sound ID

Todos los cambios notables realizados en el proyecto **Sound ID** se documentan en este archivo.
El formato se basa en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y este proyecto se adhiere a [Semantic Versioning](https://semver.org/lang/es/).

---

## [1.2.0] - 2026-11-10

### Añadido
- **Visualización en Cuadrícula (Grid View)**: Soporte conmutable entre vista de Lista y vista de Cuadrícula (2 columnas) en las pestañas *Canciones* y *Carpetas*, con persistencia de preferencias en `SettingsManager`.
- **Reconocimiento de Jerarquía de Subcarpetas**: Escaneo anatómico en `FolderManager` que desglosa y reconoce cada subcarpeta con archivos de audio como una carpeta independiente con su nombre y recuento de pistas correspondiente.
- **Gestión y Desvinculación de Carpetas**: Botón de desvinculación individual por carpeta en `FoldersScreen` con diálogo de confirmación seguro (sin eliminar archivos físicos del almacenamiento).
- **Componentes `TrackGridCard` y `FolderGridCard`**: Renderizado enriquecido para tarjetas en cuadrícula con carátulas cuadradas de alta resolución e insignias de formato.
- **Firma Determinista en CI**: Inclusión de `debug.keystore` estático en el repositorio para garantizar paridad de firmas binarias entre compilaciones locales y las compiladas en la nube mediante GitHub Actions.

### Corregido
- **Desbloqueo de Campos de Metadatos en Editor**: Configuración de `windowSoftInputMode="adjustResize"` en `AndroidManifest.xml` y holgura mediante `Modifier.imePadding()` en `SongEditorScreen`, resolviendo el bloqueo del teclado virtual y permitiendo editar libremente título, artista, álbum, año, género, pista y notas.
- **Adaptación 100% a Temas en Selector de Carátulas**: Erradicación del color verde estático en `CoverArtPickerSheet`, conectando la paleta a `LocalAccentColor` (Material You) y al negro absoluto `#000000` en modo AMOLED.
- **Actualización de Modelos Gemini (HTTP 404)**: Migración obligatoria de los identificadores retirados `gemini-2.5-flash` hacia los modelos modernos activos `gemini-3.8-flash` y `gemini-3.8-pro`, con migración transparente de claves previas en disco.

---

## [1.1.0] - 2026-11-09

### Añadido
- **Integración Dual de Google Gemini AI**:
  - *Enfoque 1 (Compartir Asistido - Sin API Key)*: Generación de prompts deterministas de catalogación musical para compartir mediante `Intent.ACTION_SEND` o copiar al portapapeles hacia la app o web de Gemini.
  - *Enfoque 2 (Integración Nativa REST - 1 Toque)*: Cliente nativo `GeminiRepository` con llamadas HTTP directas a Google AI Studio utilizando `gemini-3.8-flash`.
- **OCR y Visión Multimodal sobre Carátulas**: Detección de títulos, artistas y sellos discográficos analizando visualmente imágenes adjuntas.
- **Búsqueda y Formateo de Letras con IA**: Generación y formateo de letras limpias o sincronizadas (`.lrc`).
- **Configuración de IA en Ajustes**: Selector de activación, gestión segura de API Key de AI Studio y selector de modelos de Gemini.

---

## [1.0.1] - 2026-11-08

### Añadido
- **Soporte Material You**: Paleta de color dinámico extraída del sistema operativo en Android 12+.
- **Modo Negro AMOLED Puro**: Fondo `#000000` y superficies optimizadas para máxima autonomía en paneles OLED.
- **Escáner SAF Ultrarrápido**: Almacenamiento en caché JSON local (`sound_id_library_cache.json`) logrando arranques instantáneos (<10ms).
- **Expansión de Fuentes de Búsqueda Online**: Soporte para 8 proveedores (iTunes, Deezer, MusicBrainz, Spotify, TheAudioDB, NetEase, Last.fm, Discogs) con activación modular en Ajustes.
- **Ordenamiento Avanzado**: Clasificación de canciones por fecha de última modificación, título, artista, álbum, año y duración.

---

## [1.0.0] - 2026-11-07

### Añadido
- **Editor de Metadatos de Audio**: Edición de etiquetas ID3v2 (MP3) y átomos MP4 (M4A).
- **Reproductor de Previsualización**: Barra de reproducción flotante y preescucha interna antes de aplicar cambios.
- **Incrustación de Letras y Carátulas**: Descarga e inyección de metadatos desde LRCLIB y Cover Art Archive.
