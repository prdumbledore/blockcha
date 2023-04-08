package com.eriksargsyan.blockchain.plugins

import com.eriksargsyan.blockchain.data.Blockchain.blockchain
import com.eriksargsyan.blockchain.util.Constants.VALIDATE_BLOCKCHAIN_ROUTING
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureValidationReceiverRouting() {
    routing {
        get(VALIDATE_BLOCKCHAIN_ROUTING) {
            call.respond(blockchain)
        }
    }
}