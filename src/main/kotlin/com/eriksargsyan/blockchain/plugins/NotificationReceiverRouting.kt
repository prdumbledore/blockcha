package com.eriksargsyan.blockchain.plugins

import com.eriksargsyan.blockchain.Vars.repository
import com.eriksargsyan.blockchain.data.Block
import com.eriksargsyan.blockchain.data.NotificationReceivedCallback
import com.eriksargsyan.blockchain.data.Repository.Companion.handleReceivedBlock
import com.eriksargsyan.blockchain.util.Constants.BLOCK_INSERTED_ROUTING
import com.eriksargsyan.blockchain.util.Constants.PORT
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureNotificationReceiverRouting(notificationReceivedCallback: NotificationReceivedCallback) {
    routing {
        post(BLOCK_INSERTED_ROUTING) {
            val block = call.receive(Block::class)
            val senderNodePort = call.request.headers[PORT]
            println("Notification from other node. It generated block $block")
            if (senderNodePort != null) {
                repository.handleReceivedBlock(block, senderNodePort, notificationReceivedCallback)
            }
            call.respond(1)
        }
    }
}