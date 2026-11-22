package com.example.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import com.example.engine.BeeEntity
import com.example.engine.BombState
import com.example.engine.BoulderState
import com.example.engine.SimulationSnapshot
import com.example.levels.LevelRepository
import com.example.model.DogMood
import com.example.model.DogSkin
import com.example.model.GamePhase
import com.example.model.HazardKind
import com.example.model.MovingKind
import com.example.model.PenModel
import com.example.model.PlatformKind
import com.example.model.PortalSpec
import com.example.model.Vec2
import com.example.ui.theme.BrickBrown
import com.example.ui.theme.BrickDark
import com.example.ui.theme.BrickLight
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.CreamPattern
import com.example.ui.theme.GrassDark
import com.example.ui.theme.GrassGreen
import com.example.ui.theme.InkBlack
import com.example.ui.theme.SkyWater
import com.example.ui.theme.SkyWaterHighlight
import com.example.ui.theme.SpikeMetal
import com.example.ui.theme.SpikeShadow
import com.example.ui.theme.WoodCrate
import com.example.ui.theme.WoodCrateDark
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val bombTextPaint =
  android.graphics.Paint().apply {
    color = android.graphics.Color.WHITE
    textAlign = android.graphics.Paint.Align.CENTER
    typeface = android.graphics.Typeface.DEFAULT_BOLD
    isAntiAlias = true
  }

data class WorldViewport(
  val scale: Float,
  val offsetX: Float,
  val offsetY: Float,
) {
  fun screenToWorld(screenPos: Offset): Vec2 {
    return Vec2(
      (screenPos.x - offsetX) / scale,
      (screenPos.y - offsetY) / scale,
    )
  }
}

fun computeViewport(canvasW: Float, canvasH: Float): WorldViewport {
  val scale = min(canvasW / LevelRepository.WORLD_W, canvasH / LevelRepository.WORLD_H)
  val offsetX = (canvasW - LevelRepository.WORLD_W * scale) * 0.5f
  val offsetY = (canvasH - LevelRepository.WORLD_H * scale) * 0.5f
  return WorldViewport(scale, offsetX, offsetY)
}

