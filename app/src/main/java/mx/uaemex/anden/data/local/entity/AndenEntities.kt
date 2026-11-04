package mx.uaemex.anden.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stations")
data class StationEntity(
    @PrimaryKey val stationId: String,
    val name: String,
    val systemLine: String,
    val municipality: String,
    val latitude: Double,
    val longitude: Double,
    val scheduleText: String,
    val elevatorStatus: String,
    val rampStatus: String,
    val serviceStatus: String,
    val feederConnections: String,
    val isFavorite: Boolean = false
)

@Entity(tableName = "pending_reports")
data class PendingReportEntity(
    @PrimaryKey(autoGenerate = true) val reportId: Long = 0,
    val firestoreDocId: String = "",
    val stationId: String,
    val stationName: String,
    val incidentType: String,
    val description: String,
    val mediaUri: String,
    val isVideo: Boolean = false,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float = 5.0f,
    val distanceToStationMeters: Int = 85,
    val authorHandle: String = "@UsuarioAnonimo",
    val timestampMillis: Long = System.currentTimeMillis(),
    val syncStatus: String = "SINCRONIZADO_FIRESTORE",
    val moderationStatus: String = "APROBADO"
)

@Entity(tableName = "community_stops")
data class CommunityStopEntity(
    @PrimaryKey(autoGenerate = true) val stopId: Long = 0,
    val title: String,
    val corridorReference: String,
    val nearestStationName: String,
    val authorHandle: String,
    val timeAgoText: String,
    val latitude: Double,
    val longitude: Double,
    val moderationStatus: String = "PENDIENTE"
)

/**
 * Activo de la red (unidad, andén, tramo o estación) que el administrador da de alta.
 * isActive = true -> En operación; false -> Fuera de servicio / mantenimiento.
 * photoUri: "res:<drawable>" para las fotos incluidas en la app, "file://..." para
 * fotos elegidas en el teléfono o "https://..." si viene de Cloud Storage.
 */
@Entity(tableName = "network_assets")
data class NetworkAssetEntity(
    @PrimaryKey val assetId: String,
    val name: String,
    val category: String,
    val stationName: String,
    val description: String,
    val isActive: Boolean,
    val photoUri: String,
    val photoUrl: String = "",
    val updatedBy: String = "Equipo Andén",
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val syncedToCloud: Boolean = false
)
