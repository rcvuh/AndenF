package mx.uaemex.anden.domain

import mx.uaemex.anden.data.local.database.AssetSeedData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** T4.10: catalogo de activos (5 activos y 5 inactivos con fotos distintas) y administradores. */
class T410CatalogTest {

    @Test
    fun catalogoTieneCincoActivosYCincoInactivos() {
        val assets = AssetSeedData.assets
        assertEquals(5, assets.count { it.isActive })
        assertEquals(5, assets.count { !it.isActive })
    }

    @Test
    fun cadaActivoTieneFotografiaDistinta() {
        val photos = AssetSeedData.assets.map { it.photoUri }
        assertEquals(photos.size, photos.toSet().size)
        assertTrue(photos.all { it.startsWith("res:") })
    }

    @Test
    fun identificadoresUnicosYDatosCompletos() {
        val assets = AssetSeedData.assets
        assertEquals(assets.size, assets.map { it.assetId }.toSet().size)
        assertTrue(assets.all { it.name.isNotBlank() && it.description.length >= 30 && it.stationName.isNotBlank() })
    }

    @Test
    fun equipoDesarrolladorSonTresAdministradores() {
        assertEquals(3, TeamMembers.members.size)
        TeamMembers.members.forEach { member ->
            assertTrue(TeamMembers.isAdmin(member.email))
            assertTrue(TeamMembers.isAdmin(member.email.uppercase()))
            assertEquals(TeamMembers.ADMIN_ROLE, TeamMembers.roleFor(member.email))
        }
    }

    @Test
    fun correoAjenoNoEsAdministrador() {
        assertFalse(TeamMembers.isAdmin("pasajero@gmail.com"))
        assertEquals(TeamMembers.CITIZEN_ROLE, TeamMembers.roleFor("pasajero@gmail.com"))
        assertTrue(TeamMembers.isAdmin("supervisor@movilidad.edomex.gob.mx"))
    }
}
