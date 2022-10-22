package example.feature.users

import example.feature.auth.Role
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ResultRow

object UserTable: LongIdTable("users") {
    val name = varchar("name", 255)
    val username = varchar("username", 255).uniqueIndex()
    val password = varchar("password", 255)
    val role = enumerationByName<Role>("role", 12)
}

fun ResultRow.toUser() = User(
    id =  this[UserTable.id].value,
    name =  this[UserTable.name],
    username =  this[UserTable.username],
    password =  this[UserTable.password],
    role =  this[UserTable.role],
)