fun DrawScope.renderGameWorld(
  snap: SimulationSnapshot,
  skin: DogSkin,
  pen: PenModel,
  highGraphics: Boolean,
) {
  // Full cream background across entire canvas
  drawRect(color = CreamBackground)

  val vp = computeViewport(size.width, size.height)

  translate(left = vp.offsetX, top = vp.offsetY) {
    scale(scaleX = vp.scale, scaleY = vp.scale, pivot = Offset.Zero) {
      // Faint background bricks pattern matching cartoon puzzle style
      drawBackgroundPattern()

      // Water & Lava pools behind platforms
      snap.level.hazards.forEach { hz ->
        when (hz.kind) {
          HazardKind.WATER -> drawWaterZone(hz.x, hz.y, hz.w, hz.h, snap.elapsed, highGraphics)
          HazardKind.LAVA -> drawLavaZone(hz.x, hz.y, hz.w, hz.h, snap.elapsed, highGraphics)
          else -> {}
        }
      }

      // Slopes
      snap.level.slopes.forEach { s ->
        drawSlopeTriangle(s.x1, s.y1, s.x2, s.y2, s.x3, s.y3)
      }

      // Static platforms, crates, walls, peppermint pedestals
      snap.level.platforms.forEach { p ->
        when (p.kind) {
          PlatformKind.GRASS_BRICK -> drawGrassBrickPlatform(p.x, p.y, p.w, p.h, snap.elapsed, highGraphics)
          PlatformKind.WOOD_PLANK -> drawWoodPlank(p.x, p.y, p.w, p.h)
          PlatformKind.WOOD_CRATE -> drawWoodCrate(p.x, p.y, p.w, p.h)
          PlatformKind.STONE_WALL -> drawStoneWall(p.x, p.y, p.w, p.h)
          PlatformKind.PEPPERMINT -> drawPeppermintPedestal(p.x, p.y, p.w, p.h)
        }
      }

      // Overhead Portals / Tunnel Rings
      snap.level.portals.forEach { portal ->
        drawPortal(portal, snap.elapsed)
      }

      // Moving obstacles (moving planks, spiky star balls, flying spiky bat monsters)
      snap.movingObstacles.forEach { m ->
        when (m.kind) {
          MovingKind.PLATFORM -> drawWoodPlank(m.x, m.y, m.w, m.h, isMoving = true)
          MovingKind.SPIKY_BALL -> drawSpikyBall(m.x + m.w * 0.5f, m.y + m.h * 0.5f, m.w * 0.45f, m.spinAngle)
          MovingKind.SPIKY_BAT -> drawSpikyBat(m.x + m.w * 0.5f, m.y + m.h * 0.5f, m.w * 0.48f, snap.elapsed)
        }
      }

      // Spikes hazards
      snap.level.hazards.filter { it.kind != HazardKind.WATER && it.kind != HazardKind.LAVA }.forEach { hz ->
        drawSpikesHazard(hz.x, hz.y, hz.w, hz.h, hz.kind)
      }

      // Rolling Stone Boulders & Timed Bombs
      snap.boulders.forEach { boulder ->
        drawBoulder(boulder)
      }
      snap.bombs.forEach { bomb ->
        drawBomb(bomb, snap.elapsed)
      }

      // Level 1 dotted guide arc when waiting for user draw
      if (snap.phase == GamePhase.DRAWING && snap.rawDrawPoints.isEmpty() && snap.level.guideArc.size >= 2) {
        val guidePath = Path().apply {
          moveTo(snap.level.guideArc.first().x, snap.level.guideArc.first().y)
          for (i in 1 until snap.level.guideArc.size) {
            lineTo(snap.level.guideArc[i].x, snap.level.guideArc[i].y)
          }
        }
        val pulseAlpha = 0.38f + 0.25f * sin(snap.elapsed * 4f)
        drawPath(
          path = guidePath,
          color = Color(0xFFFF8F00).copy(alpha = pulseAlpha),
          style =
            Stroke(
              width = 8f,
              cap = StrokeCap.Round,
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 14f), snap.elapsed * 30f),
            ),
        )
      }

      // Hives
      snap.level.hives.forEach { hive ->
        drawBeeHive(hive.x, hive.y, hive.radius, snap.hivePulse, snap.elapsed)
      }

      // Cute Cartoon Dog
      val breath = if (snap.phase == GamePhase.WON) {
        abs(sin(snap.elapsed * 8f)) * -10f
      } else {
        sin(snap.elapsed * 3.2f) * 1.6f
      }
      drawCartoonDog(
        center = Offset(snap.dog.pos.x, snap.dog.pos.y + breath),
        radius = snap.dog.radius,
        skin = skin,
        mood = snap.dog.mood,
        isBlinking = snap.dog.isBlinking,
        elapsed = snap.elapsed,
      )

      // Drawn Protective Ink Line (either raw during draw or physics body during simulation)
      val drawPts = snap.drawnBody?.worldPoints ?: snap.rawDrawPoints
      if (drawPts.size >= 2) {
        val inkPath = Path().apply {
          moveTo(drawPts.first().x, drawPts.first().y)
          for (i in 1 until drawPts.size) {
            lineTo(drawPts[i].x, drawPts[i].y)
          }
        }
        // Soft shadow under drawn line
        if (highGraphics) {
          drawPath(
            path = inkPath,
            color = Color.Black.copy(alpha = 0.14f),
            style = Stroke(width = 16f, cap = StrokeCap.Round, join = StrokeJoin.Round),
          )
        }
        drawPath(
          path = inkPath,
          color = pen.inkColor,
          style = Stroke(width = 13f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawPath(
          path = inkPath,
          color = pen.accentColor.copy(alpha = 0.42f),
          style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
      }

      // Show animated Cartoon Pen right at drawing nib whenever player touches/draws
      if (snap.phase == GamePhase.DRAWING && drawPts.isNotEmpty()) {
        val tip = drawPts.last()
        drawCircle(pen.inkColor.copy(alpha = 0.28f), radius = 16f, center = Offset(tip.x, tip.y))
        drawCircle(pen.inkColor, radius = 6.5f, center = Offset(tip.x, tip.y))
        drawCartoonPen(
          tip = Offset(tip.x, tip.y),
          pen = pen,
          elapsed = snap.elapsed,
          length = 96f,
        )
      }

      // Angry Cartoon Bees
      snap.bees.forEach { bee ->
        drawCartoonBee(bee, highGraphics)
      }

      // Particles (confetti, sparks, hearts)
      snap.particles.forEach { p ->
        val alpha = (1f - p.life / p.maxLife).coerceIn(0f, 1f)
        if (p.isHeart) {
          drawHeartParticle(Offset(p.pos.x, p.pos.y), p.size, p.color.copy(alpha = alpha))
        } else {
          drawCircle(
            color = p.color.copy(alpha = alpha),
            radius = p.size * (0.6f + 0.4f * alpha),
            center = Offset(p.pos.x, p.pos.y),
          )
        }
      }
    }
  }
}

private fun DrawScope.drawBackgroundPattern() {
  val spots =
    listOf(
      Offset(55f, 80f),
      Offset(220f, 65f),
      Offset(440f, 95f),
      Offset(120f, 230f),
      Offset(360f, 210f),
      Offset(510f, 270f),
      Offset(70f, 390f),
      Offset(470f, 410f),
    )
  spots.forEach { pt ->
    drawRoundRect(
      color = CreamPattern,
      topLeft = pt,
      size = Size(58f, 22f),
      cornerRadius = CornerRadius(6f, 6f),
    )
    drawRoundRect(
      color = CreamPattern,
      topLeft = Offset(pt.x + 28f, pt.y + 26f),
      size = Size(44f, 18f),
      cornerRadius = CornerRadius(5f, 5f),
    )
  }
}

private fun DrawScope.drawGrassBrickPlatform(
  x: Float,
  y: Float,
  w: Float,
  h: Float,
  elapsed: Float,
  highGraphics: Boolean,
) {
  // Brick base
  drawRoundRect(
    color = BrickBrown,
    topLeft = Offset(x, y),
    size = Size(w, h),
    cornerRadius = CornerRadius(6f, 6f),
  )

  // Brick grid mortar lines — crisp square tiles matching reference style
  val tileSize = 32f
  var curY = y + 18f
  while (curY < y + h - 2f) {
    drawLine(
      color = BrickDark.copy(alpha = 0.50f),
      start = Offset(x + 2f, curY),
      end = Offset(x + w - 2f, curY),
      strokeWidth = 2.4f,
    )
    var curX = x + tileSize
    while (curX < x + w - 4f) {
      val nextY = min(curY + tileSize, y + h)
      drawLine(
        color = BrickDark.copy(alpha = 0.50f),
        start = Offset(curX, curY),
        end = Offset(curX, nextY),
        strokeWidth = 2.4f,
      )
      // Subtle top-left bevel inside each square tile
      drawLine(
        color = BrickLight.copy(alpha = 0.32f),
        start = Offset(curX - tileSize + 4f, curY + 3f),
        end = Offset(curX - 4f, curY + 3f),
        strokeWidth = 2f,
      )
      curX += tileSize
    }
    curY += tileSize
  }

  // Highlight border
  drawRoundRect(
    color = BrickLight.copy(alpha = 0.48f),
    topLeft = Offset(x + 2f, y + 2f),
    size = Size((w - 4f).coerceAtLeast(1f), (h - 4f).coerceAtLeast(1f)),
    cornerRadius = CornerRadius(5f, 5f),
    style = Stroke(width = 3f),
  )

  // Lush green grass cap
  val grassH = min(18f, h * 0.35f)
  drawRoundRect(
    color = GrassGreen,
    topLeft = Offset(x - 1f, y - 2f),
    size = Size(w + 2f, grassH),
    cornerRadius = CornerRadius(7f, 7f),
  )
  drawLine(
    color = GrassDark,
    start = Offset(x, y + grassH - 2f),
    end = Offset(x + w, y + grassH - 2f),
    strokeWidth = 3f,
  )

  // Cute swaying grass blades
  if (highGraphics && w >= 60f) {
    var gx = x + 18f
    while (gx < x + w - 18f) {
      val sway = sin(elapsed * 2.8f + gx * 0.05f) * 3f
      val bladePath = Path().apply {
        moveTo(gx - 3f, y)
        lineTo(gx + sway, y - 9f)
        lineTo(gx + 3f, y)
        close()
      }
      drawPath(bladePath, GrassGreen)
      gx += 46f
    }
  }
}

private fun DrawScope.drawSlopeTriangle(
  x1: Float,
  y1: Float,
  x2: Float,
  y2: Float,
  x3: Float,
  y3: Float,
) {
  val path = Path().apply {
    moveTo(x1, y1)
    lineTo(x2, y2)
    lineTo(x3, y3)
    close()
  }
  drawPath(path, BrickBrown)
  drawPath(path, BrickDark.copy(alpha = 0.5f), style = Stroke(width = 3f))
  // Highlight grass on top edge (between lowest-y vertex and mid-y vertex)
  val pts = listOf(Offset(x1, y1), Offset(x2, y2), Offset(x3, y3)).sortedBy { it.y }
  drawLine(
    color = GrassGreen,
    start = pts[0],
    end = pts[1],
    strokeWidth = 11f,
    cap = StrokeCap.Round,
  )
}

private fun DrawScope.drawWoodPlank(x: Float, y: Float, w: Float, h: Float, isMoving: Boolean = false) {
  drawRoundRect(
    color = if (isMoving) Color(0xFFCC8442) else WoodCrate,
    topLeft = Offset(x, y),
    size = Size(w, h),
    cornerRadius = CornerRadius(8f, 8f),
  )
  drawRoundRect(
    color = WoodCrateDark,
    topLeft = Offset(x, y),
    size = Size(w, h),
    cornerRadius = CornerRadius(8f, 8f),
    style = Stroke(width = 3.5f),
  )
  // Nails at corners
  val nailColor = Color(0xFF5D4037)
  drawCircle(nailColor, 3f, Offset(x + 9f, y + h * 0.5f))
  drawCircle(nailColor, 3f, Offset(x + w - 9f, y + h * 0.5f))
}

private fun DrawScope.drawWoodCrate(x: Float, y: Float, w: Float, h: Float) {
  drawRoundRect(
    color = WoodCrate,
    topLeft = Offset(x, y),
    size = Size(w, h),
    cornerRadius = CornerRadius(6f, 6f),
  )
  drawRoundRect(
    color = WoodCrateDark,
    topLeft = Offset(x, y),
    size = Size(w, h),
    cornerRadius = CornerRadius(6f, 6f),
    style = Stroke(width = 4f),
  )
  // Diagonal brace
  drawLine(
    color = WoodCrateDark.copy(alpha = 0.7f),
    start = Offset(x + 6f, y + 6f),
    end = Offset(x + w - 6f, y + h - 6f),
    strokeWidth = 3.5f,
  )
  drawLine(
    color = WoodCrateDark.copy(alpha = 0.7f),
    start = Offset(x + w - 6f, y + 6f),
    end = Offset(x + 6f, y + h - 6f),
    strokeWidth = 3.5f,
  )
}

private fun DrawScope.drawStoneWall(x: Float, y: Float, w: Float, h: Float) {
  drawRoundRect(
    color = Color(0xFF8C9BAE),
    topLeft = Offset(x, y),
    size = Size(w, h),
    cornerRadius = CornerRadius(7f, 7f),
  )
  drawRoundRect(
    color = Color(0xFF56657A),
    topLeft = Offset(x, y),
    size = Size(w, h),
    cornerRadius = CornerRadius(7f, 7f),
    style = Stroke(width = 3.5f),
  )
}

private fun DrawScope.drawWaterZone(
  x: Float,
  y: Float,
  w: Float,
  h: Float,
  elapsed: Float,
  highGraphics: Boolean,
) {
  val wavePath = Path().apply {
    moveTo(x, y + h)
    lineTo(x, y + 6f)
    val step = 20f
    var wx = x
    while (wx <= x + w) {
      val wy = y + sin(elapsed * 4.2f + wx * 0.06f) * 5.5f
      lineTo(wx, wy)
      wx += step
    }
    lineTo(x + w, y + 6f)
    lineTo(x + w, y + h)
    close()
  }
  drawPath(
    path = wavePath,
    brush = Brush.verticalGradient(listOf(SkyWater, Color(0xFF28A4DC)), startY = y, endY = y + h),
  )
  if (highGraphics) {
    // White-cyan wave crest highlight
    val crest = Path().apply {
      var wx = x
      moveTo(x, y + sin(elapsed * 4.2f + x * 0.06f) * 5.5f)
      while (wx <= x + w) {
        lineTo(wx, y + sin(elapsed * 4.2f + wx * 0.06f) * 5.5f)
        wx += 16f
      }
    }
    drawPath(crest, SkyWaterHighlight, style = Stroke(width = 4f, cap = StrokeCap.Round))
  }
}

private fun DrawScope.drawSpikesHazard(x: Float, y: Float, w: Float, h: Float, kind: HazardKind) {
  val toothCount =
    when (kind) {
      HazardKind.SPIKES_UP,
      HazardKind.SPIKES_DOWN -> (w / 22f).toInt().coerceAtLeast(2)
      HazardKind.SPIKES_LEFT,
      HazardKind.SPIKES_RIGHT -> (h / 22f).toInt().coerceAtLeast(2)
      else -> 0
    }
  if (toothCount == 0) return

  for (i in 0 until toothCount) {
    val path = Path()
    when (kind) {
      HazardKind.SPIKES_UP -> {
        val tw = w / toothCount
        val sx = x + i * tw
        path.moveTo(sx, y + h)
        path.lineTo(sx + tw * 0.5f, y)
        path.lineTo(sx + tw, y + h)
      }
      HazardKind.SPIKES_DOWN -> {
        val tw = w / toothCount
        val sx = x + i * tw
        path.moveTo(sx, y)
        path.lineTo(sx + tw * 0.5f, y + h)
        path.lineTo(sx + tw, y)
      }
      HazardKind.SPIKES_RIGHT -> {
        val th = h / toothCount
        val sy = y + i * th
        path.moveTo(x, sy)
        path.lineTo(x + w, sy + th * 0.5f)
        path.lineTo(x, sy + th)
      }
      HazardKind.SPIKES_LEFT -> {
        val th = h / toothCount
        val sy = y + i * th
        path.moveTo(x + w, sy)
        path.lineTo(x, sy + th * 0.5f)
        path.lineTo(x + w, sy + th)
      }
      else -> {}
    }
    path.close()
    drawPath(path, SpikeMetal)
    drawPath(path, SpikeShadow, style = Stroke(width = 2.5f, join = StrokeJoin.Round))
  }
}

private fun DrawScope.drawSpikyBall(cx: Float, cy: Float, radius: Float, spinAngle: Float) {
  rotate(degrees = spinAngle * (180f / PI.toFloat()), pivot = Offset(cx, cy)) {
    val spikes = 10
    val outerR = radius * 1.28f
    val innerR = radius * 0.78f
    val path = Path()
    for (i in 0 until spikes * 2) {
      val r = if (i % 2 == 0) outerR else innerR
      val ang = (i * PI / spikes).toFloat()
      val px = cx + cos(ang) * r
      val py = cy + sin(ang) * r
      if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    drawPath(path, SpikeMetal)
    drawPath(path, SpikeShadow, style = Stroke(width = 3f, join = StrokeJoin.Round))
    drawCircle(color = Color(0xFFB5C7DE), radius = radius * 0.55f, center = Offset(cx, cy))
    drawCircle(color = SpikeShadow, radius = radius * 0.25f, center = Offset(cx - 4f, cy - 4f))
  }
}

private fun DrawScope.drawLavaZone(
  x: Float,
  y: Float,
  w: Float,
  h: Float,
  elapsed: Float,
  highGraphics: Boolean,
) {
  // Glowing molten orange-red base
  drawRect(
    brush = Brush.verticalGradient(listOf(Color(0xFFFF7029), Color(0xFFE63E11)), startY = y, endY = y + h),
    topLeft = Offset(x, y),
    size = Size(w, h),
  )
  // Animated molten yellow-gold magma pockets (Voronoi-cell vibe from screenshots)
  val cols = (w / 46f).toInt().coerceAtLeast(2)
  val rows = (h / 42f).toInt().coerceAtLeast(2)
  val cellW = w / cols
  val cellH = h / rows
  for (r in 0 until rows) {
    for (c in 0 until cols) {
      val cx = x + (c + 0.5f) * cellW + sin(elapsed * 1.4f + r * 2.1f + c) * 4f
      val cy = y + (r + 0.55f) * cellH + cos(elapsed * 1.2f + c * 1.7f + r) * 3f
      val rx = cellW * (0.36f + 0.05f * sin(elapsed * 2f + c + r))
      val ry = cellH * (0.34f + 0.05f * cos(elapsed * 2.3f + c - r))
      drawOval(
        color = Color(0xFFFFD54F).copy(alpha = 0.86f),
        topLeft = Offset(cx - rx, cy - ry),
        size = Size(rx * 2f, ry * 2f),
      )
      drawOval(
        color = Color(0xFFFFA000).copy(alpha = 0.7f),
        topLeft = Offset(cx - rx * 0.55f, cy - ry * 0.55f),
        size = Size(rx * 1.1f, ry * 1.1f),
      )
    }
  }
  // Glowing undulating molten surface rim
  if (highGraphics) {
    val rim = Path().apply {
      moveTo(x, y + sin(elapsed * 3.5f + x * 0.05f) * 3f)
      var lx = x
      while (lx <= x + w) {
        lineTo(lx, y + sin(elapsed * 3.5f + lx * 0.05f) * 3.5f)
        lx += 16f
      }
    }
    drawPath(rim, Color(0xFFFFE57F), style = Stroke(width = 5f, cap = StrokeCap.Round))
  }
}

private fun DrawScope.drawPeppermintPedestal(x: Float, y: Float, w: Float, h: Float) {
  // Cylindrical striped pink-and-cream pedestal side
  drawRoundRect(
    color = Color(0xFFF6D5D1),
    topLeft = Offset(x, y + h * 0.25f),
    size = Size(w, h * 0.75f),
    cornerRadius = CornerRadius(14f, 14f),
  )
  val stripeCount = 9
  val sw = w / stripeCount
  for (i in 0 until stripeCount) {
    if (i % 2 == 0) {
      drawLine(
        color = Color(0xFFD88488).copy(alpha = 0.72f),
        start = Offset(x + (i + 0.5f) * sw, y + h * 0.36f),
        end = Offset(x + (i + 0.5f) * sw, y + h * 0.94f),
        strokeWidth = 3f,
        cap = StrokeCap.Round,
      )
    }
  }
  drawRoundRect(
    color = Color(0xFFBC6A6E),
    topLeft = Offset(x, y + h * 0.25f),
    size = Size(w, h * 0.75f),
    cornerRadius = CornerRadius(14f, 14f),
    style = Stroke(width = 2.5f),
  )
  // Oval candy top disc with red/pink peppermint swirls
  val topH = h * 0.52f
  drawOval(
    color = Color(0xFFFFF5F5),
    topLeft = Offset(x - 2f, y - 2f),
    size = Size(w + 4f, topH),
  )
  val cx = x + w * 0.5f
  val cy = y + topH * 0.5f - 1f
  for (i in 0 until 6) {
    val ang = i * (PI.toFloat() / 3f)
    val wedge = Path().apply {
      moveTo(cx, cy)
      val xA = cx + cos(ang) * (w * 0.44f)
      val yA = cy + sin(ang) * (topH * 0.42f)
      val xCtrl = cx + cos(ang + 0.28f) * (w * 0.30f)
      val yCtrl = cy + sin(ang + 0.28f) * (topH * 0.30f)
      val xB = cx + cos(ang + 0.42f) * (w * 0.44f)
      val yB = cy + sin(ang + 0.42f) * (topH * 0.42f)
      quadraticTo(xCtrl, yCtrl, xA, yA)
      lineTo(xB, yB)
      close()
    }
    drawPath(wedge, Color(0xFFE55365))
  }
  drawOval(
    color = Color(0xFFC46D72),
    topLeft = Offset(x - 2f, y - 2f),
    size = Size(w + 4f, topH),
    style = Stroke(width = 2.5f),
  )
}

private fun DrawScope.drawPortal(portal: PortalSpec, elapsed: Float) {
  val center = Offset(portal.x, portal.y)
  val r = portal.radius
  val pulse = 1f + sin(elapsed * 3.2f) * 0.04f
  // Outer tan ring rim (matching Level 46 portal aesthetic)
  drawCircle(
    brush = Brush.radialGradient(listOf(Color(0xFFE8D1B5), Color(0xFF9C7A54)), center = center, radius = r * 1.15f * pulse),
    radius = r * pulse,
    center = center,
  )
  // Deep void center
  drawCircle(color = Color(0xFF2B180C), radius = r * 0.72f, center = center)
  drawCircle(
    color = Color(0xFF5D4037),
    radius = r * pulse,
    center = center,
    style = Stroke(width = 3f),
  )
}

private fun DrawScope.drawSpikyBat(cx: Float, cy: Float, r: Float, elapsed: Float) {
  val flap = sin(elapsed * 16f) * (r * 0.26f)
  translate(cx, cy) {
    // Dark crimson-black bat wings on left & right
    listOf(-1f, 1f).forEach { dir ->
      val wing = Path().apply {
        moveTo(dir * r * 0.45f, -r * 0.25f)
        lineTo(dir * r * 1.15f, -r * 0.95f - flap)
        lineTo(dir * r * 0.95f, -r * 0.1f - flap * 0.4f)
        lineTo(dir * r * 0.72f, r * 0.15f)
        close()
      }
      drawPath(wing, Color(0xFFA93226))
      drawPath(wing, InkBlack, style = Stroke(width = 2.5f, join = StrokeJoin.Round))
    }

    // Spiked collar around bottom rim
    for (i in -3..3) {
      val ang = (PI * 0.5 + i * 0.34).toFloat()
      val sx = cos(ang) * (r * 0.85f)
      val sy = sin(ang) * (r * 0.82f)
      val tx = cos(ang) * (r * 1.24f)
      val ty = sin(ang) * (r * 1.20f)
      val spike = Path().apply {
        moveTo(sx - 6f, sy)
        lineTo(tx, ty)
        lineTo(sx + 6f, sy)
        close()
      }
      drawPath(spike, Color(0xFFE0E6ED))
      drawPath(spike, InkBlack, style = Stroke(width = 2f))
    }

    // Grey monster head
    drawCircle(Color(0xFF7B8794), r * 0.88f, Offset.Zero)
    drawCircle(InkBlack, r * 0.88f, Offset.Zero, style = Stroke(width = 3f))

    // Crimson spiked collar band
    drawArc(
      color = Color(0xFFC62828),
      startAngle = 20f,
      sweepAngle = 140f,
      useCenter = false,
      topLeft = Offset(-r * 0.82f, -r * 0.72f),
      size = Size(r * 1.64f, r * 1.56f),
      style = Stroke(width = 6f, cap = StrokeCap.Round),
    )

    // Angry eyes & furrowed brow
    listOf(-1f, 1f).forEach { dir ->
      val ex = dir * r * 0.34f
      val ey = -r * 0.14f
      drawCircle(Color.White, r * 0.21f, Offset(ex, ey))
      drawCircle(InkBlack, r * 0.21f, Offset(ex, ey), style = Stroke(width = 2.2f))
      drawCircle(Color(0xFFD32F2F), r * 0.08f, Offset(ex - dir * 2f, ey))
      drawLine(InkBlack, Offset(ex - dir * r * 0.22f, ey - r * 0.24f), Offset(ex + dir * r * 0.16f, ey - r * 0.06f), strokeWidth = 3.2f, cap = StrokeCap.Round)
    }

    // Fanged mouth
    val mouth = Path().apply {
      moveTo(-r * 0.42f, r * 0.36f)
      quadraticTo(0f, r * 0.16f, r * 0.42f, r * 0.36f)
      lineTo(r * 0.32f, r * 0.56f)
      lineTo(-r * 0.32f, r * 0.56f)
      close()
    }
    drawPath(mouth, Color(0xFF2B180C))
    // Two crisp white fangs
    listOf(-r * 0.24f, r * 0.16f).forEach { fx ->
      val fang = Path().apply {
        moveTo(fx, r * 0.30f)
        lineTo(fx + 5f, r * 0.48f)
        lineTo(fx + 10f, r * 0.30f)
        close()
      }
      drawPath(fang, Color.White)
    }
  }
}

private fun DrawScope.drawBoulder(boulder: BoulderState) {
  val cx = boulder.pos.x
  val cy = boulder.pos.y
  val r = boulder.radius
  rotate(degrees = boulder.angle * (180f / PI.toFloat()), pivot = Offset(cx, cy)) {
    val pts = 9
    val rockPath = Path()
    val radii = floatArrayOf(0.96f, 0.88f, 1.0f, 0.90f, 0.98f, 0.87f, 0.97f, 0.92f, 0.94f)
    for (i in 0 until pts) {
      val ang = (i * 2.0 * PI / pts).toFloat()
      val px = cx + cos(ang) * r * radii[i]
      val py = cy + sin(ang) * r * radii[i]
      if (i == 0) rockPath.moveTo(px, py) else rockPath.lineTo(px, py)
    }
    rockPath.close()
    drawPath(rockPath, Color(0xFF8C96A4))
    drawPath(rockPath, Color(0xFF4F5864), style = Stroke(width = 3f, join = StrokeJoin.Round))

    // Facet highlights & shadow cracks
    drawCircle(Color(0xFFB5BFC9).copy(alpha = 0.6f), r * 0.35f, Offset(cx - r * 0.24f, cy - r * 0.24f))
    drawLine(Color(0xFF5D6673), Offset(cx - r * 0.3f, cy + r * 0.1f), Offset(cx + r * 0.25f, cy - r * 0.15f), strokeWidth = 2.6f, cap = StrokeCap.Round)
    drawLine(Color(0xFF5D6673), Offset(cx + r * 0.05f, cy - r * 0.05f), Offset(cx + r * 0.28f, cy + r * 0.35f), strokeWidth = 2.4f, cap = StrokeCap.Round)
  }
}

private fun DrawScope.drawBomb(bomb: BombState, elapsed: Float) {
  val cx = bomb.pos.x
  val cy = bomb.pos.y
  val r = bomb.radius

  if (bomb.exploded) {
    if (bomb.blastTimer < 0.5f) {
      val t = bomb.blastTimer / 0.5f
      val blastR = r + t * 145f
      val alpha = (1f - t).coerceIn(0f, 1f)
      drawCircle(Color(0xFFFF6D00).copy(alpha = alpha * 0.45f), blastR, Offset(cx, cy))
      drawCircle(Color(0xFFFFD54F).copy(alpha = alpha * 0.7f), blastR * 0.65f, Offset(cx, cy))
    }
    return
  }

  // Soft pink danger box aura around bomb (just like Level 63 screenshot!)
  val auraPulse = 0.22f + 0.12f * abs(sin(elapsed * 5f))
  drawRoundRect(
    color = Color(0xFFF48FB1).copy(alpha = auraPulse),
    topLeft = Offset(cx - r * 1.4f, cy - r * 1.4f),
    size = Size(r * 2.8f, r * 2.4f),
    cornerRadius = CornerRadius(8f, 8f),
  )

  // Fuse cap & burning wick
  drawRoundRect(
    color = Color(0xFF455A64),
    topLeft = Offset(cx - r * 0.26f, cy - r * 1.15f),
    size = Size(r * 0.52f, r * 0.28f),
    cornerRadius = CornerRadius(4f, 4f),
  )
  val wick = Path().apply {
    moveTo(cx, cy - r * 1.15f)
    quadraticTo(cx + r * 0.35f, cy - r * 1.55f, cx + r * 0.62f, cy - r * 1.38f)
  }
  drawPath(wick, Color(0xFF5D4037), style = Stroke(width = 3.5f, cap = StrokeCap.Round))
  // Flickering fuse spark
  val sparkR = 6f + sin(elapsed * 26f) * 2.5f
  drawCircle(Color(0xFFFF6D00), sparkR, Offset(cx + r * 0.62f, cy - r * 1.38f))
  drawCircle(Color(0xFFFFF59D), sparkR * 0.55f, Offset(cx + r * 0.62f, cy - r * 1.38f))

  // Glossy navy-blue bomb sphere
  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(Color(0xFF3949AB), Color(0xFF1A237E)),
      center = Offset(cx - r * 0.28f, cy - r * 0.28f),
      radius = r * 1.3f,
    ),
    radius = r,
    center = Offset(cx, cy),
  )
  drawCircle(InkBlack, r, Offset(cx, cy), style = Stroke(width = 2.8f))
  // Specular shine arc
  drawCircle(Color.White.copy(alpha = 0.28f), r * 0.22f, Offset(cx - r * 0.38f, cy - r * 0.38f))

  // Bold white fuse countdown number inside the bomb
  val secText = "${ceil(bomb.fuseLeft).toInt().coerceAtLeast(1)}"
  bombTextPaint.textSize = r * 1.05f
  drawContext.canvas.nativeCanvas.drawText(secText, cx, cy + r * 0.36f, bombTextPaint)
}

