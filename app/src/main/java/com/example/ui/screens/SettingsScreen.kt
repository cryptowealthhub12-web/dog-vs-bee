package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.GameCatalog
import com.example.data.Localization
import com.example.model.AppLanguage
import com.example.model.SaveProgress
import com.example.ui.components.drawCartoonPen
import com.example.ui.theme.AlarmRed
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.DeepText
import com.example.ui.theme.HoneyPrimary

@Composable
fun SettingsScreen(
  progress: SaveProgress,
  onToggleSound: () -> Unit,
  onToggleMusic: () -> Unit,
  onToggleVibration: () -> Unit,
  onToggleHighGraphics: () -> Unit,
  onSelectLanguage: (AppLanguage) -> Unit,
  onSelectPen: (String) -> Unit,
  onResetProgress: () -> Unit,
  onBack: () -> Unit,
) {
  val str = Localization.get(progress.settings.language)
  var showResetDialog by remember { mutableStateOf(false) }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(CreamBackground)
        .safeDrawingPadding()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 10.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(shape = CircleShape, color = Color.White, shadowElevation = 3.dp) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
          Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = DeepText)
        }
      }
      Spacer(modifier = Modifier.width(12.dp))
      Text(str.settings, style = MaterialTheme.typography.headlineMedium, color = DeepText)
    }

    Spacer(modifier = Modifier.height(16.dp))

    Column(
      modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      SettingSwitchRow(
        icon = Icons.AutoMirrored.Rounded.VolumeUp,
        label = str.soundFx,
        checked = progress.settings.soundEnabled,
        testTag = "setting_sound_switch",
        onToggle = onToggleSound,
      )
      SettingSwitchRow(
        icon = Icons.Rounded.MusicNote,
        label = str.bgMusic,
        checked = progress.settings.musicEnabled,
        testTag = "setting_music_switch",
        onToggle = onToggleMusic,
      )
      SettingSwitchRow(
        icon = Icons.Rounded.Vibration,
        label = str.vibration,
        checked = progress.settings.vibrationEnabled,
        testTag = "setting_vibration_switch",
        onToggle = onToggleVibration,
      )
      SettingSwitchRow(
        icon = Icons.Rounded.HighQuality,
        label = str.highGraphics,
        checked = progress.settings.highGraphics,
        testTag = "setting_graphics_switch",
        onToggle = onToggleHighGraphics,
      )

      // Language selector card
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Language, contentDescription = null, tint = HoneyPrimary)
            Spacer(modifier = Modifier.width(10.dp))
            Text(str.languageLabel, style = MaterialTheme.typography.titleMedium, color = DeepText)
          }
          Spacer(modifier = Modifier.height(10.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppLanguage.entries.forEach { lang ->
              FilterChip(
                selected = progress.settings.language == lang,
                onClick = { onSelectLanguage(lang) },
                label = { Text(lang.label) },
                colors =
                  FilterChipDefaults.filterChipColors(
                    selectedContainerColor = HoneyPrimary,
                    selectedLabelColor = Color.White,
                  ),
              )
            }
          }
        }
      }

      // Pen Model selector card (Red, Green, Yellow, Black)
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Brush, contentDescription = null, tint = HoneyPrimary)
            Spacer(modifier = Modifier.width(10.dp))
            Text(str.penModelsLabel, style = MaterialTheme.typography.titleMedium, color = DeepText)
          }
          Spacer(modifier = Modifier.height(12.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            GameCatalog.penModels.forEach { pen ->
              val selected = progress.settings.selectedPenId == pen.id
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (selected) pen.accentColor.copy(alpha = 0.45f) else CreamBackground,
                modifier =
                  Modifier.weight(1f)
                    .height(92.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                      width = if (selected) 2.5.dp else 1.dp,
                      color = if (selected) pen.barrelColor else Color(0xFFE0D4BC),
                      shape = RoundedCornerShape(16.dp),
                    )
                    .clickable { onSelectPen(pen.id) }
                    .testTag("pen_model_${pen.id}"),
              ) {
                Box(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                  if (selected) {
                    Icon(
                      imageVector = Icons.Rounded.CheckCircle,
                      contentDescription = "Selected",
                      tint = pen.barrelColor,
                      modifier = Modifier.size(18.dp).align(Alignment.TopEnd),
                    )
                  }
                  Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                  ) {
                    Canvas(modifier = Modifier.size(44.dp)) {
                      drawCartoonPen(
                        tip = Offset(size.width * 0.28f, size.height * 0.86f),
                        pen = pen,
                        elapsed = 0f,
                        length = size.minDimension * 0.88f,
                        tiltDegrees = 38f,
                      )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = pen.name,
                      style = MaterialTheme.typography.labelLarge,
                      fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                      color = DeepText,
                    )
                  }
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Button(
        onClick = { showResetDialog = true },
        modifier = Modifier.fillMaxWidth().height(54.dp).testTag("reset_progress_button"),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AlarmRed, contentColor = Color.White),
      ) {
        Icon(Icons.Rounded.DeleteForever, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(str.resetProgress, style = MaterialTheme.typography.titleMedium)
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = stringResource(R.string.powered_by_developer),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        color = DeepText.copy(alpha = 0.65f),
        modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 12.dp),
      )
    }
  }

  if (showResetDialog) {
    AlertDialog(
      onDismissRequest = { showResetDialog = false },
      title = { Text(str.resetConfirmTitle) },
      text = { Text(str.resetConfirmBody) },
      confirmButton = {
        TextButton(
          onClick = {
            showResetDialog = false
            onResetProgress()
          }
        ) {
          Text(str.confirm, color = AlarmRed)
        }
      },
      dismissButton = {
        TextButton(onClick = { showResetDialog = false }) { Text(str.cancel) }
      },
    )
  }
}

@Composable
private fun SettingSwitchRow(
  icon: ImageVector,
  label: String,
  checked: Boolean,
  testTag: String,
  onToggle: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(20.dp),
    color = Color.White,
    shadowElevation = 2.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = HoneyPrimary)
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.titleMedium, color = DeepText)
      }
      Switch(
        checked = checked,
        onCheckedChange = { onToggle() },
        modifier = Modifier.testTag(testTag),
      )
    }
  }
}
