package com.example.model

import androidx.compose.ui.graphics.Color
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

data class Vec2(val x: Float = 0f, val y: Float = 0f) {
  operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)

  operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)

  operator fun times(s: Float) = Vec2(x * s, y * s)

  operator fun div(s: Float) = if (s != 0f) Vec2(x / s, y / s) else Vec2(0f, 0f)

  fun dot(other: Vec2): Float = x * other.x + y * other.y

  fun cross(other: Vec2): Float = x * other.y - y * other.x

  fun lenSq(): Float = x * x + y * y

  fun len(): Float = hypot(x, y)

  fun normalized(): Vec2 {
    val l = len()
    return if (l > 1e-5f) Vec2(x / l, y / l) else Vec2(0f, -1f)
  }

  fun perp(): Vec2 = Vec2(-y, x)

  companion object {
    fun dist(a: Vec2, b: Vec2): Float = hypot(a.x - b.x, a.y - b.y)

    fun closestPointOnSegment(p: Vec2, a: Vec2, b: Vec2): Vec2 {
      val ab = b - a
      val lenSq = ab.lenSq()
      if (lenSq < 1e-6f) return a
      val t = max(0f, min(1f, (p - a).dot(ab) / lenSq))
      return Vec2(a.x + ab.x * t, a.y + ab.y * t)
    }
  }
}

enum class PlatformKind {
  GRASS_BRICK,
  WOOD_PLANK,
  WOOD_CRATE,
  STONE_WALL,
  PEPPERMINT,
}

data class PlatformRect(
  val x: Float,
  val y: Float,
  val w: Float,
  val h: Float,
  val kind: PlatformKind = PlatformKind.GRASS_BRICK,
) {
  val left: Float get() = x
  val top: Float get() = y
  val right: Float get() = x + w
  val bottom: Float get() = y + h
}

data class SlopeTriangle(
  val x1: Float,
  val y1: Float,
  val x2: Float,
  val y2: Float,
  val x3: Float,
  val y3: Float,
  val kind: PlatformKind = PlatformKind.GRASS_BRICK,
)

enum class HazardKind {
  SPIKES_UP,
  SPIKES_DOWN,
  SPIKES_LEFT,
  SPIKES_RIGHT,
  WATER,
  LAVA,
}

data class HazardZone(
  val x: Float,
  val y: Float,
  val w: Float,
  val h: Float,
  val kind: HazardKind,
)

enum class MovingKind {
  PLATFORM,
  SPIKY_BALL,
  SPIKY_BAT,
}

data class MovingObstacleSpec(
  val startX: Float,
  val startY: Float,
  val endX: Float,
  val endY: Float,
  val w: Float,
  val h: Float,
  val speed: Float,
  val kind: MovingKind,
  val phaseOffset: Float = 0f,
)

data class BoulderSpec(
  val x: Float,
  val y: Float,
  val radius: Float = 32f,
)

data class BombSpec(
  val x: Float,
  val y: Float,
  val radius: Float = 24f,
  val fuseSeconds: Float = 5f,
)

data class PortalSpec(
  val x: Float,
  val y: Float,
  val radius: Float = 28f,
)

data class HiveSpec(
  val x: Float,
  val y: Float,
  val beeCount: Int,
  val spawnInterval: Float = 0.25f,
  val radius: Float = 32f,
)

data class DogSpec(
  val x: Float,
  val y: Float,
  val radius: Float = 32f,
)

enum class DifficultyTier {
  EASY,
  MEDIUM,
  HARD,
  EXPERT,
  MASTER,
}

data class LevelSpec(
  val id: Int,
  val title: String,
  val difficulty: DifficultyTier,
  val dog: DogSpec,
  val hives: List<HiveSpec>,
  val platforms: List<PlatformRect>,
  val slopes: List<SlopeTriangle> = emptyList(),
  val hazards: List<HazardZone> = emptyList(),
  val movingObstacles: List<MovingObstacleSpec> = emptyList(),
  val boulders: List<BoulderSpec> = emptyList(),
  val bombs: List<BombSpec> = emptyList(),
  val portals: List<PortalSpec> = emptyList(),
  val inkBudget: Float = 950f,
  val survivalSeconds: Float = 10f,
  val guideArc: List<Vec2> = emptyList(),
)

enum class GamePhase {
  DRAWING,
  SIMULATING,
  WON,
  LOST,
  PAUSED,
}

enum class DogMood {
  CALM,
  SCARED,
  HAPPY,
  STUNG,
}

enum class FailReason {
  BEE_STING,
  HAZARD_SPIKE,
  WATER_FALL,
  LAVA_BURN,
  BOMB_BLAST,
}

data class DogSkin(
  val id: String,
  val name: String,
  val cost: Int,
  val furColor: Color,
  val muzzleColor: Color,
  val earColor: Color,
  val accessory: String,
)

data class AchievementDef(
  val id: String,
  val title: String,
  val description: String,
  val rewardCoins: Int,
  val targetProgress: Int,
)

data class PenModel(
  val id: String,
  val name: String,
  val barrelColor: Color,
  val accentColor: Color,
  val inkColor: Color,
)

enum class AppLanguage(val code: String, val label: String) {
  EN("en", "English"),
  HI("hi", "Hinglish"),
  ES("es", "Español"),
}

data class GameSettings(
  val soundEnabled: Boolean = true,
  val musicEnabled: Boolean = true,
  val vibrationEnabled: Boolean = true,
  val highGraphics: Boolean = true,
  val language: AppLanguage = AppLanguage.EN,
  val selectedPenId: String = "black",
)

data class SaveProgress(
  val unlockedLevel: Int = 1,
  val starsPerLevel: Map<Int, Int> = emptyMap(),
  val coins: Int = 50,
  val unlockedSkins: Set<String> = setOf("shiba"),
  val selectedSkinId: String = "shiba",
  val unlockedAchievements: Set<String> = emptySet(),
  val totalWins: Int = 0,
  val settings: GameSettings = GameSettings(),
) {
  val totalStars: Int
    get() = starsPerLevel.values.sum()
}

sealed interface AppScreen {
  data object MainMenu : AppScreen
  data object LevelSelect : AppScreen
  data class Gameplay(val levelId: Int) : AppScreen
  data object SkinsAndAchievements : AppScreen
  data object Settings : AppScreen
}