private fun DrawScope.drawBeeHive(x: Float, y: Float, r: Float, pulse: Float, elapsed: Float) {
  val wobble = sin(elapsed * 3.5f) * 2.5f
  val scaleF = 1f + pulse * 0.12f
  rotate(degrees = wobble, pivot = Offset(x, y - r)) {
    scale(scaleF, scaleF, pivot = Offset(x, y)) {
      // Stacked honey rings
      val rings = listOf(0.68f to -0.58f, 0.92f to -0.22f, 1.0f to 0.18f, 0.74f to 0.54f)
      rings.forEachIndexed { idx, (rw, ryOff) ->
        val rx = r * rw
        val ry = r * 0.34f
        drawOval(
          color = if (idx % 2 == 0) Color(0xFFFFC125) else Color(0xFFF39C12),
          topLeft = Offset(x - rx, y + r * ryOff - ry),
          size = Size(rx * 2f, ry * 2f),
        )
        drawOval(
          color = Color(0xFFB76E00),
          topLeft = Offset(x - rx, y + r * ryOff - ry),
          size = Size(rx * 2f, ry * 2f),
          style = Stroke(width = 2.5f),
        )
      }
      // Dark entrance hole
      drawCircle(color = Color(0xFF3E2723), radius = r * 0.26f, center = Offset(x + r * 0.12f, y + r * 0.16f))
      // Highlight dot
      drawCircle(
        color = Color(0xFFFFF3B0).copy(alpha = 0.75f),
        radius = r * 0.14f,
        center = Offset(x - r * 0.34f, y - r * 0.22f),
      )
    }
  }
}

