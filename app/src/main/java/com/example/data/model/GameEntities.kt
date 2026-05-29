package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ItemType {
    WEAPON, ARMOR, ACCESSORY, CONSUMABLE, KEY
}

enum class Rarity {
    COMMON, RARE, DREAMY
}

data class LootItem(
    val id: String,
    val name: String,
    val type: ItemType,
    val rarity: Rarity,
    val atkBonus: Int = 0,
    val defBonus: Int = 0,
    val maxHpBonus: Int = 0,
    val effectDescription: String = "",
    val value: Int = 0,
    var count: Int = 1
)

enum class MobType {
    HALLWAY_TRIPPER,
    LUNCH_MONEY_THIEF,
    LOCKER_SHOVER,
    CAFETERIA_CUTTER,
    CHALK_FLINGER,
    CYBER_TAUNTER,
    DESK_SLAMMER,
    GYM_CLASS_TYRANT,
    CORRIDOR_BLOCKER,
    JUICE_STALKER,
    CHAD_THE_OVERLORD
}

data class MobInstance(
    val id: String,
    val name: String,
    val type: MobType,
    val x: Int,
    val y: Int,
    var hp: Int,
    val maxHp: Int,
    val atk: Int,
    val rewardXp: Int,
    var isDefeated: Boolean = false
)

data class ChestInstance(
    val x: Int,
    val y: Int,
    val lootItem: LootItem,
    var isOpen: Boolean = false
)

@Entity(tableName = "dungeon_save")
data class DungeonSave(
    @PrimaryKey val id: Int = 1,
    val playerX: Int,
    val playerY: Int,
    val playerDir: Int, // 0: N, 1: E, 2: S, 3: W
    val floor: Int,
    val hp: Int,
    val maxHp: Int,
    val dp: Int,
    val maxDp: Int,
    val level: Int,
    val xp: Int,
    val dreamShards: Int,
    val nightmareIntensity: Float, // 0.0 to 1.0f
    
    // Serialized JSON/String contents
    val inventoryJson: String,
    val equippedWeaponJson: String?,
    val equippedArmorJson: String?,
    val equippedAccessoryJson: String?,
    val dungeonGridJson: String,
    val mobsJson: String,
    val chestsJson: String,
    val visitedJson: String
)

@Entity(tableName = "score_history")
data class ScoreHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playerName: String,
    val floorReached: Int,
    val mobsDefeated: Int,
    val finalLevel: Int,
    val wakeUpReason: String,
    val datePlayed: Long,
    val score: Int
)
