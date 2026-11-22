package com.example

import com.example.engine.GameSimulation
import com.example.levels.LevelRepository
import com.example.model.FailReason
import com.example.model.GamePhase
import com.example.model.Vec2
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GamePhysicsUnitTest {
  @Test
  fun all100LevelsAreValid() {
    assertEquals(100, LevelRepository.levels.size)
    LevelRepository.levels.forEach { lvl ->
      assertTrue("Level ${lvl.id} has hives", lvl.hives.isNotEmpty())
      assertTrue("Level ${lvl.id} has platforms", lvl.platforms.isNotEmpty())
      assertTrue("Level ${lvl.id} has positive ink", lvl.inkBudget >= 500f)
    }
  }

  @Test
  fun protectiveDomeSavesDogInLevel1() {
    val lvl1 = LevelRepository.getLevel(1)
    var wonStars = 0
    val sim =
      GameSimulation(
        level = lvl1,
        onWin = { stars, _ -> wonStars = stars },
      )

    // Draw closed dome anchored to the ground around dog at (300, 658)
    val cx = lvl1.dog.x
    val groundY = 690f
    val r = 82f
    sim.beginDraw(Vec2(cx - r, groundY - 4f))
    for (i in 1..20) {
      val ang = PI + (PI * i / 20.0)
      sim.continueDraw(Vec2(cx + (cos(ang) * r).toFloat(), groundY - 4f + (sin(ang) * r).toFloat()))
    }
    assertTrue(sim.finishDraw())
    assertEquals(GamePhase.SIMULATING, sim.phase)

    // Simulate 10.5 seconds at 60Hz
    repeat(630) {
      sim.step(1f / 60f, highGraphics = false)
    }

    val snap = sim.snapshot()
    assertEquals(
      "phase=${snap.phase}, timeLeft=${snap.timeLeft}, reason=${snap.failReason}, bodyPos=${snap.drawnBody?.pos}, dogPos=${snap.dog.pos}, bees=${snap.bees.map { it.pos }}",
      GamePhase.WON,
      sim.phase,
    )
    assertTrue("Expected at least 1 star", wonStars in 1..3)
  }

  @Test
  fun unshieldedDogGetsStungByBees() {
    val lvl1 = LevelRepository.getLevel(1)
    var lostReason: FailReason? = null
    val sim =
      GameSimulation(
        level = lvl1,
        onLose = { reason -> lostReason = reason },
      )

    // Draw a tiny line far away in upper-right corner so bees have clear path to dog
    sim.beginDraw(Vec2(520f, 80f))
    sim.continueDraw(Vec2(560f, 80f))
    assertTrue(sim.finishDraw())

    repeat(600) {
      if (sim.phase == GamePhase.LOST) return@repeat
      sim.step(1f / 60f, highGraphics = false)
    }

    assertEquals(GamePhase.LOST, sim.phase)
    assertEquals(FailReason.BEE_STING, lostReason)
  }

  @Test
  fun signatureLevelsMatchReferenceMechanics() {
    val lvl22 = LevelRepository.getLevel(22)
    assertTrue("Level 22 has bottom hive", lvl22.hives.any { it.y > 650f })
    assertTrue("Level 22 has wooden crates", lvl22.platforms.size >= 6)

    val lvl30 = LevelRepository.getLevel(30)
    assertTrue("Level 30 has slope", lvl30.slopes.isNotEmpty())
    assertTrue("Level 30 has spiky ball", lvl30.movingObstacles.isNotEmpty())

    val lvl46 = LevelRepository.getLevel(46)
    assertTrue("Level 46 has lava", lvl46.hazards.any { it.kind == com.example.model.HazardKind.LAVA })
    assertTrue("Level 46 has portal", lvl46.portals.isNotEmpty())
    assertTrue(
      "Level 46 has spiky bat",
      lvl46.movingObstacles.any { it.kind == com.example.model.MovingKind.SPIKY_BAT },
    )

    val lvl63 = LevelRepository.getLevel(63)
    assertTrue("Level 63 has lava", lvl63.hazards.any { it.kind == com.example.model.HazardKind.LAVA })
    assertTrue("Level 63 has boulder", lvl63.boulders.isNotEmpty())
    assertTrue("Level 63 has timed bomb", lvl63.bombs.isNotEmpty())

    val lvl76 = LevelRepository.getLevel(76)
    assertTrue(
      "Level 76 has peppermint platform",
      lvl76.platforms.any { it.kind == com.example.model.PlatformKind.PEPPERMINT },
    )
    assertTrue("Level 76 has water", lvl76.hazards.any { it.kind == com.example.model.HazardKind.WATER })
    assertEquals(2, lvl76.hives.size)
  }

  @Test
  fun bombShieldBlocksBlastAndUnshieldedExplodesDog() {
    val base = LevelRepository.getLevel(63)
    val closeBombLevel =
      base.copy(
        hives = emptyList(),
        boulders = emptyList(),
        bombs = listOf(com.example.model.BombSpec(base.dog.x, base.dog.y - 110f, radius = 26f, fuseSeconds = 1.5f)),
      )

    // Unshielded triggers BOMB_BLAST after fuse
    val simFail = GameSimulation(level = closeBombLevel)
    simFail.beginDraw(Vec2(40f, 80f))
    simFail.continueDraw(Vec2(120f, 80f))
    assertTrue(simFail.finishDraw())
    repeat(120) {
      if (simFail.phase == GamePhase.LOST) return@repeat
      simFail.step(1f / 60f, highGraphics = false)
    }
    assertEquals(GamePhase.LOST, simFail.phase)
    assertEquals(FailReason.BOMB_BLAST, simFail.snapshot().failReason)

    // Shielded by drawn horizontal barrier resting across U-pit ledges blocks blast
    val simPass = GameSimulation(level = closeBombLevel)
    simPass.beginDraw(Vec2(270f, 540f))
    simPass.continueDraw(Vec2(495f, 540f))
    assertTrue(simPass.finishDraw())
    repeat(120) { simPass.step(1f / 60f, highGraphics = false) }
    assertEquals(GamePhase.SIMULATING, simPass.phase)
  }

  @Test
  fun penModelsCatalogIncludesRedGreenYellowBlack() {
    val ids = com.example.data.GameCatalog.penModels.map { it.id }.toSet()
    assertTrue(ids.containsAll(setOf("red", "green", "yellow", "black")))
    assertEquals("red", com.example.data.GameCatalog.findPenModel("red").id)
    assertEquals("green", com.example.data.GameCatalog.findPenModel("green").id)
    assertEquals("yellow", com.example.data.GameCatalog.findPenModel("yellow").id)
    assertEquals("black", com.example.data.GameCatalog.findPenModel("black").id)
  }
}
