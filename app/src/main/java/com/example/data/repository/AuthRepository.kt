package com.example.data.repository

import com.example.data.local.UserDao
import com.example.data.local.UserEntity
import com.example.data.security.SecurityHelper
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.ConcurrentHashMap

data class OtpChallenge(
    val salt: String,
    val hashedOtp: String,
    val expiresAt: Long,
    val plainOtpForSimulation: String // For realistic SMS notification preview in UI
)

class AuthRepository(private val userDao: UserDao) {

    // In-memory secure OTP challenge map (phone -> OtpChallenge)
    private val activeOtpChallenges = ConcurrentHashMap<String, OtpChallenge>()

    fun getActiveUser(): Flow<UserEntity?> = userDao.getActiveUser()

    suspend fun getActiveUserDirect(): UserEntity? = userDao.getActiveUserDirect()

    suspend fun checkUserExists(phoneNumber: String): Boolean {
        return userDao.getUserByPhone(phoneNumber) != null
    }

    /**
     * Dispatches a 6-digit OTP code to the mobile number.
     * The OTP is hashed with a cryptographic salt and stored with a 5-minute expiry.
     * Returns the simulated OTP code for UI delivery notification.
     */
    fun sendOtp(phoneNumber: String): String {
        val plainOtp = SecurityHelper.generateOtp()
        val salt = SecurityHelper.generateSalt()
        val hashedOtp = SecurityHelper.hashWithSalt(plainOtp, salt)
        val expiresAt = System.currentTimeMillis() + (5 * 60 * 1000) // 5 minutes

        activeOtpChallenges[phoneNumber] = OtpChallenge(
            salt = salt,
            hashedOtp = hashedOtp,
            expiresAt = expiresAt,
            plainOtpForSimulation = plainOtp
        )

        return plainOtp
    }

    /**
     * Verifies the OTP and logs in an existing user.
     */
    suspend fun verifyOtpAndLogin(phoneNumber: String, enteredOtp: String): Result<UserEntity> {
        val challenge = activeOtpChallenges[phoneNumber]
            ?: return Result.failure(IllegalArgumentException("No OTP was requested for this phone number. Please request an OTP."))

        if (System.currentTimeMillis() > challenge.expiresAt) {
            activeOtpChallenges.remove(phoneNumber)
            return Result.failure(IllegalStateException("OTP has expired. Please request a new code."))
        }

        val isValid = SecurityHelper.verifyHash(enteredOtp.trim(), challenge.salt, challenge.hashedOtp)
        if (!isValid) {
            return Result.failure(IllegalArgumentException("Invalid verification code. Please check and try again."))
        }

        // Clean up challenge upon successful verification
        activeOtpChallenges.remove(phoneNumber)

        val existingUser = userDao.getUserByPhone(phoneNumber)
            ?: return Result.failure(IllegalStateException("Account not found with this mobile number. Please Sign Up first."))

        // Generate new secure session token
        val sessionToken = SecurityHelper.generateSessionToken()
        val now = System.currentTimeMillis()

        userDao.logoutAll()
        userDao.updateLoginSession(
            phoneNumber = phoneNumber,
            isLoggedIn = true,
            sessionToken = sessionToken,
            loginTime = now
        )

        return Result.success(existingUser.copy(isLoggedIn = true, sessionToken = sessionToken, lastLoginAt = now))
    }

