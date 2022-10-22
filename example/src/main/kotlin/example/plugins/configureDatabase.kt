package example.plugins

import example.feature.articles.ArticleTable
import example.feature.auth.Role
import example.feature.users.User
import example.feature.users.UserTable
import io.ktor.server.application.Application
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabase() {
    Database.connect("jdbc:sqlite:./example.db", "org.sqlite.JDBC")

    TransactionManager.manager.defaultIsolationLevel = java.sql.Connection.TRANSACTION_SERIALIZABLE

    val users = listOf(
        User(
            id = 1,
            name = "Admin",
            username = "admin",
            password = "1234",
            role = Role.Admin,
        ),
        User(
            id = 2,
            name = "Manager",
            username = "manager",
            password = "1234",
            role = Role.Manager,
        ),
        User(
            id = 3,
            name = "Some User",
            username = "user",
            password = "1234",
            role = Role.User,
        )
    )

    transaction {
        SchemaUtils.createMissingTablesAndColumns(
            UserTable,
            ArticleTable,
        )

        if (UserTable.selectAll().empty()) {
            UserTable.batchInsert(users) {
                set(UserTable.name, it.name)
                set(UserTable.username, it.username)
                set(UserTable.password, it.password)
                set(UserTable.role, it.role)
            }
        }

        if (ArticleTable.selectAll().empty()) {
            ArticleTable.insert {
                it[authorId] = 1
                it[title] = "Example title"
                it[content] = "Example content"
            }
            ArticleTable.insert {
                it[authorId] = 2
                it[title] = "Example title 2"
                it[content] = "Example content 2"
            }
            ArticleTable.insert {
                it[authorId] = 1
                it[title] = "Example title 3"
                it[content] = "Example content 3"
            }
        }
    }
}