fun DrawScope.drawCartoonDog(
  center: Offset,
  radius: Float,
  skin: DogSkin,
  mood: DogMood,
  isBlinking: Boolean,
  elapsed: Float,
) {
  // Ears (left & right triangles)
  val earWiggle = sin(elapsed * 4.5f) * (if (mood == DogMood.HAPPY) 4f else 1.2f)
  val leftEar = Path().apply {
    moveTo(center.x - radius * 0.76f, center.y - radius * 0.28f)
    lineTo(center.x - radius * 0.88f, center.y - radius * 1.08f + earWiggle)
    lineTo(center.x - radius * 0.22f, center.y - radius * 0.78f)
    close()
  }
  val rightEar = Path().apply {
    moveTo(center.x + radius * 0.76f, center.y - radius * 0.28f)
    lineTo(center.x + radius * 0.88f, center.y - radius * 1.08f - earWiggle)
    lineTo(center.x + radius * 0.22f, center.y - radius * 0.78f)
    close()
  }
  drawPath(leftEar, skin.earColor)
  drawPath(leftEar, InkBlack, style = Stroke(width = 3f, join = StrokeJoin.Round))
  drawPath(rightEar, skin.earColor)
  drawPath(rightEar, InkBlack, style = Stroke(width = 3f, join = StrokeJoin.Round))

  // Head circle
  drawOval(
    color = skin.furColor,
    topLeft = Offset(center.x - radius * 1.04f, center.y - radius * 0.92f),
    size = Size(radius * 2.08f, radius * 1.84f),
  )
  drawOval(
    color = InkBlack,
    topLeft = Offset(center.x - radius * 1.04f, center.y - radius * 0.92f),
    size = Size(radius * 2.08f, radius * 1.84f),
    style = Stroke(width = 3.2f),
  )

  // Cream muzzle / cheeks
  drawOval(
    color = skin.muzzleColor,
    topLeft = Offset(center.x - radius * 0.78f, center.y - radius * 0.14f),
    size = Size(radius * 1.56f, radius * 0.98f),
  )

  // Iconic Shiba cream oval eyebrow spots above eyes
  listOf(-1f, 1f).forEach { dir ->
    drawOval(
      color = skin.muzzleColor,
      topLeft = Offset(center.x + dir * radius * 0.36f - radius * 0.14f, center.y - radius * 0.56f),
      size = Size(radius * 0.28f, radius * 0.15f),
    )
  }

  // Rosy cheek blushes (puffed up when stung!)
  val cheekScale = if (mood == DogMood.STUNG) 1.45f else 1f
  drawCircle(Color(0xFFFF8A80).copy(alpha = 0.6f), radius * 0.16f * cheekScale, Offset(center.x - radius * 0.58f, center.y + radius * 0.18f))
  drawCircle(Color(0xFFFF8A80).copy(alpha = 0.6f), radius * 0.16f * cheekScale, Offset(center.x + radius * 0.58f, center.y + radius * 0.18f))

  // Eyes by mood
  val eyeY = center.y - radius * 0.22f
  val eyeOffsetX = radius * 0.38f
  when (mood) {
    DogMood.STUNG -> {
      // Dizzy X eyes
      listOf(-eyeOffsetX, eyeOffsetX).forEach { ox ->
        val ex = center.x + ox
        val s = radius * 0.14f
        drawLine(InkBlack, Offset(ex - s, eyeY - s), Offset(ex + s, eyeY + s), strokeWidth = 3.5f, cap = StrokeCap.Round)
        drawLine(InkBlack, Offset(ex + s, eyeY - s), Offset(ex - s, eyeY + s), strokeWidth = 3.5f, cap = StrokeCap.Round)
      }
      // Red cartoon bump
      drawCircle(Color(0xFFFF5252), radius * 0.22f, Offset(center.x + radius * 0.25f, center.y - radius * 0.85f))
      drawCircle(InkBlack, radius * 0.22f, Offset(center.x + radius * 0.25f, center.y - radius * 0.85f), style = Stroke(width = 2.5f))
    }
    DogMood.HAPPY -> {
      // Happy ^ ^ arcs
      listOf(-eyeOffsetX, eyeOffsetX).forEach { ox ->
        val ex = center.x + ox
        val arc = Path().apply {
          moveTo(ex - radius * 0.16f, eyeY + radius * 0.04f)
          quadraticTo(ex, eyeY - radius * 0.18f, ex + radius * 0.16f, eyeY + radius * 0.04f)
        }
        drawPath(arc, InkBlack, style = Stroke(width = 3.6f, cap = StrokeCap.Round))
      }
    }
    DogMood.SCARED -> {
      // Wide frightened eyes + tiny pupils + sweat drop
      listOf(-eyeOffsetX, eyeOffsetX).forEach { ox ->
        val ex = center.x + ox
        drawCircle(Color.White, radius * 0.21f, Offset(ex, eyeY))
        drawCircle(InkBlack, radius * 0.21f, Offset(ex, eyeY), style = Stroke(width = 2.6f))
        drawCircle(InkBlack, radius * 0.07f, Offset(ex, eyeY))
      }
      // Sweat drop
      val sx = center.x + radius * 0.72f
      val sy = center.y - radius * 0.45f
      drawCircle(Color(0xFF4FC3F7), radius * 0.12f, Offset(sx, sy))
    }
    DogMood.CALM -> {
      if (isBlinking) {
        listOf(-eyeOffsetX, eyeOffsetX).forEach { ox ->
          drawLine(
            InkBlack,
            Offset(center.x + ox - radius * 0.15f, eyeY),
            Offset(center.x + ox + radius * 0.15f, eyeY),
            strokeWidth = 3.5f,
            cap = StrokeCap.Round,
          )
        }
      } else {
        listOf(-eyeOffsetX, eyeOffsetX).forEach { ox ->
          val ex = center.x + ox
          drawCircle(InkBlack, radius * 0.17f, Offset(ex, eyeY))
          drawCircle(Color.White, radius * 0.06f, Offset(ex - radius * 0.05f, eyeY - radius * 0.06f))
        }
      }
    }
  }

  // Cute Nose
  drawOval(
    color = InkBlack,
    topLeft = Offset(center.x - radius * 0.14f, center.y - radius * 0.02f),
    size = Size(radius * 0.28f, radius * 0.18f),
  )

  // Mouth
  val mouthY = center.y + radius * 0.24f
  when (mood) {
    DogMood.HAPPY -> {
      val smile = Path().apply {
        moveTo(center.x - radius * 0.24f, mouthY - 2f)
        quadraticTo(center.x, mouthY + radius * 0.28f, center.x + radius * 0.24f, mouthY - 2f)
        close()
      }
      drawPath(smile, Color(0xFFFF6B81))
      drawPath(smile, InkBlack, style = Stroke(width = 2.6f, join = StrokeJoin.Round))
    }
    DogMood.SCARED,
    DogMood.STUNG -> {
      val wavy = Path().apply {
        moveTo(center.x - radius * 0.22f, mouthY + 4f)
        quadraticTo(center.x - radius * 0.1f, mouthY - 5f, center.x, mouthY + 4f)
        quadraticTo(center.x + radius * 0.1f, mouthY + 12f, center.x + radius * 0.22f, mouthY + 4f)
      }
      drawPath(wavy, InkBlack, style = Stroke(width = 3f, cap = StrokeCap.Round))
    }
    DogMood.CALM -> {
      val smile = Path().apply {
        moveTo(center.x - radius * 0.2f, mouthY)
        quadraticTo(center.x - radius * 0.08f, mouthY + radius * 0.13f, center.x, mouthY + 1f)
        quadraticTo(center.x + radius * 0.08f, mouthY + radius * 0.13f, center.x + radius * 0.2f, mouthY)
      }
      drawPath(smile, InkBlack, style = Stroke(width = 2.8f, cap = StrokeCap.Round))
    }
  }

  // Accessories from equipped skin
  when (skin.accessory) {
    "collar" -> {
      drawLine(Color(0xFFE53935), Offset(center.x - radius * 0.55f, center.y + radius * 0.78f), Offset(center.x + radius * 0.55f, center.y + radius * 0.78f), strokeWidth = 7f, cap = StrokeCap.Round)
      drawCircle(Color(0xFFFFC107), radius * 0.14f, Offset(center.x, center.y + radius * 0.90f))
    }
    "scarf" -> {
      drawRoundRect(Color(0xFF29B6F6), Offset(center.x - radius * 0.65f, center.y + radius * 0.65f), Size(radius * 1.3f, radius * 0.28f), CornerRadius(6f, 6f))
    }
    "bow" -> {
      drawCircle(Color(0xFFEC407A), radius * 0.16f, Offset(center.x - radius * 0.52f, center.y - radius * 0.66f))
      drawCircle(Color(0xFFEC407A), radius * 0.16f, Offset(center.x - radius * 0.30f, center.y - radius * 0.66f))
      drawCircle(Color(0xFFF48FB1), radius * 0.08f, Offset(center.x - radius * 0.41f, center.y - radius * 0.66f))
    }
    "headband" -> {
      drawLine(Color(0xFFE53935), Offset(center.x - radius * 0.9f, center.y - radius * 0.48f), Offset(center.x + radius * 0.9f, center.y - radius * 0.48f), strokeWidth = 8f, cap = StrokeCap.Round)
    }
    "crown" -> {
      val crown = Path().apply {
        moveTo(center.x - radius * 0.36f, center.y - radius * 0.86f)
        lineTo(center.x - radius * 0.42f, center.y - radius * 1.24f)
        lineTo(center.x - radius * 0.16f, center.y - radius * 1.02f)
        lineTo(center.x, center.y - radius * 1.32f)
        lineTo(center.x + radius * 0.16f, center.y - radius * 1.02f)
        lineTo(center.x + radius * 0.42f, center.y - radius * 1.24f)
        lineTo(center.x + radius * 0.36f, center.y - radius * 0.86f)
        close()
      }
      drawPath(crown, Color(0xFFFFD54F))
      drawPath(crown, InkBlack, style = Stroke(width = 2.5f, join = StrokeJoin.Round))
    }
  }
}

