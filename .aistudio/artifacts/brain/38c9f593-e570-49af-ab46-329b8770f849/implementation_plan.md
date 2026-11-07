# SonicTag - Editor de Metadatos y Portadas Musicales

SonicTag es una aplicación nativa para Android desarrollada con Jetpack Compose y Material Design 3 que permite organizar bibliotecas musicales, buscar y aplicar automáticamente metadatos y carátulas en alta resolución para archivos MP3 y M4A desde bases de datos libres (MusicBrainz, Cover Art Archive, iTunes y Deezer), con previsualización comparativa antes de guardar y reproductor integrado.

### Decisiones Críticas y Preferencias del Usuario

> [!IMPORTANT]
> A partir de las aclaraciones confirmadas en la Fase 1, se han fijado las siguientes especificaciones:
> - **Fuentes en línea integradas**: Todas las gratuitas disponibles sin requerir claves de API: **MusicBrainz API v2**, **Cover Art Archive (Archive.org)**, **iTunes Search API (Apple)** y **Deezer Public API**.
> - **Carga y prueba de archivos**: Selector de archivos del dispositivo mediante Storage Access Framework (SAF) con soporte para MP3 y M4A, además de un paquete de canciones de prueba precargadas para probar la app de forma inmediata en el emulador.
> - **Flujo de trabajo principal**: Flujo canción por canción con búsqueda manual y asistida, selector múltiple de opciones de metadatos y carátulas, previsualización detallada "antes y después", y reproductor de audio integrado para verificar la pista.

---

## 1. Visión General y Concepto Principal

- **Propósito**: Solucionar el problema común de archivos de música descargados con títulos genéricos, artistas desconocidos o carátulas pixeladas/ausentes, permitiendo restaurar la calidad estética y documental de la colección.
- **Audiencia**: Amantes de la música, coleccionistas de audio digital y usuarios que desean mantener sus canciones con etiquetas ID3 y metadatos MP4 perfectamente normalizados.
- **Propuesta de Valor**: Integración simultánea de múltiples motores de búsqueda gratuitos, galería de carátulas en alta resolución con selección interactiva, visor comparativo en tiempo real antes de aplicar cambios irreversibles, y edición tanto en MP3 como en M4A.

---

## 2. Experiencia de Usuario y Diseño Visual

### Flujos Principales de Usuario

1. **Explorador y Organizador de Biblioteca**:
   - Vista principal con pestañas o filtros rápidos: *Todas*, *Sin carátula*, *Metadatos incompletos*, *MP3*, *M4A*.
   - Barra de búsqueda local por título, artista o álbum.
   - Opciones para importar canciones del almacenamiento del teléfono o activar las "Pistas de Demostración".
   - Tarjetas de canción con miniatura de carátula actual, insignia de formato, calidad y estado de etiquetado.

2. **Editor Detallado de Canción y Búsqueda en Línea**:
   - Al pulsar una canción, se abre la pantalla de edición completa.
   - Botón destacado "Buscar en Bases de Datos" que consulta MusicBrainz, iTunes y Deezer en paralelo con el título/nombre de archivo.
   - Pestaña de candidatos con insignias de origen (`MusicBrainz`, `iTunes`, `Deezer`), coincidencia de pista, año, álbum y duración.

3. **Selector Múltiple de Carátulas**:
   - Visualización en cuadrícula de todas las carátulas encontradas (Cover Art Archive 1200px/500px, iTunes 1000px, Deezer XL, además de opción de elegir imagen desde la galería local o cámara).
   - Selector táctil para asignar la carátula deseada.

4. **Previsualización Comparativa "Antes vs. Después"**:
   - Hoja deslizante (Bottom Sheet) o diálogo modal comparativo que muestra en dos columnas el estado actual del archivo contra los nuevos datos propuestos (destacando en color esmeralda los campos modificados).
   - Confirmación explícita antes de escribir las etiquetas en el archivo físico.

5. **Reproductor de Audio Mini Integrado**:
   - Barra de reproducción para escuchar la pista directamente en la app y confirmar auditivamente de qué canción se trata.

### Identidad Visual y Estilo

- **Atmósfera**: *Midnight Studio* — Tema oscuro elegante inspirado en interfaces de producción de audio (fondos en gris carbón profundo `#121214`, superficies elevadas `#1E1E24`, acentos en verde neón esmeralda `#10B981` y púrpura sónico `#8B5CF6`).
- **Tipografía**: Jerarquía limpia y legible con números monoespaciados para duración y metadatos técnicos.
- **Microinteracciones**: Transiciones animadas (`AnimatedContent`), efecto shimmer durante la búsqueda en red, e indicadores de progreso al escribir los metadatos.

