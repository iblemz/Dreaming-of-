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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.runtime.remember
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

    val context = androidx.compose.ui.platform.LocalContext.current
    val resources = context.resources
    val packageName = context.packageName

    // Preload textures dynamically if they ever exist in standard assets or resource drawables
    val wallTextures = remember {
        fun loadTexture(name: String): androidx.compose.ui.graphics.ImageBitmap? {
            // 1. Try to load directly from raw Android assets (app/src/main/assets) with clean formats
            val assetManager = context.assets
            val extensions = listOf("png", "jpg", "jpeg", "webp")
            for (ext in extensions) {
                try {
                    val stream = assetManager.open("$name.$ext")
                    val bitmap = android.graphics.BitmapFactory.decodeStream(stream)
                    stream.close()
                    if (bitmap != null) {
                        return bitmap.asImageBitmap()
                    }
                } catch (e: Exception) {
                    // Fall through to try next extension
                }
            }

            // 2. Try to fall back to drawable resources if assets don't exist
            val resId = resources.getIdentifier(name, "drawable", packageName)
            if (resId == 0) return null
            return try {
                val bitmap = android.graphics.BitmapFactory.decodeResource(resources, resId)
                if (bitmap != null) {
                    bitmap.asImageBitmap()
                } else null
            } catch (e: Exception) {
                null
            }
        }

        mapOf(
            "lockers" to loadTexture("wall_lockers"),
            "bulletin" to loadTexture("wall_bulletin"),
            "tiles" to loadTexture("wall_tiles"),
            "cracked" to loadTexture("wall_cracked"),
            "water" to loadTexture("wall_water"),
            "graffiti" to loadTexture("wall_graffiti")
        )
    }

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
                        color = shadedOutline.copy(alpha = 0.4f),
                        cx = cx,
                        cy = cy,
                        shade = shade,
                        textures = wallTextures
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
                            color = shadedOutline.copy(alpha = 0.4f),
                            seedX = lx, seedY = ly,
                            shade = shade,
                            textures = wallTextures
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
                            color = shadedOutline.copy(alpha = 0.4f),
                            seedX = rx, seedY = ry,
                            shade = shade,
                            textures = wallTextures
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
    color: Color,
    cx: Int,
    cy: Int,
    shade: Float,
    textures: Map<String, androidx.compose.ui.graphics.ImageBitmap?>
) {
    drawProceduralSchoolWallPanel(
        x1 = xL, yTop1 = yT, yBot1 = yB,
        x2 = xR, yTop2 = yT, yBot2 = yB,
        color = color,
        seedX = cx,
        seedY = cy,
        shade = shade,
        textures = textures
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
    color: Color,
    seedX: Int,
    seedY: Int,
    shade: Float,
    textures: Map<String, androidx.compose.ui.graphics.ImageBitmap?>
) {
    drawProceduralSchoolWallPanel(
        x1 = x1, yTop1 = yTop1, yBot1 = yBot1,
        x2 = x2, yTop2 = yTop2, yBot2 = yBot2,
        color = color,
        seedX = seedX,
        seedY = seedY,
        shade = shade,
        textures = textures
    )
}

// Unified first-person procedural school corridor wall decorator
private fun DrawScope.drawProceduralSchoolWallPanel(
    x1: Float, yTop1: Float, yBot1: Float,
    x2: Float, yTop2: Float, yBot2: Float,
    color: Color, // shadedOutline color
    seedX: Int,
    seedY: Int,
    shade: Float,
    textures: Map<String, androidx.compose.ui.graphics.ImageBitmap?>
) {
    val h1 = yBot1 - yTop1
    val h2 = yBot2 - yTop2
    if (h1 < 2f || h2 < 2f) return

    val panelSeed = (seedX * 104729) xor (seedY * 224737)
    val random = java.util.Random(panelSeed.toLong())

    // helper projection function
    fun getPt(u: Float, v: Float): Offset {
        val px = x1 + (x2 - x1) * u
        val yT = yTop1 + (yTop2 - yTop1) * u
        val yB = yBot1 + (yBot2 - yBot1) * u
        return Offset(px, yT + (yB - yT) * v)
    }

    // Shading multiplier helper to shadow decorative assets
    fun shadeCol(base: Color): Color {
        return lerpColor(Color.Black, base, shade)
    }

    // Determine school theme
    // .05 lockers (5%)
    // .05 bulletin board (5%)
    // .10 dirty tiles (10%)
    // .50 cracked plaster images (cracked/peeling plaster) (50%)
    // Remaining .30 standard brick panels
    val rollStyle = random.nextFloat()
    val theme = when {
        rollStyle < 0.05f -> "lockers"
        rollStyle < 0.10f -> "bulletin"
        rollStyle < 0.20f -> "tiles"
        rollStyle < 0.70f -> "cracked"
        else -> "standard"
    }

    val hasWaterDamage = random.nextFloat() < 0.20f // .10 & .10 mentioned twice -> 20%
    val hasGraffiti = random.nextFloat() < 0.13f // .10 & .03 mentioned -> 13%

    // Base background wall fill has already been colored by drawPolygon, but we can draw custom patterns on top!
    var customTextureDrawn = false
    val customBitmap = textures[theme]
    if (customBitmap != null) {
        drawIntoCanvas { canvas ->
            try {
                val nativeCanvas = canvas.nativeCanvas
                nativeCanvas.save()
                
                val bmpW = customBitmap.width.toFloat()
                val bmpH = customBitmap.height.toFloat()
                val srcPts = floatArrayOf(
                    0f, 0f,
                    bmpW, 0f,
                    bmpW, bmpH,
                    0f, bmpH
                )
                
                val p1 = getPt(0f, 0.08f)
                val p2 = getPt(1f, 0.08f)
                val p3 = getPt(1f, 0.92f)
                val p4 = getPt(0f, 0.92f)
                val dstPts = floatArrayOf(
                    p1.x, p1.y,
                    p2.x, p2.y,
                    p3.x, p3.y,
                    p4.x, p4.y
                )
                
                val matrix = android.graphics.Matrix()
                matrix.setPolyToPoly(srcPts, 0, dstPts, 0, 4)
                nativeCanvas.concat(matrix)
                
                val nativeBmp = customBitmap.asAndroidBitmap()
                val paint = android.graphics.Paint().apply {
                    isFilterBitmap = true
                    isAntiAlias = true
                    // Light shade: darken wall based on depth (shade: 0..1)
                    val alphaShadow = ((1f - shade) * 255).toInt().coerceIn(0, 255)
                    colorFilter = android.graphics.PorterDuffColorFilter(
                        android.graphics.Color.argb(alphaShadow, 0, 0, 0),
                        android.graphics.PorterDuff.Mode.SRC_ATOP
                    )
                }
                
                nativeCanvas.drawBitmap(nativeBmp, 0f, 0f, paint)
                nativeCanvas.restore()
                customTextureDrawn = true
            } catch (e: Exception) {
                // Return false to fallback
            }
        }
    }

    if (!customTextureDrawn) {
        when (theme) {
        "lockers" -> {
            // Draw adjacent steel lockers
            val doorCount = 3
            for (col in 0 until doorCount) {
                val dL = 0.05f + col * 0.31f
                val dR = dL + 0.28f
                
                // Draw locker door background filling
                val path = Path().apply {
                    val pTL = getPt(dL, 0.08f)
                    val pTR = getPt(dR, 0.08f)
                    val pBR = getPt(dR, 0.92f)
                    val pBL = getPt(dL, 0.92f)
                    moveTo(pTL.x, pTL.y)
                    lineTo(pTR.x, pTR.y)
                    lineTo(pBR.x, pBR.y)
                    lineTo(pBL.x, pBL.y)
                    close()
                }
                // Distressed school locker slate grey/blue or dark olive/green
                val lockerBase = if (col % 2 == 0) Color(0xFF455A64) else Color(0xFF37474F)
                drawPath(path = path, color = shadeCol(lockerBase))
                
                // Draw individual locker door outline
                val points = listOf(getPt(dL, 0.08f), getPt(dR, 0.08f), getPt(dR, 0.92f), getPt(dL, 0.92f))
                for (idx in 0 until 4) {
                    val pA = points[idx]
                    val pB = points[(idx + 1) % 4]
                    drawLine(color = Color.Black.copy(alpha = 0.8f), start = pA, end = pB, strokeWidth = 1.5f)
                }

                // Vertical locker door center crease line
                drawLine(
                    color = Color.Black.copy(alpha = 0.3f),
                    start = getPt((dL + dR) / 2f, 0.08f),
                    end = getPt((dL + dR) / 2f, 0.92f),
                    strokeWidth = 1f
                )

                // Top Louvers
                val midU = (dL + dR) / 2f
                val lW = 0.08f
                for (l in 0..3) {
                    val lvY = 0.14f + l * 0.03f
                    drawLine(
                        color = Color.Black.copy(alpha = 0.85f),
                        start = getPt(midU - lW, lvY),
                        end = getPt(midU + lW, lvY),
                        strokeWidth = 1.6f
                    )
                }

                // Bottom Louvers
                for (l in 0..3) {
                    val lvY = 0.76f + l * 0.03f
                    drawLine(
                        color = Color.Black.copy(alpha = 0.85f),
                        start = getPt(midU - lW, lvY),
                        end = getPt(midU + lW, lvY),
                        strokeWidth = 1.6f
                    )
                }

                // Locker lock pocket handle well
                val hL = dL + 0.04f
                val hR = dL + 0.09f
                val hT = 0.45f
                val hB = 0.53f
                val handlePath = Path().apply {
                    moveTo(getPt(hL, hT).x, getPt(hL, hT).y)
                    lineTo(getPt(hR, hT).x, getPt(hR, hT).y)
                    lineTo(getPt(hR, hB).x, getPt(hR, hB).y)
                    lineTo(getPt(hL, hB).x, getPt(hL, hB).y)
                    close()
                }
                drawPath(path = handlePath, color = shadeCol(Color(0xFF212121))) // Black recess

                // Tiny silver lever
                drawLine(
                    color = shadeCol(Color(0xFFB0BEC5)),
                    start = getPt(hL + 0.015f, hT + 0.02f),
                    end = getPt(hL + 0.015f, hB - 0.02f),
                    strokeWidth = 2.5f
                )

                // Hanging combination lock body
                val shackleCX = (hL + hR)/2f
                val lockCY = hB + 0.03f
                // Padlock ring
                drawCircle(
                    color = shadeCol(Color(0xFFD4AF37)), // Brass
                    radius = (h1 * 0.012f).coerceAtLeast(1.5f),
                    center = getPt(shackleCX, hB + 0.005f),
                    style = Stroke(width = 1.2f)
                )
                // Black dial lock body
                drawCircle(
                    color = shadeCol(Color(0xFF1A1A1A)),
                    radius = (h1 * 0.018f).coerceAtLeast(2.5f),
                    center = getPt(shackleCX, lockCY)
                )
            }
        }
        "bulletin" -> {
            // Draw a cork board with papers pinned onto it
            val bL = 0.15f; val bR = 0.85f
            val bT = 0.18f; val bB = 0.68f

            // Corkboard back fill
            val corkPath = Path().apply {
                moveTo(getPt(bL, bT).x, getPt(bL, bT).y)
                lineTo(getPt(bR, bT).x, getPt(bR, bT).y)
                lineTo(getPt(bR, bB).x, getPt(bR, bB).y)
                lineTo(getPt(bL, bB).x, getPt(bL, bB).y)
                close()
            }
            drawPath(path = corkPath, color = shadeCol(Color(0xFF8D6E63))) // Cork board brown

            // Scattered tiny cork grain spec dots
            for (g in 0..12) {
                val gU = bL + random.nextFloat() * (bR - bL)
                val gV = bT + random.nextFloat() * (bB - bT)
                drawCircle(
                    color = Color.Black.copy(alpha = 0.22f),
                    radius = 1.2f,
                    center = getPt(gU, gV)
                )
            }

            // High-contrast corkboard woody outer border frame
            val pts = listOf(getPt(bL, bT), getPt(bR, bT), getPt(bR, bB), getPt(bL, bB))
            for (idx in 0 until 4) {
                drawLine(
                    color = shadeCol(Color(0xFF3E2723)), // Wood brown
                    start = pts[idx],
                    end = pts[(idx + 1) % 4],
                    strokeWidth = 4f
                )
            }

            // Draw loose pinned papers
            // Paper 1
            val p1Corners = listOf(
                getPt(0.22f, 0.28f), getPt(0.38f, 0.30f),
                getPt(0.36f, 0.52f), getPt(0.20f, 0.48f)
            )
            val p1Path = Path().apply {
                moveTo(p1Corners[0].x, p1Corners[0].y)
                p1Corners.forEach { lineTo(it.x, it.y) }
                close()
            }
            drawPath(path = p1Path, color = shadeCol(Color(0xFFECEFF1))) // White ivory sheet
            p1Corners.indices.forEach { idx ->
                drawLine(color = Color.Black.copy(alpha = 0.25f), start = p1Corners[idx], end = p1Corners[(idx + 1) % 4], strokeWidth = 1f)
            }
            // Scribble math equations on paper 1
            for (sc in 0..2) {
                val sY = 0.34f + sc * 0.05f
                drawLine(
                    color = Color.Black.copy(alpha = 0.35f),
                    start = getPt(0.24f, sY),
                    end = getPt(0.34f, sY + 0.01f),
                    strokeWidth = 1f
                )
            }

            // Paper 2
            val p2Corners = listOf(
                getPt(0.44f, 0.35f), getPt(0.56f, 0.32f),
                getPt(0.58f, 0.54f), getPt(0.46f, 0.57f)
            )
            val p2Path = Path().apply {
                moveTo(p2Corners[0].x, p2Corners[0].y)
                p2Corners.forEach { lineTo(it.x, it.y) }
                close()
            }
            drawPath(path = p2Path, color = shadeCol(Color(0xFFFFF9C4))) // Soft yellow notebook note
            p2Corners.indices.forEach { idx ->
                drawLine(color = Color.Black.copy(alpha = 0.25f), start = p2Corners[idx], end = p2Corners[(idx + 1) % 4], strokeWidth = 1f)
            }

            // Pinned pushpins red/blue
            drawCircle(color = shadeCol(Color(0xFFD32F2F)), radius = 2.5f, center = getPt(0.30f, 0.29f)) // red pushpin on Paper 1
            drawCircle(color = shadeCol(Color(0xFF1976D2)), radius = 2.5f, center = getPt(0.50f, 0.34f)) // blue pushpin on Paper 2
        }
        "tiles" -> {
            // Institutional dirty corridor wall tiling
            val rows = 8
            val cols = 8
            
            // Fill background
            val tileBackground = Path().apply {
                moveTo(getPt(0f, 0.08f).x, getPt(0f, 0.08f).y)
                lineTo(getPt(1f, 0.08f).x, getPt(1f, 0.08f).y)
                lineTo(getPt(1f, 0.92f).x, getPt(1f, 0.92f).y)
                lineTo(getPt(0f, 0.92f).x, getPt(0f, 0.92f).y)
                close()
            }
            drawPath(path = tileBackground, color = shadeCol(Color(0xFF78909C)))

            // Grimy grout lines
            for (i in 1..rows) {
                val v = 0.08f + (0.84f * i / rows)
                drawLine(
                    color = Color.Black.copy(alpha = 0.55f),
                    start = getPt(0f, v),
                    end = getPt(1f, v),
                    strokeWidth = 1.5f
                )
            }
            for (j in 1 until cols) {
                val u = j.toFloat() / cols
                drawLine(
                    color = Color.Black.copy(alpha = 0.55f),
                    start = getPt(u, 0.08f),
                    end = getPt(u, 0.92f),
                    strokeWidth = 1.5f
                )
            }

            // Grout stains and mold drips
            for (st in 0..5) {
                val uStain = (st + 1) * 0.15f
                val vStain = 0.25f + (st * 11 % 100) / 250f
                drawLine(
                    color = Color(0x66263238),
                    start = getPt(uStain, vStain),
                    end = getPt(uStain, vStain + 0.18f),
                    strokeWidth = 2.5f
                )
            }
        }
        "cracked" -> {
            // Draw peeling plaster revealing background bricks + jagged masonry cracks!
            drawSchoolGridBricks(color.copy(alpha = 0.25f), h1, h2, ::getPt, random)

            // Let's draw 2 large chipped/peeled plaster holes revealing dark raw brick masonry backing
            val peeledChippings = listOf(
                // Shape 1 (Left hole)
                listOf(getPt(0.12f, 0.18f), getPt(0.35f, 0.15f), getPt(0.38f, 0.38f), getPt(0.15f, 0.42f)),
                // Shape 2 (Right hole)
                listOf(getPt(0.58f, 0.24f), getPt(0.85f, 0.20f), getPt(0.80f, 0.54f), getPt(0.60f, 0.48f))
            )

            for (chip in peeledChippings) {
                val path = Path().apply {
                    moveTo(chip[0].x, chip[0].y)
                    chip.forEach { lineTo(it.x, it.y) }
                    close()
                }
                // Fill behind chipped plaster with dark charcoal raw stone color
                drawPath(path = path, color = shadeCol(Color(0xFF263238)))
                
                // Draw plaster crumbling bezel border
                for (idx in chip.indices) {
                    drawLine(
                        color = shadeCol(Color(0xFFECEFF1)).copy(alpha = 0.75f),
                        start = chip[idx],
                        end = chip[(idx + 1) % chip.size],
                        strokeWidth = 1.8f
                    )
                }
            }

            // Jagged, branching line cracks running down the wall face
            // Crack 1
            drawLine(shadeCol(Color.Black), getPt(0.38f, 0.38f), getPt(0.44f, 0.58f), 1.5f)
            drawLine(shadeCol(Color.Black), getPt(0.44f, 0.58f), getPt(0.40f, 0.74f), 1.2f)
            
            // Crack 2
            drawLine(shadeCol(Color.Black), getPt(0.60f, 0.48f), getPt(0.52f, 0.65f), 1.5f)
            drawLine(shadeCol(Color.Black), getPt(0.52f, 0.65f), getPt(0.58f, 0.82f), 1.2f)
        }
        else -> {
            drawSchoolGridBricks(color.copy(alpha = 0.32f), h1, h2, ::getPt, random)
        }
    }
    }

    // --- OVERLAY 1: WATER DAMAGE (Leaking stains, 20% total probability) ---
    if (hasWaterDamage) {
        val waterBmp = textures["water"]
        var waterDrawn = false
        if (waterBmp != null) {
            drawIntoCanvas { canvas ->
                try {
                    val nativeCanvas = canvas.nativeCanvas
                    nativeCanvas.save()
                    val bmpW = waterBmp.width.toFloat()
                    val bmpH = waterBmp.height.toFloat()
                    val srcPts = floatArrayOf(0f, 0f, bmpW, 0f, bmpW, bmpH, 0f, bmpH)
                    val p1 = getPt(0f, 0.08f)
                    val p2 = getPt(1f, 0.08f)
                    val p3 = getPt(1f, 0.92f)
                    val p4 = getPt(0f, 0.92f)
                    val dstPts = floatArrayOf(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y, p4.x, p4.y)
                    val matrix = android.graphics.Matrix()
                    matrix.setPolyToPoly(srcPts, 0, dstPts, 0, 4)
                    nativeCanvas.concat(matrix)
                    val nativeBmp = waterBmp.asAndroidBitmap()
                    val paint = android.graphics.Paint().apply {
                        isFilterBitmap = true
                        isAntiAlias = true
                        val alphaShadow = ((1f - shade) * 255).toInt().coerceIn(0, 255)
                        colorFilter = android.graphics.PorterDuffColorFilter(
                            android.graphics.Color.argb(alphaShadow, 0, 0, 0),
                            android.graphics.PorterDuff.Mode.SRC_ATOP
                        )
                    }
                    nativeCanvas.drawBitmap(nativeBmp, 0f, 0f, paint)
                    nativeCanvas.restore()
                    waterDrawn = true
                } catch (e: Exception) {}
            }
        }
        if (!waterDrawn) {
            val leaksCount = 4 + random.nextInt(4)
            for (i in 0 until leaksCount) {
                val uLeak = 0.1f + random.nextFloat() * 0.8f
                val maxV = 0.2f + random.nextFloat() * 0.5f
                
                var curU = uLeak
                var curV = 0.08f
                while (curV < maxV) {
                    val nextV = curV + 0.06f
                    val nextU = curU + (random.nextFloat() - 0.5f) * 0.02f
                    
                    drawLine(
                        color = Color(0x772D1E12), // Dark dirty water staining brown
                        start = getPt(curU, curV),
                        end = getPt(nextU, nextV),
                        strokeWidth = 2.5f
                    )
                    curU = nextU
                    curV = nextV
                }
            }

            // Wet dripping ceiling smudge across the top wall joint
            val waterSmudge = Path().apply {
                moveTo(getPt(0f, 0.08f).x, getPt(0f, 0.08f).y)
                lineTo(getPt(1f, 0.08f).x, getPt(1f, 0.08f).y)
                lineTo(getPt(1f, 0.18f).x, getPt(1f, 0.18f).y)
                lineTo(getPt(0.7f, 0.14f).x, getPt(0.7f, 0.14f).y)
                lineTo(getPt(0.4f, 0.20f).x, getPt(0.4f, 0.20f).y)
                lineTo(getPt(0f, 0.14f).x, getPt(0f, 0.14f).y)
                close()
            }
            drawPath(path = waterSmudge, color = Color(0x553E2723))
        }
    }

    // --- OVERLAY 2: CREEPY SCHOOLYARD GRAFFITI (13% probability: .10 + .03 config) ---
    if (hasGraffiti) {
        val graffitiBmp = textures["graffiti"]
        var graffitiDrawn = false
        if (graffitiBmp != null) {
            drawIntoCanvas { canvas ->
                try {
                    val nativeCanvas = canvas.nativeCanvas
                    nativeCanvas.save()
                    val bmpW = graffitiBmp.width.toFloat()
                    val bmpH = graffitiBmp.height.toFloat()
                    val srcPts = floatArrayOf(0f, 0f, bmpW, 0f, bmpW, bmpH, 0f, bmpH)
                    val p1 = getPt(0f, 0.08f)
                    val p2 = getPt(1f, 0.08f)
                    val p3 = getPt(1f, 0.92f)
                    val p4 = getPt(0f, 0.92f)
                    val dstPts = floatArrayOf(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y, p4.x, p4.y)
                    val matrix = android.graphics.Matrix()
                    matrix.setPolyToPoly(srcPts, 0, dstPts, 0, 4)
                    nativeCanvas.concat(matrix)
                    val nativeBmp = graffitiBmp.asAndroidBitmap()
                    val paint = android.graphics.Paint().apply {
                        isFilterBitmap = true
                        isAntiAlias = true
                        val alphaShadow = ((1f - shade) * 255).toInt().coerceIn(0, 255)
                        colorFilter = android.graphics.PorterDuffColorFilter(
                            android.graphics.Color.argb(alphaShadow, 0, 0, 0),
                            android.graphics.PorterDuff.Mode.SRC_ATOP
                        )
                    }
                    nativeCanvas.drawBitmap(nativeBmp, 0f, 0f, paint)
                    nativeCanvas.restore()
                    graffitiDrawn = true
                } catch (e: Exception) {}
            }
        }
        if (!graffitiDrawn) {
            val graffitiType = random.nextInt(4)
            when (graffitiType) {
                0 -> {
                    // RUN
                    drawGraffitiText("RUN", 0.38f, 0.35f, 0.07f, 0.14f, shadeCol(Color(0xFFD32F2F)), ::getPt)
                }
                1 -> {
                    // Scribbled child skeleton monster and creepy eye rings
                    val hCX = 0.5f; val hCY = 0.45f
                    val faceRad = h1 * 0.05f
                    
                    // Head outline
                    drawCircle(
                        color = shadeCol(Color(0xDD212121)),
                        radius = faceRad,
                        center = getPt(hCX, hCY),
                        style = Stroke(width = 1.8f)
                    )
                    // Scribbled wire rings representing intense school anxiety
                    for (rot in 0..10) {
                        drawCircle(
                            color = shadeCol(Color(0x77212121)),
                            radius = faceRad * (1f + rot * 0.04f),
                            center = getPt(hCX + (random.nextFloat() - 0.5f) * 0.03f, hCY + (random.nextFloat() - 0.5f) * 0.03f),
                            style = Stroke(width = 1f)
                        )
                    }
                    // Hollow black eye dots + red center pupil dots
                    drawCircle(color = shadeCol(Color.Black), radius = 3.5f, center = getPt(hCX - 0.03f, hCY - 0.01f))
                    drawCircle(color = shadeCol(Color.Black), radius = 3.5f, center = getPt(hCX + 0.03f, hCY - 0.01f))
                    drawCircle(color = Color(0xFFFF5252), radius = 1.2f, center = getPt(hCX - 0.03f, hCY - 0.01f))
                    drawCircle(color = Color(0xFFFF5252), radius = 1.2f, center = getPt(hCX + 0.03f, hCY - 0.01f))
                    
                    // Jagged skeletal teeth mouth line
                    drawLine(color = shadeCol(Color.Black), start = getPt(hCX - 0.04f, hCY + 0.025f), end = getPt(hCX + 0.04f, hCY + 0.025f), strokeWidth = 1.5f)
                }
                2 -> {
                    // LOSER
                    drawGraffitiText("LOSER", 0.32f, 0.42f, 0.06f, 0.12f, shadeCol(Color(0xFF2E3D30)), ::getPt)
                }
                else -> {
                    val bX = 0.45f
                    val bY = 0.38f
                    val sH = 0.16f
                    // Spinal bones
                    drawLine(shadeCol(Color.Black), getPt(bX, bY), getPt(bX, bY + sH), 1.8f)
                    // Head
                    drawCircle(shadeCol(Color.Black), radius = 6f, center = getPt(bX, bY - 0.02f), style = Stroke(width = 1.5f))
                    // Arms
                    drawLine(shadeCol(Color.Black), getPt(bX - 0.08f, bY + 0.04f), getPt(bX + 0.08f, bY + 0.04f), 1.5f)
                    // Legs
                    drawLine(shadeCol(Color.Black), getPt(bX, bY + sH), getPt(bX - 0.05f, bY + sH + 0.10f), 1.5f)
                    drawLine(shadeCol(Color.Black), getPt(bX, bY + sH), getPt(bX + 0.05f, bY + sH + 0.10f), 1.5f)
                }
            }
        }
    }

    // --- BASEBOARD: SCHOOL LOCKER ROOM DARK RUBBER BASEBOARD (Always present at bottom) ---
    val bbT = 0.08f // Baseboard occupies the bottom 8% of the panel
    val baseboardPath = Path().apply {
        moveTo(getPt(0f, 1f - bbT).x, getPt(0f, 1f - bbT).y)
        lineTo(getPt(1f, 1f - bbT).x, getPt(1f, 1f - bbT).y)
        lineTo(getPt(1f, 1f).x, getPt(1f, 1f).y)
        lineTo(getPt(0f, 1f).x, getPt(0f, 1f).y)
        close()
    }
    drawPath(
        path = baseboardPath,
        color = shadeCol(Color(0xFF3E2723)) // High quality school wood/rubber baseboard
    )
    drawLine(
        color = Color.Black.copy(alpha = 0.9f),
        start = getPt(0f, 1f - bbT),
        end = getPt(1f, 1f - bbT),
        strokeWidth = 1.5f
    )
}

// Helper block texturing method
private fun DrawScope.drawSchoolGridBricks(
    color: Color,
    h1: Float,
    h2: Float,
    getPt: (Float, Float) -> Offset,
    random: java.util.Random
) {
    val rows = 5
    val cols = 5
    
    // Draw horizontal grout lines
    for (i in 1 until rows) {
        val v = 0.08f + (0.84f * i / rows)
        drawLine(
            color = color,
            start = getPt(0f, v),
            end = getPt(1f, v),
            strokeWidth = 2f
        )
    }
    
    // Vertical mortar running bond pattern
    for (i in 0 until rows) {
        val v1 = 0.08f + (0.84f * i / rows)
        val v2 = 0.08f + (0.84f * (i + 1) / rows)
        val shift = if (i % 2 == 0) 0f else 0.5f
        for (j in 0..cols) {
            val fraction = (j.toFloat() - 0.5f + shift) / cols
            if (fraction in 0.01f..0.99f) {
                drawLine(
                    color = color,
                    start = getPt(fraction, v1),
                    end = getPt(fraction, v2),
                    strokeWidth = 1.8f
                )
            }
            
            val cellFractionStart = (j.toFloat() - 1f + shift).coerceAtLeast(0f) / cols
            val cellFractionEnd = (j.toFloat() + shift).coerceAtMost(cols.toFloat()) / cols
            val cellCenterX = (cellFractionStart + cellFractionEnd) / 2
            val cellCenterY = (v1 + v2) / 2
            
            // Draw some tiny texture spots on the brick blocks
            if ((i + j) % 3 == 0) {
                drawCircle(
                    color = color.copy(alpha = 0.20f),
                    radius = ((v2 - v1) * h1 * 0.12f).coerceAtMost(5f).coerceAtLeast(1f),
                    center = getPt(cellCenterX, cellCenterY)
                )
            }
        }
    }
}

// Letter renderer for school graffiti
private fun DrawScope.drawGraffitiText(
    word: String,
    uStart: Float,
    vStart: Float,
    charW: Float,
    charH: Float,
    color: Color,
    getPt: (Float, Float) -> Offset
) {
    var curU = uStart
    for (char in word) {
        when (char) {
            'R' -> {
                drawLine(color, getPt(curU, vStart), getPt(curU, vStart + charH), 2.2f)
                drawLine(color, getPt(curU, vStart), getPt(curU + charW, vStart), 2.2f)
                drawLine(color, getPt(curU + charW, vStart), getPt(curU + charW, vStart + charH * 0.5f), 2.2f)
                drawLine(color, getPt(curU, vStart + charH * 0.5f), getPt(curU + charW, vStart + charH * 0.5f), 2.2f)
                drawLine(color, getPt(curU + charW * 0.2f, vStart + charH * 0.5f), getPt(curU + charW, vStart + charH), 2.2f)
            }
            'U' -> {
                drawLine(color, getPt(curU, vStart), getPt(curU, vStart + charH), 2.2f)
                drawLine(color, getPt(curU + charW, vStart), getPt(curU + charW, vStart + charH), 2.2f)
                drawLine(color, getPt(curU, vStart + charH), getPt(curU + charW, vStart + charH), 2.2f)
            }
            'N' -> {
                drawLine(color, getPt(curU, vStart), getPt(curU, vStart + charH), 2.2f)
                drawLine(color, getPt(curU, vStart), getPt(curU + charW, vStart + charH), 2.2f)
                drawLine(color, getPt(curU + charW, vStart), getPt(curU + charW, vStart + charH), 2.2f)
            }
            'L' -> {
                drawLine(color, getPt(curU, vStart), getPt(curU, vStart + charH), 2.2f)
                drawLine(color, getPt(curU, vStart + charH), getPt(curU + charW, vStart + charH), 2.2f)
            }
            'O' -> {
                drawLine(color, getPt(curU, vStart), getPt(curU + charW, vStart), 2.2f)
                drawLine(color, getPt(curU, vStart + charH), getPt(curU + charW, vStart + charH), 2.2f)
                drawLine(color, getPt(curU, vStart), getPt(curU, vStart + charH), 2.2f)
                drawLine(color, getPt(curU + charW, vStart), getPt(curU + charW, vStart + charH), 2.2f)
            }
            'S' -> {
                drawLine(color, getPt(curU, vStart), getPt(curU + charW, vStart), 2.2f)
                drawLine(color, getPt(curU, vStart), getPt(curU, vStart + charH * 0.5f), 2.2f)
                drawLine(color, getPt(curU, vStart + charH * 0.5f), getPt(curU + charW, vStart + charH * 0.5f), 2.2f)
                drawLine(color, getPt(curU + charW, vStart + charH * 0.5f), getPt(curU + charW, vStart + charH), 2.2f)
                drawLine(color, getPt(curU, vStart + charH), getPt(curU + charW, vStart + charH), 2.2f)
            }
            'E' -> {
                drawLine(color, getPt(curU, vStart), getPt(curU, vStart + charH), 2.2f)
                drawLine(color, getPt(curU, vStart), getPt(curU + charW, vStart), 2.2f)
                drawLine(color, getPt(curU, vStart + charH * 0.5f), getPt(curU + charW * 0.7f, vStart + charH * 0.5f), 2.2f)
                drawLine(color, getPt(curU, vStart + charH), getPt(curU + charW, vStart + charH), 2.2f)
            }
        }
        curU += charW + 0.02f
    }
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
