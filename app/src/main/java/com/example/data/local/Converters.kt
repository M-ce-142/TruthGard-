package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.Verdict

class Converters {
    @TypeConverter
    fun fromVerdict(verdict: Verdict): String = verdict.name

    @TypeConverter
    fun toVerdict(name: String): Verdict = try {
        Verdict.valueOf(name)
    } catch (e: Exception) {
        Verdict.NOT_ENOUGH_EVIDENCE
    }

    @TypeConverter
    fun fromList(list: List<String>): String = list.joinToString("|||")

    @TypeConverter
    fun toList(data: String): List<String> = if (data.isEmpty()) emptyList() else data.split("|||")
}
