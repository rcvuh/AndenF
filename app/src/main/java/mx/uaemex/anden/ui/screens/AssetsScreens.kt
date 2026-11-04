package mx.uaemex.anden.ui.screens

import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import mx.uaemex.anden.R
import mx.uaemex.anden.data.local.entity.NetworkAssetEntity
import mx.uaemex.anden.domain.TeamMembers
import mx.uaemex.anden.ui.theme.AndenAmberWarning
import mx.uaemex.anden.ui.theme.AndenBurgundyPrimary
import mx.uaemex.anden.ui.theme.AndenDangerRed
import mx.uaemex.anden.ui.theme.AndenEmeraldValid
import mx.uaemex.anden.ui.theme.AndenGoldSecondary
import mx.uaemex.anden.ui.viewmodel.AndenUiState
import mx.uaemex.anden.ui.viewmodel.AssetFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val ASSET_CATEGORIES = listOf("Unidad", "Andén", "Tramo", "Estación")

/** Fotografías incluidas en la app para el catálogo inicial (res:<nombre>). */
private val BUNDLED_PHOTOS: Map<String, Int> = mapOf(
    "activo_operacion_01" to R.drawable.activo_operacion_01,
    "activo_operacion_02" to R.drawable.activo_operacion_02,
    "activo_operacion_03" to R.drawable.activo_operacion_03,
    "activo_operacion_04" to R.drawable.activo_operacion_04,
    "activo_operacion_05" to R.drawable.activo_operacion_05,
    "activo_mantenimiento_01" to R.drawable.activo_mantenimiento_01,
    "activo_mantenimiento_02" to R.drawable.activo_mantenimiento_02,
    "activo_mantenimiento_03" to R.drawable.activo_mantenimiento_03,
    "activo_mantenimiento_04" to R.drawable.activo_mantenimiento_04,
    "activo_mantenimiento_05" to R.drawable.activo_mantenimiento_05
)

