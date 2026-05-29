package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
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
                    // Draw brick textures
                    drawFrontWallBricks(
                        xL = b1.xL,
                        xR = b1.xR,
                        yT = b1.yT,
                        yB = b1.yB,
                        color = shadedOutline.copy(alpha = 0.4f)
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
                    // Draw Floor Tiles
                    drawFloorCeilingTiles(
                        xL1 = b1.xL, xR1 = b1.xR, y1 = b1.yB,
                        xL2 = b2.xL, xR2 = b2.xR, y2 = b2.yB,
                        color = shadedOutline.copy(alpha = 0.35f)
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
                    // Draw Ceiling Tiles
                    drawFloorCeilingTiles(
                        xL1 = b1.xL, xR1 = b1.xR, y1 = b1.yT,
                        xL2 = b2.xL, xR2 = b2.xR, y2 = b2.yT,
                        color = shadedOutline.copy(alpha = 0.3f)
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
                        // Perspective Left Wall brick textures
                        drawPerspectiveWallBricks(
                            x1 = b1.xL, yTop1 = b1.yT, yBot1 = b1.yB,
                            x2 = b2.xL, yTop2 = b2.yT, yBot2 = b2.yB,
                            color = shadedOutline.copy(alpha = 0.4f)
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
                        // Perspective Right Wall brick textures
                        drawPerspectiveWallBricks(
                            x1 = b1.xR, yTop1 = b1.yT, yBot1 = b1.yB,
                            x2 = b2.xR, yTop2 = b2.yT, yBot2 = b2.yB,
                            color = shadedOutline.copy(alpha = 0.4f)
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

// Drawing realistic chest boxes on canvas
private fun DrawScope.drawSparkleStar(cX: Float, cY: Float, size: Float, color: Color) {
    if (size <= 0.1f) return
    val path = Path().apply {
        moveTo(cX, cY - size)
        quadraticTo(cX, cY, cX + size, cY)
        quadraticTo(cX, cY, cX, cY + size)
        quadraticTo(cX, cY, cX - size, cY)
        quadraticTo(cX, cY, cX, cY - size)
        close()
    }
    drawPath(path = path, color = color, style = Fill)
}

private fun DrawScope.drawChest(
    cX: Float,
    cY: Float,
    w: Float,
    h: Float,
    chest: ChestInstance,
    outlineColor: Color
) {
    if (w < 4f || h < 4f) return
    val left = cX - w * 0.4f
    val top = cY - h * 0.35f
    val rWidth = w * 0.8f
    val rHeight = h * 0.7f

    if (!chest.isOpen) {
        // --- CLOSED RETRO SCHOOL LOCKER ---
        // Metallic door panel
        drawRoundRect(
            color = Color(0xFF607D8B), // Sleek Steel Blue Locker
            topLeft = Offset(left, top),
            size = Size(rWidth, rHeight),
            cornerRadius = CornerRadius(8f, 8f),
            style = Fill
        )
        
        // Vent slots at the top (Realistic locker louvers)
        val louverW = rWidth * 0.4f
        val louverH = rHeight * 0.03f
        val louverYStart = top + rHeight * 0.1f
        for (i in 0..3) {
            drawRoundRect(
                color = Color(0xFF1B2A32), // Dark interior shadow
                topLeft = Offset(cX - louverW / 2, louverYStart + i * (louverH * 2f)),
                size = Size(louverW, louverH),
                cornerRadius = CornerRadius(2f, 2f)
            )
        }

        // Vent slots at the bottom (for symmetry)
        val louverYStartBot = top + rHeight * 0.75f
        for (i in 0..2) {
            drawRoundRect(
                color = Color(0xFF1B2A32),
                topLeft = Offset(cX - louverW / 2, louverYStartBot + i * (louverH * 2f)),
                size = Size(louverW, louverH),
                cornerRadius = CornerRadius(2f, 2f)
            )
        }

        // Locker Handle recessed cup well and lever
        val handleW = rWidth * 0.16f
        val handleH = rHeight * 0.22f
        val handleX = left + rWidth * 0.18f
        val handleY = top + rHeight * 0.38f
        drawRoundRect(
            color = Color(0xFF263238), // Recessed pocket
            topLeft = Offset(handleX, handleY),
            size = Size(handleW, handleH),
            cornerRadius = CornerRadius(4f, 4f)
        )
        // Silver lever
        drawRect(
            color = Color(0xFFCFD8DC),
            topLeft = Offset(handleX + handleW * 0.3f, handleY + handleH * 0.1f),
            size = Size(handleW * 0.4f, handleH * 0.8f)
        )

        // Hanging combination lock pad! (Black circular lock with brass shackle)
        val shackleRadius = handleW * 0.45f
        val shackleCenterX = handleX + handleW * 0.5f
        val shackleCenterY = handleY + handleH * 0.95f
        
        // Shackle loop
        drawCircle(
            color = Color(0xFFD4AF37), // Brass ring
            radius = shackleRadius,
            center = Offset(shackleCenterX, shackleCenterY),
            style = Stroke(width = 3.5f)
        )
        // Padlock body
        val lockBodyRadius = handleW * 0.65f
        val lockBodyY = shackleCenterY + shackleRadius * 0.9f
        drawCircle(
            color = Color(0xFF212121), // Black body
            radius = lockBodyRadius,
            center = Offset(shackleCenterX, lockBodyY)
        )
        // Outer shiny silver ring on dial bezel
        drawCircle(
            color = Color(0xFFECEFF1),
            radius = lockBodyRadius * 0.7f,
            center = Offset(shackleCenterX, lockBodyY),
            style = Stroke(width = 2f)
        )
        // Small dial knob
        drawCircle(
            color = Color(0xFF9E9E9E),
            radius = lockBodyRadius * 0.25f,
            center = Offset(shackleCenterX, lockBodyY)
        )
        // Tiny clock dial ticks
        for (a in 0 until 8) {
            val rads = Math.toRadians((a * 45).toDouble())
            val tickX = shackleCenterX + cos(rads).toFloat() * lockBodyRadius * 0.5f
            val tickY = lockBodyY + sin(rads).toFloat() * lockBodyRadius * 0.5f
            drawCircle(
                color = Color.White,
                radius = 1.5f,
                center = Offset(tickX, tickY)
            )
        }

        // Outermost outline border
        drawRoundRect(
            color = outlineColor,
            topLeft = Offset(left, top),
            size = Size(rWidth, rHeight),
            style = Stroke(width = 3f),
            cornerRadius = CornerRadius(8f, 8f)
        )
    } else {
        // --- OPEN SCHOOL LOCKER ---
        // Dark interior background of locker interior
        drawRoundRect(
            color = Color(0xFF1E282C), // shadowed inside grey-slate
            topLeft = Offset(left, top),
            size = Size(rWidth, rHeight),
            cornerRadius = CornerRadius(8f, 8f),
            style = Fill
        )
        
        // Inner shelves (two horizontal shelves inside locker)
        val shelfH1 = top + rHeight * 0.32f
        val shelfH2 = top + rHeight * 0.68f
        drawLine(
            color = Color(0xFF37474F),
            start = Offset(left, shelfH1),
            end = Offset(left + rWidth, shelfH1),
            strokeWidth = 3f
        )
        drawLine(
            color = Color(0xFF37474F),
            start = Offset(left, shelfH2),
            end = Offset(left + rWidth, shelfH2),
            strokeWidth = 3f
        )

        // Stored school utilities on top shelf (colored binders, textbooks)
        drawRect(
            color = Color(0xFFD32F2F), // Red Binder
            topLeft = Offset(left + rWidth * 0.15f, shelfH1 - rHeight * 0.2f),
            size = Size(rWidth * 0.18f, rHeight * 0.18f)
        )
        drawRect(
            color = Color(0xFF1E88E5), // Blue Textbook
            topLeft = Offset(left + rWidth * 0.38f, shelfH1 - rHeight * 0.22f),
            size = Size(rWidth * 0.15f, rHeight * 0.2f)
        )

        // Middle shelf: Golden confidence sparkles!
        val starY = shelfH2 - rHeight * 0.18f
        val pulseStarSize = rWidth * 0.15f
        drawSparkleStar(cX, starY, pulseStarSize, Color(0xFFFFD700))
        drawSparkleStar(left + rWidth * 0.25f, shelfH2 - rHeight * 0.28f, pulseStarSize * 0.6f, Color.White)
        drawSparkleStar(left + rWidth * 0.72f, shelfH2 - rHeight * 0.24f, pulseStarSize * 0.7f, Color(0xFF00FFFF))

        // Open Locker Door swung out to the right (perspective trapezoid)
        val doorWidth = rWidth * 0.45f
        val swingXStart = left + rWidth
        val swingXEnd = swingXStart + doorWidth * cos(Math.toRadians(40.0)).toFloat()
        val doorTopY1 = top
        val doorTopY2 = top - rHeight * 0.08f
        val doorBotY1 = top + rHeight
        val doorBotY2 = top + rHeight + rHeight * 0.08f

        val doorPath = Path().apply {
            moveTo(swingXStart, doorTopY1)
            lineTo(swingXEnd, doorTopY2)
            lineTo(swingXEnd, doorBotY2)
            lineTo(swingXStart, doorBotY1)
            close()
        }
        // Swing door color (Steel Blue)
        drawPath(path = doorPath, color = Color(0xFF78909C))
        drawPath(path = doorPath, color = outlineColor, style = Stroke(width = 2.5f))

        // Outermost outline border around the locker cabinet box
        drawRoundRect(
            color = outlineColor,
            topLeft = Offset(left, top),
            size = Size(rWidth, rHeight),
            style = Stroke(width = 3f),
            cornerRadius = CornerRadius(8f, 8f)
        )
    }
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
        MobType.HALLWAY_TRIPPER -> {
            // Draw a student with a tilted snapback cap and an extended shoe trying to trip people
            // Sideways cap
            val capPath = Path().apply {
                moveTo(finalX - baseRadius * 0.8f, finalY - baseRadius * 0.9f)
                lineTo(finalX + baseRadius * 0.4f, finalY - baseRadius * 1.1f)
                lineTo(finalX + baseRadius * 0.9f, finalY - baseRadius * 0.6f)
                lineTo(finalX - baseRadius * 0.5f, finalY - baseRadius * 0.5f)
                close()
            }
            drawPath(capPath, color = Color(0xFFE91E63)) // Hot Pink Cap

            // Face
            drawCircle(color = Color(0xFFFFCC99), radius = baseRadius * 0.65f, center = Offset(finalX, finalY - baseRadius * 0.1f))

            // Sly winking eye and grin
            drawCircle(color = Color(0xFF4CAF50), radius = 6f, center = Offset(finalX - baseRadius * 0.25f, finalY - baseRadius * 0.15f))
            drawLine(color = Color.Black, start = Offset(finalX + baseRadius * 0.15f, finalY - baseRadius * 0.2f), end = Offset(finalX + baseRadius * 0.35f, finalY - baseRadius * 0.1f), strokeWidth = 3f)

            // Smirk
            val smilePath = Path().apply {
                moveTo(finalX - baseRadius * 0.25f, finalY + baseRadius * 0.15f)
                quadraticTo(finalX + baseRadius * 0.1f, finalY + baseRadius * 0.35f, finalX + baseRadius * 0.3f, finalY + baseRadius * 0.2f)
            }
            drawPath(smilePath, color = Color.Black, style = Stroke(width = 3.5f))

            // Tripping Red sneaker sticking out!
            val footX = finalX + baseRadius * 0.6f + (15f * sin(tickTime * 0.08).toFloat())
            val footY = finalY + baseRadius * 0.7f
            drawRoundRect(
                color = Color(0xFFD32F2F), // Red sneaker
                topLeft = Offset(footX, footY),
                size = Size(baseRadius * 0.8f, baseRadius * 0.35f),
                cornerRadius = CornerRadius(8f, 8f)
            )
            // Sneaker white sole
            drawRect(
                color = Color.White,
                topLeft = Offset(footX, footY + baseRadius * 0.25f),
                size = Size(baseRadius * 0.8f, baseRadius * 0.1f)
            )
        }

        MobType.LUNCH_MONEY_THIEF -> {
            // Sneaky dark hooded figure clutching a golden stolen coin
            // Hood cowl
            drawCircle(color = Color(0xFF263238), radius = baseRadius * 0.85f, center = Offset(finalX, finalY))
            // Dark face shadow inner circle
            drawCircle(color = Color(0xFF111111), radius = baseRadius * 0.55f, center = Offset(finalX, finalY))

            // Glowing yellow eyes
            drawCircle(color = Color.Yellow, radius = 7f, center = Offset(finalX - 12f, finalY - 6f))
            drawCircle(color = Color.Yellow, radius = 7f, center = Offset(finalX + 12f, finalY - 6f))

            // Messenger bag belt crossing chest
            drawLine(color = Color(0xFF5D4037), start = Offset(finalX - baseRadius, finalY - baseRadius), end = Offset(finalX + baseRadius, finalY + baseRadius), strokeWidth = 5f)

            // Stolen gold coin sparkles
            val coinPulse = baseRadius * 0.3f
            val coinX = finalX + baseRadius * 0.5f
            val coinY = finalY + baseRadius * 0.4f
            drawCircle(color = Color(0xFFFFD700), radius = coinPulse, center = Offset(coinX, coinY))
            drawLine(color = Color.Black, start = Offset(coinX, coinY - 6f), end = Offset(coinX, coinY + 6f), strokeWidth = 3f)
        }

        MobType.LOCKER_SHOVER -> {
            // Giant broad-shouldered rectangle block representing a locker-slamming bully
            val bColor = Color(0xFFE65100) // Deep orange athletic uniform
            // Shoulders
            drawRect(
                color = bColor,
                topLeft = Offset(finalX - baseRadius * 1.1f, finalY - baseRadius * 0.2f),
                size = Size(baseRadius * 2.2f, baseRadius * 1.1f)
            )
            // Head
            drawCircle(color = Color(0xFFFDD835), radius = baseRadius * 0.45f, center = Offset(finalX, finalY - baseRadius * 0.45f))
            // Tilted school locker on background
            drawRect(
                color = Color(0xFF78909C), // Slate locker
                topLeft = Offset(finalX - baseRadius * 1.3f, finalY - baseRadius * 1.2f),
                size = Size(baseRadius * 0.4f, baseRadius * 1.8f)
            )
            // Locker slits
            drawLine(color = Color.Black, start = Offset(finalX - baseRadius * 1.25f, finalY - baseRadius * 0.9f), end = Offset(finalX - baseRadius * 1.15f, finalY - baseRadius * 0.9f), strokeWidth = 2f)
            drawLine(color = Color.Black, start = Offset(finalX - baseRadius * 1.25f, finalY - baseRadius * 0.8f), end = Offset(finalX - baseRadius * 1.15f, finalY - baseRadius * 0.8f), strokeWidth = 2f)

            // Red angry headband on student
            drawLine(color = Color.Red, start = Offset(finalX - baseRadius * 0.4f, finalY - baseRadius * 0.65f), end = Offset(finalX + baseRadius * 0.4f, finalY - baseRadius * 0.65f), strokeWidth = 5f)
        }

        MobType.CAFETERIA_CUTTER -> {
            // Student holding a food tray with malicious items
            // Head and wild orange hair
            drawCircle(color = Color(0xFFFF9800), radius = baseRadius * 0.65f, center = Offset(finalX, finalY - baseRadius * 0.2f))
            drawCircle(color = Color(0xFFF57C00), radius = baseRadius * 0.35f, center = Offset(finalX, finalY - baseRadius * 0.75f))

            // Round eyes of greedy anticipation
            drawCircle(color = Color.White, radius = 9f, center = Offset(finalX - 14f, finalY - baseRadius * 0.25f))
            drawCircle(color = Color.White, radius = 9f, center = Offset(finalX + 14f, finalY - baseRadius * 0.25f))
            drawCircle(color = Color.Black, radius = 4f, center = Offset(finalX - 14f, finalY - baseRadius * 0.25f))
            drawCircle(color = Color.Black, radius = 4f, center = Offset(finalX + 14f, finalY - baseRadius * 0.25f))

            // Cafeteria tray in front of them
            drawRoundRect(
                color = Color(0xFF90A4AE), // Silver metal tray
                topLeft = Offset(finalX - baseRadius * 0.9f, finalY + baseRadius * 0.2f),
                size = Size(baseRadius * 1.8f, baseRadius * 0.45f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            // Green apple and milk box on the tray!
            drawCircle(color = Color(0xFF4CAF50), radius = baseRadius * 0.15f, center = Offset(finalX - baseRadius * 0.4f, finalY + baseRadius * 0.35f))
            drawRect(color = Color(0xFFE0E0E0), topLeft = Offset(finalX + baseRadius * 0.2f, finalY + baseRadius * 0.25f), size = Size(baseRadius * 0.25f, baseRadius * 0.25f))
        }

        MobType.CHALK_FLINGER -> {
            // Kid drawing slingshot, surrounded by orbiting white chalk pieces
            drawCircle(color = Color(0xFFFFDAB9), radius = baseRadius * 0.65f, center = Offset(finalX, finalY))
            // Blue baseball uniform cap
            val capPath = Path().apply {
                moveTo(finalX - baseRadius * 0.7f, finalY - baseRadius * 0.4f)
                lineTo(finalX, finalY - baseRadius * 0.9f)
                lineTo(finalX + baseRadius * 0.7f, finalY - baseRadius * 0.4f)
                close()
            }
            drawPath(capPath, color = Color(0xFF1E88E5))

            // Slingshot frame held in hand
            drawLine(color = Color(0xFF8D6E63), start = Offset(finalX - baseRadius * 0.4f, finalY + baseRadius * 0.2f), end = Offset(finalX - baseRadius * 0.4f, finalY + baseRadius * 0.7f), strokeWidth = 5f)
            drawLine(color = Color(0xFF8D6E63), start = Offset(finalX - baseRadius * 0.6f, finalY + baseRadius * 0.2f), end = Offset(finalX - baseRadius * 0.2f, finalY + baseRadius * 0.2f), strokeWidth = 5f)

            // Flying chalk shards floating around the head (orbit animation)
            val orbitAngle = (tickTime / 100.0)
            for (j in 0..2) {
                val angleRad = orbitAngle + j * (2 * Math.PI / 3)
                val chalkX = finalX + cos(angleRad).toFloat() * baseRadius * 1.3f
                val chalkY = finalY + sin(angleRad).toFloat() * baseRadius * 1.3f
                drawCircle(color = Color.White, radius = 5f, center = Offset(chalkX, chalkY))
            }
        }

        MobType.CYBER_TAUNTER -> {
            // Screen lighting up on a student’s face casting a glowing green/cyan reflection
            // Back dark shadow hood
            drawCircle(color = Color(0xFF37474F), radius = baseRadius * 0.75f, center = Offset(finalX, finalY - baseRadius * 0.1f))

            // Face
            drawCircle(color = Color(0xFFECEFF1), radius = baseRadius * 0.55f, center = Offset(finalX, finalY - baseRadius * 0.1f))

            // Glowing blue smartphone in foreground
            val phoneY = finalY + baseRadius * 0.4f
            drawRect(
                color = Color.Black,
                topLeft = Offset(finalX - baseRadius * 0.25f, phoneY),
                size = Size(baseRadius * 0.5f, baseRadius * 0.6f)
            )
            // Phone screen neon cyan glow
            drawRect(
                color = Color(0xFF00E676),
                topLeft = Offset(finalX - baseRadius * 0.2f, phoneY + 5f),
                size = Size(baseRadius * 0.4f, baseRadius * 0.45f)
            )

            // Upward neon cyan screen lighting beams casting over face
            val lightPath = Path().apply {
                moveTo(finalX - baseRadius * 0.2f, phoneY)
                lineTo(finalX + baseRadius * 0.2f, phoneY)
                lineTo(finalX + baseRadius * 0.45f, finalY - 10f)
                lineTo(finalX - baseRadius * 0.45f, finalY - 10f)
                close()
            }
            drawPath(lightPath, color = Color(0xFF00E676).copy(alpha = 0.22f))
        }

        MobType.DESK_SLAMMER -> {
            // Angry brawler with a physical miniature desk in front of them that they slam
            drawCircle(color = Color(0xFFD84315), radius = baseRadius * 0.8f, center = Offset(finalX, finalY)) // Orange-red anger face
            // Screaming mouth
            drawCircle(color = Color.Black, radius = baseRadius * 0.22f, center = Offset(finalX, finalY + baseRadius * 0.2f))

            // Furious eyes
            drawLine(color = Color.White, start = Offset(finalX - 22f, finalY - 15f), end = Offset(finalX - 6f, finalY - 5f), strokeWidth = 5f)
            drawLine(color = Color.White, start = Offset(finalX + 22f, finalY - 15f), end = Offset(finalX + 6f, finalY - 5f), strokeWidth = 5f)

            // Desk in front
            drawRect(
                color = Color(0xFF5D4037), // wood desk surface
                topLeft = Offset(finalX - baseRadius * 1.1f, finalY + baseRadius * 0.3f),
                size = Size(baseRadius * 2.2f, baseRadius * 0.5f)
            )
            // Desk legs
            drawLine(color = Color.Black, start = Offset(finalX - baseRadius * 0.9f, finalY + baseRadius * 0.8f), end = Offset(finalX - baseRadius * 0.9f, finalY + baseRadius * 1.3f), strokeWidth = 6f)
            drawLine(color = Color.Black, start = Offset(finalX + baseRadius * 0.9f, finalY + baseRadius * 0.8f), end = Offset(finalX + baseRadius * 0.9f, finalY + baseRadius * 1.3f), strokeWidth = 6f)
        }

        MobType.GYM_CLASS_TYRANT -> {
            // Muscle jersey wearing student holding a shiny giant pulsating red dodgeball
            drawCircle(color = Color(0xFFFFA726), radius = baseRadius * 0.72f, center = Offset(finalX, finalY))
            // Red Sports headband
            drawRect(
                color = Color.Red,
                topLeft = Offset(finalX - baseRadius * 0.65f, finalY - baseRadius * 0.55f),
                size = Size(baseRadius * 1.3f, baseRadius * 0.2f)
            )

            // Glowing red angry eyes
            drawCircle(color = Color.Red, radius = 5f, center = Offset(finalX - 14f, finalY - 10f))
            drawCircle(color = Color.Red, radius = 5f, center = Offset(finalX + 14f, finalY - 10f))

            // Beautiful pulsating high-velocity red dodgeball
            val ballSize = baseRadius * 0.45f + (8f * sin(tickTime * 0.1).toFloat())
            val ballX = finalX + baseRadius * 0.8f
            val ballY = finalY + baseRadius * 0.3f
            drawCircle(color = Color(0xFFC62828), radius = ballSize, center = Offset(ballX, ballY))
            // Cross lines on dodgeball texture
            drawLine(color = Color.White.copy(alpha = 0.5f), start = Offset(ballX - ballSize, ballY), end = Offset(ballX + ballSize, ballY), strokeWidth = 3f)
            drawLine(color = Color.White.copy(alpha = 0.5f), start = Offset(ballX, ballY - ballSize), end = Offset(ballX, ballY + ballSize), strokeWidth = 3f)
        }

        MobType.CORRIDOR_BLOCKER -> {
            // Giant brick wall guard standing broad with crossed arms
            // Large gray jacket
            drawRect(
                color = Color(0xFF455A64),
                topLeft = Offset(finalX - baseRadius * 1.2f, finalY - baseRadius * 0.2f),
                size = Size(baseRadius * 2.4f, baseRadius * 1.3f)
            )
            // Big head with cap
            drawCircle(color = Color(0xFFFFDAB9), radius = baseRadius * 0.5f, center = Offset(finalX, finalY - baseRadius * 0.4f))
            drawRect(color = Color(0xFF263238), topLeft = Offset(finalX - baseRadius * 0.45f, finalY - baseRadius * 0.9f), size = Size(baseRadius * 0.9f, baseRadius * 0.3f))

            // Yellow warning stripes across chest (crossed thick arms)
            val armColor = Color(0xFFCFD8DC)
            drawRoundRect(
                color = armColor,
                topLeft = Offset(finalX - baseRadius * 0.8f, finalY + baseRadius * 0.1f),
                size = Size(baseRadius * 1.6f, baseRadius * 0.35f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawLine(color = Color.Black, start = Offset(finalX - baseRadius * 0.8f, finalY + baseRadius * 0.25f), end = Offset(finalX + baseRadius * 0.8f, finalY + baseRadius * 0.25f), strokeWidth = 3f)
        }

        MobType.JUICE_STALKER -> {
            // A cute but evil green juice carton with teeth and a plastic drinking straw horn
            // Green Box body
            drawRect(
                color = Color(0xFF81C784), // Light Green Juice Carton
                topLeft = Offset(finalX - baseRadius * 0.7f, finalY - baseRadius * 0.9f),
                size = Size(baseRadius * 1.4f, baseRadius * 1.7f)
            )
            // Orange splash design on carton
            drawCircle(color = Color(0xFFFFB74D), radius = baseRadius * 0.35f, center = Offset(finalX, finalY + baseRadius * 0.1f))

            // Drinking straw horn sticking out of top-corner
            val strawPath = Path().apply {
                moveTo(finalX - baseRadius * 0.4f, finalY - baseRadius * 0.9f)
                lineTo(finalX - baseRadius * 0.4f, finalY - baseRadius * 1.3f)
                lineTo(finalX - baseRadius * 0.7f, finalY - baseRadius * 1.5f)
            }
            drawPath(strawPath, color = Color.White, style = Stroke(width = 8f))
            drawPath(strawPath, color = Color.Red, style = Stroke(width = 8f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f,5f), 0f)))

            // Angry face on juice box
            drawCircle(color = Color.Red, radius = 5f, center = Offset(finalX - 12f, finalY - baseRadius * 0.3f))
            drawCircle(color = Color.Red, radius = 5f, center = Offset(finalX + 12f, finalY - baseRadius * 0.3f))
            // Jagged teeth smile
            drawLine(color = Color.Black, start = Offset(finalX - baseRadius * 0.3f, finalY - baseRadius * 0.1f), end = Offset(finalX + baseRadius * 0.3f, finalY - baseRadius * 0.1f), strokeWidth = 3f)
        }

        MobType.CHAD_THE_OVERLORD -> {
            // The ultimate school brawler Chad! Massive crimson varsity jacket, golden crown, gold dollar chain and glowing double row grin
            // Giant red/golden varsity jacket
            drawCircle(color = Color(0xFFFF1744), radius = baseRadius * 1.2f, center = Offset(finalX, finalY + baseRadius * 0.2f))

            // Royal golden letters/borders on jacket
            drawCircle(color = Color(0xFFFFD700), radius = baseRadius * 1.2f, center = Offset(finalX, finalY + baseRadius * 0.2f), style = Stroke(width = 6f))

            // Head and gold crown
            drawCircle(color = Color(0xFFFFCC99), radius = baseRadius * 0.65f, center = Offset(finalX, finalY - baseRadius * 0.25f))

            // Royal Golden Crown on top of head!
            val crownPath = Path().apply {
                moveTo(finalX - baseRadius * 0.5f, finalY - baseRadius * 0.8f) // left
                lineTo(finalX - baseRadius * 0.4f, finalY - baseRadius * 1.2f) // left spike
                lineTo(finalX - baseRadius * 0.2f, finalY - baseRadius * 0.9f) // vale L
                lineTo(finalX, finalY - baseRadius * 1.4f)                     // central peak
                lineTo(finalX + baseRadius * 0.2f, finalY - baseRadius * 0.9f) // vale R
                lineTo(finalX + baseRadius * 0.4f, finalY - baseRadius * 1.2f) // right spike
                lineTo(finalX + baseRadius * 0.5f, finalY - baseRadius * 0.8f) // right
                close()
            }
            drawPath(crownPath, color = Color(0xFFFFD700))

            // Glowing blue sunglasses
            drawRoundRect(
                color = Color(0xFF00E5FF), // Royal cyan mirror glasses
                topLeft = Offset(finalX - baseRadius * 0.45f, finalY - baseRadius * 0.38f),
                size = Size(baseRadius * 0.9f, baseRadius * 0.22f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawLine(color = Color.Black, start = Offset(finalX - baseRadius * 0.45f, finalY - baseRadius * 0.27f), end = Offset(finalX + baseRadius * 0.45f, finalY - baseRadius * 0.27f), strokeWidth = 3f)

            // Heavy golden chain medalion under chin!
            val chainY = finalY + baseRadius * 0.35f
            for (offset in -2..2) {
                drawCircle(color = Color(0xFFFFD700), radius = 8f, center = Offset(finalX + offset * 14f, chainY), style = Stroke(width = 2.5f))
            }
            drawCircle(color = Color(0xFFFFD700), radius = 15f, center = Offset(finalX, chainY + 12f))

            // Double row of white grin teeth
            val mouthWidth = baseRadius * 0.8f
            val mouthHeight = baseRadius * 0.25f
            val mouthY = finalY
            drawRect(color = Color.Black, topLeft = Offset(finalX - mouthWidth / 2, mouthY - mouthHeight / 2), size = Size(mouthWidth, mouthHeight))
            for (j in 0..5) {
                val tX = finalX - mouthWidth / 2 + j * (mouthWidth / 5.5f)
                drawLine(color = Color.White, start = Offset(tX, mouthY - mouthHeight / 2 + 2f), end = Offset(tX, mouthY + mouthHeight / 2 - 2f), strokeWidth = 4f)
            }
        }
    }
}

// Custom 3D perspective wall brick texturing helper
private fun DrawScope.drawFrontWallBricks(
    xL: Float,
    xR: Float,
    yT: Float,
    yB: Float,
    color: Color
) {
    val rows = 5
    val cols = 5
    val h = yB - yT
    val w = xR - xL
    
    // Draw horizontal grout lines
    for (i in 1 until rows) {
        val y = yT + (h * i / rows)
        drawLine(
            color = color,
            start = Offset(xL, y),
            end = Offset(xR, y),
            strokeWidth = 2f
        )
    }
    
    // Draw vertical grout joints in running bond pattern
    for (i in 0 until rows) {
        val y1 = yT + (h * i / rows)
        val y2 = yT + (h * (i + 1) / rows)
        val shift = if (i % 2 == 0) 0f else 0.5f
        for (j in 0..cols) {
            val fraction = (j.toFloat() - 0.5f + shift) / cols
            if (fraction in 0.01f..0.99f) {
                val x = xL + (w * fraction)
                drawLine(
                    color = color,
                    start = Offset(x, y1),
                    end = Offset(x, y2),
                    strokeWidth = 1.8f
                )
            }
            
            val cellFractionStart = (j.toFloat() - 1f + shift).coerceAtLeast(0f) / cols
            val cellFractionEnd = (j.toFloat() + shift).coerceAtMost(cols.toFloat()) / cols
            val cellCenterX = xL + w * (cellFractionStart + cellFractionEnd) / 2
            val cellCenterY = (y1 + y2) / 2
            
            if ((i + j) % 3 == 0) {
                drawCircle(
                    color = color.copy(alpha = 0.25f),
                    radius = ((y2 - y1) * 0.12f).coerceAtMost(6f),
                    center = Offset(cellCenterX, cellCenterY)
                )
            }
        }
    }
    
    // Draw a dark school wood/rubber baseboard at the bottom of front wall
    val baseboardHeight = (yB - yT) * 0.08f
    drawRect(
        color = Color(0xFF3E2723), // Dark brown school hallway baseboard
        topLeft = Offset(xL, yB - baseboardHeight),
        size = Size(w, baseboardHeight)
    )
    drawLine(
        color = Color.Black,
        start = Offset(xL, yB - baseboardHeight),
        end = Offset(xR, yB - baseboardHeight),
        strokeWidth = 1.5f
    )
}

// Custom 3D perspective side wall brick texturing helper
private fun DrawScope.drawPerspectiveWallBricks(
    x1: Float,
    yTop1: Float,
    yBot1: Float,
    x2: Float,
    yTop2: Float,
    yBot2: Float,
    color: Color
) {
    val rows = 5
    // Perspective horizontal curves (straight line rows in 3D projection)
    for (i in 1 until rows) {
        val t = i.toFloat() / rows
        val yStart = yTop1 + (yBot1 - yTop1) * t
        val yEnd = yTop2 + (yBot2 - yTop2) * t
        drawLine(
            color = color,
            start = Offset(x1, yStart),
            end = Offset(x2, yEnd),
            strokeWidth = 2f
        )
    }
    
    // Perspective vertical running-bond joints
    val cols = 4
    for (i in 0 until rows) {
        val t1 = i.toFloat() / rows
        val t2 = (i + 1).toFloat() / rows
        val shift = if (i % 2 == 0) 0f else 0.5f
        for (j in 1..cols) {
            val u = (j.toFloat() - 0.5f + shift) / (cols + 0.5f)
            if (u in 0.01f..0.99f) {
                val xVal = x1 + (x2 - x1) * u
                val yTopVal = yTop1 + (yTop2 - yTop1) * u
                val yBotVal = yBot1 + (yBot2 - yBot1) * u
                
                val yStart = yTopVal + (yBotVal - yTopVal) * t1
                val yEnd = yTopVal + (yBotVal - yTopVal) * t2
                
                drawLine(
                    color = color,
                    start = Offset(xVal, yStart),
                    end = Offset(xVal, yEnd),
                    strokeWidth = 1.8f
                )

                if ((i + j) % 3 == 1) {
                    val midY = (yStart + yEnd) / 2
                    drawCircle(
                        color = color.copy(alpha = 0.22f),
                        radius = 3f,
                        center = Offset(xVal, midY)
                    )
                }
            }
        }
    }

    // Perspective baseboard at the bottom of side wall
    val bbT = 0.08f // baseboard height percentage
    val yBB1 = yBot1 - (yBot1 - yTop1) * bbT
    val yBB2 = yBot2 - (yBot2 - yTop2) * bbT
    val baseboardPath = Path().apply {
        moveTo(x1, yBot1)
        lineTo(x2, yBot2)
        lineTo(x2, yBB2)
        lineTo(x1, yBB1)
        close()
    }
    drawPath(
        path = baseboardPath,
        color = Color(0xFF3E2723) // Rich baseboard brown
    )
    drawLine(
        color = Color.Black,
        start = Offset(x1, yBB1),
        end = Offset(x2, yBB2),
        strokeWidth = 1.5f
    )
}

// Custom 3D perspective floor/ceiling pavement tile helper
private fun DrawScope.drawFloorCeilingTiles(
    xL1: Float, xR1: Float, y1: Float,
    xL2: Float, xR2: Float, y2: Float,
    color: Color
) {
    val rows = 6
    for (i in 1 until rows) {
        val t = i.toFloat() / rows
        val yVal = y1 + (y2 - y1) * t
        val xLVal = xL1 + (xL2 - xL1) * t
        val xRVal = xR1 + (xR2 - xR1) * t
        
        drawLine(
            color = color,
            start = Offset(xLVal, yVal),
            end = Offset(xRVal, yVal),
            strokeWidth = 2f
        )
    }
    
    val cols = 5
    for (j in 1 until cols) {
        val u = j.toFloat() / cols
        val startX = xL1 + (xR1 - xL1) * u
        val endX = xL2 + (xR2 - xL2) * u
        
        drawLine(
            color = color,
            start = Offset(startX, y1),
            end = Offset(endX, y2),
            strokeWidth = 2f
        )
    }

    val dotsCount = 18
    for (idx in 0 until dotsCount) {
        val t = (idx * 0.05f) + 0.05f
        val u = ((idx * 17) % 100) / 100f
        
        val yScatter = y1 + (y2 - y1) * t
        val xLVal = xL1 + (xL2 - xL1) * t
        val xRVal = xR1 + (xR2 - xR1) * t
        val xScatter = xLVal + (xRVal - xLVal) * u
        
        val dotColor = when (idx % 3) {
            0 -> Color(0xFFB0BEC5).copy(alpha = 0.4f)
            1 -> Color(0xFFFFD54F).copy(alpha = 0.35f)
            else -> Color.White.copy(alpha = 0.5f)
        }
        drawCircle(
            color = dotColor,
            radius = (1.5f + (idx % 3).toFloat()).coerceAtMost(4f),
            center = Offset(xScatter, yScatter)
        )
    }
}
