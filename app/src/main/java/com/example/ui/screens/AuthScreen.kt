package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.remote.attemptAutoSignIn
import com.example.data.remote.onGoogleSignInClicked
import com.example.ui.components.EmphasizedDecelerate
import com.example.ui.components.LivePulseDot
import com.example.ui.components.StaggeredEntrance
import com.example.ui.theme.DanaBlue
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Emerald800
import com.example.ui.theme.Emerald950
import com.example.ui.theme.Slate900
import com.example.ui.theme.Teal800
import com.example.ui.theme.adaptiveColors
import com.example.ui.theme.adaptiveFieldColors
import kotlin.math.cos
import kotlin.math.sin

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
    val ac = adaptiveColors

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

    val bgTop by animateColorAsState(
        targetValue = if (isAdminMode) Color(0xFF0B1120) else Emerald950,
        animationSpec = tween(420),
        label = "auth_bg_top"
    )
    val bgMid by animateColorAsState(
        targetValue = if (isAdminMode) Emerald950 else Emerald800,
        animationSpec = tween(420),
        label = "auth_bg_mid"
    )
    val bgBottom by animateColorAsState(
        targetValue = if (isAdminMode) Color(0xFF0E3A47) else Teal800,
        animationSpec = tween(420),
        label = "auth_bg_bottom"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "auth_orbs")
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_angle"
    )
    val iconFloat by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_float"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(bgTop, bgMid, bgBottom)))
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        // Ambient animated background orbs & subtle geometric rings
        Canvas(modifier = Modifier.fillMaxSize()) {
            val orb1Center = Offset(
                x = size.width * (0.82f + 0.08f * cos(orbitAngle)),
                y = size.height * (0.14f + 0.04f * sin(orbitAngle))
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        (if (isAdminMode) DanaBlue else Emerald500).copy(alpha = 0.28f),
                        Color.Transparent
                    ),
                    center = orb1Center,
                    radius = size.minDimension * 0.55f
                ),
                radius = size.minDimension * 0.55f,
                center = orb1Center
            )

            val orb2Center = Offset(
                x = size.width * (0.15f + 0.07f * sin(orbitAngle)),
                y = size.height * (0.36f + 0.05f * cos(orbitAngle))
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = orb2Center,
                    radius = size.minDimension * 0.42f
                ),
                radius = size.minDimension * 0.42f,
                center = orb2Center
            )
        }

        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Animated Hero Header
            StaggeredEntrance(delayMillis = 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.16f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LivePulseDot(
                                color = if (isAdminMode) Color(0xFF38BDF8) else Emerald500,
                                size = 7.dp
                            )
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
                        color = Color(0xFF09523E),
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(
                            1.5.dp,
                            if (isAdminMode) Color(0xFF38BDF8).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.28f)
                        ),
                        shadowElevation = 12.dp,
                        modifier = Modifier
                            .size(68.dp)
                            .graphicsLayer { translationY = iconFloat }
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_art),
                            contentDescription = "Logo Blok CE PSA",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
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
                            "Akses Khusus Admin Utama & Pengurus Blok"
                        } else {
                            "Paguyuban Warga Perumahan Silih Asih"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Emerald100
                    )
                }
            }

            // Bottom Auth Sheet Card (Adaptive Light/Dark Surface)
            StaggeredEntrance(delayMillis = 90) {
                Surface(
                    color = ac.cardContainer,
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    shadowElevation = 18.dp,
                    border = BorderStroke(1.dp, ac.borderSubtle.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 24.dp, vertical = 24.dp)
                            .animateContentSize(
                                animationSpec = spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Portal Switcher (Warga vs Khusus Admin)
                        Surface(
                            color = ac.subtleContainer,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, ac.borderSubtle),
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
                                    inactiveTextColor = ac.textSecondary,
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
                                    activeColor = if (ac.isDark) DanaBlue else Slate900,
                                    inactiveTextColor = ac.textSecondary,
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

                        AnimatedContent(
                            targetState = isAdminMode,
                            transitionSpec = {
                                (fadeIn(tween(240, easing = EmphasizedDecelerate)) +
                                    slideInVertically(tween(260, easing = EmphasizedDecelerate)) { it / 5 }) togetherWith
                                    (fadeOut(tween(150)) + slideOutVertically(tween(180)) { -it / 6 })
                            },
                            label = "auth_portal_copy"
                        ) { adminSelected ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (adminSelected) "Akses Masuk Khusus Admin" else "Masuk ke Aplikasi Warga",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = ac.textPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (adminSelected) {
                                        "Kelola verifikasi pembayaran warga, analitik kas, keuangan blok, role pengurus, dan DANA live."
                                    } else {
                                        "Pantau iuran bulanan, Hidmat, kas transparan blok, scan struk OCR, dan info warga secara real-time."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ac.textSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = isAdminMode,
                            enter = expandVertically(tween(280, easing = EmphasizedDecelerate)) +
                                fadeIn(tween(240, easing = EmphasizedDecelerate)),
                            exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                OutlinedTextField(
                                    value = adminInviteCode,
                                    onValueChange = { adminInviteCode = it.uppercase() },
                                    label = { Text("Kode Undangan Admin (opsional)") },
                                    placeholder = { Text("Contoh: CE-ADM-XXXX") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.Key,
                                            contentDescription = null,
                                            tint = ac.textMuted
                                        )
                                    },
                                    singleLine = true,
                                    colors = adaptiveFieldColors(),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_invite_code_input")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        AnimatedVisibility(
                            visible = errorMessage != null,
                            enter = expandVertically(tween(240, easing = EmphasizedDecelerate)) + fadeIn(tween(200)),
                            exit = shrinkVertically(tween(180)) + fadeOut(tween(140))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Surface(
                                    color = ac.roseSurface,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, ac.roseBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = errorMessage ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ac.roseText,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                            }
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
                                containerColor = if (isAdminMode) {
                                    if (ac.isDark) DanaBlue else Slate900
                                } else {
                                    Emerald700
                                },
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
                                subtitle = "Google & Biometrik",
                                modifier = Modifier.weight(1f)
                            )
                            FeatureChip(
                                icon = Icons.Filled.Storage,
                                title = "Offline Siap",
                                subtitle = "Cloud & SQLite",
                                modifier = Modifier.weight(1f)
                            )
                        }
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
    inactiveTextColor: Color,
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
                tint = if (selected) Color.White else inactiveTextColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) Color.White else inactiveTextColor,
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
    val ac = adaptiveColors
    Surface(
        color = ac.emeraldSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ac.emeraldBorder),
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
                    .background(ac.cardContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ac.emeraldText,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = ac.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = ac.textSecondary
                )
            }
        }
    }
}
