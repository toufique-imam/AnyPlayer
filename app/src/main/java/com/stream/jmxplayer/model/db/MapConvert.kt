package com.stream.jmxplayer.model.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MapConvert {

    @TypeConverter
    fun stringToMap(value: String?): Map<String, String> {
        if (value == null) return HashMap()
        val type = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun mapToString(value: Map<String, String>?): String {
        if (value == null) return ""
        return gson.toJson(value)
    }

    companion object {
        val gson = Gson()
    }
}