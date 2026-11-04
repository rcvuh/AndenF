package mx.uaemex.anden.domain

import mx.uaemex.anden.data.local.entity.StationEntity
import kotlin.math.roundToInt

object SecurityPrivacyRules {

    const val MAX_DESCRIPTION_CHARS = 280

    private val HTML_TAG_REGEX = Regex("<[^>]*>")
    private val SQL_INJECTION_TOKENS = listOf("DROP TABLE", "DELETE FROM", "INSERT INTO", "--", ";--", "/*")

    fun sanitizeReportDescription(raw: String): String {
        var cleaned = raw.replace(HTML_TAG_REGEX, "")
        SQL_INJECTION_TOKENS.forEach { token ->
            cleaned = cleaned.replace(token, "", ignoreCase = true)
        }
        cleaned = cleaned
            .filter { it >= ' ' || it == '\n' }
            .replace(Regex("\\s+"), " ")
            .trim()
        return if (cleaned.length > MAX_DESCRIPTION_CHARS) {
            cleaned.take(MAX_DESCRIPTION_CHARS)
        } else {
            cleaned
        }
    }

    fun pseudonymizeAuthorHandle(
        email: String,
        isGuest: Boolean,
        anonymousPrivacyMode: Boolean
    ): String {
        if (isGuest) return "@InvitadoVerificado"
        val localPart = email.substringBefore('@').trim().ifBlank { "usuario" }
        if (!anonymousPrivacyMode) {
            return "@$localPart"
        }
        val first = localPart.first()
        val last = localPart.last()
        return "@${first}***${last}_anon"
    }

    fun maskEmailForPrivacy(email: String): String {
        val trimmed = email.trim()
        if (!trimmed.contains('@')) return "usuario@anonimo.mx"
        val local = trimmed.substringBefore('@')
        val domain = trimmed.substringAfter('@')
        if (local.length <= 2) return "${local.firstOrNull() ?: 'u'}***@$domain"
        return "${local.take(2)}***${local.last()}@$domain"
    }

    fun obfuscateCoordinateForPrivacy(coordinate: Double): Double =
        (coordinate * 10_000.0).roundToInt() / 10_000.0

    fun isInstitutionalAdminEmail(email: String): Boolean {
        val lower = email.trim().lowercase()
        return lower.endsWith("@movilidad.edomex.gob.mx") || lower.endsWith("@uaemex.mx")
    }

    fun buildStationAccessibilityLabel(station: StationEntity): String {
        val favText = if (station.isFavorite) "Marcada como estación favorita." else ""
        return "Estación ${station.name}, municipio de ${station.municipality}. " +
            "Estado del servicio: ${station.serviceStatus}. " +
            "Accesibilidad motriz: Elevadores ${station.elevatorStatus}, Rampas ${station.rampStatus}. " +
            "Rutas alimentadoras: ${station.feederConnections}. $favText".trim()
    }

    fun buildGeofenceAccessibilityLabel(distanceMeters: Int, isWithinGeofence: Boolean): String =
        if (isWithinGeofence) {
            "Geocerca validada a $distanceMeters metros de la estación, dentro del límite de 300 metros. Envío de reporte habilitado."
        } else {
            "Fuera de geocerca a $distanceMeters metros de la estación. Debe acercarse a 300 metros o menos para enviar un reporte."
        }
}
