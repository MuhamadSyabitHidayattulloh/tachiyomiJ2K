package eu.kanade.tachiyomi.data.track

import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.source.Source

/**
 * An Enhanced Track Service will never prompt the user to match a manga with the remote.
 * It is expected that such Track Service can only work with specific sources and unique IDs.
 */
interface EnhancedTrackService {
    /**
     * This TrackService will only work with the sources that are accepted by this filter function.
     */
    fun accept(source: Source): Boolean = source::class.qualifiedName in getAcceptedSources()

    /**
     * Fully qualified source classes that this track service is compatible with.
     */
    fun getAcceptedSources(): List<String>

    fun loginNoop()

    /**
     * match is similar to TrackService.search, but only return zero or one match.
     */
    suspend fun match(manga: Manga): TrackSearch?

    /**
     * Checks whether the provided source/track/manga triplet is from this TrackService
     */
    fun isTrackFrom(
        track: Track,
        manga: Manga,
        source: Source?,
    ): Boolean = track.tracking_url == manga.url && source?.let { accept(it) } == true

    /**
     * Migrates the given track for the manga to the newSource, if possible
     */
    fun migrateTrack(
        track: Track,
        manga: Manga,
        newSource: Source,
    ): Track? =
        if (accept(newSource)) {
            track.also { track.tracking_url = manga.url }
        } else {
            null
        }
}
