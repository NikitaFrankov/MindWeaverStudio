package com.example.mindweaverstudio.data.models.user

class AskUserRequest(
    val ask: String,
) {

    companion object {
        fun createRequest(ask: String) = AskUserRequest(ask = ask)
    }
}