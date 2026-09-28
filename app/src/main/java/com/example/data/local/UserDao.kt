package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE isLoggedIn = 1 LIMIT 1")
    fun getActiveUser(): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE isLoggedIn = 1 LIMIT 1")
    suspend fun getActiveUserDirect(): UserEntity?

    @Query("SELECT * FROM users WHERE phoneNumber = :phoneNumber LIMIT 1")
    suspend fun getUserByPhone(phoneNumber: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET isLoggedIn = :isLoggedIn, sessionToken = :sessionToken, lastLoginAt = :loginTime WHERE phoneNumber = :phoneNumber")
    suspend fun updateLoginSession(phoneNumber: String, isLoggedIn: Boolean, sessionToken: String?, loginTime: Long)

    @Query("UPDATE users SET isLoggedIn = 0, sessionToken = NULL")
    suspend fun logoutAll()

    @Query("DELETE FROM users WHERE phoneNumber = :phoneNumber")
    suspend fun deleteUser(phoneNumber: String)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE phoneNumber != :excludePhone ORDER BY name ASC")
    fun getOtherUsers(excludePhone: String): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUsers(users: List<UserEntity>)
}
