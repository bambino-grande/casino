package ru.pogosian.kazik.rewards.business.providers

interface PrizeImageProvider {
    fun pick(): PrizeImage
}

data class PrizeImage(val fileName: String)

interface PrizeImageStorage {
    fun read(fileName: String): StoredImage
}

class StoredImage(
    val bytes: ByteArray,
    val contentType: String,
)
