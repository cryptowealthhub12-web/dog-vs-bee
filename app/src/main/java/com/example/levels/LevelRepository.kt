package com.example.levels

import com.example.model.BombSpec
import com.example.model.BoulderSpec
import com.example.model.DifficultyTier
import com.example.model.DogSpec
import com.example.model.HazardKind
import com.example.model.HazardZone
import com.example.model.HiveSpec
import com.example.model.LevelSpec
import com.example.model.MovingKind
import com.example.model.MovingObstacleSpec
import com.example.model.PlatformKind
import com.example.model.PlatformRect
import com.example.model.PortalSpec
import com.example.model.SlopeTriangle
import com.example.model.Vec2
import kotlin.math.cos
import kotlin.math.sin

object LevelRepository {
  const val WORLD_W = 600f
  const val WORLD_H = 820f
  private const val GROUND_Y = 690f
  private const val DOG_R = 32f

  val levels: List<LevelSpec> by lazy { (1..100).map { buildLevel(it) } }

  fun getLevel(id: Int): LevelSpec = levels[(id - 1).coerceIn(0, 99)]

  private fun buildLevel(id: Int): LevelSpec {
    val tier =
      when (id) {
        in 1..15 -> DifficultyTier.EASY
        in 16..35 -> DifficultyTier.MEDIUM
        in 36..60 -> DifficultyTier.HARD
        in 61..85 -> DifficultyTier.EXPERT
        else -> DifficultyTier.MASTER
      }

    // Handcrafted signature levels + rich progressive architectural templates (1..100)
    return when (id) {
      // LEVEL 1: Classic flat meadow intro with helpful dotted arc
      1 ->
        LevelSpec(
          id = 1,
          title = "First Shield",
          difficulty = DifficultyTier.EASY,
          dog = DogSpec(300f, GROUND_Y - DOG_R, DOG_R),
          hives = listOf(HiveSpec(135f, 175f, beeCount = 3, spawnInterval = 0.45f)),
          platforms = listOf(PlatformRect(0f, GROUND_Y, WORLD_W, 130f, PlatformKind.GRASS_BRICK)),
          inkBudget = 1200f,
          guideArc = createGuideDome(300f, GROUND_Y, 92f),
        )
      // LEVEL 2: Hive on the right, higher count
      2 ->
        LevelSpec(
          id = 2,
          title = "Right Wing",
          difficulty = DifficultyTier.EASY,
          dog = DogSpec(210f, GROUND_Y - DOG_R, DOG_R),
          hives = listOf(HiveSpec(470f, 190f, beeCount = 4, spawnInterval = 0.4f)),
          platforms = listOf(PlatformRect(0f, GROUND_Y, WORLD_W, 130f, PlatformKind.GRASS_BRICK)),
          inkBudget = 1150f,
        )
      // LEVEL 3: Wooden crates beside dog
      3 ->
        LevelSpec(
          id = 3,
          title = "Crate Shelter",
          difficulty = DifficultyTier.EASY,
          dog = DogSpec(300f, GROUND_Y - DOG_R, DOG_R),
          hives = listOf(HiveSpec(300f, 150f, beeCount = 5, spawnInterval = 0.35f)),
          platforms =
            listOf(
              PlatformRect(0f, GROUND_Y, WORLD_W, 130f, PlatformKind.GRASS_BRICK),
              PlatformRect(185f, GROUND_Y - 76f, 64f, 76f, PlatformKind.WOOD_CRATE),
              PlatformRect(351f, GROUND_Y - 76f, 64f, 76f, PlatformKind.WOOD_CRATE),
            ),
          inkBudget = 950f,
        )
      // LEVEL 4: Dog on an elevated platform
      4 ->
        LevelSpec(
          id = 4,
          title = "High Perch",
          difficulty = DifficultyTier.EASY,
          dog = DogSpec(300f, 450f - DOG_R, DOG_R),
          hives = listOf(HiveSpec(120f, 200f, beeCount = 5, spawnInterval = 0.35f)),
          platforms =
            listOf(
              PlatformRect(0f, GROUND_Y, WORLD_W, 130f, PlatformKind.GRASS_BRICK),
              PlatformRect(190f, 450f, 220f, 34f, PlatformKind.WOOD_PLANK),
            ),
          inkBudget = 1050f,
        )
      // LEVEL 5: Twin stone pillars
      5 ->
        LevelSpec(
          id = 5,
          title = "Twin Pillars",
          difficulty = DifficultyTier.EASY,
          dog = DogSpec(300f, GROUND_Y - DOG_R, DOG_R),
          hives = listOf(HiveSpec(480f, 160f, beeCount = 6, spawnInterval = 0.32f)),
          platforms =
            listOf(
              PlatformRect(0f, GROUND_Y, WORLD_W, 130f, PlatformKind.GRASS_BRICK),
              PlatformRect(140f, 520f, 52f, 170f, PlatformKind.GRASS_BRICK),
              PlatformRect(408f, 520f, 52f, 170f, PlatformKind.GRASS_BRICK),
            ),
          inkBudget = 950f,
        )
      // LEVEL 6: Sloped hillside
      6 ->
        LevelSpec(
          id = 6,
          title = "Hillside Slide",
          difficulty = DifficultyTier.EASY,
          dog = DogSpec(170f, GROUND_Y - DOG_R, DOG_R),
          hives = listOf(HiveSpec(470f, 210f, beeCount = 6, spawnInterval = 0.32f)),
          platforms =
            listOf(
              PlatformRect(0f, GROUND_Y, WORLD_W, 130f, PlatformKind.GRASS_BRICK),
              PlatformRect(470f, 470f, 130f, 220f, PlatformKind.GRASS_BRICK),
            ),
          slopes = listOf(SlopeTriangle(280f, GROUND_Y, 470f, GROUND_Y, 470f, 470f)),
          inkBudget = 980f,
        )
      // LEVEL 7: Safe pit pocket
      7 ->
        LevelSpec(
          id = 7,
          title = "Cozy Trench",
          difficulty = DifficultyTier.EASY,
          dog = DogSpec(300f, 650f - DOG_R, DOG_R),
          hives = listOf(HiveSpec(300f, 165f, beeCount = 7, spawnInterval = 0.28f)),
          platforms =
            listOf(
              PlatformRect(0f, 520f, 210f, 300f, PlatformKind.GRASS_BRICK),
              PlatformRect(390f, 520f, 210f, 300f, PlatformKind.GRASS_BRICK),
              PlatformRect(210f, 650f, 180f, 170f, PlatformKind.GRASS_BRICK),
            ),
          inkBudget = 820f,
        )
      // LEVEL 8: Crate tower guard
      8 ->
        LevelSpec(
          id = 8,
          title = "Wooden Wall",
          difficulty = DifficultyTier.EASY,
          dog = DogSpec(455f, GROUND_Y - DOG_R, DOG_R),
          hives = listOf(HiveSpec(120f, 220f, beeCount = 7, spawnInterval = 0.28f)),
          platforms =
            listOf(
              PlatformRect(0f, GROUND_Y, WORLD_W, 130f, PlatformKind.GRASS_BRICK),
              PlatformRect(290f, GROUND_Y - 120f, 64f, 120f, PlatformKind.WOOD_CRATE),
              PlatformRect(290f, GROUND_Y - 184f, 64f, 64f, PlatformKind.WOOD_CRATE),
            ),
          inkBudget = 920f,
        )
      // LEVEL 9: Bridge over dry canyon
      9 ->
        LevelSpec(
          id = 9,
          title = "Canyon Shelf",
          difficulty = DifficultyTier.EASY,
          dog = DogSpec(300f, 420f - DOG_R, DOG_R),
          hives = listOf(HiveSpec(130f, 160f, beeCount = 7, spawnInterval = 0.26f)),
          platforms =
            listOf(
              PlatformRect(0f, GROUND_Y, 180f, 130f, PlatformKind.GRASS_BRICK),
              PlatformRect(420f, GROUND_Y, 180f, 130f, PlatformKind.GRASS_BRICK),
              PlatformRect(220f, 420f, 160f, 30f, PlatformKind.WOOD_PLANK),
            ),
          inkBudget = 960f,
        )
      // LEVEL 10: Dual overhead hives intro!
      10 ->
        LevelSpec(
          id = 10,
          title = "Double Buzz",
          difficulty = DifficultyTier.EASY,
          dog = DogSpec(300f, GROUND_Y - DOG_R, DOG_R),
          hives =
            listOf(
              HiveSpec(110f, 180f, beeCount = 4, spawnInterval = 0.35f),
              HiveSpec(490f, 180f, beeCount = 4, spawnInterval = 0.35f),
            ),
          platforms = listOf(PlatformRect(0f, GROUND_Y, WORLD_W, 130f, PlatformKind.GRASS_BRICK)),
          inkBudget = 1000f,
        )
      // SIGNATURE LEVEL 22: Bottom hive pit + tall twin crate towers + plank bridge (matches screenshot!)
      22 ->
        LevelSpec(
          id = 22,
          title = "Underground Hive",
          difficulty = DifficultyTier.MEDIUM,
          dog = DogSpec(300f, 355f - DOG_R, DOG_R),
          hives = listOf(HiveSpec(300f, 705f, beeCount = 10, spawnInterval = 0.22f)),
          platforms =
            listOf(
              PlatformRect(0f, 540f, 210f, 280f, PlatformKind.GRASS_BRICK),
              PlatformRect(390f, 540f, 210f, 280f, PlatformKind.GRASS_BRICK),
              PlatformRect(210f, 755f, 180f, 65f, PlatformKind.GRASS_BRICK),
              PlatformRect(158f, 486f, 54f, 54f, PlatformKind.WOOD_CRATE),
              PlatformRect(158f, 432f, 54f, 54f, PlatformKind.WOOD_CRATE),
              PlatformRect(158f, 378f, 54f, 54f, PlatformKind.WOOD_CRATE),
              PlatformRect(388f, 486f, 54f, 54f, PlatformKind.WOOD_CRATE),
              PlatformRect(388f, 432f, 54f, 54f, PlatformKind.WOOD_CRATE),
              PlatformRect(388f, 378f, 54f, 54f, PlatformKind.WOOD_CRATE),
              PlatformRect(198f, 355f, 204f, 24f, PlatformKind.WOOD_PLANK),
            ),
          inkBudget = 920f,
        )
      // SIGNATURE LEVEL 30: Deep spike pit + steep grass slope + spiky star ball (matches screenshot!)
      30 ->
        LevelSpec(
          id = 30,
          title = "Spike Valley",
          difficulty = DifficultyTier.MEDIUM,
          dog = DogSpec(192f, 745f - DOG_R, DOG_R),
          hives = listOf(HiveSpec(105f, 205f, beeCount = 12, spawnInterval = 0.20f)),
          platforms =
            listOf(
              PlatformRect(0f, 540f, 110f, 280f, PlatformKind.GRASS_BRICK),
              PlatformRect(110f, 745f, 165f, 75f, PlatformKind.GRASS_BRICK),
              PlatformRect(275f, 540f, 325f, 280f, PlatformKind.GRASS_BRICK),
              PlatformRect(510f, 290f, 90f, 250f, PlatformKind.GRASS_BRICK),
            ),
          slopes = listOf(SlopeTriangle(275f, 540f, 510f, 540f, 510f, 290f)),
          hazards =
            listOf(
              HazardZone(110f, 580f, 24f, 125f, HazardKind.SPIKES_RIGHT),
              HazardZone(251f, 580f, 24f, 125f, HazardKind.SPIKES_LEFT),
            ),
          movingObstacles =
            listOf(MovingObstacleSpec(375f, 275f, 490f, 195f, 54f, 54f, 130f, MovingKind.SPIKY_BALL)),
          inkBudget = 860f,
        )
      // SIGNATURE LEVEL 46: Twin cliffs, molten lava, overhead portal hook, spiky bat monster (matches screenshot!)
      46 ->
        LevelSpec(
          id = 46,
          title = "Lava Portal",
          difficulty = DifficultyTier.HARD,
          dog = DogSpec(300f, 340f, DOG_R),
          hives = listOf(HiveSpec(105f, 225f, beeCount = 12, spawnInterval = 0.20f)),
          portals = listOf(PortalSpec(300f, 225f, radius = 32f)),
          platforms =
            listOf(
              PlatformRect(0f, 575f, 210f, 245f, PlatformKind.GRASS_BRICK),
              PlatformRect(390f, 575f, 210f, 245f, PlatformKind.GRASS_BRICK),
            ),
          hazards = listOf(HazardZone(210f, 695f, 180f, 125f, HazardKind.LAVA)),
          movingObstacles =
            listOf(MovingObstacleSpec(430f, 295f, 520f, 345f, 68f, 68f, 115f, MovingKind.SPIKY_BAT)),
          inkBudget = 860f,
        )
      // SIGNATURE LEVEL 63: Molten lava canyon + U-pit + stone boulder + '5' timed bomb (matches screenshot!)
      63 ->
        LevelSpec(
          id = 63,
          title = "Boulder & Bomb",
          difficulty = DifficultyTier.EXPERT,
          dog = DogSpec(382f, 650f - DOG_R, DOG_R),
          hives = listOf(HiveSpec(105f, 205f, beeCount = 14, spawnInterval = 0.19f)),
          platforms =
            listOf(
              PlatformRect(220f, 435f, 100f, 385f, PlatformKind.GRASS_BRICK),
              PlatformRect(320f, 650f, 125f, 170f, PlatformKind.GRASS_BRICK),
              PlatformRect(445f, 435f, 155f, 385f, PlatformKind.GRASS_BRICK),
            ),
          hazards = listOf(HazardZone(0f, 585f, 220f, 235f, HazardKind.LAVA)),
          boulders = listOf(BoulderSpec(278f, 398f, radius = 36f)),
          bombs = listOf(BombSpec(508f, 408f, radius = 26f, fuseSeconds = 5f)),
          inkBudget = 820f,
        )
      // SIGNATURE LEVEL 76: Peppermint candy pedestal over full water + dual hives (matches screenshot!)
      76 ->
        LevelSpec(
          id = 76,
          title = "Peppermint Sea",
          difficulty = DifficultyTier.EXPERT,
          dog = DogSpec(300f, 590f - DOG_R, DOG_R),
          hives =
            listOf(
              HiveSpec(105f, 235f, beeCount = 8, spawnInterval = 0.21f),
              HiveSpec(495f, 235f, beeCount = 8, spawnInterval = 0.21f),
            ),
          platforms = listOf(PlatformRect(240f, 590f, 120f, 44f, PlatformKind.PEPPERMINT)),
          hazards = listOf(HazardZone(0f, 690f, WORLD_W, 130f, HazardKind.WATER)),
          inkBudget = 840f,
        )
      else -> buildArchetypeLevel(id, tier)
    }
  }

