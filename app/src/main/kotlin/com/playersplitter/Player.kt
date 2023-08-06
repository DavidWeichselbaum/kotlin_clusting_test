package com.playersplitter
import java.time.LocalDateTime

data class Player(val name: String, val elo: Float, val joiningDate: LocalDateTime)
