package com.eriksargsyan.blockchain.data

data class Block(
    val index: Int,
    val prevHash: String,
    var hash: String,
    val data: String,
    var nonce: Int
)