  private fun buildArchetypeLevel(id: Int, tier: DifficultyTier): LevelSpec {
    // Choose architectural archetype by phase in progression
    return when (id) {
      in 11..20 -> buildTier2Level(id, tier)
      in 21..30 -> buildTier3SpikesAndSlopes(id, tier)
      in 31..40 -> buildTier4WaterAndMultiHive(id, tier)
      in 41..60 -> buildTier5MovingHazardsAndSwarms(id, tier)
      in 61..80 -> buildTier6ComplexPhysics(id, tier)
      else -> buildTier7MasterPuzzles(id, tier)
    }
  }

  // Levels 11-20: Multiple bees, small platforms, narrow protection zones, limited ink
  private fun buildTier2Level(id: Int, tier: DifficultyTier): LevelSpec {
    val variant = (id - 11) % 5
    val bees = 6 + (id - 10) / 2
    val ink = (940f - (id - 10) * 14f).coerceAtLeast(740f)
    return when (variant) {
      0 -> // Small floating island
        LevelSpec(
          id = id,
          title = "Sky Island #$id",
          difficulty = tier,
          dog = DogSpec(300f, 460f - DOG_R, DOG_R),
          hives = listOf(HiveSpec(120f, 160f, bees, 0.25f)),
          platforms =
            listOf(
              PlatformRect(0f, GROUND_Y, WORLD_W, 130f, PlatformKind.GRASS_BRICK),
              PlatformRect(215f, 460f, 170f, 32f, PlatformKind.WOOD_PLANK),
            ),
          inkBudget = ink,
        )
      1 -> // Narrow crate alley
        LevelSpec(
          id = id,
          title = "Crate Alley #$id",
          difficulty = tier,
          dog = DogSpec(300f, GROUND_Y - DOG_R, DOG_R),
          hives =
            listOf(
              HiveSpec(105f, 170f, bees / 2 + 1, 0.28f),
              HiveSpec(495f, 170f, bees / 2 + 1, 0.28f),
            ),
          platforms =
            listOf(
              PlatformRect(0f, GROUND_Y, WORLD_W, 130f, PlatformKind.GRASS_BRICK),
              PlatformRect(215f, GROUND_Y - 120f, 44f, 120f, PlatformKind.WOOD_CRATE),
              PlatformRect(341f, GROUND_Y - 120f, 44f, 120f, PlatformKind.WOOD_CRATE),
            ),
          inkBudget = ink,
        )
      2 -> // Corner shelter
        LevelSpec(
          id = id,
          title = "Corner Guard #$id",
          difficulty = tier,
          dog = DogSpec(105f, GROUND_Y - DOG_R, DOG_R),
          hives = listOf(HiveSpec(480f, 190f, bees, 0.23f)),
          platforms =
            listOf(
              PlatformRect(0f, GROUND_Y, WORLD_W, 130f, PlatformKind.GRASS_BRICK),
              PlatformRect(0f, 420f, 36f, 270f, PlatformKind.GRASS_BRICK),
              PlatformRect(260f, 520f, 48f, 170f, PlatformKind.WOOD_CRATE),
            ),
          inkBudget = ink,
        )
      3 -> // Stepped terraces
        LevelSpec(
          id = id,
          title = "Terrace Defense #$id",
          difficulty = tier,
          dog = DogSpec(460f, 540f - DOG_R, DOG_R),
          hives = listOf(HiveSpec(130f, 210f, bees, 0.24f)),
          platforms =
            listOf(
              PlatformRect(0f, GROUND_Y, 340f, 130f, PlatformKind.GRASS_BRICK),
              PlatformRect(340f, 540f, 260f, 280f, PlatformKind.GRASS_BRICK),
              PlatformRect(180f, 450f, 120f, 26f, PlatformKind.WOOD_PLANK),
            ),
          inkBudget = ink,
        )
      else -> // Overhead shelf shield + rolling stone boulder intro
        LevelSpec(
          id = id,
          title = if (id >= 15) "Boulder Ridge #$id" else "Roof Shelter #$id",
          difficulty = tier,
          dog = DogSpec(300f, GROUND_Y - DOG_R, DOG_R),
          hives = listOf(HiveSpec(300f, 145f, bees, 0.22f)),
          platforms =
            listOf(
              PlatformRect(0f, GROUND_Y, WORLD_W, 130f, PlatformKind.GRASS_BRICK),
              PlatformRect(140f, 420f, 120f, 26f, PlatformKind.WOOD_PLANK),
              PlatformRect(340f, 420f, 120f, 26f, PlatformKind.WOOD_PLANK),
            ),
          boulders = if (id >= 15) listOf(BoulderSpec(190f, 386f, radius = 32f)) else emptyList(),
          inkBudget = ink,
        )
    }
  }

