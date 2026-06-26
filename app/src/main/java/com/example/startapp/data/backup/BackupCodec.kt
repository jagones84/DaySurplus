package com.example.startapp.data.backup

import com.example.startapp.data.model.AppBackup
import com.google.gson.Gson

object BackupCodec {

    private val gson = Gson()

    fun encode(backup: AppBackup): String {
        return gson.toJson(backup)
    }

    fun decode(json: String): AppBackup {
        return gson.fromJson(json, AppBackup::class.java)
            ?: throw IllegalArgumentException("Invalid backup JSON")
    }
}

