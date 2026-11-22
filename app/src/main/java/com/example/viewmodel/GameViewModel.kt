package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.audio.SoundEngine
import com.example.data.GameCatalog
import com.example.data.SaveRepository
import com.example.engine.GameSimulation
import com.example.engine.SimulationSnapshot
import com.example.levels.LevelRepository
import com.example.model.AppLanguage
import com.example.model.AppScreen
import com.example.model.FailReason
import com.example.model.GamePhase
import com.example.model.SaveProgress
import com.example.model.Vec2
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class GameViewModel(application: Application) : AndroidViewModel(application) {
  private val saveRepo = SaveRepository(application)
  val soundEngine = SoundEngine(application)

  private val _progress = MutableStateFlow(saveRepo.load())
  val progress: StateFlow<SaveProgress> = _progress.asStateFlow()

  private val _screen = MutableStateFlow<AppScreen>(AppScreen.MainMenu)
  val screen: StateFlow<AppScreen> = _screen.asStateFlow()

  private val _bannerMessage = MutableStateFlow<String?>(null)
  val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

  private var simulation = createSimulation(LevelRepository.getLevel(1))

  private val _snapshot = MutableStateFlow(simulation.snapshot())
  val snapshot: StateFlow<SimulationSnapshot> = _snapshot.asStateFlow()

  init {
    val s = _progress.value.settings
    soundEngine.updateSettings(s.soundEnabled, s.musicEnabled, s.vibrationEnabled)
  }

  private fun createSimulation(levelSpec: com.example.model.LevelSpec): GameSimulation {
    return GameSimulation(
      level = levelSpec,
      onBeeBounce = { soundEngine.playBounce() },
      onBeeSpawn = { soundEngine.playBeeBuzz() },
      onWin = { stars, coins -> handleLevelWon(levelSpec.id, stars, coins) },
      onLose = { reason -> handleLevelLost(reason) },
    )
  }

  fun navigateTo(target: AppScreen) {
    soundEngine.playClick()
    if (target !is AppScreen.Gameplay) {
      soundEngine.stopBeeBuzz()
      soundEngine.resumeMusicIfEnabled()
    }
    if (target is AppScreen.Gameplay) {
      startLevel(target.levelId)
    }
    _screen.value = target
  }

  fun navigateBack(): Boolean {
    return when (_screen.value) {
      AppScreen.MainMenu -> false
      AppScreen.LevelSelect,
      AppScreen.SkinsAndAchievements,
      AppScreen.Settings -> {
        soundEngine.playClick()
        soundEngine.stopBeeBuzz()
        soundEngine.resumeMusicIfEnabled()
        _screen.value = AppScreen.MainMenu
        true
      }
      is AppScreen.Gameplay -> {
        soundEngine.playClick()
        if (simulation.phase == GamePhase.SIMULATING) {
          simulation.pause()
          soundEngine.stopBeeBuzz()
          _snapshot.value = simulation.snapshot()
        } else {
          soundEngine.stopBeeBuzz()
          soundEngine.resumeMusicIfEnabled()
          _screen.value = AppScreen.LevelSelect
        }
        true
      }
    }
  }

  fun playContinue() {
    val targetLevel = _progress.value.unlockedLevel.coerceIn(1, 100)
    navigateTo(AppScreen.Gameplay(targetLevel))
  }

  private var lastAnnouncedSecond = -1

  fun startLevel(levelId: Int) {
    soundEngine.stopBeeBuzz()
    soundEngine.resumeMusicIfEnabled()
    lastAnnouncedSecond = -1
    val lvl = LevelRepository.getLevel(levelId)
    simulation = createSimulation(lvl)
    _snapshot.value = simulation.snapshot()
  }

  fun restartCurrentLevel() {
    soundEngine.playClick()
    soundEngine.stopBeeBuzz()
    soundEngine.resumeMusicIfEnabled()
    lastAnnouncedSecond = -1
    simulation.reset(simulation.level)
    _snapshot.value = simulation.snapshot()
  }

  fun nextLevel() {
    val nextId = (simulation.level.id + 1).coerceAtMost(100)
    soundEngine.playClick()
    startLevel(nextId)
    _screen.value = AppScreen.Gameplay(nextId)
  }

  fun pauseGame() {
    soundEngine.playClick()
    soundEngine.stopBeeBuzz()
    simulation.pause()
    _snapshot.value = simulation.snapshot()
  }

  fun resumeGame() {
    soundEngine.playClick()
    simulation.resume()
    val snap = simulation.snapshot()
    if (snap.phase == GamePhase.SIMULATING) {
      soundEngine.startBeeBuzz()
    }
    _snapshot.value = snap
  }

  fun beginDraw(worldPoint: Vec2) {
    if (simulation.beginDraw(worldPoint)) {
      soundEngine.playDrawTick()
      _snapshot.value = simulation.snapshot()
    }
  }

  fun continueDraw(worldPoint: Vec2) {
    if (simulation.continueDraw(worldPoint)) {
      soundEngine.playDrawTick()
      _snapshot.value = simulation.snapshot()
    }
  }

  fun finishDraw() {
    if (simulation.finishDraw()) {
      soundEngine.vibrate(18L)
      lastAnnouncedSecond = 10
      soundEngine.startBeeBuzz()
      _snapshot.value = simulation.snapshot()
    } else {
      _snapshot.value = simulation.snapshot()
    }
  }

  fun tickSimulation(dt: Float) {
    if (_screen.value !is AppScreen.Gameplay) return
    simulation.step(dt, _progress.value.settings.highGraphics)
    val snap = simulation.snapshot()
    if (snap.phase == GamePhase.SIMULATING) {
      val sec = kotlin.math.ceil(snap.timeLeft).toInt()
      if (sec != lastAnnouncedSecond && sec in 1..5) {
        lastAnnouncedSecond = sec
        soundEngine.playClockTick(urgent = sec <= 3)
      }
    }
    _snapshot.value = snap
  }

  private fun handleLevelWon(levelId: Int, stars: Int, coins: Int) {
    soundEngine.playWin()
    _progress.update { prev ->
      val oldStars = prev.starsPerLevel[levelId] ?: 0
      val updatedStarsMap = prev.starsPerLevel.toMutableMap()
      if (stars > oldStars) {
        updatedStarsMap[levelId] = stars
      }
      val newUnlocked = maxOf(prev.unlockedLevel, (levelId + 1).coerceAtMost(100))
      val completedCount = updatedStarsMap.size
      val totalStars = updatedStarsMap.values.sum()

      var bonusCoins = 0
      val newAch = prev.unlockedAchievements.toMutableSet()
      val unlockedNames = mutableListOf<String>()

      fun unlockAch(id: String, condition: Boolean) {
        if (condition && !newAch.contains(id)) {
          val def = GameCatalog.achievements.firstOrNull { it.id == id } ?: return
          newAch.add(id)
          bonusCoins += def.rewardCoins
          unlockedNames.add("${def.title} (+${def.rewardCoins} coins)")
        }
      }

      unlockAch("first_save", levelId >= 1)
      unlockAch("ten_wins", completedCount >= 10)
      unlockAch("twenty_five_wins", completedCount >= 25)
      unlockAch("fifty_wins", completedCount >= 50)
      unlockAch("stars_30", totalStars >= 30)
      unlockAch("stars_100", totalStars >= 100)
      unlockAch("ink_saver", stars == 3)

      if (unlockedNames.isNotEmpty()) {
        _bannerMessage.value = "Achievement: ${unlockedNames.first()}"
      }

      val next =
        prev.copy(
          unlockedLevel = newUnlocked,
          starsPerLevel = updatedStarsMap,
          coins = prev.coins + coins + bonusCoins,
          unlockedAchievements = newAch,
          totalWins = prev.totalWins + 1,
        )
      saveRepo.save(next)
      next
    }
  }

  private fun handleLevelLost(@Suppress("UNUSED_PARAMETER") reason: FailReason) {
    soundEngine.playLose()
  }

  fun dismissBanner() {
    _bannerMessage.value = null
  }

  fun buyOrEquipSkin(skinId: String) {
    val skin = GameCatalog.findSkin(skinId)
    val cur = _progress.value
    if (cur.unlockedSkins.contains(skinId)) {
      soundEngine.playClick()
      val updated = cur.copy(selectedSkinId = skinId)
      _progress.value = updated
      saveRepo.save(updated)
    } else if (cur.coins >= skin.cost) {
      soundEngine.playCoin()
      val newSkins = cur.unlockedSkins + skinId
      val newAch = cur.unlockedAchievements.toMutableSet()
      var bonus = 0
      if (newSkins.size >= 2 && !newAch.contains("fashion_pup")) {
        newAch.add("fashion_pup")
        bonus = 50
        _bannerMessage.value = "Achievement: Fashion Pup (+50 coins)"
      }
      val updated =
        cur.copy(
          coins = cur.coins - skin.cost + bonus,
          unlockedSkins = newSkins,
          selectedSkinId = skinId,
          unlockedAchievements = newAch,
        )
      _progress.value = updated
      saveRepo.save(updated)
    }
  }

  fun toggleSound() {
    val cur = _progress.value
    val nextSettings = cur.settings.copy(soundEnabled = !cur.settings.soundEnabled)
    applySettings(nextSettings)
  }

  fun toggleMusic() {
    val cur = _progress.value
    val nextSettings = cur.settings.copy(musicEnabled = !cur.settings.musicEnabled)
    applySettings(nextSettings)
  }

  fun toggleVibration() {
    val cur = _progress.value
    val nextSettings = cur.settings.copy(vibrationEnabled = !cur.settings.vibrationEnabled)
    applySettings(nextSettings)
    if (nextSettings.vibrationEnabled) soundEngine.vibrate(25L)
  }

  fun toggleHighGraphics() {
    val cur = _progress.value
    val nextSettings = cur.settings.copy(highGraphics = !cur.settings.highGraphics)
    applySettings(nextSettings)
  }

  fun setLanguage(lang: AppLanguage) {
    val cur = _progress.value
    applySettings(cur.settings.copy(language = lang))
  }

  fun selectPen(penId: String) {
    val cur = _progress.value
    applySettings(cur.settings.copy(selectedPenId = penId))
  }

  private fun applySettings(newSettings: com.example.model.GameSettings) {
    soundEngine.updateSettings(
      newSettings.soundEnabled,
      newSettings.musicEnabled,
      newSettings.vibrationEnabled,
    )
    soundEngine.playClick()
    val updated = _progress.value.copy(settings = newSettings)
    _progress.value = updated
    saveRepo.save(updated)
  }

  fun resetAllProgress() {
    soundEngine.playClick()
    val fresh = saveRepo.reset()
    _progress.value = fresh
    soundEngine.updateSettings(
      fresh.settings.soundEnabled,
      fresh.settings.musicEnabled,
      fresh.settings.vibrationEnabled,
    )
  }

  override fun onCleared() {
    super.onCleared()
    soundEngine.release()
  }
}
