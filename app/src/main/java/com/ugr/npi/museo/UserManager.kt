package com.ugr.npi.museo

import android.content.Context
import java.io.File

data class User(
    val username: String,
    val password: String, // Storing plain text as requested for the demo
    val email: String,
    var biometricEnabled: Boolean = false,
    var points: Int = 0
)

object UserManager {
    private const val FILE_NAME = "users.txt"
    private var currentUser: User? = null

    // Format per line: username,password,email,biometricEnabled,points

    fun register(context: Context, user: User): Boolean {
        if (userExists(context, user.username)) return false

        val file = File(context.filesDir, FILE_NAME)
        file.appendText("${user.username},${user.password},${user.email},${user.biometricEnabled},${user.points}\n")
        currentUser = user
        return true
    }

    fun login(context: Context, username: String, password: String): Boolean {
        val users = readUsers(context)
        val user = users.find { it.username == username && it.password == password }
        if (user != null) {
            currentUser = user
            return true
        }
        return false
    }

    fun loginWithBiometric(context: Context): Boolean {
        // Deprecated or simplified check. Use getBiometricUser for flow.
        return getBiometricUser(context) != null
    }

    fun getBiometricUser(context: Context): User? {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val username = prefs.getString("biometric_user", null) ?: return null
        val users = readUsers(context)
        return users.find { it.username == username && it.biometricEnabled }
    }
    
    fun getCurrentUser(): User? = currentUser

    fun logout() {
        currentUser = null
    }

    fun addPoints(context: Context, amount: Int) {
        val user = currentUser ?: return
        user.points += amount
        updateUserInFile(context, user)
    }

    fun deductPoints(context: Context, amount: Int): Boolean {
        val user = currentUser ?: return false
        if (user.points >= amount) {
            user.points -= amount
            updateUserInFile(context, user)
            return true
        }
        return false
    }

    fun enableBiometric(context: Context): Boolean {
        val user = currentUser ?: return false
        user.biometricEnabled = true
        updateUserInFile(context, user)
        
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("biometric_user", user.username)
            .apply()
            
        return true
    }

    private fun userExists(context: Context, username: String): Boolean {
        return readUsers(context).any { it.username == username }
    }

    private fun readUsers(context: Context): List<User> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return emptyList()

        return file.readLines().mapNotNull { line ->
            val parts = line.split(",")
            if (parts.size >= 4) {
                // Backward compatibility: if 4 parts, points = 0. If 5 parts, read points.
                val points = if (parts.size >= 5) parts[4].toIntOrNull() ?: 0 else 0
                User(parts[0], parts[1], parts[2], parts[3].toBoolean(), points)
            } else {
                null
            }
        }
    }

    private fun updateUserInFile(context: Context, updatedUser: User) {
        val users = readUsers(context).toMutableList()
        val index = users.indexOfFirst { it.username == updatedUser.username }
        if (index != -1) {
            users[index] = updatedUser
            val file = File(context.filesDir, FILE_NAME)
            file.writeText("") // Clear file
            users.forEach { user ->
                file.appendText("${user.username},${user.password},${user.email},${user.biometricEnabled},${user.points}\n")
            }
        }
    }
}
