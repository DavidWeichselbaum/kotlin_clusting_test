package com.playersplitter
import java.time.LocalDateTime

data class Game(
    val player1: Int,
    val player2: Int,
    val dateTime: LocalDateTime
)
