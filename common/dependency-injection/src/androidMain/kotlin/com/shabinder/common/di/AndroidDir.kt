/*
 *  * Copyright (c)  2021  Shabinder Singh
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  *  You should have received a copy of the GNU General Public License
 *  *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.shabinder.common.di

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.os.Environment
import androidx.compose.ui.graphics.asImageBitmap
import co.touchlab.kermit.Kermit
import com.mpatric.mp3agic.Mp3File
import com.shabinder.common.database.appContext
import com.shabinder.common.models.TrackDetails
import com.shabinder.database.Database
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

actual class Dir actual constructor(
    private val logger: Kermit,
    private val database: Database?,
) {
    companion object {
        const val SharedPreferencesKey = "configurations"
        const val DirKey = "downloadDir"
    }

    private val context: Context
        get() = appContext

    private val sharedPreferences:SharedPreferences by lazy {
        context.getSharedPreferences(SharedPreferencesKey,MODE_PRIVATE)
    }

    fun setDownloadDirectory(newBasePath:String){
        sharedPreferences.edit().putString(DirKey,newBasePath).apply()
    }

    @Suppress("DEPRECATION")
    private val defaultBaseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).toString()

    actual fun fileSeparator(): String = File.separator

    actual fun imageCacheDir(): String = context.cacheDir.absolutePath + File.separator

    // fun call in order to always access Updated Value
    actual fun defaultDir(): String = sharedPreferences.getString(DirKey,defaultBaseDir)!! + File.separator +
            "SpotiFlyer" + File.separator

    actual fun isPresent(path: String): Boolean = File(path).exists()

    actual fun createDirectory(dirPath: String) {
        val yourAppDir = File(dirPath)

        if (!yourAppDir.exists() && !yourAppDir.isDirectory) { // create empty directory
            if (yourAppDir.mkdirs()) { logger.i { "$dirPath created" } } else {
                logger.e { "Unable to create Dir: $dirPath!" }
            }
        } else {
            logger.i { "$dirPath already exists" }
        }
    }

    actual suspend fun clearCache() {
        File(imageCacheDir()).deleteRecursively()
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    actual suspend fun saveFileWithMetadata(
        mp3ByteArray: ByteArray,
        trackDetails: TrackDetails
    ) {
        withContext(Dispatchers.IO){
            val songFile = File(trackDetails.outputFilePath)
            /*
            * Check , if Fetch was Used, File is saved Already, else write byteArray we Received
            * */
            // if(!m4aFile.exists()) m4aFile.writeBytes(mp3ByteArray)

            when (trackDetails.outputFilePath.substringAfterLast('.')) {
                ".mp3" -> {
                    Mp3File(File(songFile.absolutePath))
                        .removeAllTags()
                        .setId3v1Tags(trackDetails)
                        .setId3v2TagsAndSaveFile(trackDetails)
                    addToLibrary(songFile.absolutePath)
                }
                ".m4a" -> {
                    /*FFmpeg.executeAsync(
                        "-i ${m4aFile.absolutePath} -y -b:a 160k -acodec libmp3lame -vn ${m4aFile.absolutePath.substringBeforeLast('.') + ".mp3"}"
                    ){ _, returnCode ->
                        when (returnCode) {
                            Config.RETURN_CODE_SUCCESS  -> {
                                //FFMPEG task Completed
                                logger.d{ "Async command execution completed successfully." }
                                scope.launch {
                                    Mp3File(File(m4aFile.absolutePath.substringBeforeLast('.') + ".mp3"))
                                        .removeAllTags()
                                        .setId3v1Tags(trackDetails)
                                        .setId3v2TagsAndSaveFile(trackDetails)
                                    addToLibrary(m4aFile.absolutePath.substringBeforeLast('.') + ".mp3")
                                }
                            }
                            Config.RETURN_CODE_CANCEL -> {
                                logger.d{"Async command execution cancelled by user."}
                            }
                            else -> {
                                logger.d { "Async command execution failed with rc=$returnCode" }
                            }
                        }
                    }*/
                }
                else -> {
                    try {
                        Mp3File(File(songFile.absolutePath))
                            .removeAllTags()
                            .setId3v1Tags(trackDetails)
                            .setId3v2TagsAndSaveFile(trackDetails)
                        addToLibrary(songFile.absolutePath)
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
        }
    }

    actual fun addToLibrary(path: String) {
        logger.d { "Scanning File" }
        MediaScannerConnection.scanFile(
            appContext,
            listOf(path).toTypedArray(), null, null
        )
    }

    actual suspend fun loadImage(url: String): Picture = withContext(Dispatchers.IO){
        val cachePath = imageCacheDir() + getNameURL(url)
        Picture(image = (loadCachedImage(cachePath) ?: freshImage(url))?.asImageBitmap())
    }

    private fun loadCachedImage(cachePath: String): Bitmap? {
        return try {
            BitmapFactory.decodeFile(cachePath)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    actual suspend fun cacheImage(image: Any, path: String) {
        withContext(Dispatchers.IO){
            try {
                FileOutputStream(path).use { out ->
                    (image as? Bitmap)?.compress(Bitmap.CompressFormat.JPEG, 100, out)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun freshImage(url: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val source = URL(url)
            val connection: HttpURLConnection = source.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.connect()

            val input: InputStream = connection.inputStream
            val result: Bitmap? = BitmapFactory.decodeStream(input)

            if (result != null) {
                GlobalScope.launch(Dispatchers.IO) {
                    cacheImage(result, imageCacheDir() + getNameURL(url))
                }
                result
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    actual val db: Database? = database
}
