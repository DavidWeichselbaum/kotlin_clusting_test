package com.cluster_test

import java.time.LocalDateTime
import java.util.Stack
import java.util.ArrayDeque

import smile.clustering.linkage.CompleteLinkage
import smile.clustering.HierarchicalClustering
import smile.math.distance.EuclideanDistance


data class Player(val name: String, val elo: Float, val joiningDate: LocalDateTime)


data class Game(val player1: String, val player2: String, val dateTime: LocalDateTime)


class Node {
    var child1: Node? = null
    var child2: Node? = null
    var leaf: Int? = null
    var cluster: Int? = null
    var height: Double = 0.0
}


class TabelSplitter (val players: List<Player>, val games: List<Game>) {

    val affinityMatrix: Array<DoubleArray> = Array(players.size) { DoubleArray(players.size) }
    var clustering: HierarchicalClustering? = null
    public var tree: Node? = null

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
                val lastGame = games.filter { game -> (game.player1 == player1.name && game.player2 == player2.name) ||
                                                      (game.player1 == player2.name && game.player2 == player1.name) }
                                     .maxByOrNull { it.dateTime }
                val mostRecentJoinDate = players[i].joiningDate.coerceAtLeast(players[j].joiningDate)

                val secondsSinceLastGame = lastGame?.dateTime?.let {
                    java.time.temporal.ChronoUnit.SECONDS.between(it, LocalDateTime.now()).toDouble()
                } ?: java.time.temporal.ChronoUnit.SECONDS.between(mostRecentJoinDate, LocalDateTime.now()).toDouble()

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
                val mostRecentJoinDate = players[i].joiningDate.coerceAtLeast(players[j].joiningDate)

                val secondsSinceLastGame = lastGame?.dateTime?.let {
                    java.time.temporal.ChronoUnit.SECONDS.between(it, LocalDateTime.now()).toDouble()
                } ?: java.time.temporal.ChronoUnit.SECONDS.between(mostRecentJoinDate, LocalDateTime.now()).toDouble()

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
                    val score = affinityMatrix[i][j]
                    println("FAFMATS distance for ${player1.name} and ${player2.name}: $score")
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

        if (merge != null && heights != null) {
            for (i in merge.indices) {
                val merge = merge[i]
                val firstItem = if (merge[0] < players.size) players[merge[0]].name else "Cluster ${merge[0] - players.size}"
                val secondItem = if (merge[1] < players.size) players[merge[1]].name else "Cluster ${merge[1] - players.size}"
                println("Step ${i+1}: Merge $firstItem and $secondItem into Cluster ${i}")
            }
        }

        if (heights != null) {
            for (i in heights.indices) {
                val clusterHeight = heights[i]
                println("Cluster ${i} height: ${clusterHeight}")
            }
        }

    }


    fun createTree() {
        val n = players.size

        // init player nodes
        val nodes = MutableList(n) { Node().apply { leaf = it } }

        val merge = clustering?.tree()
        val heights = clustering?.height()

        if (merge != null && heights != null) {
            for (i in 0 until n-1) {
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

        if (node.leaf != null) {
            println("${indent}└─── Player: ${node.leaf}, Height: ${node.height}")
        } else {
            println("${indent}└─── Cluster: ${node.cluster}, Height: ${node.height}")
            printTree(node.child1!!, indent + "   |")
            printTree(node.child2!!, indent + "    ")
        }
    }


    fun split(groupSizes: List<Int>): MutableList<MutableList<Int>>? {
        if (tree == null) {
            return null
        }
        // Stack for DFS
        val stack = ArrayDeque<Node>()
        stack.push(tree)

        val result = mutableListOf<MutableList<Int>>()

        for (groupSize in groupSizes) {
            val group = mutableListOf<Int>()

            while (stack.isNotEmpty() && group.size < groupSize) {
                val node = stack.pop()

                // If the node is a leaf, add it to group
                if (node.leaf != null) {
                    group.add(node.leaf!!)
                } else {
                    // Else, add its children to stack
                    // We add child2 first, so child1 is processed first (LIFO stack)
                    if (node.child2 != null) {
                        stack.push(node.child2!!)
                    }
                    if (node.child1 != null) {
                        stack.push(node.child1!!)
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


    // fun split(playerSplits: List<Int>){
    //     if (tree == null) {
    //         println("The tree is empty.")
    //         return
    //     }

    //     val playerSubset: List<Player>
    //     for (nPlayers in playerSplits) {
    //         println("Splitting off ${nPlayers} players.")
    //         val lowestNode = getLowestNode()
    //         println("Lowest Player: ${lowestNode}")

    //         var splitPlayers = splitPlayersFromTable(tree, nPlayers)
    //         if (splitPlayers == null) {
    //             println("No Players split")
    //             return
    //         }
    //         for (splitPlayer in splitPlayers) {
    //             println("SplitPlayer: ${splitPlayer}")
    //         }

    //         printTree()
    //         println()
    //     }
    // }


    // fun splitPlayersFromTable(root: Node?, groupSize: Int): MutableList<Int>? {
    //     if (root == null) {
    //         return null
    //     }

    //     // List to store leaf nodes
    //     val leaves = mutableListOf<Int>()

    //     // Stack for DFS
    //     val stack = Stack<Node>()
    //     stack.push(root)

    //     while (stack.isNotEmpty() && leaves.size < groupSize) {
    //         val node = stack.pop()

    //         // If the node is a leaf, add it to leaves
    //         if (node.leaf != null) {
    //             leaves.add(node.leaf!!)
    //         } else {
    //             // Else, add its children to stack
    //             // We add child2 first, so child1 is processed first (LIFO stack)
    //             if (node.child2 != null) {
    //                 stack.push(node.child2!!)
    //             }
    //             if (node.child1 != null) {
    //                 stack.push(node.child1!!)
    //             }
    //         }
    //     }

    //     // If we have enough leaves, return them
    //     if (leaves.size == groupSize) {
    //         return leaves
    //     }

    //     return null
    // }


    // fun getLowestNode(root: Node? = tree): Int? {
    //     var currentNode = root
    //     while (currentNode != null && currentNode.leaf == null) {
    //         currentNode = if (currentNode.child1!!.height > currentNode.child2!!.height) {
    //             currentNode.child1
    //         } else {
    //             currentNode.child2
    //         }
    //     }
    //     return currentNode?.leaf
    // }


    // fun splitPlayersFromTable(nPlayers: Int) {

    // }


}



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

    val tableSplitter = TabelSplitter(players, games)
    tableSplitter.makeAffinityMatrix(0.5)
    tableSplitter.printAffinityMatrix()
    println()
    tableSplitter.createClustering()
    println()
    tableSplitter.printClustering()
    tableSplitter.createTree()
    println()
    tableSplitter.printTree()

    val playerSplitTargets = listOf(8, 9)
    println("Split Targets: ${playerSplitTargets}")

    val playerSplits = tableSplitter.split(playerSplitTargets)

    if (playerSplits != null){
        for (playerSplit in playerSplits) {
            println("Split: ${playerSplit}")
            println("N Players: ${playerSplit.size}")
        }
    }
}
