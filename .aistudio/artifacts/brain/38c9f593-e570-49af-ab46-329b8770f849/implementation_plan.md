# Sound ID - Actualización y Corrección Integral

Sound ID (anteriormente SonicTag) se actualiza para solucionar de raíz los problemas de persistencia de archivos reales y selección de metadatos, incorporar escaneo exclusivo de carpetas de música elegidas por el usuario mediante Storage Access Framework (SAF), eliminar pistas demo y añadir la búsqueda, descarga e incrustación de letras de canciones (.lrc y tags internos).

### Decisiones Críticas y Preferencias Confirmadas

> [!IMPORTANT]
> - **Nombre de la Aplicación**: Cambio oficial a **Sound ID** en interfaz, recursos y metadatos de la plataforma.
> - **Gestión de Música**: Eliminación completa de pistas de prueba y selección individual de canciones. El flujo se basará exclusivamente en seleccionar carpetas completas del dispositivo con permisos persistentes de lectura y escritura.
> - **Corrección de Persistencia (Carátula y Etiquetas)**: Escritura directa en los archivos de origen a través de `ContentResolver.openOutputStream(uri, "wt")` y descriptores de archivo reales, garantizando que tanto los metadatos como las carátulas se guarden físicamente en el archivo MP3/M4A del usuario.
> - **Selección de Metadatos Asistida**: Al seleccionar un resultado de MusicBrainz, iTunes o Deezer, se aplicarán inmediatamente todos los campos de metadatos al borrador de edición, manteniendo la posibilidad de editar cada campo manualmente.
> - **Letras de Canciones**: Doble almacenamiento confirmado por el usuario: incrustadas en las etiquetas del archivo de audio (marco `USLT` en MP3 y átomo `©lyr` en M4A) y guardadas como archivo complementario `.lrc` en la misma carpeta.

---

## 1. Visión General y Cambios Principales

1. **Renombrado Completo a Sound ID**:
   - `strings.xml`, `metadata.json`, cabeceras de navegación y pruebas unitarias.
2. **Escaneo y Detección de Carpetas (SAF Persistente)**:
   - Selector de directorios mediante `ACTION_OPEN_DOCUMENT_TREE`.
   - Solicitud y retención de permisos persistentes (`takePersistableUriPermission`) para que la app recuerde las carpetas añadidas entre reinicios.
   - Exploración recursiva de archivos `.mp3` y `.m4a` dentro de las carpetas seleccionadas.
   - Eliminación definitiva del cargador de canciones de prueba y de archivos temporales de demo.
3. **Corrección de Escritura Real en Almacenamiento**:
   - El error anterior se debía a que se intentaba escribir en rutas de archivo estándar bloqueadas por Scoped Storage de Android o en caché local sin volcar los bytes al archivo de origen.
   - Se utilizará `ContentResolver.openInputStream` y `ContentResolver.openOutputStream(targetUri, "wt")` sobre los `DocumentFile` reales.
   - Corrección en la memoria de carátula: al seleccionar una carátula, se descargarán los bytes y se actualizarán inmediatamente en la memoria del modelo para que la previsualización y el guardado conserven la imagen elegida.
4. **Solución a la Selección de Metadatos Online**:
   - Corrección en la vinculación entre los candidatos devueltos por MusicBrainz / iTunes / Deezer y el formulario de edición: al presionar "Elegir", se actualizarán en un solo paso todos los campos de texto y se buscarán las portadas correspondientes.
5. **Módulo de Letras de Canciones (LRCLIB API)**:
   - Integración con la API libre y gratuita de letras **LRCLIB** (`https://lrclib.net/api/get`).
   - Obtención de letra sincronizada y letra en texto plano.
   - Escritura de letra en formato `.lrc` en el directorio de la canción.
   - Incrustación de la letra dentro del archivo de audio:
     - MP3: Marco ID3v2.3 `USLT` (Unsynchronized lyrics/text transcription).
     - M4A: Átomo MP4 `©lyr`.

---

## 2. Experiencia de Usuario y Flujo de Interfaz

