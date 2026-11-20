package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auth.attemptAutoSignIn
import com.example.auth.authStateFlow
import com.example.auth.signOut
import com.example.data.remote.StatinCloudRepository
import com.example.notifications.ReminderCachePrefs
import com.example.notifications.ReminderScheduler
import com.example.ui.StatinLogApp
import com.example.ui.StatinViewModel
import com.example.ui.components.AppearanceSheet
import com.example.ui.screens.AuthScreen
import com.example.ui.theme.AppPalette
import com.example.ui.theme.AppearanceConfig
import com.example.ui.theme.StatinLogTheme
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.ThemePreferences
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        val themePrefs = ThemePreferences(applicationContext)
        ReminderScheduler.ensureNotificationChannel(applicationContext)

        setContent {
            val appearance by themePrefs.config.collectAsStateWithLifecycle()
            StatinLogTheme(appearance = appearance) {
                AppNavigation(
                    appearance = appearance,
                    onSelectPalette = themePrefs::setPalette,
                    onSelectThemeMode = themePrefs::setThemeMode
                )
            }
        }
    }
}

@Composable
private fun AppNavigation(
    appearance: AppearanceConfig,
    onSelectPalette: (AppPalette) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    val credentialManager = remember(context) { CredentialManager.create(context) }
    val scope = rememberCoroutineScope()

    var isCheckingAutoSignIn by remember { mutableStateOf(auth.currentUser == null) }
    var showAuthAppearanceSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { isCheckingAutoSignIn = false },
                onUnauthenticated = { isCheckingAutoSignIn = false },
                scope = scope
            )
        } else {
            isCheckingAutoSignIn = false
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            isCheckingAutoSignIn = isCheckingAutoSignIn,
            onAuthSuccess = { isCheckingAutoSignIn = false },
            onOpenAppearance = { showAuthAppearanceSheet = true }
        )
        if (showAuthAppearanceSheet) {
            AppearanceSheet(
                appearance = appearance,
                onSelectPalette = onSelectPalette,
                onSelectMode = onSelectThemeMode,
                onDismiss = { showAuthAppearanceSheet = false }
            )
        }
    } else {
        key(user.uid) {
            val repository = remember(appContext) {
                val dbId = appContext.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                StatinCloudRepository(db, auth)
            }
            val vm: StatinViewModel = viewModel(
                key = user.uid,
                factory = StatinViewModel.factory(repository, user.uid)
            )
            StatinLogApp(
                viewModel = vm,
                userDisplayName = user.displayName,
                userEmail = user.email,
                onSignOut = {
                    signOut(
                        credentialManager = credentialManager,
                        scope = scope,
                        onBeforeFirebaseSignOut = {
                            ReminderCachePrefs.clearOnSignOut(appContext)
                            ReminderScheduler.cancelDailyReminder(appContext)
                        }
                    )
                },
                appearance = appearance,
                onSelectPalette = onSelectPalette,
                onSelectThemeMode = onSelectThemeMode
            )
        }
    }
}
