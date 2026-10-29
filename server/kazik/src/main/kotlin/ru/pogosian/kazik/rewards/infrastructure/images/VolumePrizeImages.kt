package ru.pogosian.kazik.rewards.infrastructure.images

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.pogosian.kazik.rewards.business.exceptions.NoPrizeImagesException
import ru.pogosian.kazik.rewards.business.exceptions.PrizeImageMissingException
import ru.pogosian.kazik.rewards.business.providers.PrizeImage
import ru.pogosian.kazik.rewards.business.providers.PrizeImageProvider
import ru.pogosian.kazik.rewards.business.providers.PrizeImageStorage
import ru.pogosian.kazik.rewards.business.providers.StoredImage
import ru.pogosian.kazik.rewards.infrastructure.config.RewardsProperties
import java.nio.file.Files
import java.nio.file.Path
import java.security.SecureRandom
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

private val PRIZE_CONTENT_TYPES = mapOf(
    "png" to "image/png",
    "jpg" to "image/jpeg",
    "jpeg" to "image/jpeg",
    "webp" to "image/webp",
    "gif" to "image/gif",
    "svg" to "image/svg+xml",
)

@Component
class VolumePrizeImages(
    properties: RewardsProperties,
) : PrizeImageProvider, PrizeImageStorage {
    private val log = LoggerFactory.getLogger(javaClass)
    private val directory: Path = Path.of(properties.imagesDir).toAbsolutePath().normalize()
    private val random = SecureRandom()

    init {
        val files = listImages()
        if (files.isEmpty()) {
            log.error("[REWARDS] no prize images in {}: a jackpot would be paid without a picture", directory)
        } else {
            log.info("[REWARDS] {} prize images in {}", files.size, directory)
        }
    }

    override fun pick(): PrizeImage {
        val files = listImages()
        if (files.isEmpty()) throw NoPrizeImagesException(directory.toString())
        return PrizeImage(files[random.nextInt(files.size)])
    }

    override fun read(fileName: String): StoredImage {
        val path = directory.resolve(fileName).normalize()
        val contentType = PRIZE_CONTENT_TYPES[path.extension.lowercase()]
        if (path.parent != directory || !path.isRegularFile() || contentType == null) throw PrizeImageMissingException(fileName)
        return StoredImage(Files.readAllBytes(path), contentType)
    }

    private fun listImages(): List<String> {
        if (!Files.isDirectory(directory)) return emptyList()
        return Files.list(directory).use { stream ->
            stream.filter { it.isRegularFile() && it.extension.lowercase() in PRIZE_CONTENT_TYPES }.map { it.name }.sorted().toList()
        }
    }
}
