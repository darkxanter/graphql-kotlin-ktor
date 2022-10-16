package example.feature.users

import example.feature.auth.Role

class UserRepository {
    fun list() = listOf(
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

    fun findUserByCredentials(username: String, password: String): User? {
        return list().find {
            it.username == username && it.password == password
        }
    }

    fun findUserById(userId: Long): User? = list().find {
        it.id == userId
    }
}
