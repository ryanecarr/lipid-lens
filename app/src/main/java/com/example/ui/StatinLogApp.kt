package com.example.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Biotech
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.DoseLog
import com.example.data.model.KnownStatin
import com.example.data.model.LipidPanel
import com.example.data.model.StatinRegimen
import com.example.data.remote.PdfBriefUiState
import com.example.data.remote.TherapyAddMode
import com.example.ui.components.AppearanceSheet
import com.example.ui.components.ClinicalTargetsSheet
import com.example.ui.components.LogDoseSheet
import com.example.ui.components.LogLipidPanelSheet
import com.example.ui.components.ReminderSheet
import com.example.ui.components.SwitchStatinSheet
import com.example.ui.screens.LabsScreen
import com.example.ui.screens.LogsScreen
import com.example.ui.screens.MedicationsScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SummaryScreen
import com.example.ui.theme.AppPalette
import com.example.ui.theme.AppearanceConfig
import com.example.ui.theme.ThemeMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatinLogApp(
    viewModel: StatinViewModel,
    userDisplayName: String?,
    userEmail: String?,
    onSignOut: () -> Unit,
    appearance: AppearanceConfig = AppearanceConfig(),
    onSelectPalette: (AppPalette) -> Unit = {},
    onSelectThemeMode: (ThemeMode) -> Unit = {}
) {
    val context = LocalContext.current
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val aiInsightState by viewModel.aiInsightState.collectAsStateWithLifecycle()
    val includeAiInPdf by viewModel.includeAiInPdf.collectAsStateWithLifecycle()
    val pdfBriefState by viewModel.pdfBriefState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState = (screenState as? UiState.Success)?.data

    var currentTab by remember { mutableStateOf(AppTab.SUMMARY) }
    var showLogSheet by remember { mutableStateOf(false) }
    var editingLog by remember { mutableStateOf<DoseLog?>(null) }
    var initialLogRegimen by remember { mutableStateOf<StatinRegimen?>(null) }
    var showLabSheet by remember { mutableStateOf(false) }
    var editingLabPanel by remember { mutableStateOf<LipidPanel?>(null) }
    var showSwitchSheet by remember { mutableStateOf(false) }
    var preselectedCatalogStatin by remember { mutableStateOf<KnownStatin?>(null) }
    var initialSwitchMode by remember { mutableStateOf<TherapyAddMode?>(null) }
    var showTargetsSheet by remember { mutableStateOf(false) }
    var showReminderSheet by remember { mutableStateOf(false) }
    var showAppearanceSheet by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val data = uiState ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            viewModel.savePdfToUri(context, data, uri)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(uiState?.reminderSettings, uiState?.activeRegimens) {
        uiState?.let { viewModel.syncReminderCache(context, it) }
    }

    if (currentTab != AppTab.SUMMARY) {
        BackHandler {
            currentTab = AppTab.SUMMARY
        }
    }

    val openLogSheet: (DoseLog?, StatinRegimen?) -> Unit = { log, regimen ->
        if (uiState?.activeRegimens.isNullOrEmpty()) {
            preselectedCatalogStatin = null
            initialSwitchMode = null
            showSwitchSheet = true
        } else {
            editingLog = log
            initialLogRegimen = regimen
            showLogSheet = true
        }
    }

    val openSwitchSheet: (KnownStatin?, TherapyAddMode?) -> Unit = { statin, mode ->
        preselectedCatalogStatin = statin
        initialSwitchMode = mode
        showSwitchSheet = true
    }

    val openLabSheet: (LipidPanel?) -> Unit = { panel ->
        editingLabPanel = panel
        showLabSheet = true
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 640.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = { showAppearanceSheet = true },
                            modifier = Modifier.testTag("topbar_palette_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Palette,
                                contentDescription = stringResource(R.string.cd_theme_palette),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge
                            )
                            val subtitleText = when {
                                uiState == null || uiState.activeRegimens.isEmpty() ->
                                    stringResource(R.string.summary_subtitle_cloud)
                                uiState.activeRegimens.size == 1 -> {
                                    val first = uiState.activeRegimens.first()
                                    "${first.statinName} ${first.doseMg} mg"
                                }
                                else -> {
                                    val shown = uiState.activeRegimens.take(2)
                                        .joinToString(" + ") { "${it.statinName} ${it.doseMg} mg" }
                                    if (uiState.activeRegimens.size > 2) "$shown +${uiState.activeRegimens.size - 2}"
                                    else shown
                                }
                            }
                            Text(
                                text = subtitleText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showReminderSheet = true },
                            enabled = uiState != null,
                            modifier = Modifier.testTag("topbar_reminder_button")
                        ) {
                            Icon(
                                imageVector = if (uiState?.reminderSettings?.enabled == true) {
                                    Icons.Outlined.NotificationsActive
                                } else {
                                    Icons.Outlined.NotificationsNone
                                },
                                contentDescription = stringResource(R.string.cd_reminder_settings),
                                tint = if (uiState?.reminderSettings?.enabled == true) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                        IconButton(
                            onClick = { showAccountDialog = true },
                            modifier = Modifier.testTag("topbar_account_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AccountCircle,
                                contentDescription = stringResource(R.string.cd_account_menu),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            floatingActionButton = {
                if ((currentTab == AppTab.SUMMARY || currentTab == AppTab.LOGS) && !uiState?.activeRegimens.isNullOrEmpty()) {
                    FloatingActionButton(
                        onClick = { openLogSheet(null, null) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("fab_log_dose")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.action_log_todays_dose)
                        )
                    }
                } else if (currentTab == AppTab.LABS && uiState != null) {
                    FloatingActionButton(
                        onClick = { openLabSheet(null) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("fab_log_lab")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.action_log_lab_panel)
                        )
                    }
                }
            },
            bottomBar = {
                if (!useRail && uiState != null) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        NavTabItems().forEach { item ->
                            val selected = currentTab == item.tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = item.tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag("nav_tab_${item.tab.name}")
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
                if (useRail && uiState != null) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(Modifier.size(12.dp))
                        NavTabItems().forEach { item ->
                            val selected = currentTab == item.tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = item.tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag("rail_tab_${item.tab.name}")
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f).fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (val state = screenState) {
                        UiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator()
                                    Text(
                                        text = "Syncing your medication logs...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        is UiState.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Text(
                                        text = "Cloud Sync Unavailable",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    OutlinedButton(
                                        onClick = onSignOut,
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text(stringResource(R.string.action_sign_out))
                                    }
                                }
                            }
                        }

                        is UiState.Success -> {
                            val loaded = state.data
                            Crossfade(targetState = currentTab, label = "tab_crossfade") { tab ->
                                when (tab) {
                                    AppTab.SUMMARY -> SummaryScreen(
                                        state = loaded,
                                        aiInsightState = aiInsightState,
                                        onSetupOrSwitchStatin = { openSwitchSheet(null, null) },
                                        onLogTodayDose = { openLogSheet(null, null) },
                                        onLogForRegimen = { reg -> openLogSheet(null, reg) },
                                        onEditLog = { log -> openLogSheet(log, null) },
                                        onOpenReminders = { showReminderSheet = true },
                                        onOpenReports = { currentTab = AppTab.REPORTS },
                                        onOpenLabs = { currentTab = AppTab.LABS },
                                        onGenerateInsight = viewModel::generatePersonalInsight
                                    )
                                    AppTab.LOGS -> LogsScreen(
                                        state = loaded,
                                        onSelectFilter = viewModel::setLogFilter,
                                        onSelectStatinFilter = viewModel::setLogStatinFilter,
                                        onAddLog = { openLogSheet(null, null) },
                                        onEditLog = { log -> openLogSheet(log, null) },
                                        onDeleteLog = viewModel::deleteDoseLog
                                    )
                                    AppTab.LABS -> LabsScreen(
                                        state = loaded,
                                        aiInsightState = aiInsightState,
                                        onSelectMetric = viewModel::setLabMetric,
                                        onSelectRange = viewModel::setLabRange,
                                        onAddPanel = { openLabSheet(null) },
                                        onEditPanel = { panel -> openLabSheet(panel) },
                                        onDeletePanel = viewModel::deleteLipidPanel,
                                        onGenerateInsight = viewModel::generatePersonalInsight,
                                        onOpenTargets = { showTargetsSheet = true }
                                    )
                                    AppTab.MEDICATIONS -> MedicationsScreen(
                                        state = loaded,
                                        onSwitchWithCatalogItem = { statin -> openSwitchSheet(statin, null) },
                                        onOpenAddTherapy = { mode -> openSwitchSheet(null, mode) },
                                        onDeactivateRegimen = viewModel::deactivateRegimen
                                    )
                                    AppTab.REPORTS -> {
                                        val baseReport = remember(loaded) {
                                            StatinViewModel.buildClinicalReportData(loaded)
                                        }
                                        val currentScopeKey = remember(loaded, baseReport) {
                                            StatinViewModel.reportScopeKey(loaded, baseReport)
                                        }
                                        val matchedBrief = remember(includeAiInPdf, pdfBriefState, currentScopeKey) {
                                            if (includeAiInPdf) {
                                                (pdfBriefState as? PdfBriefUiState.Success)
                                                    ?.brief
                                                    ?.takeIf { it.scopeKey == currentScopeKey }
                                            } else {
                                                null
                                            }
                                        }
                                        val reportData = remember(baseReport, matchedBrief) {
                                            baseReport.copy(aiVisitBrief = matchedBrief)
                                        }
                                        ReportsScreen(
                                            state = loaded,
                                            reportData = reportData,
                                            includeAiInPdf = includeAiInPdf,
                                            pdfBriefState = pdfBriefState,
                                            currentScopeKey = currentScopeKey,
                                            onToggleIncludeAi = viewModel::setIncludeAiInPdf,
                                            onRefreshAiBrief = viewModel::generatePdfVisitBrief,
                                            onSelectRange = viewModel::setReportRange,
                                            onSelectStatinFilter = viewModel::setReportStatinFilter,
                                            onSharePdf = {
                                                viewModel.exportSharePdf(context, loaded) { uri ->
                                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                        type = "application/pdf"
                                                        putExtra(Intent.EXTRA_STREAM, uri)
                                                        putExtra(
                                                            Intent.EXTRA_SUBJECT,
                                                            "Lipid Lens Clinical Lipid & Therapy Report"
                                                        )
                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    context.startActivity(
                                                        Intent.createChooser(shareIntent, "Share Clinical PDF Report")
                                                    )
                                                }
                                            },
                                            onSavePdf = {
                                                val stamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                                                savePdfLauncher.launch("LipidLens_Report_$stamp.pdf")
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLogSheet && !uiState?.activeRegimens.isNullOrEmpty()) {
        LogDoseSheet(
            activeRegimens = uiState!!.activeRegimens,
            existingLog = editingLog,
            initialRegimen = initialLogRegimen,
            onDismiss = {
                showLogSheet = false
                editingLog = null
                initialLogRegimen = null
            },
            onSave = { existingId, chosenRegimen, taken, dateKey, timeMinutes, feel, effects, notes ->
                viewModel.saveDoseLog(
                    existingLogId = existingId,
                    regimen = chosenRegimen,
                    taken = taken,
                    logDateKey = dateKey,
                    takenAtMinutes = timeMinutes,
                    feelLevel = feel,
                    sideEffects = effects,
                    notes = notes
                )
                showLogSheet = false
                editingLog = null
                initialLogRegimen = null
            }
        )
    }

    if (showLabSheet && uiState != null) {
        LogLipidPanelSheet(
            existingPanel = editingLabPanel,
            onDismiss = {
                showLabSheet = false
                editingLabPanel = null
            },
            onSave = { existingId, dateKey, drawnMillis, total, ldl, hdl, tg, nonHdl, apoB, lpA, fasting, baselinePanel, labName, notes ->
                viewModel.saveLipidPanel(
                    existingPanelId = existingId,
                    drawDateKey = dateKey,
                    drawnAtMillis = drawnMillis,
                    totalCholesterolMgDl = total,
                    ldlMgDl = ldl,
                    hdlMgDl = hdl,
                    triglyceridesMgDl = tg,
                    nonHdlMgDl = nonHdl,
                    apoBMgDl = apoB,
                    lpAMgDl = lpA,
                    fasting = fasting,
                    baselinePanel = baselinePanel,
                    labName = labName,
                    notes = notes
                )
                showLabSheet = false
                editingLabPanel = null
            }
        )
    }

    if (showSwitchSheet && uiState != null) {
        SwitchStatinSheet(
            activeRegimens = uiState.activeRegimens,
            preselectedStatin = preselectedCatalogStatin,
            initialMode = initialSwitchMode,
            onDismiss = {
                showSwitchSheet = false
                preselectedCatalogStatin = null
                initialSwitchMode = null
            },
            onSave = { name, brand, doseMg, frequency, timeMinutes, therapyMode, reason, notes ->
                viewModel.saveRegimen(
                    statinName = name,
                    brandName = brand,
                    doseMg = doseMg,
                    frequency = frequency,
                    scheduledTimeMinutes = timeMinutes,
                    therapyMode = therapyMode,
                    switchReason = reason,
                    notes = notes
                )
                showSwitchSheet = false
                preselectedCatalogStatin = null
                initialSwitchMode = null
            }
        )
    }

    if (showTargetsSheet && uiState != null) {
        ClinicalTargetsSheet(
            currentTargets = uiState.clinicalTargets,
            onDismiss = { showTargetsSheet = false },
            onSave = { ldlTarget, nonHdlTarget, apoBTarget, riskLabel, clinicianNote ->
                viewModel.saveClinicalTargets(
                    ldlTargetMgDl = ldlTarget,
                    nonHdlTargetMgDl = nonHdlTarget,
                    apoBTargetMgDl = apoBTarget,
                    riskLabel = riskLabel,
                    clinicianNote = clinicianNote
                )
                showTargetsSheet = false
            }
        )
    }

    if (showReminderSheet && uiState != null) {
        ReminderSheet(
            settings = uiState.reminderSettings,
            activeRegimen = uiState.activeRegimen,
            onDismiss = { showReminderSheet = false },
            onSave = { enabled, hour, minute ->
                viewModel.saveReminderSettings(context, enabled, hour, minute)
                showReminderSheet = false
            },
            onSendTest = {
                viewModel.sendTestNotification(context)
            }
        )
    }

    if (showAppearanceSheet) {
        AppearanceSheet(
            appearance = appearance,
            onSelectPalette = onSelectPalette,
            onSelectMode = onSelectThemeMode,
            onDismiss = { showAppearanceSheet = false }
        )
    }

    if (showAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.CloudDone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Cloud Sync Account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!userDisplayName.isNullOrBlank()) {
                        Text(
                            text = userDisplayName,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    if (!userEmail.isNullOrBlank()) {
                        Text(
                            text = userEmail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Your statin regimens, daily check-ins, and reminder settings are encrypted in transit and isolated to your Google account in Cloud Firestore.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAccountDialog = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.action_sign_out))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccountDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

private data class NavTabConfig(
    val tab: AppTab,
    val label: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
private fun NavTabItems(): List<NavTabConfig> = listOf(
    NavTabConfig(
        tab = AppTab.SUMMARY,
        label = stringResource(R.string.nav_summary),
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard
    ),
    NavTabConfig(
        tab = AppTab.LOGS,
        label = stringResource(R.string.nav_logs),
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History
    ),
    NavTabConfig(
        tab = AppTab.LABS,
        label = stringResource(R.string.nav_labs),
        selectedIcon = Icons.Filled.Biotech,
        unselectedIcon = Icons.Outlined.Biotech
    ),
    NavTabConfig(
        tab = AppTab.MEDICATIONS,
        label = stringResource(R.string.nav_medications),
        selectedIcon = Icons.Filled.Medication,
        unselectedIcon = Icons.Outlined.Medication
    ),
    NavTabConfig(
        tab = AppTab.REPORTS,
        label = stringResource(R.string.nav_reports),
        selectedIcon = Icons.Filled.Assessment,
        unselectedIcon = Icons.Outlined.Assessment
    )
)