  // Levels 21-30: Spikes, moving platforms, uneven ground, bottom hives (inspired by screenshot!)
  private fun buildTier3SpikesAndSlopes(id: Int, tier: DifficultyTier): LevelSpec {
    val variant = (id - 21) % 5
    val bees = 8 + (id - 21) / 2
    val ink = (880f - (id - 21) * 12f).coerceAtLeast(700f)
    return when (variant) {
      0 -> // Bottom hive with crate pit (like Level 22 in screenshot!)
        LevelSpec(
          id = id,
          title = "Underground Hive #$id",
          difficulty = tier,
          dog = DogSpec(300f, 385f - DOG_R, DOG_R),
          hives = listOf(HiveSpec(300f, 705f, bees, 0.22f)),
          platforms =
            listOf(
              PlatformRect(0f, 540f, 210f, 280f, PlatformKind.GRASS_BRICK),
              PlatformRect(390f, 540f, 210f, 280f, PlatformKind.GRASS_BRICK),
              PlatformRect(210f, 755f, 180f, 65f, PlatformKind.GRASS_BRICK),
              PlatformRect(160f, 390f, 54f, 150f, PlatformKind.WOOD_CRATE),
              PlatformRect(386f, 390f, 54f, 150f, PlatformKind.WOOD_CRATE),
              PlatformRect(214f, 385f, 172f, 24f, PlatformKind.WOOD_PLANK),
            ),
          inkBudget = ink,
        )
      1 -> // Spike pit + steep ramp (like Level 30 in screenshot!)
        LevelSpec(
          id = id,
          title = "Spike Valley #$id",
          difficulty = tier,
          dog = DogSpec(200f, 745f - DOG_R, DOG_R),
          hives = listOf(HiveSpec(110f, 210f, bees, 0.22f)),
          platforms =
            listOf(
              PlatformRect(0f, 540f, 110f, 280f, PlatformKind.GRASS_BRICK),
              PlatformRect(110f, 745f, 180f, 75f, PlatformKind.GRASS_BRICK),
              PlatformRect(290f, 540f, 310f, 280f, PlatformKind.GRASS_BRICK),
              PlatformRect(450f, 360f, 150f, 180f, PlatformKind.GRASS_BRICK),
            ),
          slopes = listOf(SlopeTriangle(290f, 540f, 450f, 540f, 450f, 360f)),
          hazards =
            listOf(
              HazardZone(110f, 580f, 24f, 120f, HazardKind.SPIKES_RIGHT),
              HazardZone(266f, 580f, 24f, 120f, HazardKind.SPIKES_LEFT),
            ),
          inkBudget = ink,
        )
      2 -> // Moving bridge platform over spikes
        LevelSpec(
          id = id,
          title = "Moving Ferry #$id",
          difficulty = tier,
          dog = DogSpec(300f, 450f - DOG_R, DOG_R),
          hives = listOf(HiveSpec(480f, 165f, bees, 0.22f)),
          platforms =
            listOf(
              PlatformRect(0f, GROUND_Y, 130f, 130f, PlatformKind.GRASS_BRICK),
              PlatformRect(470f, GROUND_Y, 130f, 130f, PlatformKind.GRASS_BRICK),
              PlatformRect(230f, 450f, 140f, 28f, PlatformKind.WOOD_PLANK),
            ),
          hazards = listOf(HazardZone(130f, GROUND_Y + 28f, 340f, 36f, HazardKind.SPIKES_UP)),
          movingObstacles =
            listOf(
              MovingObstacleSpec(
                startX = 140f,
                startY = 300f,
                endX = 360f,
                endY = 300f,
                w = 100f,
                h = 24f,
                speed = 110f,
                kind = MovingKind.PLATFORM,
              )
            ),
          inkBudget = ink,
        )
      3 -> // Floor spikes flanking dog
        LevelSpec(
          id = id,
          title = "Thorny Path #$id",
          difficulty = tier,
          dog = DogSpec(300f, GROUND_Y - DOG_R, DOG_R),
          hives =
            listOf(
              HiveSpec(105f, 200f, bees / 2 + 1, 0.25f),
              HiveSpec(495f, 200f, bees / 2 + 1, 0.25f),
            ),
          platforms = listOf(PlatformRect(0f, GROUND_Y, WORLD_W, 130f, PlatformKind.GRASS_BRICK)),
          hazards =
            listOf(
              HazardZone(40f, GROUND_Y - 26f, 110f, 26f, HazardKind.SPIKES_UP),
              HazardZone(450f, GROUND_Y - 26f, 110f, 26f, HazardKind.SPIKES_UP),
            ),
          inkBudget = ink,
        )
      else -> // Twin slopes funnel
        LevelSpec(
          id = id,
          title = "Funnel Trap #$id",
          difficulty = tier,
          dog = DogSpec(300f, GROUND_Y - DOG_R, DOG_R),
          hives = listOf(HiveSpec(300f, 145f, bees + 2, 0.20f)),
          platforms =
            listOf(
              PlatformRect(0f, GROUND_Y, WORLD_W, 130f, PlatformKind.GRASS_BRICK),
              PlatformRect(0f, 470f, 90f, 220f, PlatformKind.GRASS_BRICK),
              PlatformRect(510f, 470f, 90f, 220f, PlatformKind.GRASS_BRICK),
            ),
          slopes =
            listOf(
              SlopeTriangle(90f, 470f, 90f, GROUND_Y, 210f, GROUND_Y),
              SlopeTriangle(510f, 470f, 390f, GROUND_Y, 510f, GROUND_Y),
            ),
          inkBudget = ink,
        )
    }
  }

