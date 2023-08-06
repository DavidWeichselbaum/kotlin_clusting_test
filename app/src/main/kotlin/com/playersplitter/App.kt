package com.playersplitter

import java.time.LocalDateTime

import com.playersplitter.PlayerSplitter
import com.playersplitter.Player
import com.playersplitter.Game
import com.playersplitter.Node


fun main() {
    val players = List(17) {
        Player(
            "Player${it}",
            (1000..2000).random().toFloat(),
            LocalDateTime.now().minusDays((0..30).random().toLong())
        )
    }


    val games = List(100) {
        val player1 = players.random().name
        var player2 = players.random().name
        while (player1 == player2) {
            player2 = players.random().name // Ensure we don't have a game with two identical players
        }
        val dateTime = LocalDateTime.now().minusDays((0..30).random().toLong())
        Game(player1, player2, dateTime)
    }


    val funFriendshipWeight = 0.5
    val playerSplitter = PlayerSplitter(players, games, funFriendshipWeight)

    playerSplitter.printAffinityMatrix()
    println()
    playerSplitter.printClustering()
    println()
    playerSplitter.printTree()
    println()

    var playerSplitTargets = listOf(8, 9)
    println("Split Targets: ${playerSplitTargets}")
    var playerSplits = playerSplitter.split(playerSplitTargets)
    if (playerSplits != null){
        for (playerSplit in playerSplits) {
            println("Split: ${playerSplit}")
            println("N Players: ${playerSplit.size}")
        }
    }
    println()

    playerSplitTargets = listOf(6, 6, 5)
    println("Split Targets: ${playerSplitTargets}")
    playerSplits = playerSplitter.split(playerSplitTargets)
    if (playerSplits != null){
        for (playerSplit in playerSplits) {
            println("Split: ${playerSplit}")
            println("N Players: ${playerSplit.size}")
        }
    }
    println()

    playerSplitTargets = listOf(8, 8, 1)
    println("Split Targets: ${playerSplitTargets}")
    playerSplits = playerSplitter.split(playerSplitTargets)
    if (playerSplits != null){
        for (playerSplit in playerSplits) {
            println("Split: ${playerSplit}")
            println("N Players: ${playerSplit.size}")
        }
    }
    println()

    playerSplitTargets = listOf(8, 8)
    println("Split Targets: ${playerSplitTargets}")
    playerSplits = playerSplitter.split(playerSplitTargets)
    if (playerSplits != null){
        for (playerSplit in playerSplits) {
            println("Split: ${playerSplit}")
            println("N Players: ${playerSplit.size}")
        }
    }
    println()

    playerSplitTargets = listOf(8, 9, 0)
    println("Split Targets: ${playerSplitTargets}")
    playerSplits = playerSplitter.split(playerSplitTargets)
    if (playerSplits != null){
        for (playerSplit in playerSplits) {
            println("Split: ${playerSplit}")
            println("N Players: ${playerSplit.size}")
        }
    }
    println()
}
