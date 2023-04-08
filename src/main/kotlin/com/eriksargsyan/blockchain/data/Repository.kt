package com.eriksargsyan.blockchain.data

import com.eriksargsyan.blockchain.Vars.currentNodePort
import com.eriksargsyan.blockchain.Vars.node1Port
import com.eriksargsyan.blockchain.Vars.url1
import com.eriksargsyan.blockchain.Vars.url2
import com.eriksargsyan.blockchain.data.Blockchain.blockchain
import com.eriksargsyan.blockchain.util.Constants.ASK_THIRD_NODE_ROUTING
import com.eriksargsyan.blockchain.util.Constants.BLOCK_INSERTED_ROUTING
import com.eriksargsyan.blockchain.util.Constants.PORT
import com.eriksargsyan.blockchain.util.Constants.VALIDATE_BLOCKCHAIN_ROUTING
import com.eriksargsyan.blockchain.util.generateData
import com.eriksargsyan.blockchain.util.sha256
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface NotificationReceivedCallback {
    fun onNotificationReceived()
}

class Repository(private val client: HttpClient) {

    suspend fun notify(block: Block, url: String): Boolean {
        val response = client.post(url) {
            contentType(ContentType.Application.Json)
            header(PORT, currentNodePort)
            setBody(block)
        }
        return response.bodyAsText() == "1"
    }

    suspend fun ask(url: String): Block {
        val response = client.get(url) {
            contentType(ContentType.Application.Json)
        }
        return response.body()
    }

    suspend fun validate(url: String): MutableList<Block> {
        val response = client.get(url) {
            contentType(ContentType.Application.Json)
        }
        return response.body()
    }

    fun insert(block: Block) {
        blockchain.add(block)
        println("Block was inserted. Current BlockChain size is ${blockchain.size}, last block hash is ${blockchain.last().hash}")
    }


    companion object {
        private var instance: Repository? = null

        fun get(): Repository {

            if (instance == null) {
                instance = Repository(HttpClient(CIO) {
                    install(ContentNegotiation) {
                        gson()
                    }
                })
            }
            return instance!!
        }



        suspend fun Repository.validateBlockChain(): Boolean {
            val requestedBlockChain = this.validate(url1 + VALIDATE_BLOCKCHAIN_ROUTING)
            return if (requestedBlockChain == blockchain) {
                println("Current node is ACTUAL. Blockchain wasn't updated")
                true
            } else {
                blockchain = requestedBlockChain
                println("Current node is in MINORITY. Updated current blockchain. Cool!")
                false
            }
        }

        suspend fun Repository.generateNewBlock(): Block? {
            var gotBlock = false
            val prevBlock = blockchain.lastOrNull()
            if (prevBlock == null) {
                this.validate(url1 + VALIDATE_BLOCKCHAIN_ROUTING)
                return null
            }
            val currentBlock = Block(
                blockchain.size,
                prevBlock.hash,
                "",
                generateData(),
                0
            )
            while (!gotBlock) {
                val hashInput = currentBlock.index.toString() +
                        currentBlock.prevHash +
                        currentBlock.data +
                        currentBlock.nonce.toString()
                val hashOutput = sha256(hashInput)
                if (hashOutput.takeLast(6) == "000000") {
                    gotBlock = true
                    currentBlock.hash = hashOutput
                    println("Successfully generated block with $hashOutput. Inserting it")
                    handleReceivedBlock(currentBlock, currentNodePort)

                    withContext(Dispatchers.IO) {
                        try {
                            notify(currentBlock, url1 + BLOCK_INSERTED_ROUTING)
                            notify(currentBlock, url2 + BLOCK_INSERTED_ROUTING)
                        } catch (_: Exception) {}
                    }
                } else {
                    currentBlock.nonce++
                }
            }
            return currentBlock
        }

        suspend fun Repository.handleReceivedBlock(
            block: Block,
            senderNodePort: String,
            notificationReceivedCallback: NotificationReceivedCallback? = null
        ) {
            val lastBlock = blockchain.last()
            if (block.index == lastBlock.index + 1 && block.prevHash == lastBlock.hash) {
                println("handling received block. Everything is OK. Inserting it")
                this.insert(block)
                notificationReceivedCallback?.onNotificationReceived()
            } else if (senderNodePort != currentNodePort) {
                val askUrl = if (senderNodePort == node1Port) url2 + ASK_THIRD_NODE_ROUTING else url1 + ASK_THIRD_NODE_ROUTING
                val thirdNodeBlock = this.ask(askUrl)
                if (thirdNodeBlock.hash == block.hash) {
                    println("Node is in minority. Validating full blockchain")
                    val validateUrl = if (senderNodePort == node1Port) url2 + VALIDATE_BLOCKCHAIN_ROUTING else url1 + VALIDATE_BLOCKCHAIN_ROUTING
                    this.validate(validateUrl)
                } else if (thirdNodeBlock.hash == lastBlock.hash) {
                    println("Node is actual. Nothing to do")
                }
            }
        }

        fun sendLastBlock(): Block {
            return blockchain.last()
        }


    }
}