  // Levels 31-40: Water pools, dangerous gaps, 2 hives, suspended islands
  private fun buildTier4WaterAndMultiHive(id: Int, tier: DifficultyTier): LevelSpec {
    val variant = (id - 31) % 5
    val bees = 10 + (id - 31) / 2
    val ink = (860f - (id - 31) * 10f).coerceAtLeast(680f)
    return when (variant) {
      0 -> // Floating raft over water
        LevelSpec(
          id = id,
          title = "Water Crossing #$id",
          difficulty = tier,
          dog = DogSpec(300f, 610f - DOG_R, DOG_R),
          hives =
            listOf(
              HiveSpec(105f, 175f, bees / 2 + 1, 0.23f),
              HiveSpec(495f, 175f, bees / 2 + 1, 0.23f),
            ),
          platforms =
            listOf(
              PlatformRect(0f, 650f, 110f, 170f, PlatformKind.GRASS_BRICK),
              PlatformRect(490f, 650f, 110f, 170f, PlatformKind.GRASS_BRICK),
              PlatformRect(225f, 610f, 150f, 30f, PlatformKind.WOOD_PLANK),
            ),
          hazards = listOf(HazardZone(110f, 685f, 380f, 135f, HazardKind.WATER)),
          inkBudget = ink,
        )
      1 -> // Side island with water moat
        LevelSpec(
          id = id,
          title = "Moat Fortress #$id",
          difficulty = tier,
          dog = DogSpec(145f, 590f - DOG_R, DOG_R),
          hives = listOf(HiveSpec(470f, 195f, bees + 2, 0.20f)),
          platforms =
            listOf(
              PlatformRect(55f, 590f, 180f, 230f, PlatformKind.GRASS_BRICK),
              PlatformRect(460f, 620f, 140f, 200f, PlatformKind.GRASS_BRICK),
            ),
          hazards =
            listOf(
              HazardZone(0f, 700f, 55f, 120f, HazardKind.WATER),
              HazardZone(235f, 700f, 225f, 120f, HazardKind.WATER),
            ),
          inkBudget = ink,
        )
      2 -> // High plank above water + spikes
        LevelSpec(
          id = id,
          title = "Pier Rescue #$id",
          difficulty = tier,
          dog = DogSpec(300f, 470f - DOG_R, DOG_R),
          hives =
            listOf(
              HiveSpec(110f, 190f, bees / 2, 0.24f),
              HiveSpec(490f, 260f, bees / 2 + 2, 0.22f),
            ),
          platforms =
            listOf(
              PlatformRect(205f, 470f, 190f, 32f, PlatformKind.WOOD_PLANK),
              PlatformRect(0f, 640f, 95f, 180f, PlatformKind.GRASS_BRICK),
              PlatformRect(505f, 640f, 95f, 180f, PlatformKind.GRASS_BRICK),
            ),
          hazards = listOf(HazardZone(95f, 695f, 410f, 125f, HazardKind.WATER)),
          inkBudget = ink,
        )
      3 -> // Moving platform under water danger
        LevelSpec(
          id = id,
          title = "Tide Runner #$id",
          difficulty = tier,
          dog = DogSpec(445f, 560f - DOG_R, DOG_R),
          hives = listOf(HiveSpec(120f, 180f, bees + 2, 0.20f)),
          platforms =
            listOf(
              PlatformRect(365f, 560f, 190f, 260f, PlatformKind.GRASS_BRICK),
              PlatformRect(0f, 600f, 120f, 220f, PlatformKind.GRASS_BRICK),
            ),
          hazards =
            listOf(
              HazardZone(120f, 690f, 245f, 130f, HazardKind.WATER),
              HazardZone(555f, 690f, 45f, 130f, HazardKind.WATER),
            ),
          movingObstacles =
            listOf(
              MovingObstacleSpec(135f, 410f, 315f, 410f, 110f, 24f, 125f, MovingKind.PLATFORM)
            ),
          inkBudget = ink,
        )
      else -> // Central peppermint/brick island surrounded by water
        LevelSpec(
          id = id,
          title = "Candy Island #$id",
          difficulty = tier,
          dog = DogSpec(300f, 510f - DOG_R, DOG_R),
          hives =
            listOf(
              HiveSpec(95f, 165f, bees / 2 + 1, 0.22f),
              HiveSpec(505f, 165f, bees / 2 + 1, 0.22f),
            ),
          platforms =
            listOf(
              PlatformRect(235f, 510f, 130f, 42f, PlatformKind.PEPPERMINT),
              PlatformRect(245f, 290f, 110f, 24f, PlatformKind.WOOD_PLANK),
            ),
          hazards =
            listOf(
              HazardZone(0f, 695f, WORLD_W, 125f, HazardKind.WATER),
            ),
          inkBudget = ink,
        )
    }
  }

