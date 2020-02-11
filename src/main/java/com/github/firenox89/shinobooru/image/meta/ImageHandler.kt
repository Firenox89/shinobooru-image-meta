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

data class Post (
    val board: String,
    val id: String,
    val author: String,
    val source: String,
    val rating: String,
    val tags: String
)

object ImageHandler {
    fun savePostToImage(image: File, post: Post) {
        val format = Imaging.guessFormat(image)
        BufferedOutputStream(FileOutputStream(image)).use { os ->
            when (format) {
                ImageFormats.PNG -> {
                    val metadata = Imaging.getMetadata(image).items.map {
                        it as GenericImageMetadata.GenericImageMetadataItem
                        PngText.Text(it.keyword, it.text)
                    }

                    println(metadata)

                    val params = mapOf(PngConstants.PARAM_KEY_PNG_TEXT_CHUNKS to metadata.toMutableSet().apply {
                        add(PngText.Text("shinobooru-board", post.board))
                        add(PngText.Text("shinobooru-id", post.id))
                        add(PngText.Text("shinobooru-author", post.author))
                        add(PngText.Text("shinobooru-source", post.source))
                        add(PngText.Text("shinobooru-rating", post.rating))
                        add(PngText.Text("shinobooru-tags", post.tags))
                    }.toList())

                    Imaging.writeImage(
                        Imaging.getBufferedImage(image), os, ImageFormats.PNG,
                        params.toMutableMap() as Map<String, Any>?
                    )

                    val dstMetadata = Imaging.getMetadata(image);
                    println(dstMetadata)
                }
                ImageFormats.JPEG -> {
                    val metadata = Imaging.getMetadata(image);
                    println(metadata)

                    val outputSet =
                        if (metadata != null && metadata is JpegImageMetadata) metadata.exif.outputSet else TiffOutputSet()

                    val exifDirectory = outputSet.orCreateExifDirectory
                    exifDirectory.removeField(ExifTagConstants.EXIF_TAG_USER_COMMENT)
                    exifDirectory.add(
                        ExifTagConstants.EXIF_TAG_USER_COMMENT,
                        Gson().toJson(post)
                    )

                    ExifRewriter().updateExifMetadataLossless(
                        image, os,
                        outputSet
                    )

                    val dstMetadata = Imaging.getMetadata(image);
                    println(dstMetadata)
                }
            }
        }
    }
}