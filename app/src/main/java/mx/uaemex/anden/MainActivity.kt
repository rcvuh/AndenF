package mx.uaemex.anden

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import mx.uaemex.anden.ui.screens.*
import mx.uaemex.anden.ui.theme.AndenTheme
import mx.uaemex.anden.ui.viewmodel.ReportViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val reportViewModel: ReportViewModel = viewModel()
            val uiState by reportViewModel.uiState.collectAsStateWithLifecycle()
            AndenTheme(highContrastMode = uiState.highContrastAccessibleMode) {
                AndenNavigationHost(reportViewModel = reportViewModel)
            }
        }
    }
}

@Composable
fun AndenNavigationHost(reportViewModel: ReportViewModel) {
    val navController = rememberNavController()
    val uiState by reportViewModel.uiState.collectAsStateWithLifecycle()
    val stations by reportViewModel.filteredStations.collectAsStateWithLifecycle()
    val allStations by reportViewModel.stations.collectAsStateWithLifecycle()
    val allReports by reportViewModel.allReports.collectAsStateWithLifecycle()
    val communityStops by reportViewModel.communityStops.collectAsStateWithLifecycle()
    val allAssets by reportViewModel.assets.collectAsStateWithLifecycle()
    val visibleAssets by reportViewModel.filteredAssets.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            reportViewModel.clearSnackbar()
        }
    }

    val currentStation = allStations.find { it.stationId == uiState.selectedStationId }
        ?: allStations.firstOrNull()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = "LoginFragment"
            ) {
                composable("LoginFragment") {
                    LoginFragmentScreen(
                        onLoginCitizen = { email, pwd ->
                            reportViewModel.loginAsCitizen(email, pwd) {
                                navController.navigate("MapHomeFragment") {
                                    popUpTo("LoginFragment") { inclusive = true }
                                }
                            }
                        },
                        onLoginGuest = {
                            reportViewModel.loginAsGuest {
                                navController.navigate("MapHomeFragment") {
                                    popUpTo("LoginFragment") { inclusive = true }
                                }
                            }
                        },
                        onNavigateAdminLogin = {
                            navController.navigate("AdminLoginFragment")
                        }
                    )
                }

                composable("AdminLoginFragment") {
                    AdminLoginFragmentScreen(
                        onAdminAuthenticated = { email, pwd ->
                            reportViewModel.loginAsAdmin(email, pwd) {
                                navController.navigate("AdminDashboardFragment") {
                                    popUpTo("LoginFragment") { inclusive = true }
                                }
                            }
                        },
                        onBackToUserLogin = {
                            navController.popBackStack()
                        }
                    )
                }

                composable("MapHomeFragment") {
                    MapHomeFragmentScreen(
                        uiState = uiState,
                        stations = stations,
                        allReports = allReports,
                        onSelectStation = { st -> reportViewModel.selectStation(st.stationId) },
                        onOpenStationDetail = { st ->
                            reportViewModel.selectStation(st.stationId)
                            navController.navigate("StationDetailFragment")
                        },
                        onOpenCreateReport = {
                            navController.navigate("CreateReportFragment")
                        },
                        onOpenAdminPanel = {
                            navController.navigate("AdminDashboardFragment")
                        },
                        onOpenSecurityAccessibility = {
                            navController.navigate("SecurityAccessibilityFragment")
                        },
                        onOpenAssets = {
                            navController.navigate("AssetsFragment")
                        },
                        onSearchChange = reportViewModel::updateSearchQuery,
                        onToggleAccessibleFilter = reportViewModel::toggleAccessibleFilter,
                        onToggleFavoritesFilter = reportViewModel::toggleFavoritesFilter,
                        onToggleOfflineMode = reportViewModel::toggleOfflineMode,
                        onRefreshGps = { reportViewModel.refreshGpsLocation() },
                        onLogout = {
                            reportViewModel.logout()
                            navController.navigate("LoginFragment") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

                composable("StationDetailFragment") {
                    if (currentStation != null) {
                        val activeReports = reportViewModel.getActiveReportsForStation(
                            stationId = currentStation.stationId,
                            reports = allReports
                        )
                        StationDetailFragmentScreen(
                            station = currentStation,
                            allStations = allStations,
                            allReports = allReports,
                            activeReports = activeReports,
                            communityStops = communityStops,
                            onSelectStation = { st -> reportViewModel.selectStation(st.stationId) },
                            onToggleFavorite = { reportViewModel.toggleFavoriteStation(currentStation) },
                            onProposeCommunityStop = { title, ref ->
                                reportViewModel.proposeNewCommunityStop(title, ref)
                            },
                            onNavigateCreateReport = { navController.navigate("CreateReportFragment") },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                composable("CreateReportFragment") {
                    if (currentStation != null) {
                        CreateReportFragmentScreen(
                            station = currentStation,
                            allStations = allStations,
                            uiState = uiState,
                            onSelectStation = { st -> reportViewModel.selectStation(st.stationId) },
                            onOpenCameraCapture = { navController.navigate("CameraCaptureFragment") },
                            onSimulateDistanceChange = { dist -> reportViewModel.refreshGpsLocation(dist) },
                            onToggleOfflineMode = reportViewModel::toggleOfflineMode,
                            onSubmitReport = { category, desc ->
                                reportViewModel.submitCitizenReport(category, desc) {
                                    navController.popBackStack()
                                }
                            },
                            onCancel = { navController.popBackStack() }
                        )
                    }
                }

                composable("CameraCaptureFragment") {
                    CameraCaptureFragmentScreen(
                        uiState = uiState,
                        onMediaCaptured = { uriStr, isVideo ->
                            reportViewModel.onMediaCaptured(uriStr, isVideo)
                            navController.navigate("MediaPreviewFragment")
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable("MediaPreviewFragment") {
                    MediaPreviewFragmentScreen(
                        uiState = uiState,
                        onRetryCapture = {
                            reportViewModel.clearCapturedMedia()
                            navController.popBackStack()
                        },
                        onConfirmEvidence = {
                            navController.popBackStack("CreateReportFragment", inclusive = false)
                        }
                    )
                }

                composable("AdminDashboardFragment") {
                    AdminDashboardFragmentScreen(
                        uiState = uiState,
                        allStations = allStations,
                        communityStops = communityStops,
                        reports = allReports,
                        onUpdateStationStatus = reportViewModel::updateStationStatusByAdmin,
                        onModerateStop = reportViewModel::moderateCommunityStop,
                        onModerateReport = reportViewModel::moderateReport,
                        onOpenAssets = {
                            navController.navigate("AssetsFragment")
                        },
                        onBackToMap = {
                            navController.navigate("MapHomeFragment")
                        }
                    )
                }

                composable("AssetsFragment") {
                    AssetsFragmentScreen(
                        uiState = uiState,
                        allAssets = allAssets,
                        visibleAssets = visibleAssets,
                        stationNames = allStations.map { it.name },
                        onSetFilter = reportViewModel::setAssetFilter,
                        onSaveAsset = { existing, name, category, station, description, active, photo, onDone ->
                            reportViewModel.saveAsset(existing, name, category, station, description, active, photo, onDone)
                        },
                        onToggleActive = reportViewModel::toggleAssetActive,
                        onDeleteAsset = reportViewModel::deleteAsset,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable("SecurityAccessibilityFragment") {
                    SecurityAccessibilityFragmentScreen(
                        uiState = uiState,
                        selectedStation = currentStation,
                        onToggleHighContrast = reportViewModel::toggleHighContrastMode,
                        onToggleAnonymousPrivacy = reportViewModel::toggleAnonymousPrivacyMode,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
