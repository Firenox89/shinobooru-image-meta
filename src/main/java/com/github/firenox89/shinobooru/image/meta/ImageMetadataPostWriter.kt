package com.github.firenox89.shinobooru.image.meta

import ar.com.hjg.pngj.PngReader
import ar.com.hjg.pngj.PngWriter
import ar.com.hjg.pngj.chunks.ChunkCopyBehaviour
import ar.com.hjg.pngj.chunks.PngChunkTEXT
import com.google.gson.Gson
import org.apache.commons.imaging.ImageFormats
import org.apache.commons.imaging.Imaging
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.CopyOption
import java.nio.file.Files
import java.nio.file.StandardCopyOption

data class Post(
    val board: String,
    val id: String,
    val author: String,
    val source: String,
    val rating: String,
    val tags: String
)

private const val PNG_BOARD_KEY = "shinobooru-board"
private const val PNG_ID_KEY = "shinobooru-id"
private const val PNG_AUTHOR_KEY = "shinobooru-author"
private const val PNG_SOURCE_KEY = "shinobooru-source"
private const val PNG_RATING_KEY = "shinobooru-rating"
private const val PNG_TAGS_KEY = "shinobooru-tags"

object ImageMetadataPostWriter {
    fun writePostToImage(source: File, destination: File, post: Post) {
        var destFile = destination
        if (source == destination) {
            destFile = createTempFile()
        }
        when (Imaging.guessFormat(source)) {
            ImageFormats.PNG -> {

                val pngr = PngReader(source)
                println(pngr.toString())
                val pngw = PngWriter(destFile, pngr.imgInfo, true)

                pngw.copyChunksFrom(pngr.chunksList, ChunkCopyBehaviour.COPY_PALETTE)

                pngw.metadata.setText(PNG_BOARD_KEY, post.board)
                pngw.metadata.setText(PNG_ID_KEY, post.id)
                pngw.metadata.setText(PNG_AUTHOR_KEY, post.author)
                pngw.metadata.setText(PNG_SOURCE_KEY, post.source)
                pngw.metadata.setText(PNG_RATING_KEY, post.rating)
                pngw.metadata.setText(PNG_TAGS_KEY, post.tags)

                pngw.writeRows(pngr.readRows());
                pngr.end(); // it's recommended to end the reader first, in case there are trailing chunks to read
                pngw.end();
            }
            ImageFormats.JPEG -> {
                val metadata = Imaging.getMetadata(source);

                val outputSet =
                    if (metadata != null && metadata is JpegImageMetadata) metadata.exif.outputSet else TiffOutputSet()

                val exifDirectory = outputSet.orCreateExifDirectory
                exifDirectory.removeField(ExifTagConstants.EXIF_TAG_USER_COMMENT)
                exifDirectory.add(
                    ExifTagConstants.EXIF_TAG_USER_COMMENT,
                    Gson().toJson(post)
                )

                BufferedOutputStream(FileOutputStream(destFile)).use { os ->
                    ExifRewriter().updateExifMetadataLossless(
                        source, os,
                        outputSet
                    )
                }
            }
            else -> {
                throw IllegalArgumentException("Unsupported image format.")
            }
        }
        if (source == destination) {
            Files.move(destFile.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    fun readPostFromImage(image: File): Post {
        return when (Imaging.guessFormat(image)) {
            ImageFormats.PNG -> {
                val pngr = PngReader(image)

                //PNGJ writes metadata to the end of the file so we need to read the whole thing
                pngr.readSkippingAllRows()

                val board = pngr.metadata.getTxtForKey(PNG_BOARD_KEY)
                val id = pngr.metadata.getTxtForKey(PNG_ID_KEY)
                val author = pngr.metadata.getTxtForKey(PNG_AUTHOR_KEY)
                val source = pngr.metadata.getTxtForKey(PNG_SOURCE_KEY)
                val rating = pngr.metadata.getTxtForKey(PNG_RATING_KEY)
                val tags = pngr.metadata.getTxtForKey(PNG_TAGS_KEY)

                if (board.isNullOrBlank() || id.isNullOrBlank()) {
                    throw IllegalArgumentException("Image does not contain post information $image")
                }

                Post(board, id, author, source, rating, tags)
            }
            ImageFormats.JPEG -> {
                val metadata = Imaging.getMetadata(image) as JpegImageMetadata
                val field = metadata.exif.findField(ExifTagConstants.EXIF_TAG_USER_COMMENT)
                field.stringValue
                Gson().fromJson<Post>(field.stringValue, Post::class.java)
            }
            else -> {
                throw IllegalArgumentException("Unsupported image format.")
            }
        }
    }
}
