package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import com.example.data.model.*
import com.example.game.DungeonGenerator
import kotlin.math.cos
import kotlin.math.sin

data class ViewportColors(
    val skyColor: Color,
    val floorColor: Color,
    val wallColor: Color,
    val wallOutline: Color,
    val ceilingColor: Color,
    val centerLineColor: Color
)

@Composable
fun RetroViewport(
    grid: List<List<Int>>,
    playerX: Int,
    playerY: Int,
    playerDir: Int,
    mobs: List<MobInstance>,
    chests: List<ChestInstance>,
    nightmareIntensity: Float, // 0.0 to 1.0f
    tickTime: Long = System.currentTimeMillis(),
    modifier: Modifier = Modifier
) {
    // Generate dynamic shifting theme colors based on nightmare intensity
    val colors = getDynamicColors(nightmareIntensity)

    Box(
        modifier = modifier
            .background(colors.skyColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Draw solid background skies or ceiling/floor halves as fallback base
            drawRect(
                color = colors.floorColor,
                topLeft = Offset(0f, height * 0.5f),
                size = Size(width, height * 0.5f)
            )
            drawRect(
                color = colors.ceilingColor,
                topLeft = Offset(0f, 0f),
                size = Size(width, height * 0.5f)
            )

            // Let's establish directional vectors based on playerDir
            val (dx, dy) = when (playerDir) {
                0 -> Pair(0, -1) // North
                1 -> Pair(1, 0)  // East
                2 -> Pair(0, 1)  // South
                3 -> Pair(-1, 0) // West
                else -> Pair(0, 0)
            }

            // Left perpendicular
            val (ldx, ldy) = when (playerDir) {
                0 -> Pair(-1, 0)
                1 -> Pair(0, -1)
                2 -> Pair(1, 0)
                3 -> Pair(0, 1)
                else -> Pair(0, 0)
            }

            // Right perpendicular
            val (rdx, rdy) = when (playerDir) {
                0 -> Pair(1, 0)
                1 -> Pair(0, 1)
                2 -> Pair(-1, 0)
                3 -> Pair(0, -1)
                else -> Pair(0, 0)
            }

            // Depth calculation boundaries (d: 0 to 5)
            // L, R, T, B at depth d
            fun getBounds(d: Int): RectCoords {
                val maxDepth = 5.0f
                val factor = d.toFloat() / (maxDepth + 0.3f)
                val xL = width * (factor / 2f)
                val xR = width * (1f - factor / 2f)
                val yT = height * (factor / 2f)
                val yB = height * (1f - factor / 2f)
                return RectCoords(xL, xR, yT, yB)
            }

            fun getTileAt(x: Int, y: Int): Int {
                if (y >= 0 && y < grid.size && x >= 0 && x < grid[y].size) {
                    return grid[y][x]
                }
                return DungeonGenerator.TILE_WALL
            }

            // Loop back-to-front (depth 5 down to 0) so closer walls are rendered on top
            for (d in 5 downTo 0) {
                // Bounds at depth d (closer) and d+1 (further away)
                val b1 = getBounds(d)
                val b2 = getBounds(d + 1)

                val cx = playerX + d * dx
                val cy = playerY + d * dy

                val lx = cx + ldx
                val ly = cy + ldy

                val rx = cx + rdx
                val ry = cy + rdy

                val centerTile = getTileAt(cx, cy)
                val leftTile = getTileAt(lx, ly)
                val rightTile = getTileAt(rx, ry)

                // Shading intensity multiplier gets darker with depth for Dynamic Light!
                val baseShade = (1.0f - (d / 5.5f)).coerceIn(0.12f, 1.0f)
                // Add retro "pulsing flashlight" effect when nightmare increases
                val pulse = if (nightmareIntensity > 0.5f) {
                    1.0f - (0.05f * sin((tickTime / 180f).toDouble()).toFloat())
                } else 1.0f

                val shade = baseShade * pulse
                
                // Shade colors
                val shadedWall = lerpColor(Color.Black, colors.wallColor, shade)
                val shadedOutline = lerpColor(Color.Black, colors.wallOutline, shade)
                val shadedFloor = lerpColor(Color.Black, colors.floorColor, shade)
                val shadedCeiling = lerpColor(Color.Black, colors.ceilingColor, shade)

                if (centerTile == DungeonGenerator.TILE_WALL) {
                    // Draw a flat solid front-facing rectangle representing looking straight into a wall!
                    drawRect(
                        color = shadedWall,
                        topLeft = Offset(b1.xL, b1.yT),
                        size = Size(b1.xR - b1.xL, b1.yB - b1.yT)
                    )
                    // Draw outline
                    drawRect(
                        color = shadedOutline,
                        topLeft = Offset(b1.xL, b1.yT),
                        size = Size(b1.xR - b1.xL, b1.yB - b1.yT),
                        style = Stroke(width = 3.5f)
                    )
                } else {
                    // Draw Floor segment
                    drawPolygon(
                        points = listOf(
                            Offset(b1.xL, b1.yB),
                            Offset(b2.xL, b2.yB),
                            Offset(b2.xR, b2.yB),
                            Offset(b1.xR, b1.yB)
                        ),
                        color = shadedFloor,
                        outlineColor = shadedOutline
                    )

                    // Draw Ceiling segment
                    drawPolygon(
                        points = listOf(
                            Offset(b1.xL, b1.yT),
                            Offset(b2.xL, b2.yT),
                            Offset(b2.xR, b2.yT),
                            Offset(b1.xR, b1.yT)
                        ),
                        color = shadedCeiling,
                        outlineColor = shadedOutline
                    )

                    // Draw Left Wall if solid
                    if (leftTile == DungeonGenerator.TILE_WALL) {
                        drawPolygon(
                            points = listOf(
                                Offset(b1.xL, b1.yT),
                                Offset(b2.xL, b2.yT),
                                Offset(b2.xL, b2.yB),
                                Offset(b1.xL, b1.yB)
                            ),
                            color = shadedWall,
                            outlineColor = shadedOutline
                        )
                    } else {
                        // Drawing side corridor opening back walls to make maze exploration cohesive
                        drawPolygon(
                            points = listOf(
                                Offset(b1.xL, b1.yT),
                                Offset(b2.xL, b2.yT),
                                Offset(b2.xL, b2.yB),
                                Offset(b1.xL, b1.yB)
                            ),
                            color = shadedFloor.copy(alpha = 0.4f),
                            outlineColor = shadedOutline.copy(alpha = 0.3f)
                        )
                    }

                    // Draw Right Wall if solid
                    if (rightTile == DungeonGenerator.TILE_WALL) {
                        drawPolygon(
                            points = listOf(
                                Offset(b1.xR, b1.yT),
                                Offset(b2.xR, b2.yT),
                                Offset(b2.xR, b2.yB),
                                Offset(b1.xR, b1.yB)
                            ),
                            color = shadedWall,
                            outlineColor = shadedOutline
                        )
                    } else {
                        drawPolygon(
                            points = listOf(
                                Offset(b1.xR, b1.yT),
                                Offset(b2.xR, b2.yT),
                                Offset(b2.xR, b2.yB),
                                Offset(b1.xR, b1.yB)
                            ),
                            color = shadedFloor.copy(alpha = 0.4f),
                            outlineColor = shadedOutline.copy(alpha = 0.3f)
                        )
                    }

                    // --- DRAW SPRITES (CHESTS, ENEMIES, EXIT) ---
                    // Draw sprites at position (cx, cy)
                    val chestHere = chests.find { it.x == cx && it.y == cy }
                    val mobHere = mobs.find { it.x == cx && it.y == cy && !it.isDefeated }

                    val hSize = (b1.yB - b1.yT) * 0.45f
                    val wSize = hSize
                    val cX = (b1.xL + b1.xR) / 2f
                    val cY = b1.yB - (hSize / 2f) // Align bottom to hallway floor!

                    // Draw exit stairs if tile is 3
                    if (centerTile == DungeonGenerator.TILE_EXIT) {
                        // Draw glowing crescent moon vortex
                        val animPulse = 1.0f + 0.15f * sin((tickTime / 150f).toDouble()).toFloat()
                        drawCircle(
                            color = if (nightmareIntensity > 0.6f) Color(0xFFFF00FF) else Color(0xFF00FFFF),
                            radius = (b1.xR - b1.xL) * 0.32f * animPulse,
                            center = Offset(cX, b1.yB - 10f),
                            style = Stroke(width = 4f)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.8f),
                            radius = (b1.xR - b1.xL) * 0.15f * animPulse,
                            center = Offset(cX, b1.yB - 10f),
                            style = Fill
                        )
                    }

                    // Draw starting platform moon standard glow
                    if (centerTile == DungeonGenerator.TILE_START && d == 0) {
                        drawCircle(
                            color = Color.Yellow.copy(alpha = 0.3f),
                            radius = width * 0.2f,
                            center = Offset(width * 0.5f, height * 0.9f),
                            style = Fill
                        )
                    }

                    if (chestHere != null) {
                        // Retro toy-box treasure chest
                        drawChest(cX, cY, wSize, hSize, chestHere, shadedOutline)
                    }

                    if (mobHere != null) {
                        // Crux part! Procedural Canvas vector monster drawing!
                        drawMob(cX, cY - (hSize * 0.15f), wSize, hSize, mobHere, tickTime, nightmareIntensity)
                    }
                }
            }
        }
    }
}