/**
 * T4.10 · Activos de la red: catálogo con fotografía de unidades, andenes y tramos,
 * filtrado en Activos (en operación) e Inactivos (fuera de servicio). El
 * administrador (equipo desarrollador) da de alta, edita, cambia el estado y elimina.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsFragmentScreen(
    uiState: AndenUiState,
    allAssets: List<NetworkAssetEntity>,
    visibleAssets: List<NetworkAssetEntity>,
    stationNames: List<String>,
    onSetFilter: (String) -> Unit,
    onSaveAsset: (NetworkAssetEntity?, String, String, String, String, Boolean, Uri?, () -> Unit) -> Unit,
    onToggleActive: (NetworkAssetEntity) -> Unit,
    onDeleteAsset: (NetworkAssetEntity) -> Unit,
    onBack: () -> Unit
) {
    val isAdmin = uiState.isAdminRole
    var editorOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<NetworkAssetEntity?>(null) }
    var toDelete by remember { mutableStateOf<NetworkAssetEntity?>(null) }
    var showTeam by remember { mutableStateOf(false) }
    var showFares by remember { mutableStateOf(false) }
    val activeCount = allAssets.count { it.isActive }
    val inactiveCount = allAssets.size - activeCount

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Activos de la red", color = Color.White, fontWeight = FontWeight.Bold)
                        Text(
                            if (isAdmin) "Modo administrador · ${uiState.currentUserEmail}" else "Consulta de estado del servicio",
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showFares = true }) {
                        Icon(Icons.Default.Payments, contentDescription = "Tarifas", tint = Color.White)
                    }
                    IconButton(onClick = { showTeam = true }) {
                        Icon(Icons.Default.Groups, contentDescription = "Equipo administrador", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AndenBurgundyPrimary)
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                ExtendedFloatingActionButton(
                    onClick = { editing = null; editorOpen = true },
                    containerColor = AndenBurgundyPrimary,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Nuevo activo") }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryBox("En operación", activeCount, AndenEmeraldValid, Modifier.weight(1f))
                    SummaryBox("Fuera de servicio", inactiveCount, AndenDangerRed, Modifier.weight(1f))
                    SummaryBox("Total", allAssets.size, AndenBurgundyPrimary, Modifier.weight(1f))
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = uiState.assetFilter == AssetFilter.TODOS,
                            onClick = { onSetFilter(AssetFilter.TODOS) },
                            label = { Text("Todos (${allAssets.size})") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = uiState.assetFilter == AssetFilter.ACTIVOS,
                            onClick = { onSetFilter(AssetFilter.ACTIVOS) },
                            label = { Text("Activos ($activeCount)") },
                            leadingIcon = { Icon(Icons.Default.CheckCircle, null, tint = AndenEmeraldValid, modifier = Modifier.size(18.dp)) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = uiState.assetFilter == AssetFilter.INACTIVOS,
                            onClick = { onSetFilter(AssetFilter.INACTIVOS) },
                            label = { Text("Inactivos ($inactiveCount)") },
                            leadingIcon = { Icon(Icons.Default.Build, null, tint = AndenDangerRed, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }
            }
            if (visibleAssets.isEmpty()) {
                item {
                    Text(
                        "No hay activos en este filtro.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
            items(visibleAssets, key = { it.assetId }) { asset ->
                AssetCard(
                    asset = asset,
                    isAdmin = isAdmin,
                    onToggleActive = { onToggleActive(asset) },
                    onEdit = { editing = asset; editorOpen = true },
                    onDelete = { toDelete = asset }
                )
            }
        }
    }

    if (editorOpen) {
        AssetEditorDialog(
            existing = editing,
            stationNames = stationNames,
            onDismiss = { editorOpen = false },
            onSave = { name, category, station, description, active, photo ->
                onSaveAsset(editing, name, category, station, description, active, photo) {
                    editorOpen = false
                }
            }
        )
    }

    toDelete?.let { asset ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Eliminar activo") },
            text = { Text("¿Eliminar \"${asset.name}\" de Room y Firestore?") },
            confirmButton = {
                TextButton(onClick = { onDeleteAsset(asset); toDelete = null }) {
                    Text("Eliminar", color = AndenDangerRed)
                }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancelar") } }
        )
    }

    if (showTeam) TeamDialog(currentEmail = uiState.currentUserEmail, onDismiss = { showTeam = false })
    if (showFares) FaresDialog(onDismiss = { showFares = false })
}

@Composable
private fun SummaryBox(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.08f)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(value.toString(), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AssetCard(
    asset: NetworkAssetEntity,
    isAdmin: Boolean,
    onToggleActive: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = if (asset.isActive) AndenEmeraldValid else AndenDangerRed
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box {
            AssetPhoto(
                photoUri = asset.photoUri,
                contentDescription = "Fotografía de ${asset.name}",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            )
            Surface(
                color = statusColor,
                shape = RoundedCornerShape(bottomEnd = 14.dp),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    if (asset.isActive) "ACTIVO · EN OPERACIÓN" else "INACTIVO · MANTENIMIENTO",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    asset.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (asset.syncedToCloud) Icons.Default.CloudDone else Icons.Default.CloudUpload,
                    contentDescription = if (asset.syncedToCloud) "Respaldado en Firestore" else "Pendiente de subir",
                    tint = if (asset.syncedToCloud) AndenEmeraldValid else AndenAmberWarning,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                "${asset.category} · Estación ${asset.stationName}",
                style = MaterialTheme.typography.labelMedium,
                color = AndenBurgundyPrimary
            )
            Spacer(Modifier.height(6.dp))
            Text(asset.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "Actualizó: ${asset.updatedBy} · ${formatDate(asset.updatedAtMillis)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isAdmin) {
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = asset.isActive, onCheckedChange = { onToggleActive() })
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (asset.isActive) "Activo" else "Inactivo",
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = AndenBurgundyPrimary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = AndenDangerRed)
                    }
                }
            }
        }
    }
}

/** Muestra la foto venga de los recursos de la app (res:), del teléfono (file://) o de Storage (https://). */
@SuppressLint("DiscouragedApi")
@Composable
fun AssetPhoto(photoUri: String, contentDescription: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val model: Any? = when {
        photoUri.startsWith("res:") -> {
            val name = photoUri.removePrefix("res:")
            BUNDLED_PHOTOS[name]
                ?: context.resources.getIdentifier(name, "drawable", context.packageName).takeIf { it != 0 }
        }
        photoUri.startsWith("file://") || photoUri.startsWith("content://") -> Uri.parse(photoUri)
        photoUri.isNotBlank() -> photoUri
        else -> null
    }
    if (model == null) {
        Box(
            modifier = modifier.background(Color(0xFFE9E1E3)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = contentDescription, tint = AndenBurgundyPrimary)
        }
    } else {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier.background(Color(0xFFE9E1E3))
        )
    }
}

