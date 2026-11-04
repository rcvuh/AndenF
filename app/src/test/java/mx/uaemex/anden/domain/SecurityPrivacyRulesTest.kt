package mx.uaemex.anden.domain

import mx.uaemex.anden.data.local.entity.StationEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityPrivacyRulesTest {

    @Test
    fun sanitizacionEliminaEtiquetasHtmlYScripts() {
        val input = "<script>alert('xss')</script>Elevador detenido en andén norte <b>urgente</b>"
        val clean = SecurityPrivacyRules.sanitizeReportDescription(input)
        assertEquals("alert('xss')Elevador detenido en andén norte urgente", clean)
        assertFalse(clean.contains("<script>"))
    }

    @Test
    fun sanitizacionEliminaTokensDeInyeccionSql() {
        val input = "Retraso de 15 min; DROP TABLE pending_reports; --"
        val clean = SecurityPrivacyRules.sanitizeReportDescription(input)
        assertFalse(clean.contains("DROP TABLE", ignoreCase = true))
        assertFalse(clean.contains("--"))
    }

    @Test
    fun sanitizacionTruncaADoscientosOchentaCaracteres() {
        val longInput = "A".repeat(350)
        val clean = SecurityPrivacyRules.sanitizeReportDescription(longInput)
        assertEquals(280, clean.length)
    }

    @Test
    fun seudonimizacionProtegeCorreoEnModoPrivado() {
        val handle = SecurityPrivacyRules.pseudonymizeAuthorHandle(
            email = "hector.rodriguez@alumno.uaemex.mx",
            isGuest = false,
            anonymousPrivacyMode = true
        )
        assertEquals("@h***z_anon", handle)
    }

    @Test
    fun seudonimizacionMuestraHandleEnModoPublicoEInvitado() {
        val publicHandle = SecurityPrivacyRules.pseudonymizeAuthorHandle(
            email = "hector.rodriguez@alumno.uaemex.mx",
            isGuest = false,
            anonymousPrivacyMode = false
        )
        val guestHandle = SecurityPrivacyRules.pseudonymizeAuthorHandle(
            email = "hector.rodriguez@alumno.uaemex.mx",
            isGuest = true,
            anonymousPrivacyMode = true
        )
        assertEquals("@hector.rodriguez", publicHandle)
        assertEquals("@InvitadoVerificado", guestHandle)
    }

    @Test
    fun enmascaramientoDeCorreoParaInterfazSegura() {
        val masked = SecurityPrivacyRules.maskEmailForPrivacy("hector.rodriguez@alumno.uaemex.mx")
        assertEquals("he***z@alumno.uaemex.mx", masked)
    }

    @Test
    fun ofuscacionGeoespacialRedondeaACuatroDecimales() {
        val lat = SecurityPrivacyRules.obfuscateCoordinateForPrivacy(19.28426789)
        val lng = SecurityPrivacyRules.obfuscateCoordinateForPrivacy(-99.65564321)
        assertEquals(19.2843, lat, 0.00001)
        assertEquals(-99.6556, lng, 0.00001)
    }

    @Test
    fun validacionRbacDeCorreoInstitucionalAdministrador() {
        assertTrue(SecurityPrivacyRules.isInstitutionalAdminEmail("supervisor@movilidad.edomex.gob.mx"))
        assertTrue(SecurityPrivacyRules.isInstitutionalAdminEmail("coordinador@uaemex.mx"))
        assertFalse(SecurityPrivacyRules.isInstitutionalAdminEmail("intruso@gmail.com"))
    }

    @Test
    fun etiquetaSemanticaTalkBackDescribeAccesibilidadDeEstacion() {
        val st = StationEntity(
            stationId = "INS-03",
            name = "Metepec",
            systemLine = "Tren El Insurgente",
            municipality = "Metepec",
            latitude = 19.2772,
            longitude = -99.5754,
            scheduleText = "05:00 a 23:00 hrs",
            elevatorStatus = "Operando (2/2)",
            rampStatus = "Accesibles",
            serviceStatus = "Servicio Regular",
            feederConnections = "Ruta Tenango",
            isFavorite = true
        )
        val label = SecurityPrivacyRules.buildStationAccessibilityLabel(st)
        assertTrue(label.contains("Estación Metepec"))
        assertTrue(label.contains("Elevadores Operando (2/2)"))
        assertTrue(label.contains("favorita"))
    }

    @Test
    fun etiquetaSemanticaTalkBackDeGeocercaInformaEstado() {
        val inside = SecurityPrivacyRules.buildGeofenceAccessibilityLabel(85, true)
        val outside = SecurityPrivacyRules.buildGeofenceAccessibilityLabel(450, false)
        assertTrue(inside.contains("Geocerca validada"))
        assertTrue(outside.contains("Fuera de geocerca"))
    }
}