private fun DrawScope.drawCartoonBee(bee: BeeEntity, highGraphics: Boolean) {
  val heading = atan2(bee.vel.y, bee.vel.x) * (180f / PI.toFloat())
  val facingRight = bee.vel.x >= 0f
  val wingOffset = sin(bee.wingPhase) * 5f

  // Comic motion dash trail behind bee
  if (highGraphics && bee.vel.lenSq() > 4000f) {
    val back = bee.vel.normalized() * -20f
    drawLine(
      color = Color(0xFFFFB300).copy(alpha = 0.45f),
      start = Offset(bee.pos.x + back.x * 0.6f, bee.pos.y + back.y * 0.6f),
      end = Offset(bee.pos.x + back.x * 1.4f, bee.pos.y + back.y * 1.4f),
      strokeWidth = 3f,
      cap = StrokeCap.Round,
      pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f)),
    )
  }

  translate(bee.pos.x, bee.pos.y) {
    val tilt = (heading * 0.15f).coerceIn(-25f, 25f)
    rotate(tilt, pivot = Offset.Zero) {
      val dir = if (facingRight) 1f else -1f
      // Fluttering translucent wings
      drawOval(
        color = Color(0xFFE1F5FE).copy(alpha = 0.85f),
        topLeft = Offset(-8f * dir - 6f, -18f + wingOffset * 0.4f),
        size = Size(12f, 13f),
      )
      drawOval(
        color = InkBlack.copy(alpha = 0.5f),
        topLeft = Offset(-8f * dir - 6f, -18f + wingOffset * 0.4f),
        size = Size(12f, 13f),
        style = Stroke(width = 1.6f),
      )

      // Stinger
      val stinger = Path().apply {
        moveTo(-11f * dir, -2f)
        lineTo(-18f * dir, 1f)
        lineTo(-11f * dir, 4f)
        close()
      }
      drawPath(stinger, InkBlack)

      // Yellow body
      drawOval(
        color = Color(0xFFFFD600),
        topLeft = Offset(-13f, -10f),
        size = Size(26f, 20f),
      )
      // Black stripes
      drawLine(InkBlack, Offset(-5f, -9f), Offset(-5f, 9f), strokeWidth = 4f)
      drawLine(InkBlack, Offset(2f, -9f), Offset(2f, 9f), strokeWidth = 4f)
      drawOval(
        color = InkBlack,
        topLeft = Offset(-13f, -10f),
        size = Size(26f, 20f),
        style = Stroke(width = 2.4f),
      )

      // Angry eye & furrowed brow
      val eyeX = 7f * dir
      drawCircle(Color.White, 3.8f, Offset(eyeX, -2f))
      drawCircle(InkBlack, 1.8f, Offset(eyeX + dir * 0.8f, -2f))
      drawLine(
        InkBlack,
        Offset(eyeX - 4f * dir, -7f),
        Offset(eyeX + 4f * dir, -4f),
        strokeWidth = 2.4f,
        cap = StrokeCap.Round,
      )
    }
  }
}

