package com.playersplitter

import java.time.LocalDateTime
import java.util.Stack
import java.util.ArrayDeque

import smile.clustering.linkage.CompleteLinkage
import smile.clustering.HierarchicalClustering

import com.playersplitter.Player
import com.playersplitter.Game
import com.playersplitter.Node


fun getSecondsSinceLastGame(player1: Player, player2: Player, games: List<Game>): Double {
    // If the players have not encountered each other in a game, it will pick the most recent join date.
    val lastGameDate = games.filter { game -> (game.player1 == player1.id && game.player2 == player2.id) ||
                                          (game.player1 == player2.id && game.player2 == player1.id) }
                         .maxByOrNull { it.dateTime }
    val mostRecentJoinDate = player1.joiningDate.coerceAtLeast(player2.joiningDate)

    val secondsSinceLastGame = lastGameDate?.dateTime?.let {
        java.time.temporal.ChronoUnit.SECONDS.between(it, LocalDateTime.now()).toDouble()
    } ?: java.time.temporal.ChronoUnit.SECONDS.between(mostRecentJoinDate, LocalDateTime.now()).toDouble()

    return secondsSinceLastGame
}


class PlayerSplitter (val players: List<Player>, val games: List<Game>, val funFriendshipWeight: Double) {

    val affinityMatrix: Array<DoubleArray> = Array(players.size) { DoubleArray(players.size) }
    var clustering: HierarchicalClustering? = null
    public var tree: Node? = null

    init {
        makeAffinityMatrix(funFriendshipWeight)
        createClustering()
        createTree()
    }

    fun makeAffinityMatrix(funFriendshipWeight: Double) {
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
                val secondsSinceLastGame = getSecondsSinceLastGame(player1, player2, games)

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
                val secondsSinceLastGame = getSecondsSinceLastGame(player1, player2, games)

                // Normalize differences
                val normalizedEloDiff = (eloDiff - minEloDiff) / (maxEloDiff - minEloDiff)
                val normalizedTimeDiff = (secondsSinceLastGame - minTimeDiff) / (maxTimeDiff - minTimeDiff)

                var fafmatsScore = normalizedEloDiff * funFriendshipWeight + normalizedTimeDiff * (1-funFriendshipWeight)
                affinityMatrix[i][j] = fafmatsScore 
                affinityMatrix[j][i] = fafmatsScore 
                }
            }
    }


    fun printAffinityMatrix() {
        for (i in players.indices) {
            for (j in players.indices) {
                if (i != j) {  // Optional, if you don't want to print the affinity score of a player with themselves.
                    val player1 = players[i]
                    val player2 = players[j]
                    val eloDiff = kotlin.math.abs(player1.elo - player2.elo).toDouble()
                    val secondsSinceLastGame = getSecondsSinceLastGame(player1, player2, games)
                    val score = affinityMatrix[i][j]
                    println("FAFMATS distance for ${player1.name} [elo: ${player1.elo}] and ${player2.name} [elo: ${player2.elo}] with elo difference ${eloDiff} and time difference ${secondsSinceLastGame}: $score")
                }
            }
        }
    }


    fun createClustering() {
        val linkage = CompleteLinkage.of(affinityMatrix)
        clustering = HierarchicalClustering.fit(linkage)
    }


    fun printClustering() {
        val merge = clustering?.tree()
        val heights = clustering?.height()

        if (merge == null || heights == null) {
            return
        }

        for (i in merge.indices) {
            val currentMerge = merge[i]
            val firstItem = if (currentMerge[0] < players.size) players[currentMerge[0]].name else "Cluster ${currentMerge[0] - players.size}"
            val secondItem = if (currentMerge[1] < players.size) players[currentMerge[1]].name else "Cluster ${currentMerge[1] - players.size}"
            println("Step ${i+1}: Merge $firstItem and $secondItem into Cluster ${i}")
        }

        for (i in heights.indices) {
            val clusterHeight = heights[i]
            println("Cluster ${i} height: ${clusterHeight}")
        }

    }


    fun createTree() {

        // init player nodes
        val nodes = players.map { Node().apply { player = it } }.toMutableList()

        val merge = clustering?.tree()
        val heights = clustering?.height()

        if (merge != null && heights != null) {
            for (i in 0 until players.size-1) {
                val newNode = Node()
                newNode.cluster = i
                newNode.child1 = nodes[merge[i][0]]
                newNode.child2 = nodes[merge[i][1]]
                newNode.height = heights[i]
                nodes.add(newNode)
            }
            tree = nodes.last()
        }
    }


    fun printTree(node: Node? = tree, indent: String = "") {
        if (node == null) {
            println("The tree is empty.")
            return
        }

        val nodePlayer = node.player
        if (nodePlayer != null) {
            println("${indent}└─── Player: ${nodePlayer.name}, [${"%.2f".format(node.height)}]")
        } else {
            println("${indent}└─── Cluster: ${node.cluster}, [${"%.2f".format(node.height)}]")
            val child1height = node.child1?.height ?: 0.0
            val child2height = node.child2?.height ?: 0.0
            if (child1height > child2height) {
                printTree(node.child1!!, indent + "     |")
                printTree(node.child2!!, indent + "      ")
            } else {
                printTree(node.child2!!, indent + "     |")
                printTree(node.child1!!, indent + "      ")
        }
        }
    }


    fun checkSplits(groupSizes: List<Int>): Boolean {
        if (groupSizes.sum() != players.size) {
            println("Group sizes ${groupSizes} (sum ${groupSizes.sum()}) do not fit into a set of ${players.size} players!")
            return false
        }

        for (groupSize in groupSizes) {
            if (groupSize < 1) {
                println("Group size ${groupSize} is too small!")
                return false
            }
        }

        return true
    }


    fun split(groupSizes: List<Int>): MutableList<MutableList<Player>>? {
        if (tree == null) {
            println("The tree is empty.")
            return null
        }
        if (! checkSplits(groupSizes)){
            return null
        }

        // Stack for DFS
        val stack = ArrayDeque<Node>()
        stack.push(tree)

        val result = mutableListOf<MutableList<Player>>()

        for (groupSize in groupSizes) {
            val group = mutableListOf<Player>()

            while (stack.isNotEmpty() && group.size < groupSize) {
                val node = stack.pop()

                // If the node is a player, add it to group
                if (node.player != null) {
                    group.add(node.player!!)
                } else {
                    // Else, add its children to stack
                    val child1height = node.child1?.height ?: 0.0
                    val child2height = node.child2?.height ?: 0.0
                    if (child1height > child2height) {
                        // To follow the heights, add child2 first, so child1 is processed first (LIFO stack)
                        if (node.child2 != null) {
                            stack.push(node.child2!!)
                        }
                        if (node.child1 != null) {
                            stack.push(node.child1!!)
                        }
                    } else {
                        // To follow the heights, add child1 first, so child2 is processed first (LIFO stack)
                        if (node.child1 != null) {
                            stack.push(node.child1!!)
                        }
                        if (node.child2 != null) {
                            stack.push(node.child2!!)
                        }
                    }
                }
            }

            // If we have enough leaves for this group, add to result
            if (group.size == groupSize) {
                result.add(group)
            } else {
                break
            }
        }

        return result
    }
}
