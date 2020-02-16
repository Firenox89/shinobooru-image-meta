package com.github.firenox89.shinobooru.image.meta

import com.google.gson.Gson
import org.apache.commons.imaging.ImageFormats
import org.apache.commons.imaging.Imaging
import org.apache.commons.imaging.common.GenericImageMetadata
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter
import org.apache.commons.imaging.formats.png.PngConstants
import org.apache.commons.imaging.formats.png.PngText
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.IllegalArgumentException

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
        when (Imaging.guessFormat(source)) {
            ImageFormats.PNG -> {
                val metadata = Imaging.getMetadata(source)?.items?.map {
                    it as GenericImageMetadata.GenericImageMetadataItem
                    PngText.Text(it.keyword, it.text)
                } ?: emptyList()

                val params = mapOf(PngConstants.PARAM_KEY_PNG_TEXT_CHUNKS to metadata.toMutableSet().apply {
                    add(PngText.Text(PNG_BOARD_KEY, post.board))
                    add(PngText.Text(PNG_ID_KEY, post.id))
                    add(PngText.Text(PNG_AUTHOR_KEY, post.author))
                    add(PngText.Text(PNG_SOURCE_KEY, post.source))
                    add(PngText.Text(PNG_RATING_KEY, post.rating))
                    add(PngText.Text(PNG_TAGS_KEY, post.tags))
                }.toList())

                BufferedOutputStream(FileOutputStream(destination)).use { os ->
                    Imaging.writeImage(
                        Imaging.getBufferedImage(source), os, ImageFormats.PNG,
                        params.toMutableMap() as Map<String, Any>?
                    )

                }
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

                BufferedOutputStream(FileOutputStream(destination)).use { os ->
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
    }

    fun readPostFromImage(image: File): Post {
        return when (Imaging.guessFormat(image)) {
            ImageFormats.PNG -> {
                val metadata = Imaging.getMetadata(image).items.map {
                    it as GenericImageMetadata.GenericImageMetadataItem
                    PngText.Text(it.keyword, it.text)
                }
                val board = metadata.find { it.keyword == PNG_BOARD_KEY }?.text
                val id = metadata.find { it.keyword == PNG_ID_KEY }?.text
                val author = metadata.find { it.keyword == PNG_AUTHOR_KEY }?.text ?: ""
                val source = metadata.find { it.keyword == PNG_SOURCE_KEY }?.text ?: ""
                val rating = metadata.find { it.keyword == PNG_RATING_KEY }?.text
                val tags = metadata.find { it.keyword == PNG_TAGS_KEY }?.text ?: ""
                if (board == null || id == null || rating == null) {
                    throw IllegalArgumentException("Image does not contain post information")
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
