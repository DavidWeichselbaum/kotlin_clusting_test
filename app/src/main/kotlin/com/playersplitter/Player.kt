package com.playersplitter
import java.time.LocalDateTime

data class Player(
    val id: Int,
    val name: String,
    val joiningDate: LocalDateTime,
    val realElo: Double,
    var elo: Double,
    var rank: Int = 0
)
