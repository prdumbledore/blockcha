package com.eriksargsyan.blockchain

import com.eriksargsyan.blockchain.Vars.currentNodePort
import com.eriksargsyan.blockchain.Vars.mainNode
import com.eriksargsyan.blockchain.Vars.node1Port
import com.eriksargsyan.blockchain.Vars.node2Port
import com.eriksargsyan.blockchain.Vars.repository
import com.eriksargsyan.blockchain.Vars.url1
import com.eriksargsyan.blockchain.Vars.url2
import com.eriksargsyan.blockchain.data.Blockchain.blockchain
import com.eriksargsyan.blockchain.data.Blockchain.generateGenesis
import com.eriksargsyan.blockchain.data.NotificationReceivedCallback
import com.eriksargsyan.blockchain.data.Repository
import com.eriksargsyan.blockchain.data.Repository.Companion.generateNewBlock
import com.eriksargsyan.blockchain.data.Repository.Companion.validateBlockChain
import com.eriksargsyan.blockchain.plugins.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import kotlinx.coroutines.*
import java.net.ConnectException


fun main(args: Array<String>) {
    currentNodePort = args[0]
    node1Port = args[1]
    node2Port = args[2]
    mainNode = args[3] == "1"
    println(node1Port)
    println(node2Port)

    embeddedServer(CIO, port = currentNodePort.toInt(), host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureSerialization()
    url1 = when (node1Port) {
        "8081" -> "http://server2:8081"
        "8082" -> "http://server3:8082"
        else -> "http://server1:8080"
    }
    url2  = when (node2Port) {
        "8081" -> "http://server2:8081"
        "8082" -> "http://server3:8082"
        else -> "http://server1:8080"
    }

    if (mainNode) {
        generateGenesis()
    }

    configureAskThirdNodeReceiverRouting()
    configureRouting()
    configureValidationReceiverRouting()
    var job = if (mainNode) {
        startMining(this)
    } else { // if current node isn't main it just asks for actual blockChain every 0.5 second until gets it
        waitForMainNode(this)
    }

    configureNotificationReceiverRouting(object : NotificationReceivedCallback {
        override fun onNotificationReceived() {
            println("Notification received => restarting coroutine and mining")
            job.cancel()
            job = startMining(this@module)
        }

    })
}

private fun startMining(coroutineScope: CoroutineScope): Job {
    return coroutineScope.launch(Dispatchers.Default + CoroutineExceptionHandler { _, _ ->  }) {
        while (true) {

            repository.generateNewBlock()
        }
    }
}

private fun waitForMainNode(coroutineScope: CoroutineScope): Job {
    return coroutineScope.launch(Dispatchers.IO + CoroutineExceptionHandler { _, _ ->  }) {
        while (blockchain.isEmpty()) {
            try {
                repository.validateBlockChain()
            } catch (e: ConnectException) {
                e.printStackTrace()
            }
            delay(500)
        }
        cancel()
        startMining(coroutineScope)
    }
}

object Vars {
    var node1Port = ""
    var node2Port = ""
    var currentNodePort = ""
    var mainNode = false
    val repository by lazy { Repository.get() }
    var url1 = ""
    var url2 = ""
}
