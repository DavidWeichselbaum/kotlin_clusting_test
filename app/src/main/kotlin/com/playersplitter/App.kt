package com.playersplitter

import kotlin.math.pow
import kotlin.random.Random
import java.time.LocalDateTime

import com.playersplitter.PlayerSplitter
import com.playersplitter.Player
import com.playersplitter.Game
import com.playersplitter.Node


val expectedTenfoldAdvantage: Double = 400.0
val kFactor: Double = 32.0
val funFriendshipWeight = 0.9
val doFAFMATS: Boolean = true


fun splitBySizes(list: MutableList<Player>, sizes: List<Int>): MutableList<MutableList<Player>> {
    val result = mutableListOf<MutableList<Player>>()
    var currentIndex = 0
    for (size in sizes) {
        val end = currentIndex + size
        if (end <= list.size) {
            result.add(list.subList(currentIndex, end))
            currentIndex = end
        } else {
            // Handle case where there aren't enough elements left in the list for the given size
            result.add(list.subList(currentIndex, list.size))
            break
        }
    }
    return result
}


fun main() {

    val players = List(17) {
        Player(
            id=it,
            name="Player${it}",
            elo=1000.0,
            realElo=(500..1500).random().toDouble(),
            joiningDate=LocalDateTime.now().minusHours(103)
        )
    }

    val games: MutableList<Game> = mutableListOf()
    val playerSplitTargets: List<Int> = listOf(8, 9)

    val draftStartTime = LocalDateTime.now().minusHours(100)

    for (draftID in 1..10){
        var draftTime = draftStartTime.plusHours(6 * draftID.toLong())

        println("\nDraft ${draftID}")

        var playerSplits: MutableList<MutableList<Player>>? = null
        if (doFAFMATS){
            val playerSplitter = PlayerSplitter(players, games, funFriendshipWeight)

            // playerSplitter.printDistanceMatrix()
            // println()
            // playerSplitter.printClustering()
            // println()
            // playerSplitter.printTree()
            // println()
            
            playerSplits = playerSplitter.split(playerSplitTargets)
        } else {
            val mutablePlayers = players.toMutableList()
            mutablePlayers.shuffle()
            playerSplits = splitBySizes(mutablePlayers, playerSplitTargets)
        }

        if (playerSplits != null){
            for (tableID in 0 until playerSplits.size) {
                var playerSplit = playerSplits[tableID]
                println("\nSplit of ${playerSplit.size} players:")
                for (player in playerSplit) {
                    println("    " + player)
                }

                println("Fake Draft ${draftID} Table ${tableID}")
                fakeDraft(playerSplit, games, draftTime) 
            }
        }
    }


    println("Matches total: ${games.size}")
    val matchFrequencyMatrix: Array<IntArray> = Array(players.size) { IntArray(players.size) }
    for (game in games){
        matchFrequencyMatrix[game.player1][game.player2] = matchFrequencyMatrix[game.player1][game.player2] +1
        matchFrequencyMatrix[game.player2][game.player1] = matchFrequencyMatrix[game.player2][game.player1] +1
    }
    println("Times played against:")
    for (i in players.indices) {
        for (j in players.indices) {
            println("${players[i].name} vs. ${players[j].name} : ${matchFrequencyMatrix[i][j]}")
        }
    }

    val matchFrequencyList: MutableList<Int> = mutableListOf()
    for (i in players.indices) {
        for (j in players.indices) {
            if (i > j){
                matchFrequencyList.add(matchFrequencyMatrix[i][j])
            }
        }
    }
    val matchFrequencyCounter = matchFrequencyList.groupingBy { it }.eachCount()
    // for (matchFrequencyCount in matchFrequencyCounter){
    //     println("2 Same Player being paired ${matchFrequencyCount.} times happened ${} times")
    // }
    matchFrequencyCounter.entries.sortedBy { it.key }.forEach {
        println("The same pairing ${it.key} times appears ${it.value} times")
    }
}


fun getElos(eloAOld: Double, eloBOld: Double, result: String): Pair<Double, Double>{
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
    val realEloDifference = player2.elo - player1.elo

    val player1WinProb: Double = (1 / (1 + 10.toDouble().pow((realEloDifference / expectedTenfoldAdvantage))))

    val randomValue = Random.nextDouble()
    val winner = if (randomValue < player1WinProb) player1 else player2

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
    unmatchedPlayers.shuffle()

    for (round in 1..rounds) {
        // println("Round: ${round}")

        unmatchedPlayers.sortBy { - it.rank }  // sort inverted

        val currentRoundMatches = mutableListOf<Game>()

        // for (player in unmatchedPlayers){
        //     println("${player.name}: ${player.rank}")
        // }

        for (i in 0 until unmatchedPlayers.size-1 step 2) {
            val player1 = unmatchedPlayers[i]
            var player2 = unmatchedPlayers[i + 1]

            // // Check if these players have already played before and find a new match if necessary
            // while (games.any { game -> game.hasPlayed(player1, player2) } && i + 2 < unmatchedPlayers.size) {
            //     i += 2
            //     player2 = unmatchedPlayers[i + 1]
            // }

            // println("${player1}, ${player2}")
            val winner = fakeMatch(player1, player2, time, currentRoundMatches)
            // println("${player1}, ${player2}")
            println("${player1.name} vs ${player2.name} winner: ${winner.name}")

            // Adjust rankings based on the result
            if (winner == player1) {
                player1.rank++
                player2.rank--
            } else {
                player2.rank++
                player1.rank--
            }
        }

        games.addAll(currentRoundMatches)
    }
    resetRanks(players)

}