// Coordinate layout structure
private data class RectCoords(val xL: Float, val xR: Float, val yT: Float, val yB: Float)

// Interpolate colors easily
private fun lerpColor(start: Color, stop: Color, fraction: Float): Color {
    return lerp(start, stop, fraction)
}

// Generate stylized colors based on nightmare intensity
private fun getDynamicColors(intensity: Float): ViewportColors {
    return when {
         intensity < 0.30f -> {
             // Sweet dreamy phase - pastel yellow/wood beige, soft blue, lavender
             val t = intensity / 0.30f
             ViewportColors(
                 skyColor = lerpColor(Color(0xFF87CEEB), Color(0xFF6A5ACD), t), // soft blue -> slate violet
                 floorColor = lerpColor(Color(0xFFFFC0CB), Color(0xFFE6E6FA), t), // bubble pink -> soft lavender
                 wallColor = lerpColor(Color(0xFFFFFDD0), Color(0xFFF0E68C), t), // cream-yellow -> khaki wood
                 wallOutline = lerpColor(Color(0xFF8B4513), Color(0xFF4B3621), t), // warm wood brown
                 ceilingColor = lerpColor(Color(0xFFE6E6FA), Color(0xFFD8BFD8), t), // light lavender -> thistle purple
                 centerLineColor = Color(0xFFFFD700) // soft gold
             )
         }
         intensity < 0.70f -> {
             // Classroom / school hallways anxieties - teal green, slate grey, neon yellow accent
             val t = (intensity - 0.30f) / 0.40f
             ViewportColors(
                 skyColor = lerpColor(Color(0xFF6A5ACD), Color(0xFF3B1E43), t), // purpleish -> night wine
                 floorColor = lerpColor(Color(0xFFE6E6FA), Color(0xFF708090), t), // soft lav -> chalkboard grey
                 wallColor = lerpColor(Color(0xFFF0E68C), Color(0xFF2E5A44), t), // wood -> school desk steel green
                 wallOutline = lerpColor(Color(0xFF4B3621), Color(0xFFFFFF00), t), // neon yellow outline
                 ceilingColor = lerpColor(Color(0xFFD8BFD8), Color(0xFF483D8B), t), // dark slate blue
                 centerLineColor = Color(0xFF00FF00) // electric lime green
             )
         }
         else -> {
             // Peak clinic nightmare - charcoal black, eerie crimson, glitching hot pink / cyan outline
             val t = (intensity - 0.70f) / 0.30f
             ViewportColors(
                 skyColor = lerpColor(Color(0xFF3B1E43), Color(0xFF030005), t), // dark wine -> absolute black
                 floorColor = lerpColor(Color(0xFF708090), Color(0xFF1F030B), t), // gray slate -> blood maroon
                 wallColor = lerpColor(Color(0xFF2E5A44), Color(0xFF101015), t), // dark green -> cold metallic shadow slate
                 wallOutline = lerpColor(Color(0xFFFFFF00), Color(0xFFFF0055), t), // screeching yellow -> electric crimson outline
                 ceilingColor = lerpColor(Color(0xFF483D8B), Color(0xFF0C0212), t), // dark slate -> spooky deep violet void
                 centerLineColor = Color(0xFFFF00FF) // neon magenta
             )
         }
    }
}

