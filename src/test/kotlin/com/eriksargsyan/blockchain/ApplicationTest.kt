package com.eriksargsyan.blockchain

import com.eriksargsyan.blockchain.Vars.currentNodePort
import com.eriksargsyan.blockchain.Vars.mainNode
import com.eriksargsyan.blockchain.Vars.node1Port
import com.eriksargsyan.blockchain.Vars.node2Port
import com.eriksargsyan.blockchain.data.Block
import com.eriksargsyan.blockchain.data.Blockchain.blockchain
import com.eriksargsyan.blockchain.data.Blockchain.generateGenesis
import com.eriksargsyan.blockchain.data.Repository
import com.eriksargsyan.blockchain.data.Repository.Companion.generateNewBlock
import com.eriksargsyan.blockchain.data.Repository.Companion.handleReceivedBlock
import com.eriksargsyan.blockchain.util.Constants.BLOCK_INSERTED_ROUTING
import com.eriksargsyan.blockchain.util.sha256
import com.google.gson.GsonBuilder
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.utils.io.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test


class ApplicationTest {

    private val repository = Repository.get()

    @Test
    fun testValidateGenesisManually() {
        initMainNode()
        mainNode = false
        assertEquals(false, generateGenesis())
        mainNode = true
        assertEquals(true, generateGenesis())
        val genesis = blockchain[0]
        assertEquals("000000", genesis.hash.takeLast(6))
        assertEquals("Genesis", genesis.data)
    }

    @Test
    fun testValidateBlockChainManually() {
        initMainNode()
        generateGenesis()
        val genesis = blockchain[0]
        val secondBlock: Block?
        val thirdBlock: Block?
        val fourthBlock: Block?
        runBlocking {
            secondBlock = repository.generateNewBlock()
            thirdBlock = repository.generateNewBlock()
            fourthBlock = repository.generateNewBlock()
        }
        assertEquals(genesis.hash, secondBlock?.prevHash)
        assertEquals(secondBlock?.hash, thirdBlock?.prevHash)
        assertEquals(thirdBlock?.hash, fourthBlock?.prevHash)
        assertEquals(4, blockchain.size)
        assertEquals("000000", genesis.hash.takeLast(6))
        assertEquals("000000", secondBlock?.hash?.takeLast(6))
        assertEquals("000000", thirdBlock?.hash?.takeLast(6))
        assertEquals("000000", fourthBlock?.hash?.takeLast(6))

        assertEquals(0, genesis.index)
        assertEquals(1, secondBlock?.index)
        assertEquals(2, thirdBlock?.index)
        assertEquals(3, fourthBlock?.index)
    }

    @Test
    fun testBlockInsertedNotification() {
        initMainNode()
        generateGenesis()
        runBlocking {
            repository.generateNewBlock()
        }
        val mockEngine = MockEngine { _ ->
            respond(
                content = ByteReadChannel("""1"""),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = Repository(HttpClient(mockEngine) {
            install(ContentNegotiation) {
                gson()
            }
        })

        runBlocking {
            assertEquals(
                true,
                client.notify(blockchain.last(), BLOCK_INSERTED_ROUTING + node1Port)
            )
        }
    }

    @Test
    fun testBlockInsertedNotificationFailed() {
        initMainNode()
        generateGenesis()
        runBlocking {
            repository.generateNewBlock()
        }
        val mockEngine = MockEngine { _ ->
            respond(
                content = ByteReadChannel("""0"""),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = Repository(HttpClient(mockEngine) {
            install(ContentNegotiation) {
                gson()
            }
        })

        runBlocking {
            assertEquals(
                false,
                client.notify(blockchain.last(), BLOCK_INSERTED_ROUTING + node1Port)
            )
        }
    }

    @Test
    fun testAskThirdNode() {
        initMainNode()
        generateGenesis()
        runBlocking {
            repository.generateNewBlock()
        }
        val mockEngine = MockEngine { _ ->
            GsonBuilder().create().toJson(blockchain.last())
            respond(
                content = ByteReadChannel(GsonBuilder().create().toJson(blockchain.last())),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = Repository(HttpClient(mockEngine) {
            install(ContentNegotiation) {
                gson()
            }
        })

        runBlocking {
            assertEquals(
                blockchain.last(),
                client.ask(BLOCK_INSERTED_ROUTING + node2Port)
            )
        }
    }


    @Test
    fun testValidateBlockChain() {
        initMainNode()
        generateGenesis()
        runBlocking {
            repository.generateNewBlock()
        }
        val mockEngine = MockEngine { _ ->
            respond(
                content = ByteReadChannel(GsonBuilder().create().toJson(blockchain)),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = Repository(HttpClient(mockEngine) {
            install(ContentNegotiation) {
                gson()
            }
        })

        runBlocking {
            assertEquals(
                blockchain,
                client.validate(BLOCK_INSERTED_ROUTING + node1Port)
            )
        }
    }

    @Test
    fun testValidateBlockChainFailed() {
        initMainNode()
        generateGenesis()
        runBlocking {
            repository.generateNewBlock()
        }
        val mockEngine = MockEngine { _ ->
            respond(
                content = ByteReadChannel(GsonBuilder().create().toJson(listOf(blockchain.first()))),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = Repository(HttpClient(mockEngine) {
            install(ContentNegotiation) {
                gson()
            }
        })

        runBlocking {
            assertNotEquals(
                blockchain,
                client.validate(BLOCK_INSERTED_ROUTING + node1Port)
            )
        }
    }

    @Test
    fun testInsertBlock() {
        initMainNode()
        generateGenesis()
        val blockToInsert: Block?
        runBlocking {
            blockToInsert = repository.generateNewBlock()
        }
        blockchain.dropLast(1)
        if (blockToInsert != null) {
            repository.insert(blockToInsert)
        }
        assertEquals(blockToInsert, blockchain.last())
    }

    @Test
    fun testHandleReceivedBlock() {
        initMainNode()
        generateGenesis()
        runBlocking {
            repository.generateNewBlock()
        }
        val receivedBlock = Block(
            blockchain.size,
            blockchain.last().hash,
            sha256("Received block").dropLast(6).plus("000000"),
            "receivedBlock", 0)

        runBlocking {
            repository.handleReceivedBlock(receivedBlock, node1Port)
        }
        assertEquals(receivedBlock, blockchain.last())
    }

    private fun initMainNode() {
        blockchain.clear()
        currentNodePort = "8080"
        node1Port = "8081"
        node2Port = "8082"
        mainNode = true
    }
}
