package com.example.engine

import androidx.compose.ui.graphics.Color
import com.example.levels.LevelRepository
import com.example.model.DogMood
import com.example.model.FailReason
import com.example.model.GamePhase
import com.example.model.HazardKind
import com.example.model.LevelSpec
import com.example.model.MovingKind
import com.example.model.PlatformRect
import com.example.model.SlopeTriangle
import com.example.model.Vec2
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

data class DrawnLineBody(
  val localPoints: List<Vec2>,
  var pos: Vec2,
  var vel: Vec2 = Vec2(),
  var angle: Float = 0f,
  var omega: Float = 0f,
  val mass: Float,
  val inertia: Float,
  val thickness: Float = 13f,
) {
  var worldPoints: List<Vec2> = localPoints.map { it + pos }
    internal set

  fun updateWorldPoints() {
    val c = cos(angle)
    val s = sin(angle)
    worldPoints =
      localPoints.map { lp ->
        Vec2(pos.x + lp.x * c - lp.y * s, pos.y + lp.x * s + lp.y * c)
      }
  }

  fun snapshotCopy(): DrawnLineBody {
    val copied = copy()
    copied.worldPoints = worldPoints.toList()
    return copied
  }
}

data class DogBody(
  var pos: Vec2,
  var vel: Vec2 = Vec2(),
  val radius: Float = 32f,
  var angle: Float = 0f,
  var mood: DogMood = DogMood.CALM,
  var blinkTimer: Float = 2.5f,
  var isBlinking: Boolean = false,
)

data class BeeEntity(
  var pos: Vec2,
  var vel: Vec2,
  val radius: Float = 12f,
  val phase: Float = Random.nextFloat() * 6.28f,
  var wingPhase: Float = Random.nextFloat() * 6.28f,
  var tangentDir: Float = if (Random.nextBoolean()) 1f else -1f,
  var stuckTimer: Float = 0f,
)

data class MovingObstacleState(
  val startX: Float,
  val startY: Float,
  val endX: Float,
  val endY: Float,
  val w: Float,
  val h: Float,
  val speed: Float,
  val kind: MovingKind,
  var progress: Float = 0f,
  var dir: Float = 1f,
  var x: Float = startX,
  var y: Float = startY,
  var vx: Float = 0f,
  var vy: Float = 0f,
  var spinAngle: Float = 0f,
)

data class BoulderState(
  var pos: Vec2,
  var vel: Vec2 = Vec2(),
  val radius: Float = 32f,
  var angle: Float = 0f,
)

data class BombState(
  val pos: Vec2,
  val radius: Float = 24f,
  var fuseLeft: Float = 5f,
  var exploded: Boolean = false,
  var blastTimer: Float = 0f,
)

data class Particle(
  var pos: Vec2,
  var vel: Vec2,
  var life: Float,
  val maxLife: Float,
  val size: Float,
  val color: Color,
  val isHeart: Boolean = false,
)

data class SimulationSnapshot(
  val level: LevelSpec,
  val phase: GamePhase,
  val timeLeft: Float,
  val inkUsed: Float,
  val inkBudget: Float,
  val dog: DogBody,
  val bees: List<BeeEntity>,
  val rawDrawPoints: List<Vec2>,
  val drawnBody: DrawnLineBody?,
  val movingObstacles: List<MovingObstacleState>,
  val boulders: List<BoulderState>,
  val bombs: List<BombState>,
  val particles: List<Particle>,
  val failReason: FailReason?,
  val earnedStars: Int,
  val earnedCoins: Int,
  val elapsed: Float,
  val hivePulse: Float,
)

