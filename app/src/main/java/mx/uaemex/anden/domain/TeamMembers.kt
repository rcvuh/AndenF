package mx.uaemex.anden.domain

/** Integrante del equipo desarrollador de Andén. */
data class TeamMember(
    val fullName: String,
    val studentId: String,
    val email: String,
    val roleInProject: String
)

/**
 * Equipo desarrollador = administradores de la aplicación (T4.10).
 * Para dar de alta a otro administrador basta con agregarlo a esta lista.
 */
object TeamMembers {

    const val ADMIN_ROLE = "ADMINISTRADOR"
    const val CITIZEN_ROLE = "PASAJERO"

    val members: List<TeamMember> = listOf(
        TeamMember(
            fullName = "Roberto Carlos González Caballero",
            studentId = "2370574",
            email = "rcarlosgc2@outlook.com",
            roleInProject = "Desarrollo Android · Firebase"
        ),
        TeamMember(
            fullName = "Héctor Tomás Rodríguez Flores",
            studentId = "2370623",
            email = "hrodriguezf002@alumno.uaemex.mx",
            roleInProject = "Desarrollo Android · Interfaz"
        ),
        TeamMember(
            fullName = "Uzziel Valdez Ordoñez",
            studentId = "1912158",
            email = "uvaldezo001@alumno.uaemex.mx",
            roleInProject = "Pruebas · Documentación"
        )
    )

    fun findByEmail(email: String): TeamMember? {
        val clean = email.trim().lowercase()
        return members.firstOrNull { it.email.lowercase() == clean }
    }

    /** Administrador = integrante del equipo o correo institucional de movilidad. */
    fun isAdmin(email: String): Boolean =
        findByEmail(email) != null || SecurityPrivacyRules.isInstitutionalAdminEmail(email)

    fun roleFor(email: String): String = if (isAdmin(email)) ADMIN_ROLE else CITIZEN_ROLE
}
