package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.dana.DanaLiveBridge
import com.example.data.local.AppDatabase
import com.example.data.model.BILLING_YEARS
import com.example.data.model.DuesStatus
import com.example.data.model.INDONESIAN_MONTHS
import com.example.data.model.INDONESIAN_MONTHS_SHORT
import com.example.data.model.MAX_BILLING_YEAR
import com.example.data.model.MIN_BILLING_YEAR
import com.example.data.model.computeMonthSummary
import com.example.data.model.currentBillingPeriodCode
import com.example.data.model.formatPeriodCode
import com.example.data.model.monthNameOnly
import com.example.data.model.parsePeriodCode
import com.example.data.remote.authStateFlow
import com.example.data.remote.signOut
import com.example.data.repository.BlokCeRepository
import com.example.data.repository.ConnectivityObserver
import com.example.ui.components.QrisZoomDialog
import com.example.ui.components.ReminderBlastDialog
import com.example.ui.screens.AccountTab
import com.example.ui.screens.AdminWargaTab
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HomeTab
import com.example.ui.screens.InfoTab
import com.example.ui.screens.PayTab
import com.example.ui.screens.QrisScannerDialog
import com.example.ui.theme.Amber600
import com.example.ui.theme.BlokCePsaTheme
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald50
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Emerald800
import com.example.ui.theme.Emerald950
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate900
import com.example.ui.theme.Teal800
import com.example.ui.viewmodel.MainTab
import com.example.ui.viewmodel.MainUiState
import com.example.ui.viewmodel.MainViewModel
import com.example.worker.MonthlyBillingScheduler
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MonthlyBillingScheduler.ensureMonthlyBillingScheduled(applicationContext)
        enableEdgeToEdge()
        setContent {
            BlokCePsaTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var skipAutoSignIn by remember { mutableStateOf(false) }
    var preferAdminPortal by remember { mutableStateOf(false) }
    var pendingAdminPortal by remember { mutableStateOf(false) }
    var pendingInviteCode by remember { mutableStateOf("") }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            skipAutoSignIn = skipAutoSignIn,
            initialAdminPortal = preferAdminPortal,
            onPrepareAuthIntent = { isAdminMode, code ->
                pendingAdminPortal = isAdminMode
                pendingInviteCode = code
            },
            onAuthSuccess = {
                skipAutoSignIn = false
                preferAdminPortal = false
            }
        )
    } else {
        key(user.uid) {
            AuthenticatedShell(
                currentUserId = user.uid,
                requestedAdminPortal = pendingAdminPortal,
                pendingAdminInviteCode = pendingInviteCode,
                onSignOutRequested = { reopenAdminTab ->
                    skipAutoSignIn = true
                    preferAdminPortal = reopenAdminTab
                    pendingAdminPortal = false
                    pendingInviteCode = ""
                }
            )
        }
    }
}

