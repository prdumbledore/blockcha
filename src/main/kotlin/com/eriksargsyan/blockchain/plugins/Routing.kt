package com.eriksargsyan.blockchain.plugins

import com.eriksargsyan.blockchain.data.Blockchain.blockchain
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun Application.configureRouting() {
    routing {
        get("/") {
            println(blockchain.map { listOf(it.index, it.prevHash, it.hash).joinToString(separator = " ") })
            call.respond(blockchain.map { listOf(it.index, it.prevHash, it.hash).joinToString(separator = " ") })
        }
    }


}
