package com.eriksargsyan.blockchain.plugins

import com.eriksargsyan.blockchain.data.Repository.Companion.sendLastBlock
import com.eriksargsyan.blockchain.util.Constants.ASK_THIRD_NODE_ROUTING
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureAskThirdNodeReceiverRouting() {
    routing {
        get(ASK_THIRD_NODE_ROUTING) {
            call.respond(sendLastBlock())
        }
    }
}