  // Levels 41-60: Moving spiky balls, strong bee swarms, smaller ink budgets, tricky dog spots
  private fun buildTier5MovingHazardsAndSwarms(id: Int, tier: DifficultyTier): LevelSpec {
    val variant = (id - 41) % 6
    val bees = 12 + (id - 41) / 3
    val ink = (800f - (id - 41) * 8f).coerceAtLeast(620f)
    return when (variant) {
      0 -> // Patrolling spiky ball above dog
        LevelSpec(
          id = id,
          title = "Spiky Patrol #$id",
          difficulty = tier,
          dog = DogSpec(300f, GROUND_Y - DOG_R, DOG_R),
          hives =
            listOf(
              HiveSpec(105f, 175f, bees / 2 + 1, 0.20f),
              HiveSpec(495f, 175f, bees / 2 + 1, 0.20f),
            ),
          platforms = listOf(PlatformRect(0f, GROUND_Y, WORLD_W, 130f, PlatformKind.GRASS_BRICK)),
          movingObstacles =
            listOf(MovingObstacleSpec(130f, 430f, 470f, 430f, 54f, 54f, 155f, MovingKind.SPIKY_BALL)),
          inkBudget = ink,
        )
      1 -> // Diagonal ramp + spiky ball
        LevelSpec(
          id = id,
          title = "Rolling Danger #$id",
          difficulty = tier,
          dog = DogSpec(150f, GROUND_Y - DOG_R, DOG_R),
          hives = listOf(HiveSpec(480f, 170f, bees, 0.19f)),
          platforms =
            listOf(
              PlatformRect(0f, GROUND_Y, WORLD_W, 130f, PlatformKind.GRASS_BRICK),
              PlatformRect(440f, 490f, 160f, 200f, PlatformKind.GRASS_BRICK),
            ),
          slopes = listOf(SlopeTriangle(260f, GROUND_Y, 440f, GROUND_Y, 440f, 490f)),
          movingObstacles =
            listOf(MovingObstacleSpec(280f, 340f, 510f, 340f, 52f, 52f, 140f, MovingKind.SPIKY_BALL)),
          inkBudget = ink,
        )
      2 -> // 3 hives siege
        LevelSpec(
          id = id,
          title = "Swarm Siege #$id",
          difficulty = tier,
          dog = DogSpec(300f, 560f - DOG_R, DOG_R),
          hives =
            listOf(
              HiveSpec(95f, 165f, bees / 3 + 2, 0.24f),
              HiveSpec(300f, 135f, bees / 3 + 2, 0.24f),
              HiveSpec(505f, 165f, bees / 3 + 2, 0.24f),
            ),
          platforms =
            listOf(
              PlatformRect(185f, 560f, 230f, 260f, PlatformKind.GRASS_BRICK),
              PlatformRect(0f, GROUND_Y, 185f, 130f, PlatformKind.GRASS_BRICK),
              PlatformRect(415f, GROUND_Y, 185f, 130f, PlatformKind.GRASS_BRICK),
            ),
          hazards =
            listOf(
              HazardZone(25f, GROUND_Y - 26f, 120f, 26f, HazardKind.SPIKES_UP),
              HazardZone(455f, GROUND_Y - 26f, 120f, 26f, HazardKind.SPIKES_UP),
            ),
          inkBudget = ink,
        )
      3 -> // Lava moat + spiky bat
        LevelSpec(
          id = id,
          title = "Magma Guard #$id",
          difficulty = tier,
          dog = DogSpec(300f, 540f - DOG_R, DOG_R),
          hives =
            listOf(
              HiveSpec(105f, 180f, bees / 2 + 1, 0.20f),
              HiveSpec(495f, 180f, bees / 2 + 1, 0.20f),
            ),
          platforms = listOf(PlatformRect(210f, 540f, 180f, 280f, PlatformKind.GRASS_BRICK)),
          hazards =
            listOf(
              HazardZone(0f, 690f, 210f, 130f, HazardKind.LAVA),
              HazardZone(390f, 690f, 210f, 130f, HazardKind.LAVA),
            ),
          movingObstacles =
            listOf(MovingObstacleSpec(160f, 340f, 440f, 340f, 64f, 64f, 155f, MovingKind.SPIKY_BAT)),
          inkBudget = ink,
        )
      4 -> // Crate bridge under bomb & hive threat
        LevelSpec(
          id = id,
          title = "Crate Blast #$id",
          difficulty = tier,
          dog = DogSpec(300f, 430f - DOG_R, DOG_R),
          hives = listOf(HiveSpec(300f, 700f, bees, 0.19f)),
          platforms =
            listOf(
              PlatformRect(0f, GROUND_Y, 190f, 130f, PlatformKind.GRASS_BRICK),
              PlatformRect(410f, GROUND_Y, 190f, 130f, PlatformKind.GRASS_BRICK),
              PlatformRect(205f, 430f, 190f, 28f, PlatformKind.WOOD_PLANK),
              PlatformRect(140f, 540f, 54f, 150f, PlatformKind.WOOD_CRATE),
              PlatformRect(406f, 540f, 54f, 150f, PlatformKind.WOOD_CRATE),
            ),
          bombs = listOf(BombSpec(185f, 360f, radius = 24f, fuseSeconds = 5f)),
          inkBudget = ink,
        )
      else -> // Vertical moving platform + spikes + boulder
        LevelSpec(
          id = id,
          title = "Elevator Buzz #$id",
          difficulty = tier,
          dog = DogSpec(460f, GROUND_Y - DOG_R, DOG_R),
          hives = listOf(HiveSpec(120f, 175f, bees, 0.19f)),
          platforms =
            listOf(
              PlatformRect(0f, GROUND_Y, WORLD_W, 130f, PlatformKind.GRASS_BRICK),
              PlatformRect(280f, 460f, 44f, 230f, PlatformKind.STONE_WALL),
            ),
          hazards = listOf(HazardZone(40f, GROUND_Y - 26f, 200f, 26f, HazardKind.SPIKES_UP)),
          movingObstacles =
            listOf(MovingObstacleSpec(150f, 310f, 150f, 530f, 90f, 24f, 120f, MovingKind.PLATFORM)),
          boulders = listOf(BoulderSpec(302f, 426f, radius = 32f)),
          inkBudget = ink,
        )
    }
  }

