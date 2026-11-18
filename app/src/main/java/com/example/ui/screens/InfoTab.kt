package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Announcement
import com.example.ui.components.AnnouncementImage
import com.example.ui.components.StaggeredEntrance
import com.example.ui.components.uriToCompressedDataUri
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Emerald700
import com.example.ui.theme.adaptiveColors
import com.example.ui.theme.adaptiveFieldColors
import com.example.ui.viewmodel.MainUiState

@Composable
fun InfoTab(
    state: MainUiState,
    onSaveAnnouncement: (
        existingId: String?,
        title: String,
        category: String,
        imageUrl: String,
        content: String,
        urgent: Boolean
    ) -> Unit,
    onDeleteAnnouncement: (String) -> Unit
) {
    val isAdmin = state.currentResident?.adminRole == true
    val ac = adaptiveColors
    var editingAnnouncement by remember { mutableStateOf<Announcement?>(null) }
    var showForm by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Informasi Warga",
                style = MaterialTheme.typography.titleMedium,
                color = ac.textPrimary
            )
            if (isAdmin) {
                Button(
                    onClick = {
                        editingAnnouncement = null
                        showForm = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("add_announcement_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tulis", fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        if (state.announcements.isEmpty()) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = ac.cardContainer),
                border = BorderStroke(1.dp, ac.borderSubtle)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Newspaper,
                        contentDescription = null,
                        tint = ac.textMuted,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "Belum ada informasi warga",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ac.textMuted
                    )
                }
            }
        } else {
            state.announcements.forEachIndexed { idx, item ->
                StaggeredEntrance(delayMillis = (idx * 45).coerceAtMost(220)) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = ac.cardContainer),
                    border = BorderStroke(1.dp, ac.borderSubtle)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (item.imageUrl.isNotBlank()) {
                            AnnouncementImage(
                                imageUrl = item.imageUrl,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (item.urgent) {
                                Surface(
                                    color = ac.roseSurface,
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, ac.roseBorder)
                                ) {
                                    Text(
                                        text = "PENTING",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ac.roseText,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Surface(
                                color = ac.subtleContainer,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = item.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ac.textSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = ac.textPrimary
                        )
                        Text(
                            text = item.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ac.textSecondary
                        )

                        HorizontalDivider(color = ac.divider)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${item.displayDate} • ${item.authorName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ac.textMuted,
                                modifier = Modifier.weight(1f)
                            )
                            if (isAdmin) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        color = ac.danaSurface,
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                editingAnnouncement = item
                                                showForm = true
                                            },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("edit_announcement_${item.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Edit,
                                                contentDescription = "Edit pengumuman",
                                                tint = ac.danaText,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Surface(
                                        color = ac.roseSurface,
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        IconButton(
                                            onClick = { onDeleteAnnouncement(item.id) },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("delete_announcement_${item.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = "Hapus pengumuman",
                                                tint = ac.roseText,
                                                modifier = Modifier.size(18.dp)
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
        }
    }

    if (showForm) {
        AnnouncementFormDialog(
            existing = editingAnnouncement,
            onDismiss = { showForm = false },
            onSubmit = { title, category, imageUrl, content, urgent ->
                onSaveAnnouncement(editingAnnouncement?.id, title, category, imageUrl, content, urgent)
                showForm = false
            }
        )
    }
}

@Composable
private fun AnnouncementFormDialog(
    existing: Announcement?,
    onDismiss: () -> Unit,
    onSubmit: (title: String, category: String, imageUrl: String, content: String, urgent: Boolean) -> Unit
) {
    val context = LocalContext.current
    val ac = adaptiveColors
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: "Pengumuman") }
    var imageUrl by remember { mutableStateOf(existing?.imageUrl ?: "") }
    var content by remember { mutableStateOf(existing?.content ?: "") }
    var urgent by remember { mutableStateOf(existing?.urgent ?: false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val encoded = uriToCompressedDataUri(context, uri, maxDim = 640)
            if (encoded != null) imageUrl = encoded
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = ac.cardContainer),
            border = BorderStroke(1.dp, ac.borderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (existing != null) "Edit Info" else "Tulis Info Baru",
                        style = MaterialTheme.typography.titleMedium,
                        color = ac.textPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Tutup form",
                            tint = ac.textMuted
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul") },
                    singleLine = true,
                    colors = adaptiveFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("announcement_title_input")
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Kategori") },
                    singleLine = true,
                    colors = adaptiveFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Image,
                        contentDescription = null,
                        tint = ac.emeraldText,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (imageUrl.isBlank()) "Pilih Foto (Opsional)" else "Foto Terpasang — Ganti",
                        color = ac.textPrimary
                    )
                }

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Isi Pengumuman") },
                    minLines = 3,
                    colors = adaptiveFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("announcement_content_input")
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = urgent,
                        onCheckedChange = { urgent = it }
                    )
                    Text(
                        text = "Tandai Penting (Picu Notifikasi Device)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ac.textPrimary
                    )
                }

                Button(
                    onClick = { onSubmit(title, category, imageUrl, content, urgent) },
                    enabled = title.isNotBlank() && content.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("submit_announcement_button")
                ) {
                    Text(
                        text = if (existing != null) "Simpan Perubahan" else "Publikasikan",
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}