@Composable
private fun AssetEditorDialog(
    existing: NetworkAssetEntity?,
    stationNames: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Boolean, Uri?) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var category by remember { mutableStateOf(existing?.category ?: ASSET_CATEGORIES.first()) }
    var station by remember { mutableStateOf(existing?.stationName ?: stationNames.firstOrNull().orEmpty()) }
    var description by remember { mutableStateOf(existing?.description.orEmpty()) }
    var active by remember { mutableStateOf(existing?.isActive ?: true) }
    var pickedPhoto by remember { mutableStateOf<Uri?>(null) }
    var stationMenu by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) pickedPhoto = uri
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 720.dp)
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    if (existing == null) "Alta de activo" else "Editar activo",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                ) {
                    val preview = pickedPhoto?.toString() ?: existing?.photoUri.orEmpty()
                    AssetPhoto(preview, "Fotografía del activo", Modifier.fillMaxSize())
                    Surface(
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(topStart = 12.dp),
                        modifier = Modifier.align(Alignment.BottomEnd)
                    ) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PhotoLibrary, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (preview.isBlank()) "Elegir foto" else "Cambiar foto", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre (ej. Unidad 08 · Andén 2)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Tipo de activo", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ASSET_CATEGORIES) { option ->
                        FilterChip(
                            selected = category == option,
                            onClick = { category = option },
                            label = { Text(option) }
                        )
                    }
                }
                Box {
                    OutlinedButton(onClick = { stationMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Estación: ${station.ifBlank { "Elegir" }}")
                    }
                    DropdownMenu(expanded = stationMenu, onDismissRequest = { stationMenu = false }) {
                        stationNames.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = { station = option; stationMenu = false }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción del estado") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = active, onCheckedChange = { active = it })
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (active) "Activo · En operación" else "Inactivo · Fuera de servicio",
                        fontWeight = FontWeight.Bold,
                        color = if (active) AndenEmeraldValid else AndenDangerRed
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(name, category, station, description, active, pickedPhoto) },
                        colors = ButtonDefaults.buttonColors(containerColor = AndenBurgundyPrimary)
                    ) {
                        Text(if (existing == null) "Dar de alta" else "Guardar cambios")
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamDialog(currentEmail: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.VerifiedUser, null, tint = AndenGoldSecondary) },
        title = { Text("Equipo desarrollador · Administradores") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TeamMembers.members.forEach { member ->
                    val isMe = member.email.equals(currentEmail.trim(), ignoreCase = true)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AndenBurgundyPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(member.fullName.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                member.fullName + if (isMe) " (tú)" else "",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "${member.studentId} · ${member.roleInProject}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(member.email, style = MaterialTheme.typography.labelSmall)
                        }
                        AssistChip(onClick = {}, label = { Text("Admin", fontSize = 11.sp) })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

@Composable
private fun FaresDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Payments, null, tint = AndenBurgundyPrimary) },
        title = { Text("Tarifas · Tren El Insurgente") },
        text = {
            Column {
                Image(
                    painter = painterResource(R.drawable.tarifas_insurgente),
                    contentDescription = "Tabla de tarifas por interestación en pesos",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tarifa por interestación en pesos M.N. (de $15 a $100 según origen y destino).",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

private fun formatDate(millis: Long): String =
    if (millis <= 0L) "—" else SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "MX")).format(Date(millis))
