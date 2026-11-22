package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.Localization
import com.example.levels.LevelRepository
import com.example.model.SaveProgress
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.DeepText
import com.example.ui.theme.GoldStar
import com.example.ui.theme.GrassGreen
import com.example.ui.theme.HoneyDark
import com.example.ui.theme.HoneyLight
import com.example.ui.theme.HoneyPrimary
import kotlinx.coroutines.launch

@Composable
fun LevelSelectScreen(
  progress: SaveProgress,
  onSelectLevel: (Int) -> Unit,
  onBack: () -> Unit,
) {
  val str = Localization.get(progress.settings.language)
  val gridState = rememberLazyGridState()
  val scope = rememberCoroutineScope()
  var selectedTier by remember { mutableIntStateOf(0) }
  val tierRanges = listOf(1..20, 21..40, 41..60, 61..80, 81..100)

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(CreamBackground)
        .safeDrawingPadding()
        .padding(horizontal = 16.dp, vertical = 10.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // Top Header
    Row(
      modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Surface(shape = CircleShape, color = Color.White, shadowElevation = 3.dp) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("levels_back_button")) {
          Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = DeepText)
        }
      }
      Text(str.levels, style = MaterialTheme.typography.headlineMedium, color = DeepText)
      StatPill(icon = Icons.Rounded.Star, iconTint = GoldStar, text = "${progress.totalStars}/300")
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Progress card
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = Color.White,
      shadowElevation = 2.dp,
      modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp),
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Text(
          text = str.progressSummary(progress.starsPerLevel.size, progress.totalStars),
          style = MaterialTheme.typography.titleMedium,
          color = DeepText,
        )
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
          progress = { progress.starsPerLevel.size / 100f },
          modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)),
          color = GrassGreen,
          trackColor = HoneyLight.copy(alpha = 0.4f),
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Tier jump chips
    Row(
      modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp).horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      tierRanges.forEachIndexed { index, range ->
        FilterChip(
          selected = selectedTier == index,
          onClick = {
            selectedTier = index
            scope.launch { gridState.animateScrollToItem(range.first - 1) }
          },
          label = { Text("${range.first}–${range.last}", style = MaterialTheme.typography.labelLarge) },
          colors =
            FilterChipDefaults.filterChipColors(
              selectedContainerColor = HoneyPrimary,
              selectedLabelColor = Color.White,
            ),
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // 100 Levels Grid
    LazyVerticalGrid(
      columns = GridCells.Adaptive(minSize = 84.dp),
      state = gridState,
      contentPadding = PaddingValues(vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.fillMaxSize().widthIn(max = 680.dp),
    ) {
      items(LevelRepository.levels, key = { it.id }) { lvl ->
        val unlocked = lvl.id <= progress.unlockedLevel
        val stars = progress.starsPerLevel[lvl.id] ?: 0
        val isCurrent = lvl.id == progress.unlockedLevel

        Surface(
          shape = RoundedCornerShape(18.dp),
          color =
            when {
              isCurrent -> HoneyLight
              unlocked -> Color.White
              else -> Color(0xFFEAE2D0)
            },
          shadowElevation = if (unlocked) 3.dp else 0.dp,
          modifier =
            Modifier.aspectRatio(0.95f)
              .then(
                if (isCurrent) Modifier.border(2.5.dp, HoneyDark, RoundedCornerShape(18.dp))
                else Modifier
              )
              .clip(RoundedCornerShape(18.dp))
              .clickable(enabled = unlocked) { onSelectLevel(lvl.id) }
              .testTag("level_tile_${lvl.id}"),
        ) {
          Column(
            modifier = Modifier.fillMaxSize().padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
          ) {
            if (unlocked) {
              Text(
                text = "${lvl.id}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = DeepText,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (s in 1..3) {
                  Icon(
                    imageVector = if (s <= stars) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = null,
                    tint = if (s <= stars) GoldStar else Color(0xFFD0C3AC),
                    modifier = Modifier.size(16.dp),
                  )
                }
              }
            } else {
              Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = "Locked",
                tint = Color(0xFF9C8E78),
                modifier = Modifier.size(22.dp),
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "${lvl.id}",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF9C8E78),
              )
            }
          }
        }
      }
    }
  }
}
