package mx.uaemex.anden.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import mx.uaemex.anden.data.local.database.AndenDatabase
import mx.uaemex.anden.data.local.entity.CommunityStopEntity
import mx.uaemex.anden.data.local.entity.NetworkAssetEntity
import mx.uaemex.anden.domain.TeamMembers
import mx.uaemex.anden.data.local.entity.PendingReportEntity
import mx.uaemex.anden.data.local.entity.StationEntity
import mx.uaemex.anden.data.repository.AndenRepository
import mx.uaemex.anden.data.repository.PhotoUtils
import mx.uaemex.anden.domain.AndenRules
import mx.uaemex.anden.domain.SecurityPrivacyRules
import mx.uaemex.anden.hardware.GpsReading
import mx.uaemex.anden.hardware.LocationGeofenceHelper

data class AndenUiState(
    val currentUserEmail: String = "",
    val isGuest: Boolean = false,
    val isAdminRole: Boolean = false,
    val selectedStationId: String = "INS-03",
    val searchQuery: String = "",
    val filterAccessibleOnly: Boolean = false,
    val filterFavoritesOnly: Boolean = false,
    val isOfflineSimulationMode: Boolean = false,
    val highContrastAccessibleMode: Boolean = false,
    val anonymousPrivacyMode: Boolean = true,
    val capturedMediaUri: String = "",
    val capturedIsVideo: Boolean = false,
    val gpsReading: GpsReading = GpsReading(
        latitude = 19.277194,
        longitude = -99.575417,
        accuracyMeters = 4.5f,
        nearestStation = null,
        distanceToNearestMeters = 85,
        isWithinGeofence300m = true
    ),
    val snackbarMessage: String? = null,
    val assetFilter: String = AssetFilter.TODOS
)

/** Filtro de la pantalla Activos de la red. */
object AssetFilter {
    const val TODOS = "TODOS"
    const val ACTIVOS = "ACTIVOS"
    const val INACTIVOS = "INACTIVOS"
}

class ReportViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AndenDatabase.getInstance(application)
    private val repository = AndenRepository(database.andenDao(), application.applicationContext)
    private val locationHelper = LocationGeofenceHelper(application)

    private val _uiState = MutableStateFlow(AndenUiState())
    val uiState: StateFlow<AndenUiState> = _uiState.asStateFlow()

    val stations: StateFlow<List<StationEntity>> = repository.allStations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReports: StateFlow<List<PendingReportEntity>> = repository.allReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val communityStops: StateFlow<List<CommunityStopEntity>> = repository.allCommunityStops
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredStations: StateFlow<List<StationEntity>> = combine(
        stations,
        _uiState
    ) { list, state ->
        AndenRules.filterStations(
            stations = list,
            query = state.searchQuery,
            accessibleOnly = state.filterAccessibleOnly,
            favoritesOnly = state.filterFavoritesOnly
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ---------- T4.10 · Activos de la red ----------
    val assets: StateFlow<List<NetworkAssetEntity>> = repository.allAssets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredAssets: StateFlow<List<NetworkAssetEntity>> = combine(assets, _uiState) { list, state ->
        when (state.assetFilter) {
            AssetFilter.ACTIVOS -> list.filter { it.isActive }
            AssetFilter.INACTIVOS -> list.filter { !it.isActive }
            else -> list
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Lo que dan de alta los demas integrantes llega por Firestore y se guarda en Room.
        viewModelScope.launch {
            runCatching {
                repository.observeRemoteAssets().collect { snapshot ->
                    withContext(Dispatchers.IO) { repository.mergeRemoteAssets(snapshot) }
                }
            }
        }
    }

    fun setAssetFilter(filter: String) {
        _uiState.value = _uiState.value.copy(assetFilter = filter)
    }

    /**
     * Alta (existing == null) o edicion de un activo. Si se eligio una foto nueva se
     * copia a la memoria de la app para que no se pierda al cerrar el selector.
     */
    fun saveAsset(
        existing: NetworkAssetEntity?,
        name: String,
        category: String,
        stationName: String,
        description: String,
        isActive: Boolean,
        pickedPhoto: Uri?,
        onDone: () -> Unit
    ) {
        val cleanName = SecurityPrivacyRules.sanitizeReportDescription(name).trim()
        val cleanDesc = SecurityPrivacyRules.sanitizeReportDescription(description).trim()
        if (cleanName.isBlank()) {
            _uiState.value = _uiState.value.copy(snackbarMessage = "Escribe el nombre del activo")
            return
        }
        if (existing == null && pickedPhoto == null) {
            _uiState.value = _uiState.value.copy(snackbarMessage = "Agrega una fotografía del activo")
            return
        }
        viewModelScope.launch {
            val assetId = existing?.assetId ?: "AST-" + UUID.randomUUID().toString().take(8).uppercase()
            val localPhoto = pickedPhoto?.let { copyPhotoToAppStorage(it, assetId) }
            val asset = NetworkAssetEntity(
                assetId = assetId,
                name = cleanName,
                category = category,
                stationName = stationName,
                description = cleanDesc.ifBlank { "Sin descripción" },
                isActive = isActive,
                photoUri = localPhoto ?: existing?.photoUri.orEmpty(),
                photoUrl = if (localPhoto != null) "" else existing?.photoUrl.orEmpty(),
                updatedBy = currentEditorName()
            )
            val saved = repository.saveAssetLocal(asset)
            onDone()
            _uiState.value = _uiState.value.copy(
                snackbarMessage = (if (existing == null) "Activo dado de alta" else "Activo actualizado") + " en Room"
            )
            if (repository.pushAssetToCloud(saved)) {
                _uiState.value = _uiState.value.copy(snackbarMessage = "${saved.name}: respaldado en Firestore")
            }
        }
    }

    fun toggleAssetActive(asset: NetworkAssetEntity) {
        viewModelScope.launch {
            val next = !asset.isActive
            val saved = repository.saveAssetLocal(asset.copy(isActive = next, updatedBy = currentEditorName()))
            _uiState.value = _uiState.value.copy(
                snackbarMessage = "${asset.name}: " + if (next) "EN OPERACIÓN" else "FUERA DE SERVICIO"
            )
            repository.pushAssetToCloud(saved)
        }
    }

    fun deleteAsset(asset: NetworkAssetEntity) {
        viewModelScope.launch {
            repository.deleteAsset(asset.assetId)
            _uiState.value = _uiState.value.copy(snackbarMessage = "Activo eliminado: ${asset.name}")
        }
    }

    fun pushPendingAssets() {
        viewModelScope.launch {
            val count = repository.pushPendingAssets()
            if (count > 0) {
                _uiState.value = _uiState.value.copy(snackbarMessage = "$count activo(s) respaldados en Firestore")
            }
        }
    }

    fun logout() {
        repository.signOut()
        _uiState.value = AndenUiState()
    }

    private fun currentEditorName(): String {
        val email = _uiState.value.currentUserEmail
        return TeamMembers.findByEmail(email)?.fullName ?: email.ifBlank { "Administrador" }
    }

    private suspend fun copyPhotoToAppStorage(source: Uri, assetId: String): String? =
        withContext(Dispatchers.IO) {
            PhotoUtils.saveReduced(
                getApplication<Application>(), source, "${assetId}_${System.currentTimeMillis()}.jpg"
            )?.let { Uri.fromFile(it).toString() }
        }

    companion object {
        const val REPORT_EXPIRATION_MILLIS = AndenRules.REPORT_EXPIRATION_MILLIS
    }

    fun getActiveReportsForStation(stationId: String, reports: List<PendingReportEntity>): List<PendingReportEntity> =
        AndenRules.activeReportsForStation(stationId, reports, System.currentTimeMillis())

    fun loginAsCitizen(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.signInWithEmail(email, password)
            val isTeamAdmin = TeamMembers.findByEmail(email) != null
            viewModelScope.launch {
                repository.registerUserProfile(
                    email = email,
                    fullName = TeamMembers.findByEmail(email)?.fullName ?: email.substringBefore('@'),
                    role = TeamMembers.roleFor(email)
                )
                if (isTeamAdmin) repository.pushPendingAssets()
            }
            _uiState.value = _uiState.value.copy(
                currentUserEmail = email.ifBlank { "hector.rodriguez@alumno.uaemex.mx" },
                isGuest = false,
                isAdminRole = isTeamAdmin,
                snackbarMessage = if (isTeamAdmin) {
                    "Bienvenido, ${TeamMembers.findByEmail(email)?.fullName}: perfil Administrador"
                } else {
                    "Sesión iniciada correctamente con Firebase Auth"
                }
            )
            refreshGpsLocation()
            onSuccess()
        }
    }

    fun loginAsGuest(onSuccess: () -> Unit) {
        _uiState.value = _uiState.value.copy(
            currentUserEmail = "Invitado (Solo Consulta)",
            isGuest = true,
            isAdminRole = false,
            snackbarMessage = "Modo Invitado activo: Catálogo Offline en Room disponible"
        )
        refreshGpsLocation()
        onSuccess()
    }

    fun loginAsAdmin(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val adminEmail = email.ifBlank { "supervisor@movilidad.edomex.gob.mx" }
            if (!TeamMembers.isAdmin(adminEmail)) {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "Acceso denegado: $adminEmail no está dado de alta como administrador"
                )
                return@launch
            }
            repository.signInWithEmail(adminEmail, password)
            val member = TeamMembers.findByEmail(adminEmail)
            viewModelScope.launch {
                repository.registerUserProfile(
                    email = adminEmail,
                    fullName = member?.fullName ?: "Supervisor de Movilidad",
                    role = TeamMembers.ADMIN_ROLE
                )
                repository.pushPendingAssets()
            }
            _uiState.value = _uiState.value.copy(
                currentUserEmail = adminEmail,
                isGuest = false,
                isAdminRole = true,
                snackbarMessage = if (member != null) {
                    "Administrador verificado: ${member.fullName} (equipo desarrollador)"
                } else {
                    "Perfil Administrador RBAC verificado (correo institucional)"
                }
            )
            onSuccess()
        }
    }

    fun selectStation(stationId: String) {
        _uiState.value = _uiState.value.copy(selectedStationId = stationId)
        val station = stations.value.find { it.stationId == stationId }
        if (station != null) {
            val simulatedReading = locationHelper.simulateInStationGeofence(
                station = station,
                stations = stations.value,
                simulatedDistanceMeters = 85
            )
            _uiState.value = _uiState.value.copy(gpsReading = simulatedReading)
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun toggleAccessibleFilter() {
        val next = !_uiState.value.filterAccessibleOnly
        _uiState.value = _uiState.value.copy(
            filterAccessibleOnly = next,
            snackbarMessage = if (next) "Filtro activo: Solo estaciones con elevador operando" else null
        )
    }

    fun toggleFavoritesFilter() {
        val next = !_uiState.value.filterFavoritesOnly
        _uiState.value = _uiState.value.copy(filterFavoritesOnly = next)
    }

    fun toggleFavoriteStation(station: StationEntity) {
        viewModelScope.launch {
            repository.toggleFavoriteStation(station.stationId, station.isFavorite)
        }
    }

    fun toggleHighContrastMode() {
        val next = !_uiState.value.highContrastAccessibleMode
        _uiState.value = _uiState.value.copy(
            highContrastAccessibleMode = next,
            snackbarMessage = if (next) {
                "Accesibilidad WCAG 2.1: Modo Alto Contraste Activado"
            } else {
                "Tema Institucional Estándar Restaurado"
            }
        )
    }

    fun toggleAnonymousPrivacyMode() {
        val next = !_uiState.value.anonymousPrivacyMode
        _uiState.value = _uiState.value.copy(
            anonymousPrivacyMode = next,
            snackbarMessage = if (next) {
                "Privacidad LFPDPPP: Seudonimización y Ofuscación GPS Activas"
            } else {
                "Modo Identificador Público Activo"
            }
        )
    }

    fun toggleOfflineMode() {
        val nextOffline = !_uiState.value.isOfflineSimulationMode
        _uiState.value = _uiState.value.copy(
            isOfflineSimulationMode = nextOffline,
            snackbarMessage = if (nextOffline) {
                "Modo Sin Conexión (Andén Subterráneo): Guardando en Room SQLite"
            } else {
                "Conexión restablecida: Sincronizando cola Room -> Firebase Firestore"
            }
        )
        if (!nextOffline) {
            syncPendingReportsNow()
        }
    }

    fun refreshGpsLocation(simulatedDistance: Int? = null) {
        viewModelScope.launch {
            val list = stations.value
            val target = list.find { it.stationId == _uiState.value.selectedStationId }
            val reading = if (simulatedDistance != null) {
                locationHelper.simulateInStationGeofence(target, list, simulatedDistance)
            } else {
                locationHelper.fetchCurrentGpsReading(list, target)
            }
            _uiState.value = _uiState.value.copy(gpsReading = reading)
        }
    }

    fun onMediaCaptured(uriString: String, isVideo: Boolean) {
        _uiState.value = _uiState.value.copy(
            capturedMediaUri = uriString,
            capturedIsVideo = isVideo
        )
    }

    fun clearCapturedMedia() {
        _uiState.value = _uiState.value.copy(
            capturedMediaUri = "",
            capturedIsVideo = false
        )
    }

    fun submitCitizenReport(
        incidentType: String,
        description: String,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val state = _uiState.value
            val station = stations.value.find { it.stationId == state.selectedStationId }
            val stationName = station?.name ?: "Metepec"
            val isOnline = !state.isOfflineSimulationMode

            val sanitizedDesc = SecurityPrivacyRules.sanitizeReportDescription(
                description.ifBlank { "Reporte de $incidentType verificado con GPS en estación $stationName." }
            )
            val authorHandle = SecurityPrivacyRules.pseudonymizeAuthorHandle(
                email = state.currentUserEmail.ifBlank { "hector.rodriguez@alumno.uaemex.mx" },
                isGuest = state.isGuest,
                anonymousPrivacyMode = state.anonymousPrivacyMode
            )
            val safeLat = if (state.anonymousPrivacyMode) {
                SecurityPrivacyRules.obfuscateCoordinateForPrivacy(state.gpsReading.latitude)
            } else {
                state.gpsReading.latitude
            }
            val safeLng = if (state.anonymousPrivacyMode) {
                SecurityPrivacyRules.obfuscateCoordinateForPrivacy(state.gpsReading.longitude)
            } else {
                state.gpsReading.longitude
            }

            val newReport = PendingReportEntity(
                stationId = state.selectedStationId,
                stationName = stationName,
                incidentType = incidentType,
                description = sanitizedDesc,
                mediaUri = state.capturedMediaUri,
                isVideo = state.capturedIsVideo,
                latitude = safeLat,
                longitude = safeLng,
                accuracyMeters = state.gpsReading.accuracyMeters,
                distanceToStationMeters = state.gpsReading.distanceToNearestMeters,
                authorHandle = authorHandle,
                timestampMillis = System.currentTimeMillis()
            )

            val result = repository.createAndSyncReport(newReport, isOnline = isOnline)
            val msg = if (result.syncStatus == "SINCRONIZADO_FIRESTORE") {
                "Reporte en ${stationName} (${authorHandle}) sincronizado en Firestore (${result.firestoreDocId})"
            } else {
                "Sin red: Reporte en ${stationName} (${authorHandle}) encolado en Room SQLite"
            }

            _uiState.value = _uiState.value.copy(
                capturedMediaUri = "",
                capturedIsVideo = false,
                snackbarMessage = msg
            )
            onComplete()
        }
    }

    fun proposeNewCommunityStop(
        title: String,
        corridorReference: String
    ) {
        viewModelScope.launch {
            val state = _uiState.value
            val station = stations.value.find { it.stationId == state.selectedStationId }
            val stationName = station?.name ?: "Metepec"
            val cleanTitle = SecurityPrivacyRules.sanitizeReportDescription(title)
            val cleanRef = SecurityPrivacyRules.sanitizeReportDescription(corridorReference)
            if (cleanTitle.isBlank()) return@launch

            val authorHandle = SecurityPrivacyRules.pseudonymizeAuthorHandle(
                email = state.currentUserEmail.ifBlank { "hector.rodriguez@alumno.uaemex.mx" },
                isGuest = state.isGuest,
                anonymousPrivacyMode = state.anonymousPrivacyMode
            )

            val stop = CommunityStopEntity(
                title = cleanTitle,
                corridorReference = cleanRef.ifBlank { "Conexión alimentadora hacia estación $stationName" },
                nearestStationName = stationName,
                authorHandle = authorHandle,
                timeAgoText = "Hace 1 min",
                latitude = station?.latitude ?: state.gpsReading.latitude,
                longitude = station?.longitude ?: state.gpsReading.longitude,
                moderationStatus = "PENDIENTE"
            )
            repository.proposeCommunityStop(stop)
            _uiState.value = _uiState.value.copy(
                snackbarMessage = "Parada alimentadora propuesta en $stationName enviada a Moderación Admin"
            )
        }
    }

    fun updateStationStatusByAdmin(
        stationId: String,
        elevatorStatus: String,
        serviceStatus: String
    ) {
        viewModelScope.launch {
            repository.updateStationStatusByAdmin(stationId, elevatorStatus, serviceStatus)
            val stName = stations.value.find { it.stationId == stationId }?.name ?: stationId
            _uiState.value = _uiState.value.copy(
                snackbarMessage = "Estación $stName actualizada en Room y Firestore ($elevatorStatus • $serviceStatus)"
            )
        }
    }

    fun syncPendingReportsNow() {
        viewModelScope.launch {
            val count = repository.syncAllPendingOfflineReports()
            if (count > 0) {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "$count reporte(s) pendientes subidos de Room a Firestore y Cloud Storage"
                )
            }
        }
    }

    fun moderateReport(reportId: Long, approve: Boolean) {
        viewModelScope.launch {
            repository.moderateReport(reportId, approve)
            _uiState.value = _uiState.value.copy(
                snackbarMessage = if (approve) "Reporte validado y publicado en Firestore" else "Reporte descartado por moderación"
            )
        }
    }

    fun moderateCommunityStop(stopId: Long, approve: Boolean) {
        viewModelScope.launch {
            repository.moderateCommunityStop(stopId, approve)
            _uiState.value = _uiState.value.copy(
                snackbarMessage = if (approve) "Parada comunitaria certificada en el corredor" else "Propuesta de parada descartada"
            )
        }
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}
