package com.playersplitter

import java.time.LocalDateTime

import com.playersplitter.PlayerSplitter
import com.playersplitter.Player
import com.playersplitter.Game
import com.playersplitter.Node


fun main() {
    val allPlayers = List(100) {
        Player(
            id=it,
            "Player${it}",
            (1000..2000).random().toFloat(),
            LocalDateTime.now().minusDays((0..30).random().toLong())
        )
    }
    val players = allPlayers.slice(30..46)  // to test indexing


    val games = List(100) {
        val player1 = players.random().id
        var player2 = players.random().id
        while (player1 == player2) {
            player2 = players.random().id // Ensure we don't have a game with two identical players
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

    var playerSplitTargetList = listOf(
        listOf(8, 9),
        listOf(6, 6, 5),
        listOf(8, 8, 1),
        listOf(8, 8),
        listOf(8, 9, 0)
    )

    for (playerSplitTargets in playerSplitTargetList) {
        println("Split Targets: ${playerSplitTargets}")
        val playerSplits = playerSplitter.split(playerSplitTargets)
        if (playerSplits != null){
            for (playerSplit in playerSplits) {
                println("Split of ${playerSplit.size} players:")
                for (player in playerSplit) {
                    println("    " + player)
                }
            }
        }
        println()
    }
}
