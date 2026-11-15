package com.nihonix.mobile.feature.auth.data.repository

import com.google.gson.Gson
import com.nihonix.mobile.core.network.util.safeApiCall
import com.nihonix.mobile.core.result.Resource
import com.nihonix.mobile.core.storage.TokenStorage
import com.nihonix.mobile.core.storage.UserStorage
import com.nihonix.mobile.feature.auth.data.api.AuthApiService
import com.nihonix.mobile.feature.auth.data.dto.*
import com.nihonix.mobile.feature.auth.domain.model.ResetCodeVerification
import com.nihonix.mobile.feature.auth.domain.model.User
import com.nihonix.mobile.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class AuthRepositoryImpl(
    private val apiService: AuthApiService,
    private val tokenStorage: TokenStorage,
    private val userStorage: UserStorage,
    private val gson: Gson,
    private val beforeLogout: suspend () -> Unit = {}
) : AuthRepository {

    override val currentUserFlow: StateFlow<User?> = userStorage.currentUserFlow

    override fun isLoggedIn(): Boolean = tokenStorage.hasAccessToken()

    override fun login(username: String, password: String): Flow<Resource<User>> = flow {
        emit(Resource.Loading)
        val result = safeApiCall(
            gson = gson,
            apiCall = { apiService.login(LoginRequestDto(username.trim(), password)) },
            transform = { loginData ->
                val user = loginData.user.toDomain()
                synchronized(userStorage) {
                    try {
                        tokenStorage.saveTokens(loginData.accessToken, loginData.refreshToken)
                        userStorage.saveUser(user)
                    } catch (error: Exception) {
                        tokenStorage.clearTokens()
                        userStorage.clearUser()
                        throw error
                    }
                }
                user
            }
        )
        emit(result)
    }.flowOn(Dispatchers.IO)

    override fun register(
        username: String,
        email: String,
        password: String,
        fullName: String?
    ): Flow<Resource<User>> = flow {
        emit(Resource.Loading)
        val result = safeApiCall(
            gson = gson,
            apiCall = {
                apiService.register(
                    RegisterRequestDto(
                        username = username.trim(),
                        email = email.trim(),
                        password = password,
                        fullName = fullName?.trim()?.ifBlank { null }
                    )
                )
            },
            transform = { it.user.toDomain() }
        )
        emit(result)
    }.flowOn(Dispatchers.IO)

    override fun logout(): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        val owner=userStorage.getUser()?.id
        val session=com.nihonix.mobile.core.network.interceptor.accessSessionId(tokenStorage.getAccessToken())
        fun unchanged()=userStorage.getUser()?.id==owner && (session==null || session==com.nihonix.mobile.core.network.interceptor.accessSessionId(tokenStorage.getAccessToken()))
        var replaced=false
        try {
            beforeLogout()
            if(!unchanged())replaced=true
            else {
                val refreshToken=tokenStorage.getRefreshToken().orEmpty()
                if(refreshToken.isNotBlank())apiService.logout(LogoutRequestDto(refreshToken),com.nihonix.mobile.core.network.interceptor.SessionRequest(owner,session))
            }
        } catch (_: Exception) {
            // Offline logout still hides local credentials and notification binding.
        } finally {
            synchronized(userStorage) {
                if(unchanged()){tokenStorage.clearTokens();userStorage.clearUser()}else replaced=true
            }
        }
        if(replaced)emit(Resource.Error("Session changed. Your current sign-in was kept."))
        else emit(Resource.Success(Unit, "Logged out successfully"))
    }.flowOn(Dispatchers.IO)

    override fun getCurrentUser(): Flow<Resource<User>> = flow {
        emit(Resource.Loading)
        val initialUser = userStorage.getUser()
        val initialToken = tokenStorage.getAccessToken()
        val result = safeApiCall(
            gson = gson,
            apiCall = { apiService.getProfile() },
            transform = { dto ->
                val remote = dto.toDomain()
                val current = userStorage.getUser()
                val sameAccount = current?.id == remote.id || (current == null && tokenStorage.getAccessToken() == initialToken)
                check(tokenStorage.hasAccessToken() && sameAccount) { "Your session changed. Sign in again." }
                // A profile/level edit completed while this fetch was in flight.
                if (current != null && current != initialUser) current else remote
            }
        )

        if (result is Resource.Success && tokenStorage.hasAccessToken() &&
            (userStorage.getUser()?.id == result.data.id ||
                (userStorage.getUser() == null && tokenStorage.getAccessToken() == initialToken))
        ) {
            userStorage.saveUser(result.data)
        }
        emit(result)
    }.flowOn(Dispatchers.IO)

    override fun forgotPassword(email: String): Flow<Resource<String>> = flow {
        emit(Resource.Loading)
        val result = safeApiCall(
            gson = gson,
            apiCall = { apiService.forgotPassword(ForgotPasswordRequestDto(email.trim())) },
            transform = { it.message ?: "Reset code sent" }
        )
        emit(result)
    }.flowOn(Dispatchers.IO)

    override fun verifyResetCode(email: String, code: String): Flow<Resource<ResetCodeVerification>> = flow {
        emit(Resource.Loading)
        val result = safeApiCall(
            gson = gson,
            apiCall = { apiService.verifyResetCode(VerifyResetCodeRequestDto(email.trim(), code.trim())) },
            transform = { it.toDomain() }
        )
        if (result is Resource.Success) {
            emit(Resource.Success(result.data.copy(message = result.message ?: result.data.message), result.message))
        } else {
            emit(result)
        }
    }.flowOn(Dispatchers.IO)

    override fun resetPassword(token: String, password: String): Flow<Resource<String>> = flow {
        emit(Resource.Loading)
        val result = safeApiCall(
            gson = gson,
            apiCall = { apiService.resetPassword(ResetPasswordRequestDto(token, password)) },
            transform = { it.message ?: "Password reset successfully" }
        )
        emit(result)
    }.flowOn(Dispatchers.IO)

    override fun resendCode(email: String): Flow<Resource<String>> = flow {
        emit(Resource.Loading)
        val result = safeApiCall(
            gson = gson,
            apiCall = { apiService.resendCode(ResendCodeRequestDto(email.trim())) },
            transform = { it.message ?: "Verification code resent" }
        )
        emit(result)
    }.flowOn(Dispatchers.IO)
}