// Draw a polygon
private fun DrawScope.drawPolygon(
    points: List<Offset>,
    color: Color,
    outlineColor: Color?,
    lineWidth: Float = 2.5f
) {
    if (points.size < 3) return
    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            lineTo(points[i].x, points[i].y)
        }
        close()
    }
    drawPath(path, color = color, style = Fill)
    if (outlineColor != null) {
        drawPath(path, color = outlineColor, style = Stroke(width = lineWidth))
    }
}

// Drawing chest boxes on canvas
private fun DrawScope.drawChest(
    cX: Float,
    cY: Float,
    w: Float,
    h: Float,
    chest: ChestInstance,
    outlineColor: Color
) {
    val left = cX - w * 0.4f
    val top = cY - h * 0.3f
    val rWidth = w * 0.8f
    val rHeight = h * 0.6f

    // Base body color - retro brown
    val bodyColor = if (chest.isOpen) Color(0xFF6E473B) else Color(0xFF8B5A2B)
    drawRect(
        color = bodyColor,
        topLeft = Offset(left, top),
        size = Size(rWidth, rHeight)
    )

    // Gold trim/accent
    val trimColor = Color(0xFFFFD700)
    // Draw lid border line
    drawLine(
        color = trimColor,
        start = Offset(left, top + rHeight * 0.35f),
        end = Offset(left + rWidth, top + rHeight * 0.35f),
        strokeWidth = 3f
    )

    // Center lock
    val lockColor = if (chest.isOpen) Color.Gray else Color(0xFFFFD700)
    drawRect(
        color = lockColor,
        topLeft = Offset(cX - w * 0.08f, top + rHeight * 0.25f),
        size = Size(w * 0.16f, h * 0.18f)
    )

    // Chest outline
    drawRect(
        color = outlineColor,
        topLeft = Offset(left, top),
        size = Size(rWidth, rHeight),
        style = Stroke(width = 3f)
    )
}

