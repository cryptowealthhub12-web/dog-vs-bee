package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.AppScreen
import com.example.ui.screens.GameScreen
import com.example.ui.screens.LevelSelectScreen
import com.example.ui.screens.MainMenuScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SkinsAndAchievementsScreen
import com.example.ui.theme.SaveTheDogTheme
import com.example.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      SaveTheDogTheme {
        SaveTheDogApp()
      }
    }
  }
}

@Composable
fun SaveTheDogApp(viewModel: GameViewModel = viewModel()) {
  val progress by viewModel.progress.collectAsStateWithLifecycle()
  val screen by viewModel.screen.collectAsStateWithLifecycle()
  val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
  val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()

  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> viewModel.soundEngine.onAppResume()
        Lifecycle.Event.ON_PAUSE -> viewModel.soundEngine.onAppPause()
        else -> {}
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  BackHandler(enabled = screen !is AppScreen.MainMenu) {
    viewModel.navigateBack()
  }

  Crossfade(
    targetState = screen,
    animationSpec = tween(durationMillis = 220),
    label = "screen_crossfade",
  ) { target ->
    when (target) {
      AppScreen.MainMenu ->
        MainMenuScreen(
          progress = progress,
          onPlayContinue = { viewModel.playContinue() },
          onOpenLevels = { viewModel.navigateTo(AppScreen.LevelSelect) },
          onOpenSkins = { viewModel.navigateTo(AppScreen.SkinsAndAchievements) },
          onOpenSettings = { viewModel.navigateTo(AppScreen.Settings) },
          onToggleSound = { viewModel.toggleSound() },
          onToggleMusic = { viewModel.toggleMusic() },
        )

      AppScreen.LevelSelect ->
        LevelSelectScreen(
          progress = progress,
          onSelectLevel = { levelId -> viewModel.navigateTo(AppScreen.Gameplay(levelId)) },
          onBack = { viewModel.navigateTo(AppScreen.MainMenu) },
        )

      is AppScreen.Gameplay ->
        GameScreen(
          snapshot = snapshot,
          progress = progress,
          bannerMessage = bannerMessage,
          onTick = { dt -> viewModel.tickSimulation(dt) },
          onBeginDraw = { pt -> viewModel.beginDraw(pt) },
          onContinueDraw = { pt -> viewModel.continueDraw(pt) },
          onFinishDraw = { viewModel.finishDraw() },
          onPause = { viewModel.pauseGame() },
          onResume = { viewModel.resumeGame() },
          onRestart = { viewModel.restartCurrentLevel() },
          onNextLevel = { viewModel.nextLevel() },
          onToggleSound = { viewModel.toggleSound() },
          onOpenLevels = { viewModel.navigateTo(AppScreen.LevelSelect) },
          onOpenMainMenu = { viewModel.navigateTo(AppScreen.MainMenu) },
          onDismissBanner = { viewModel.dismissBanner() },
        )

      AppScreen.SkinsAndAchievements ->
        SkinsAndAchievementsScreen(
          progress = progress,
          onSelectOrBuySkin = { id -> viewModel.buyOrEquipSkin(id) },
          onBack = { viewModel.navigateTo(AppScreen.MainMenu) },
        )

      AppScreen.Settings ->
        SettingsScreen(
          progress = progress,
          onToggleSound = { viewModel.toggleSound() },
          onToggleMusic = { viewModel.toggleMusic() },
          onToggleVibration = { viewModel.toggleVibration() },
          onToggleHighGraphics = { viewModel.toggleHighGraphics() },
          onSelectLanguage = { lang -> viewModel.setLanguage(lang) },
          onSelectPen = { penId -> viewModel.selectPen(penId) },
          onResetProgress = { viewModel.resetAllProgress() },
          onBack = { viewModel.navigateTo(AppScreen.MainMenu) },
        )
    }
  }
}