@Composable
private fun AuthenticatedShell(
    currentUserId: String,
    requestedAdminPortal: Boolean,
    pendingAdminInviteCode: String,
    onSignOutRequested: (reopenAdminTab: Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: MainViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val dao = AppDatabase.getInstance(app).blokCeDao()
                val connectivity = ConnectivityObserver(app)
                val repo = BlokCeRepository(db, dao, connectivity)
                MainViewModel(
                    repository = repo,
                    currentUserId = currentUserId,
                    requestedAdminPortal = requestedAdminPortal,
                    pendingAdminInviteCode = pendingAdminInviteCode,
                    appContext = app.applicationContext
                )
            }
        }
    )

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val toastMsg by viewModel.toastEvent.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshDanaListenerStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(toastMsg) {
        val msg = toastMsg
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeToast()
        }
    }

    val isAdmin = state.currentResident?.adminRole == true
    LaunchedEffect(isAdmin, state.activeTab) {
        if (!isAdmin && state.activeTab == MainTab.ADMIN_WARGA) {
            viewModel.selectTab(MainTab.HOME)
        }
    }

    if (state.activeTab != MainTab.HOME) {
        BackHandler {
            viewModel.selectTab(MainTab.HOME)
        }
    }

    val navItems = remember(isAdmin) {
        buildList {
            add(NavDestination(MainTab.HOME, "Beranda", Icons.Filled.Home, Icons.Outlined.Home, "tab_home"))
            add(NavDestination(MainTab.PAY, "Bayar", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "tab_pay"))
            add(NavDestination(MainTab.INFO, "Info", Icons.Filled.Newspaper, Icons.Outlined.Newspaper, "tab_info"))
            if (isAdmin) {
                add(NavDestination(MainTab.ADMIN_WARGA, "Admin", Icons.Filled.People, Icons.Outlined.People, "tab_admin"))
            }
            add(NavDestination(MainTab.ACCOUNT, "Akun", Icons.Filled.ManageAccounts, Icons.Outlined.ManageAccounts, "tab_account"))
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate50)
    ) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = Slate50,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        containerColor = Color.White,
                        tonalElevation = 8.dp
                    ) {
                        navItems.forEach { item ->
                            val selected = state.activeTab == item.tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(item.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Emerald700,
                                    selectedTextColor = Emerald700,
                                    indicatorColor = Emerald50,
                                    unselectedIconColor = Slate500,
                                    unselectedTextColor = Slate500
                                ),
                                modifier = Modifier.testTag(item.tag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = Color.White,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        navItems.forEach { item ->
                            val selected = state.activeTab == item.tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(item.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = Emerald700,
                                    selectedTextColor = Emerald700,
                                    indicatorColor = Emerald50,
                                    unselectedIconColor = Slate500,
                                    unselectedTextColor = Slate500
                                ),
                                modifier = Modifier.testTag(item.tag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (state.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(color = Emerald700)
                                Text(
                                    text = "Memuat data warga...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Slate500
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .widthIn(max = 600.dp)
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            TopBlokHeader(
                                state = state,
                                onSelectMonth = viewModel::selectMonth
                            )

                            when (state.activeTab) {
                                MainTab.HOME -> HomeTab(
                                    state = state,
                                    onNavigateTab = viewModel::selectTab,
                                    onOpenQrisScanner = viewModel::openQrisScanner
                                )
                                MainTab.PAY -> PayTab(
                                    state = state,
                                    onSelectPackage = viewModel::selectPayPackage,
                                    onCopyDana = { num ->
                                        copyToClipboard(context, "Nomor DANA", num)
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Nomor DANA $num disalin!")
                                        }
                                    },
                                    onOpenDanaApp = { num ->
                                        copyToClipboard(context, "Nomor DANA", num)
                                        val launched = DanaLiveBridge.openDanaApp(context)
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                if (launched) {
                                                    "Nomor DANA $num disalin & membuka aplikasi DANA..."
                                                } else {
                                                    "Nomor DANA $num disalin. Membuka halaman DANA di Play Store."
                                                }
                                            )
                                        }
                                    },
                                    onOpenQrisZoom = viewModel::openQrisZoom,
                                    onOpenQrisScanner = viewModel::openQrisScanner,
                                    onClearScannedQris = viewModel::clearScannedQris,
                                    onSubmitPayment = viewModel::submitPayment
                                )
                                MainTab.INFO -> InfoTab(
                                    state = state,
                                    onSaveAnnouncement = viewModel::adminSaveAnnouncement,
                                    onDeleteAnnouncement = viewModel::adminDeleteAnnouncement
                                )
                                MainTab.ADMIN_WARGA -> {
                                    if (isAdmin) {
                                        AdminWargaTab(
                                            state = state,
                                            onSetFilter = viewModel::setAdminFilter,
                                            onSetSearch = viewModel::setAdminSearch,
                                            onVerifyPayment = viewModel::adminVerifyPayment,
                                            onRejectPayment = viewModel::adminRejectPayment,
                                            onMarkPaid = viewModel::adminMarkPaid,
                                            onToggleHidmat = viewModel::adminToggleHidmat,
                                            onEditResidentProfile = viewModel::adminEditResidentProfile,
                                            onSetResidentRole = viewModel::adminSetResidentRole,
                                            onDeleteResident = viewModel::adminDeleteResident,
                                            onSaveSettings = viewModel::adminSaveSettings,
                                            onNavigateToInfo = { viewModel.selectTab(MainTab.INFO) },
                                            onPushBlast = { viewModel.adminSendPushBlast("all_unpaid") },
                                            onToggleReminder = viewModel::adminToggleReminder,
                                            onGenerateMonthlyBills = viewModel::adminGenerateMonthlyBills,
                                            onToggleDanaAutoVerify = viewModel::setDanaAutoVerifyEnabled,
                                            onVerifyLiveDanaManual = viewModel::adminVerifyLiveDanaManual,
                                            onClearDanaLiveEvents = viewModel::adminClearDanaLiveEvents,
                                            onSaveWebhookConfig = viewModel::adminSaveWebhookConfig,
                                            onTestWebhookPing = viewModel::adminTestWebhookPing,
                                            onVerifyWebhookEventManual = viewModel::adminVerifyWebhookEventManual,
                                            onDeleteWebhookEvent = viewModel::adminDeleteWebhookEvent,
                                            onBuildCsv = viewModel::buildMonthlyCsv,
                                            onBuildFundRecapCsv = viewModel::buildFundRecapCsv,
                                            onShowMessage = { msg ->
                                                scope.launch { snackbarHostState.showSnackbar(msg) }
                                            }
                                        )
                                    } else {
                                        HomeTab(
                                            state = state,
                                            onNavigateTab = viewModel::selectTab,
                                            onOpenQrisScanner = viewModel::openQrisScanner
                                        )
                                    }
                                }
                                MainTab.ACCOUNT -> AccountTab(
                                    state = state,
                                    onSaveProfile = viewModel::updateOwnProfile,
                                    onSaveSettings = viewModel::adminSaveSettings,
                                    onCreateAdminInvite = viewModel::adminCreateInvite,
                                    onDeleteAdminInvite = viewModel::adminDeleteInvite,
                                    onRevokeDelegatedAdmin = viewModel::adminRevokeDelegatedAdmin,
                                    onRedeemAdminInvite = viewModel::redeemAdminInvite,
                                    onCopyText = { label, text ->
                                        copyToClipboard(context, label, text)
                                        scope.launch {
                                            snackbarHostState.showSnackbar("$label $text disalin!")
                                        }
                                    },
                                    onFlushOfflineQueue = viewModel::flushOfflineNow,
                                    onSignOut = {
                                        onSignOutRequested(false)
                                        signOut(
                                            credentialManager = credentialManager,
                                            scope = scope,
                                            onBeforeSignOut = { viewModel.clearCachesBeforeSignOut() }
                                        )
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }

    if (state.showQrisZoom) {
        QrisZoomDialog(
            settings = state.settings,
            onCopyDana = {
                copyToClipboard(context, "Nomor DANA", state.settings.danaNumber)
                scope.launch {
                    snackbarHostState.showSnackbar("Nomor DANA disalin!")
                }
            },
            onScanQris = viewModel::openQrisScanner,
            onDismiss = viewModel::closeQrisZoom
        )
    }

    if (state.showQrisScanner) {
        QrisScannerDialog(
            settings = state.settings,
            onQrScanned = viewModel::onQrisScanned,
            onDismiss = viewModel::closeQrisScanner
        )
    }

    if (state.showAdminAccessDeniedDialog) {
        var dialogInviteCode by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = viewModel::dismissAdminAccessDeniedDialog,
            icon = {
                Icon(
                    imageVector = Icons.Filled.AdminPanelSettings,
                    contentDescription = null,
                    tint = Emerald700
                )
            },
            title = { Text("Akses Khusus Admin") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Akun ${state.currentResident?.email.orEmpty()} belum terdaftar sebagai Admin. Masuk dengan akun Google Admin utama (${BlokCeRepository.BOOTSTRAPPED_ADMIN_EMAIL}) atau masukkan Kode Undangan Admin.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = dialogInviteCode,
                        onValueChange = { dialogInviteCode = it.uppercase() },
                        label = { Text("Kode Undangan Admin") },
                        placeholder = { Text("Contoh: CE-ADM-XXXX") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_admin_invite_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (dialogInviteCode.trim().length >= 6) {
                            viewModel.redeemAdminInvite(dialogInviteCode)
                        } else {
                            viewModel.dismissAdminAccessDeniedDialog()
                            onSignOutRequested(true)
                            signOut(
                                credentialManager = credentialManager,
                                scope = scope,
                                onBeforeSignOut = { viewModel.clearCachesBeforeSignOut() }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                    modifier = Modifier.testTag("switch_admin_google_account_button")
                ) {
                    Text(if (dialogInviteCode.trim().length >= 6) "Aktifkan Kode" else "Ganti Akun Admin")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = viewModel::dismissAdminAccessDeniedDialog,
                    modifier = Modifier.testTag("continue_as_resident_button")
                ) {
                    Text("Lanjut sebagai Warga")
                }
            }
        )
    }

    if (state.showReminderDialog) {
        val resident = state.currentResident
        if (resident != null && !resident.adminRole) {
            val sum = computeMonthSummary(resident, state.userPayments, state.selectedMonth)
            val totalDue = (if (sum.iuranStatus != DuesStatus.PAID) state.settings.iuranAmount else 0L) +
                (if (sum.hidmatActive && sum.hidmatStatus != DuesStatus.PAID) state.settings.hidmatAmount else 0L)
            if (totalDue > 0L) {
                ReminderBlastDialog(
                    settings = state.settings,
                    house = resident.house,
                    selectedMonth = state.selectedMonth,
                    totalDue = totalDue,
                    onPayNow = {
                        viewModel.dismissReminderDialog()
                        viewModel.selectTab(MainTab.PAY)
                    },
                    onDismiss = viewModel::dismissReminderDialog
                )
            }
        }
    }
}

@Composable
private fun TopBlokHeader(
    state: MainUiState,
    onSelectMonth: (String) -> Unit
) {
    var showPeriodPicker by remember { mutableStateOf(false) }
    val (selectedYear, selectedMonthNum) = remember(state.selectedMonth) {
        parsePeriodCode(state.selectedMonth)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(listOf(Emerald950, Emerald800, Teal800)),
                RoundedCornerShape(26.dp)
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Home,
                                contentDescription = null,
                                tint = Emerald100,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = state.settings.blokName,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (state.isOnline) Emerald500 else Amber600)
                            )
                        }
                        Text(
                            text = state.settings.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Emerald100.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Surface(
                color = Color.Black.copy(alpha = 0.26f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showPeriodPicker = true }
                    .testTag("month_selector_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            tint = Emerald100,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Periode:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Emerald100
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = Color.White.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = monthNameOnly(selectedMonthNum),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            color = Emerald500.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = selectedYear.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = "Pilih tahun dan bulan periode",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }

    if (showPeriodPicker) {
        PeriodPickerDialog(
            initialPeriodCode = state.selectedMonth,
            onSelectPeriod = { code ->
                onSelectMonth(code)
                showPeriodPicker = false
            },
            onDismiss = { showPeriodPicker = false }
        )
    }
}

@Composable
private fun PeriodPickerDialog(
    initialPeriodCode: String,
    onSelectPeriod: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val (initYear, initMonth) = remember(initialPeriodCode) { parsePeriodCode(initialPeriodCode) }
    var activeYear by remember(initialPeriodCode) { mutableIntStateOf(initYear) }
    val yearListState = rememberLazyListState()

    LaunchedEffect(activeYear) {
        val idx = (activeYear - MIN_BILLING_YEAR).coerceIn(0, BILLING_YEARS.lastIndex)
        yearListState.animateScrollToItem(idx)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Pilih Periode (Tahun & Bulan)",
                    style = MaterialTheme.typography.titleMedium,
                    color = Slate900,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Data periode tersedia dari tahun $MIN_BILLING_YEAR sampai dengan $MAX_BILLING_YEAR.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500
                )

                // Year stepper control
                Surface(
                    color = Emerald50,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Emerald100),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (activeYear > MIN_BILLING_YEAR) activeYear-- },
                            enabled = activeYear > MIN_BILLING_YEAR,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("prev_year_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Tahun sebelumnya",
                                tint = if (activeYear > MIN_BILLING_YEAR) Emerald700 else Slate500
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "TAHUN",
                                style = MaterialTheme.typography.labelSmall,
                                color = Emerald700
                            )
                            Text(
                                text = activeYear.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                color = Slate900,
                                fontWeight = FontWeight.Black
                            )
                        }
                        IconButton(
                            onClick = { if (activeYear < MAX_BILLING_YEAR) activeYear++ },
                            enabled = activeYear < MAX_BILLING_YEAR,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("next_year_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Tahun berikutnya",
                                tint = if (activeYear < MAX_BILLING_YEAR) Emerald700 else Slate500
                            )
                        }
                    }
                }

                // Quick horizontal year chips (2026 .. 2050)
                LazyRow(
                    state = yearListState,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(BILLING_YEARS) { year ->
                        val selected = year == activeYear
                        Surface(
                            color = if (selected) Emerald700 else Slate100,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .heightIn(min = 40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { activeYear = year }
                                .testTag("year_chip_$year")
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = year.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) Color.White else Slate600,
                                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // 3x4 Month Grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (rowIdx in 0 until 4) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (colIdx in 0 until 3) {
                                val monthNum = rowIdx * 3 + colIdx + 1
                                val isSelected = activeYear == initYear && monthNum == initMonth
                                Surface(
                                    color = if (isSelected) Emerald700 else Slate50,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) Emerald700 else Slate200
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            onSelectPeriod(formatPeriodCode(activeYear, monthNum))
                                        }
                                        .testTag("month_pick_$monthNum")
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = INDONESIAN_MONTHS_SHORT[monthNum - 1],
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (isSelected) Color.White else Slate900,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Dipilih: ${INDONESIAN_MONTHS[initMonth - 1]} $initYear",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate600
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { onSelectPeriod(currentBillingPeriodCode()) },
                        modifier = Modifier.testTag("current_period_button")
                    ) {
                        Text("Bulan Ini", color = Emerald700, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Tutup", color = Slate600)
                    }
                }
            }
        }
    }
}

private data class NavDestination(
    val tab: MainTab,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}