---

## 3. Decisiones de Producto y Arquitectura Técnica

### Elección de Tecnologías
- **Motor de Red**: Retrofit + Moshi + OkHttp para llamadas paralelas y concurrentes a MusicBrainz, Cover Art Archive, iTunes y Deezer.
- **Carga de Imágenes**: Coil Compose con soporte para caché en disco y transformaciones de esquinas redondeadas.
- **Lectura y Escritura de Audio**:
  - Lectura inmediata mediante `android.media.MediaMetadataRetriever` y extractor binario de tags.
  - Motor de escritura nativo en Kotlin para marcos ID3v2 (TIT2, TPE1, TALB, TYER, TCON, TRCK, APIC) en archivos MP3 y átomos MP4 (`©nam`, `©ART`, `©alb`, `©day`, `©gen`, `covr`) en archivos M4A.
- **Almacenamiento Local de Estado**: ViewModel con StateFlow para gestión reactiva de la biblioteca en memoria, permitiendo guardar los archivos modificados en el almacenamiento del usuario o en el directorio accesible de música.

```
┌─────────────────────────────────────────────────────────────────┐
│                        SonicTag UI                              │
├───────────────────────────────┬─────────────────────────────────┤
│    Biblioteca & Filtros       │   Editor & Previsualización     │
│  - Tarjetas de canciones      │  - Campos editables             │
│  - Filtro sin carátula        │  - Comparador Antes/Después     │
│  - Mini reproductor           │  - Selector de carátulas        │
└───────────────▲───────────────┴────────────────▲────────────────┘
                │                                │
┌───────────────┴────────────────────────────────┴────────────────┐
│                     MusicViewModel                              │
│   (StateFlow: libraryState, searchResults, editingSession)      │
├───────────────────────────────┬─────────────────────────────────┤
│       MetadataEngine          │       OnlineSearchManager       │
│  - MediaMetadataRetriever     │  - MusicBrainz API Client       │
│  - ID3v2 Tag Writer (MP3)     │  - Cover Art Archive Client     │
│  - MP4 Atom Writer (M4A)      │  - iTunes Search Client         │
│  - Embedded Picture Extractor │  - Deezer Public Client         │
└───────────────────────────────┴─────────────────────────────────┘
```

---

## 4. Plan de Implementación por Fases

1. **Configuración de Dependencias**:
   - Activar `coil-compose` en `build.gradle.kts` para carga de imágenes en Compose.
   - Declarar permisos necesarios (`READ_MEDIA_AUDIO`, `INTERNET`) en `AndroidManifest.xml`.
   - Crear canciones de muestra (.mp3 y .m4a sintetizados/generados con audio válido) en el directorio de assets para pruebas inmediatas.

2. **Capa de Servicios de Metadatos en Línea**:
   - Implementar modelos de datos unificados: `OnlineTrackCandidate` y `CoverArtOption`.
   - Implementar cliente Retrofit para MusicBrainz (`/ws/2/recording` y `/ws/2/release`), Cover Art Archive, iTunes Search API y Deezer Search API.

3. **Motor de Etiquetas de Audio (ID3 / MP4)**:
   - Módulo de lectura mediante `MediaMetadataRetriever`.
   - Módulo de escritura para MP3 (ID3v2.3 con marco APIC para carátula) y M4A (átomo `covr` y átomos de texto).
   - Generador y conversor de carátulas (descarga de bytes, compresión JPEG/PNG, incrustación).

4. **Componentes de Interfaz de Usuario (Jetpack Compose)**:
   - `LibraryScreen`: Lista de canciones con buscador, filtros y botón para agregar archivos o canciones demo.
   - `SongEditorScreen`: Formulario de edición con campos manuales, botón de búsqueda en línea y mini reproductor de audio.
   - `OnlineResultsSheet`: Lista de resultados encontrados en MusicBrainz, iTunes y Deezer.
   - `CoverArtPickerSheet`: Cuadrícula de carátulas disponibles en diferentes resoluciones.
   - `PreviewDiffDialog`: Ventana comparativa de cambios antes de escribir el archivo definitivamente.

5. **Verificación y Pruebas**:
   - Compilación con `compile_applet`.
   - Verificación de búsqueda con datos reales y guardado de etiquetas en canciones de prueba.
