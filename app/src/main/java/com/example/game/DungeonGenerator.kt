package com.example.game

import com.example.data.model.*
import java.util.Stack
import kotlin.random.Random

data class GeneratedDungeon(
    val grid: List<List<Int>>,
    val startX: Int,
    val startY: Int,
    val exitX: Int,
    val exitY: Int,
    val chests: List<ChestInstance>,
    val mobs: List<MobInstance>
)

object DungeonGenerator {
    
    // TILE TYPE CONSTANTS
    const val TILE_WALL = 0
    const val TILE_FLOOR = 1
    const val TILE_START = 2
    const val TILE_EXIT = 3

    fun generate(width: Int, height: Int, floor: Int): GeneratedDungeon {
        // Must be odd size for perfect maze cell/wall alignment
        val w = if (width % 2 == 0) width + 1 else width
        val h = if (height % 2 == 0) height + 1 else height
        
        val grid = Array(h) { IntArray(w) { TILE_WALL } }

        // Start carve position
        carveMaze(grid, 1, 1, w, h)
        
        // Let's carve a few open 3x3 rooms to make navigation interesting and place chests/mobs
        val numRooms = 3 + Random.nextInt(3)
        for (r in 0 until numRooms) {
            val rxChoices = (1 until w - 3 step 2).toList()
            val ryChoices = (1 until h - 3 step 2).toList()
            val rx = if (rxChoices.isNotEmpty()) rxChoices.random() else 1
            val ry = if (ryChoices.isNotEmpty()) ryChoices.random() else 1
            val roomWidth = 3
            val roomHeight = 3
            for (dy in 0 until roomHeight) {
                for (dx in 0 until roomWidth) {
                    if (rx + dx < w - 1 && ry + dy < h - 1) {
                        grid[ry + dy][rx + dx] = TILE_FLOOR
                    }
                }
            }
        }

        // Set Start position
        val startX = 1
        val startY = 1
        grid[startY][startX] = TILE_START

        // Set Exit position (find a furthest floor tile)
        var exitX = w - 2
        var exitY = h - 2
        while (grid[exitY][exitX] != TILE_FLOOR) {
            exitX--
            if (exitX <= 1) {
                exitX = w - 2
                exitY--
            }
            if (exitY <= 1) {
                exitX = w - 2
                exitY = h - 2
                break
            }
        }
        grid[exitY][exitX] = TILE_EXIT

        // Collect all open Floor tiles so we can place chests and mobs randomly
        val openTiles = mutableListOf<Pair<Int, Int>>()
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                if (grid[y][x] == TILE_FLOOR && (x != startX || y != startY) && (x != exitX || y != exitY)) {
                    openTiles.add(Pair(x, y))
                }
            }
        }
        openTiles.shuffle()

        // Place Chests
        val chestCount = 3 + Random.nextInt(3)
        val chests = mutableListOf<ChestInstance>()
        val chestSpots = openTiles.take(chestCount.coerceAtMost(openTiles.size))
        openTiles.removeAll(chestSpots)

        for (spot in chestSpots) {
            val loot = generateLootForChest(floor)
            chests.add(
                ChestInstance(
                    x = spot.first,
                    y = spot.second,
                    lootItem = loot,
                    isOpen = false
                )
            )
        }

        // Place Mobs
        val mobCount = 4 + Random.nextInt(4)
        val mobs = mutableListOf<MobInstance>()
        val mobSpots = openTiles.take(mobCount.coerceAtMost(openTiles.size))
        
        for (i in mobSpots.indices) {
            val spot = mobSpots[i]
            val mob = createRandomMob(floor, spot.first, spot.second, "mob_$i")
            mobs.add(mob)
        }

        // Return the final Grid and assets
        return GeneratedDungeon(
            grid = grid.map { it.toList() },
            startX = startX,
            startY = startY,
            exitX = exitX,
            exitY = exitY,
            chests = chests,
            mobs = mobs
        )
    }

    private fun carveMaze(grid: Array<IntArray>, startX: Int, startY: Int, w: Int, h: Int) {
        val stack = Stack<Pair<Int, Int>>()
        grid[startY][startX] = TILE_FLOOR
        stack.push(Pair(startX, startY))

        while (stack.isNotEmpty()) {
            val (cx, cy) = stack.peek()
            val neighbors = mutableListOf<Triple<Int, Int, Pair<Int, Int>>>()

            // 4 directions at distance 2
            val dirs = listOf(
                Triple(0, -2, Pair(0, -1)), // North
                Triple(2, 0, Pair(1, 0)),   // East
                Triple(0, 2, Pair(0, 1)),   // South
                Triple(-2, 0, Pair(-1, 0))  // West
            )

            for (dir in dirs) {
                val nx = cx + dir.first
                val ny = cy + dir.second
                if (nx > 0 && nx < w - 1 && ny > 0 && ny < h - 1) {
                    if (grid[ny][nx] == TILE_WALL) {
                        neighbors.add(Triple(nx, ny, dir.third))
                    }
                }
            }

            if (neighbors.isNotEmpty()) {
                val next = neighbors.random()
                val (nx, ny, wallOffset) = next
                
                // Carve through the wall
                grid[cy + wallOffset.second][cx + wallOffset.first] = TILE_FLOOR
                grid[ny][nx] = TILE_FLOOR
                
                stack.push(Pair(nx, ny))
            } else {
                stack.pop()
            }
        }
    }

    private fun generateLootForChest(floor: Int): LootItem {
        val rand = Random.nextDouble()
        val rarity = when {
            rand < 0.10 + (floor * 0.05) -> Rarity.DREAMY
            rand < 0.40 + (floor * 0.05) -> Rarity.RARE
            else -> Rarity.COMMON
        }

        // Weighted items depending on floor
        val lootPool = mutableListOf<LootItem>()

        if (rarity == Rarity.COMMON) {
            lootPool.add(LootItem("milk", "Warm Glass of Milk", ItemType.CONSUMABLE, Rarity.COMMON, effectDescription = "Restores 40 HP. Calms the spirit.", value = 10, count = 1))
            lootPool.add(LootItem("cookie", "Star Cookie", ItemType.CONSUMABLE, Rarity.COMMON, effectDescription = "Restores 15 DP (Dream Power).", value = 8, count = 1))
            lootPool.add(LootItem("slingshot", "Slingshot of Courage", ItemType.WEAPON, Rarity.COMMON, atkBonus = 6, effectDescription = "Simple wooden slingshot.", value = 25))
            lootPool.add(LootItem("onesie_blue", "Blue Starry Pajamas", ItemType.ARMOR, Rarity.COMMON, defBonus = 3, maxHpBonus = 10, effectDescription = "Soft sleepwear.", value = 25))
            lootPool.add(LootItem("marble", "Lucky Red Marble", ItemType.ACCESSORY, Rarity.COMMON, atkBonus = 1, defBonus = 1, effectDescription = "Found in your pocket.", value = 15))
        } else if (rarity == Rarity.RARE) {
            lootPool.add(LootItem("teddy_healing", "Comforting Teddy Bear", ItemType.CONSUMABLE, Rarity.RARE, effectDescription = "Restores 100 HP & cleanses 30% Nightmare.", value = 35, count = 1))
            lootPool.add(LootItem("laser_gun", "Retro Laser Pistol", ItemType.WEAPON, Rarity.RARE, atkBonus = 15, effectDescription = "Beeps with neon cosmic lights.", value = 75))
            lootPool.add(LootItem("sword_wooden", "Imaginary Hero's Sword", ItemType.WEAPON, Rarity.RARE, atkBonus = 18, effectDescription = "Glows with daytime courage.", value = 90))
            lootPool.add(LootItem("dino_pjs", "Green T-Rex Onesie", ItemType.ARMOR, Rarity.RARE, defBonus = 8, maxHpBonus = 25, effectDescription = "Roar! Makes you feel tough.", value = 80))
            lootPool.add(LootItem("flashlight", "Flashlight of Truth", ItemType.WEAPON, Rarity.RARE, atkBonus = 12, defBonus = 4, effectDescription = "Banishes closet monsters.", value = 70))
            lootPool.add(LootItem("badge", "Super Kid Sheriff Badge", ItemType.ACCESSORY, Rarity.RARE, atkBonus = 3, defBonus = 3, effectDescription = "Shiny plastic yellow badge.", value = 50))
        } else { // DREAMY
            lootPool.add(LootItem("pills_sleep", "Dream Gateway Elixir", ItemType.CONSUMABLE, Rarity.DREAMY, effectDescription = "Heals full HP/DP & wipes all Nightmare.", value = 90, count = 1))
            lootPool.add(LootItem("imaginary_friend", "Imaginary Friend's Shield", ItemType.ARMOR, Rarity.DREAMY, defBonus = 20, maxHpBonus = 60, effectDescription = "They are always right beside you.", value = 200))
            lootPool.add(LootItem("cosmic_saber", "Cosmic Dream Sabre", ItemType.WEAPON, Rarity.DREAMY, atkBonus = 35, effectDescription = "Humming weapon forged in deep sleep.", value = 220))
            lootPool.add(LootItem("super_cape", "Bed-Sheet Hero Cape", ItemType.ARMOR, Rarity.DREAMY, defBonus = 12, maxHpBonus = 40, effectDescription = "You feel completely invincible.", value = 160))
            lootPool.add(LootItem("fairy_coin", "Dentist Tooth Fairy Coin", ItemType.ACCESSORY, Rarity.DREAMY, atkBonus = 6, defBonus = 6, maxHpBonus = 30, effectDescription = "Pure golden coin direct from dreams.", value = 150))
        }

        return lootPool.random()
    }

    private fun createRandomMob(floor: Int, x: Int, y: Int, id: String): MobInstance {
        // Different pools depending on floor level
        val (type, name, hp, maxHp, atk, rewardXp) = when (floor) {
            1 -> {
                if (Random.nextBoolean()) {
                    val mHp = 25 + Random.nextInt(10)
                    Hexad(MobType.SCRIBBLE_SPIDER, "Scribble Spider", mHp, mHp, 5, 20)
                } else {
                    val mHp = 35 + Random.nextInt(10)
                    Hexad(MobType.BROKEN_CRANE, "Broken Sandbox Crane", mHp, mHp, 4, 25)
                }
            }
            2 -> {
                val rand = Random.nextInt(3)
                when (rand) {
                    0 -> {
                        val mHp = 45 + Random.nextInt(10)
                        Hexad(MobType.SPELLING_BEE, "Orthodontist Spelling Bee", mHp, mHp, 9, 35)
                    }
                    1 -> {
                        val mHp = 40 + Random.nextInt(12)
                        Hexad(MobType.FRACTURED_RULER, "Fractured Ruler Mimic", mHp, mHp, 11, 40)
                    }
                    else -> {
                        val mHp = 50 + Random.nextInt(10)
                        Hexad(MobType.LATE_ALARM, "Class-Bell Late Alarm", mHp, mHp, 8, 45)
                    }
                }
            }
            3 -> {
                val rand = Random.nextInt(3)
                when (rand) {
                    0 -> {
                        val mHp = 65 + Random.nextInt(15)
                        Hexad(MobType.CLOSET_SHADOW, "Closet Shadow Creeper", mHp, mHp, 15, 60)
                    }
                    1 -> {
                        val mHp = 60 + Random.nextInt(10)
                        Hexad(MobType.JACK_IN_THE_BOX, "Grinning Jack-in-the-Box", mHp, mHp, 18, 65)
                    }
                    else -> {
                        val mHp = 80 + Random.nextInt(20)
                        Hexad(MobType.DUST_BUNNY_BEHEMOTH, "Dust Bunny Behemoth", mHp, mHp, 13, 80)
                    }
                }
            }
            else -> {
                // Clinic levels (Floor 4)
                val rand = Random.nextInt(3)
                when (rand) {
                    0 -> {
                        val mHp = 90 + Random.nextInt(20)
                        Hexad(MobType.DRILL_TEDDY, "Dentist Drill Bear", mHp, mHp, 20, 100)
                    }
                    1 -> {
                        val mHp = 85 + Random.nextInt(15)
                        Hexad(MobType.LOLLIPOP_MIMIC, "Sticky Lollipop Golem", mHp, mHp, 18, 110)
                    }
                    else -> {
                        val mHp = 130 + Random.nextInt(30)
                        Hexad(MobType.TOOTH_COLLECTOR, "Grinning Tooth Collector", mHp, mHp, 25, 150)
                    }
                }
            }
        }

        // Scale attributes slightly with general floor level or random offset
        return MobInstance(
            id = id,
            name = name,
            type = type,
            x = x,
            y = y,
            hp = hp,
            maxHp = maxHp,
            atk = atk,
            rewardXp = rewardXp
        )
    }

    // A helper hexad class instead of creating custom tuples
    private data class Hexad<A, B, C, D, E, F>(
        val first: A, val second: B, val third: C, val fourth: D, val fifth: E, val sixth: F
    )
}
