package cr.ac.una.glifo.feature.profile.presentation

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.glifo.core.data.dummy.DummyAiCostProvider
import cr.ac.una.glifo.core.data.dummy.DummyCourseProvider
import cr.ac.una.glifo.core.data.dummy.DummyNoteProvider
import cr.ac.una.glifo.core.data.session.SessionManager
import cr.ac.una.glifo.core.ui.component.*
import cr.ac.una.glifo.core.ui.theme.GlifoTheme

@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
    onNavigateToCourses: () -> Unit,
    onNavigateToAiCost: () -> Unit,
    onLogout: () -> Unit
) {
    val colors = GlifoTheme.colors
    val context = LocalContext.current
    val user = SessionManager.currentUser
    val role = user?.role ?: GlifoRole.Student
    val roleLabel = when (role) {
        GlifoRole.Teacher -> "DOCENTE"
        GlifoRole.Admin -> "ADMIN"
        GlifoRole.Student -> "ESTUDIANTE"
    }
    val pagesCaptured = DummyNoteProvider.getNoteMetaList().sumOf { it.pageCount }
    val resolvedPercent = DummyAiCostProvider.getStats(role).headlinePercent
    val courseCount = when (role) {
        GlifoRole.Teacher -> DummyCourseProvider.getTeacherCourses.size
        GlifoRole.Admin -> (DummyCourseProvider.getCourses + DummyCourseProvider.getTeacherCourses).distinctBy { it.id }.size
        GlifoRole.Student -> DummyCourseProvider.getCourses.size
    }

    val photoUri = SessionManager.profilePhotoUri
    val avatarBitmap = remember(photoUri) {
        photoUri?.let { uri ->
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            }.getOrNull()?.asImageBitmap()
        }
    }
    // Test-only photo picker: nothing is uploaded, it's just kept for this session
    // (Android's built-in photo picker, no runtime permission needed).
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) SessionManager.setProfilePhoto(uri) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        GlifoTopAppBar(
            title = "Perfil",
            isBack = true,
            onNavigationClick = onNavigateBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(14.dp)
        ) {
            // Profile Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceHigh)
                            .border(1.dp, colors.border, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarBitmap != null) {
                            Image(
                                bitmap = avatarBitmap,
                                contentDescription = "Foto de perfil",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(user?.initials ?: "BB", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = colors.accentText)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(colors.accent)
                            .border(2.dp, colors.background, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Cambiar foto de perfil",
                            tint = colors.onAccent,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(user?.fullName ?: "Brandon Brenes", fontSize = 20.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                Text(user?.email ?: "estudiante@est.una.ac.cr", fontSize = 13.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
                GlifoBadge(text = roleLabel)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Two stat cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlifoCard(modifier = Modifier.weight(1f).height(80.dp)) {
                    Text("$pagesCaptured", fontSize = 20.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("Páginas capturadas", fontSize = 11.sp, color = colors.textSecondary)
                }
                GlifoCard(modifier = Modifier.weight(1f).height(80.dp)) {
                    Text("$resolvedPercent%", fontSize = 20.sp, fontWeight = FontWeight.Medium, color = colors.verified)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("Resueltas sin llamadas de pago", fontSize = 11.sp, color = colors.textSecondary, lineHeight = 14.sp)
                }
            }

            Text(
                text = "CUENTA",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
            )

            GlifoCard(tight = true) {
                GlifoItem(compact = true, onClick = onNavigateToChangePassword) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                    Text("Cambiar contraseña", fontSize = 14.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(15.dp))
                }
                GlifoItem(compact = true, onClick = onNavigateToCourses) {
                    Icon(Icons.Default.School, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                    Text("Mis cursos", fontSize = 14.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    Text("$courseCount", fontSize = 12.sp, color = colors.textSecondary)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(15.dp))
                }
                GlifoItem(compact = true, showBottomBorder = false, onClick = onNavigateToAiCost) {
                    Icon(Icons.Default.BarChart, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                    Text("Mi consumo de IA", fontSize = 14.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(15.dp))
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            GlifoButton(
                text = "Cerrar sesión",
                variant = GlifoButtonVariant.Danger,
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                onClick = onLogout
            )
        }
    }
}
