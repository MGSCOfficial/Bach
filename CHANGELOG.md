# Changelog — Bach

Todos los cambios notables realizados en el proyecto **Bach** se documentan en este archivo.
El formato se basa en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y este proyecto se adhiere a [Semantic Versioning](https://semver.org/lang/es/).

---

## [1.6.5] - 2026-11-15

### Añadido & UX Amigable
- **Banner de Actualización Integrado en Biblioteca**: Implementada tarjeta nativa de actualización con la misma estética de una canción (`TrackCard`), ubicada de manera no intrusiva en la parte superior de la lista y cuadrícula. Se desplaza naturalmente junto a la biblioteca sin bloquear la pantalla.
- **Acciones Rápidas en Banner**: Incluye 2 botones de acceso inmediato:
  1. **Icono de Documento**: Despliega el diálogo interactivo con las notas completas de la versión (`AppUpdateDialog`).
  2. **Icono de Flecha de Descarga**: Inicia la descarga directa del APK de la nueva versión desde GitHub Releases.
- **Descarte Temporal por Gesto (Swipe to Dismiss)**: El banner puede deslizarse lateralmente para descartarse durante la sesión activa sin molestar al usuario; reaparecerá amigablemente al reabrir la app hasta que se complete la actualización.

---

## [1.6.4] - 2026-11-15

### Modificado & Optimizado
- **Simplificación del Guardado en el Editor**: Eliminado el botón secundario "Previsualizar" de la barra inferior de acciones en la pantalla de edición, dejando como llamada a la acción principal y exclusiva **"Guardar Archivo"** ocupando todo el ancho para una experiencia de guardado más limpia, directa y sin fricción.
- **Preparación de Prueba de Detección de Actualizaciones**: Despliegue de la versión 1.6.4 en GitHub Releases para validar el flujo completo de detección y descarga de actualización en vivo desde la versión 1.6.3 en el dispositivo.

---

## [1.6.3] - 2026-11-15

### Añadido
- **Detección Automática y Manual de Actualizaciones**: Integración de `UpdateCheckerRepository` con la API de GitHub Releases (`MGSCOfficial/Bach`), notificando al usuario mediante diálogo modal interactivo con notas de versión y descarga directa del APK.
- **Aplicación Granular de Resultados en Línea**: Ahora el usuario puede elegir aplicar **"Ambas"** (Metadatos y Carátula), **"Solo Metadatos"** o **"Solo Carátula"** en cada candidato de búsqueda (iTunes, MusicBrainz, Deezer, etc.).
- **Insignia del Proveedor en Letras**: Cada versión de letra muestra prominentemente su fuente de origen (`LRCLIB`, `NetEase`, `Genius`) con diseño consistente a los metadatos.
- **Enlaces Directos de Obtención de API Keys**: Botones directos en el diálogo de configuración para solicitar claves API en Google AI Studio, Spotify Dashboard, Last.fm, Discogs y Genius.
- **Modelos Oficiales de Gemini**: Presentación exclusiva de los 7 modelos recomendados según AI Studio (`4 Hydrogen`, `3.8 Flash`, `3.6 Flash`, `3.5 Flash Lite`, `3.1 Flash Lite`, `Flash Latest`, `Flash Lite Latest`).

### Modificado & Corregido
- **Corrección de Invocación de Gemini sin Clave**: Eliminado por completo el fallback que copiaba el prompt al portapapeles o abría el menú de compartir al oprimir opciones de IA sin tener API Key. Ahora se muestra un aviso claro requiriendo la clave de Google AI Studio.
- **Eliminación de Musixmatch**: Retirado Musixmatch del catálogo de fuentes de búsqueda lírica y de los ajustes de la aplicación.
- **Optimización de la Barra Superior del Editor**: Eliminado el icono redundante de previsualización en la esquina superior derecha del TopAppBar.

---

## [1.6] - 2026-11-13

### Añadido & Rebranding
- **Rebranding Completo a "Bach"**:
  - Transformación integral de identidad visual y denominación de la aplicación: de "Sound ID" a **Bach** (`by: MGSCOfficial`).
  - Nuevo imagotipo en vector SVG (`bach_logo.svg`) y Android Vector Drawable (`ic_bach_logo.xml`), representando la silueta clásica de Johann Sebastian Bach fusionada con una clave de sol en sus rizos.
  - Generación de iconos adaptativos del sistema (`ic_launcher`, `ic_launcher_round`, `ic_launcher_foreground`, `ic_launcher_background`) con fondo azul nocturno `#08203C` y relieve dorado `#C6A56E`.
  - Nueva sección "Acerca de" en Ajustes con el isotipo de Bach, versión 1.6 y créditos canónicos a MGSCOfficial.
- **Paletas Clásicas Elegantes**:
  - Reemplazo de los colores genéricos por una colección armónica de alta distinción:
    - **Oro Clásico (Bach)** (`#C6A56E`) — Preset principal por defecto.
    - **Azul Nocturno** (`#2563EB`)
    - **Bronce Real** (`#B45309`)
    - **Esmeralda Barroca** (`#059669`)
    - **Púrpura Imperial** (`#7C3AED`)
    - **Vino Tinto** (`#9F1239`)
  - Preservación íntegra de soporte dinámico **Material You** y fondo **Negro AMOLED Puro (`#000000`)**.
- **Modelos de Gemini Actualizados e Inteligentes**:
  - Depuración y filtro inteligente en `GeminiRepository`: erradicación de modelos experimentales obsoletos o incompatibles (`tts`, `image`, `nano`, `banana`, `omni`, `lyria`, `transcribe`, `robotics`, `preview`).
  - Priorización de los alias canónicos `-latest` siempre vigentes: `gemini-flash-latest` (Recomendado), `gemini-flash-lite-latest` (Ultra Rápido) y `gemini-pro-latest` (Máxima Inteligencia), junto a las versiones de producción estables `gemini-2.5-flash`, `gemini-2.5-flash-lite` y `gemini-2.5-pro`.

### Corregido
- **Reparación Determinista del Renombrado de Archivos Físicos**:
  - Diagnóstico e identificación de la causa raíz: `SingleDocumentFile.renameTo()` en Android SAF arrojaba internamente `java.lang.UnsupportedOperationException` sobre URIs de árbol de documentos.
  - Implementación de `DocumentsContract.renameDocument(context.contentResolver, uri, sanitizedNewName)` nativo con fallback a `TreeDocumentFile` e invalidación sincronizada de archivos complementarios `.lrc`, logrando renombrar de forma 100% efectiva los archivos en almacenamiento físico local.
- **Migración de Repositorio en GitHub**:
  - Renombrado del repositorio oficial en GitHub a `mgsc-personal/Bach` y actualización del remote local.

---

## [1.5.0] - 2026-11-13

### Añadido
- **Operaciones Masivas por Carpeta (`BatchOperationsSheet`)**:
  - Nuevo modal bottom sheet accesible desde la barra superior de Carpetas (`FoldersScreen`) mediante el icono `PlaylistAddCheck`.
  - Selector interactivo de carpetas de destino con casilla de selección global ("Seleccionar todas") y conteo de canciones por carpeta.
  - **Renombrado Masivo de Archivos**: Procesa todas las pistas de las carpetas seleccionadas renombrando cada archivo físico según el patrón de metadatos configurado (`%artist% - %title%`), actualizando en tiempo real la caché local y sincronizando archivos `.lrc` asociados.
  - **Búsqueda y Auto-etiquetado Masivo**: Identificación y aplicación automática multi-fuente (iTunes, MusicBrainz, Deezer) de título, artista, álbum, año, género y carátula en lote, con opción de filtro "Solo incompletas o sin carátula" y retardo adaptativo de 250ms para evitar rate-limits.
  - Monitor de progreso en tiempo real con barra lineal porcentual, métricas vivas de éxito / omitidas / errores, y botón de cancelación determinista.

### Corregido & Navegación Persistente
- **Persistencia de Navegación Dentro de Carpetas**:
  - Elevación del estado `selectedFolder` al `MusicViewModel`, evitando que el usuario sea devuelto a la raíz de selección de carpetas tras editar una pista o presionar Atrás.
- **Retención Absoluta de la Posición de Scroll**:
  - Reemplazo de la navegación destructiva `AnimatedContent` en `MainActivity` por una arquitectura desacoplada en dos capas: el `Scaffold` base (Biblioteca, Carpetas, Ajustes) permanece permanentemente vivo en la composición, mientras que `SongEditorScreen` se despliega como capa flotante con `AnimatedVisibility`.
  - Al regresar del editor, la lista mantiene al 100% su índice de scroll exacto, erradicando la molestia de tener que scrollear nuevamente desde el inicio.

---

## [1.4.0] - 2026-11-12

### Añadido
- **Redimensionamiento Quirúrgico de Carátulas**:
  - Selector de resolución de carátula integrado en `CoverArtPickerSheet` y configurable por defecto en ajustes: `Original (sin redimensionar)`, `1000 px`, `960 px`, `600 px`, `500 px` y `250 px`.
  - Motor de compresión y escalado de bitmap preservando relación de aspecto (`Bitmap.createScaledBitmap`) en `AudioTagWriter`, reduciendo el peso innecesario de metadatos en archivos MP3 y M4A.
- **Renombrado Automático de Archivos según Etiquetas de Metadatos**:
  - Motor de resolución de patrones configurables (`FileRenameHelper`) con soporte para `%artist%`, `%title%`, `%album%`, `%track%` y `%year%`.
  - Nueva sección en Configuración con conmutador de activación, edición de plantilla, chips de inserción rápida de variables y vista previa en vivo.
  - Ejecución transaccional tanto en archivos locales como a través de Storage Access Framework (`DocumentFile.renameTo`), sincronizando automáticamente el renombre de archivos complementarios `.lrc` e invalidando la caché de miniaturas sin rescaneo redundante.
  - Indicador visual dinámico en la cabecera del editor mostrando el nombre de destino resultante en tiempo real.

### Corregido & Anti-Slop UI Polish
- **Erradicación de Saltos de Línea y Partición de Palabras en Botones**: Corrección anatómica de la barra inferior de `SongEditorScreen` (`Previsualizar` y `Guardar Archivo`) fijando altura estándar de 48.dp, `singleLine = true`, `softWrap = false` y elipsis controlada, eliminando los quiebres anómalos de texto.
- **Simetría y Balance de Botones de Identificación**: Unificación de dimensiones y etiquetas en los botones de Gemini bajo la carátula (`Por Carátula` / `Por Metadatos` y `Por Audio`), garantizando alineación limpia a una sola línea.
- **Distribución de Columnas en Formulario de Etiquetas**: Rebalanceo del ancho asignado a la fila de `Año` y `Género` (peso 0.35f para Año y 0.65f para Género), erradicando el truncamiento del texto de géneros musicales complejos.
- **Prevención de Truncamiento de Chips en Biblioteca**: Aplicación de `maxLines = 1` y `softWrap = false` en la fila de filtros de `LibraryScreen`, evitando el recorte del chip `Incompletas`.
- **Mitigación de Ruido Cromático (Candy Colors)**: Armonización de los estados de insignia en `TrackCard` con los tonos de superficie de Material Design 3 en lugar de colores saturados discordantes.

---

## [1.3.1] - 2026-11-12

### Añadido
- **Transcripción Acústica Directa con Gemini AI (`Transcribir Audio`)**: Nueva función multimodal en el editor que envía los bytes de audio codificados para que Gemini transcriba fonéticamente la pista de voz real grabada, capturando con exactitud covers, parodias y variaciones vocales sin alucinar la letra original de otro intérprete.
- **Separación Clara de Acciones Líricas**: La tarjeta de letra ahora separa explícitamente `Buscar en LRCLIB` (búsqueda online con hojas de candidatos), `Transcribir Audio (Gemini)` (análisis del archivo sonoro), `Sincronizar Audio` (alineación a marcas temporales [mm:ss.xx]) y `Generar por Texto (IA)` (creación sintética a partir de etiquetas).

### Corregido
- **Saneamiento del Selector Dinámico de Modelos Gemini**: Filtro de exclusión estricto para eliminar modelos no aptos para visión y texto cotidiano como "Nano Banana" (generación de imágenes), "Gemini Omni" (edición de video), "Lyria 3" (generación musical), previsualizaciones TTS de audio y robótica, dejando exclusivamente variantes estables de Flash y Pro.
- **Búsqueda Resiliente Multi-Query en LRCLIB, iTunes y Deezer**: Detección y fallback automático ante pistas complejas de bandas sonoras: sanitización de puntos suspensivos iniciales (ej. `...Baby One More Time`), remoción de corchetes/paréntesis `feat.`, búsqueda combinada de `pista + álbum` y resolución del alias de Jack Black hacia su banda oficial `Tenacious D`.
- **Prevención de Truncamiento de Letras**: Parámetro `maxOutputTokens = 4096` garantizado en las peticiones líricas para evitar cortes o respuestas incompletas de Gemini.

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