  // Levels 61-80: Multi-obstacle physics puzzles
  private fun buildTier6ComplexPhysics(id: Int, tier: DifficultyTier): LevelSpec {
    val variant = (id - 61) % 5
    val bees = 14 + (id - 61) / 4
    val ink = (740f - (id - 61) * 6f).coerceAtLeast(590f)
    return when (variant) {
      0 -> // Dual spiky balls + pit
        LevelSpec(
          id = id,
          title = "Crossfire #$id",
          difficulty = tier,
          dog = DogSpec(300f, GROUND_Y - DOG_R, DOG_R),
          hives =
            listOf(
              HiveSpec(95f, 165f, bees / 2 + 1, 0.18f),
              HiveSpec(505f, 165f, bees / 2 + 1, 0.18f),
            ),
          platforms =
            listOf(
              PlatformRect(0f, GROUND_Y, WORLD_W, 130f, PlatformKind.GRASS_BRICK),
              PlatformRect(185f, GROUND_Y - 90f, 48f, 90f, PlatformKind.WOOD_CRATE),
              PlatformRect(367f, GROUND_Y - 90f, 48f, 90f, PlatformKind.WOOD_CRATE),
            ),
          movingObstacles =
            listOf(
              MovingObstacleSpec(110f, 360f, 280f, 360f, 48f, 48f, 160f, MovingKind.SPIKY_BALL),
              MovingObstacleSpec(490f, 470f, 320f, 470f, 48f, 48f, 160f, MovingKind.SPIKY_BALL),
            ),
          inkBudget = ink,
        )
      1 -> // Narrow island + 3 hives + water
        LevelSpec(
          id = id,
          title = "Ocean Storm #$id",
          difficulty = tier,
          dog = DogSpec(300f, 500f - DOG_R, DOG_R),
          hives =
            listOf(
              HiveSpec(95f, 160f, bees / 3 + 2, 0.21f),
              HiveSpec(300f, 140f, bees / 3 + 2, 0.21f),
              HiveSpec(505f, 160f, bees / 3 + 2, 0.21f),
            ),
          platforms = listOf(PlatformRect(220f, 500f, 160f, 32f, PlatformKind.WOOD_PLANK)),
          hazards = listOf(HazardZone(0f, 690f, WORLD_W, 130f, HazardKind.WATER)),
          inkBudget = ink,
        )
      2 -> // Hanging platform + bottom hive + side spikes
        LevelSpec(
          id = id,
          title = "Sky Cage #$id",
          difficulty = tier,
          dog = DogSpec(300f, 430f - DOG_R, DOG_R),
          hives =
            listOf(
              HiveSpec(120f, 650f, bees / 2 + 1, 0.20f),
              HiveSpec(480f, 650f, bees / 2 + 1, 0.20f),
            ),
          platforms =
            listOf(
              PlatformRect(205f, 430f, 190f, 28f, PlatformKind.WOOD_PLANK),
              PlatformRect(0f, 750f, WORLD_W, 70f, PlatformKind.GRASS_BRICK),
            ),
          hazards =
            listOf(
              HazardZone(0f, 360f, 24f, 180f, HazardKind.SPIKES_RIGHT),
              HazardZone(576f, 360f, 24f, 180f, HazardKind.SPIKES_LEFT),
            ),
          inkBudget = ink,
        )
      3 -> // Asymmetric slope & water
        LevelSpec(
          id = id,
          title = "Cliff Hanger #$id",
          difficulty = tier,
          dog = DogSpec(145f, 500f - DOG_R, DOG_R),
          hives = listOf(HiveSpec(485f, 175f, bees + 2, 0.18f)),
          platforms =
            listOf(
              PlatformRect(40f, 500f, 200f, 320f, PlatformKind.GRASS_BRICK),
              PlatformRect(430f, 580f, 170f, 240f, PlatformKind.GRASS_BRICK),
            ),
          slopes = listOf(SlopeTriangle(430f, 580f, 560f, 580f, 560f, 440f)),
          hazards =
            listOf(
              HazardZone(240f, 700f, 190f, 120f, HazardKind.WATER),
              HazardZone(40f, 474f, 52f, 26f, HazardKind.SPIKES_UP),
            ),
          movingObstacles =
            listOf(MovingObstacleSpec(260f, 330f, 420f, 330f, 50f, 50f, 180f, MovingKind.SPIKY_BALL)),
          inkBudget = ink,
        )
      else -> // Twin platforms + moving ferry
        LevelSpec(
          id = id,
          title = "Bridge Master #$id",
          difficulty = tier,
          dog = DogSpec(465f, 500f - DOG_R, DOG_R),
          hives =
            listOf(
              HiveSpec(105f, 165f, bees / 2 + 1, 0.19f),
              HiveSpec(310f, 145f, bees / 2 + 1, 0.19f),
            ),
          platforms =
            listOf(
              PlatformRect(0f, 500f, 150f, 320f, PlatformKind.GRASS_BRICK),
              PlatformRect(370f, 500f, 230f, 320f, PlatformKind.GRASS_BRICK),
            ),
          hazards = listOf(HazardZone(150f, 690f, 220f, 130f, HazardKind.WATER)),
          movingObstacles =
            listOf(
              MovingObstacleSpec(165f, 360f, 320f, 360f, 90f, 24f, 140f, MovingKind.PLATFORM),
              MovingObstacleSpec(360f, 250f, 540f, 250f, 46f, 46f, 165f, MovingKind.SPIKY_BALL),
            ),
          inkBudget = ink,
        )
    }
  }

