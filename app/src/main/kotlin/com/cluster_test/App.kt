package com.cluster_test

import smile.clustering.KMeans

class App {
    val data = arrayOf(
        doubleArrayOf(1.0, 2.0, 3.0),
        doubleArrayOf(4.0, 5.0, 6.0),
        doubleArrayOf(7.0, 8.0, 9.0), 
        doubleArrayOf(1.0, 8.0, 3.0), 
        doubleArrayOf(7.0, 8.0, 9.0), 
        doubleArrayOf(7.0, 8.0, 9.0), 
    )

    val k = 2
    val kmeans = KMeans.fit(data, k)
}

fun main() {
    println(App().kmeans)
}
