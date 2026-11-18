package com.example

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
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
import com.example.data.model.formatRupiah
import com.example.data.model.monthLabel
import com.example.data.model.monthNameOnly
import com.example.data.model.parsePeriodCode
import com.example.data.remote.authStateFlow
import com.example.data.remote.signOut
import com.example.data.repository.BlokCeRepository
import com.example.data.repository.ConnectivityObserver
import com.example.notification.HimbauanNotificationHelper
import com.example.security.BiometricAuthHelper
import com.example.security.BiometricAvailability
import com.example.security.BiometricSecurityState
import com.example.ui.components.AppLaunchSplashOverlay
import com.example.ui.components.BiometricLockOverlay
import com.example.ui.components.EmphasizedDecelerate
import com.example.ui.components.LivePulseDot
import com.example.ui.components.QrisZoomDialog
import com.example.ui.components.ReminderBlastDialog
import com.example.ui.components.StaggeredEntrance
import com.example.ui.components.directionalTabTransition
import com.example.ui.components.portalSwitchTransition
import com.example.ui.screens.AccountTab
import com.example.ui.screens.AdminWargaTab
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HomeTab
import com.example.ui.screens.InfoTab
import com.example.ui.screens.KeuanganTab
import com.example.ui.screens.PayTab
import com.example.ui.screens.QrisScannerDialog
import com.example.ui.screens.ReceiptScannerDialog
import com.example.ui.theme.Amber600
import com.example.ui.theme.BlokCePsaTheme
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Emerald800
import com.example.ui.theme.Emerald950
import com.example.ui.theme.Teal800
import com.example.ui.theme.adaptiveColors
import com.example.ui.theme.adaptiveFieldColors
import com.example.ui.viewmodel.MainTab
import com.example.ui.viewmodel.MainUiState
import com.example.notification.evaluateResidentPendingNotification
import com.example.ui.viewmodel.MainViewModel
import com.example.worker.MonthlyBillingScheduler
import com.example.worker.PendingPaymentNotificationScheduler
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private var notifOpenTabState by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MonthlyBillingScheduler.ensureMonthlyBillingScheduled(applicationContext)
        PendingPaymentNotificationScheduler.ensureScheduled(applicationContext)
        HimbauanNotificationHelper.ensureChannel(applicationContext)
        notifOpenTabState = intent?.getStringExtra(HimbauanNotificationHelper.EXTRA_OPEN_TAB)
        enableEdgeToEdge()
        setContent {
            BlokCePsaTheme {
                AppNavigation(
                    pendingNotificationTab = notifOpenTabState,
                    onNotificationTabConsumed = { notifOpenTabState = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notifOpenTabState = intent.getStringExtra(HimbauanNotificationHelper.EXTRA_OPEN_TAB)
    }
}

@Composable
fun AppNavigation(
    auth: FirebaseAuth = Firebase.auth,
    pendingNotificationTab: String? = null,
    onNotificationTabConsumed: () -> Unit = {}
) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var skipAutoSignIn by remember { mutableStateOf(false) }
    var preferAdminPortal by remember { mutableStateOf(false) }
    var pendingAdminPortal by remember { mutableStateOf(false) }
    var pendingInviteCode by remember { mutableStateOf("") }

    var showLaunchSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(780L)
        showLaunchSplash = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentUser?.uid,
            transitionSpec = { portalSwitchTransition() },
            label = "auth_portal_transition"
        ) { uid ->
            if (uid == null) {
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
                key(uid) {
                    AuthenticatedShell(
                        currentUserId = uid,
                        requestedAdminPortal = pendingAdminPortal,
                        pendingAdminInviteCode = pendingInviteCode,
                        pendingNotificationTab = pendingNotificationTab,
                        onNotificationTabConsumed = onNotificationTabConsumed,
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

        AppLaunchSplashOverlay(visible = showLaunchSplash)
    }
}

@Composable
private fun AuthenticatedShell(
    currentUserId: String,
    requestedAdminPortal: Boolean,
    pendingAdminInviteCode: String,
    pendingNotificationTab: String? = null,
    onNotificationTabConsumed: () -> Unit = {},
    onSignOutRequested: (reopenAdminTab: Boolean) -> Unit
) {
    val context = LocalContext.current
    val ac = adaptiveColors
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

    var biometricState by remember(currentUserId) {
        val avail = BiometricAuthHelper.checkAvailability(context)
        val portalEnabled = BiometricAuthHelper.isPortalLockEnabled(context, currentUserId)
        val payEnabled = BiometricAuthHelper.isPaymentBiometricEnabled(context, currentUserId)
        val promoDismissed = BiometricAuthHelper.isHomePromoDismissed(context, currentUserId)
        mutableStateOf(
            BiometricSecurityState(
                availability = avail,
                portalLockEnabled = portalEnabled,
                requireOnPayment = payEnabled,
                isPortalLocked = portalEnabled && avail == BiometricAvailability.AVAILABLE,
                homePromoDismissed = promoDismissed
            )
        )
    }
    var isBiometricPromptActive by remember { mutableStateOf(false) }
    var lastBackgroundElapsedMs by remember { mutableStateOf(0L) }

    fun triggerPortalUnlockPrompt() {
        if (isBiometricPromptActive) return
        val avail = BiometricAuthHelper.checkAvailability(context)
        if (avail != BiometricAvailability.AVAILABLE) {
            biometricState = biometricState.copy(
                availability = avail,
                isPortalLocked = false,
                lastErrorMessage = null
            )
            return
        }
        BiometricAuthHelper.authenticate(
            context = context,
            title = context.getString(R.string.biometric_portal_title),
            subtitle = context.getString(R.string.biometric_portal_subtitle),
            negativeButtonText = context.getString(R.string.biometric_negative_cancel),
            onPromptShown = { isBiometricPromptActive = true },
            onPromptFinished = { isBiometricPromptActive = false },
            onSuccess = {
                biometricState = biometricState.copy(
                    isPortalLocked = false,
                    lastErrorMessage = null
                )
            },
            onError = { _, msg ->
                biometricState = biometricState.copy(lastErrorMessage = msg)
            }
        )
    }

    fun handleTogglePortalBiometric(enable: Boolean) {
        val avail = BiometricAuthHelper.checkAvailability(context)
        biometricState = biometricState.copy(availability = avail)
        if (enable) {
            if (avail == BiometricAvailability.NONE_ENROLLED) {
                BiometricAuthHelper.openBiometricEnrollSettings(context)
                scope.launch {
                    snackbarHostState.showSnackbar("Daftarkan sidik jari atau wajah di Setelan perangkat terlebih dahulu.")
                }
                return
            }
            if (avail != BiometricAvailability.AVAILABLE) {
                scope.launch {
                    snackbarHostState.showSnackbar("Sensor biometrik tidak tersedia di perangkat ini.")
                }
                return
            }
            BiometricAuthHelper.authenticate(
                context = context,
                title = context.getString(R.string.biometric_toggle_title),
                subtitle = "Aktifkan Kunci Portal Warga Blok CE PSA",
                negativeButtonText = context.getString(R.string.biometric_negative_cancel),
                onPromptShown = { isBiometricPromptActive = true },
                onPromptFinished = { isBiometricPromptActive = false },
                onSuccess = {
                    BiometricAuthHelper.setPortalLockEnabled(context, currentUserId, true)
                    biometricState = biometricState.copy(
                        portalLockEnabled = true,
                        homePromoDismissed = true,
                        lastErrorMessage = null
                    )
                    scope.launch {
                        snackbarHostState.showSnackbar("Kunci biometrik Portal Warga diaktifkan!")
                    }
                },
                onError = { _, msg ->
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                }
            )
        } else {
            if (avail == BiometricAvailability.AVAILABLE) {
                BiometricAuthHelper.authenticate(
                    context = context,
                    title = context.getString(R.string.biometric_toggle_title),
                    subtitle = "Konfirmasi untuk menonaktifkan Kunci Portal Warga",
                    negativeButtonText = context.getString(R.string.biometric_negative_cancel),
                    onPromptShown = { isBiometricPromptActive = true },
                    onPromptFinished = { isBiometricPromptActive = false },
                    onSuccess = {
                        BiometricAuthHelper.setPortalLockEnabled(context, currentUserId, false)
                        biometricState = biometricState.copy(
                            portalLockEnabled = false,
                            isPortalLocked = false,
                            lastErrorMessage = null
                        )
                        scope.launch {
                            snackbarHostState.showSnackbar("Kunci biometrik Portal Warga dinonaktifkan.")
                        }
                    },
                    onError = { _, msg ->
                        scope.launch { snackbarHostState.showSnackbar(msg) }
                    }
                )
            } else {
                BiometricAuthHelper.setPortalLockEnabled(context, currentUserId, false)
                biometricState = biometricState.copy(
                    portalLockEnabled = false,
                    isPortalLocked = false,
                    lastErrorMessage = null
                )
            }
        }
    }

    fun handleTogglePaymentBiometric(enable: Boolean) {
        val avail = BiometricAuthHelper.checkAvailability(context)
        biometricState = biometricState.copy(availability = avail)
        if (enable) {
            if (avail == BiometricAvailability.NONE_ENROLLED) {
                BiometricAuthHelper.openBiometricEnrollSettings(context)
                return
            }
            if (avail != BiometricAvailability.AVAILABLE) {
                scope.launch {
                    snackbarHostState.showSnackbar("Sensor biometrik tidak tersedia di perangkat ini.")
                }
                return
            }
            BiometricAuthHelper.authenticate(
                context = context,
                title = context.getString(R.string.biometric_toggle_title),
                subtitle = "Aktifkan verifikasi biometrik saat pembayaran",
                negativeButtonText = context.getString(R.string.biometric_negative_cancel),
                onPromptShown = { isBiometricPromptActive = true },
                onPromptFinished = { isBiometricPromptActive = false },
                onSuccess = {
                    BiometricAuthHelper.setPaymentBiometricEnabled(context, currentUserId, true)
                    biometricState = biometricState.copy(requireOnPayment = true)
                    scope.launch {
                        snackbarHostState.showSnackbar("Verifikasi biometrik pembayaran diaktifkan!")
                    }
                },
                onError = { _, msg ->
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                }
            )
        } else {
            BiometricAuthHelper.setPaymentBiometricEnabled(context, currentUserId, false)
            biometricState = biometricState.copy(requireOnPayment = false)
            scope.launch {
                snackbarHostState.showSnackbar("Verifikasi biometrik pembayaran dimatikan.")
            }
        }
    }

    LaunchedEffect(biometricState.isPortalLocked) {
        if (biometricState.isPortalLocked && !isBiometricPromptActive) {
            triggerPortalUnlockPrompt()
        }
    }

    var notifPermissionGranted by remember(currentUserId) {
        mutableStateOf(HimbauanNotificationHelper.hasNotificationPermission(context))
    }
    val notifPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifPermissionGranted = granted
    }
    LaunchedEffect(currentUserId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !HimbauanNotificationHelper.hasNotificationPermission(context)
        ) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(pendingNotificationTab, state.currentResident) {
        if (!pendingNotificationTab.isNullOrBlank() && state.currentResident != null) {
            when (pendingNotificationTab.uppercase()) {
                "PAY" -> viewModel.selectTab(MainTab.PAY)
                "INFO" -> viewModel.selectTab(MainTab.INFO)
                "FINANCE" -> viewModel.selectTab(MainTab.FINANCE)
                "ACCOUNT" -> viewModel.selectTab(MainTab.ACCOUNT)
                "ADMIN_WARGA" -> if (state.currentResident?.adminRole == true) viewModel.selectTab(MainTab.ADMIN_WARGA)
            }
            onNotificationTabConsumed()
        }
    }

    LaunchedEffect(
        notifPermissionGranted,
        state.settings.pushBlastId,
        state.settings.reminderEnabled,
        state.selectedMonth,
        state.currentResident,
        state.userPayments
    ) {
        val resident = state.currentResident ?: return@LaunchedEffect
        if (resident.adminRole) return@LaunchedEffect
        val target = state.settings.pushTargetUserId.ifBlank { "all_unpaid" }
        val activeMonth = currentBillingPeriodCode()
        if (target == "all_pending") {
            val dayKey = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            val pendingPayload = evaluateResidentPendingNotification(
                resident = resident,
                monthPayments = state.userPayments,
                currentMonth = activeMonth,
                dayKey = "${dayKey}_${state.settings.pushBlastId}",
                settings = state.settings
            )
            if (pendingPayload != null) {
                HimbauanNotificationHelper.dispatchPendingStatusNotification(
                    context = context,
                    alertKey = pendingPayload.alertKey,
                    title = pendingPayload.title,
                    message = pendingPayload.message,
                    targetTabName = pendingPayload.targetTabName
                )
            }
            return@LaunchedEffect
        }
        val sum = computeMonthSummary(resident, state.userPayments, state.selectedMonth)
        val totalDue = (if (sum.iuranStatus != DuesStatus.PAID) state.settings.iuranAmount else 0L) +
            (if (sum.hidmatActive && sum.hidmatStatus != DuesStatus.PAID) state.settings.hidmatAmount else 0L)
        val targetMatch = target.isBlank() ||
            target == "all_unpaid" ||
            target == resident.id
        if (totalDue > 0L && (state.settings.pushBlastId > 0L || state.settings.reminderEnabled) && targetMatch) {
            val alertKey = "himbauan_${state.settings.pushBlastId}_${state.selectedMonth}_${resident.id}"
            val notifTitle = state.settings.reminderTitle.ifBlank {
                "Himbauan Iuran ${monthLabel(state.selectedMonth)}"
            }
            val notifBody = "Tagihan ${resident.house} sebesar ${formatRupiah(totalDue)} menunggu pembayaran. ${state.settings.reminderMessage}"
            HimbauanNotificationHelper.dispatchHimbauanIfNew(
                context = context,
                alertKey = alertKey,
                title = notifTitle,
                message = notifBody,
                targetTabName = "PAY"
            )
        }
    }

    LaunchedEffect(notifPermissionGranted, state.announcements, state.currentResident) {
        if (state.currentResident == null) return@LaunchedEffect
        val urgent = state.announcements.firstOrNull { it.urgent && it.published } ?: return@LaunchedEffect
        HimbauanNotificationHelper.dispatchHimbauanIfNew(
            context = context,
            alertKey = "urgent_${urgent.id}",
            title = "Info Penting: ${urgent.title}",
            message = urgent.content,
            targetTabName = "INFO"
        )
    }

    DisposableEffect(lifecycleOwner, currentUserId) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    viewModel.setPresenceOnline(false)
                    if (!isBiometricPromptActive) {
                        lastBackgroundElapsedMs = SystemClock.elapsedRealtime()
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.refreshDanaListenerStatus()
                    viewModel.setPresenceOnline(true)
                    notifPermissionGranted = HimbauanNotificationHelper.hasNotificationPermission(context)
                    val avail = BiometricAuthHelper.checkAvailability(context)
                    val shouldRelock = biometricState.portalLockEnabled &&
                        avail == BiometricAvailability.AVAILABLE &&
                        BiometricAuthHelper.shouldRelockAfterBackground(
                            lastStoppedElapsedMs = lastBackgroundElapsedMs,
                            nowElapsedMs = SystemClock.elapsedRealtime()
                        )
                    lastBackgroundElapsedMs = 0L
                    biometricState = biometricState.copy(
                        availability = avail,
                        isPortalLocked = (biometricState.isPortalLocked || shouldRelock) &&
                            avail == BiometricAvailability.AVAILABLE
                    )
                }
                else -> Unit
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
            add(NavDestination(MainTab.PAY, "Bayar", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong, "tab_pay"))
            add(NavDestination(MainTab.FINANCE, "Keuangan", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "tab_finance"))
            add(NavDestination(MainTab.INFO, "Info", Icons.Filled.Newspaper, Icons.Outlined.Newspaper, "tab_info"))
            if (isAdmin) {
                add(NavDestination(MainTab.ADMIN_WARGA, "Admin", Icons.Filled.People, Icons.Outlined.People, "tab_admin"))
            }
            add(NavDestination(MainTab.ACCOUNT, "Akun", Icons.Filled.ManageAccounts, Icons.Outlined.ManageAccounts, "tab_account"))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        containerColor = ac.cardContainer,
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
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ac.emeraldText,
                                    selectedTextColor = ac.emeraldText,
                                    indicatorColor = ac.emeraldSurface,
                                    unselectedIconColor = ac.textMuted,
                                    unselectedTextColor = ac.textMuted
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
                        containerColor = ac.cardContainer,
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
                                    selectedIconColor = ac.emeraldText,
                                    selectedTextColor = ac.emeraldText,
                                    indicatorColor = ac.emeraldSurface,
                                    unselectedIconColor = ac.textMuted,
                                    unselectedTextColor = ac.textMuted
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
                    AnimatedContent(
                        targetState = state.isLoading,
                        transitionSpec = { portalSwitchTransition() },
                        label = "portal_loading_transition"
                    ) { loading ->
                    if (loading) {
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
                                    color = ac.textSecondary
                                )
                            }
                        }
                    } else {
                        val tabScrollState = rememberScrollState()
                        LaunchedEffect(state.activeTab) {
                            tabScrollState.scrollTo(0)
                        }
                        Column(
                            modifier = Modifier
                                .widthIn(max = 600.dp)
                                .fillMaxSize()
                        ) {
                            StaggeredEntrance(delayMillis = 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 8.dp)
                                        .testTag("pinned_blok_header_container")
                                ) {
                                    TopBlokHeader(
                                        state = state,
                                        onSelectMonth = viewModel::selectMonth
                                    )
                                }
                            }

                            HorizontalDivider(
                                color = ac.borderSubtle.copy(alpha = 0.65f),
                                thickness = 1.dp
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(tabScrollState)
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                AnimatedContent(
                                targetState = state.activeTab,
                                transitionSpec = {
                                    directionalTabTransition(
                                        forward = targetState.ordinal >= initialState.ordinal
                                    )
                                },
                                label = "main_tab_transition"
                            ) { activeTab ->
                                when (activeTab) {
                                    MainTab.HOME -> HomeTab(
                                        state = state,
                                        biometricState = biometricState,
                                        onEnableBiometricClick = { handleTogglePortalBiometric(true) },
                                        onDismissBiometricPromo = {
                                            BiometricAuthHelper.setHomePromoDismissed(context, currentUserId, true)
                                            biometricState = biometricState.copy(homePromoDismissed = true)
                                        },
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
                                        onOpenReceiptScanner = viewModel::openReceiptScanner,
                                        onClearScannedReceipt = viewModel::clearScannedReceipt,
                                        requireBiometricConfirm = biometricState.requireOnPayment &&
                                            biometricState.availability == BiometricAvailability.AVAILABLE,
                                        onRequestBiometricForPayment = { amount, onVerified ->
                                            BiometricAuthHelper.authenticate(
                                                context = context,
                                                title = context.getString(R.string.biometric_payment_title),
                                                subtitle = "Konfirmasi pembayaran ${formatRupiah(amount)}",
                                                negativeButtonText = context.getString(R.string.biometric_negative_cancel),
                                                onPromptShown = { isBiometricPromptActive = true },
                                                onPromptFinished = { isBiometricPromptActive = false },
                                                onSuccess = onVerified,
                                                onError = { _, msg ->
                                                    scope.launch { snackbarHostState.showSnackbar(msg) }
                                                }
                                            )
                                        },
                                        onSubmitPayment = viewModel::submitPayment
                                    )
                                    MainTab.FINANCE -> KeuanganTab(
                                        state = state,
                                        onSaveFinanceEntry = viewModel::adminSaveFinanceEntry,
                                        onDeleteFinanceEntry = viewModel::adminDeleteFinanceEntry,
                                        onSaveBudgetPlan = viewModel::adminSaveBudgetPlan,
                                        onDeleteBudgetPlan = viewModel::adminDeleteBudgetPlan,
                                        onExportCompleted = { formatLabel, msg ->
                                            viewModel.logFinanceExport(formatLabel)
                                            scope.launch { snackbarHostState.showSnackbar(msg) }
                                        }
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
                                                onTogglePendingNotifier = viewModel::adminTogglePendingNotifierService,
                                                onTriggerPendingNotifications = viewModel::adminTriggerPendingPushNotificationNow,
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
                                        biometricState = biometricState,
                                        onTogglePortalBiometric = ::handleTogglePortalBiometric,
                                        onTogglePaymentBiometric = ::handleTogglePaymentBiometric,
                                        onLockPortalNow = {
                                            biometricState = biometricState.copy(
                                                isPortalLocked = true,
                                                lastErrorMessage = null
                                            )
                                        },
                                        onOpenBiometricEnroll = {
                                            BiometricAuthHelper.openBiometricEnrollSettings(context)
                                        },
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
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        }
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

    if (state.showReceiptScanner) {
        ReceiptScannerDialog(
            onReceiptScanned = viewModel::onReceiptScanned,
            onDismiss = viewModel::closeReceiptScanner
        )
    }

    if (state.showAdminAccessDeniedDialog) {
        var dialogInviteCode by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = viewModel::dismissAdminAccessDeniedDialog,
            containerColor = ac.cardContainer,
            titleContentColor = ac.textPrimary,
            textContentColor = ac.textPrimary,
            icon = {
                Icon(
                    imageVector = Icons.Filled.AdminPanelSettings,
                    contentDescription = null,
                    tint = ac.emeraldText
                )
            },
            title = { Text("Akses Khusus Admin") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Akun ${state.currentResident?.email.orEmpty()} belum terdaftar sebagai Admin. Masuk dengan akun Google Admin utama (${BlokCeRepository.BOOTSTRAPPED_ADMIN_EMAIL}) atau masukkan Kode Undangan Admin.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ac.textSecondary
                    )
                    OutlinedTextField(
                        value = dialogInviteCode,
                        onValueChange = { dialogInviteCode = it.uppercase() },
                        label = { Text("Kode Undangan Admin") },
                        placeholder = { Text("Contoh: CE-ADM-XXXX") },
                        singleLine = true,
                        colors = adaptiveFieldColors(),
                        shape = RoundedCornerShape(14.dp),
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Emerald700,
                        contentColor = Color.White
                    ),
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
                    Text("Lanjut sebagai Warga", color = ac.textSecondary)
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

    AnimatedVisibility(
        visible = biometricState.isPortalLocked,
        enter = fadeIn(tween(240, easing = EmphasizedDecelerate)) +
            scaleIn(initialScale = 0.95f, animationSpec = tween(260, easing = EmphasizedDecelerate)),
        exit = fadeOut(tween(200)) + scaleOut(targetScale = 1.03f, animationSpec = tween(200))
    ) {
        BiometricLockOverlay(
            resident = state.currentResident,
            blokName = state.settings.blokName,
            availability = biometricState.availability,
            errorMessage = biometricState.lastErrorMessage,
            onUnlockClick = ::triggerPortalUnlockPrompt,
            onOpenSettings = { BiometricAuthHelper.openBiometricEnrollSettings(context) },
            onSignOut = {
                biometricState = biometricState.copy(isPortalLocked = false)
                onSignOutRequested(false)
                signOut(
                    credentialManager = credentialManager,
                    scope = scope,
                    onBeforeSignOut = { viewModel.clearCachesBeforeSignOut() }
                )
            }
        )
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
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(listOf(Emerald950, Emerald800, Teal800))
            )
            .drawBehind {
                drawCircle(
                    color = Emerald500.copy(alpha = 0.22f),
                    radius = size.maxDimension * 0.42f,
                    center = Offset(size.width * 0.94f, size.height * 0.1f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.06f),
                    radius = size.maxDimension * 0.26f,
                    center = Offset(size.width * 0.1f, size.height * 0.95f)
                )
            }
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
                        color = Color(0xFF09523E),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_art),
                            contentDescription = "Logo Blok CE PSA",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
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
                            LivePulseDot(
                                color = if (state.isOnline) Emerald500 else Amber600,
                                size = 6.dp
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
    val ac = adaptiveColors
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
            colors = CardDefaults.cardColors(containerColor = ac.cardContainer),
            border = BorderStroke(1.dp, ac.borderSubtle),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Pilih Periode (Tahun & Bulan)",
                    style = MaterialTheme.typography.titleMedium,
                    color = ac.textPrimary,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Data periode tersedia dari tahun $MIN_BILLING_YEAR sampai dengan $MAX_BILLING_YEAR.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ac.textMuted
                )

                // Year stepper control
                Surface(
                    color = ac.emeraldSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, ac.emeraldBorder),
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
                                tint = if (activeYear > MIN_BILLING_YEAR) ac.emeraldText else ac.textMuted
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "TAHUN",
                                style = MaterialTheme.typography.labelSmall,
                                color = ac.emeraldText
                            )
                            Text(
                                text = activeYear.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                color = ac.textPrimary,
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
                                tint = if (activeYear < MAX_BILLING_YEAR) ac.emeraldText else ac.textMuted
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
                            color = if (selected) Emerald700 else ac.subtleContainer,
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
                                    color = if (selected) Color.White else ac.textSecondary,
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
                                    color = if (isSelected) Emerald700 else ac.cardElevated,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) Emerald700 else ac.borderSubtle
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
                                            color = if (isSelected) Color.White else ac.textPrimary,
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
                    color = ac.textSecondary
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
                        Text("Bulan Ini", color = ac.emeraldText, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Tutup", color = ac.textSecondary)
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
