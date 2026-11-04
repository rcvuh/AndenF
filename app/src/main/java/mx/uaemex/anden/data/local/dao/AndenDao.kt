package mx.uaemex.anden.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import mx.uaemex.anden.data.local.entity.CommunityStopEntity
import mx.uaemex.anden.data.local.entity.NetworkAssetEntity
import mx.uaemex.anden.data.local.entity.PendingReportEntity
import mx.uaemex.anden.data.local.entity.StationEntity

@Dao
interface AndenDao {

    @Query("SELECT * FROM stations ORDER BY stationId ASC")
    fun observeAllStations(): Flow<List<StationEntity>>

    @Query("SELECT * FROM stations WHERE stationId = :stationId LIMIT 1")
    suspend fun getStationById(stationId: String): StationEntity?

    @Query("SELECT COUNT(*) FROM stations")
    suspend fun getStationsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStations(stations: List<StationEntity>)

    @Query("UPDATE stations SET isFavorite = :isFav WHERE stationId = :stationId")
    suspend fun updateFavoriteStatus(stationId: String, isFav: Boolean)

    @Query("UPDATE stations SET elevatorStatus = :elevatorStatus, serviceStatus = :serviceStatus WHERE stationId = :stationId")
    suspend fun updateStationOperationalStatus(
        stationId: String,
        elevatorStatus: String,
        serviceStatus: String
    )

    @Query("SELECT * FROM pending_reports WHERE stationId = :stationId AND moderationStatus != 'DESCARTADO' ORDER BY timestampMillis DESC")
    fun observeReportsForStation(stationId: String): Flow<List<PendingReportEntity>>

    @Query("SELECT * FROM pending_reports ORDER BY timestampMillis DESC")
    fun observeAllReports(): Flow<List<PendingReportEntity>>

    @Query("SELECT COUNT(*) FROM pending_reports WHERE timestampMillis > :minTimestamp")
    suspend fun getRecentReportsCount(minTimestamp: Long): Int

    @Query("SELECT * FROM pending_reports WHERE syncStatus = 'PENDIENTE_ROOM' ORDER BY timestampMillis ASC")
    suspend fun getUnsyncedReports(): List<PendingReportEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: PendingReportEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReports(reports: List<PendingReportEntity>)

    @Query("UPDATE pending_reports SET syncStatus = :syncStatus, firestoreDocId = :firestoreId WHERE reportId = :reportId")
    suspend fun markReportSynced(reportId: Long, syncStatus: String, firestoreId: String)

    @Query("UPDATE pending_reports SET moderationStatus = :status WHERE reportId = :reportId")
    suspend fun updateReportModerationStatus(reportId: Long, status: String)

    @Query("SELECT * FROM community_stops ORDER BY stopId DESC")
    fun observeAllCommunityStops(): Flow<List<CommunityStopEntity>>

    @Query("SELECT COUNT(*) FROM community_stops")
    suspend fun getCommunityStopsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommunityStops(stops: List<CommunityStopEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommunityStop(stop: CommunityStopEntity): Long

    @Query("UPDATE community_stops SET moderationStatus = :status WHERE stopId = :stopId")
    suspend fun updateCommunityStopStatus(stopId: Long, status: String)

    // ---- Activos de la red (T4.10) ----
    @Query("SELECT * FROM network_assets ORDER BY isActive DESC, name ASC")
    fun observeAllAssets(): Flow<List<NetworkAssetEntity>>

    @Query("SELECT * FROM network_assets WHERE assetId = :assetId LIMIT 1")
    suspend fun getAssetById(assetId: String): NetworkAssetEntity?

    @Query("SELECT * FROM network_assets")
    suspend fun getAllAssets(): List<NetworkAssetEntity>

    @Query("SELECT COUNT(*) FROM network_assets")
    suspend fun getAssetsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAsset(asset: NetworkAssetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAssets(assets: List<NetworkAssetEntity>)

    @Query("UPDATE network_assets SET syncedToCloud = 1, photoUrl = :photoUrl WHERE assetId = :assetId")
    suspend fun markAssetSynced(assetId: String, photoUrl: String)

    @Query("DELETE FROM network_assets WHERE assetId = :assetId")
    suspend fun deleteAsset(assetId: String)
}
