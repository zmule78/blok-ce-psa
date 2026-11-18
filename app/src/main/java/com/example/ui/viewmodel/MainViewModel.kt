package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dana.DanaLiveBridge
import com.example.dana.DanaWebhookDispatcher
import com.example.data.local.DanaLiveEventEntity
import com.example.data.model.ActivityLog
import com.example.data.model.AdminInvite
import com.example.data.model.AdminRecord
import com.example.data.model.Announcement
import com.example.data.model.BudgetPlan
import com.example.data.model.DanaWebhookConfig
import com.example.data.model.DanaWebhookEvent
import com.example.data.model.DuesStatus
import com.example.data.model.FinanceEntry
import com.example.data.model.MonthlySummary
import com.example.data.model.Payment
import com.example.data.model.PaymentPackage
import com.example.data.model.Resident
import com.example.data.model.SystemSettings
import com.example.data.model.UserPresence
import com.example.data.model.computeMonthSummary
import com.example.data.model.currentBillingPeriodCode
import com.example.data.model.formatRupiah
import com.example.data.model.monthLabel
import com.example.data.repository.BlokCeRepository
import com.example.data.repository.SubmissionOutcome
import java.util.Collections
import com.example.ui.components.ScannedQrisInfo
import com.example.ui.screens.ScannedReceiptResult
import com.example.worker.BillingWorkerStatus
import com.example.worker.MonthlyBillingScheduler
import com.example.worker.PendingNotifierStatus
import com.example.worker.PendingPaymentNotificationScheduler
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class MainTab {
    HOME,
    PAY,
    FINANCE,
    INFO,
    ADMIN_WARGA,
    ACCOUNT
}

