package com.aurum.domain

import com.aurum.data.model.Song

/**
 * Options for sorting song lists.
 */
enum class SongSortBy {
    TITLE,
    ARTIST,
    ALBUM,
    GENRE,
    DURATION,
    DATE_ADDED
}

/**
 * Direction for sorting lists.
 */
enum class SortOrder {
    ASCENDING,
    DESCENDING
}

/**
 * Extension function to sort a list of songs by the given [sortBy] and [order].
 */
fun List<Song>.sortSongs(
    sortBy: SongSortBy = SongSortBy.TITLE,
    order: SortOrder = SortOrder.ASCENDING,
): List<Song> {
    val sorted = when (sortBy) {
        SongSortBy.TITLE -> sortedBy { it.title.lowercase() }
        SongSortBy.ARTIST -> sortedBy { it.artist.lowercase() }
        SongSortBy.ALBUM -> sortedBy { it.album.lowercase() }
        SongSortBy.GENRE -> sortedBy { it.artist.lowercase() }
        SongSortBy.DURATION -> sortedBy { it.duration }
        SongSortBy.DATE_ADDED -> sortedBy { it.dateAdded }
    }
    return if (order == SortOrder.DESCENDING) sorted.reversed() else sorted
}

/**
 * Extension function to filter a list of songs by a query string.
 */
fun List<Song>.filterByQuery(query: String): List<Song> {
    if (query.isBlank()) return this
    val q = query.trim().lowercase()
    return filter {
        it.title.lowercase().contains(q) ||
        it.artist.lowercase().contains(q) ||
        it.album.lowercase().contains(q)
    }
}
