# Changelog — Sound ID

Todos los cambios notables realizados en el proyecto **Sound ID** se documentan en este archivo.
El formato se basa en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y este proyecto se adhiere a [Semantic Versioning](https://semver.org/lang/es/).

---

## [1.3.0] - 2026-11-11

### Añadido
- **Sincronización Forzada de Letras con Audio vía Gemini AI**: Capacidad multimodal para escuchar el flujo de audio de la pista y alinear automáticamente letras en texto plano o con desfase a marcas temporales milimétricas `[mm:ss.xx]` en formato LRC sin alterar el archivo original en disco.
- **Selector y Descubrimiento Dinámico de Modelos de Gemini**: Consulta en vivo de los modelos oficiales disponibles en la API de Google AI Studio con botón de refresco y selección dinámica, resolviendo saturaciones en modelos experimentales.
- **Identificación Acústica y Asistida Sin Carátula**: Posibilidad de identificar canciones mediante análisis de audio acústico directo o deducción por metadatos/nombre de archivo cuando la pista no cuenta con carátula visual.
- **Badges de Formato Dinámicos con Material You (`AudioFormatBadge`)**: Sustitución de colores rígidos estáticos por `secondaryContainer` y `tertiaryContainer` armonizados automáticamente con la paleta activa del dispositivo (Material You).
- **Criterios Avanzados de Ordenación en Biblioteca**: Soporte para ordenar por `Modificado (Más antiguos)`, `Con letra primero` y `Sin letra primero`, integrados tanto en la vista general como en cada subcarpeta de `FoldersScreen`.

### Corregido
- **Mitigación Anatómica de Errores 503 y Timeouts en Gemini**: Optimización de la cascada hacia modelos estables (`gemini-3.5-flash-lite` y `gemini-3.5-flash`) y desconexión inmediata ante saturación del servidor o retrasos (>35s) para evitar bloqueos prolongados de la interfaz.

---

## [1.2.0] - 2026-11-10

### Añadido
- **Selector de Opciones de Letra (`LyricsPickerSheet`)**: Al buscar en LRCLIB ya no se sobrescribe ciegamente la letra. Ahora se despliega una hoja modal con todas las versiones candidatas disponibles (diferenciando `LRC Sincronizada` vs `Texto Plano`, previsualización de estrofas y álbum), permitiendo al usuario elegir cuál aplicar o descartar.
- **Botón de Eliminación Rápida de Letra**: Botón "Borrar" en el encabezado de letra de `SongEditorScreen` para vaciar el borrador lírico con un solo toque.
- **Visualización en Cuadrícula (Grid View)**: Soporte conmutable entre vista de Lista y vista de Cuadrícula (2 columnas) en las pestañas *Canciones* y *Carpetas*, con persistencia en `SettingsManager`.
- **Reconocimiento de Jerarquía de Subcarpetas**: Detección anatómica en `FolderManager` que desglosa y reconoce cada subcarpeta con archivos de audio como carpeta independiente con su propio contador de canciones.
- **Desvinculación Segura de Carpetas**: Botón de desvinculación individual por carpeta en `FoldersScreen` con diálogo de confirmación seguro.

### Corregido
- **Desbloqueo Total de Edición de Campos (`AudioTrack.equals` & `StateFlow`)**: Corrección arquitectónica crítica en `AudioModels.kt`. La sobreescritura previa de `equals()` comparaba únicamente `id == other.id`, provocando que `MutableStateFlow` considerara los estados idénticos tras cada tecla y descartara el 100% de los caracteres tipeados o borrados. Con la comparación estructural completa, todos los campos (`Título`, `Artista`, `Álbum`, `Artista del Álbum`, `Año`, `Género`, `Pista`, `Notas`) responden de forma inmediata y fluida al teclado.
- **Gobernanza Estricta de Guardado de Letras**: En `AudioTagWriter` y `MusicViewModel`, si las opciones "Incrustar letra" y "Guardar archivo .lrc" están apagadas, se garantiza que no se escribe ningún tag USLT en MP3, ningún átomo `©lyr` en M4A, ni se genera archivo `.lrc`. Si el usuario borra la letra y guarda con incrustación activa, los tags antiguos se eliminan limpiamente del archivo.
- **Calibración de Letras con Gemini para Singles y Filtraciones**: Ajuste de prompt en `GeminiRepository` para evitar disclaimers de catalogación tradicional en temas recientes o colaboraciones (como "Tsunami" de DJ Snake, Future y Travis Scott).
- **Adaptación 100% a Temas en Selector de Carátulas**: Erradicación del color verde estático en `CoverArtPickerSheet`, conectando la paleta a `LocalAccentColor` (Material You) y al negro absoluto `#000000` en modo AMOLED.
- **Cascada de Modelos Gemini y Downsampling**: Soporte para `gemini-3.5-flash-lite`, `gemini-flash-lite-latest` y `gemini-3.5-flash` con reintento ante 503 y compresión de carátulas para evitar caídas por timeout.

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
