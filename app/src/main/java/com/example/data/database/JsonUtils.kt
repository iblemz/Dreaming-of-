package com.example.data.database

import com.example.data.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object JsonUtils {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    fun lootItemListToJson(list: List<LootItem>): String {
        val type = Types.newParameterizedType(List::class.java, LootItem::class.java)
        val adapter = moshi.adapter<List<LootItem>>(type)
        return adapter.toJson(list)
    }

    fun jsonToLootItemList(json: String): List<LootItem> {
        return try {
            val type = Types.newParameterizedType(List::class.java, LootItem::class.java)
            val adapter = moshi.adapter<List<LootItem>>(type)
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun lootItemToJson(item: LootItem?): String? {
        if (item == null) return null
        val adapter = moshi.adapter(LootItem::class.java)
        return adapter.toJson(item)
    }

    fun jsonToLootItem(json: String?): LootItem? {
        if (json.isNullOrEmpty()) return null
        return try {
            val adapter = moshi.adapter(LootItem::class.java)
            adapter.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    fun mobInstanceListToJson(list: List<MobInstance>): String {
        val type = Types.newParameterizedType(List::class.java, MobInstance::class.java)
        val adapter = moshi.adapter<List<MobInstance>>(type)
        return adapter.toJson(list)
    }

    fun jsonToMobInstanceList(json: String): List<MobInstance> {
        return try {
            val type = Types.newParameterizedType(List::class.java, MobInstance::class.java)
            val adapter = moshi.adapter<List<MobInstance>>(type)
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun chestInstanceListToJson(list: List<ChestInstance>): String {
        val type = Types.newParameterizedType(List::class.java, ChestInstance::class.java)
        val adapter = moshi.adapter<List<ChestInstance>>(type)
        return adapter.toJson(list)
    }

    fun jsonToChestInstanceList(json: String): List<ChestInstance> {
        return try {
            val type = Types.newParameterizedType(List::class.java, ChestInstance::class.java)
            val adapter = moshi.adapter<List<ChestInstance>>(type)
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun nestedIntListToJson(matrix: List<List<Int>>): String {
        val type = Types.newParameterizedType(List::class.java, List::class.java, Integer::class.java)
        val adapter = moshi.adapter<List<List<Int>>>(type)
        return adapter.toJson(matrix)
    }

    fun jsonToNestedIntList(json: String): List<List<Int>> {
        return try {
            val type = Types.newParameterizedType(List::class.java, List::class.java, Integer::class.java)
            val adapter = moshi.adapter<List<List<Int>>>(type)
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun nestedBooleanListToJson(matrix: List<List<Boolean>>): String {
        val type = Types.newParameterizedType(List::class.java, List::class.java, java.lang.Boolean::class.java)
        val adapter = moshi.adapter<List<List<Boolean>>>(type)
        return adapter.toJson(matrix)
    }

    fun jsonToNestedBooleanList(json: String): List<List<Boolean>> {
        return try {
            val type = Types.newParameterizedType(List::class.java, List::class.java, java.lang.Boolean::class.java)
            val adapter = moshi.adapter<List<List<Boolean>>>(type)
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
