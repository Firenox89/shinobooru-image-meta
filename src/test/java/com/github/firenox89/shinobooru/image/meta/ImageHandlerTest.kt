package com.github.firenox89.shinobooru.image.meta

import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ImageHandlerTest {
    private val testPost = Post(
        "Test board",
        "Test ID",
        "Test autohr",
        "Test source",
        "Test rating",
        "Test tags"
    )
    private val testJPEG = File("src/test/resources/yande.re 388528 famy_siraso seifuku.jpg")
    private val testPNG = File("src/test/resources/yande.re 385402 cleavage nanaroba_hana thighhighs transparent_png.png")

    private val post388525 = Post(
        "yande.re",
        "388528",
        "",
        "",
        "s",
        "famy_siraso seifuku"
    )
    private val post388525File = File("src/test/resources/yande.re_388528_shinoMetadata.jpg")

    private val post385402 = Post(
        "yande.re",
        "385402",
        "",
        "",
        "s",
        "cleavage nanaroba_hana thighhighs transparent_png.png"
    )
    private val post385402File = File("src/test/resources/yande.re_385402_shinoMetadata.png")

    @Rule @JvmField
    val tmpDir = TemporaryFolder();

    @Test
    fun savePostToJPEG() {
        val tmpFile = tmpDir.newFile()
        ImageMetadataPostWriter.writePostToImage(
            testJPEG,
            tmpFile,
            testPost
        )

        val post =
            ImageMetadataPostWriter.readPostFromImage(
                tmpFile
            )
        Assert.assertEquals(post, testPost)
    }

    @Test
    fun savePostToPNG() {
        val tmpFile = tmpDir.newFile()
        ImageMetadataPostWriter.writePostToImage(
            testPNG,
            tmpFile,
            testPost
        )

        val post =
            ImageMetadataPostWriter.readPostFromImage(
                tmpFile
            )
        Assert.assertEquals(post, testPost)
    }

    @Test
    fun loadPostFromJPEG() {
        val post =
            ImageMetadataPostWriter.readPostFromImage(
                post388525File
            )

        Assert.assertEquals(post, post388525)
    }

    @Test
    fun loadPostFromPNG() {
        val post =
            ImageMetadataPostWriter.readPostFromImage(
                post385402File
            )

        Assert.assertEquals(post, post385402)
    }
}