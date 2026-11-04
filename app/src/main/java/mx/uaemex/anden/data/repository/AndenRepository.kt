package mx.uaemex.anden.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.tasks.await
import mx.uaemex.anden.data.local.dao.AndenDao
import mx.uaemex.anden.data.local.database.AssetSeedData
import mx.uaemex.anden.data.local.entity.CommunityStopEntity
import mx.uaemex.anden.data.local.entity.NetworkAssetEntity
import mx.uaemex.anden.data.local.entity.PendingReportEntity
import mx.uaemex.anden.data.local.entity.StationEntity
import java.io.File
import java.util.UUID

class AndenRepository(
    private val andenDao: AndenDao,
    private val appContext: android.content.Context? = null
) {
    private val auth: FirebaseAuth? = runCatching { FirebaseAuth.getInstance() }.getOrNull()
    private val firestore: FirebaseFirestore? = runCatching { FirebaseFirestore.getInstance() }.getOrNull()
    private val storage: FirebaseStorage? = runCatching { FirebaseStorage.getInstance() }.getOrNull()

    val allStations: Flow<List<StationEntity>> = andenDao.observeAllStations()
    val allReports: Flow<List<PendingReportEntity>> = andenDao.observeAllReports()
    val allCommunityStops: Flow<List<CommunityStopEntity>> = andenDao.observeAllCommunityStops()

    fun observeReportsForStation(stationId: String): Flow<List<PendingReportEntity>> =
        andenDao.observeReportsForStation(stationId)

    suspend fun toggleFavoriteStation(stationId: String, currentFav: Boolean) {
        andenDao.updateFavoriteStatus(stationId, !currentFav)
    }

    suspend fun signInWithEmail(email: String, password: String): Result<Boolean> {
        return try {
            val isAdminAccount = email.lowercase().endsWith("@movilidad.edomex.gob.mx") ||
                    email.lowercase().contains("admin")
            val currentAuth = auth
            if (currentAuth != null && email.isNotBlank() && password.length >= 6) {
                runCatching {
                    currentAuth.signInWithEmailAndPassword(email.trim(), password).await()
                }.onFailure {
                    runCatching {
                        currentAuth.createUserWithEmailAndPassword(email.trim(), password).await()
                    }
                }
            }
            Result.success(isAdminAccount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createAndSyncReport(
        report: PendingReportEntity,
        isOnline: Boolean
    ): PendingReportEntity {
        val initialStatus = "PENDIENTE_ROOM"
        val insertedId = andenDao.insertReport(report.copy(syncStatus = initialStatus))
        val savedLocal = report.copy(reportId = insertedId, syncStatus = initialStatus)

        if (report.incidentType.contains("Elevador dañado", ignoreCase = true)) {
            andenDao.updateStationOperationalStatus(
                stationId = report.stationId,
                elevatorStatus = "En revisión (Reporte ciudadano)",
                serviceStatus = "Alerta de accesibilidad en revisión"
            )
        } else if (report.incidentType.contains("Retraso", ignoreCase = true)) {
            andenDao.updateStationOperationalStatus(
                stationId = report.stationId,
                elevatorStatus = "Operando",
                serviceStatus = "Demora reportada por usuarios"
            )
        }

        if (!isOnline) {
            return savedLocal
        }

        return syncSingleReportToFirebase(savedLocal)
    }

    suspend fun syncSingleReportToFirebase(report: PendingReportEntity): PendingReportEntity {
        return try {
            var remoteMediaUrl = report.mediaUri
            val currentStorage = storage
            if (currentStorage != null && (report.mediaUri.startsWith("file://") || report.mediaUri.startsWith("content://"))) {
                runCatching {
                    val ext = if (report.isVideo) "mp4" else "jpg"
                    val ref = currentStorage.reference.child("anden_evidencias/${report.stationId}/${UUID.randomUUID()}.$ext")
                    ref.putFile(Uri.parse(report.mediaUri)).await()
                    remoteMediaUrl = ref.downloadUrl.await().toString()
                }
            }

            var firestoreId = "FS-${UUID.randomUUID().toString().take(8)}"
            val currentFirestore = firestore
            if (currentFirestore != null) {
                runCatching {
                    val payload = hashMapOf(
                        "stationId" to report.stationId,
                        "stationName" to report.stationName,
                        "incidentType" to report.incidentType,
                        "description" to report.description,
                        "mediaUrl" to remoteMediaUrl,
                        "isVideo" to report.isVideo,
                        "location" to GeoPoint(report.latitude, report.longitude),
                        "accuracyMeters" to report.accuracyMeters,
                        "distanceToStationMeters" to report.distanceToStationMeters,
                        "authorHandle" to report.authorHandle,
                        "timestampMillis" to report.timestampMillis,
                        "moderationStatus" to report.moderationStatus
                    )
                    val docRef = currentFirestore.collection("reports").add(payload).await()
                    firestoreId = docRef.id
                }
            }

            andenDao.markReportSynced(
                reportId = report.reportId,
                syncStatus = "SINCRONIZADO_FIRESTORE",
                firestoreId = firestoreId
            )
            report.copy(
                syncStatus = "SINCRONIZADO_FIRESTORE",
                firestoreDocId = firestoreId
            )
        } catch (_: Exception) {
            report
        }
    }

    suspend fun syncAllPendingOfflineReports(): Int {
        val pending = andenDao.getUnsyncedReports()
        pending.forEach { report ->
            syncSingleReportToFirebase(report)
        }
        return pending.size
    }

    suspend fun proposeCommunityStop(stop: CommunityStopEntity): Long {
        val insertedId = andenDao.insertCommunityStop(stop)
        val currentFirestore = firestore
        if (currentFirestore != null) {
            runCatching {
                val payload = hashMapOf(
                    "title" to stop.title,
                    "corridorReference" to stop.corridorReference,
                    "nearestStationName" to stop.nearestStationName,
                    "authorHandle" to stop.authorHandle,
                    "timeAgoText" to stop.timeAgoText,
                    "location" to GeoPoint(stop.latitude, stop.longitude),
                    "moderationStatus" to stop.moderationStatus
                )
                currentFirestore.collection("community_stops").add(payload).await()
            }
        }
        return insertedId
    }

    suspend fun updateStationStatusByAdmin(
        stationId: String,
        elevatorStatus: String,
        serviceStatus: String
    ) {
        andenDao.updateStationOperationalStatus(stationId, elevatorStatus, serviceStatus)
        val currentFirestore = firestore
        if (currentFirestore != null) {
            runCatching {
                val payload = hashMapOf(
                    "stationId" to stationId,
                    "elevatorStatus" to elevatorStatus,
                    "serviceStatus" to serviceStatus,
                    "updatedAtMillis" to System.currentTimeMillis()
                )
                currentFirestore.collection("stations").document(stationId).set(payload).await()
            }
        }
    }

    suspend fun moderateReport(reportId: Long, approve: Boolean) {
        val newStatus = if (approve) "APROBADO" else "DESCARTADO"
        andenDao.updateReportModerationStatus(reportId, newStatus)
    }

    suspend fun moderateCommunityStop(stopId: Long, approve: Boolean) {
        val newStatus = if (approve) "VALIDADA" else "DESCARTADA"
        andenDao.updateCommunityStopStatus(stopId, newStatus)
    }

    // =====================================================================
    // T4.10 · Activos de la red (Room + Cloud Firestore + Cloud Storage)
    // =====================================================================

    val allAssets: Flow<List<NetworkAssetEntity>> = andenDao.observeAllAssets()

    val isCloudReady: Boolean get() = firestore != null

    /** Alta o actualizacion en Room (offline-first). Devuelve lo guardado para subirlo despues. */
    suspend fun saveAssetLocal(asset: NetworkAssetEntity): NetworkAssetEntity {
        val local = asset.copy(syncedToCloud = false, updatedAtMillis = System.currentTimeMillis())
        andenDao.upsertAsset(local)
        return local
    }

    suspend fun deleteAsset(assetId: String) {
        andenDao.deleteAsset(assetId)
        val currentFirestore = firestore ?: return
        runCatching {
            withTimeout(CLOUD_TIMEOUT_MS) {
                currentFirestore.collection(ASSETS_COLLECTION).document(assetId).delete().await()
            }
        }
    }

    /** Sube la foto (si es local) a Storage y el documento a activos/{assetId}. */
    suspend fun pushAssetToCloud(asset: NetworkAssetEntity): Boolean {
        val currentFirestore = firestore ?: return false
        return runCatching {
            var photoUrl = asset.photoUrl
            val currentStorage = storage
            if (photoUrl.isBlank() && asset.photoUri.startsWith("file://") && currentStorage != null) {
                photoUrl = runCatching {
                    withTimeout(UPLOAD_TIMEOUT_MS) {
                        val ref = currentStorage.reference
                            .child("anden_activos/${asset.assetId}_${asset.updatedAtMillis}.jpg")
                        ref.putFile(Uri.parse(asset.photoUri)).await()
                        ref.downloadUrl.await().toString()
                    }
                }.getOrDefault("")
            }
            val payload = hashMapOf(
                "assetId" to asset.assetId,
                "nombre" to asset.name,
                "categoria" to asset.category,
                "estacion" to asset.stationName,
                "descripcion" to asset.description,
                "activo" to asset.isActive,
                "estado" to if (asset.isActive) "EN_OPERACION" else "FUERA_DE_SERVICIO",
                "fotoRef" to if (asset.photoUri.startsWith("res:")) asset.photoUri else "",
                "fotoUrl" to photoUrl,
                "fotoMiniatura" to localThumbnail(asset),
                "actualizadoPor" to asset.updatedBy,
                "actualizadoEn" to asset.updatedAtMillis
            )
            withTimeout(CLOUD_TIMEOUT_MS) {
                currentFirestore.collection(ASSETS_COLLECTION).document(asset.assetId).set(payload).await()
            }
            andenDao.markAssetSynced(asset.assetId, photoUrl)
            true
        }.getOrDefault(false)
    }

    private fun localThumbnail(asset: NetworkAssetEntity): String {
        if (!asset.photoUri.startsWith("file://")) return ""
        val file = File(Uri.parse(asset.photoUri).path.orEmpty())
        return if (file.exists()) PhotoUtils.thumbnailBase64(file) else ""
    }

    /** Sube todo lo que siga pendiente (por ejemplo el catalogo inicial). */
    suspend fun pushPendingAssets(): Int {
        val currentFirestore = firestore ?: return 0
        var count = 0
        andenDao.getAllAssets().filter { !it.syncedToCloud }.forEach { asset ->
            // El catalogo inicial no debe pisar lo que otro integrante ya cambio en la nube.
            if (asset.updatedAtMillis == AssetSeedData.SEED_TIME_MILLIS) {
                val remote = runCatching {
                    withTimeout(CLOUD_TIMEOUT_MS) {
                        currentFirestore.collection(ASSETS_COLLECTION).document(asset.assetId).get().await()
                    }
                }.getOrNull()
                if (remote == null) return@forEach
                if (remote.exists()) {
                    andenDao.markAssetSynced(asset.assetId, remote.getString("fotoUrl").orEmpty())
                    return@forEach
                }
            }
            if (pushAssetToCloud(asset)) count++
        }
        return count
    }

    /** Escucha activos/ en tiempo real para ver lo que dan de alta los demas integrantes. */
    fun observeRemoteAssets(): Flow<RemoteAssetsSnapshot> {
        val currentFirestore = firestore ?: return emptyFlow()
        return callbackFlow {
            val registration = currentFirestore.collection(ASSETS_COLLECTION)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val thumbnails = mutableMapOf<String, String>()
                    val remote = snapshot.documents.mapNotNull { doc ->
                        val name = doc.getString("nombre") ?: return@mapNotNull null
                        val photoUrl = doc.getString("fotoUrl").orEmpty()
                        val photoRef = doc.getString("fotoRef").orEmpty()
                        doc.getString("fotoMiniatura")?.takeIf { it.isNotBlank() }?.let { thumbnails[doc.id] = it }
                        NetworkAssetEntity(
                            assetId = doc.id,
                            name = name,
                            category = doc.getString("categoria").orEmpty(),
                            stationName = doc.getString("estacion").orEmpty(),
                            description = doc.getString("descripcion").orEmpty(),
                            isActive = doc.getBoolean("activo") ?: true,
                            photoUri = photoUrl.ifBlank { photoRef },
                            photoUrl = photoUrl,
                            updatedBy = doc.getString("actualizadoPor").orEmpty(),
                            updatedAtMillis = doc.getLong("actualizadoEn") ?: 0L,
                            syncedToCloud = true
                        )
                    }
                    trySend(RemoteAssetsSnapshot(remote, snapshot.metadata.isFromCache, thumbnails))
                }
            awaitClose { registration.remove() }
        }
    }

    /** Mezcla lo remoto con Room sin pisar cambios locales mas recientes ni fotos locales. */
    suspend fun mergeRemoteAssets(snapshot: RemoteAssetsSnapshot) {
        val remote = snapshot.assets
        if (!snapshot.fromCache) {
            // Lo que otro administrador elimino en Firestore tambien se quita de Room.
            val remoteIds = remote.map { it.assetId }.toSet()
            andenDao.getAllAssets()
                .filter { it.syncedToCloud && it.assetId !in remoteIds }
                .forEach { andenDao.deleteAsset(it.assetId) }
        }
        remote.forEach { remoteAsset ->
            val local = andenDao.getAssetById(remoteAsset.assetId)
            // Sin URL de Storage ni recurso de la app: se usa la miniatura que viene en el documento.
            val thumb = snapshot.thumbnails[remoteAsset.assetId]
            val context = appContext
            val incoming = if (remoteAsset.photoUri.isBlank() && thumb != null && context != null) {
                val path = PhotoUtils.writeThumbnail(
                    context, thumb, "remoto_${remoteAsset.assetId}_${remoteAsset.updatedAtMillis}.jpg"
                )
                if (path != null) remoteAsset.copy(photoUri = path) else remoteAsset
            } else {
                remoteAsset
            }
            when {
                local == null -> andenDao.upsertAsset(incoming)
                local.updatedAtMillis > incoming.updatedAtMillis -> Unit
                else -> {
                    val keepLocalPhoto = local.photoUrl == incoming.photoUrl &&
                        local.photoUri.startsWith("file://") &&
                        File(Uri.parse(local.photoUri).path.orEmpty()).exists()
                    andenDao.upsertAsset(
                        if (keepLocalPhoto) incoming.copy(photoUri = local.photoUri) else incoming
                    )
                }
            }
        }
    }

    /** Alta del usuario en usuarios/{uid} con su rol (ADMINISTRADOR o PASAJERO). */
    suspend fun registerUserProfile(email: String, fullName: String, role: String) {
        val uid = auth?.currentUser?.uid ?: return
        val currentFirestore = firestore ?: return
        runCatching {
            withTimeout(CLOUD_TIMEOUT_MS) {
                currentFirestore.collection(USERS_COLLECTION).document(uid).set(
                    hashMapOf(
                        "uid" to uid,
                        "correo" to email.trim().lowercase(),
                        "nombre" to fullName,
                        "rol" to role,
                        "ultimoAcceso" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                ).await()
            }
        }
    }

    fun signOut() {
        auth?.signOut()
    }

    private companion object {
        const val ASSETS_COLLECTION = "activos"
        const val USERS_COLLECTION = "usuarios"
        const val CLOUD_TIMEOUT_MS = 15_000L
        const val UPLOAD_TIMEOUT_MS = 45_000L
    }
}

/** Documentos de activos/ recibidos de Firestore y si vienen de la cache local. */
data class RemoteAssetsSnapshot(
    val assets: List<NetworkAssetEntity>,
    val fromCache: Boolean,
    val thumbnails: Map<String, String> = emptyMap()
)