### Pantalla Principal (Biblioteca de Carpetas)
- Barra superior con identidad **Sound ID**.
- Panel de carpetas configuradas: lista de directorios vinculados con botón "Añadir Carpeta de Música" y "Re-escanear".
- Pestañas de filtrado: *Todas*, *Sin carátula*, *Sin letra*, *Incompletas*, *MP3*, *M4A*.
- Lista de canciones organizadas con información de archivo, formato, estado de carátula y estado de letra.

### Pantalla de Edición Detallada
- **Sección de Carátula**: Visualización de la carátula actual o nueva seleccionada, selector de imágenes desde línea o almacenamiento.
- **Sección de Letra**:
  - Visualizador/editor de letra con botón "Buscar Letra en Línea".
  - Casillas de verificación para guardar como archivo `.lrc` en la carpeta y/o incrustar en el audio.
- **Formulario de Metadatos**: Título, Artista, Álbum, Año, Género, Pista, Disco.
- **Búsqueda en Línea Unificada**: Botón destacado para consultar bases de datos. Al seleccionar una coincidencia, se pueblan todos los metadatos y la portada automáticamente.
- **Previsualización de Cambios y Guardado Físico**: Muestra los cambios exactos que se escribirán en el archivo de disco y ejecuta la escritura en el URI del usuario.

---

## 3. Arquitectura Técnica y Componentes

```
┌─────────────────────────────────────────────────────────────────┐
│                           Sound ID                              │
├───────────────────────────────┬─────────────────────────────────┤
│    FolderManager & Storage    │   Metadata & Lyrics Engine      │
│  - ACTION_OPEN_DOCUMENT_TREE  │  - AudioTagWriter (Direct SAF)  │
│  - takePersistableUriPerms    │  - MP3: ID3v2.3 + APIC + USLT   │
│  - DocumentFile Tree Scanner  │  - M4A: atoms + covr + ©lyr     │
│  - LRC File Writer in folder  │  - LRCLIB API Integration       │
└───────────────▲───────────────┴────────────────▲────────────────┘
                │                                │
┌───────────────┴────────────────────────────────┴────────────────┐
│                       MusicViewModel                            │
│  - activeFolders: StateFlow<List<FolderEntry>>                  │
│  - libraryTracks: StateFlow<List<AudioTrack>>                   │
│  - editorSession: StateFlow<EditorUiState>                      │
└─────────────────────────────────────────────────────────────────┘
```

---

## 4. Plan de Ejecución por Pasos

1. **Renombrado y Limpieza**:
   - Cambiar nombre a "Sound ID" en `strings.xml`, `metadata.json`, `MainActivity.kt` y pruebas.
   - Eliminar los archivos demo en `assets/sample_tracks/` y el código de carga de muestras.

2. **Gestión de Carpetas y Permisos SAF**:
   - Crear `FolderManager` para persistir URIs de carpetas en `SharedPreferences`.
   - Implementar escáner recursivo de archivos con `DocumentFile` que extrae metadatos con `MediaMetadataRetriever`.

3. **Corrección de Escritura Física de Archivos (`AudioTagWriter`)**:
   - Adaptar `AudioTagWriter` para escribir directamente a través de `ContentResolver.openOutputStream(targetUri, "wt")`.
   - Soporte para marco `USLT` (letra) en MP3 y átomo `©lyr` en M4A.
   - Función para crear o actualizar el archivo `<cancion>.lrc` en la misma carpeta usando `DocumentFile.createFile("text/plain", ...)`.

4. **Cliente de Letras (LRCLIB)**:
   - Implementar `LyricsRepository` que consulte `https://lrclib.net/api/get` con título, artista, álbum y duración.

5. **Corrección de la Selección de Metadatos en UI**:
   - Asegurar que al pulsar un candidato de búsqueda, se actualicen inmediatamente todos los campos del borrador y se cargue su carátula asociada.

6. **Verificación y Pruebas**:
   - Compilación completa con `compile_applet`.
   - Pruebas unitarias de escritura de etiquetas y letras.
