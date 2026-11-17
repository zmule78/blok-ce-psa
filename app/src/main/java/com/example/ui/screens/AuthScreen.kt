package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.remote.attemptAutoSignIn
import com.example.data.remote.onGoogleSignInClicked
import com.example.data.repository.BlokCeRepository
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald50
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Emerald800
import com.example.ui.theme.Emerald950
import com.example.ui.theme.Rose100
import com.example.ui.theme.Rose50
import com.example.ui.theme.Rose600
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate900
import com.example.ui.theme.Teal800

private enum class AuthPortalMode {
    WARGA,
    ADMIN
}

@Composable
fun AuthScreen(
    skipAutoSignIn: Boolean = false,
    initialAdminPortal: Boolean = false,
    onPrepareAuthIntent: (isAdminPortal: Boolean, inviteCode: String) -> Unit = { _, _ -> },
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var portalMode by remember(initialAdminPortal) {
        mutableStateOf(if (initialAdminPortal) AuthPortalMode.ADMIN else AuthPortalMode.WARGA)
    }
    var adminInviteCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isAutoChecking by remember { mutableStateOf(!skipAutoSignIn && !initialAdminPortal) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(skipAutoSignIn, initialAdminPortal) {
        if (skipAutoSignIn || initialAdminPortal) {
            isAutoChecking = false
        } else {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    isAutoChecking = false
                    onAuthSuccess()
                },
                onUnauthenticated = {
                    isAutoChecking = false
                },
                scope = scope
            )
        }
    }

    val isAdminMode = portalMode == AuthPortalMode.ADMIN

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (isAdminMode) {
                        listOf(Slate900, Emerald950, Emerald800)
                    } else {
                        listOf(Emerald950, Emerald800, Teal800)
                    }
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Emerald Hero Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.16f),
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isAdminMode) Icons.Filled.AdminPanelSettings else Icons.Filled.CloudDone,
                            contentDescription = null,
                            tint = Emerald100,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isAdminMode) "Portal Pengurus • Akses Terverifikasi" else "Cloud Firestore • Cache SQLite Offline",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 10.dp,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isAdminMode) Icons.Filled.AdminPanelSettings else Icons.Filled.Home,
                            contentDescription = "Ikon Blok CE PSA",
                            tint = if (isAdminMode) Slate900 else Emerald700,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Blok CE PSA",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isAdminMode) {
                        "Akses Khusus Admin, Ketua Blok & Bendahara"
                    } else {
                        "Paguyuban Warga Perumahan Silih Asih"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Emerald100
                )
            }

            // Bottom Auth Sheet Card
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                shadowElevation = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Portal Switcher (Warga vs Khusus Admin)
                    Surface(
                        color = Slate100,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            PortalModeTab(
                                label = "Masuk Warga",
                                icon = Icons.Filled.Person,
                                selected = !isAdminMode,
                                activeColor = Emerald700,
                                onClick = {
                                    portalMode = AuthPortalMode.WARGA
                                    errorMessage = null
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("auth_mode_warga_tab")
                            )
                            PortalModeTab(
                                label = "Khusus Admin",
                                icon = Icons.Filled.AdminPanelSettings,
                                selected = isAdminMode,
                                activeColor = Slate900,
                                onClick = {
                                    portalMode = AuthPortalMode.ADMIN
                                    isAutoChecking = false
                                    errorMessage = null
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("auth_mode_admin_tab")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = if (isAdminMode) "Akses Masuk Khusus Admin" else "Masuk ke Aplikasi Warga",
                        style = MaterialTheme.typography.titleLarge,
                        color = Slate900,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isAdminMode) {
                            "Kelola verifikasi pembayaran warga, tarif DANA/QRIS, data warga, pengumuman, dan ekspor CSV."
                        } else {
                            "Pantau iuran bulanan, Hidmat, pembayaran DANA/QRIS, dan info warga secara real-time."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate600,
                        textAlign = TextAlign.Center
                    )

                    if (isAdminMode) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            color = Emerald50,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "AKUN ADMIN UTAMA TERDAFTAR",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Emerald700,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = BlokCeRepository.BOOTSTRAPPED_ADMIN_EMAIL,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Slate900,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Masuk dengan akun Google di atas untuk akses penuh, atau masukkan Kode Undangan Admin untuk akun pengurus lain.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate600
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = adminInviteCode,
                            onValueChange = { adminInviteCode = it.uppercase() },
                            label = { Text("Kode Undangan Admin (opsional)") },
                            placeholder = { Text("Contoh: CE-ADM-XXXX") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Key,
                                    contentDescription = null,
                                    tint = Slate500
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_invite_code_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    if (errorMessage != null) {
                        Surface(
                            color = Rose50,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Rose100),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = Rose600,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    Button(
                        onClick = {
                            errorMessage = null
                            isLoading = true
                            onPrepareAuthIntent(isAdminMode, adminInviteCode.trim())
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    errorMessage = msg
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    isLoading = false
                                }
                            )
                        },
                        enabled = !isLoading && !isAutoChecking,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAdminMode) Slate900 else Emerald700,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag(if (isAdminMode) "admin_google_sign_in_button" else "google_sign_in_button")
                    ) {
                        if (isLoading || isAutoChecking) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isAutoChecking) "Memeriksa sesi..." else "Menghubungi Google...",
                                style = MaterialTheme.typography.labelLarge
                            )
                        } else {
                            if (isAdminMode) {
                                Icon(
                                    imageVector = Icons.Filled.AdminPanelSettings,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = if (isAdminMode) "Masuk Khusus Admin (Google)" else "Masuk dengan Google",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FeatureChip(
                            icon = Icons.Filled.Lock,
                            title = "Aman",
                            subtitle = "Google Auth",
                            modifier = Modifier.weight(1f)
                        )
                        FeatureChip(
                            icon = Icons.Filled.Storage,
                            title = "Offline Siap",
                            subtitle = "SQLite Room",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PortalModeTab(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) activeColor else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.White else Slate600,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) Color.White else Slate600,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun FeatureChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Emerald50,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Slate200),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Emerald700,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Slate900,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate600
                )
            }
        }
    }
}
