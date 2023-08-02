package com.cluster_test

import smile.clustering.linkage.CompleteLinkage
import smile.clustering.HierarchicalClustering
import smile.math.distance.EuclideanDistance

class App {
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
    // println(App().hclust.tree())
    val tree = App().hclust.tree()
    for (row in tree) {
        println(row.joinToString(", "))
    }
}
