package com.cluster_test

import java.time.LocalDateTime

import smile.clustering.linkage.CompleteLinkage
import smile.clustering.HierarchicalClustering
import smile.math.distance.EuclideanDistance


// data class Player(val name: String, val elo: Int)
data class Player(val name: String, val elo: Float, val joiningDate: LocalDateTime)
data class Game(val player1: String, val player2: String, val dateTime: LocalDateTime)


class App (val players: List<Player>, val games: List<Game>) {

    // val affinityScores = mutableMapOf<Pair<String, String>, Double>()
    val affinityScoresMatrix: Array<DoubleArray> = Array(players.size) { DoubleArray(players.size) }

    fun make_affinity_matrix(elo_difference_weight: Double, time_difference_weight: Double) {
        var minEloDiff = Double.MAX_VALUE
        var maxEloDiff = Double.MIN_VALUE
        var minTimeDiff = Double.MAX_VALUE
        var maxTimeDiff = Double.MIN_VALUE

        // Find min and max for both Elo and time differences
        for (i in players.indices) {
            for (j in i+1 until players.size) {
                val player1 = players[i]
                val player2 = players[j]
                val eloDiff = kotlin.math.abs(player1.elo - player2.elo).toDouble()
                val lastGame = games.filter { game -> (game.player1 == player1.name && game.player2 == player2.name) ||
                                                      (game.player1 == player2.name && game.player2 == player1.name) }
                                     .maxByOrNull { it.dateTime }
                val most_recent_join_date = players[i].joiningDate.coerceAtLeast(players[j].joiningDate)

                val secondsSinceLastGame = lastGame?.dateTime?.let {
                    java.time.temporal.ChronoUnit.SECONDS.between(it, LocalDateTime.now()).toDouble()
                } ?: java.time.temporal.ChronoUnit.SECONDS.between(most_recent_join_date, LocalDateTime.now()).toDouble()

                minEloDiff = minEloDiff.coerceAtMost(eloDiff)
                maxEloDiff = maxEloDiff.coerceAtLeast(eloDiff)
                minTimeDiff = minTimeDiff.coerceAtMost(secondsSinceLastGame)
                maxTimeDiff = maxTimeDiff.coerceAtLeast(secondsSinceLastGame)
            }
        }

        // Calculate normalized differences and affinity scores
        for (i in players.indices) {
            for (j in i+1 until players.size) {
                val player1 = players[i]
                val player2 = players[j]
                val eloDiff = kotlin.math.abs(player1.elo - player2.elo).toDouble()
                val lastGame = games.filter { game -> (game.player1 == player1.name && game.player2 == player2.name) ||
                                                      (game.player1 == player2.name && game.player2 == player1.name) }
                                     .maxByOrNull { it.dateTime }
                val most_recent_join_date = players[i].joiningDate.coerceAtLeast(players[j].joiningDate)

                val secondsSinceLastGame = lastGame?.dateTime?.let {
                    java.time.temporal.ChronoUnit.SECONDS.between(it, LocalDateTime.now()).toDouble()
                } ?: java.time.temporal.ChronoUnit.SECONDS.between(most_recent_join_date, LocalDateTime.now()).toDouble()

                // Normalize differences
                val normalizedEloDiff = (eloDiff - minEloDiff) / (maxEloDiff - minEloDiff)
                val normalizedTimeDiff = (secondsSinceLastGame - minTimeDiff) / (maxTimeDiff - minTimeDiff)

                // Calculate the affinity score and add it to the map
                // affinityScores[player1.name to player2.name] = normalizedEloDiff * elo_difference_weight + normalizedTimeDiff * time_difference_weight

                var fafmats_score = normalizedEloDiff * elo_difference_weight + normalizedTimeDiff * time_difference_weight
                affinityScoresMatrix[i][j] = fafmats_score 
                affinityScoresMatrix[j][i] = fafmats_score 
                }
            }
    }


    fun print_affinity_matrix() {
        for (i in players.indices) {
            for (j in players.indices) {
                if (i != j) {  // Optional, if you don't want to print the affinity score of a player with themselves.
                    val player1 = players[i]
                    val player2 = players[j]
                    val score = affinityScoresMatrix[i][j]
                    println("Affinity score for ${player1.name} and ${player2.name}: $score")
                }
            }
        }
    }




    val list = listOf(
        Triple("string1", 1.23f, LocalDateTime.now()),
        Triple("string2", 4.56f, LocalDateTime.now().plusDays(1)),
        Triple("string3", 7.89f, LocalDateTime.now().plusDays(2))
    )

    val similarities = arrayOf(
        doubleArrayOf(1.0, 2.0, 3.0, 4.0),
        doubleArrayOf(4.0, 5.0, 6.0, 7.0),
        doubleArrayOf(7.0, 8.0, 9.0, 10.0), 
        doubleArrayOf(11.0, 12.0, 13.0, 14.0),
    )

    val linkage = CompleteLinkage.of(similarities)
    val hclust = HierarchicalClustering.fit(linkage)
}


fun main() {
    val players = List(17) {
        Player(
            "Player${it + 1}",
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

    val app = App(players, games)
    app.make_affinity_matrix(0.5, 0.5)
    app.print_affinity_matrix()
    // val tree = App().hclust.tree()
    // for (row in tree) {
    //     println(row.joinToString(", "))
    // }
}
