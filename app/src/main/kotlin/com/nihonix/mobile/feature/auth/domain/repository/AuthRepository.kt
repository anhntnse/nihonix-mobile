package com.nihonix.mobile.feature.auth.domain.repository

import com.nihonix.mobile.core.result.Resource
import com.nihonix.mobile.feature.auth.domain.model.ResetCodeVerification
import com.nihonix.mobile.feature.auth.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUserFlow: StateFlow<User?>

    fun isLoggedIn(): Boolean

    fun login(username: String, password: String): Flow<Resource<User>>

    fun register(
        username: String,
        email: String,
        password: String,
        fullName: String? = null
    ): Flow<Resource<User>>

    fun logout(): Flow<Resource<Unit>>

    fun getCurrentUser(): Flow<Resource<User>>

    fun forgotPassword(email: String): Flow<Resource<String>>

    fun verifyResetCode(email: String, code: String): Flow<Resource<ResetCodeVerification>>

    fun resetPassword(token: String, password: String): Flow<Resource<String>>

    fun resendCode(email: String): Flow<Resource<String>>
}