enum class AdminFilter {
    ALL,
    UNPAID,
    PENDING,
    PAID
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class MainUiState(
    val isLoading: Boolean = true,
    val isOnline: Boolean = true,
    val selectedMonth: String = currentBillingPeriodCode(),
    val activeTab: MainTab = MainTab.HOME,
    val selectedPayPackage: PaymentPackage = PaymentPackage.IURAN,
    val adminFilter: AdminFilter = AdminFilter.ALL,
    val adminSearchQuery: String = "",
    val settings: SystemSettings = SystemSettings(),
    val currentResident: Resident? = null,
    val allResidents: List<Resident> = emptyList(),
    val userPayments: List<Payment> = emptyList(),
    val allPayments: List<Payment> = emptyList(),
    val adminInvites: List<AdminInvite> = emptyList(),
    val delegatedAdmins: List<AdminRecord> = emptyList(),
    val billingWorkerStatus: BillingWorkerStatus = BillingWorkerStatus(),
    val pendingNotifierStatus: PendingNotifierStatus = PendingNotifierStatus(),
    val danaLiveEvents: List<DanaLiveEventEntity> = emptyList(),
    val danaListenerEnabled: Boolean = false,
    val danaAutoVerifyEnabled: Boolean = true,
    val danaWebhookConfig: DanaWebhookConfig = DanaWebhookConfig(),
    val danaWebhookEvents: List<DanaWebhookEvent> = emptyList(),
    val financeEntries: List<FinanceEntry> = emptyList(),
    val budgetPlans: List<BudgetPlan> = emptyList(),
    val onlinePresence: List<UserPresence> = emptyList(),
    val activityLogs: List<ActivityLog> = emptyList(),
    val pendingOfflineCount: Int = 0,
    val announcements: List<Announcement> = emptyList(),
    val monthlySummary: MonthlySummary? = null,
    val showQrisZoom: Boolean = false,
    val showQrisScanner: Boolean = false,
    val scannedQris: ScannedQrisInfo? = null,
    val showReceiptScanner: Boolean = false,
    val scannedReceipt: ScannedReceiptResult? = null,
    val showReminderDialog: Boolean = false,
    val showAdminAccessDeniedDialog: Boolean = false,
    val isMutating: Boolean = false,
    val errorBanner: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val repository: BlokCeRepository,
    private val currentUserId: String,
    private val requestedAdminPortal: Boolean = false,
    private val pendingAdminInviteCode: String = "",
    private val appContext: Context? = null
) : ViewModel() {

    private companion object {
        const val TAG = "MainVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }

    private val _selectedMonth = MutableStateFlow(currentBillingPeriodCode())
    private val _activeTab = MutableStateFlow(MainTab.HOME)
    private val _selectedPayPackage = MutableStateFlow(PaymentPackage.IURAN)
    private val _adminFilter = MutableStateFlow(AdminFilter.ALL)
    private val _adminSearchQuery = MutableStateFlow("")
    private val _billingWorkerStatus = MutableStateFlow(
        appContext?.let { MonthlyBillingScheduler.getStatus(it) } ?: BillingWorkerStatus()
    )
    private val _pendingNotifierStatus = MutableStateFlow(
        appContext?.let { PendingPaymentNotificationScheduler.getStatus(it) } ?: PendingNotifierStatus()
    )
    private val _danaListenerEnabled = MutableStateFlow(
        appContext?.let { DanaLiveBridge.isNotificationListenerEnabled(it) } ?: false
    )
    private val _danaAutoVerifyEnabled = MutableStateFlow(
        appContext?.let { DanaLiveBridge.isAutoVerifyEnabled(it) } ?: true
    )
    private val _showQrisZoom = MutableStateFlow(false)
    private val _showQrisScanner = MutableStateFlow(false)
    private val _scannedQris = MutableStateFlow<ScannedQrisInfo?>(null)
    private val _showReceiptScanner = MutableStateFlow(false)
    private val _scannedReceipt = MutableStateFlow<ScannedReceiptResult?>(null)
    private val _showReminderDialog = MutableStateFlow(false)
    private val _showAdminAccessDeniedDialog = MutableStateFlow(false)
    private val _isMutating = MutableStateFlow(false)
    private val _errorBanner = MutableStateFlow<String?>(null)
    private val _bootstrapped = MutableStateFlow(false)

    private val _toastEvent = MutableStateFlow<String?>(null)
    val toastEvent: StateFlow<String?> = _toastEvent

    private val _latestWebhookConfig = MutableStateFlow(DanaWebhookConfig())
    private val processedWebhookIds = Collections.synchronizedSet(mutableSetOf<String>())

    private var dismissedReminderThisSession = false
    private var lastHandledPushBlastId = 0L

    private fun <T> Flow<T>.wrapFirestoreErrors(fallbackFlow: Flow<T>, label: String): Flow<T> =
        this.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Stream warning ($label): ${rootError.message}", rootError)
            _errorBanner.value = rootError.localizedMessage ?: "Menggunakan cache lokal SQLite"
            fallbackFlow.collect { emit(it) }
        }

    private val isOnlineFlow: StateFlow<Boolean> = repository.observeOnlineStatus()
        .onEach { online ->
            if (online) {
                viewModelScope.launch {
                    val synced = repository.flushOfflinePayments().getOrDefault(0)
                    if (synced > 0) {
                        _toastEvent.value = "$synced transaksi antrean offline berhasil disinkronkan!"
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), true)

    private val settingsFlow: Flow<SystemSettings> = repository.observeSystemSettings()
        .wrapFirestoreErrors(repository.observeCachedSettings(), "settings")

    private val currentResidentFlow: Flow<Resident?> = repository.observeCurrentResident(currentUserId)
        .wrapFirestoreErrors(repository.observeCachedResident(currentUserId), "resident")

    private val isAdminFlow: Flow<Boolean> = currentResidentFlow
        .map { it?.adminRole == true }
        .distinctUntilChanged()

    private val isPrimaryAdminFlow: Flow<Boolean> = currentResidentFlow
        .map { it?.primaryAdmin == true }
        .distinctUntilChanged()

    private val allResidentsFlow: Flow<List<Resident>> = isAdminFlow.flatMapLatest { isAdmin ->
        if (isAdmin) {
            repository.observeAllResidents()
                .wrapFirestoreErrors(repository.observeCachedAllResidents(), "allResidents")
        } else {
            flowOf(emptyList())
        }
    }

    private val userPaymentsFlow: Flow<List<Payment>> = combine(
        repository.observeUserPayments(currentUserId)
            .wrapFirestoreErrors(repository.observeCachedUserPayments(currentUserId), "userPayments"),
        repository.observePendingOfflinePayments(currentUserId)
    ) { cloudOrCached, offlineQueued ->
        val cloudIds = cloudOrCached.map { it.id }.toSet()
        val pendingOnly = offlineQueued.filter { it.id !in cloudIds }
        pendingOnly + cloudOrCached
    }

    private val allPaymentsFlow: Flow<List<Payment>> = isAdminFlow.flatMapLatest { isAdmin ->
        if (isAdmin) {
            repository.observeAllPayments()
                .wrapFirestoreErrors(repository.observeCachedAllPayments(), "allPayments")
        } else {
            flowOf(emptyList())
        }
    }

    private val adminInvitesFlow: Flow<List<AdminInvite>> = isPrimaryAdminFlow.flatMapLatest { isPrimary ->
        if (isPrimary) {
            repository.observeAdminInvites()
                .catch { error ->
                    val firestoreError = generateSequence<Throwable>(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    emit(emptyList())
                }
        } else {
            flowOf(emptyList())
        }
    }

    private val delegatedAdminsFlow: Flow<List<AdminRecord>> = isAdminFlow.flatMapLatest { isAdmin ->
        if (isAdmin) {
            repository.observeDelegatedAdmins()
                .catch { error ->
                    val firestoreError = generateSequence<Throwable>(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    emit(emptyList())
                }
        } else {
            flowOf(emptyList())
        }
    }

    private val danaLiveEventsFlow: Flow<List<DanaLiveEventEntity>> = isPrimaryAdminFlow.flatMapLatest { isPrimary ->
        if (isPrimary) {
            repository.observeDanaLiveEvents()
        } else {
            flowOf(emptyList())
        }
    }

    private val danaWebhookConfigFlow: Flow<DanaWebhookConfig> = isPrimaryAdminFlow.flatMapLatest { isPrimary ->
        if (isPrimary) {
            repository.observeDanaWebhookConfig()
                .catch { error ->
                    val firestoreError = generateSequence<Throwable>(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    emit(DanaWebhookConfig())
                }
                .onEach { cfg ->
                    _latestWebhookConfig.value = cfg
                    appContext?.let { ctx ->
                        DanaLiveBridge.saveWebhookLocalPrefs(
                            context = ctx,
                            webhookUrl = cfg.webhookUrl,
                            webhookSecret = cfg.webhookSecret,
                            relayEnabled = cfg.relayFromAndroidEnabled
                        )
                    }
                }
        } else {
            flowOf(DanaWebhookConfig())
        }
    }

    private val danaWebhookEventsFlow: Flow<List<DanaWebhookEvent>> = isPrimaryAdminFlow.flatMapLatest { isPrimary ->
        if (isPrimary) {
            repository.observeDanaWebhookEvents()
                .catch { error ->
                    val firestoreError = generateSequence<Throwable>(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    emit(emptyList())
                }
                .onEach { events ->
                    if (_danaAutoVerifyEnabled.value && _latestWebhookConfig.value.autoVerifyWebhookEvents) {
                        val incomingReceived = events.filter {
                            it.status == "RECEIVED" && it.id.isNotBlank() && processedWebhookIds.add(it.id)
                        }
                        if (incomingReceived.isNotEmpty()) {
                            viewModelScope.launch {
                                incomingReceived.forEach { ev ->
                                    val claimRes = repository.claimAndVerifyWebhookEvent(ev)
                                    claimRes.onSuccess { msg ->
                                        if (msg.startsWith("LUNAS WEBHOOK")) {
                                            syncSummaryAfterMutation()
                                            _toastEvent.value = msg
                                        }
                                    }.onFailure {
                                        processedWebhookIds.remove(ev.id)
                                    }
                                }
                            }
                        }
                    }
                }
        } else {
            flowOf(emptyList())
        }
    }

    private val financeEntriesFlow: Flow<List<FinanceEntry>> = repository.observeFinanceEntries()
        .catch { error ->
            val firestoreError = generateSequence<Throwable>(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(emptyList())
        }

    private val budgetPlansFlow: Flow<List<BudgetPlan>> = repository.observeBudgetPlans()
        .catch { error ->
            val firestoreError = generateSequence<Throwable>(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(emptyList())
        }

    private val onlinePresenceFlow: Flow<List<UserPresence>> = isPrimaryAdminFlow.flatMapLatest { isPrimary ->
        if (isPrimary) {
            repository.observeOnlinePresence()
                .catch { error ->
                    val firestoreError = generateSequence<Throwable>(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    emit(emptyList())
                }
        } else {
            flowOf(emptyList())
        }
    }

    private val activityLogsFlow: Flow<List<ActivityLog>> = isPrimaryAdminFlow.flatMapLatest { isPrimary ->
        if (isPrimary) {
            repository.observeActivityLogs()
                .catch { error ->
                    val firestoreError = generateSequence<Throwable>(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    emit(emptyList())
                }
        } else {
            flowOf(emptyList())
        }
    }

    private val announcementsFlow: Flow<List<Announcement>> = repository.observeAnnouncements()
        .wrapFirestoreErrors(repository.observeCachedAnnouncements(), "announcements")

    private val monthlySummaryFlow: Flow<MonthlySummary?> = _selectedMonth.flatMapLatest { month ->
        repository.observeMonthlySummary(month)
            .wrapFirestoreErrors(repository.observeCachedMonthlySummary(month), "monthlySummary")
    }

    private val pendingOfflineCountFlow: Flow<Int> =
        repository.observePendingOfflinePayments(currentUserId).map { it.size }

    private val coreDataFlow = combine(
        settingsFlow,
        currentResidentFlow,
        allResidentsFlow,
        userPaymentsFlow,
        allPaymentsFlow
    ) { settings, currentResident, allResidents, userPayments, allPayments ->
        CoreDataBundle(settings, currentResident, allResidents, userPayments, allPayments)
    }

    private val secondaryDataFlow = combine(
        announcementsFlow,
        monthlySummaryFlow,
        pendingOfflineCountFlow,
        isOnlineFlow,
        _bootstrapped
    ) { announcements, monthlySummary, pendingCount, isOnline, bootstrapped ->
        SecondaryDataBundle(announcements, monthlySummary, pendingCount, isOnline, bootstrapped)
    }

    private val uiControlFlow = combine(
        _selectedMonth,
        _activeTab,
        _selectedPayPackage,
        _adminFilter,
        _adminSearchQuery
    ) { month, tab, pkg, filter, search ->
        UiControlBundle(month, tab, pkg, filter, search)
    }

    private val danaWebhookPairFlow = combine(
        danaWebhookConfigFlow,
        danaWebhookEventsFlow
    ) { cfg, events -> cfg to events }

    private val adminManagementFlow = combine(
        adminInvitesFlow,
        delegatedAdminsFlow,
        _billingWorkerStatus,
        danaLiveEventsFlow,
        combine(_danaListenerEnabled, _danaAutoVerifyEnabled) { enabled, auto -> enabled to auto },
        danaWebhookPairFlow
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val invites = args[0] as List<AdminInvite>
        @Suppress("UNCHECKED_CAST")
        val admins = args[1] as List<AdminRecord>
        val workerStatus = args[2] as BillingWorkerStatus
        @Suppress("UNCHECKED_CAST")
        val danaEvents = args[3] as List<DanaLiveEventEntity>
        @Suppress("UNCHECKED_CAST")
        val danaFlags = args[4] as Pair<Boolean, Boolean>
        @Suppress("UNCHECKED_CAST")
        val webhookPair = args[5] as Pair<DanaWebhookConfig, List<DanaWebhookEvent>>
        AdminManagementBundle(
            invites = invites,
            delegatedAdmins = admins,
            workerStatus = workerStatus,
            danaEvents = danaEvents,
            danaListenerEnabled = danaFlags.first,
            danaAutoVerifyEnabled = danaFlags.second,
            danaWebhookConfig = webhookPair.first,
            danaWebhookEvents = webhookPair.second
        )
    }

    private val financeAndMonitorFlow = combine(
        financeEntriesFlow,
        budgetPlansFlow,
        onlinePresenceFlow,
        activityLogsFlow,
        _pendingNotifierStatus
    ) { entries, plans, presence, logs, pendingNotif ->
        FinanceAndMonitorBundle(entries, plans, presence, logs, pendingNotif)
    }

    private val adminAndFinanceFlow = combine(
        adminManagementFlow,
        financeAndMonitorFlow
    ) { adminMgmt, finMon -> adminMgmt to finMon }

    private val dialogStateFlow = combine(
        _showQrisZoom,
        _showQrisScanner,
        _scannedQris,
        _showReceiptScanner,
        _scannedReceipt,
        _showReminderDialog,
        _showAdminAccessDeniedDialog,
        _isMutating,
        _errorBanner
    ) { args ->
        DialogStateBundle(
            showQrisZoom = args[0] as Boolean,
            showQrisScanner = args[1] as Boolean,
            scannedQris = args[2] as ScannedQrisInfo?,
            showReceiptScanner = args[3] as Boolean,
            scannedReceipt = args[4] as ScannedReceiptResult?,
            showReminderDialog = args[5] as Boolean,
            showAdminAccessDeniedDialog = args[6] as Boolean,
            isMutating = args[7] as Boolean,
            errorBanner = args[8] as String?
        )
    }

    val uiState: StateFlow<MainUiState> = combine(
        coreDataFlow,
        secondaryDataFlow,
        uiControlFlow,
        dialogStateFlow,
        adminAndFinanceFlow
    ) { core, sec, ctrl, dlg, (adminMgmt, finMon) ->
        evaluateReminderPopup(core.settings, core.currentResident, core.userPayments, ctrl.selectedMonth)
        val effectiveTab = if (ctrl.activeTab == MainTab.ADMIN_WARGA && core.currentResident?.adminRole != true) {
            MainTab.HOME
        } else {
            ctrl.activeTab
        }
        MainUiState(
            isLoading = !sec.bootstrapped && core.currentResident == null,
            isOnline = sec.isOnline,
            selectedMonth = ctrl.selectedMonth,
            activeTab = effectiveTab,
            selectedPayPackage = ctrl.selectedPayPackage,
            adminFilter = ctrl.adminFilter,
            adminSearchQuery = ctrl.adminSearchQuery,
            settings = core.settings,
            currentResident = core.currentResident,
            allResidents = core.allResidents,
            userPayments = core.userPayments,
            allPayments = core.allPayments,
            adminInvites = adminMgmt.invites,
            delegatedAdmins = adminMgmt.delegatedAdmins,
            billingWorkerStatus = adminMgmt.workerStatus,
            pendingNotifierStatus = finMon.pendingNotifierStatus,
            danaLiveEvents = adminMgmt.danaEvents,
            danaListenerEnabled = adminMgmt.danaListenerEnabled,
            danaAutoVerifyEnabled = adminMgmt.danaAutoVerifyEnabled,
            danaWebhookConfig = adminMgmt.danaWebhookConfig,
            danaWebhookEvents = adminMgmt.danaWebhookEvents,
            financeEntries = finMon.financeEntries,
            budgetPlans = finMon.budgetPlans,
            onlinePresence = finMon.onlinePresence,
            activityLogs = finMon.activityLogs,
            pendingOfflineCount = sec.pendingCount,
            announcements = sec.announcements,
            monthlySummary = sec.monthlySummary,
            showQrisZoom = dlg.showQrisZoom,
            showQrisScanner = dlg.showQrisScanner,
            scannedQris = dlg.scannedQris,
            showReceiptScanner = dlg.showReceiptScanner,
            scannedReceipt = dlg.scannedReceipt,
            showReminderDialog = dlg.showReminderDialog,
            showAdminAccessDeniedDialog = dlg.showAdminAccessDeniedDialog,
            isMutating = dlg.isMutating,
            errorBanner = dlg.errorBanner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = MainUiState()
    )

    init {
        viewModelScope.launch {
            val user = Firebase.auth.currentUser
            if (user != null && user.uid == currentUserId) {
                var residentRes = repository.ensureSessionBootstrapped(user)
                if (residentRes.isFailure) {
                    _errorBanner.value = residentRes.exceptionOrNull()?.localizedMessage
                }
                if (pendingAdminInviteCode.isNotBlank() && residentRes.getOrNull()?.adminRole != true) {
                    val redeemRes = repository.redeemAdminInvite(pendingAdminInviteCode)
                    redeemRes.onSuccess { roleLabel ->
                        _toastEvent.value = "Akses Admin aktif ($roleLabel)!"
                        residentRes = repository.ensureSessionBootstrapped(user)
                    }.onFailure { err ->
                        _toastEvent.value = err.localizedMessage ?: "Kode undangan admin tidak valid."
                    }
                }
                val resident = residentRes.getOrNull()
                if (requestedAdminPortal && resident?.adminRole != true) {
                    _showAdminAccessDeniedDialog.value = true
                }
                if (appContext != null) {
                    MonthlyBillingScheduler.ensureMonthlyBillingScheduled(appContext)
                    _billingWorkerStatus.value = MonthlyBillingScheduler.getStatus(appContext)
                    PendingPaymentNotificationScheduler.ensureScheduled(appContext)
                    _pendingNotifierStatus.value = PendingPaymentNotificationScheduler.getStatus(appContext)
                }
                repository.flushOfflinePayments()
                repository.upsertUserPresence(resident, tabLabel(_activeTab.value), online = true)
                repository.logActivity(
                    resident = resident,
                    actionType = "LOGIN",
                    screen = tabLabel(_activeTab.value),
                    detail = "Membuka aplikasi (${resident?.roleLabel ?: "Warga"})"
                )
            }
            _bootstrapped.value = true
        }
    }

    private fun evaluateReminderPopup(
        settings: SystemSettings,
        resident: Resident?,
        payments: List<Payment>,
        month: String
    ) {
        if (resident == null || resident.adminRole) return
        val summary = computeMonthSummary(resident, payments, month)
        if (summary.overall == DuesStatus.PAID) return

        val blastId = settings.pushBlastId
        val target = settings.pushTargetUserId.ifBlank { "all_unpaid" }
        val hasPendingThisMonth = summary.overall == DuesStatus.PENDING ||
            payments.any { it.month == month && it.status == "pending" }
        val matchesTarget = when (target) {
            "all_unpaid" -> true
            "all_pending" -> hasPendingThisMonth
            else -> target == resident.id
        }
        val isNewBlast = blastId > lastHandledPushBlastId && matchesTarget

        if (isNewBlast) {
            lastHandledPushBlastId = blastId
            dismissedReminderThisSession = false
            _showReminderDialog.value = true
        } else if (settings.reminderEnabled && !dismissedReminderThisSession && matchesTarget) {
            _showReminderDialog.value = true
        }
    }

    private fun tabLabel(tab: MainTab): String = when (tab) {
        MainTab.HOME -> "Beranda"
        MainTab.PAY -> "Bayar"
        MainTab.FINANCE -> "Keuangan"
        MainTab.INFO -> "Info"
        MainTab.ADMIN_WARGA -> "Admin"
        MainTab.ACCOUNT -> "Akun"
    }

    private fun requireAdminAccess(): Boolean {
        if (uiState.value.currentResident?.adminRole == true) return true
        _toastEvent.value = "Akses ditolak: menu & aksi ini khusus Admin."
        return false
    }

    private fun requirePrimaryAdminAccess(): Boolean {
        if (uiState.value.currentResident?.primaryAdmin == true) return true
        _toastEvent.value = "Akses khusus Admin Utama (${BlokCeRepository.BOOTSTRAPPED_ADMIN_EMAIL})."
        return false
    }

    fun setPresenceOnline(online: Boolean) {
        val state = uiState.value
        val resident = state.currentResident ?: return
        viewModelScope.launch {
            repository.upsertUserPresence(resident, tabLabel(state.activeTab), online)
        }
    }

    fun selectTab(tab: MainTab) {
        val state = uiState.value
        if (tab == MainTab.ADMIN_WARGA && state.currentResident?.adminRole != true) {
            _showAdminAccessDeniedDialog.value = true
            _toastEvent.value = "Akses Admin Warga khusus akun dengan role: admin."
            return
        }
        val previous = _activeTab.value
        _activeTab.value = tab
        if (previous != tab) {
            val label = tabLabel(tab)
            val resident = state.currentResident
            if (resident != null) {
                viewModelScope.launch {
                    repository.upsertUserPresence(resident, label, online = true)
                    repository.logActivity(
                        resident = resident,
                        actionType = "NAVIGATE",
                        screen = label,
                        detail = "Membuka tab $label"
                    )
                }
            }
        }
    }

    fun selectMonth(month: String) {
        _selectedMonth.value = month
    }

    fun selectPayPackage(pkg: PaymentPackage) {
        _selectedPayPackage.value = pkg
    }

    fun setAdminFilter(filter: AdminFilter) {
        _adminFilter.value = filter
    }

    fun setAdminSearch(query: String) {
        _adminSearchQuery.value = query
    }

    fun openQrisZoom() {
        _showQrisZoom.value = true
    }

    fun closeQrisZoom() {
        _showQrisZoom.value = false
    }

    fun openQrisScanner() {
        _showQrisZoom.value = false
        _showQrisScanner.value = true
    }

    fun closeQrisScanner() {
        _showQrisScanner.value = false
    }

    fun onQrisScanned(info: ScannedQrisInfo) {
        _scannedQris.value = info
        _showQrisScanner.value = false
        _activeTab.value = MainTab.PAY
        _toastEvent.value = "QRIS terdeteksi: ${info.merchantName}"
    }

    fun clearScannedQris() {
        _scannedQris.value = null
    }

    fun openReceiptScanner() {
        _showReceiptScanner.value = true
    }

    fun closeReceiptScanner() {
        _showReceiptScanner.value = false
    }

    fun onReceiptScanned(result: ScannedReceiptResult) {
        _scannedReceipt.value = result
        _showReceiptScanner.value = false
        _toastEvent.value = "Struk pembayaran siap dilampirkan!"
        val resident = uiState.value.currentResident
        if (resident != null) {
            viewModelScope.launch {
                repository.logActivity(
                    resident = resident,
                    actionType = "RECEIPT_SCAN",
                    screen = "Bayar",
                    detail = result.parsed.summaryText.ifBlank { "Memindai struk pembayaran" }
                )
            }
        }
    }

    fun clearScannedReceipt() {
        _scannedReceipt.value = null
    }

    fun dismissReminderDialog() {
        dismissedReminderThisSession = true
        _showReminderDialog.value = false
    }

    fun dismissAdminAccessDeniedDialog() {
        _showAdminAccessDeniedDialog.value = false
    }

    fun redeemAdminInvite(code: String) {
        viewModelScope.launch {
            _isMutating.value = true
            val res = repository.redeemAdminInvite(code)
            res.onSuccess { label ->
                Firebase.auth.currentUser?.let { repository.ensureSessionBootstrapped(it) }
                _showAdminAccessDeniedDialog.value = false
                _toastEvent.value = "Akses Admin berhasil diaktifkan ($label)!"
            }.onFailure { err ->
                _toastEvent.value = err.localizedMessage ?: "Gagal mengaktifkan kode admin."
            }
            _isMutating.value = false
        }
    }

    fun adminCreateInvite(label: String, targetEmail: String) {
        if (uiState.value.currentResident?.primaryAdmin != true) {
            _toastEvent.value = "Pembuatan kode undangan khusus Admin Utama."
            return
        }
        viewModelScope.launch {
            _isMutating.value = true
            val res = repository.createAdminInvite(label, targetEmail)
            _isMutating.value = false
            res.onSuccess { invite ->
                _toastEvent.value = "Kode admin dibuat: ${invite.code}"
            }.onFailure { err ->
                _toastEvent.value = err.localizedMessage ?: "Gagal membuat kode undangan."
            }
        }
    }

    fun adminDeleteInvite(code: String) {
        if (uiState.value.currentResident?.primaryAdmin != true) {
            _toastEvent.value = "Akses khusus Admin Utama."
            return
        }
        viewModelScope.launch {
            _isMutating.value = true
            val res = repository.deleteAdminInvite(code)
            _isMutating.value = false
            _toastEvent.value = if (res.isSuccess) {
                "Kode undangan dihapus."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Gagal menghapus kode."
            }
        }
    }

    fun adminRevokeDelegatedAdmin(targetUid: String) {
        adminSetResidentRole(targetUid, makeAdmin = false, roleLabel = "Warga / Member")
    }

    fun adminSetResidentRole(
        targetUid: String,
        makeAdmin: Boolean,
        roleLabel: String = "Admin / Pengurus Blok"
    ) {
        if (!requireAdminAccess()) return
        viewModelScope.launch {
            _isMutating.value = true
            val res = repository.setResidentRole(targetUid, makeAdmin, roleLabel)
            _isMutating.value = false
            _toastEvent.value = if (res.isSuccess) {
                if (makeAdmin) "Role warga berhasil dinaikkan menjadi Admin!" else "Role admin diturunkan menjadi Warga."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Gagal mengubah role admin."
            }
        }
    }

    fun adminEditResidentProfile(
        targetUid: String,
        username: String,
        name: String,
        house: String,
        phone: String,
        hidmatEnabled: Boolean,
        makeAdmin: Boolean,
        roleLabel: String
    ) {
        if (!requireAdminAccess()) return
        viewModelScope.launch {
            _isMutating.value = true
            val res = repository.adminEditResidentProfile(
                targetUid = targetUid,
                username = username,
                name = name,
                house = house,
                phone = phone,
                hidmatEnabled = hidmatEnabled,
                makeAdmin = makeAdmin,
                roleLabel = roleLabel
            )
            _isMutating.value = false
            _toastEvent.value = if (res.isSuccess) {
                "Profil warga diperbarui!"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Gagal memperbarui profil warga."
            }
        }
    }

    fun refreshDanaListenerStatus() {
        appContext?.let {
            _danaListenerEnabled.value = DanaLiveBridge.isNotificationListenerEnabled(it)
            _danaAutoVerifyEnabled.value = DanaLiveBridge.isAutoVerifyEnabled(it)
        }
    }

    fun setDanaAutoVerifyEnabled(enabled: Boolean) {
        if (!requirePrimaryAdminAccess()) return
        appContext?.let {
            DanaLiveBridge.setAutoVerifyEnabled(it, enabled)
            _danaAutoVerifyEnabled.value = enabled
            _toastEvent.value = if (enabled) {
                "Verifikasi DANA Live otomatis diaktifkan."
            } else {
                "Verifikasi DANA Live otomatis dijeda (mode pantau)."
            }
        }
    }

    fun adminVerifyLiveDanaManual(eventId: Long, paymentId: String, senderHint: String) {
        if (!requirePrimaryAdminAccess()) return
        viewModelScope.launch {
            _isMutating.value = true
            val res = repository.verifyLiveDanaForPayment(eventId, paymentId, senderHint)
            if (res.isSuccess) {
                syncSummaryAfterMutation()
                _toastEvent.value = "Tagihan dicocokkan & diverifikasi LUNAS dari mutasi DANA!"
            } else {
                _toastEvent.value = res.exceptionOrNull()?.localizedMessage ?: "Gagal mencocokkan mutasi DANA."
            }
            _isMutating.value = false
        }
    }

    fun adminClearDanaLiveEvents() {
        if (!requirePrimaryAdminAccess()) return
        viewModelScope.launch {
            repository.clearDanaLiveEvents()
            _toastEvent.value = "Log mutasi notifikasi DANA dibersihkan."
        }
    }

    fun adminSaveWebhookConfig(
        webhookUrl: String,
        webhookSecret: String,
        relayEnabled: Boolean,
        autoVerifyEnabled: Boolean
    ) {
        if (!requirePrimaryAdminAccess()) return
        viewModelScope.launch {
            _isMutating.value = true
            val cfg = DanaWebhookConfig(
                webhookUrl = webhookUrl.trim().take(500),
                webhookSecret = webhookSecret.trim().take(200),
                relayFromAndroidEnabled = relayEnabled,
                autoVerifyWebhookEvents = autoVerifyEnabled
            )
            val res = repository.saveDanaWebhookConfig(cfg)
            _isMutating.value = false
            res.onSuccess {
                _latestWebhookConfig.value = cfg
                appContext?.let { ctx ->
                    DanaLiveBridge.saveWebhookLocalPrefs(ctx, cfg.webhookUrl, cfg.webhookSecret, cfg.relayFromAndroidEnabled)
                }
                _toastEvent.value = "Konfigurasi Webhook DANA berhasil disimpan!"
            }.onFailure { err ->
                _toastEvent.value = err.localizedMessage ?: "Gagal menyimpan konfigurasi webhook."
            }
        }
    }

    fun adminTestWebhookPing(webhookUrl: String, webhookSecret: String) {
        if (!requirePrimaryAdminAccess()) return
        viewModelScope.launch {
            _isMutating.value = true
            val res = DanaWebhookDispatcher.sendPing(webhookUrl, webhookSecret)
            _isMutating.value = false
            res.onSuccess { dispatch ->
                _toastEvent.value = "Webhook aktif (HTTP ${dispatch.statusCode})!"
            }.onFailure { err ->
                _toastEvent.value = err.localizedMessage ?: "Gagal menghubungi endpoint webhook."
            }
        }
    }

    fun adminVerifyWebhookEventManual(eventId: String, paymentId: String, senderName: String) {
        if (!requirePrimaryAdminAccess()) return
        viewModelScope.launch {
            _isMutating.value = true
            val res = repository.verifyWebhookEventForPayment(eventId, paymentId, senderName)
            if (res.isSuccess) {
                syncSummaryAfterMutation()
                _toastEvent.value = "Tagihan diverifikasi LUNAS dari event webhook!"
            } else {
                _toastEvent.value = res.exceptionOrNull()?.localizedMessage ?: "Gagal memverifikasi event webhook."
            }
            _isMutating.value = false
        }
    }

    fun adminDeleteWebhookEvent(eventId: String) {
        if (!requirePrimaryAdminAccess()) return
        viewModelScope.launch {
            val res = repository.deleteDanaWebhookEvent(eventId)
            _toastEvent.value = if (res.isSuccess) {
                "Event webhook dihapus."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Gagal menghapus event webhook."
            }
        }
    }

    fun adminGenerateMonthlyBills(targetMonth: String = uiState.value.selectedMonth) {
        if (!requireAdminAccess()) return
        viewModelScope.launch {
            _isMutating.value = true
            val res = repository.generateMonthlyUnpaidPayments(targetMonth)
            _isMutating.value = false
            res.onSuccess { createdCount ->
                if (appContext != null) {
                    MonthlyBillingScheduler.recordRunResult(appContext, targetMonth, createdCount)
                    _billingWorkerStatus.value = MonthlyBillingScheduler.getStatus(appContext)
                }
                val periodText = monthLabel(targetMonth)
                _toastEvent.value = if (createdCount > 0) {
                    "$createdCount tagihan unpaid dibuat untuk $periodText."
                } else {
                    "Seluruh tagihan periode $periodText sudah lengkap."
                }
            }.onFailure { err ->
                _toastEvent.value = err.localizedMessage ?: "Gagal membuat tagihan otomatis."
            }
        }
    }

    fun consumeToast() {
        _toastEvent.value = null
    }

    fun clearErrorBanner() {
        _errorBanner.value = null
    }

    fun submitPayment(method: String, note: String) {
        val state = uiState.value
        val resident = state.currentResident ?: return
        val sum = computeMonthSummary(resident, state.userPayments, state.selectedMonth)
        val items = mutableListOf<Pair<String, Long>>()

        if ((state.selectedPayPackage == PaymentPackage.IURAN || state.selectedPayPackage == PaymentPackage.BOTH) &&
            sum.iuranStatus == DuesStatus.UNPAID
        ) {
            items.add("iuran" to state.settings.iuranAmount)
        }
        if ((state.selectedPayPackage == PaymentPackage.HIDMAT || state.selectedPayPackage == PaymentPackage.BOTH) &&
            resident.hidmatEnabled && sum.hidmatStatus == DuesStatus.UNPAID
        ) {
            items.add("hidmat" to state.settings.hidmatAmount)
        }
        if (items.isEmpty()) {
            _toastEvent.value = "Tagihan sudah lunas atau sedang menunggu verifikasi."
            return
        }

        val receiptImg = _scannedReceipt.value?.receiptImageUrl.orEmpty()
        val receiptOcr = _scannedReceipt.value?.parsed?.summaryText.orEmpty()

        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.submitPayment(
                month = state.selectedMonth,
                types = items,
                method = method,
                note = note,
                receiptImageUrl = receiptImg,
                receiptOcrText = receiptOcr
            )
            _isMutating.value = false
            result.onSuccess { outcome ->
                _scannedQris.value = null
                _scannedReceipt.value = null
                _toastEvent.value = when (outcome) {
                    SubmissionOutcome.SENT_CLOUD -> "Konfirmasi terkirim! Menunggu verifikasi Admin."
                    SubmissionOutcome.QUEUED_OFFLINE -> "Offline: disimpan ke SQLite lokal, otomatis terkirim saat online."
                }
                repository.logActivity(
                    resident = resident,
                    actionType = "PAYMENT_SUBMIT",
                    screen = "Bayar",
                    detail = "${items.joinToString("+") { it.first.uppercase() }} ${monthLabel(state.selectedMonth)} ($method)"
                )
            }.onFailure { err ->
                _toastEvent.value = err.localizedMessage ?: "Gagal mengirim konfirmasi."
            }
        }
    }

    fun flushOfflineNow() {
        viewModelScope.launch {
            _isMutating.value = true
            val count = repository.flushOfflinePayments().getOrDefault(0)
            _isMutating.value = false
            _toastEvent.value = if (count > 0) {
                "$count transaksi offline berhasil disinkronkan!"
            } else {
                "Tidak ada antrean offline yang tertunda."
            }
        }
    }

    fun updateOwnProfile(username: String, name: String, house: String, phone: String) {
        viewModelScope.launch {
            _isMutating.value = true
            val res = repository.updateOwnProfile(username, name, house, phone)
            _isMutating.value = false
            _toastEvent.value = if (res.isSuccess) {
                "Profil berhasil disimpan!"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Gagal menyimpan profil."
            }
        }
    }

    fun adminVerifyPayment(paymentId: String) {
        if (!requireAdminAccess()) return
        val actor = uiState.value.currentResident
        viewModelScope.launch {
            _isMutating.value = true
            val res = repository.verifyPayment(paymentId)
            if (res.isSuccess) {
                syncSummaryAfterMutation()
                _toastEvent.value = "Pembayaran diverifikasi LUNAS."
                repository.logActivity(actor, "ADMIN_ACTION", "Admin", "Verifikasi LUNAS transaksi $paymentId")
            } else {
                _toastEvent.value = res.exceptionOrNull()?.localizedMessage ?: "Gagal memverifikasi."
            }
            _isMutating.value = false
        }
    }

    fun adminRejectPayment(paymentId: String) {
        if (!requireAdminAccess()) return
        val actor = uiState.value.currentResident
        viewModelScope.launch {
            _isMutating.value = true
            val res = repository.rejectPayment(paymentId)
            if (res.isSuccess) {
                syncSummaryAfterMutation()
                _toastEvent.value = "Konfirmasi ditolak & direset."
                repository.logActivity(actor, "ADMIN_ACTION", "Admin", "Menolak konfirmasi $paymentId")
            } else {
                _toastEvent.value = res.exceptionOrNull()?.localizedMessage ?: "Gagal menolak pembayaran."
            }
            _isMutating.value = false
        }
    }

    fun adminMarkPaid(targetUid: String, type: String) {
        if (!requireAdminAccess()) return
        val state = uiState.value
        val actor = state.currentResident
        val amount = if (type == "hidmat") state.settings.hidmatAmount else state.settings.iuranAmount
        viewModelScope.launch {
            _isMutating.value = true
            val res = repository.markPaymentPaid(targetUid, state.selectedMonth, type, amount)
            if (res.isSuccess) {
                syncSummaryAfterMutation()
                _toastEvent.value = "${type.uppercase()} ditandai LUNAS."
                repository.logActivity(actor, "ADMIN_ACTION", "Admin", "Menandai LUNAS ${type.uppercase()} (${state.selectedMonth})")
            } else {
                _toastEvent.value = res.exceptionOrNull()?.localizedMessage ?: "Gagal mencatat pembayaran."
            }
            _isMutating.value = false
        }
    }

    fun adminToggleHidmat(targetUid: String, enabled: Boolean) {
        if (!requireAdminAccess()) return
        val actor = uiState.value.currentResident
        viewModelScope.launch {
            _isMutating.value = true
            val res = repository.toggleResidentHidmat(targetUid, enabled)
            if (res.isSuccess) {
                _toastEvent.value = "Hidmat ${if (enabled) "diaktifkan" else "dinonaktifkan"}."
                repository.logActivity(actor, "ADMIN_ACTION", "Admin", "Hidmat ${if (enabled) "ON" else "OFF"} untuk $targetUid")
            } else {
                _toastEvent.value = res.exceptionOrNull()?.localizedMessage ?: "Gagal mengubah status Hidmat."
            }
            _isMutating.value = false
        }
    }

    fun adminDeleteResident(targetUid: String) {
        if (!requirePrimaryAdminAccess()) return
        val actor = uiState.value.currentResident
        viewModelScope.launch {
            _isMutating.value = true
            val res = repository.deleteResident(targetUid)
            if (res.isSuccess) {
                syncSummaryAfterMutation()
                _toastEvent.value = "Akun berhasil dihapus oleh Admin Utama."
                repository.logActivity(actor, "ADMIN_ACTION", "Admin", "Menghapus akun $targetUid")
            } else {
                _toastEvent.value = res.exceptionOrNull()?.localizedMessage ?: "Gagal menghapus akun."
            }
            _isMutating.value = false
        }
    }

    fun adminSaveAnnouncement(
        existingId: String?,
        title: String,
        category: String,
        imageUrl: String,
        content: String,
        urgent: Boolean
    ) {
        if (!requireAdminAccess()) return
        val actor = uiState.value.currentResident
        val authorName = actor?.name ?: "Admin"
        viewModelScope.launch {
            _isMutating.value = true
            val res = repository.saveAnnouncement(existingId, title, category, imageUrl, content, urgent, authorName)
            _isMutating.value = false
            if (res.isSuccess) {
                _toastEvent.value = "Informasi dipublikasikan!"
                repository.logActivity(actor, "INFO_POST", "Info", "Publikasi info: ${title.take(60)}")
            } else {
                _toastEvent.value = res.exceptionOrNull()?.localizedMessage ?: "Gagal menyimpan informasi."
            }
        }
    }

    fun adminDeleteAnnouncement(id: String) {
        if (!requireAdminAccess()) return
        viewModelScope.launch {
            _isMutating.value = true
            val res = repository.deleteAnnouncement(id)
            _isMutating.value = false
            _toastEvent.value = if (res.isSuccess) {
                "Informasi dihapus."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Gagal menghapus informasi."
            }
        }
    }

    fun adminSaveSettings(updated: SystemSettings) {
        if (!requirePrimaryAdminAccess()) return
        val actor = uiState.value.currentResident
        viewModelScope.launch {
            _isMutating.value = true
            val res = repository.saveSystemSettings(updated)
            _isMutating.value = false
            if (res.isSuccess) {
                _toastEvent.value = "Pengaturan blok disimpan!"
                repository.logActivity(actor, "SYSTEM_UPDATE", "Admin", "Memperbarui Sistem Admin Blok")
            } else {
                _toastEvent.value = res.exceptionOrNull()?.localizedMessage ?: "Gagal menyimpan pengaturan."
            }
        }
    }

    fun adminSendPushBlast(targetUserId: String = "all_unpaid") {
        if (!requirePrimaryAdminAccess()) return
        val actor = uiState.value.currentResident
        val updated = uiState.value.settings.copy(
            pushBlastId = System.currentTimeMillis(),
            pushTargetUserId = targetUserId,
            reminderEnabled = true
        )
        viewModelScope.launch {
            _isMutating.value = true
            val res = repository.saveSystemSettings(updated)
            _isMutating.value = false
            if (res.isSuccess) {
                _toastEvent.value = "Push notif himbauan dikirim ke seluruh warga!"
                repository.logActivity(actor, "SYSTEM_UPDATE", "Admin", "Mengirim Push Blast Himbauan")
            } else {
                _toastEvent.value = res.exceptionOrNull()?.localizedMessage ?: "Gagal mengirim himbauan."
            }
        }
    }

    fun adminToggleReminder(enabled: Boolean) {
        if (!requirePrimaryAdminAccess()) return
        val updated = uiState.value.settings.copy(reminderEnabled = enabled)
        viewModelScope.launch {
            val res = repository.saveSystemSettings(updated)
            _toastEvent.value = if (res.isSuccess) {
                "Himbauan otomatis ${if (enabled) "dinyalakan" else "dimatikan"}."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Gagal mengubah himbauan."
            }
        }
    }

    fun adminTogglePendingNotifierService(enabled: Boolean) {
        if (!requirePrimaryAdminAccess()) return
        val ctx = appContext ?: return
        PendingPaymentNotificationScheduler.setServiceEnabled(ctx, enabled)
        _pendingNotifierStatus.value = PendingPaymentNotificationScheduler.getStatus(ctx)
        _toastEvent.value = if (enabled) {
            "Layanan notifikasi otomatis status pending diaktifkan (cek tiap 12 jam)."
        } else {
            "Layanan notifikasi otomatis status pending dijeda."
        }
        val actor = uiState.value.currentResident
        viewModelScope.launch {
            repository.logActivity(
                actor,
                "SYSTEM_UPDATE",
                "Admin",
                "Notif Pending Otomatis ${if (enabled) "ON" else "OFF"}"
            )
        }
    }

    fun adminTriggerPendingPushNotificationNow() {
        if (!requirePrimaryAdminAccess()) return
        val actor = uiState.value.currentResident
        val updated = uiState.value.settings.copy(
            pushBlastId = System.currentTimeMillis(),
            pushTargetUserId = "all_pending",
            reminderEnabled = true
        )
        viewModelScope.launch {
            _isMutating.value = true
            val res = repository.saveSystemSettings(updated)
            _isMutating.value = false
            if (res.isSuccess) {
                appContext?.let { ctx ->
                    PendingPaymentNotificationScheduler.setServiceEnabled(ctx, true)
                    PendingPaymentNotificationScheduler.enqueueImmediateCheck(ctx, forceNotify = true)
                    _pendingNotifierStatus.value = PendingPaymentNotificationScheduler.getStatus(ctx)
                }
                _toastEvent.value = "Push notif pengingat dikirim ke seluruh warga berstatus pending bulan ini!"
                repository.logActivity(
                    actor,
                    "SYSTEM_UPDATE",
                    "Admin",
                    "Mengirim Notifikasi Otomatis Pending Bulan Ini"
                )
            } else {
                _toastEvent.value = res.exceptionOrNull()?.localizedMessage ?: "Gagal memicu notifikasi pending."
            }
        }
    }

    fun logFinanceExport(formatLabel: String) {
        val actor = uiState.value.currentResident ?: return
        if (!actor.primaryAdmin) return
        viewModelScope.launch {
            repository.logActivity(
                actor,
                "FINANCE_UPDATE",
                "Keuangan",
                "Ekspor Ringkasan Keuangan ($formatLabel • ${monthLabel(uiState.value.selectedMonth)})"
            )
        }
    }

    fun adminSaveFinanceEntry(
        existingId: String?,
        kind: String,
        title: String,
        category: String,
        amount: Long,
        month: String,
        note: String
    ) {
        if (!requirePrimaryAdminAccess()) return
        val actor = uiState.value.currentResident
        viewModelScope.launch {
            _isMutating.value = true
            val res = repository.saveFinanceEntry(
                existingId = existingId,
                kind = kind,
                title = title,
                category = category,
                amount = amount,
                month = month,
                note = note,
                actorName = actor?.name ?: "Admin Utama"
            )
            _isMutating.value = false
            if (res.isSuccess) {
                _toastEvent.value = "Catatan keuangan disimpan!"
                repository.logActivity(
                    actor,
                    "FINANCE_UPDATE",
                    "Keuangan",
                    "${kind.uppercase()} ${formatRupiah(amount)} • ${title.take(40)}"
                )
            } else {
                _toastEvent.value = res.exceptionOrNull()?.localizedMessage ?: "Gagal menyimpan catatan keuangan."
            }
        }
    }

    fun adminDeleteFinanceEntry(entryId: String) {
        if (!requirePrimaryAdminAccess()) return
        val actor = uiState.value.currentResident
        viewModelScope.launch {
            _isMutating.value = true
            val res = repository.deleteFinanceEntry(entryId)
            _isMutating.value = false
            if (res.isSuccess) {
                _toastEvent.value = "Catatan keuangan dihapus."
                repository.logActivity(actor, "FINANCE_UPDATE", "Keuangan", "Menghapus entri $entryId")
            } else {
                _toastEvent.value = res.exceptionOrNull()?.localizedMessage ?: "Gagal menghapus catatan."
            }
        }
    }

    fun adminSaveBudgetPlan(
        existingId: String?,
        title: String,
        category: String,
        month: String,
        plannedAmount: Long,
        realizedAmount: Long,
        status: String,
        note: String
    ) {
        if (!requirePrimaryAdminAccess()) return
        val actor = uiState.value.currentResident
        viewModelScope.launch {
            _isMutating.value = true
            val res = repository.saveBudgetPlan(
                existingId = existingId,
                title = title,
                category = category,
                month = month,
                plannedAmount = plannedAmount,
                realizedAmount = realizedAmount,
                status = status,
                note = note,
                actorName = actor?.name ?: "Admin Utama"
            )
            _isMutating.value = false
            if (res.isSuccess) {
                _toastEvent.value = "Rencana anggaran disimpan!"
                repository.logActivity(
                    actor,
                    "FINANCE_UPDATE",
                    "Keuangan",
                    "RAB: ${title.take(40)} (${formatRupiah(plannedAmount)})"
                )
            } else {
                _toastEvent.value = res.exceptionOrNull()?.localizedMessage ?: "Gagal menyimpan rencana anggaran."
            }
        }
    }

    fun adminDeleteBudgetPlan(planId: String) {
        if (!requirePrimaryAdminAccess()) return
        val actor = uiState.value.currentResident
        viewModelScope.launch {
            _isMutating.value = true
            val res = repository.deleteBudgetPlan(planId)
            _isMutating.value = false
            if (res.isSuccess) {
                _toastEvent.value = "Rencana anggaran dihapus."
                repository.logActivity(actor, "FINANCE_UPDATE", "Keuangan", "Menghapus RAB $planId")
            } else {
                _toastEvent.value = res.exceptionOrNull()?.localizedMessage ?: "Gagal menghapus anggaran."
            }
        }
    }

    private suspend fun syncSummaryAfterMutation() {
        val state = uiState.value
        if (state.currentResident?.adminRole == true) {
            repository.syncMonthlySummary(state.selectedMonth, state.allResidents, state.allPayments)
        }
    }

    fun buildMonthlyCsv(): String {
        val state = uiState.value
        return repository.buildMonthlyCsv(state.selectedMonth, state.allResidents, state.allPayments)
    }

    fun buildFundRecapCsv(): String {
        val state = uiState.value
        return repository.buildFundRecapCsv(
            month = state.selectedMonth,
            residents = state.allResidents,
            payments = state.allPayments,
            settings = state.settings
        )
    }

    suspend fun clearCachesBeforeSignOut() {
        val resident = uiState.value.currentResident
        withContext(NonCancellable) {
            runCatching {
                repository.upsertUserPresence(resident, "Keluar", online = false)
            }
            repository.clearSensitiveLocalCaches()
        }
    }
}

private data class CoreDataBundle(
    val settings: SystemSettings,
    val currentResident: Resident?,
    val allResidents: List<Resident>,
    val userPayments: List<Payment>,
    val allPayments: List<Payment>
)

private data class SecondaryDataBundle(
    val announcements: List<Announcement>,
    val monthlySummary: MonthlySummary?,
    val pendingCount: Int,
    val isOnline: Boolean,
    val bootstrapped: Boolean
)

private data class UiControlBundle(
    val selectedMonth: String,
    val activeTab: MainTab,
    val selectedPayPackage: PaymentPackage,
    val adminFilter: AdminFilter,
    val adminSearchQuery: String
)

private data class AdminManagementBundle(
    val invites: List<AdminInvite>,
    val delegatedAdmins: List<AdminRecord>,
    val workerStatus: BillingWorkerStatus,
    val danaEvents: List<DanaLiveEventEntity>,
    val danaListenerEnabled: Boolean,
    val danaAutoVerifyEnabled: Boolean,
    val danaWebhookConfig: DanaWebhookConfig,
    val danaWebhookEvents: List<DanaWebhookEvent>
)

private data class FinanceAndMonitorBundle(
    val financeEntries: List<FinanceEntry>,
    val budgetPlans: List<BudgetPlan>,
    val onlinePresence: List<UserPresence>,
    val activityLogs: List<ActivityLog>,
    val pendingNotifierStatus: PendingNotifierStatus
)

private data class DialogStateBundle(
    val showQrisZoom: Boolean,
    val showQrisScanner: Boolean,
    val scannedQris: ScannedQrisInfo?,
    val showReceiptScanner: Boolean,
    val scannedReceipt: ScannedReceiptResult?,
    val showReminderDialog: Boolean,
    val showAdminAccessDeniedDialog: Boolean,
    val isMutating: Boolean,
    val errorBanner: String?
)
