package com.example.data.model

enum class SortOrder(val label: String) {
    RECENTLY_MODIFIED("Modificado recientemente"),
    TITLE_ASC("Nombre (A - Z)"),
    TITLE_DESC("Nombre (Z - A)"),
    ARTIST_ASC("Artista (A - Z)"),
    ALBUM_ASC("Álbum (A - Z)"),
    DURATION_DESC("Duración (Mayor a menor)"),
    DURATION_ASC("Duración (Menor a mayor)"),
    SIZE_DESC("Tamaño (Mayor a menor)")
}
