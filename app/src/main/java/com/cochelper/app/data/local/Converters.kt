package com.cochelper.app.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromSkillList(list: List<SkillItem>): String = json.encodeToString(
        kotlinx.serialization.builtins.ListSerializer(SkillItem.serializer()), list
    )

    @TypeConverter
    fun toSkillList(s: String): List<SkillItem> = runCatching {
        json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(SkillItem.serializer()), s)
    }.getOrDefault(emptyList())
}
