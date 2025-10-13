package com.example.mindweaverstudio.di

import com.example.mindweaverstudio.data.clients.GithubClient
import org.koin.dsl.module

val clientsModule = module {
    factory<GithubClient> {
        GithubClient(
            config = get()
        )
    }
}