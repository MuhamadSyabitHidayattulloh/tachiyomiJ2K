package eu.kanade.tachiyomi.data.cache

import android.content.Context
import android.text.format.Formatter
import coil.imageLoader
import coil.memory.MemoryCache
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.util.storage.DiskUtil
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.system.withIOContext
import eu.kanade.tachiyomi.util.system.withUIContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * Class used to create cover cache.
 * It is used to store the covers of the library.
 * Names of files are created with the md5 of the thumbnail URL.
 *
 * @param context the application context.
 * @constructor creates an instance of the cover cache.
 */
class CoverCache(
    val context: Context,
) {
    companion object {
        private const val COVERS_DIR = "covers"
        private const val CUSTOM_COVERS_DIR = "covers/custom"
        private const val ONLINE_COVERS_DIR = "online_covers"
    }

    /** Cache directory used for cache management.*/
    private val cacheDir = getCacheDir(COVERS_DIR)

    /** Cache directory used for custom cover cache management.*/
    private val customCoverCacheDir = getCacheDir(CUSTOM_COVERS_DIR)

    /** Cache directory used for covers not in library management.*/
    private val onlineCoverDirectory = File(context.cacheDir, ONLINE_COVERS_DIR).also { it.mkdirs() }

    private val maxOnlineCacheSize = 50L * 1024L * 1024L // 50 MB

    private var lastClean = 0L

    /**
     * The interval after which this cache should be invalidated. 1 hour shouldn't cause major
     * issues, as the cache is only used for UI feedback.
     */
    private val renewInterval = TimeUnit.HOURS.toMillis(1)

    fun getChapterCacheSize(): String = Formatter.formatFileSize(context, DiskUtil.getDirectorySize(cacheDir))

    fun getOnlineCoverCacheSize(): String = Formatter.formatFileSize(context, DiskUtil.getDirectorySize(onlineCoverDirectory))

    suspend fun deleteOldCovers() {
        val db = Injekt.get<DatabaseHelper>()
        var deletedSize = 0L
        val urls =
            db.getFavoriteMangas().executeOnIO().mapNotNull {
                it.thumbnail_url?.let { url -> return@mapNotNull DiskUtil.hashKeyForDisk(url) }
                null
            }
        val files = cacheDir.listFiles()?.iterator() ?: return
        while (files.hasNext()) {
            val file = files.next()
            if (file.isFile && file.name !in urls) {
                deletedSize += file.length()
                file.delete()
            }
        }
        withUIContext {
            context.toast(
                context.getString(
                    R.string.deleted_,
                    Formatter.formatFileSize(context, deletedSize),
                ),
            )
        }
    }

    /**
     * Clear out all online covers
     */
    suspend fun deleteAllCachedCovers() {
        val directory = onlineCoverDirectory
        var deletedSize = 0L
        val files =
            directory.listFiles()?.sortedBy { it.lastModified() }?.iterator() ?: return
        while (files.hasNext()) {
            val file = files.next()
            deletedSize += file.length()
            file.delete()
        }
        withContext(Dispatchers.Main) {
            context.toast(
                context.getString(
                    R.string.deleted_,
                    Formatter.formatFileSize(context, deletedSize),
                ),
            )
        }
        context.imageLoader.memoryCache?.clear()

        lastClean = System.currentTimeMillis()
    }

    /**
     * Clear out online covers until its under a certain size
     */
    suspend fun deleteCachedCovers() {
        withIOContext {
            if (lastClean + renewInterval < System.currentTimeMillis()) {
                try {
                    val directory = onlineCoverDirectory
                    val size = DiskUtil.getDirectorySize(directory)
                    if (size <= maxOnlineCacheSize) {
                        return@withIOContext
                    }
                    var deletedSize = 0L
                    val files =
                        directory.listFiles()?.sortedBy { it.lastModified() }?.iterator()
                            ?: return@withIOContext
                    while (files.hasNext()) {
                        val file = files.next()
                        deletedSize += file.length()
                        file.delete()
                        if (size - deletedSize <= maxOnlineCacheSize) {
                            break
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                }
                lastClean = System.currentTimeMillis()
            }
        }
    }

    /**
     * Returns the custom cover from cache.
     *
     * @param manga the manga.
     * @return cover image.
     */
    fun getCustomCoverFile(manga: Manga): File = File(customCoverCacheDir, DiskUtil.hashKeyForDisk(manga.id.toString()))

    /**
     * Saves the given stream as the manga's custom cover to cache.
     *
     * @param manga the manga.
     * @param inputStream the stream to copy.
     * @throws IOException if there's any error.
     */
    @Throws(IOException::class)
    fun setCustomCoverToCache(
        manga: Manga,
        inputStream: InputStream,
    ) {
        getCustomCoverFile(manga).outputStream().use {
            inputStream.copyTo(it)
            context.imageLoader.memoryCache?.remove(MemoryCache.Key(manga.key()))
        }
    }

    /**
     * Delete custom cover of the manga from the cache
     *
     * @param manga the manga.
     * @return whether the cover was deleted.
     */
    fun deleteCustomCover(manga: Manga): Boolean {
        val result =
            getCustomCoverFile(manga).let {
                it.exists() && it.delete()
            }
        context.imageLoader.memoryCache?.remove(MemoryCache.Key(manga.key()))
        return result
    }

    /**
     * Returns the cover from cache.
     *
     * @param thumbnailUrl the thumbnail url.
     * @return cover image.
     */
    fun getCoverFile(manga: Manga): File {
        val hashKey = DiskUtil.hashKeyForDisk((manga.thumbnail_url.orEmpty()))
        return if (manga.favorite) {
            File(cacheDir, hashKey)
        } else {
            File(onlineCoverDirectory, hashKey)
        }
    }

    fun deleteFromCache(name: String?) {
        if (name.isNullOrEmpty()) return
        val file = getCoverFile(MangaImpl().apply { thumbnail_url = name })
        context.imageLoader.memoryCache?.remove(MemoryCache.Key(file.name))
        if (file.exists()) file.delete()
    }

    /**
     * Delete the cover file from the disk cache and optional from memory cache
     *
     * @param thumbnailUrl the thumbnail url.
     * @return status of deletion.
     */
    fun deleteFromCache(
        manga: Manga,
        deleteCustom: Boolean = true,
    ) {
        // Check if url is empty.
        if (manga.thumbnail_url.isNullOrEmpty()) return

        // Remove file
        val file = getCoverFile(manga)
        if (deleteCustom) deleteCustomCover(manga)
        if (file.exists()) {
            context.imageLoader.memoryCache?.remove(MemoryCache.Key(manga.key()))
            file.delete()
        }
    }

    private fun getCacheDir(dir: String): File =
        context.getExternalFilesDir(dir)
            ?: File(context.filesDir, dir).also { it.mkdirs() }
}
