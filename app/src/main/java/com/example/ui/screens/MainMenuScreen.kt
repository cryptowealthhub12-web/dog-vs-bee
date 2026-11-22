package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.GameCatalog
import com.example.data.Localization
import com.example.model.DogMood
import com.example.model.SaveProgress
import com.example.ui.components.drawCartoonDog
import com.example.ui.theme.BrickBrown
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.CreamPattern
import com.example.ui.theme.DeepText
import com.example.ui.theme.GoldStar
import com.example.ui.theme.GrassGreen
import com.example.ui.theme.HoneyDark
import com.example.ui.theme.HoneyLight
import com.example.ui.theme.HoneyPrimary
import com.example.ui.theme.InkBlack
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MainMenuScreen(
  progress: SaveProgress,
  onPlayContinue: () -> Unit,
  onOpenLevels: () -> Unit,
  onOpenSkins: () -> Unit,
  onOpenSettings: () -> Unit,
  onToggleSound: () -> Unit,
  onToggleMusic: () -> Unit,
) {
  val str = Localization.get(progress.settings.language)
  val skin = GameCatalog.findSkin(progress.selectedSkinId)
  val infinite = rememberInfiniteTransition(label = "menu_anim")
  val animTime by
    infinite.animateFloat(
      initialValue = 0f,
      targetValue = (2f * PI).toFloat(),
      animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart),
      label = "menu_phase",
    )

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Brush.verticalGradient(listOf(CreamBackground, Color(0xFFFFEFC6))))
  ) {
    // Decorative cartoon meadow footer
    Canvas(modifier = Modifier.fillMaxSize()) {
      val groundY = size.height * 0.85f
      drawRect(color = BrickBrown, topLeft = Offset(0f, groundY), size = Size(size.width, size.height - groundY))
      drawRect(color = GrassGreen, topLeft = Offset(0f, groundY - 12f), size = Size(size.width, 22f))
      // Soft decorative bricks
      drawRoundRect(CreamPattern, Offset(size.width * 0.08f, size.height * 0.16f), Size(70f, 24f), CornerRadius(8f, 8f))
      drawRoundRect(CreamPattern, Offset(size.width * 0.72f, size.height * 0.22f), Size(84f, 24f), CornerRadius(8f, 8f))
    }

    Column(
      modifier =
        Modifier.fillMaxSize()
          .safeDrawingPadding()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 14.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Top Stats & Audio Quick Bar
      Row(
        modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          StatPill(
            icon = Icons.Rounded.MonetizationOn,
            iconTint = HoneyPrimary,
            text = "${progress.coins}",
          )
          StatPill(
            icon = Icons.Rounded.Star,
            iconTint = GoldStar,
            text = "${progress.totalStars}",
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Surface(
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 3.dp,
            modifier = Modifier.size(48.dp),
          ) {
            IconButton(onClick = onToggleSound, modifier = Modifier.testTag("menu_sound_toggle")) {
              Icon(
                imageVector =
                  if (progress.settings.soundEnabled) Icons.AutoMirrored.Rounded.VolumeUp
                  else Icons.AutoMirrored.Rounded.VolumeOff,
                contentDescription = str.soundFx,
                tint = DeepText,
              )
            }
          }
          Surface(
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 3.dp,
            modifier = Modifier.size(48.dp),
          ) {
            IconButton(onClick = onToggleMusic, modifier = Modifier.testTag("menu_music_toggle")) {
              Icon(
                imageVector =
                  if (progress.settings.musicEnabled) Icons.Rounded.MusicNote
                  else Icons.Rounded.MusicOff,
                contentDescription = str.bgMusic,
                tint = DeepText,
              )
            }
          }
          Surface(
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 3.dp,
            modifier = Modifier.size(48.dp),
          ) {
            IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("menu_settings_button")) {
              Icon(Icons.Rounded.Settings, contentDescription = str.settings, tint = DeepText)
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // Game Title Logo Badge
      Surface(
        shape = RoundedCornerShape(28.dp),
        color = HoneyLight,
        shadowElevation = 6.dp,
        modifier = Modifier.border(3.dp, HoneyDark, RoundedCornerShape(28.dp)),
      ) {
        Column(
          modifier = Modifier.padding(horizontal = 28.dp, vertical = 14.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Rounded.Pets,
              contentDescription = null,
              tint = HoneyDark,
              modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = str.gameTitle,
              style = MaterialTheme.typography.headlineLarge,
              color = DeepText,
              fontWeight = FontWeight.ExtraBold,
            )
          }
          Text(
            text = str.subtitle,
            style = MaterialTheme.typography.titleMedium,
            color = HoneyDark,
            textAlign = TextAlign.Center,
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Animated Mascot Canvas (Dog inside drawn shield + orbiting angry bee)
      Box(
        modifier = Modifier.size(210.dp),
        contentAlignment = Alignment.Center,
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val cx = size.width * 0.5f
          val cy = size.height * 0.56f
          val bob = sin(animTime * 2f) * 7f

          // Protective ink dome around dog
          drawArc(
            color = InkBlack,
            startAngle = 195f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(cx - 82f, cy - 84f),
            size = Size(164f, 168f),
            style = Stroke(width = 11f, cap = StrokeCap.Round),
          )

          val blinking = sin(animTime * 1.5f) > 0.96f
          drawCartoonDog(
            center = Offset(cx, cy + bob),
            radius = 48f,
            skin = skin,
            mood = DogMood.HAPPY,
            isBlinking = blinking,
            elapsed = animTime,
          )

          // Orbiting tiny cartoon bee
          val bx = cx + cos(animTime) * 92f
          val by = cy - 42f + sin(animTime * 2f) * 24f
          drawOval(Color(0xFFFFD600), Offset(bx - 13f, by - 9f), Size(26f, 18f))
          drawLine(InkBlack, Offset(bx - 3f, by - 8f), Offset(bx - 3f, by + 8f), strokeWidth = 3.5f)
          drawLine(InkBlack, Offset(bx + 4f, by - 8f), Offset(bx + 4f, by + 8f), strokeWidth = 3.5f)
          drawOval(InkBlack, Offset(bx - 13f, by - 9f), Size(26f, 18f), style = Stroke(width = 2.2f))
          drawCircle(Color.White.copy(alpha = 0.85f), 6f, Offset(bx - 2f, by - 13f))
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Primary PLAY Button
      Button(
        onClick = onPlayContinue,
        modifier =
          Modifier.fillMaxWidth()
            .widthIn(max = 360.dp)
            .height(66.dp)
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .testTag("play_button"),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = GrassGreen, contentColor = Color.White),
      ) {
        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(34.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.Start) {
          Text(str.play, style = MaterialTheme.typography.headlineMedium, fontSize = 22.sp)
          Text(
            str.continueFromLevel(progress.unlockedLevel),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.92f),
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Secondary action buttons row
      Row(
        modifier = Modifier.fillMaxWidth().widthIn(max = 360.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        FilledTonalButton(
          onClick = onOpenLevels,
          modifier = Modifier.weight(1f).height(56.dp).testTag("levels_button"),
          shape = RoundedCornerShape(20.dp),
          colors =
            ButtonDefaults.filledTonalButtonColors(
              containerColor = HoneyPrimary,
              contentColor = Color.White,
            ),
        ) {
          Icon(Icons.Rounded.Apps, contentDescription = null)
          Spacer(modifier = Modifier.width(6.dp))
          Text(str.levels, style = MaterialTheme.typography.labelLarge)
        }

        FilledTonalButton(
          onClick = onOpenSkins,
          modifier = Modifier.weight(1f).height(56.dp).testTag("skins_button"),
          shape = RoundedCornerShape(20.dp),
          colors =
            ButtonDefaults.filledTonalButtonColors(
              containerColor = Color.White,
              contentColor = DeepText,
            ),
        ) {
          Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = HoneyPrimary)
          Spacer(modifier = Modifier.width(6.dp))
          Text(str.skinsAndTrophies, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // Developer Credit Badge
      Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.88f),
        shadowElevation = 2.dp,
        modifier = Modifier.border(1.5.dp, HoneyDark.copy(alpha = 0.45f), RoundedCornerShape(50)),
      ) {
        Text(
          text = stringResource(R.string.powered_by_developer),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = 0.8.sp,
          color = HoneyDark,
          modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp),
        )
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
fun StatPill(icon: androidx.compose.ui.graphics.vector.ImageVector, iconTint: Color, text: String) {
  Surface(
    shape = RoundedCornerShape(50),
    color = Color.White,
    shadowElevation = 3.dp,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
      Spacer(modifier = Modifier.width(5.dp))
      Text(text, style = MaterialTheme.typography.titleMedium, color = DeepText)
    }
  }
}
