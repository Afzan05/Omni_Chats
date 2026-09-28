package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val phoneNumber: String, // Normalized phone number with country code, e.g. "+15551234567"
    val name: String,
    val countryCode: String = "+1",
    val avatarEmoji: String = "😎",
    val avatarColorHex: String = "#4F46E5",
    val salt: String, // Cryptographic random salt
    val credentialHash: String, // Securely salted SHA-256 hash of credential
    val sessionToken: String? = null, // Cryptographic session token
    val isLoggedIn: Boolean = false,
    val isOnline: Boolean = true,
    val bio: String = "Hey there! I am using OmniChat.",
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
)
