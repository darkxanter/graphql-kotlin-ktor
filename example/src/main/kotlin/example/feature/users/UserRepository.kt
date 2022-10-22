package example.feature.users

import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class UserRepository {
    fun listByIds(ids: List<Long>): List<User> {
        return transaction {
            UserTable.select {
                UserTable.id inList ids
            }.map { row ->
                row.toUser()
            }
        }
    }


    fun list(): List<User> {
        return transaction {
            UserTable.selectAll().map { row ->
                row.toUser()
            }
        }
    }

    fun findUserByCredentials(username: String, password: String): User? {
        return transaction {
            UserTable.select {
                UserTable.username eq username
            }.andWhere {
                UserTable.password eq password
            }.singleOrNull()?.toUser()
        }
    }

    fun findUserById(userId: Long): User? {
        return transaction {
            UserTable.select {
                UserTable.id eq userId
            }.singleOrNull()?.toUser()
        }
    }
}
