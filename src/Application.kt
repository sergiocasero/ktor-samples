package com.tutorial

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(ContentNegotiation) {
        gson { }
    }

    user()
}

data class User(val id: Long, val name: String, val channel: String) {
    override fun equals(other: Any?): Boolean {
        if (other is User) {
            return other.id == id
        }
        return super.equals(other)
    }

    override fun hashCode(): Int = id.hashCode()
}

data class Error(val message: String)

val users = mutableListOf<User>()

fun Application.user() {
    routing {
        route("/user") {
            get { call.respond(users) }
            get("/{id}") {
                val candidate = call.parameters["id"]?.toLongOrNull()

                val result = when (candidate) {
                    null -> call.respond(HttpStatusCode.BadRequest, Error("ID must be long"))
                    else -> {
                        val user = users.firstOrNull { it.id == candidate }
                        when (user) {
                            null -> call.respond(HttpStatusCode.NotFound, Error("User with id $candidate not found"))
                            else -> call.respond(user)
                        }
                    }
                }
            }


            post {
                val candidate = call.receive<User>()

                users.add(candidate)
                call.respond(HttpStatusCode.Created)
            }
            put {
                val candidate = call.receive<User>()

                val result =
                    when (users.contains(candidate)) {
                        true -> {
                            users[users.indexOf(candidate)] = candidate
                            call.respond(HttpStatusCode.OK)
                        }
                        false -> call.respond(HttpStatusCode.NotFound, Error("User with id ${candidate.id} not found"))
                    }
            }
            delete("/{id}") {
                val candidateId = call.parameters["id"]?.toLongOrNull()

                val result = when (candidateId) {
                    null -> call.respond(HttpStatusCode.BadRequest, Error("ID must be long"))
                    else -> {
                        val user = users.firstOrNull { it.id == candidateId }
                        when (user) {
                            null -> call.respond(HttpStatusCode.NotFound, Error("User with id $candidateId not found"))
                            else -> {
                                users.remove(user)
                                call.respond(HttpStatusCode.OK, Error("User $candidateId deleted"))
                            }
                        }
                    }
                }
            }
        }
    }
}