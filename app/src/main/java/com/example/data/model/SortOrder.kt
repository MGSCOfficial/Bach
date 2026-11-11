package com.example.data.model

enum class SortOrder(val label: String) {
    RECENTLY_MODIFIED("Modificado (Más recientes)"),
    OLDEST_MODIFIED("Modificado (Más antiguos)"),
    HAS_LYRICS_FIRST("Con letra primero"),
    NO_LYRICS_FIRST("Sin letra primero"),
    TITLE_ASC("Nombre (A - Z)"),
    TITLE_DESC("Nombre (Z - A)"),
    ARTIST_ASC("Artista (A - Z)"),
    ALBUM_ASC("Álbum (A - Z)"),
    DURATION_DESC("Duración (Mayor a menor)"),
    DURATION_ASC("Duración (Menor a mayor)"),
    SIZE_DESC("Tamaño (Mayor a menor)")
}