private fun DrawScope.drawHeartParticle(center: Offset, size: Float, color: Color) {
  val path = Path().apply {
    moveTo(center.x, center.y + size * 0.45f)
    cubicTo(
      center.x - size, center.y - size * 0.1f,
      center.x - size * 0.5f, center.y - size * 0.9f,
      center.x, center.y - size * 0.3f,
    )
    cubicTo(
      center.x + size * 0.5f, center.y - size * 0.9f,
      center.x + size, center.y - size * 0.1f,
      center.x, center.y + size * 0.45f,
    )
  }
  drawPath(path, color)
}

fun DrawScope.drawCartoonPen(
  tip: Offset,
  pen: PenModel,
  elapsed: Float = 0f,
  length: Float = 92f,
  tiltDegrees: Float = 36f,
) {
  val wobble = if (elapsed > 0f) sin(elapsed * 18f) * 2.5f else 0f
  val width = length * 0.22f
  val coneLen = length * 0.24f
  val capLen = length * 0.16f
  val bodyLen = length - coneLen - capLen

  rotate(degrees = tiltDegrees + wobble, pivot = tip) {
    // Soft shadow offset
    translate(left = 4f, top = 5f) {
      drawRoundRect(
        color = Color.Black.copy(alpha = 0.16f),
        topLeft = Offset(tip.x - width * 0.5f, tip.y - length),
        size = Size(width, length - coneLen * 0.55f),
        cornerRadius = CornerRadius(width * 0.4f, width * 0.4f),
      )
    }

    // Tapered wooden/metallic cone tapering into nib at tip
    val cone = Path().apply {
      moveTo(tip.x, tip.y)
      lineTo(tip.x - width * 0.5f, tip.y - coneLen)
      lineTo(tip.x + width * 0.5f, tip.y - coneLen)
      close()
    }
    drawPath(cone, Color(0xFFFFE0B2))
    drawPath(cone, InkBlack, style = Stroke(width = 2.4f, join = StrokeJoin.Round))

    // Colored ink nib at the tip
    val nib = Path().apply {
      moveTo(tip.x, tip.y)
      lineTo(tip.x - width * 0.24f, tip.y - coneLen * 0.45f)
      lineTo(tip.x + width * 0.24f, tip.y - coneLen * 0.45f)
      close()
    }
    drawPath(nib, pen.inkColor)

    // Main pen barrel
    val barrelTopY = tip.y - coneLen - bodyLen
    drawRoundRect(
      color = pen.barrelColor,
      topLeft = Offset(tip.x - width * 0.5f, barrelTopY),
      size = Size(width, bodyLen),
      cornerRadius = CornerRadius(3f, 3f),
    )
    // Gloss highlight band along left side of barrel
    drawRoundRect(
      color = pen.accentColor.copy(alpha = 0.55f),
      topLeft = Offset(tip.x - width * 0.32f, barrelTopY + 4f),
      size = Size(width * 0.22f, (bodyLen - 8f).coerceAtLeast(2f)),
      cornerRadius = CornerRadius(2f, 2f),
    )
    drawRect(
      color = InkBlack,
      topLeft = Offset(tip.x - width * 0.5f, barrelTopY),
      size = Size(width, bodyLen),
      style = Stroke(width = 2.5f),
    )

    // Metallic ferrule collar band
    val ferruleH = (length * 0.08f).coerceAtLeast(5f)
    drawRect(
      color = Color(0xFFCFD8DC),
      topLeft = Offset(tip.x - width * 0.52f, barrelTopY - ferruleH),
      size = Size(width * 1.04f, ferruleH),
    )
    drawRect(
      color = InkBlack,
      topLeft = Offset(tip.x - width * 0.52f, barrelTopY - ferruleH),
      size = Size(width * 1.04f, ferruleH),
      style = Stroke(width = 2.2f),
    )

    // Top eraser / crown cap
    val capTopY = barrelTopY - ferruleH - capLen * 0.72f
    drawRoundRect(
      color = pen.accentColor,
      topLeft = Offset(tip.x - width * 0.48f, capTopY),
      size = Size(width * 0.96f, capLen * 0.78f),
      cornerRadius = CornerRadius(width * 0.35f, width * 0.35f),
    )
    drawRoundRect(
      color = InkBlack,
      topLeft = Offset(tip.x - width * 0.48f, capTopY),
      size = Size(width * 0.96f, capLen * 0.78f),
      cornerRadius = CornerRadius(width * 0.35f, width * 0.35f),
      style = Stroke(width = 2.2f),
    )

    // Pocket clip on side of barrel
    drawLine(
      color = Color(0xFFECEFF1),
      start = Offset(tip.x + width * 0.55f, barrelTopY - ferruleH * 0.3f),
      end = Offset(tip.x + width * 0.55f, barrelTopY + bodyLen * 0.36f),
      strokeWidth = 3f,
      cap = StrokeCap.Round,
    )
  }
}
