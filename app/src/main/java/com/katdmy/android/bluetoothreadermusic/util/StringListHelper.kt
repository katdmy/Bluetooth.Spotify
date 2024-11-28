package com.katdmy.android.bluetoothreadermusic.util

object StringListHelper {
    fun String.getList(): List<String> = this.split(", ")

    fun List<String>.getString(): String = this.joinToString(", ")
}