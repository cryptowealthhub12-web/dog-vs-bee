package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameCatalog
import com.example.data.Localization
import com.example.engine.SimulationSnapshot
import com.example.model.FailReason
import com.example.model.GamePhase
import com.example.model.SaveProgress
import com.example.model.Vec2
import com.example.ui.components.computeViewport
import com.example.ui.components.renderGameWorld
import com.example.ui.theme.AlarmRed
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.DeepText
import com.example.ui.theme.GoldStar
import com.example.ui.theme.GrassGreen
import com.example.ui.theme.HoneyDark
import com.example.ui.theme.HoneyLight
import com.example.ui.theme.HoneyPrimary
import kotlin.math.ceil
import kotlin.math.sin

@Composable
fun GameScreen(
  snapshot: SimulationSnapshot,
  progress: SaveProgress,
  bannerMessage: String?,
  onTick: (Float) -> Unit,
  onBeginDraw: (Vec2) -> Unit,
  onContinueDraw: (Vec2) -> Unit,
  onFinishDraw: () -> Unit,
  onPause: () -> Unit,
  onResume: () -> Unit,
  onRestart: () -> Unit,
  onNextLevel: () -> Unit,
  onToggleSound: () -> Unit,
  onOpenLevels: () -> Unit,
  onOpenMainMenu: () -> Unit,
  onDismissBanner: () -> Unit,
) {
  val str = Localization.get(progress.settings.language)
  val skin = remember(progress.selectedSkinId) { GameCatalog.findSkin(progress.selectedSkinId) }
  val pen = remember(progress.settings.selectedPenId) { GameCatalog.findPenModel(progress.settings.selectedPenId) }
  var canvasSize by remember { mutableStateOf(IntSize(600, 820)) }
  val currentPhase by androidx.compose.runtime.rememberUpdatedState(snapshot.phase)

  // 60fps simulation frame ticker (skips redundant ticks while paused or lost)
  LaunchedEffect(snapshot.level.id) {
    var lastNanos = 0L
    while (true) {
      withFrameNanos { nanos ->
        if (lastNanos != 0L && currentPhase != GamePhase.PAUSED && currentPhase != GamePhase.LOST) {
          val dt = (nanos - lastNanos) / 1_000_000_000f
          onTick(dt)
        }
        lastNanos = nanos
      }
    }
  }

  // Auto-dismiss achievement banner
  LaunchedEffect(bannerMessage) {
    if (bannerMessage != null) {
      kotlinx.coroutines.delay(2800L)
      onDismissBanner()
    }
  }

  Column(
    modifier = Modifier.fillMaxSize().background(CreamBackground).navigationBarsPadding(),
  ) {
    // Top HUD Bar (seamless cream style matching Save The Dog, separated above canvas!)
    Surface(
      color = CreamBackground,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 12.dp, vertical = 6.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          // Left Action Buttons: Pause, Restart, Sound
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            HudCircleButton(
              icon = Icons.Rounded.Pause,
              contentDescription = str.paused,
              testTag = "hud_pause_button",
              onClick = onPause,
            )
            HudCircleButton(
              icon = Icons.Rounded.Refresh,
              contentDescription = str.restart,
              testTag = "hud_restart_button",
              onClick = onRestart,
            )
            HudCircleButton(
              icon =
                if (progress.settings.soundEnabled) Icons.AutoMirrored.Rounded.VolumeUp
                else Icons.AutoMirrored.Rounded.VolumeOff,
              contentDescription = str.soundFx,
              testTag = "hud_sound_button",
              onClick = onToggleSound,
            )
          }

          // Center Level Title (viral brown cartoon header style)
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = str.levelLabel(snapshot.level.id),
              style = MaterialTheme.typography.headlineMedium,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 22.sp,
              color = Color(0xFF6B3E1E),
            )
            Text(
              text = snapshot.level.title,
              style = MaterialTheme.typography.labelSmall,
              color = HoneyDark,
            )
          }

          // Right Signature Red Twin-Bell Alarm Clock Timer
          val displaySecs = ceil(snapshot.timeLeft).toInt().coerceIn(0, 10)
          val urgent = snapshot.phase == GamePhase.SIMULATING && snapshot.timeLeft <= 3.0f
          val shakeDeg = if (urgent) sin(snapshot.elapsed * 32f) * 9f else 0f
          Box(
            modifier = Modifier.size(58.dp).testTag("countdown_timer"),
            contentAlignment = Alignment.Center,
          ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
              rotate(shakeDeg, pivot = center) {
                val cx = size.width * 0.5f
                val cy = size.height * 0.54f
                val r = size.minDimension * 0.36f
                // Twin alarm bell ears
                drawCircle(AlarmRed, r * 0.36f, Offset(cx - r * 0.72f, cy - r * 0.78f))
                drawCircle(AlarmRed, r * 0.36f, Offset(cx + r * 0.72f, cy - r * 0.78f))
                // Clock feet
                drawLine(AlarmRed, Offset(cx - r * 0.5f, cy + r * 0.8f), Offset(cx - r * 0.82f, cy + r * 1.12f), strokeWidth = 4.5f, cap = StrokeCap.Round)
                drawLine(AlarmRed, Offset(cx + r * 0.5f, cy + r * 0.8f), Offset(cx + r * 0.82f, cy + r * 1.12f), strokeWidth = 4.5f, cap = StrokeCap.Round)
                // Clock body
                drawCircle(if (urgent) AlarmRed else Color.White, r, Offset(cx, cy))
                drawCircle(AlarmRed, r, Offset(cx, cy), style = Stroke(width = 5f))
              }
            }
            Text(
              text = "$displaySecs",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.ExtraBold,
              color = if (urgent) Color.White else AlarmRed,
              modifier = Modifier.padding(top = 4.dp),
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Ink Budget Meter with 3-Star Threshold Pips
        val inkFrac = ((snapshot.inkBudget - snapshot.inkUsed) / snapshot.inkBudget).coerceIn(0f, 1f)
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Rounded.Brush,
            contentDescription = str.inkLabel,
            tint = pen.inkColor,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Box(modifier = Modifier.weight(1f).height(14.dp), contentAlignment = Alignment.Center) {
            LinearProgressIndicator(
              progress = { inkFrac },
              modifier =
                Modifier.fillMaxWidth()
                  .height(10.dp)
                  .clip(RoundedCornerShape(50))
                  .testTag("ink_meter"),
              color =
                when {
                  inkFrac > 0.5f -> GrassGreen
                  inkFrac > 0.22f -> HoneyPrimary
                  else -> AlarmRed
                },
              trackColor = Color(0xFFEBDDC1),
            )
            // 2-star and 3-star threshold star pips
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Spacer(modifier = Modifier.weight(0.22f))
              Icon(
                Icons.Rounded.Star,
                contentDescription = null,
                tint = if (inkFrac >= 0.22f) GoldStar else Color(0xFFBDB097),
                modifier = Modifier.size(14.dp),
              )
              Spacer(modifier = Modifier.weight(0.28f))
              Icon(
                Icons.Rounded.Star,
                contentDescription = null,
                tint = if (inkFrac >= 0.50f) GoldStar else Color(0xFFBDB097),
                modifier = Modifier.size(14.dp),
              )
              Spacer(modifier = Modifier.weight(0.50f))
            }
          }
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "${(inkFrac * 100).toInt()}%",
            style = MaterialTheme.typography.labelLarge,
            color = DeepText,
          )
        }
      }
    }

    // Playable Interactive Physics Canvas
    Box(
      modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds(),
    ) {
      Canvas(
        modifier =
          Modifier.fillMaxSize()
            .onSizeChanged { canvasSize = it }
            .pointerInput(snapshot.level.id) {
              awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                down.consume()
                val vp = computeViewport(canvasSize.width.toFloat(), canvasSize.height.toFloat())
                onBeginDraw(vp.screenToWorld(down.position))
                while (true) {
                  val event = awaitPointerEvent()
                  val change = event.changes.firstOrNull() ?: break
                  if (!change.pressed) {
                    change.consume()
                    onFinishDraw()
                    break
                  }
                  change.consume()
                  onContinueDraw(vp.screenToWorld(change.position))
                }
              }
            }
            .testTag("game_canvas"),
      ) {
        renderGameWorld(snapshot, skin, pen, progress.settings.highGraphics)
      }

      // Contextual floating instruction banner
      if (snapshot.phase == GamePhase.DRAWING || snapshot.phase == GamePhase.SIMULATING) {
        Surface(
          shape = RoundedCornerShape(50),
          color = Color.White.copy(alpha = 0.88f),
          shadowElevation = 2.dp,
          modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp),
        ) {
          Text(
            text = if (snapshot.phase == GamePhase.DRAWING) str.drawHint else str.holdOnHint,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = DeepText,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
          )
        }
      }

      // Achievement Banner Toast
      androidx.compose.animation.AnimatedVisibility(
        visible = bannerMessage != null,
        enter = slideInVertically { -it } + fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = HoneyPrimary,
          shadowElevation = 6.dp,
        ) {
          Text(
            text = bannerMessage.orEmpty(),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
          )
        }
      }

      // PAUSE OVERLAY
      androidx.compose.animation.AnimatedVisibility(
        visible = snapshot.phase == GamePhase.PAUSED,
        enter = fadeIn() + scaleIn(initialScale = 0.9f),
        exit = fadeOut(),
      ) {
        ModalOverlay {
          Text(str.paused, style = MaterialTheme.typography.headlineLarge, color = DeepText)
          Spacer(modifier = Modifier.height(18.dp))
          Button(
            onClick = onResume,
            modifier = Modifier.fillMaxWidth().height(54.dp).testTag("resume_button"),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GrassGreen),
          ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text(str.resume, style = MaterialTheme.typography.titleMedium)
          }
          Spacer(modifier = Modifier.height(10.dp))
          FilledTonalButton(
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(18.dp),
          ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text(str.restart, style = MaterialTheme.typography.titleMedium)
          }
          Spacer(modifier = Modifier.height(10.dp))
          FilledTonalButton(
            onClick = onOpenMainMenu,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(18.dp),
          ) {
            Icon(Icons.Rounded.Home, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text(str.mainMenu, style = MaterialTheme.typography.titleMedium)
          }
        }
      }

      // CONGRATULATIONS VICTORY OVERLAY
      androidx.compose.animation.AnimatedVisibility(
        visible = snapshot.phase == GamePhase.WON,
        enter = fadeIn() + scaleIn(initialScale = 0.82f),
        exit = fadeOut(),
      ) {
        ModalOverlay {
          // Festive Golden Trophy Emblem
          Surface(
            shape = CircleShape,
            color = HoneyLight,
            shadowElevation = 6.dp,
            modifier = Modifier.size(72.dp).border(3.dp, GoldStar, CircleShape),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Rounded.EmojiEvents,
                contentDescription = "Congratulations",
                tint = HoneyDark,
                modifier = Modifier.size(42.dp),
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Congratulations Ribbon Banner
          Surface(
            shape = RoundedCornerShape(50),
            color = GrassGreen,
            shadowElevation = 3.dp,
          ) {
            Text(
              text = str.levelCleared,
              style = MaterialTheme.typography.headlineMedium,
              fontWeight = FontWeight.ExtraBold,
              color = Color.White,
              modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            )
          }

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "${str.levelLabel(snapshot.level.id)} • ${str.dogRescued}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = DeepText,
            textAlign = TextAlign.Center,
          )
          Spacer(modifier = Modifier.height(14.dp))

          // Animated 3 Stars display
          Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            for (s in 1..3) {
              val earned = s <= snapshot.earnedStars
              Icon(
                imageVector = if (earned) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                contentDescription = "Star $s",
                tint = if (earned) GoldStar else Color(0xFFD5C8B0),
                modifier = Modifier.size(if (s == 2) 56.dp else 46.dp),
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          Surface(
            shape = RoundedCornerShape(50),
            color = HoneyLight,
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(Icons.Rounded.MonetizationOn, contentDescription = null, tint = HoneyDark)
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "+${snapshot.earnedCoins} Coins",
                style = MaterialTheme.typography.titleMedium,
                color = DeepText,
              )
            }
          }

          Spacer(modifier = Modifier.height(18.dp))
          if (snapshot.level.id < 100) {
            Button(
              onClick = onNextLevel,
              modifier = Modifier.fillMaxWidth().height(58.dp).testTag("next_level_button"),
              shape = RoundedCornerShape(20.dp),
              colors = ButtonDefaults.buttonColors(containerColor = GrassGreen),
            ) {
              Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(28.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("${str.nextLevel} (${snapshot.level.id + 1})", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(modifier = Modifier.height(10.dp))
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            FilledTonalButton(
              onClick = onRestart,
              modifier = Modifier.weight(1f).height(50.dp),
              shape = RoundedCornerShape(16.dp),
            ) {
              Icon(Icons.Rounded.Refresh, contentDescription = null)
              Spacer(modifier = Modifier.width(4.dp))
              Text(str.replay)
            }
            FilledTonalButton(
              onClick = onOpenLevels,
              modifier = Modifier.weight(1f).height(50.dp),
              shape = RoundedCornerShape(16.dp),
            ) {
              Icon(Icons.Rounded.Apps, contentDescription = null)
              Spacer(modifier = Modifier.width(4.dp))
              Text(str.levels)
            }
          }
        }
      }

      // DEFEAT OVERLAY
      androidx.compose.animation.AnimatedVisibility(
        visible = snapshot.phase == GamePhase.LOST,
        enter = fadeIn() + scaleIn(initialScale = 0.85f),
        exit = fadeOut(),
      ) {
        ModalOverlay {
          Text(
            text = str.ouchFailed,
            style = MaterialTheme.typography.headlineLarge,
            color = AlarmRed,
          )
          Spacer(modifier = Modifier.height(6.dp))
          val msg =
            when (snapshot.failReason) {
              FailReason.HAZARD_SPIKE -> str.spikeMsg
              FailReason.WATER_FALL -> str.waterMsg
              FailReason.LAVA_BURN -> str.lavaMsg
              FailReason.BOMB_BLAST -> str.bombMsg
              else -> str.beeStingMsg
            }
          Text(
            text = msg,
            style = MaterialTheme.typography.bodyLarge,
            color = DeepText,
            textAlign = TextAlign.Center,
          )
          Spacer(modifier = Modifier.height(20.dp))
          Button(
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth().height(56.dp).testTag("try_again_button"),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = HoneyPrimary),
          ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text(str.tryAgain, style = MaterialTheme.typography.titleLarge)
          }
          Spacer(modifier = Modifier.height(10.dp))
          FilledTonalButton(
            onClick = onOpenLevels,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(16.dp),
          ) {
            Icon(Icons.Rounded.Apps, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text(str.levels)
          }
        }
      }
    }
  }
}

@Composable
private fun HudCircleButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  contentDescription: String,
  testTag: String,
  onClick: () -> Unit,
) {
  Surface(
    shape = CircleShape,
    color = Color.White,
    shadowElevation = 2.dp,
    modifier = Modifier.size(48.dp),
  ) {
    IconButton(onClick = onClick, modifier = Modifier.testTag(testTag)) {
      Icon(icon, contentDescription = contentDescription, tint = DeepText)
    }
  }
}

@Composable
private fun ModalOverlay(content: @Composable () -> Unit) {
  Box(
    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.48f)),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      shape = RoundedCornerShape(28.dp),
      color = CreamBackground,
      shadowElevation = 12.dp,
      modifier =
        Modifier.padding(24.dp)
          .fillMaxWidth()
          .widthIn(max = 380.dp)
          .border(3.dp, HoneyPrimary, RoundedCornerShape(28.dp)),
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        content()
      }
    }
  }
}
