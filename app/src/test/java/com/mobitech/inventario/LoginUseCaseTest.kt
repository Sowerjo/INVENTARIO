package com.mobitech.inventario

import at.favre.lib.crypto.bcrypt.BCrypt
import com.mobitech.inventario.data.repository.UserRepositoryImpl
import com.mobitech.inventario.domain.common.Result
import com.mobitech.inventario.domain.model.UserEntity
import com.mobitech.inventario.domain.model.UserRole
import com.mobitech.inventario.domain.usecase.LoginUserUseCase
import com.mobitech.inventario.data.local.UserDao
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeUserDao: UserDao {
    private val users = mutableListOf<UserEntity>()
    override suspend fun findByUsername(username: String): UserEntity? = users.firstOrNull { it.username == username }
    override suspend fun insert(user: UserEntity): Long { users.add(user.copy(id = if (user.id==0L) (users.size+1).toLong() else user.id)); return user.id }
    override suspend fun count(): Long = users.size.toLong()
}

class LoginUseCaseTest {
    @Test
    fun testLoginSuccessAndFailure() = runBlocking {
        val dao = FakeUserDao()
        val hash = BCrypt.withDefaults().hashToString(12, "secret".toCharArray())
        dao.insert(UserEntity(username = "john", passwordHash = hash, role = UserRole.OPERATOR))
        val repo = UserRepositoryImpl(dao)
        val uc = LoginUserUseCase(repo)
        val ok = uc("john", "secret")
        assertTrue(ok is Result.Success)
        val fail = uc("john", "x")
        assertTrue(fail is Result.Error)
        if (fail is Result.Error) assertEquals("Senha inválida", fail.message)
    }
}

