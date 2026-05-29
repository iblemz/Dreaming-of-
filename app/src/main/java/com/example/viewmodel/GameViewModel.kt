package com.example.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.GameDatabase
import com.example.data.database.JsonUtils
import com.example.data.model.*
import com.example.data.repository.GameRepository
import com.example.game.DungeonGenerator
import com.example.game.GeneratedDungeon
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.random.Random

enum class ScreenState {
    TITLE,
    EXPLORATION,
    COMBAT,
    INVENTORY,
    RUN_SUMMARY,
    HISTORY
}

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: GameRepository
    
    // Core states
    var screenState by mutableStateOf(ScreenState.TITLE)
        private set

    // Run history scores flow
    val scoresFlow: StateFlow<List<ScoreHistory>>

    // Resume-Game availability
    var hasSavedGame by mutableStateOf(false)
        private set

    // Player Stats
    var playerHp by mutableStateOf(100)
    var playerMaxHp by mutableStateOf(100)
    var playerDp by mutableStateOf(30)
    var playerMaxDp by mutableStateOf(30)
    var playerLevel by mutableStateOf(1)
    var playerXp by mutableStateOf(0)
    var dreamShards by mutableStateOf(0)
    var currentFloor by mutableStateOf(1)
    var nightmareIntensity by mutableStateOf(0.12f) // 0.0 to 1.0

    // Equipment
    var equippedWeapon by mutableStateOf<LootItem?>(null)
    var equippedArmor by mutableStateOf<LootItem?>(null)
    var equippedAccessory by mutableStateOf<LootItem?>(null)

    // Inventory
    var inventory = mutableListOf<LootItem>()

    // Dungeon Setup
    var dungeonGrid by mutableStateOf<List<List<Int>>>(emptyList())
    var dungeonVisited by mutableStateOf<List<List<Boolean>>>(emptyList())
    var playerX by mutableStateOf(1)
    var playerY by mutableStateOf(1)
    var playerDir by mutableStateOf(0) // 0: N, 1: E, 2: S, 3: W

    // Level Mobs and Chests
    var levelMobs = mutableListOf<MobInstance>()
    var levelChests = mutableListOf<ChestInstance>()

    // Message dialog/Notifications
    var gameMessage by mutableStateOf<String?>(null)

    // Combat State
    var activeMob by mutableStateOf<MobInstance?>(null)
    var combatLogs = mutableListOf<String>()
    var isPlayerTurn by mutableStateOf(true)
    var combatSubMenu by mutableStateOf("MAIN") // MAIN, SKILLS, ITEMS

    // Run Statistics
    var statsMobsDefeated by mutableStateOf(0)
    var wakeUpReasonString by mutableStateOf("")

    init {
        val gameDao = GameDatabase.getDatabase(application).gameDao()
        repository = GameRepository(gameDao)
        scoresFlow = repository.allScores.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        checkSaveAvailability()
    }

    private fun checkSaveAvailability() {
        viewModelScope.launch {
            val save = repository.getSave()
            hasSavedGame = save != null
        }
    }

    fun startNewGame() {
        viewModelScope.launch {
            repository.deleteSave()
            // Reset items & progression
            playerHp = 90
            playerMaxHp = 90
            playerDp = 25
            playerMaxDp = 25
            playerLevel = 1
            playerXp = 0
            dreamShards = 10
            currentFloor = 1
            nightmareIntensity = 0.08f
            statsMobsDefeated = 0

            // Starter items
            inventory.clear()
            inventory.add(LootItem("milk", "Warm Glass of Milk", ItemType.CONSUMABLE, Rarity.COMMON, effectDescription = "Restores 40 HP. Calms the spirit.", value = 5, count = 1))
            
            equippedWeapon = LootItem("slingshot_toy", "Pebble Slingshot", ItemType.WEAPON, Rarity.COMMON, atkBonus = 4, effectDescription = "Hand-crafted from a tree branch.", value = 15)
            equippedArmor = LootItem("pjs_starry", "Simple Onesie", ItemType.ARMOR, Rarity.COMMON, defBonus = 1, maxHpBonus = 0, effectDescription = "Familiar bedtime clothing.", value = 15)
            equippedAccessory = null

            generateNewFloor(1)
            screenState = ScreenState.EXPLORATION
            showToast("You fall into a deep, toy-filled dream...")
        }
    }

    fun resumeGame() {
        viewModelScope.launch {
            val save = repository.getSave() ?: return@launch
            
            // Re-hydrate state
            playerX = save.playerX
            playerY = save.playerY
            playerDir = save.playerDir
            currentFloor = save.floor
            playerHp = save.hp
            playerMaxHp = save.maxHp
            playerDp = save.dp
            playerMaxDp = save.maxDp
            playerLevel = save.level
            playerXp = save.xp
            dreamShards = save.dreamShards
            nightmareIntensity = save.nightmareIntensity

            equippedWeapon = JsonUtils.jsonToLootItem(save.equippedWeaponJson)
            equippedArmor = JsonUtils.jsonToLootItem(save.equippedArmorJson)
            equippedAccessory = JsonUtils.jsonToLootItem(save.equippedAccessoryJson)

            inventory.clear()
            inventory.addAll(JsonUtils.jsonToLootItemList(save.inventoryJson))

            dungeonGrid = JsonUtils.jsonToNestedIntList(save.dungeonGridJson)
            dungeonVisited = JsonUtils.jsonToNestedBooleanList(save.visitedJson)
            
            levelMobs.clear()
            levelMobs.addAll(JsonUtils.jsonToMobInstanceList(save.mobsJson))

            levelChests.clear()
            levelChests.addAll(JsonUtils.jsonToChestInstanceList(save.chestsJson))

            screenState = ScreenState.EXPLORATION
            showToast("Dream resumed...")
        }
    }

    fun triggerAutoSave() {
        viewModelScope.launch {
            val save = DungeonSave(
                playerX = playerX,
                playerY = playerY,
                playerDir = playerDir,
                floor = currentFloor,
                hp = playerHp,
                maxHp = playerMaxHp,
                dp = playerDp,
                maxDp = playerMaxDp,
                level = playerLevel,
                xp = playerXp,
                dreamShards = dreamShards,
                nightmareIntensity = nightmareIntensity,
                inventoryJson = JsonUtils.lootItemListToJson(inventory),
                equippedWeaponJson = JsonUtils.lootItemToJson(equippedWeapon),
                equippedArmorJson = JsonUtils.lootItemToJson(equippedArmor),
                equippedAccessoryJson = JsonUtils.lootItemToJson(equippedAccessory),
                dungeonGridJson = JsonUtils.nestedIntListToJson(dungeonGrid),
                mobsJson = JsonUtils.mobInstanceListToJson(levelMobs),
                chestsJson = JsonUtils.chestInstanceListToJson(levelChests),
                visitedJson = JsonUtils.nestedBooleanListToJson(dungeonVisited)
            )
            repository.saveGame(save)
            hasSavedGame = true
        }
    }

    private fun generateNewFloor(floorNumber: Int) {
        val width = 11 + floorNumber * 2 // floor 1 is 13x13, floor 2 is 15x15 etc.
        val height = 11 + floorNumber * 2
        val dungeon = DungeonGenerator.generate(width, height, floorNumber)

        dungeonGrid = dungeon.grid
        playerX = dungeon.startX
        playerY = dungeon.startY
        playerDir = 0 // Look North
        currentFloor = floorNumber

        // Setup fog-of-war visited grid
        val v = MutableList(dungeonGrid.size) { MutableList(dungeonGrid[0].size) { false } }
        v[playerY][playerX] = true
        // Also visit neighboring starting spots
        revealFogOfWar(dungeon.startX, dungeon.startY, v)
        dungeonVisited = v

        levelMobs.clear()
        levelMobs.addAll(dungeon.mobs)

        levelChests.clear()
        levelChests.addAll(dungeon.chests)

        // Settle intensity based on floor
        nightmareIntensity = (0.05f + (floorNumber - 1) * 0.22f).coerceIn(0.0f, 1.0f)
    }

    private fun revealFogOfWar(cx: Int, cy: Int, visited: MutableList<MutableList<Boolean>>) {
        val dirs = listOf(
            Pair(0, 0), Pair(0, -1), Pair(1, 0), Pair(0, 1), Pair(-1, 0),
            Pair(-1, -1), Pair(1, -1), Pair(-1, 1), Pair(1, 1)
        )
        for (dir in dirs) {
            val nx = cx + dir.first
            val ny = cy + dir.second
            if (ny >= 0 && ny < visited.size && nx >= 0 && nx < visited[ny].size) {
                visited[ny][nx] = true
            }
        }
    }

    // COMBAT LOGIC
    fun startCombat(mob: MobInstance) {
        activeMob = mob
        combatLogs.clear()
        combatLogs.add("A spooky ${mob.name} blocks your path!")
        combatLogs.add("Your courage is tested! Defeat it to quieten the anxiety.")
        isPlayerTurn = true
        screenState = ScreenState.COMBAT
        combatSubMenu = "MAIN"
    }

    fun endCombat(win: Boolean) {
        val mob = activeMob ?: return
        if (win) {
            mob.isDefeated = true
            levelMobs.removeAll { it.id == mob.id }
            statsMobsDefeated++

            val shardReward = 5 + Random.nextInt(5) * currentFloor
            dreamShards += shardReward
            playerXp += mob.rewardXp

            gameMessage = "Anxiety Banished!\n\n+ ${mob.rewardXp} Courage XP\n+ $shardReward Dream Shards"
            
            // Check level up
            val xpNeeded = playerLevel * 60
            if (playerXp >= xpNeeded) {
                playerXp -= xpNeeded
                playerLevel++
                playerMaxHp += 15
                playerHp = playerMaxHp
                playerMaxDp += 5
                playerDp = playerMaxDp
                gameMessage += "\n🏆 Level Up! Level $playerLevel of morning courage reached!"
            }

            // High nightmare increases chance of rare items dropping from combat!
            if (Random.nextFloat() < 0.25f + (nightmareIntensity * 0.25f)) {
                val drop = DungeonGenerator.generate(11, 11, currentFloor).chests.randomOrNull()?.lootItem
                if (drop != null) {
                    addLootToInventory(drop)
                    gameMessage += "\n🎁 Item Dropped: ${drop.name} (${drop.rarity})"
                }
            }

            screenState = ScreenState.EXPLORATION
            activeMob = null
            triggerAutoSave()
        } else {
            // Player lost combat/HP hit 0
            triggerWakeUp("Overcome by Spellbound Fears")
        }
    }

    fun selectCombatAction(action: String) {
        if (!isPlayerTurn) return

        when (action) {
            "ATTACK" -> {
                val atkBonus = equippedWeapon?.atkBonus ?: 0
                val totalAtk = 10 + (playerLevel * 3) + atkBonus
                val crit = Random.nextFloat() < 0.15f
                val damage = if (crit) (totalAtk * 1.5f).toInt() else totalAtk

                activeMob?.let { mob ->
                    mob.hp = max(0, mob.hp - damage)
                    combatLogs.add("⚔️ You strike with ${equippedWeapon?.name ?: "fists"} for $damage damage!${if (crit) " (CRITICAL HIT)" else ""}")
                    if (mob.hp <= 0) {
                        combatLogs.add("✨ The ${mob.name} burst into colorful starry bubbles!")
                        viewModelScope.launch {
                            kotlinx.coroutines.delay(1000)
                            endCombat(win = true)
                        }
                    } else {
                        isPlayerTurn = false
                        viewModelScope.launch {
                            kotlinx.coroutines.delay(1200)
                            executeEnemyTurn()
                        }
                    }
                }
            }
            "SKILL_PROJECTILE" -> { // "Bubble Shield" (DP 5, heals 25)
                if (playerDp >= 5) {
                    playerDp -= 5
                    val healAmount = 25
                    playerHp = (playerHp + healAmount).coerceAtMost(playerTotalMaxHp())
                    combatLogs.add("🫧 Bubble Shield: Created floating bubble armor! Healed $healAmount HP.")
                    isPlayerTurn = false
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(1200)
                        executeEnemyTurn()
                    }
                } else {
                    combatLogs.add("❌ Not enough Dream Power (DP)!")
                }
            }
            "SKILL_STRIKE" -> { // "Flashlight Burst" (DP 8, deals 30 damage & blinds)
                if (playerDp >= 8) {
                    playerDp -= 8
                    activeMob?.let { mob ->
                        val dmg = 32 + playerLevel * 4
                        mob.hp = max(0, mob.hp - dmg)
                        combatLogs.add("🔦 Flashlight Burst: Banished shadows! Dealt $dmg flash damage.")
                        if (mob.hp <= 0) {
                            combatLogs.add("✨ The ${mob.name} vanished in the light!")
                            viewModelScope.launch {
                                kotlinx.coroutines.delay(1000)
                                endCombat(win = true)
                            }
                        } else {
                            isPlayerTurn = false
                            viewModelScope.launch {
                                kotlinx.coroutines.delay(1200)
                                executeEnemyTurn()
                            }
                        }
                    }
                } else {
                    combatLogs.add("❌ Not enough Dream Power (DP)!")
                }
            }
            "RUN" -> {
                val escape = Random.nextFloat() < 0.5f
                if (escape) {
                    combatLogs.add("🏃 You ran away with heart racing!")
                    // Move back 1 step
                    movePlayerStepBackward()
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(1200)
                        screenState = ScreenState.EXPLORATION
                        activeMob = null
                        triggerAutoSave()
                    }
                } else {
                    combatLogs.add("❌ The escape door is locked! You couldn't run!")
                    isPlayerTurn = false
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(1200)
                        executeEnemyTurn()
                    }
                }
            }
        }
    }

    private fun executeEnemyTurn() {
        val mob = activeMob ?: return
        if (mob.hp <= 0) return

        // Calculate defense bonus
        val defBonus = equippedArmor?.defBonus ?: 0
        val accBonus = equippedAccessory?.defBonus ?: 0
        val totalDef = defBonus + accBonus

        val enemyDmg = max(1, mob.atk - totalDef)
        playerHp = max(0, playerHp - enemyDmg)

        // Generate customized monster actions based on their theme!
        val actions = when (mob.type) {
            MobType.SCRIBBLE_SPIDER -> "spins a sticky spelling-bee web"
            MobType.BROKEN_CRANE -> "swings its heavy plastic claw at your toes"
            MobType.SPELLING_BEE -> "stings you with a hard spelling riddle"
            MobType.FRACTURED_RULER -> "slaps you with wooden dimensions"
            MobType.LATE_ALARM -> "screeches deafening class alarms"
            MobType.CLOSET_SHADOW -> "extends creeping dark tendrils towards you"
            MobType.JACK_IN_THE_BOX -> "springs forth laughing maniacally"
            MobType.DUST_BUNNY_BEHEMOTH -> "pounces heavily, showering static dust"
            MobType.DRILL_TEDDY -> "whirs its loud dentist tool near your teeth"
            MobType.LOLLIPOP_MIMIC -> "hurls sticky sugary rocks"
            MobType.TOOTH_COLLECTOR -> "demands shiny teeth-keys, striking with pure dread"
        }

        combatLogs.add("👾 ${mob.name} $actions! Dealt $enemyDmg anxiety damage.")

        if (playerHp <= 0) {
            combatLogs.add("😱 Everything goes dark! You wake up shivering...")
            viewModelScope.launch {
                kotlinx.coroutines.delay(1800)
                endCombat(win = false)
            }
        } else {
            isPlayerTurn = true
            combatSubMenu = "MAIN"
        }
    }

    private fun movePlayerStepBackward() {
        var oppositeDir = (playerDir + 2) % 4
        val (dx, dy) = getDeltaForDir(oppositeDir)
        val nx = playerX + dx
        val ny = playerY + dy

        if (ny >= 0 && ny < dungeonGrid.size && nx >= 0 && nx < dungeonGrid[ny].size) {
            if (dungeonGrid[ny][nx] != DungeonGenerator.TILE_WALL) {
                playerX = nx
                playerY = ny
                triggerAutoReveal()
            }
        }
    }

    // EXPLORATION ACTIONS
    fun moveForward() {
        val (dx, dy) = getDeltaForDir(playerDir)
        val nx = playerX + dx
        val ny = playerY + dy

        if (ny >= 0 && ny < dungeonGrid.size && nx >= 0 && nx < dungeonGrid[ny].size) {
            val cell = dungeonGrid[ny][nx]
            if (cell != DungeonGenerator.TILE_WALL) {
                playerX = nx
                playerY = ny
                triggerAutoReveal()
                
                // Increase nightmare intensity slightly per step
                var inc = 0.015f
                // Accessory resists nightmare growth
                equippedAccessory?.let { acc ->
                    if (acc.id == "marble") inc -= 0.003f
                }
                nightmareIntensity = (nightmareIntensity + inc).coerceAtMost(1.0f)

                // Check for chest collisions / automatic opening
                // Oh we can activate and let player press a chest opening action manually instead of auto, which is much cooler!
                
                // Check for monster collision at the new space
                val mobInSpace = levelMobs.find { it.x == playerX && it.y == playerY && !it.isDefeated }
                if (mobInSpace != null) {
                    startCombat(mobInSpace)
                } else {
                    triggerAutoSave()
                }
            } else {
                showToast("BUMP! A thick brick wall stands in your way.")
            }
        }
    }

    fun turnLeft() {
        playerDir = (playerDir + 3) % 4 // North (0) -> West (3)
        triggerAutoReveal()
    }

    fun turnRight() {
        playerDir = (playerDir + 1) % 4 // North (0) -> East (1)
        triggerAutoReveal()
    }

    private fun triggerAutoReveal() {
        val v = dungeonVisited.map { it.toMutableList() }.toMutableList()
        v[playerY][playerX] = true
        revealFogOfWar(playerX, playerY, v)
        dungeonVisited = v
    }

    // Checking adjacency to open chests or descending stairs
    fun canInteractChest(): ChestInstance? {
        // Can open adjacent or standing-on chest
        val chestsHere = levelChests.filter { !it.isOpen }
        for (c in chestsHere) {
            if (c.x == playerX && c.y == playerY) return c
            // check directions
            if (Math.abs(c.x - playerX) <= 1 && Math.abs(c.y - playerY) <= 1) {
                return c
            }
        }
        return null
    }

    fun openChest(chest: ChestInstance) {
        chest.isOpen = true
        addLootToInventory(chest.lootItem)
        gameMessage = "🎁 Opened a dream chest!\n\nFound: ${chest.lootItem.name}\n${chest.lootItem.effectDescription}"
        levelChests.removeAll { it.x == chest.x && it.y == chest.y }
        triggerAutoSave()
    }

    fun checkStandingOnExit(): Boolean {
        if (playerX >= 0 && playerY >= 0 && playerY < dungeonGrid.size && playerX < dungeonGrid[0].size) {
            return dungeonGrid[playerY][playerX] == DungeonGenerator.TILE_EXIT
        }
        return false
    }

    fun descendStairs() {
        if (currentFloor >= 4) {
            // Reached deep bottom, player is fully healed and wakes up safely with clear light!
            triggerWakeUp("Saved by Morning Light (Beat the Nightmare!)")
        } else {
            val nextF = currentFloor + 1
            generateNewFloor(nextF)
            gameMessage = "You descend deeper into the dream world...\n\nNow entering Floor $nextF!"
            triggerAutoSave()
        }
    }

    // LOOT AND EQUIP INVENTORY FUNCTIONS
    private fun addLootToInventory(item: LootItem) {
        if (item.type == ItemType.CONSUMABLE) {
            val existing = inventory.find { it.id == item.id }
            if (existing != null) {
                existing.count += item.count
            } else {
                inventory.add(item.copy())
            }
        } else {
            inventory.add(item)
        }
    }

    fun equipItem(item: LootItem) {
        inventory.remove(item)
        when (item.type) {
            ItemType.WEAPON -> {
                equippedWeapon?.let { unequipped -> addLootToInventory(unequipped) }
                equippedWeapon = item
            }
            ItemType.ARMOR -> {
                equippedArmor?.let { unequipped -> addLootToInventory(unequipped) }
                equippedArmor = item
            }
            ItemType.ACCESSORY -> {
                equippedAccessory?.let { unequipped -> addLootToInventory(unequipped) }
                equippedAccessory = item
            }
            else -> {}
        }
        showToast("Equipped: ${item.name}")
        triggerAutoSave()
    }

    fun unequipItem(item: LootItem) {
        when (item.type) {
            ItemType.WEAPON -> {
                equippedWeapon = null
                addLootToInventory(item)
            }
            ItemType.ARMOR -> {
                equippedArmor = null
                addLootToInventory(item)
            }
            ItemType.ACCESSORY -> {
                equippedAccessory = null
                addLootToInventory(item)
            }
            else -> {}
        }
        showToast("Unequipped: ${item.name}")
        triggerAutoSave()
    }

    fun useConsumable(item: LootItem) {
        if (item.type != ItemType.CONSUMABLE) return

        if (item.count > 1) {
            item.count--
        } else {
            inventory.remove(item)
        }

        when (item.id) {
            "milk" -> {
                val valHeal = 45
                playerHp = (playerHp + valHeal).coerceAtMost(playerTotalMaxHp())
                nightmareIntensity = (nightmareIntensity - 0.10f).coerceAtLeast(0.0f)
                showToast("Drank milk! HP increased by $valHeal, relaxed spirit.")
            }
            "cookie" -> {
                val dpHeal = 15
                playerDp = (playerDp + dpHeal).coerceAtMost(playerMaxDp)
                showToast("Ate cookie! DP increased by $dpHeal.")
            }
            "teddy_healing" -> {
                playerHp = (playerHp + 100).coerceAtMost(playerTotalMaxHp())
                nightmareIntensity = (nightmareIntensity - 0.35f).coerceAtLeast(0.0f)
                showToast("You hugged the plush teddy! Mind calmed.")
            }
            "pills_sleep" -> {
                playerHp = playerTotalMaxHp()
                playerDp = playerMaxDp
                nightmareIntensity = 0.0f
                showToast("Perfect Sleep Gateway activated. Fully Cleansed!")
            }
        }
        triggerAutoSave()
    }

    fun dismissMessage() {
        gameMessage = null
    }

    fun setScreen(state: ScreenState) {
        screenState = state
    }

    // STAT CALCULATIONS WITH EQUIPS
    fun playerTotalMaxHp(): Int {
        val armorBonus = equippedArmor?.maxHpBonus ?: 0
        val accBonus = equippedAccessory?.maxHpBonus ?: 0
        return playerMaxHp + armorBonus + accBonus
    }

    fun playerAtkRating(): Int {
        val base = 10 + (playerLevel * 3)
        val weaponBonus = equippedWeapon?.atkBonus ?: 0
        val accBonus = equippedAccessory?.atkBonus ?: 0
        return base + weaponBonus + accBonus
    }

    fun playerDefRating(): Int {
        val armorBonus = equippedArmor?.defBonus ?: 0
        val accBonus = equippedAccessory?.defBonus ?: 0
        return armorBonus + accBonus
    }

    // GAME WAKE-UP (RUN OVER)
    private fun triggerWakeUp(reason: String) {
        wakeUpReasonString = reason
        screenState = ScreenState.RUN_SUMMARY

        // Insert score record into local database
        viewModelScope.launch {
            val scoreVal = calculateScore()
            val scoreRecord = ScoreHistory(
                playerName = "Bedtime Crawler",
                floorReached = currentFloor,
                mobsDefeated = statsMobsDefeated,
                finalLevel = playerLevel,
                wakeUpReason = reason,
                datePlayed = System.currentTimeMillis(),
                score = scoreVal
            )
            repository.insertScore(scoreRecord)
            repository.deleteSave()
            hasSavedGame = false
        }
    }

    fun checkAbandonCurrentGame() {
        triggerWakeUp("Abruptly Shocked Awake")
    }

    private fun calculateScore(): Int {
        return (currentFloor * 1000) + (statsMobsDefeated * 250) + (playerLevel * 300) + (dreamShards * 10)
    }

    fun deleteHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // HEADING UTILS
    fun getDeltaForDir(dir: Int): Pair<Int, Int> {
        return when (dir) {
            0 -> Pair(0, -1) // North
            1 -> Pair(1, 0)  // East
            2 -> Pair(0, 1)  // South
            3 -> Pair(-1, 0) // West
            else -> Pair(0, 0)
        }
    }

    fun dirLabel(dir: Int): String {
        return when (dir) {
            0 -> "North ▲"
            1 -> "East ►"
            2 -> "South ▼"
            3 -> "West ◄"
            else -> "Unknown"
        }
    }

    // Toasts helper
    var toastMessage by mutableStateOf<String?>(null)
    private fun showToast(msg: String) {
        toastMessage = msg
    }
}
