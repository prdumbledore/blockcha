package com.eriksargsyan.blockchain.data


import com.eriksargsyan.blockchain.Vars.mainNode
import com.eriksargsyan.blockchain.util.sha256

object Blockchain {

    var blockchain = mutableListOf<Block>()

    fun generateGenesis(): Boolean {
        if (mainNode) {
            blockchain.add(
                Block(0, "", sha256("Genesis").dropLast(6).plus("000000"), "Genesis", 0)
            )
        }
        return mainNode
    }
}

