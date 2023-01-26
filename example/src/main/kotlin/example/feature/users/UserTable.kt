package example.feature.users

import com.github.darkxanter.kesp.annotation.ExposedTable
import com.github.darkxanter.kesp.annotation.Projection
import example.feature.auth.Role
import org.jetbrains.exposed.dao.id.LongIdTable

@ExposedTable
@Projection(User::class)
object UserTable: LongIdTable("users") {
    val name = varchar("name", 255)
    val username = varchar("username", 255).uniqueIndex()
    val password = varchar("password", 255)
    val role = enumerationByName<Role>("role", 12)
}
