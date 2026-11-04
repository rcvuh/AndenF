package mx.uaemex.anden.data.local.database

import mx.uaemex.anden.data.local.entity.NetworkAssetEntity

/**
 * Catalogo inicial de activos de la red del Tren El Insurgente (T4.10):
 * 5 en operacion y 5 fuera de servicio, cada uno con una fotografia distinta.
 */
object AssetSeedData {

    private const val TEAM = "Equipo Andén"

    /** Fecha fija del catalogo inicial: cualquier cambio hecho despues en Firestore gana. */
    const val SEED_TIME_MILLIS = 1_793_512_800_000L

    val assets: List<NetworkAssetEntity> = listOf(
        // ---------- ACTIVOS (en operacion) ----------
        NetworkAssetEntity(
            assetId = "ACT-01",
            name = "Unidad 01 · Tren El Insurgente",
            category = "Unidad",
            stationName = "Toluca Centro",
            description = "Tren en servicio regular en el andén de Toluca Centro con salidas cada 10 minutos hacia Observatorio.",
            isActive = true,
            photoUri = "res:activo_operacion_01",
            updatedBy = TEAM,
            updatedAtMillis = SEED_TIME_MILLIS
        ),
        NetworkAssetEntity(
            assetId = "ACT-02",
            name = "Unidad 04 · Abordaje en hora pico",
            category = "Unidad",
            stationName = "Observatorio",
            description = "Unidad operando con afluencia alta en la terminal Observatorio; abordaje ordenado por las puertas centrales.",
            isActive = true,
            photoUri = "res:activo_operacion_02",
            updatedBy = TEAM,
            updatedAtMillis = SEED_TIME_MILLIS
        ),
        NetworkAssetEntity(
            assetId = "ACT-03",
            name = "Andén 2 · Servicio nocturno",
            category = "Andén",
            stationName = "Metepec",
            description = "Andén iluminado y operando en el último corrido del día (23:00 h), con señalética y franja de seguridad visibles.",
            isActive = true,
            photoUri = "res:activo_operacion_03",
            updatedBy = TEAM,
            updatedAtMillis = SEED_TIME_MILLIS
        ),
        NetworkAssetEntity(
            assetId = "ACT-04",
            name = "Unidad 07 · Vagón con asientos disponibles",
            category = "Unidad",
            stationName = "Zinacantepec",
            description = "Vagón en operación fuera de hora pico, pantallas de información encendidas y asientos preferentes libres.",
            isActive = true,
            photoUri = "res:activo_operacion_04",
            updatedBy = TEAM,
            updatedAtMillis = SEED_TIME_MILLIS
        ),
        NetworkAssetEntity(
            assetId = "ACT-05",
            name = "Unidad 03 · Tramo Santa Fe",
            category = "Unidad",
            stationName = "Santa Fe",
            description = "Unidad en recorrido por el tramo elevado de Santa Fe con ocupación alta y servicio sin retrasos.",
            isActive = true,
            photoUri = "res:activo_operacion_05",
            updatedBy = TEAM,
            updatedAtMillis = SEED_TIME_MILLIS
        ),
        // ---------- INACTIVOS (fuera de servicio / mantenimiento) ----------
        NetworkAssetEntity(
            assetId = "INA-01",
            name = "Tramo de vía Lerma – Metepec",
            category = "Tramo",
            stationName = "Lerma",
            description = "Cuadrilla de mantenimiento trabajando en rieles y fijaciones. Tramo cerrado mientras se termina la revisión.",
            isActive = false,
            photoUri = "res:activo_mantenimiento_01",
            updatedBy = TEAM,
            updatedAtMillis = SEED_TIME_MILLIS
        ),
        NetworkAssetEntity(
            assetId = "INA-02",
            name = "Acceso poniente · Obra en estructura",
            category = "Estación",
            stationName = "Observatorio",
            description = "Escaleras y estructura del acceso poniente en obra. Se usa el acceso oriente mientras dura la construcción.",
            isActive = false,
            photoUri = "res:activo_mantenimiento_02",
            updatedBy = TEAM,
            updatedAtMillis = SEED_TIME_MILLIS
        ),
        NetworkAssetEntity(
            assetId = "INA-03",
            name = "Andén 1 · Cerrado por revisión",
            category = "Andén",
            stationName = "Vasco de Quiroga",
            description = "Andén sin servicio por inspección de cubierta y vías; los trenes se detienen solo en el andén 2.",
            isActive = false,
            photoUri = "res:activo_mantenimiento_03",
            updatedBy = TEAM,
            updatedAtMillis = SEED_TIME_MILLIS
        ),
        NetworkAssetEntity(
            assetId = "INA-04",
            name = "Tramo elevado · Inspección de vía",
            category = "Tramo",
            stationName = "Zinacantepec",
            description = "Personal técnico revisa el tramo elevado entre Zinacantepec y Toluca Centro; circulación suspendida en esa vía.",
            isActive = false,
            photoUri = "res:activo_mantenimiento_04",
            updatedBy = TEAM,
            updatedAtMillis = SEED_TIME_MILLIS
        ),
        NetworkAssetEntity(
            assetId = "INA-05",
            name = "Unidad 05 · Retirada tras falla",
            category = "Unidad",
            stationName = "Toluca Centro",
            description = "Unidad fuera de servicio después de una falla que obligó a evacuar a los pasajeros; en revisión en el taller.",
            isActive = false,
            photoUri = "res:activo_mantenimiento_05",
            updatedBy = TEAM,
            updatedAtMillis = SEED_TIME_MILLIS
        )
    )
}