class GameSimulation(
  var level: LevelSpec,
  private val onBeeBounce: () -> Unit = {},
  private val onBeeSpawn: () -> Unit = {},
  private val onWin: (stars: Int, coins: Int) -> Unit = { _, _ -> },
  private val onLose: (FailReason) -> Unit = {},
) {
  var phase: GamePhase = GamePhase.DRAWING
    private set

  private var timeLeft: Float = level.survivalSeconds
  private var inkUsed: Float = 0f
  private val rawPoints = mutableListOf<Vec2>()
  private var drawnBody: DrawnLineBody? = null
  private val dog = DogBody(pos = Vec2(level.dog.x, level.dog.y), radius = level.dog.radius)
  private val bees = mutableListOf<BeeEntity>()
  private var spawnCounts = IntArray(0)
  private var spawnTimers = FloatArray(0)
  private val movingStates = mutableListOf<MovingObstacleState>()
  private val boulderStates = mutableListOf<BoulderState>()
  private val bombStates = mutableListOf<BombState>()
  private val particles = mutableListOf<Particle>()
  private var failReason: FailReason? = null
  private var earnedStars: Int = 0
  private var earnedCoins: Int = 0
  private var elapsed: Float = 0f
  private var hivePulse: Float = 0f
  private var phaseBeforePause: GamePhase = GamePhase.DRAWING

  init {
    reset(level)
  }

  fun reset(newLevel: LevelSpec = level) {
    level = newLevel
    phase = GamePhase.DRAWING
    timeLeft = newLevel.survivalSeconds
    inkUsed = 0f
    rawPoints.clear()
    drawnBody = null
    dog.pos = Vec2(newLevel.dog.x, newLevel.dog.y)
    dog.vel = Vec2()
    dog.angle = 0f
    dog.mood = DogMood.CALM
    bees.clear()
    particles.clear()
    failReason = null
    earnedStars = 0
    earnedCoins = 0
    elapsed = 0f
    hivePulse = 0f
    movingStates.clear()
    newLevel.movingObstacles.forEach { m ->
      movingStates.add(
        MovingObstacleState(
          startX = m.startX,
          startY = m.startY,
          endX = m.endX,
          endY = m.endY,
          w = m.w,
          h = m.h,
          speed = m.speed,
          kind = m.kind,
          progress = m.phaseOffset,
        )
      )
    }
    boulderStates.clear()
    newLevel.boulders.forEach { b ->
      boulderStates.add(BoulderState(pos = Vec2(b.x, b.y), radius = b.radius))
    }
    bombStates.clear()
    newLevel.bombs.forEach { b ->
      bombStates.add(BombState(pos = Vec2(b.x, b.y), radius = b.radius, fuseLeft = b.fuseSeconds))
    }
    // Re-init hive trackers cleanly
    resetHiveTrackers(newLevel)
  }

  private fun resetHiveTrackers(lvl: LevelSpec) {
    spawnCounts = IntArray(lvl.hives.size)
    spawnTimers = FloatArray(lvl.hives.size) { 0.08f }
  }

  fun pause() {
    if (phase == GamePhase.DRAWING || phase == GamePhase.SIMULATING) {
      phaseBeforePause = phase
      phase = GamePhase.PAUSED
    }
  }

  fun resume() {
    if (phase == GamePhase.PAUSED) {
      phase = phaseBeforePause
    }
  }

  fun beginDraw(worldPoint: Vec2): Boolean {
    if (phase != GamePhase.DRAWING) return false
    rawPoints.clear()
    inkUsed = 0f
    val clamped = clampDrawPoint(worldPoint)
    rawPoints.add(clamped)
    return true
  }

  fun continueDraw(worldPoint: Vec2): Boolean {
    if (phase != GamePhase.DRAWING || rawPoints.isEmpty()) return false
    if (inkUsed >= level.inkBudget) return false
    val clamped = clampDrawPoint(worldPoint)
    val last = rawPoints.last()
    val d = Vec2.dist(last, clamped)
    if (d < 8f) return false

    val remaining = level.inkBudget - inkUsed
    if (d <= remaining) {
      rawPoints.add(clamped)
      inkUsed += d
    } else if (remaining > 2f) {
      val dir = (clamped - last).normalized()
      rawPoints.add(last + dir * remaining)
      inkUsed = level.inkBudget
    }
    return true
  }

  fun finishDraw(): Boolean {
    if (phase != GamePhase.DRAWING) return false
    if (rawPoints.size < 2 || inkUsed < 18f) {
      rawPoints.clear()
      inkUsed = 0f
      return false
    }

    // Build rigid/dynamic DrawnLineBody around weighted centroid
    var totalLen = 0f
    var cx = 0f
    var cy = 0f
    for (i in 0 until rawPoints.size - 1) {
      val a = rawPoints[i]
      val b = rawPoints[i + 1]
      val segLen = Vec2.dist(a, b)
      val mid = (a + b) * 0.5f
      cx += mid.x * segLen
      cy += mid.y * segLen
      totalLen += segLen
    }
    val centroid = if (totalLen > 1f) Vec2(cx / totalLen, cy / totalLen) else rawPoints.first()
    val localPts = rawPoints.map { it - centroid }

    var inertia = 0f
    for (p in localPts) {
      inertia += p.lenSq()
    }
    val mass = (totalLen / 85f).coerceIn(1.2f, 14f)
    val safeInertia = (inertia * mass / localPts.size.coerceAtLeast(1)).coerceAtLeast(1400f)

    drawnBody =
      DrawnLineBody(
        localPoints = localPts,
        pos = centroid,
        mass = mass,
        inertia = safeInertia,
        thickness = 13f,
      ).apply { updateWorldPoints() }

    phase = GamePhase.SIMULATING
    return true
  }

  private fun clampDrawPoint(p: Vec2): Vec2 {
    var pt = Vec2(p.x.coerceIn(8f, LevelRepository.WORLD_W - 8f), p.y.coerceIn(8f, LevelRepository.WORLD_H - 8f))
    val lineR = 6.5f
    // Keep outside dog circle
    val minDogDist = dog.radius + lineR + 5f
    val toDog = pt - dog.pos
    val dogD = toDog.len()
    if (dogD < minDogDist) {
      pt = dog.pos + (if (dogD > 0.1f) toDog.normalized() else Vec2(0f, -1f)) * minDogDist
    }
    // Keep outside hives
    for (h in level.hives) {
      val hivePos = Vec2(h.x, h.y)
      val minHiveDist = h.radius + lineR + 4f
      val diff = pt - hivePos
      val d = diff.len()
      if (d < minHiveDist) {
        pt = hivePos + (if (d > 0.1f) diff.normalized() else Vec2(0f, 1f)) * minHiveDist
      }
    }
    // Keep outside portals (solid hook pegs)
    for (portal in level.portals) {
      val pPos = Vec2(portal.x, portal.y)
      val minPortalDist = portal.radius + lineR + 3f
      val diff = pt - pPos
      val d = diff.len()
      if (d < minPortalDist) {
        pt = pPos + (if (d > 0.1f) diff.normalized() else Vec2(0f, 1f)) * minPortalDist
      }
    }
    // Push out of static platforms
    for (plat in level.platforms) {
      val pad = lineR + 1f
      val left = plat.left - pad
      val right = plat.right + pad
      val top = plat.top - pad
      val bottom = plat.bottom + pad
      if (pt.x > left && pt.x < right && pt.y > top && pt.y < bottom) {
        val dL = pt.x - left
        val dR = right - pt.x
        val dT = pt.y - top
        val dB = bottom - pt.y
        val m = min(min(dL, dR), min(dT, dB))
        pt =
          when (m) {
            dT -> Vec2(pt.x, top)
            dL -> Vec2(left, pt.y)
            dR -> Vec2(right, pt.y)
            else -> Vec2(pt.x, bottom)
          }
      }
    }
    return pt
  }

  fun step(dt: Float, highGraphics: Boolean = true) {
    val safeDt = dt.coerceIn(0.001f, 0.04f)
    elapsed += safeDt

    // Idle blink animation in all states except LOST
    dog.blinkTimer -= safeDt
    if (dog.blinkTimer <= 0f) {
      if (dog.isBlinking) {
        dog.isBlinking = false
        dog.blinkTimer = 2.2f + Random.nextFloat() * 2.2f
      } else {
        dog.isBlinking = true
        dog.blinkTimer = 0.13f
      }
    }

    updateParticles(safeDt)

    if (phase == GamePhase.PAUSED || phase == GamePhase.WON || phase == GamePhase.LOST) {
      if (phase == GamePhase.WON && highGraphics && Random.nextFloat() < 0.28f) {
        particles.add(
          Particle(
            pos = Vec2(dog.pos.x + (Random.nextFloat() - 0.5f) * 80f, dog.pos.y - 25f),
            vel = Vec2((Random.nextFloat() - 0.5f) * 60f, -90f - Random.nextFloat() * 60f),
            life = 0f,
            maxLife = 1.1f,
            size = 10f,
            color = listOf(Color(0xFFFF5252), Color(0xFFFFC107), Color(0xFF4CAF50), Color(0xFF29B6F6)).random(),
            isHeart = Random.nextFloat() < 0.35f,
          )
        )
      }
      return
    }

    // In DRAWING phase, only gently animate moving obstacles preview? No, keep world paused until release like Save The Dog!
    if (phase != GamePhase.SIMULATING) return

    // Update countdown timer
    timeLeft = (timeLeft - safeDt).coerceAtLeast(0f)

    // Spawn bees from hives
    level.hives.forEachIndexed { idx, hive ->
      if (idx < spawnCounts.size && spawnCounts[idx] < hive.beeCount) {
        spawnTimers[idx] -= safeDt
        if (spawnTimers[idx] <= 0f) {
          spawnTimers[idx] = hive.spawnInterval
          spawnCounts[idx]++
          hivePulse = 1f
          val angleToDog = atan2(dog.pos.y - hive.y, dog.pos.x - hive.x) + (Random.nextFloat() - 0.5f) * 1.1f
          val spawnPos = Vec2(hive.x + cos(angleToDog) * 18f, hive.y + sin(angleToDog) * 18f)
          bees.add(
            BeeEntity(
              pos = spawnPos,
              vel = Vec2(cos(angleToDog) * 155f, sin(angleToDog) * 155f),
              tangentDir = if (spawnCounts[idx] % 2 == 0) 1f else -1f,
            )
          )
          if (spawnCounts[idx] == 1) onBeeSpawn()
        }
      }
    }
    hivePulse = (hivePulse - safeDt * 3.5f).coerceAtLeast(0f)

    // Physics sub-stepping for rock-solid barrier collision
    val subSteps = 4
    val subDt = safeDt / subSteps
    for (s in 0 until subSteps) {
      updateMovingObstacles(subDt)
      stepDrawnBody(subDt)
      stepBoulders(subDt)
      stepBombs(subDt, highGraphics)
      if (phase != GamePhase.SIMULATING) return
      stepDog(subDt)
      if (phase != GamePhase.SIMULATING) return
      stepBees(subDt, highGraphics)
      if (phase != GamePhase.SIMULATING) return
    }

    // Update dog expression based on nearest bee distance
    val minBeeDist = bees.minOfOrNull { Vec2.dist(it.pos, dog.pos) } ?: 999f
    dog.mood = if (minBeeDist < 175f) DogMood.SCARED else DogMood.CALM

    // Win check
    if (timeLeft <= 0f && phase == GamePhase.SIMULATING) {
      triggerWin(highGraphics)
    }
  }

  private fun updateMovingObstacles(dt: Float) {
    for (m in movingStates) {
      val path = Vec2(m.endX - m.startX, m.endY - m.startY)
      val dist = path.len()
      val prevX = m.x
      val prevY = m.y
      if (dist > 1f) {
        val deltaProg = (m.speed * dt) / dist
        m.progress += deltaProg * m.dir
        if (m.progress >= 1f) {
          m.progress = 1f
          m.dir = -1f
        } else if (m.progress <= 0f) {
          m.progress = 0f
          m.dir = 1f
        }
        m.x = m.startX + path.x * m.progress
        m.y = m.startY + path.y * m.progress
      }
      m.vx = (m.x - prevX) / dt
      m.vy = (m.y - prevY) / dt
      if (m.kind == MovingKind.SPIKY_BALL) {
        m.spinAngle += 4.2f * dt * m.dir
      }
    }
  }

  private fun stepDrawnBody(dt: Float) {
    val body = drawnBody ?: return
    // Gravity
    body.vel = Vec2(body.vel.x * 0.992f, body.vel.y + 540f * dt)
    body.omega *= 0.96f
    body.pos += body.vel * dt
    body.angle += body.omega * dt
    body.updateWorldPoints()

    val r = body.thickness * 0.5f

    // Collide every point of the drawn line with static platforms, slopes, moving platforms, and bounds
    for (iter in 0 until 2) {
      for (lp in body.localPoints) {
        val c = cos(body.angle)
        val s = sin(body.angle)
        val pt = Vec2(body.pos.x + lp.x * c - lp.y * s, body.pos.y + lp.x * s + lp.y * c)
        // Static platform rects
        for (plat in level.platforms) {
          resolveBodyVsRect(body, pt, r, plat.x, plat.y, plat.w, plat.h, 0f, 0f)
        }
        // Moving platforms
        for (m in movingStates) {
          if (m.kind == MovingKind.PLATFORM) {
            val c2 = cos(body.angle)
            val s2 = sin(body.angle)
            val curPt = Vec2(body.pos.x + lp.x * c2 - lp.y * s2, body.pos.y + lp.x * s2 + lp.y * c2)
            resolveBodyVsRect(body, curPt, r, m.x, m.y, m.w, m.h, m.vx, m.vy)
          }
        }
        // Slopes
        for (slope in level.slopes) {
          val c2 = cos(body.angle)
          val s2 = sin(body.angle)
          val curPt = Vec2(body.pos.x + lp.x * c2 - lp.y * s2, body.pos.y + lp.x * s2 + lp.y * c2)
          resolveBodyVsTriangle(body, curPt, r, slope)
        }
        // Portal pegs (drawn lines can hook onto overhead portals)
        for (portal in level.portals) {
          val c2 = cos(body.angle)
          val s2 = sin(body.angle)
          val curPt = Vec2(body.pos.x + lp.x * c2 - lp.y * s2, body.pos.y + lp.x * s2 + lp.y * c2)
          val pCenter = Vec2(portal.x, portal.y)
          val diff = curPt - pCenter
          val minD = portal.radius + r
          val d2 = diff.lenSq()
          if (d2 < minD * minD) {
            val d = hypot(diff.x, diff.y)
            val n = if (d > 1e-4f) diff / d else Vec2(0f, -1f)
            applyBodyCorrection(body, curPt, n * (minD - d), n, 0f, 0f)
          }
        }
        // World bounds
        val c3 = cos(body.angle)
        val s3 = sin(body.angle)
        val bPt = Vec2(body.pos.x + lp.x * c3 - lp.y * s3, body.pos.y + lp.x * s3 + lp.y * c3)
        if (bPt.x - r < 4f) {
          applyBodyCorrection(body, bPt, Vec2(4f + r - bPt.x, 0f), Vec2(1f, 0f), 0f, 0f)
        } else if (bPt.x + r > LevelRepository.WORLD_W - 4f) {
          applyBodyCorrection(body, bPt, Vec2(LevelRepository.WORLD_W - 4f - r - bPt.x, 0f), Vec2(-1f, 0f), 0f, 0f)
        }
        if (bPt.y + r > LevelRepository.WORLD_H - 4f) {
          applyBodyCorrection(body, bPt, Vec2(0f, LevelRepository.WORLD_H - 4f - r - bPt.y), Vec2(0f, -1f), 0f, 0f)
        }
      }
      body.updateWorldPoints()
    }
  }

  private fun resolveBodyVsRect(
    body: DrawnLineBody,
    pt: Vec2,
    r: Float,
    rx: Float,
    ry: Float,
    rw: Float,
    rh: Float,
    surfVx: Float,
    surfVy: Float,
  ) {
    val cx = pt.x.coerceIn(rx, rx + rw)
    val cy = pt.y.coerceIn(ry, ry + rh)
    val dx = pt.x - cx
    val dy = pt.y - cy
    val distSq = dx * dx + dy * dy
    if (distSq < r * r) {
      val dist = hypot(dx, dy)
      val normal: Vec2
      val pen: Float
      if (dist > 1e-4f) {
        normal = Vec2(dx / dist, dy / dist)
        pen = r - dist
      } else {
        // Point inside rect
        val dT = pt.y - ry
        val dB = ry + rh - pt.y
        val dL = pt.x - rx
        val dR = rx + rw - pt.x
        val m = min(min(dT, dB), min(dL, dR))
        when (m) {
          dT -> {
            normal = Vec2(0f, -1f)
            pen = dT + r
          }
          dL -> {
            normal = Vec2(-1f, 0f)
            pen = dL + r
          }
          dR -> {
            normal = Vec2(1f, 0f)
            pen = dR + r
          }
          else -> {
            normal = Vec2(0f, 1f)
            pen = dB + r
          }
        }
      }
      applyBodyCorrection(body, pt, normal * pen, normal, surfVx, surfVy)
    }
  }

  private fun resolveBodyVsTriangle(body: DrawnLineBody, pt: Vec2, r: Float, t: SlopeTriangle) {
    val v1 = Vec2(t.x1, t.y1)
    val v2 = Vec2(t.x2, t.y2)
    val v3 = Vec2(t.x3, t.y3)
    val edges = listOf(v1 to v2, v2 to v3, v3 to v1)
    val inside = pointInTriangle(pt, v1, v2, v3)
    var bestPt = v1
    var bestD2 = Float.MAX_VALUE
    for ((a, b) in edges) {
      val cp = Vec2.closestPointOnSegment(pt, a, b)
      val d2 = (pt - cp).lenSq()
      if (d2 < bestD2) {
        bestD2 = d2
        bestPt = cp
      }
    }
    if (inside || bestD2 < r * r) {
      val d = hypot(pt.x - bestPt.x, pt.y - bestPt.y)
      val normal =
        if (inside) (bestPt - pt).normalized()
        else if (d > 1e-4f) (pt - bestPt) / d
        else Vec2(0f, -1f)
      val pen = if (inside) d + r else r - d
      applyBodyCorrection(body, pt, normal * pen, normal, 0f, 0f)
    }
  }

  private fun applyBodyCorrection(
    body: DrawnLineBody,
    contactPt: Vec2,
    correction: Vec2,
    normal: Vec2,
    surfVx: Float,
    surfVy: Float,
  ) {
    body.pos += correction * 0.85f
    val arm = contactPt - body.pos
    val ptVel = Vec2(body.vel.x - body.omega * arm.y, body.vel.y + body.omega * arm.x)
    val relVel = Vec2(ptVel.x - surfVx, ptVel.y - surfVy)
    val vn = relVel.dot(normal)
    if (vn < 0f) {
      val restitution = 0.0f
      val jn = -(1f + restitution) * vn
      val rCrossN = arm.cross(normal)
      val denom = (1f / body.mass) + (rCrossN * rCrossN) / body.inertia
      val impulseMag = jn / denom
      val impulse = normal * impulseMag
      body.vel += impulse / body.mass
      body.omega = (body.omega + arm.cross(impulse) / body.inertia).coerceIn(-4f, 4f)

      // Friction along tangent (stronger when resting on top of a surface)
      val tangent = Vec2(-normal.y, normal.x)
      val vt = relVel.dot(tangent)
      val frictionFactor = if (normal.y < -0.5f) 0.85f else 0.5f
      val jt = (-vt * frictionFactor) / denom
      val frictionImpulse = tangent * jt
      body.vel += frictionImpulse / body.mass
      body.omega = (body.omega + arm.cross(frictionImpulse) / body.inertia).coerceIn(-4f, 4f)
      if (normal.y < -0.5f) {
        body.vel = Vec2(body.vel.x * 0.82f, body.vel.y)
        body.omega *= 0.82f
      }
    }
  }

  private fun stepDog(dt: Float) {
    dog.vel = Vec2(dog.vel.x * 0.985f, dog.vel.y + 620f * dt)
    dog.pos += dog.vel * dt

    // Collide dog vs static platforms
    for (plat in level.platforms) {
      resolveCircleVsRect(dog, plat.x, plat.y, plat.w, plat.h, 0f, 0f)
    }
    // Moving platforms
    for (m in movingStates) {
      if (m.kind == MovingKind.PLATFORM) {
        resolveCircleVsRect(dog, m.x, m.y, m.w, m.h, m.vx, m.vy)
      }
    }
    // Slopes
    for (slope in level.slopes) {
      resolveCircleVsTriangle(dog, slope)
    }
    // Drawn line vs Dog (push drawn line slightly more than dog so shield cushions dog cleanly)
    val body = drawnBody
    if (body != null) {
      val pts = body.worldPoints
      val minDist = dog.radius + body.thickness * 0.5f
      for (i in 0 until pts.size - 1) {
        val cp = Vec2.closestPointOnSegment(dog.pos, pts[i], pts[i + 1])
        val diff = dog.pos - cp
        val d = diff.len()
        if (d < minDist) {
          val n = if (d > 1e-4f) diff / d else Vec2(0f, -1f)
          val pen = minDist - d
          // Split positional correction: 75% lifts the line, 25% nudges dog
          body.pos -= n * (pen * 0.7f)
          dog.pos += n * (pen * 0.3f)
          val vn = (dog.vel - body.vel).dot(n)
          if (vn < 0f) {
            dog.vel -= n * (vn * 0.4f)
            body.vel += n * (vn * 0.4f)
          }
          body.updateWorldPoints()
        }
      }
    }

    // Clamp horizontally inside world
    if (dog.pos.x < dog.radius + 4f) {
      dog.pos = Vec2(dog.radius + 4f, dog.pos.y)
      dog.vel = Vec2(abs(dog.vel.x) * 0.3f, dog.vel.y)
    } else if (dog.pos.x > LevelRepository.WORLD_W - dog.radius - 4f) {
      dog.pos = Vec2(LevelRepository.WORLD_W - dog.radius - 4f, dog.pos.y)
      dog.vel = Vec2(-abs(dog.vel.x) * 0.3f, dog.vel.y)
    }

    // Hazard checks
    for (hz in level.hazards) {
      when (hz.kind) {
        HazardKind.WATER -> {
          if (
            dog.pos.x + dog.radius * 0.5f > hz.x &&
              dog.pos.x - dog.radius * 0.5f < hz.x + hz.w &&
              dog.pos.y + dog.radius * 0.45f > hz.y
          ) {
            triggerLose(FailReason.WATER_FALL)
            return
          }
        }
        HazardKind.LAVA -> {
          if (
            dog.pos.x + dog.radius * 0.5f > hz.x &&
              dog.pos.x - dog.radius * 0.5f < hz.x + hz.w &&
              dog.pos.y + dog.radius * 0.45f > hz.y
          ) {
            triggerLose(FailReason.LAVA_BURN)
            return
          }
        }
        HazardKind.SPIKES_UP,
        HazardKind.SPIKES_DOWN,
        HazardKind.SPIKES_LEFT,
        HazardKind.SPIKES_RIGHT -> {
          if (circleHitsRect(dog.pos, dog.radius - 5f, hz.x, hz.y, hz.w, hz.h)) {
            triggerLose(FailReason.HAZARD_SPIKE)
            return
          }
        }
      }
    }

    for (m in movingStates) {
      if (m.kind == MovingKind.SPIKY_BALL || m.kind == MovingKind.SPIKY_BAT) {
        val ballCenter = Vec2(m.x + m.w * 0.5f, m.y + m.h * 0.5f)
        val ballR = m.w * 0.42f
        if (Vec2.dist(dog.pos, ballCenter) < dog.radius + ballR - 4f) {
          triggerLose(FailReason.HAZARD_SPIKE)
          return
        }
      }
    }

    if (dog.pos.y > LevelRepository.WORLD_H + 30f) {
      triggerLose(FailReason.WATER_FALL)
    }
  }

  private fun stepBoulders(dt: Float) {
    val body = drawnBody
    val bodyR = (body?.thickness ?: 13f) * 0.5f
    for (b in boulderStates) {
      b.vel = Vec2(b.vel.x * 0.994f, b.vel.y + 560f * dt)
      b.pos += b.vel * dt
      b.angle += (b.vel.x / b.radius.coerceAtLeast(10f)) * dt

      // Collide boulder vs static platforms & slopes
      for (plat in level.platforms) {
        val cx = b.pos.x.coerceIn(plat.x, plat.x + plat.w)
        val cy = b.pos.y.coerceIn(plat.y, plat.y + plat.h)
        val dx = b.pos.x - cx
        val dy = b.pos.y - cy
        val d2 = dx * dx + dy * dy
        if (d2 < b.radius * b.radius) {
          val d = hypot(dx, dy)
          val n = if (d > 1e-4f) Vec2(dx / d, dy / d) else Vec2(0f, -1f)
          b.pos += n * (b.radius - d)
          val vn = b.vel.dot(n)
          if (vn < 0f) b.vel -= n * (1.1f * vn)
        }
      }
      for (slope in level.slopes) {
        val v1 = Vec2(slope.x1, slope.y1)
        val v2 = Vec2(slope.x2, slope.y2)
        val v3 = Vec2(slope.x3, slope.y3)
        val edges = listOf(v1 to v2, v2 to v3, v3 to v1)
        var bestPt = v1
        var bestD2 = Float.MAX_VALUE
        for ((p1, p2) in edges) {
          val cp = Vec2.closestPointOnSegment(b.pos, p1, p2)
          val d2 = (b.pos - cp).lenSq()
          if (d2 < bestD2) {
            bestD2 = d2
            bestPt = cp
          }
        }
        if (bestD2 < b.radius * b.radius) {
          val d = hypot(b.pos.x - bestPt.x, b.pos.y - bestPt.y)
          val n = if (d > 1e-4f) (b.pos - bestPt) / d else Vec2(0f, -1f)
          b.pos += n * (b.radius - d)
          val vn = b.vel.dot(n)
          if (vn < 0f) b.vel -= n * (1.1f * vn)
        }
      }

      // Collide boulder vs drawn shield
      if (body != null) {
        val pts = body.worldPoints
        val minHit = b.radius + bodyR
        for (i in 0 until pts.size - 1) {
          val cp = Vec2.closestPointOnSegment(b.pos, pts[i], pts[i + 1])
          val diff = b.pos - cp
          val d = diff.len()
          if (d < minHit) {
            val n = if (d > 1e-4f) diff / d else Vec2(0f, -1f)
            b.pos = cp + n * minHit
            val vn = b.vel.dot(n)
            if (vn < 0f) {
              b.vel -= n * (1.25f * vn)
              body.vel += n * (-35f * dt / body.mass)
            }
          }
        }
      }

      // Collide boulder vs dog (pushes dog cleanly!)
      val toDog = dog.pos - b.pos
      val dDog = toDog.len()
      val minDog = b.radius + dog.radius
      if (dDog < minDog) {
        val n = if (dDog > 1e-4f) toDog / dDog else Vec2(0f, -1f)
        val pen = minDog - dDog
        dog.pos += n * (pen * 0.65f)
        b.pos -= n * (pen * 0.35f)
        dog.vel += n * 95f
      }

      // Keep horizontally bounded
      if (b.pos.x < b.radius + 4f) {
        b.pos = Vec2(b.radius + 4f, b.pos.y)
        b.vel = Vec2(abs(b.vel.x) * 0.4f, b.vel.y)
      } else if (b.pos.x > LevelRepository.WORLD_W - b.radius - 4f) {
        b.pos = Vec2(LevelRepository.WORLD_W - b.radius - 4f, b.pos.y)
        b.vel = Vec2(-abs(b.vel.x) * 0.4f, b.vel.y)
      }
    }
  }

  private fun stepBombs(dt: Float, highGraphics: Boolean) {
    val body = drawnBody
    for (bomb in bombStates) {
      if (bomb.exploded) {
        bomb.blastTimer += dt
        continue
      }
      bomb.fuseLeft -= dt
      if (bomb.fuseLeft <= 0f) {
        bomb.exploded = true
        bomb.blastTimer = 0f
        onBeeBounce()
        if (highGraphics) {
          repeat(22) {
            val ang = Random.nextFloat() * 2f * PI.toFloat()
            val spd = 90f + Random.nextFloat() * 220f
            particles.add(
              Particle(
                pos = bomb.pos,
                vel = Vec2(cos(ang) * spd, sin(ang) * spd),
                life = 0f,
                maxLife = 0.65f,
                size = 9f,
                color = if (it % 2 == 0) Color(0xFFFF6D00) else Color(0xFFFFD54F),
              )
            )
          }
        }
        // Knock nearby boulders with explosion wave
        for (boulder in boulderStates) {
          val toBoulder = boulder.pos - bomb.pos
          val bd = toBoulder.len()
          if (bd < 290f && bd > 1f) {
            val strength = ((290f - bd) / 290f) * 260f
            boulder.vel += (toBoulder / bd) * strength + Vec2(0f, -65f)
          }
        }
        // Check blast effect on drawn shield & dog
        val distToDog = Vec2.dist(bomb.pos, dog.pos)
        if (distToDog < 185f) {
          val shielded = body != null && isSegmentBlockedByLine(bomb.pos, dog.pos, body.worldPoints)
          if (!shielded) {
            triggerLose(FailReason.BOMB_BLAST)
            return
          } else {
            // Drawn barrier absorbs blast with small rattle!
            val pushDir = (dog.pos - bomb.pos).normalized()
            body.vel += pushDir * (75f / body.mass)
          }
        }
      }
    }
  }

  private fun isSegmentBlockedByLine(p1: Vec2, p2: Vec2, linePts: List<Vec2>): Boolean {
    for (i in 0 until linePts.size - 1) {
      if (segmentsIntersect(p1, p2, linePts[i], linePts[i + 1])) return true
    }
    return false
  }

  private fun segmentsIntersect(a1: Vec2, a2: Vec2, b1: Vec2, b2: Vec2): Boolean {
    val d1 = (a2 - a1).cross(b1 - a1)
    val d2 = (a2 - a1).cross(b2 - a1)
    val d3 = (b2 - b1).cross(a1 - b1)
    val d4 = (b2 - b1).cross(a2 - b1)
    return ((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) && ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))
  }

  private fun resolveCircleVsRect(
    c: DogBody,
    rx: Float,
    ry: Float,
    rw: Float,
    rh: Float,
    surfVx: Float,
    surfVy: Float,
  ) {
    val cx = c.pos.x.coerceIn(rx, rx + rw)
    val cy = c.pos.y.coerceIn(ry, ry + rh)
    val dx = c.pos.x - cx
    val dy = c.pos.y - cy
    val d2 = dx * dx + dy * dy
    if (d2 < c.radius * c.radius) {
      val d = hypot(dx, dy)
      val n = if (d > 1e-4f) Vec2(dx / d, dy / d) else Vec2(0f, -1f)
      val pen = c.radius - d
      c.pos += n * pen
      val relV = Vec2(c.vel.x - surfVx, c.vel.y - surfVy)
      val vn = relV.dot(n)
      if (vn < 0f) {
        c.vel -= n * (1.15f * vn)
      }
      // Friction on top surface
      if (n.y < -0.6f) {
        c.vel = Vec2(c.vel.x * 0.88f + surfVx * 0.12f, c.vel.y)
      }
    }
  }

  private fun resolveCircleVsTriangle(c: DogBody, t: SlopeTriangle) {
    val v1 = Vec2(t.x1, t.y1)
    val v2 = Vec2(t.x2, t.y2)
    val v3 = Vec2(t.x3, t.y3)
    val edges = listOf(v1 to v2, v2 to v3, v3 to v1)
    var bestPt = v1
    var bestD2 = Float.MAX_VALUE
    for ((a, b) in edges) {
      val cp = Vec2.closestPointOnSegment(c.pos, a, b)
      val d2 = (c.pos - cp).lenSq()
      if (d2 < bestD2) {
        bestD2 = d2
        bestPt = cp
      }
    }
    if (bestD2 < c.radius * c.radius) {
      val d = hypot(c.pos.x - bestPt.x, c.pos.y - bestPt.y)
      val n = if (d > 1e-4f) (c.pos - bestPt) / d else Vec2(0f, -1f)
      c.pos += n * (c.radius - d)
      val vn = c.vel.dot(n)
      if (vn < 0f) {
        c.vel -= n * (1.1f * vn)
      }
    }
  }

  private fun stepBees(dt: Float, highGraphics: Boolean) {
    val maxSpeed = (225f + level.id * 0.8f).coerceAtMost(310f)
    val body = drawnBody
    val bodyR = (body?.thickness ?: 13f) * 0.5f

    for (i in bees.indices) {
      val bee = bees[i]
      bee.wingPhase += 34f * dt

      // Target steering toward dog + slight buzz wobble
      val toDog = (dog.pos - bee.pos)
      val distToDog = toDog.len().coerceAtLeast(1f)
      val dirToDog = toDog / distToDog
      val buzzPerp = dirToDog.perp() * (sin(elapsed * 14f + bee.phase) * 75f)
      bee.vel = bee.vel * (1f - 1.6f * dt).coerceAtLeast(0.8f) + (dirToDog * 720f + buzzPerp) * dt

      // Bee-to-bee separation so swarms fan out & test different angles
      for (j in i + 1 until bees.size) {
        val other = bees[j]
        val diff = bee.pos - other.pos
        val d2 = diff.lenSq()
        val sep = bee.radius * 2.2f
        if (d2 in 0.01f..(sep * sep)) {
          val d = hypot(diff.x, diff.y)
          val push = (diff / d) * ((sep - d) * 14f)
          bee.vel += push * dt
          other.vel -= push * dt
        }
      }

      // Clamp speed
      val spd = bee.vel.len()
      if (spd > maxSpeed) {
        bee.vel = (bee.vel / spd) * maxSpeed
      }
      bee.pos += bee.vel * dt

      // Collide bee vs DrawnLineBody (sub-stepped capsule check so NO bee can tunnel through!)
      if (body != null) {
        val pts = body.worldPoints
        val minHit = bee.radius + bodyR
        for (s in 0 until pts.size - 1) {
          val a = pts[s]
          val b = pts[s + 1]
          val cp = Vec2.closestPointOnSegment(bee.pos, a, b)
          val diff = bee.pos - cp
          val d = diff.len()
          if (d < minHit) {
            val n = if (d > 1e-4f) diff / d else Vec2(0f, -1f)
            bee.pos = cp + n * minHit
            val vn = bee.vel.dot(n)
            if (vn < 0f) {
              // Bounce off + slide tangentially around the drawn wall!
              val tang = n.perp() * bee.tangentDir
              bee.vel = bee.vel - n * (1.65f * vn) + tang * 115f
              // Subtle framerate-independent impact rattle on the drawn barrier
              val arm = cp - body.pos
              val impact = n * (-55f * dt / body.mass)
              body.vel += impact
              body.omega = (body.omega + arm.cross(impact) * 0.08f).coerceIn(-3f, 3f)

              if (abs(vn) > 65f) {
                onBeeBounce()
                if (highGraphics && particles.size < 90) {
                  particles.add(
                    Particle(
                      pos = cp,
                      vel = n * 60f + Vec2((Random.nextFloat() - 0.5f) * 80f, -40f),
                      life = 0f,
                      maxLife = 0.35f,
                      size = 5f,
                      color = Color(0xFFFFD54F),
                    )
                  )
                }
              }
            }
          }
        }
      }

      // Collide bee vs static platforms
      for (plat in level.platforms) {
        collideBeeRect(bee, plat.x, plat.y, plat.w, plat.h)
      }
      for (m in movingStates) {
        collideBeeRect(bee, m.x, m.y, m.w, m.h)
      }
      for (slope in level.slopes) {
        collideBeeTriangle(bee, slope)
      }

      // World boundaries
      if (bee.pos.x < bee.radius + 4f) {
        bee.pos = Vec2(bee.radius + 4f, bee.pos.y)
        bee.vel = Vec2(abs(bee.vel.x), bee.vel.y)
      } else if (bee.pos.x > LevelRepository.WORLD_W - bee.radius - 4f) {
        bee.pos = Vec2(LevelRepository.WORLD_W - bee.radius - 4f, bee.pos.y)
        bee.vel = Vec2(-abs(bee.vel.x), bee.vel.y)
      }
      if (bee.pos.y < bee.radius + 4f) {
        bee.pos = Vec2(bee.pos.x, bee.radius + 4f)
        bee.vel = Vec2(bee.vel.x, abs(bee.vel.y))
      } else if (bee.pos.y > LevelRepository.WORLD_H - bee.radius - 4f) {
        bee.pos = Vec2(bee.pos.x, LevelRepository.WORLD_H - bee.radius - 4f)
        bee.vel = Vec2(bee.vel.x, -abs(bee.vel.y))
      }

      // Check sting on Dog!
      if (Vec2.dist(bee.pos, dog.pos) < dog.radius + bee.radius - 4f) {
        triggerLose(FailReason.BEE_STING)
        return
      }
    }
  }

  private fun collideBeeRect(bee: BeeEntity, rx: Float, ry: Float, rw: Float, rh: Float) {
    val cx = bee.pos.x.coerceIn(rx, rx + rw)
    val cy = bee.pos.y.coerceIn(ry, ry + rh)
    val dx = bee.pos.x - cx
    val dy = bee.pos.y - cy
    val d2 = dx * dx + dy * dy
    if (d2 < bee.radius * bee.radius) {
      val d = hypot(dx, dy)
      val n = if (d > 1e-4f) Vec2(dx / d, dy / d) else Vec2(0f, -1f)
      bee.pos += n * (bee.radius - d)
      val vn = bee.vel.dot(n)
      if (vn < 0f) {
        bee.vel = bee.vel - n * (1.5f * vn) + n.perp() * (bee.tangentDir * 75f)
      }
    }
  }

  private fun collideBeeTriangle(bee: BeeEntity, t: SlopeTriangle) {
    val v1 = Vec2(t.x1, t.y1)
    val v2 = Vec2(t.x2, t.y2)
    val v3 = Vec2(t.x3, t.y3)
    val edges = listOf(v1 to v2, v2 to v3, v3 to v1)
    var bestPt = v1
    var bestD2 = Float.MAX_VALUE
    for ((a, b) in edges) {
      val cp = Vec2.closestPointOnSegment(bee.pos, a, b)
      val d2 = (bee.pos - cp).lenSq()
      if (d2 < bestD2) {
        bestD2 = d2
        bestPt = cp
      }
    }
    if (bestD2 < bee.radius * bee.radius) {
      val d = hypot(bee.pos.x - bestPt.x, bee.pos.y - bestPt.y)
      val n = if (d > 1e-4f) (bee.pos - bestPt) / d else Vec2(0f, -1f)
      bee.pos += n * (bee.radius - d)
      val vn = bee.vel.dot(n)
      if (vn < 0f) {
        bee.vel = bee.vel - n * (1.5f * vn)
      }
    }
  }

  private fun triggerWin(highGraphics: Boolean) {
    phase = GamePhase.WON
    dog.mood = DogMood.HAPPY
    val inkLeftFrac = ((level.inkBudget - inkUsed) / level.inkBudget).coerceIn(0f, 1f)
    earnedStars =
      when {
        inkLeftFrac >= 0.50f -> 3
        inkLeftFrac >= 0.22f -> 2
        else -> 1
      }
    earnedCoins = 10 + earnedStars * 5 + (level.id / 10) * 2
    if (highGraphics) {
      repeat(26) {
        val ang = Random.nextFloat() * 2f * PI.toFloat()
        val spd = 80f + Random.nextFloat() * 190f
        particles.add(
          Particle(
            pos = dog.pos,
            vel = Vec2(cos(ang) * spd, sin(ang) * spd - 60f),
            life = 0f,
            maxLife = 1.2f,
            size = 9f,
            color = listOf(Color(0xFFFFC107), Color(0xFF4CAF50), Color(0xFF29B6F6), Color(0xFFFF5252)).random(),
            isHeart = it % 4 == 0,
          )
        )
      }
    }
    onWin(earnedStars, earnedCoins)
  }

  private fun triggerLose(reason: FailReason) {
    phase = GamePhase.LOST
    failReason = reason
    dog.mood = DogMood.STUNG
    repeat(12) {
      val ang = Random.nextFloat() * 2f * PI.toFloat()
      particles.add(
        Particle(
          pos = dog.pos,
          vel = Vec2(cos(ang) * 120f, sin(ang) * 120f),
          life = 0f,
          maxLife = 0.7f,
          size = 7f,
          color = Color(0xFFE53935),
        )
      )
    }
    onLose(reason)
  }

  private fun updateParticles(dt: Float) {
    val it = particles.iterator()
    while (it.hasNext()) {
      val p = it.next()
      p.life += dt
      if (p.life >= p.maxLife) {
        it.remove()
      } else {
        p.pos += p.vel * dt
        p.vel = Vec2(p.vel.x * 0.96f, p.vel.y + 120f * dt)
      }
    }
  }

  private fun circleHitsRect(c: Vec2, r: Float, rx: Float, ry: Float, rw: Float, rh: Float): Boolean {
    val cx = c.x.coerceIn(rx, rx + rw)
    val cy = c.posCheck(ry, ry + rh)
    val dx = c.x - cx
    val dy = c.y - cy
    return dx * dx + dy * dy < r * r
  }

  private fun Vec2.posCheck(minV: Float, maxV: Float): Float = y.coerceIn(minV, maxV)

  private fun pointInTriangle(p: Vec2, a: Vec2, b: Vec2, c: Vec2): Boolean {
    val d1 = sign(p, a, b)
    val d2 = sign(p, b, c)
    val d3 = sign(p, c, a)
    val hasNeg = (d1 < 0) || (d2 < 0) || (d3 < 0)
    val hasPos = (d1 > 0) || (d2 > 0) || (d3 > 0)
    return !(hasNeg && hasPos)
  }

  private fun sign(p1: Vec2, p2: Vec2, p3: Vec2): Float =
    (p1.x - p3.x) * (p2.y - p3.y) - (p2.x - p3.x) * (p1.y - p3.y)

  fun snapshot(): SimulationSnapshot =
    SimulationSnapshot(
      level = level,
      phase = phase,
      timeLeft = timeLeft,
      inkUsed = inkUsed,
      inkBudget = level.inkBudget,
      dog = dog.copy(),
      bees = bees.map { it.copy() },
      rawDrawPoints = rawPoints.toList(),
      drawnBody = drawnBody?.snapshotCopy(),
      movingObstacles = movingStates.map { it.copy() },
      boulders = boulderStates.map { it.copy() },
      bombs = bombStates.map { it.copy() },
      particles = particles.toList(),
      failReason = failReason,
      earnedStars = earnedStars,
      earnedCoins = earnedCoins,
      elapsed = elapsed,
      hivePulse = hivePulse,
    )
}
