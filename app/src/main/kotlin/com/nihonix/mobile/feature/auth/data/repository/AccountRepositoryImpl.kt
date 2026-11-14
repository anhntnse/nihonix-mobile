package com.nihonix.mobile.feature.auth.data.repository

import com.google.gson.Gson
import com.nihonix.mobile.core.network.dto.ApiResponseDto
import com.nihonix.mobile.core.network.util.safeApiCall
import com.nihonix.mobile.core.result.Resource
import com.nihonix.mobile.core.storage.TokenStorage
import com.nihonix.mobile.core.storage.UserStorage
import com.nihonix.mobile.feature.auth.data.api.AccountApiService
import com.nihonix.mobile.feature.auth.data.dto.*
import com.nihonix.mobile.feature.auth.domain.model.LearningSettings
import com.nihonix.mobile.feature.auth.domain.model.User
import com.nihonix.mobile.feature.auth.domain.repository.AccountRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response

class AccountRepositoryImpl(
    private val api: AccountApiService,
    private val tokens: TokenStorage,
    private val users: UserStorage,
    private val gson: Gson
) : AccountRepository {
    private fun <T, R> request(call: suspend () -> Response<ApiResponseDto<T>>, map: (T) -> R): Flow<Resource<R>> = flow {
        emit(Resource.Loading)
        emit(safeApiCall(gson, { call() }, { map(it) }))
    }.flowOn(Dispatchers.IO)

    override fun requestVerification(email: String) = request(
        { api.requestVerification(VerificationEmailDto(email.trim())) },
        { it.retryAfterSeconds.coerceIn(0, 600) }
    )

    override fun verifyEmail(email: String, code: String) = request(
        { api.verifyEmail(VerifyEmailRequestDto(email.trim(), code.trim())) }, { Unit }
    )

    override fun updateProfile(username: String, fullName: String) = request(
        { api.updateProfile(UpdateProfileRequestDto(username.trim(), fullName.trim())) },
        { cacheUser(it.toDomain()) }
    )

    override fun getSettings() = request({ api.getSettings() }, { it.toDomain() })
    override fun updateSettings(settings: LearningSettings) = request(
        { api.updateSettings(LearningSettingsDto.fromDomain(settings)) }, { it.toDomain() }
    )

    override fun uploadAvatar(bytes: ByteArray, mimeType: String): Flow<Resource<User>> {
        if (bytes.isEmpty() || bytes.size > 10 * 1024 * 1024 ||
            mimeType !in setOf("image/jpeg", "image/png", "image/webp")
        ) return flow { emit(Resource.Error("Choose a JPEG, PNG or WebP image up to 10 MB.")) }
        val part = MultipartBody.Part.createFormData("avatar", "avatar", bytes.toRequestBody(mimeType.toMediaType()))
        return request({ api.uploadAvatar(part) }, {
            val current = users.getUser() ?: error("Sign in again to update your avatar.")
            cacheUser(current.copy(avatarUrl = it.avatarUrl))
        })
    }

    override fun deleteAccount(): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        val id = users.getUser()?.id
        if (id.isNullOrBlank() || !tokens.hasAccessToken()) {
            emit(Resource.Error("Sign in again before deleting your account."))
            return@flow
        }
        try {
            val response = api.deleteAccount(id)
            if (response.isSuccessful) {
                tokens.clearTokens()
                users.clearUser()
                emit(Resource.Success(Unit, "Account deleted"))
            } else {
                emit(Resource.Error("Account could not be deleted. Please try again.", response.code()))
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            emit(Resource.Error("Unable to delete your account. Check your connection and try again."))
        }
    }.flowOn(Dispatchers.IO)

    private fun cacheUser(user: User): User {
        check(tokens.hasAccessToken() && users.getUser()?.id == user.id) { "Your session changed. Sign in again." }
        users.saveUser(user)
        return user
    }
}