// Drawing customizable cute anxiety toy monsters using vector shapes!
private fun DrawScope.drawMob(
    cX: Float,
    cY: Float,
    w: Float,
    h: Float,
    mob: MobInstance,
    tickTime: Long,
    nightmareIntensity: Float
) {
    val pulseAmount = 1.0f + 0.08f * sin((tickTime / 120.0)).toFloat()
    val shakeOffset = if (nightmareIntensity >= 0.70f) {
        // Shuddering glitch movement of scary nightmares!
        val shift = 4f * sin((tickTime / 40.0)).toFloat()
        Offset(shift, shift)
    } else {
        Offset(0f, 0f)
    }

    val finalX = cX + shakeOffset.x
    val finalY = cY + shakeOffset.y
    val baseRadius = (w * 0.36f) * pulseAmount

    when (mob.type) {
        MobType.SCRIBBLE_SPIDER -> {
            // Draw spider legs
            val legColor = Color(0xFF1E1E24)
            for (i in -3..3) {
                if (i != 0) {
                    val angle = (i * 20f).toDouble()
                    val start = Offset(finalX, finalY)
                    val xDir = sin(Math.toRadians(angle)).toFloat()
                    val yDir = cos(Math.toRadians(angle)).toFloat()
                    drawLine(
                        color = legColor,
                        start = start,
                        end = Offset(finalX + xDir * baseRadius * 1.5f, finalY + yDir * baseRadius * 0.9f),
                        strokeWidth = 4f
                    )
                }
            }
            // Spider body
            drawCircle(
                color = Color(0xFFD32F2F), // Scribble spider red
                radius = baseRadius * 0.7f,
                center = Offset(finalX, finalY)
            )
            // Tiny purple scribble eyes
            drawCircle(color = Color(0xFFEE82EE), radius = baseRadius * 0.12f, center = Offset(finalX - 10f, finalY - 5f))
            drawCircle(color = Color(0xFFEE82EE), radius = baseRadius * 0.12f, center = Offset(finalX + 10f, finalY - 5f))
        }

        MobType.BROKEN_CRANE -> {
            // Draw a blocky stack, wooden block structure (ruined playground)
            val blockColor = Color(0xFFF4A460) // Sand-brown wooden block
            drawRect(
                color = blockColor,
                topLeft = Offset(finalX - baseRadius, finalY - baseRadius * 0.8f),
                size = Size(baseRadius * 2f, baseRadius * 1.6f)
            )
            // Yellow crane arm swinging
            val armSweep = 30f * sin((tickTime / 160f).toDouble()).toFloat()
            val rads = Math.toRadians(armSweep.toDouble())
            val armEndX = finalX + cos(rads).toFloat() * baseRadius * 1.5f
            val armEndY = finalY - baseRadius - sin(rads).toFloat() * baseRadius * 1.5f
            drawLine(
                color = Color(0xFFFFD700),
                start = Offset(finalX, finalY - baseRadius * 0.6f),
                end = Offset(armEndX, armEndY),
                strokeWidth = 8f
            )
            // Draw tiny pixelated glowing red eye
            drawRect(
                color = Color.Red,
                topLeft = Offset(finalX - 5f, finalY - 8f),
                size = Size(10f, 10f)
            )
        }

        MobType.SPELLING_BEE -> {
            // Draw Orthodontist Spelling Bee
            // Striped yellow-black body
            val stripes = 4
            val stripHeight = (baseRadius * 1.4f) / stripes
            val bodyLeft = finalX - baseRadius * 0.9f
            val bodyTop = finalY - baseRadius * 0.7f
            
            for (i in 0 until stripes) {
                drawRect(
                    color = if (i % 2 == 0) Color(0xFFFFD700) else Color(0xFF111111),
                    topLeft = Offset(bodyLeft, bodyTop + i * stripHeight),
                    size = Size(baseRadius * 1.8f, stripHeight)
                )
            }
            // Cute paper wings
            val wingAngle = 25f * sin((tickTime * 0.05).toDouble()).toFloat()
            val wRadsL = Math.toRadians((-135f + wingAngle).toDouble())
            val wRadsR = Math.toRadians((-45f - wingAngle).toDouble())

            drawLine(
                color = Color.White.copy(alpha = 0.7f),
                start = Offset(finalX, bodyTop),
                end = Offset(finalX + cos(wRadsL).toFloat() * baseRadius * 1.2f, bodyTop + sin(wRadsL).toFloat() * baseRadius * 1.2f),
                strokeWidth = 5f
            )
            drawLine(
                color = Color.White.copy(alpha = 0.7f),
                start = Offset(finalX, bodyTop),
                end = Offset(finalX + cos(wRadsR).toFloat() * baseRadius * 1.2f, bodyTop + sin(wRadsR).toFloat() * baseRadius * 1.2f),
                strokeWidth = 5f
            )

            // Giant silver dental retainers brace!
            drawLine(
                color = Color(0xFFCCCCCC),
                start = Offset(finalX - baseRadius * 0.7f, finalY + baseRadius * 0.2f),
                end = Offset(finalX + baseRadius * 0.7f, finalY + baseRadius * 0.2f),
                strokeWidth = 4f
            )
            // Glowing cyan eyes
            drawCircle(color = Color(0xFF00FFFF), radius = 6f, center = Offset(finalX - 12f, finalY - 14f))
            drawCircle(color = Color(0xFF00FFFF), radius = 6f, center = Offset(finalX + 12f, finalY - 14f))
        }

        MobType.FRACTURED_RULER -> {
            // High school anxiety - green ruler segment
            val path = Path().apply {
                moveTo(finalX - baseRadius * 0.4f, finalY - baseRadius * 1.6f)
                lineTo(finalX + baseRadius * 0.4f, finalY - baseRadius * 1.2f)
                lineTo(finalX + baseRadius * 0.1f, finalY + baseRadius * 1.4f)
                lineTo(finalX - baseRadius * 0.6f, finalY + baseRadius * 1.1f)
                close()
            }
            drawPath(path, color = Color(0xFF4CAF50))
            // Fractured zigzag cracks
            drawLine(
                color = Color.Red,
                start = Offset(finalX - baseRadius * 0.1f, finalY - baseRadius * 0.2f),
                end = Offset(finalX + baseRadius * 0.2f, finalY + baseRadius * 0.3f),
                strokeWidth = 3f
            )
            // Yellow dots on teeth
            drawCircle(color = Color.Yellow, radius = 5f, center = Offset(finalX - 5f, finalY - 25f))
            drawCircle(color = Color.Yellow, radius = 5f, center = Offset(finalX + 5f, finalY - 21f))
        }

        MobType.LATE_ALARM -> {
            // Alarm clock ringing - vibrates vigorously
            val shakeScale = 8f * sin((tickTime / 30.0)).toFloat()
            val clockX = finalX + shakeScale

            // Draw clock bell ears
            drawCircle(color = Color(0xFFB22222), radius = baseRadius * 0.35f, center = Offset(clockX - baseRadius * 0.7f, finalY - baseRadius * 0.7f))
            drawCircle(color = Color(0xFFB22222), radius = baseRadius * 0.35f, center = Offset(clockX + baseRadius * 0.7f, finalY - baseRadius * 0.7f))

            // Main face
            drawCircle(color = Color(0xFFFF6347), radius = baseRadius, center = Offset(clockX, finalY))
            drawCircle(color = Color.White, radius = baseRadius * 0.75f, center = Offset(clockX, finalY))

            // Red central panic clock hands
            drawLine(color = Color.Black, start = Offset(clockX, finalY), end = Offset(clockX + baseRadius * 0.5f, finalY - baseRadius * 0.2f), strokeWidth = 5f)
            drawLine(color = Color.Black, start = Offset(clockX, finalY), end = Offset(clockX - baseRadius * 0.1f, finalY + baseRadius * 0.4f), strokeWidth = 5f)

            // Angry clock face eyebrows
            drawLine(color = Color.Red, start = Offset(clockX - 16f, finalY - 14f), end = Offset(clockX - 4f, finalY - 6f), strokeWidth = 3f)
            drawLine(color = Color.Red, start = Offset(clockX + 16f, finalY - 14f), end = Offset(clockX + 4f, finalY - 6f), strokeWidth = 3f)
        }

        MobType.CLOSET_SHADOW -> {
            // Shadow creeper, purple dark aura
            val points = 8
            val path = Path()
            for (i in 0 until points) {
                val angle = (i * (360f / points)).toDouble()
                val offsetDist = baseRadius * (1.1f + 0.25f * sin((tickTime / 100f) + i).toFloat())
                val pX = finalX + cos(Math.toRadians(angle)).toFloat() * offsetDist
                val pY = finalY + sin(Math.toRadians(angle)).toFloat() * offsetDist
                if (i == 0) path.moveTo(pX, pY) else path.lineTo(pX, pY)
            }
            path.close()
            drawPath(path, color = Color(0xFF1A112B)) // Shadow deep purple
            // Glowing screaming yellow eyes
            drawCircle(color = Color.Yellow, radius = baseRadius * 0.15f, center = Offset(finalX - 12f, finalY - 4f))
            drawCircle(color = Color.Yellow, radius = baseRadius * 0.15f, center = Offset(finalX + 12f, finalY - 4f))
            // Black slit pupil
            drawCircle(color = Color.Black, radius = 2f, center = Offset(finalX - 12f, finalY - 4f))
            drawCircle(color = Color.Black, radius = 2f, center = Offset(finalX + 12f, finalY - 4f))
        }

        MobType.JACK_IN_THE_BOX -> {
            // Draw colorful toy box and popping spring clown head
            // Box
            drawRect(
                color = Color(0xFF4169E1), // Royal blue box
                topLeft = Offset(finalX - baseRadius * 0.7f, finalY + baseRadius * 0.1f),
                size = Size(baseRadius * 1.4f, baseRadius * 0.9f)
            )
            // Yellow spiralled spring
            val springCount = 5
            val springStep = (baseRadius * 0.8f) / springCount
            for (i in 0 until springCount) {
                drawLine(
                    color = Color.LightGray,
                    start = Offset(finalX - baseRadius * 0.2f, finalY + baseRadius * 0.1f - i * springStep),
                    end = Offset(finalX + baseRadius * 0.2f, finalY + baseRadius * 0.1f - (i + 1) * springStep),
                    strokeWidth = 3f
                )
            }
            // Clown head grinning
            val headY = finalY + baseRadius * 0.1f - springCount * springStep
            drawCircle(color = Color.White, radius = baseRadius * 0.45f, center = Offset(finalX, headY))
            drawCircle(color = Color.Red, radius = 6f, center = Offset(finalX, headY + 2f)) // Red nose
            // Green clown curly hair on sides
            drawCircle(color = Color.Green, radius = 10f, center = Offset(finalX - baseRadius * 0.45f, headY - 4f))
            drawCircle(color = Color.Green, radius = 10f, center = Offset(finalX + baseRadius * 0.45f, headY - 4f))
            // Grinning black mouth
            val mouthPath = Path().apply {
                moveTo(finalX - 12f, headY + 10f)
                quadraticTo(finalX, headY + 22f, finalX + 12f, headY + 10f)
            }
            drawPath(mouthPath, color = Color.Red, style = Stroke(width = 3f))
        }

        MobType.DUST_BUNNY_BEHEMOTH -> {
            // Dust cloud with tentacles
            val cloudRadius = baseRadius * 1.2f
            drawCircle(color = Color(0xFF4A4B50), radius = cloudRadius, center = Offset(finalX, finalY))
            drawCircle(color = Color(0xFF63646B), radius = cloudRadius * 0.72f, center = Offset(finalX - 16f, finalY - 10f))
            // Scary red dot clusters for swarm eyes
            drawCircle(color = Color.Red, radius = 4f, center = Offset(finalX - 10f, finalY - 6f))
            drawCircle(color = Color.Red, radius = 4f, center = Offset(finalX - 5f, finalY - 14f))
            drawCircle(color = Color.Red, radius = 4f, center = Offset(finalX + 8f, finalY - 8f))
            drawCircle(color = Color.Red, radius = 4f, center = Offset(finalX, finalY + 4f))
        }

        MobType.DRILL_TEDDY -> {
            // Teddy bear head with whirring silver dentist drill
            // Bear rounded head
            drawCircle(color = Color(0xFF8B5A2B), radius = baseRadius * 1.0f, center = Offset(finalX, finalY))
            // Ears
            drawCircle(color = Color(0xFF8B5A2B), radius = baseRadius * 0.35f, center = Offset(finalX - baseRadius * 0.8f, finalY - baseRadius * 0.8f))
            drawCircle(color = Color(0xFF8B5A2B), radius = baseRadius * 0.35f, center = Offset(finalX + baseRadius * 0.8f, finalY - baseRadius * 0.8f))
            drawCircle(color = Color(0xFFFFC0CB), radius = baseRadius * 0.15f, center = Offset(finalX - baseRadius * 0.8f, finalY - baseRadius * 0.8f))
            drawCircle(color = Color(0xFFFFC0CB), radius = baseRadius * 0.15f, center = Offset(finalX + baseRadius * 0.8f, finalY - baseRadius * 0.8f))

            // Corrupted button eyes list
            drawCircle(color = Color.Black, radius = 8f, center = Offset(finalX - 15f, finalY - 10f))
            drawLine(color = Color.Red, start = Offset(finalX - 21f, finalY - 16f), end = Offset(finalX - 9f, finalY - 4f), strokeWidth = 3f)
            drawLine(color = Color.Red, start = Offset(finalX - 9f, finalY - 16f), end = Offset(finalX - 21f, finalY - 4f), strokeWidth = 3f)
            
            drawCircle(color = Color.Red, radius = 8f, center = Offset(finalX + 15f, finalY - 10f)) // glowing corrupted red button

            // Snout and Whirring silver drill coming out of mouth!
            drawCircle(color = Color(0xFFD2B48C), radius = baseRadius * 0.35f, center = Offset(finalX, finalY + baseRadius * 0.3f))
            
            // Drill bit
            val drillSpinAngle = (tickTime * 1.5).toDouble()
            val drillPath = Path().apply {
                moveTo(finalX - 10f, finalY + baseRadius * 0.4f)
                lineTo(finalX + 10f, finalY + baseRadius * 0.4f)
                lineTo(finalX + sin(Math.toRadians(drillSpinAngle)).toFloat() * 2f, finalY + baseRadius * 1.4f)
                close()
            }
            drawPath(drillPath, color = Color(0xFFB0C4DE))
            // Spiral screw lines
            for (j in 0..3) {
                val yOffset = baseRadius * 0.4f + j * baseRadius * 0.25f
                drawLine(
                    color = Color.DarkGray,
                    start = Offset(finalX - 8f + j * 2f, finalY + yOffset),
                    end = Offset(finalX + 8f - j * 2f, finalY + yOffset + 5f),
                    strokeWidth = 2.5f
                )
            }
        }

        MobType.LOLLIPOP_MIMIC -> {
            // Sticky candy golem
            drawCircle(color = Color(0xFFFF1493), radius = baseRadius, center = Offset(finalX, finalY)) // Hot pink swirl
            // Swirl lines on lollipop
            val swirlCount = 6
            for (i in 0 until swirlCount) {
                val sweep = (tickTime / 100.0) + (i * (360.0 / swirlCount))
                val radStart = Math.toRadians(sweep)
                val radEnd = Math.toRadians(sweep + 45)
                drawLine(
                    color = Color.White,
                    start = Offset(finalX + cos(radStart).toFloat() * (baseRadius * 0.2f), finalY + sin(radStart).toFloat() * (baseRadius * 0.2f)),
                    end = Offset(finalX + cos(radEnd).toFloat() * (baseRadius * 0.9f), finalY + sin(radEnd).toFloat() * (baseRadius * 0.9f)),
                    strokeWidth = 4f
                )
            }
            // Big sticky drooling mouth
            val droolPath = Path().apply {
                moveTo(finalX - baseRadius * 0.5f, finalY + baseRadius * 0.1f)
                lineTo(finalX + baseRadius * 0.5f, finalY + baseRadius * 0.1f)
                lineTo(finalX + baseRadius * 0.3f, finalY + baseRadius * 0.6f)
                lineTo(finalX - baseRadius * 0.3f, finalY + baseRadius * 0.6f)
                close()
            }
            drawPath(droolPath, color = Color(0xFF00FF00)) // Lime green sugar drool
            // Evil yellow eyes
            drawCircle(color = Color.Yellow, radius = 8f, center = Offset(finalX - 15f, finalY - 20f))
            drawCircle(color = Color.Yellow, radius = 8f, center = Offset(finalX + 15f, finalY - 20f))
        }

        MobType.TOOTH_COLLECTOR -> {
            // Boss - spooky creature holding giant keys and grin overflowing with white teeth-gems
            // Giant black cloud silhouette with white crown
            drawCircle(color = Color(0xFF0D0112), radius = baseRadius * 1.3f, center = Offset(finalX, finalY))
            
            // Giant white grin
            val mouthWidth = baseRadius * 1.4f
            val mouthHeight = baseRadius * 0.6f
            val mouthY = finalY + baseRadius * 0.2f
            
            drawRect(
                color = Color.Black,
                topLeft = Offset(finalX - mouthWidth/2, mouthY - mouthHeight/2),
                size = Size(mouthWidth, mouthHeight)
            )

            // Grid of individual white sharp jagged teeth
            val teethCount = 8
            val toothWidth = mouthWidth / teethCount
            for (k in 0 until teethCount) {
                // Top teeth
                val toothPathT = Path().apply {
                    moveTo(finalX - mouthWidth / 2 + k * toothWidth, mouthY - mouthHeight / 2)
                    lineTo(finalX - mouthWidth / 2 + (k + 0.5f) * toothWidth, mouthY)
                    lineTo(finalX - mouthWidth / 2 + (k + 1) * toothWidth, mouthY - mouthHeight / 2)
                    close()
                }
                drawPath(toothPathT, color = Color.White)

                // Bottom teeth
                val toothPathB = Path().apply {
                    moveTo(finalX - mouthWidth / 2 + k * toothWidth, mouthY + mouthHeight / 2)
                    lineTo(finalX - mouthWidth / 2 + (k + 0.5f) * toothWidth, mouthY)
                    lineTo(finalX - mouthWidth / 2 + (k + 1) * toothWidth, mouthY + mouthHeight / 2)
                    close()
                }
                drawPath(toothPathB, color = Color.White)
            }

            // Big glowing blood-shot red pupil eyes
            drawCircle(color = Color.Red, radius = 12f, center = Offset(finalX - 25f, finalY - 18f))
            drawCircle(color = Color.White, radius = 3f, center = Offset(finalX - 23f, finalY - 20f))

            drawCircle(color = Color.Red, radius = 12f, center = Offset(finalX + 25f, finalY - 18f))
            drawCircle(color = Color.White, radius = 3f, center = Offset(finalX + 27f, finalY - 20f))
        }
    }
}
