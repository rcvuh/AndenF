package mx.uaemex.anden.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mx.uaemex.anden.data.local.dao.AndenDao
import mx.uaemex.anden.data.local.entity.CommunityStopEntity
import mx.uaemex.anden.data.local.entity.NetworkAssetEntity
import mx.uaemex.anden.data.local.entity.PendingReportEntity
import mx.uaemex.anden.data.local.entity.StationEntity

@Database(
    entities = [
        StationEntity::class,
        PendingReportEntity::class,
        CommunityStopEntity::class,
        NetworkAssetEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AndenDatabase : RoomDatabase() {

    abstract fun andenDao(): AndenDao

    companion object {
        @Volatile
        private var INSTANCE: AndenDatabase? = null

        fun getInstance(context: Context): AndenDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AndenDatabase::class.java,
                    "anden_movilidad_toluca.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                CoroutineScope(Dispatchers.IO).launch {
                    seedInitialDataIfNeeded(instance.andenDao())
                }
                instance
            }
        }

        private suspend fun seedInitialDataIfNeeded(dao: AndenDao) {
            if (dao.getAssetsCount() == 0) {
                dao.upsertAssets(AssetSeedData.assets)
            }
            val initialStations = listOf(
                StationEntity(
                    stationId = "INS-01",
                    name = "Zinacantepec",
                    systemLine = "Tren El Insurgente",
                    municipality = "Zinacantepec, Méx. (Blvd. Las Torres esq. 16 de Septiembre)",
                    latitude = 19.281861,
                    longitude = -99.695389,
                    scheduleText = "Horario: 06:00 AM - 11:00 PM • Frecuencia: 10 min",
                    elevatorStatus = "Operando (2/2)",
                    rampStatus = "Accesibles (Norte y Sur)",
                    serviceStatus = "Servicio Regular (Sin retrasos)",
                    feederConnections = "Línea 6 Mexibús (Terminal Zinacantepec);Corredor Av. Las Torres;Ramal Villa Cuauhtémoc y Almoloya;Ruta Alameda 2000 - CU UAEMéx",
                    isFavorite = true
                ),
                StationEntity(
                    stationId = "INS-02",
                    name = "Toluca Centro",
                    systemLine = "Tren El Insurgente",
                    municipality = "Toluca de Lerdo, Méx. (Blvd. Las Torres esq. Pino Suárez)",
                    latitude = 19.270944,
                    longitude = -99.643194,
                    scheduleText = "Horario: 06:00 AM - 11:00 PM • Frecuencia: 10 min",
                    elevatorStatus = "Operando (2/2)",
                    rampStatus = "Accesibles (Pino Suárez)",
                    serviceStatus = "Servicio Regular (Sin retrasos)",
                    feederConnections = "Terminal de Autobuses de Toluca (TAT);Corredor Pino Suárez - Centro Histórico;Circuito Tollocan - CU UAEMéx;Ramal Tenango del Valle",
                    isFavorite = true
                ),
                StationEntity(
                    stationId = "INS-03",
                    name = "Metepec",
                    systemLine = "Tren El Insurgente",
                    municipality = "Metepec, Méx. (Blvd. Las Torres esq. Av. Tecnológico)",
                    latitude = 19.277194,
                    longitude = -99.575417,
                    scheduleText = "Horario: 06:00 AM - 11:00 PM • Frecuencia: 10 min",
                    elevatorStatus = "Operando (2/2)",
                    rampStatus = "Accesibles (Av. Tecnológico)",
                    serviceStatus = "Servicio Regular (Sin retrasos)",
                    feederConnections = "Corredor Av. Tecnológico - Tianguistenco;Línea 6 Mexibús (Conexión Tecnológico);Ruta Pueblo Mágico Metepec - San Bartolomé;Circuito Tollocan",
                    isFavorite = true
                ),
                StationEntity(
                    stationId = "INS-04",
                    name = "Lerma",
                    systemLine = "Tren El Insurgente",
                    municipality = "Lerma de Villada, Méx. (Blvd. Las Torres / Plazas Outlet)",
                    latitude = 19.282528,
                    longitude = -99.512472,
                    scheduleText = "Horario: 06:00 AM - 11:00 PM • Frecuencia: 10 min",
                    elevatorStatus = "En mantenimiento",
                    rampStatus = "Accesibles (Acceso Poniente)",
                    serviceStatus = "Afluencia moderada en andén",
                    feederConnections = "Línea 6 Mexibús (Terminal Lerma);Corredor Parque Industrial Lerma;Ruta Ocoyoacac - Santiago Tianguistenco;Ramal Amomolulco",
                    isFavorite = false
                ),
                StationEntity(
                    stationId = "INS-05",
                    name = "Santa Fe",
                    systemLine = "Tren El Insurgente",
                    municipality = "Cuajimalpa / Álvaro Obregón, CDMX (Av. Vasco de Quiroga)",
                    latitude = 19.361417,
                    longitude = -99.273583,
                    scheduleText = "Horario: 06:00 AM - 11:00 PM • Frecuencia: 10 min",
                    elevatorStatus = "Operando (3/3)",
                    rampStatus = "Accesibles (Pasarela Centro Comercial)",
                    serviceStatus = "Servicio Regular (Sin retrasos)",
                    feederConnections = "RTP Ruta 34-A (Santa Fe - Metro Balderas);RTP Ruta 76 (Santa Fe - Metro Auditorio);Corredor Av. Santa Fe - Reforma;Transporte Universitario Ibero/UAM",
                    isFavorite = true
                ),
                StationEntity(
                    stationId = "INS-06",
                    name = "Vasco de Quiroga",
                    systemLine = "Tren El Insurgente",
                    municipality = "Álvaro Obregón, CDMX (Pueblo de Santa Fe)",
                    latitude = 19.380639,
                    longitude = -99.242194,
                    scheduleText = "Horario: 06:00 AM - 11:00 PM • Frecuencia: 10 min",
                    elevatorStatus = "Operando (2/2)",
                    rampStatus = "Accesibles",
                    serviceStatus = "Servicio Regular (Sin retrasos)",
                    feederConnections = "Cablebús Línea 3 (Vasco de Quiroga - Los Pinos/Constituyentes);Corredor Pueblo de Santa Fe;Ruta Tacubaya - Observatorio",
                    isFavorite = false
                ),
                StationEntity(
                    stationId = "INS-07",
                    name = "Observatorio",
                    systemLine = "Tren El Insurgente",
                    municipality = "Álvaro Obregón, CDMX (CETRAM Observatorio)",
                    latitude = 19.398222,
                    longitude = -99.200333,
                    scheduleText = "Horario: 06:00 AM - 11:00 PM • Frecuencia: 10 min",
                    elevatorStatus = "Operando (4/4)",
                    rampStatus = "Accesibles (Interconexión CETRAM)",
                    serviceStatus = "Enlace multimodal CETRAM Observatorio",
                    feederConnections = "Metro CDMX Línea 1 (Observatorio - Pantitlán);Terminal Central de Autobuses del Poniente;CETRAM Observatorio (Rutas CDMX)",
                    isFavorite = true
                )
            )
            dao.insertStations(initialStations)

            val now = System.currentTimeMillis()
            val recentThreshold = now - (25 * 60 * 1000L)
            if (dao.getRecentReportsCount(recentThreshold) == 0) {
                val initialReports = listOf(
                    PendingReportEntity(
                        stationId = "INS-01",
                        stationName = "Zinacantepec",
                        incidentType = "Elevador funcionando",
                        description = "Ambos elevadores de la terminal Zinacantepec operando y tren saliendo puntual.",
                        mediaUri = "content://anden/evidencia_zinacantepec_1.jpg",
                        isVideo = false,
                        latitude = 19.2819,
                        longitude = -99.6954,
                        accuracyMeters = 4.2f,
                        distanceToStationMeters = 45,
                        authorHandle = "@z***c_anon",
                        timestampMillis = now - (4 * 60 * 1000L),
                        syncStatus = "SINCRONIZADO_FIRESTORE",
                        moderationStatus = "APROBADO"
                    ),
                    PendingReportEntity(
                        stationId = "INS-01",
                        stationName = "Zinacantepec",
                        incidentType = "Aglomeración",
                        description = "Abordaje fluido en andén 1, conexión con Mexibús L6 sin fila.",
                        mediaUri = "",
                        isVideo = false,
                        latitude = 19.2818,
                        longitude = -99.6953,
                        accuracyMeters = 5.0f,
                        distanceToStationMeters = 60,
                        authorHandle = "@p***o_anon",
                        timestampMillis = now - (11 * 60 * 1000L),
                        syncStatus = "SINCRONIZADO_FIRESTORE",
                        moderationStatus = "APROBADO"
                    ),
                    PendingReportEntity(
                        stationId = "INS-02",
                        stationName = "Toluca Centro",
                        incidentType = "Aglomeración",
                        description = "Alta afluencia en escaleras de Av. Pino Suárez por llegada de autobuses de la Terminal.",
                        mediaUri = "content://anden/evidencia_toluca_centro.jpg",
                        isVideo = false,
                        latitude = 19.2709,
                        longitude = -99.6432,
                        accuracyMeters = 4.9f,
                        distanceToStationMeters = 72,
                        authorHandle = "@t***a_anon",
                        timestampMillis = now - (6 * 60 * 1000L),
                        syncStatus = "SINCRONIZADO_FIRESTORE",
                        moderationStatus = "APROBADO"
                    ),
                    PendingReportEntity(
                        stationId = "INS-02",
                        stationName = "Toluca Centro",
                        incidentType = "Elevador funcionando",
                        description = "Elevador de acceso hacia Pino Suárez funcionando correctamente para sillas de ruedas.",
                        mediaUri = "",
                        isVideo = false,
                        latitude = 19.2710,
                        longitude = -99.6431,
                        accuracyMeters = 5.1f,
                        distanceToStationMeters = 54,
                        authorHandle = "@u***x_anon",
                        timestampMillis = now - (14 * 60 * 1000L),
                        syncStatus = "SINCRONIZADO_FIRESTORE",
                        moderationStatus = "APROBADO"
                    ),
                    PendingReportEntity(
                        stationId = "INS-03",
                        stationName = "Metepec",
                        incidentType = "Aglomeración",
                        description = "Flujo continuo en torniquetes de Av. Tecnológico, tiempo de espera menor a 4 minutos.",
                        mediaUri = "content://anden/evidencia_metepec_andenes.jpg",
                        isVideo = false,
                        latitude = 19.2772,
                        longitude = -99.5754,
                        accuracyMeters = 4.5f,
                        distanceToStationMeters = 85,
                        authorHandle = "@h***z_anon",
                        timestampMillis = now - (5 * 60 * 1000L),
                        syncStatus = "SINCRONIZADO_FIRESTORE",
                        moderationStatus = "APROBADO"
                    ),
                    PendingReportEntity(
                        stationId = "INS-03",
                        stationName = "Metepec",
                        incidentType = "Elevador funcionando",
                        description = "Elevador oriente de estación Metepec operando sin contratiempos.",
                        mediaUri = "content://anden/evidencia_elevador_metepec.jpg",
                        isVideo = false,
                        latitude = 19.2772,
                        longitude = -99.5755,
                        accuracyMeters = 5.0f,
                        distanceToStationMeters = 62,
                        authorHandle = "@r***o_anon",
                        timestampMillis = now - (12 * 60 * 1000L),
                        syncStatus = "SINCRONIZADO_FIRESTORE",
                        moderationStatus = "APROBADO"
                    ),
                    PendingReportEntity(
                        stationId = "INS-04",
                        stationName = "Lerma",
                        incidentType = "Elevador dañado",
                        description = "Elevador oriente en mantenimiento programado; personal de estación apoyando por rampa poniente.",
                        mediaUri = "content://anden/evidencia_lerma_elevador.jpg",
                        isVideo = false,
                        latitude = 19.2825,
                        longitude = -99.5125,
                        accuracyMeters = 4.7f,
                        distanceToStationMeters = 78,
                        authorHandle = "@u***l_anon",
                        timestampMillis = now - (8 * 60 * 1000L),
                        syncStatus = "SINCRONIZADO_FIRESTORE",
                        moderationStatus = "APROBADO"
                    ),
                    PendingReportEntity(
                        stationId = "INS-04",
                        stationName = "Lerma",
                        incidentType = "Retraso de tren",
                        description = "Tren dirección Santa Fe saliendo con 3 minutos de demora por lluvia en La Marquesa.",
                        mediaUri = "",
                        isVideo = false,
                        latitude = 19.2826,
                        longitude = -99.5124,
                        accuracyMeters = 5.4f,
                        distanceToStationMeters = 92,
                        authorHandle = "@l***a_anon",
                        timestampMillis = now - (16 * 60 * 1000L),
                        syncStatus = "SINCRONIZADO_FIRESTORE",
                        moderationStatus = "APROBADO"
                    ),
                    PendingReportEntity(
                        stationId = "INS-05",
                        stationName = "Santa Fe",
                        incidentType = "Aglomeración",
                        description = "Abordaje ordenado en enlace con RTP hacia Metro Auditorio y Balderas.",
                        mediaUri = "content://anden/evidencia_santafe.jpg",
                        isVideo = false,
                        latitude = 19.3614,
                        longitude = -99.2736,
                        accuracyMeters = 4.6f,
                        distanceToStationMeters = 64,
                        authorHandle = "@s***e_anon",
                        timestampMillis = now - (7 * 60 * 1000L),
                        syncStatus = "SINCRONIZADO_FIRESTORE",
                        moderationStatus = "APROBADO"
                    ),
                    PendingReportEntity(
                        stationId = "INS-05",
                        stationName = "Santa Fe",
                        incidentType = "Elevador funcionando",
                        description = "Los 3 elevadores hacia la pasarela del Centro Comercial Santa Fe operan al 100%.",
                        mediaUri = "",
                        isVideo = false,
                        latitude = 19.3615,
                        longitude = -99.2735,
                        accuracyMeters = 5.2f,
                        distanceToStationMeters = 50,
                        authorHandle = "@c***x_anon",
                        timestampMillis = now - (18 * 60 * 1000L),
                        syncStatus = "SINCRONIZADO_FIRESTORE",
                        moderationStatus = "APROBADO"
                    ),
                    PendingReportEntity(
                        stationId = "INS-06",
                        stationName = "Vasco de Quiroga",
                        incidentType = "Elevador funcionando",
                        description = "Conexión con Cablebús Línea 3 abierta y elevadores en operación continua.",
                        mediaUri = "content://anden/evidencia_vasco.jpg",
                        isVideo = false,
                        latitude = 19.3806,
                        longitude = -99.2422,
                        accuracyMeters = 4.8f,
                        distanceToStationMeters = 55,
                        authorHandle = "@v***q_anon",
                        timestampMillis = now - (9 * 60 * 1000L),
                        syncStatus = "SINCRONIZADO_FIRESTORE",
                        moderationStatus = "APROBADO"
                    ),
                    PendingReportEntity(
                        stationId = "INS-07",
                        stationName = "Observatorio",
                        incidentType = "Aglomeración",
                        description = "Transbordo activo hacia CETRAM Observatorio y Terminal de Autobuses Poniente.",
                        mediaUri = "content://anden/evidencia_observatorio.jpg",
                        isVideo = false,
                        latitude = 19.3982,
                        longitude = -99.2003,
                        accuracyMeters = 4.3f,
                        distanceToStationMeters = 70,
                        authorHandle = "@o***o_anon",
                        timestampMillis = now - (6 * 60 * 1000L),
                        syncStatus = "SINCRONIZADO_FIRESTORE",
                        moderationStatus = "APROBADO"
                    )
                )
                dao.insertReports(initialReports)
            }

            if (dao.getCommunityStopsCount() == 0) {
                val initialStops = listOf(
                    CommunityStopEntity(
                        title = "Parada: Av. Tecnológico esq. Las Torres (Ruta Tianguistenco)",
                        corridorReference = "Bahía de ascenso hacia Gualupita y Santiago Tianguistenco (CU UAEMéx)",
                        nearestStationName = "Metepec",
                        authorHandle = "@h***z_anon",
                        timeAgoText = "Hace 25 min",
                        latitude = 19.2768,
                        longitude = -99.5758,
                        moderationStatus = "PENDIENTE"
                    ),
                    CommunityStopEntity(
                        title = "Parada: Pino Suárez esq. Las Torres (Acceso TAT)",
                        corridorReference = "Conexión peatonal techada hacia Terminal de Autobuses de Toluca",
                        nearestStationName = "Toluca Centro",
                        authorHandle = "@r***o_anon",
                        timeAgoText = "Hace 40 min",
                        latitude = 19.2712,
                        longitude = -99.6435,
                        moderationStatus = "PENDIENTE"
                    ),
                    CommunityStopEntity(
                        title = "Parada: Blvd. Las Torres esq. 16 de Septiembre (Mexibús L6)",
                        corridorReference = "Andén alimentador Línea 6 Mexibús hacia Centro de Zinacantepec",
                        nearestStationName = "Zinacantepec",
                        authorHandle = "@u***l_anon",
                        timeAgoText = "Hace 1 hora",
                        latitude = 19.2821,
                        longitude = -99.6951,
                        moderationStatus = "PENDIENTE"
                    ),
                    CommunityStopEntity(
                        title = "Parada: Plazas Outlet Lerma • Bahía Corredor Industrial",
                        corridorReference = "Transporte de personal Parque Industrial Lerma y Ocoyoacac",
                        nearestStationName = "Lerma",
                        authorHandle = "@c***d_anon",
                        timeAgoText = "Hace 1 hora",
                        latitude = 19.2828,
                        longitude = -99.5121,
                        moderationStatus = "VALIDADA"
                    )
                )
                dao.insertCommunityStops(initialStops)
            }
        }
    }
}
