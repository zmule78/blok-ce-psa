package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.SystemSettings
import com.example.data.repository.BlokCeRepository
import com.example.security.BiometricAvailability
import com.example.security.BiometricSecurityState
import com.example.ui.components.uriToCompressedDataUri
import com.example.ui.theme.DanaBlue
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Rose600
import com.example.ui.theme.Slate900
import com.example.ui.theme.adaptiveColors
import com.example.ui.theme.adaptiveFieldColors
import com.example.ui.viewmodel.MainUiState

@Composable
fun AccountTab(
    state: MainUiState,
    onSaveProfile: (username: String, name: String, house: String, phone: String) -> Unit,
    onSaveSettings: (SystemSettings) -> Unit,
    onCreateAdminInvite: (label: String, targetEmail: String) -> Unit,
    onDeleteAdminInvite: (code: String) -> Unit,
    onRevokeDelegatedAdmin: (uid: String) -> Unit,
    onRedeemAdminInvite: (code: String) -> Unit,
    onCopyText: (label: String, text: String) -> Unit,
    onFlushOfflineQueue: () -> Unit,
    biometricState: BiometricSecurityState,
    onTogglePortalBiometric: (Boolean) -> Unit,
    onTogglePaymentBiometric: (Boolean) -> Unit,
    onLockPortalNow: () -> Unit,
    onOpenBiometricEnroll: () -> Unit,
    onSignOut: () -> Unit
) {
    val resident = state.currentResident ?: return
    val context = LocalContext.current
    val ac = adaptiveColors

    var username by remember(resident.id) { mutableStateOf(resident.username) }
    var name by remember(resident.id) { mutableStateOf(resident.name) }
    var house by remember(resident.id) { mutableStateOf(resident.house) }
    var phone by remember(resident.id) { mutableStateOf(resident.phone) }

    LaunchedEffect(resident.username, resident.name, resident.house, resident.phone) {
        username = resident.username
        name = resident.name
        house = resident.house
        phone = resident.phone
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Profile Summary Header
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ac.cardContainer),
            border = BorderStroke(1.dp, ac.borderSubtle)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(Emerald500, Color(0xFF0EA5E9), Emerald600, Emerald500)
                            )
                        )
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Emerald700),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = resident.name.firstOrNull()?.uppercase() ?: "W",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = resident.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = ac.textPrimary
                    )
                    Text(
                        text = "@${resident.username} • ${resident.house}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ac.textSecondary
                    )
                    Text(
                        text = resident.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = ac.textMuted
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Surface(
                        color = if (resident.primaryAdmin) ac.emeraldSurface else if (resident.adminRole) ac.purpleSurface else ac.subtleContainer,
                        shape = CircleShape
                    ) {
                        Text(
                            text = if (resident.primaryAdmin) "Admin Utama" else if (resident.adminRole) "Pengurus Blok" else "Warga",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (resident.primaryAdmin) ac.emeraldText else if (resident.adminRole) ac.purpleText else ac.textSecondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // Profile Form
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ac.cardContainer),
            border = BorderStroke(1.dp, ac.borderSubtle)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Profil Saya",
                    style = MaterialTheme.typography.titleSmall,
                    color = ac.textPrimary
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    colors = adaptiveFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_username_input")
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Lengkap") },
                    singleLine = true,
                    colors = adaptiveFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_name_input")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = house,
                        onValueChange = { house = it },
                        label = { Text("No. Rumah") },
                        singleLine = true,
                        colors = adaptiveFieldColors(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("profile_house_input")
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("No. HP") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        colors = adaptiveFieldColors(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("profile_phone_input")
                    )
                }
                Button(
                    onClick = { onSaveProfile(username, name, house, phone) },
                    enabled = !state.isMutating,
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("save_profile_button")
                ) {
                    Text("Simpan Profil", fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        // Biometric Security Card (Fingerprint / FaceID)
        BiometricSecuritySettingsCard(
            biometricState = biometricState,
            onTogglePortalBiometric = onTogglePortalBiometric,
            onTogglePaymentBiometric = onTogglePaymentBiometric,
            onLockPortalNow = onLockPortalNow,
            onOpenBiometricEnroll = onOpenBiometricEnroll
        )

        // Primary Admin ONLY: System Settings Form (Two-Tier Enforcement)
        if (resident.primaryAdmin) {
            val s = state.settings
            var blokName by remember(s) { mutableStateOf(s.blokName) }
            var subtitle by remember(s) { mutableStateOf(s.subtitle) }
            var danaNumber by remember(s) { mutableStateOf(s.danaNumber) }
            var danaOwner by remember(s) { mutableStateOf(s.danaAccountName) }
            var iuranAmount by remember(s) { mutableStateOf(s.iuranAmount.toString()) }
            var hidmatAmount by remember(s) { mutableStateOf(s.hidmatAmount.toString()) }
            var reminderTitle by remember(s) { mutableStateOf(s.reminderTitle) }
            var reminderMsg by remember(s) { mutableStateOf(s.reminderMessage) }
            var customQris by remember(s) { mutableStateOf(s.customQrisData) }

            val qrisPicker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia()
            ) { uri ->
                if (uri != null) {
                    val encoded = uriToCompressedDataUri(context, uri, maxDim = 512)
                    if (encoded != null) customQris = encoded
                }
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = ac.cardContainer),
                border = BorderStroke(1.dp, ac.borderSubtle),
                modifier = Modifier.testTag("primary_admin_settings_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Pengaturan Blok (Admin Utama)",
                        style = MaterialTheme.typography.titleSmall,
                        color = ac.textPrimary
                    )
                    OutlinedTextField(
                        value = blokName,
                        onValueChange = { blokName = it },
                        label = { Text("Nama Blok") },
                        singleLine = true,
                        colors = adaptiveFieldColors(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = subtitle,
                        onValueChange = { subtitle = it },
                        label = { Text("Subjudul") },
                        singleLine = true,
                        colors = adaptiveFieldColors(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = danaNumber,
                            onValueChange = { danaNumber = it },
                            label = { Text("Nomor DANA") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            colors = adaptiveFieldColors(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = danaOwner,
                            onValueChange = { danaOwner = it },
                            label = { Text("Atas Nama DANA") },
                            singleLine = true,
                            colors = adaptiveFieldColors(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = iuranAmount,
                            onValueChange = { iuranAmount = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Tarif Iuran") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = adaptiveFieldColors(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = hidmatAmount,
                            onValueChange = { hidmatAmount = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Tarif Hidmat") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = adaptiveFieldColors(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = reminderTitle,
                        onValueChange = { reminderTitle = it },
                        label = { Text("Judul Himbauan") },
                        singleLine = true,
                        colors = adaptiveFieldColors(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = reminderMsg,
                        onValueChange = { reminderMsg = it },
                        label = { Text("Pesan Himbauan") },
                        minLines = 2,
                        colors = adaptiveFieldColors(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                qrisPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DanaBlue, contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Filled.QrCode2, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (customQris.isBlank()) "Pilih QRIS" else "Ganti QRIS")
                        }
                        if (customQris.isNotBlank()) {
                            OutlinedButton(
                                onClick = { customQris = "" },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text("Reset QRIS", color = ac.textPrimary)
                            }
                        }
                    }
                    Button(
                        onClick = {
                            onSaveSettings(
                                s.copy(
                                    blokName = blokName,
                                    subtitle = subtitle,
                                    danaNumber = danaNumber,
                                    danaAccountName = danaOwner,
                                    iuranAmount = iuranAmount.toLongOrNull() ?: 50000L,
                                    hidmatAmount = hidmatAmount.toLongOrNull() ?: 25000L,
                                    reminderTitle = reminderTitle,
                                    reminderMessage = reminderMsg,
                                    customQrisData = customQris
                                )
                            )
                        },
                        enabled = !state.isMutating,
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("save_system_settings_button")
                    ) {
                        Text("Simpan Pengaturan Blok", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            // Primary Admin: Manage Delegated Admin Accounts & Invite Codes
            var inviteLabel by remember { mutableStateOf("Bendahara Blok CE") }
            var inviteEmail by remember { mutableStateOf("") }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = ac.cardContainer),
                border = BorderStroke(1.dp, ac.borderSubtle)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AdminPanelSettings,
                            contentDescription = null,
                            tint = ac.emeraldText
                        )
                        Text(
                            text = "Kelola Akses Admin & Pengurus",
                            style = MaterialTheme.typography.titleSmall,
                            color = ac.textPrimary
                        )
                    }
                    Surface(
                        color = ac.emeraldSurface,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, ac.emeraldBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "ADMIN UTAMA",
                                style = MaterialTheme.typography.labelSmall,
                                color = ac.emeraldText,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = BlokCeRepository.BOOTSTRAPPED_ADMIN_EMAIL,
                                style = MaterialTheme.typography.bodyMedium,
                                color = ac.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    OutlinedTextField(
                        value = inviteLabel,
                        onValueChange = { inviteLabel = it },
                        label = { Text("Jabatan / Label Pengurus") },
                        singleLine = true,
                        colors = adaptiveFieldColors(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = inviteEmail,
                        onValueChange = { inviteEmail = it },
                        label = { Text("Email Google Pengurus (opsional)") },
                        placeholder = { Text("Kosongkan agar bisa dipakai 1 akun mana saja") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        colors = adaptiveFieldColors(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            onCreateAdminInvite(inviteLabel, inviteEmail)
                            inviteEmail = ""
                        },
                        enabled = !state.isMutating,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (ac.isDark) DanaBlue else Slate900,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("create_admin_invite_button")
                    ) {
                        Icon(Icons.Filled.Key, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Buat Kode Undangan Admin", fontWeight = FontWeight.ExtraBold)
                    }

                    if (state.adminInvites.isNotEmpty()) {
                        HorizontalDivider(color = ac.divider)
                        Text(
                            text = "Daftar Kode Undangan",
                            style = MaterialTheme.typography.labelLarge,
                            color = ac.textPrimary
                        )
                        state.adminInvites.forEach { invite ->
                            Surface(
                                color = ac.cardElevated,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, ac.borderSubtle),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = invite.code,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = ac.textPrimary,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            text = buildString {
                                                append(invite.label)
                                                if (invite.targetEmail.isNotBlank()) append(" • ${invite.targetEmail}")
                                                append(if (invite.used) " • Terpakai (${invite.usedByEmail})" else " • Siap Dipakai")
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (invite.used) ac.textMuted else ac.emeraldText
                                        )
                                    }
                                    if (!invite.used) {
                                        IconButton(
                                            onClick = { onCopyText("Kode Admin", invite.code) },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("copy_invite_${invite.code}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.ContentCopy,
                                                contentDescription = "Salin kode ${invite.code}",
                                                tint = ac.emeraldText
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { onDeleteAdminInvite(invite.code) },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("delete_invite_${invite.code}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Hapus kode ${invite.code}",
                                            tint = ac.roseText
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (state.delegatedAdmins.isNotEmpty()) {
                        HorizontalDivider(color = ac.divider)
                        Text(
                            text = "Admin / Pengurus Aktif",
                            style = MaterialTheme.typography.labelLarge,
                            color = ac.textPrimary
                        )
                        state.delegatedAdmins.forEach { admin ->
                            Surface(
                                color = ac.cardElevated,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, ac.borderSubtle),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = admin.email,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = ac.textPrimary
                                        )
                                        Text(
                                            text = admin.roleLabel,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ac.textSecondary
                                        )
                                    }
                                    if (admin.userId != resident.id) {
                                        IconButton(
                                            onClick = { onRevokeDelegatedAdmin(admin.userId) },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("revoke_admin_${admin.userId}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = "Cabut akses admin ${admin.email}",
                                                tint = ac.roseText
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (!resident.adminRole) {
            // Resident: Optional Admin Invite Code Activation
            var redeemCode by remember { mutableStateOf("") }
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = ac.cardContainer),
                border = BorderStroke(1.dp, ac.borderSubtle)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AdminPanelSettings,
                            contentDescription = null,
                            tint = ac.textPrimary
                        )
                        Text(
                            text = "Aktivasi Akses Admin / Pengurus",
                            style = MaterialTheme.typography.titleSmall,
                            color = ac.textPrimary
                        )
                    }
                    Text(
                        text = "Punya kode undangan dari Admin Utama (${BlokCeRepository.BOOTSTRAPPED_ADMIN_EMAIL})? Masukkan di sini untuk mengaktifkan akses pengurus.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ac.textSecondary
                    )
                    OutlinedTextField(
                        value = redeemCode,
                        onValueChange = { redeemCode = it.uppercase() },
                        label = { Text("Kode Undangan Admin") },
                        placeholder = { Text("Contoh: CE-ADM-XXXX") },
                        singleLine = true,
                        colors = adaptiveFieldColors(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("redeem_admin_code_input")
                    )
                    Button(
                        onClick = {
                            onRedeemAdminInvite(redeemCode)
                            redeemCode = ""
                        },
                        enabled = redeemCode.trim().length >= 6 && !state.isMutating,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (ac.isDark) DanaBlue else Slate900,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("redeem_admin_code_button")
                    ) {
                        Icon(Icons.Filled.Key, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Aktifkan Akses Admin", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        // Cloud & Offline SQLite Status Card + Sign Out
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ac.cardContainer),
            border = BorderStroke(1.dp, ac.borderSubtle)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Cloud Firestore", style = MaterialTheme.typography.bodySmall, color = ac.textSecondary)
                    Text(
                        text = if (state.isOnline) "● Terhubung" else "● Offline",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.isOnline) ac.emeraldText else ac.amberText,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Cache SQLite (Room)", style = MaterialTheme.typography.bodySmall, color = ac.textSecondary)
                    Text(
                        text = "● Aktif",
                        style = MaterialTheme.typography.bodySmall,
                        color = ac.emeraldText,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Antrean Offline", style = MaterialTheme.typography.bodySmall, color = ac.textSecondary)
                    Text(
                        text = "${state.pendingOfflineCount} transaksi",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.pendingOfflineCount > 0) ac.amberText else ac.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (state.pendingOfflineCount > 0 && state.isOnline) {
                    OutlinedButton(
                        onClick = onFlushOfflineQueue,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Filled.Sync, null, modifier = Modifier.size(16.dp), tint = ac.emeraldText)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sinkronkan Antrean Sekarang", color = ac.textPrimary)
                    }
                }

                Button(
                    onClick = onSignOut,
                    colors = ButtonDefaults.buttonColors(containerColor = Rose600, contentColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Keluar Akun", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun BiometricSecuritySettingsCard(
    biometricState: BiometricSecurityState,
    onTogglePortalBiometric: (Boolean) -> Unit,
    onTogglePaymentBiometric: (Boolean) -> Unit,
    onLockPortalNow: () -> Unit,
    onOpenBiometricEnroll: () -> Unit
) {
    val ac = adaptiveColors
    val isAvailable = biometricState.availability == BiometricAvailability.AVAILABLE
    val (statusLabel, statusBg, statusFg) = when {
        isAvailable && biometricState.portalLockEnabled -> Triple("Aktif", ac.emeraldSurface, ac.emeraldText)
        isAvailable -> Triple("Siap Diaktifkan", ac.emeraldSurface, ac.emeraldText)
        biometricState.availability == BiometricAvailability.NONE_ENROLLED -> Triple("Belum Terdaftar", ac.amberSurface, ac.amberText)
        else -> Triple("Tidak Didukung", ac.subtleContainer, ac.textMuted)
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ac.cardContainer),
        border = BorderStroke(1.dp, ac.borderSubtle),
        modifier = Modifier.testTag("biometric_security_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = ac.emeraldSurface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Fingerprint,
                                contentDescription = null,
                                tint = ac.emeraldText,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Keamanan Biometrik",
                            style = MaterialTheme.typography.titleSmall,
                            color = ac.textPrimary
                        )
                        Text(
                            text = "Sidik Jari / Pengenalan Wajah",
                            style = MaterialTheme.typography.bodySmall,
                            color = ac.textSecondary
                        )
                    }
                }
                Surface(
                    color = statusBg,
                    shape = CircleShape
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusFg,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            if (biometricState.availability == BiometricAvailability.NONE_ENROLLED) {
                Surface(
                    color = ac.cardElevated,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ac.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Perangkat mendukung sensor biometrik, namun belum ada sidik jari atau wajah yang didaftarkan di Setelan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ac.textSecondary
                        )
                        OutlinedButton(
                            onClick = onOpenBiometricEnroll,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("enroll_biometric_button")
                        ) {
                            Icon(Icons.Filled.Settings, null, modifier = Modifier.size(16.dp), tint = ac.emeraldText)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Daftarkan Sidik Jari / Wajah", fontWeight = FontWeight.Bold, color = ac.textPrimary)
                        }
                    }
                }
            } else if (biometricState.availability == BiometricAvailability.NO_HARDWARE ||
                biometricState.availability == BiometricAvailability.TEMPORARILY_UNAVAILABLE
            ) {
                Text(
                    text = if (biometricState.availability == BiometricAvailability.TEMPORARILY_UNAVAILABLE) {
                        "Sensor biometrik sedang sibuk atau belum tersedia sementara."
                    } else {
                        "Perangkat ini tidak memiliki sensor sidik jari atau wajah yang kompatibel."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = ac.textSecondary
                )
            }

            HorizontalDivider(color = ac.divider)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Kunci Portal Warga",
                        style = MaterialTheme.typography.labelLarge,
                        color = ac.textPrimary
                    )
                    Text(
                        text = "Minta sidik jari / wajah saat membuka aplikasi atau kembali dari latar belakang.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ac.textSecondary
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Switch(
                    checked = biometricState.portalLockEnabled,
                    onCheckedChange = onTogglePortalBiometric,
                    enabled = isAvailable || biometricState.portalLockEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Emerald700
                    ),
                    modifier = Modifier.testTag("toggle_biometric_portal_lock")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Verifikasi Saat Pembayaran",
                        style = MaterialTheme.typography.labelLarge,
                        color = ac.textPrimary
                    )
                    Text(
                        text = "Konfirmasi biometrik sebelum mengirim bukti pembayaran iuran.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ac.textSecondary
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Switch(
                    checked = biometricState.requireOnPayment,
                    onCheckedChange = onTogglePaymentBiometric,
                    enabled = isAvailable || biometricState.requireOnPayment,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Emerald700
                    ),
                    modifier = Modifier.testTag("toggle_biometric_payment_confirm")
                )
            }

            if (isAvailable && biometricState.portalLockEnabled) {
                OutlinedButton(
                    onClick = onLockPortalNow,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ac.emeraldBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("lock_portal_now_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = ac.emeraldText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Kunci Portal Sekarang",
                        color = ac.emeraldText,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}
