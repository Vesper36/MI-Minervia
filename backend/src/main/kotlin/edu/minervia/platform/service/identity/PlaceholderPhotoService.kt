package edu.minervia.platform.service.identity

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Service
class PlaceholderPhotoService(
    @Value("\${app.photo.placeholder-base-url:https://ui-avatars.com/api/}")
    private val baseUrl: String,
    @Value("\${app.photo.placeholder-size:256}")
    private val size: Int,
    @Value("\${app.photo.placeholder-background:0D8ABC}")
    private val background: String,
    @Value("\${app.photo.placeholder-color:fff}")
    private val color: String
) {
    fun generatePhotoUrl(firstName: String, lastName: String): String {
        val name = URLEncoder.encode("$firstName $lastName", StandardCharsets.UTF_8)
        return "${baseUrl}?name=$name&size=$size&background=$background&color=$color&bold=true&format=png"
    }
}
