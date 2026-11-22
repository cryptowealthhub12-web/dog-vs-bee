package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.GameCatalog
import com.example.data.Localization
import com.example.model.DogMood
import com.example.model.SaveProgress
import com.example.ui.components.drawCartoonDog
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.DeepText
import com.example.ui.theme.GoldStar
import com.example.ui.theme.GrassGreen
import com.example.ui.theme.HoneyDark
import com.example.ui.theme.HoneyLight
import com.example.ui.theme.HoneyPrimary

@Composable
fun SkinsAndAchievementsScreen(
  progress: SaveProgress,
  onSelectOrBuySkin: (String) -> Unit,
  onBack: () -> Unit,
) {
  val str = Localization.get(progress.settings.language)
  var selectedTab by remember { mutableIntStateOf(0) }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(CreamBackground)
        .safeDrawingPadding()
        .padding(horizontal = 16.dp, vertical = 10.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Surface(shape = CircleShape, color = Color.White, shadowElevation = 3.dp) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("skins_back_button")) {
          Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = DeepText)
        }
      }
      Text(str.skinsAndTrophies, style = MaterialTheme.typography.headlineMedium, color = DeepText)
      StatPill(icon = Icons.Rounded.MonetizationOn, iconTint = HoneyPrimary, text = "${progress.coins}")
    }

    Spacer(modifier = Modifier.height(10.dp))

    TabRow(
      selectedTabIndex = selectedTab,
      containerColor = Color.White,
      contentColor = HoneyDark,
      modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp),
    ) {
      Tab(
        selected = selectedTab == 0,
        onClick = { selectedTab = 0 },
        text = { Text(str.skinsTab, style = MaterialTheme.typography.titleMedium) },
      )
      Tab(
        selected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        text = { Text(str.trophiesTab, style = MaterialTheme.typography.titleMedium) },
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    if (selectedTab == 0) {
      LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 156.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize().widthIn(max = 680.dp),
      ) {
        items(GameCatalog.skins, key = { it.id }) { skin ->
          val owned = progress.unlockedSkins.contains(skin.id)
          val equipped = progress.selectedSkinId == skin.id
          val canAfford = progress.coins >= skin.cost

          Surface(
            shape = RoundedCornerShape(22.dp),
            color = if (equipped) HoneyLight else Color.White,
            shadowElevation = 4.dp,
            modifier =
              Modifier.then(
                if (equipped) Modifier.border(3.dp, HoneyDark, RoundedCornerShape(22.dp))
                else Modifier
              ),
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Canvas(modifier = Modifier.size(86.dp)) {
                drawCartoonDog(
                  center = Offset(size.width * 0.5f, size.height * 0.56f),
                  radius = 30f,
                  skin = skin,
                  mood = if (equipped) DogMood.HAPPY else DogMood.CALM,
                  isBlinking = false,
                  elapsed = 0f,
                )
              }
              Spacer(modifier = Modifier.height(6.dp))
              Text(skin.name, style = MaterialTheme.typography.titleMedium, color = DeepText)
              Spacer(modifier = Modifier.height(8.dp))
              Button(
                onClick = { onSelectOrBuySkin(skin.id) },
                enabled = owned || canAfford,
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("skin_btn_${skin.id}"),
                shape = RoundedCornerShape(14.dp),
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor =
                      when {
                        equipped -> GrassGreen
                        owned -> HoneyPrimary
                        else -> HoneyDark
                      }
                  ),
              ) {
                Text(
                  text =
                    when {
                      equipped -> str.equipped
                      owned -> str.equip
                      else -> str.unlockFor(skin.cost)
                    },
                  style = MaterialTheme.typography.labelLarge,
                )
              }
            }
          }
        }
      }
    } else {
      LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize().widthIn(max = 680.dp),
      ) {
        items(GameCatalog.achievements, key = { it.id }) { ach ->
          val unlocked = progress.unlockedAchievements.contains(ach.id)
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (unlocked) Color(0xFFE7F9DC) else Color.White,
            shadowElevation = 3.dp,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(16.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector =
                  if (unlocked) Icons.Rounded.CheckCircle
                  else Icons.Rounded.EmojiEvents,
                contentDescription = null,
                tint = if (unlocked) GrassGreen else GoldStar,
                modifier = Modifier.size(38.dp),
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(ach.title, style = MaterialTheme.typography.titleMedium, color = DeepText)
                Text(ach.description, style = MaterialTheme.typography.bodyMedium, color = DeepText.copy(alpha = 0.75f))
              }
              Surface(
                shape = RoundedCornerShape(50),
                color = if (unlocked) GrassGreen else HoneyLight,
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector = if (unlocked) Icons.Rounded.CheckCircle else Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = if (unlocked) Color.White else HoneyDark,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = if (unlocked) str.unlocked else "+${ach.rewardCoins}",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (unlocked) Color.White else DeepText,
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
