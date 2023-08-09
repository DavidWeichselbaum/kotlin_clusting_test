package com.playersplitter

import kotlin.math.pow
import java.time.LocalDateTime

import com.playersplitter.PlayerSplitter
import com.playersplitter.Player
import com.playersplitter.Game
import com.playersplitter.Node


fun main() {
    val funFriendshipWeight = 0.5

    val players = List(17) {
        Player(
            id=it,
            name="Player${it}",
            elo=1000.0,
            realElo=(500..1500).random().toDouble(),
            joiningDate=LocalDateTime.now()
        )
    }

    val games: MutableList<Game> = mutableListOf()
    val playerSplitTargets: List<Int> = listOf(8, 9)


    for (draftID in 1..20){
        var draftTime = LocalDateTime.now().plusHours(6 * draftID.toLong())

        println("Draft ${draftID}")

        val playerSplitter = PlayerSplitter(players, games, funFriendshipWeight)

        playerSplitter.printDistanceMatrix()
        println()
        playerSplitter.printClustering()
        println()
        playerSplitter.printTree()
        println()
        val playerSplits = playerSplitter.split(playerSplitTargets)

        if (playerSplits != null){
            for (tableID in 0 until playerSplits.size) {
                var playerSplit = playerSplits[tableID]
                println("Split of ${playerSplit.size} players:")
                for (player in playerSplit) {
                    println("    " + player)
                }

                println("Fake Draft ${draftID} Table ${tableID}")
                fakeDraft(playerSplit, games, draftTime) 
            }
        }
    }
}


fun getElos(eloAOld: Double, eloBOld: Double, result: String): Pair<Double, Double>{
    val expectedTenfoldAdvantage: Double = 400.0
    val kFactor: Double = 32.0

    var eloDifference: Double = eloBOld.toDouble() - eloAOld.toDouble()
    val playerAExpectedScore: Double = (1 / (1 + 10.toDouble().pow((eloDifference / expectedTenfoldAdvantage))))
    val playerAScore: Double =
        when (result) {
            "2:0", "2:1" -> {
                1.0
            }
            "draw" -> {
                0.5
            }
            else -> {
                0.0
            }
        }
    val playerAScoreOffset: Double = playerAScore - playerAExpectedScore
    eloDifference = (kFactor * playerAScoreOffset)
    val eloANew = eloAOld + eloDifference
    val eloBNew = eloBOld - eloDifference

    return Pair(eloANew, eloBNew)
}


fun Game.hasPlayed(p1: Player, p2: Player): Boolean {
    return (player1 == p1.id && player2 == p2.id) || (player1 == p2.id && player2 == p1.id)
}


fun fakeMatch(player1: Player, player2: Player, time: LocalDateTime, games: MutableList<Game>): Player {
    // val winner = if (Math.random() > 0.5) player1 else player2
    val realEloDifference = player2.elo - player1.elo

    val winner = if (realEloDifference < 0.0) player1 else player2

    games.add(Game(player1.id, player2.id, time))

    if (player1 == winner){
        val elos = getElos(player1.elo, player2.elo, "2:0")
        player1.elo = elos.first
        player2.elo = elos.second
    } else {
        val elos = getElos(player1.elo, player2.elo, "0:2")
        player1.elo = elos.first
        player2.elo = elos.second
    }

    return winner
}


fun resetRanks(players: List<Player>) {
    players.forEach { it.rank = 0 }
}


fun fakeDraft(players: List<Player>, games: MutableList<Game>, time: LocalDateTime, rounds: Int = 3){
    val unmatchedPlayers = players.toMutableList()

    for (round in 1..rounds) {
        println("Round: ${round}")

        unmatchedPlayers.sortBy { it.rank }

        val currentRoundMatches = mutableListOf<Game>()

        for (i in 0 until unmatchedPlayers.size-1 step 2) {
            val player1 = unmatchedPlayers[i]
            var player2 = unmatchedPlayers[i + 1]

            // // Check if these players have already played before and find a new match if necessary
            // while (games.any { game -> game.hasPlayed(player1, player2) } && i + 2 < unmatchedPlayers.size) {
            //     i += 2
            //     player2 = unmatchedPlayers[i + 1]
            // }

            val winner = fakeMatch(player1, player2, time, currentRoundMatches)


            // Adjust rankings based on the result
            if (winner == player1) {
                player1.rank++
                player2.rank--
            } else {
                player2.rank++
                player1.rank--
            }

            println("${player1.name} vs ${player2.name} winner: ${winner.name}    ${player1}, ${player2}")
        }

        games.addAll(currentRoundMatches)
        resetRanks(players)
    }


}