  // Levels 81-100: Master puzzles — tight ink, 3 hives, multi-hazards, unique setups
  private fun buildTier7MasterPuzzles(id: Int, tier: DifficultyTier): LevelSpec {
    val variant = (id - 81) % 5
    val bees = 16 + (id - 81) / 4
    val ink = (680f - (id - 81) * 5f).coerceAtLeast(560f)
    return when (variant) {
      0 -> // Peppermint pedestal over water + spiky bat guard
        LevelSpec(
          id = id,
          title = "Peppermint Sea #$id",
          difficulty = tier,
          dog = DogSpec(300f, 560f - DOG_R, DOG_R),
          hives =
            listOf(
              HiveSpec(105f, 210f, bees / 2 + 1, 0.18f),
              HiveSpec(495f, 210f, bees / 2 + 1, 0.18f),
            ),
          platforms = listOf(PlatformRect(240f, 560f, 120f, 44f, PlatformKind.PEPPERMINT)),
          hazards = listOf(HazardZone(0f, 695f, WORLD_W, 125f, HazardKind.WATER)),
          movingObstacles =
            listOf(MovingObstacleSpec(140f, 340f, 460f, 340f, 62f, 62f, 175f, MovingKind.SPIKY_BAT)),
          inkBudget = ink,
        )
      1 -> // Triple hive spike canyon
        LevelSpec(
          id = id,
          title = "Gauntlet #$id",
          difficulty = tier,
          dog = DogSpec(300f, GROUND_Y - DOG_R, DOG_R),
          hives =
            listOf(
              HiveSpec(90f, 165f, bees / 3 + 2, 0.18f),
              HiveSpec(300f, 135f, bees / 3 + 2, 0.18f),
              HiveSpec(510f, 165f, bees / 3 + 2, 0.18f),
            ),
          platforms =
            listOf(
              PlatformRect(0f, GROUND_Y, WORLD_W, 130f, PlatformKind.GRASS_BRICK),
              PlatformRect(150f, 480f, 95f, 24f, PlatformKind.WOOD_PLANK),
              PlatformRect(355f, 480f, 95f, 24f, PlatformKind.WOOD_PLANK),
            ),
          hazards =
            listOf(
              HazardZone(20f, GROUND_Y - 26f, 140f, 26f, HazardKind.SPIKES_UP),
              HazardZone(440f, GROUND_Y - 26f, 140f, 26f, HazardKind.SPIKES_UP),
            ),
          movingObstacles =
            listOf(MovingObstacleSpec(170f, 330f, 430f, 330f, 48f, 48f, 200f, MovingKind.SPIKY_BALL)),
          inkBudget = ink,
        )
      2 -> // Dual spiky balls + elevated island + water
        LevelSpec(
          id = id,
          title = "Sky Fortress #$id",
          difficulty = tier,
          dog = DogSpec(300f, 470f - DOG_R, DOG_R),
          hives =
            listOf(
              HiveSpec(105f, 165f, bees / 2 + 1, 0.17f),
              HiveSpec(495f, 620f, bees / 2 + 1, 0.17f),
            ),
          platforms =
            listOf(
              PlatformRect(215f, 470f, 170f, 30f, PlatformKind.WOOD_PLANK),
              PlatformRect(420f, 700f, 180f, 120f, PlatformKind.GRASS_BRICK),
            ),
          hazards = listOf(HazardZone(0f, 700f, 420f, 120f, HazardKind.WATER)),
          movingObstacles =
            listOf(
              MovingObstacleSpec(120f, 300f, 480f, 300f, 48f, 48f, 185f, MovingKind.SPIKY_BALL),
              MovingObstacleSpec(480f, 560f, 120f, 560f, 48f, 48f, 165f, MovingKind.SPIKY_BALL),
            ),
          inkBudget = ink,
        )
      3 -> // Crates + slopes + 3 hives
        LevelSpec(
          id = id,
          title = "Bee Tempest #$id",
          difficulty = tier,
          dog = DogSpec(300f, 600f - DOG_R, DOG_R),
          hives =
            listOf(
              HiveSpec(95f, 160f, bees / 3 + 2, 0.17f),
              HiveSpec(300f, 140f, bees / 3 + 2, 0.17f),
              HiveSpec(505f, 160f, bees / 3 + 2, 0.17f),
            ),
          platforms =
            listOf(
              PlatformRect(0f, 510f, 150f, 310f, PlatformKind.GRASS_BRICK),
              PlatformRect(450f, 510f, 150f, 310f, PlatformKind.GRASS_BRICK),
              PlatformRect(150f, 600f, 300f, 220f, PlatformKind.GRASS_BRICK),
              PlatformRect(185f, 535f, 46f, 65f, PlatformKind.WOOD_CRATE),
              PlatformRect(369f, 535f, 46f, 65f, PlatformKind.WOOD_CRATE),
            ),
          hazards =
            listOf(
              HazardZone(150f, 510f, 22f, 80f, HazardKind.SPIKES_RIGHT),
              HazardZone(428f, 510f, 22f, 80f, HazardKind.SPIKES_LEFT),
            ),
          inkBudget = ink,
        )
      else -> // Final gauntlet: lava canyon, overhead portal, spiky bat & bomb
        LevelSpec(
          id = id,
          title = if (id == 100) "Grand Finale" else "Legend Trial #$id",
          difficulty = tier,
          dog = DogSpec(300f, 520f - DOG_R, DOG_R),
          hives =
            listOf(
              HiveSpec(90f, 160f, bees / 3 + 3, 0.16f),
              HiveSpec(300f, 140f, bees / 3 + 3, 0.16f),
              HiveSpec(510f, 160f, bees / 3 + 3, 0.16f),
            ),
          portals = listOf(PortalSpec(300f, 240f, radius = 30f)),
          platforms =
            listOf(
              PlatformRect(215f, 520f, 170f, 42f, PlatformKind.PEPPERMINT),
              PlatformRect(0f, 640f, 110f, 180f, PlatformKind.GRASS_BRICK),
              PlatformRect(490f, 640f, 110f, 180f, PlatformKind.GRASS_BRICK),
            ),
          hazards =
            listOf(
              HazardZone(110f, 695f, 380f, 125f, HazardKind.LAVA),
              HazardZone(20f, 614f, 64f, 26f, HazardKind.SPIKES_UP),
              HazardZone(516f, 614f, 64f, 26f, HazardKind.SPIKES_UP),
            ),
          movingObstacles =
            listOf(
              MovingObstacleSpec(130f, 330f, 470f, 330f, 60f, 60f, 185f, MovingKind.SPIKY_BAT)
            ),
          bombs = listOf(BombSpec(180f, 470f, radius = 24f, fuseSeconds = 5f)),
          inkBudget = ink,
        )
    }
  }

  private fun createGuideDome(cx: Float, baseY: Float, radius: Float): List<Vec2> {
    val pts = mutableListOf<Vec2>()
    val steps = 16
    for (i in 0..steps) {
      val angle = Math.PI + (Math.PI * i / steps)
      pts.add(Vec2(cx + (cos(angle) * radius).toFloat(), baseY + (sin(angle) * radius).toFloat()))
    }
    return pts
  }
}
