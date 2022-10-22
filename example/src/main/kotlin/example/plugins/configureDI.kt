package example.plugins

import example.feature.articles.ArticleRepository
import example.feature.users.UserRepository
import io.ktor.server.application.Application
import org.kodein.di.bindSingleton
import org.kodein.di.ktor.di
import org.kodein.di.new

fun Application.configureDI() {
    di {
        bindSingleton { new(::ArticleRepository) }
        bindSingleton { new(::UserRepository) }
    }
}