    /**
     * Verifies the OTP and registers a new user with secure salted credential storage.
     */
    suspend fun signUpWithOtp(
        phoneNumber: String,
        name: String,
        countryCode: String,
        emoji: String,
        colorHex: String,
        enteredOtp: String
    ): Result<UserEntity> {
        val challenge = activeOtpChallenges[phoneNumber]
            ?: return Result.failure(IllegalArgumentException("No OTP was requested for this phone number. Please request an OTP."))

        if (System.currentTimeMillis() > challenge.expiresAt) {
            activeOtpChallenges.remove(phoneNumber)
            return Result.failure(IllegalStateException("OTP has expired. Please request a new code."))
        }

        val isValid = SecurityHelper.verifyHash(enteredOtp.trim(), challenge.salt, challenge.hashedOtp)
        if (!isValid) {
            return Result.failure(IllegalArgumentException("Invalid verification code. Please check and try again."))
        }

        activeOtpChallenges.remove(phoneNumber)

        // Generate persistent cryptographic salt and credential hash for this user
        val userSalt = SecurityHelper.generateSalt()
        val credentialHash = SecurityHelper.hashWithSalt(phoneNumber + name, userSalt)
        val sessionToken = SecurityHelper.generateSessionToken()
        val now = System.currentTimeMillis()

        val newUser = UserEntity(
            phoneNumber = phoneNumber,
            name = name.trim(),
            countryCode = countryCode,
            avatarEmoji = emoji,
            avatarColorHex = colorHex,
            salt = userSalt,
            credentialHash = credentialHash,
            sessionToken = sessionToken,
            isLoggedIn = true,
            createdAt = now,
            lastLoginAt = now
        )

        userDao.logoutAll()
        userDao.insertUser(newUser)

        return Result.success(newUser)
    }

    suspend fun logout() {
        userDao.logoutAll()
    }

    suspend fun updateProfile(phoneNumber: String, name: String, emoji: String, colorHex: String) {
        val current = userDao.getUserByPhone(phoneNumber) ?: return
        userDao.updateUser(current.copy(name = name, avatarEmoji = emoji, avatarColorHex = colorHex))
    }

    fun getAllUsers(): Flow<List<UserEntity>> = userDao.getAllUsers()

    fun getOtherUsers(excludePhone: String): Flow<List<UserEntity>> = userDao.getOtherUsers(excludePhone)

    suspend fun ensureDefaultUsers() {
        if (userDao.getUserCount() <= 1) {
            val defaultUsers = listOf(
                UserEntity(
                    phoneNumber = "+15552345678",
                    name = "Elena Rostova",
                    countryCode = "+1",
                    avatarEmoji = "🎨",
                    avatarColorHex = "#EC4899",
                    salt = "salt_elena",
                    credentialHash = "hash_elena",
                    isLoggedIn = false,
                    isOnline = true,
                    bio = "Product Designer & UI enthusiast ☕"
                ),
                UserEntity(
                    phoneNumber = "+15553456789",
                    name = "Marcus Vance",
                    countryCode = "+1",
                    avatarEmoji = "💻",
                    avatarColorHex = "#3B82F6",
                    salt = "salt_marcus",
                    credentialHash = "hash_marcus",
                    isLoggedIn = false,
                    isOnline = true,
                    bio = "Distributed systems architect. Coffee -> Code 🚀"
                ),
                UserEntity(
                    phoneNumber = "+15554567890",
                    name = "Aria Patel",
                    countryCode = "+1",
                    avatarEmoji = "🚀",
                    avatarColorHex = "#8B5CF6",
                    salt = "salt_aria",
                    credentialHash = "hash_aria",
                    isLoggedIn = false,
                    isOnline = true,
                    bio = "Mobile Engineer building delightful mobile apps ✨"
                ),
                UserEntity(
                    phoneNumber = "+819012345678",
                    name = "Kai Takahashi",
                    countryCode = "+81",
                    avatarEmoji = "🎧",
                    avatarColorHex = "#10B981",
                    salt = "salt_kai",
                    credentialHash = "hash_kai",
                    isLoggedIn = false,
                    isOnline = false,
                    bio = "Audio engineer & Tokyo soundscapes 🎵"
                ),
                UserEntity(
                    phoneNumber = "+33612345678",
                    name = "Chloe Dubois",
                    countryCode = "+33",
                    avatarEmoji = "🌿",
                    avatarColorHex = "#F59E0B",
                    salt = "salt_chloe",
                    credentialHash = "hash_chloe",
                    isLoggedIn = false,
                    isOnline = true,
                    bio = "Nature researcher, photographer & hiker 🏔️"
                )
            )
            userDao.insertUsers(defaultUsers)
        }
    }
}
