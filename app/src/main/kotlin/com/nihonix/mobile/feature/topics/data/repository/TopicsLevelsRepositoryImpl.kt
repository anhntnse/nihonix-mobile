package com.nihonix.mobile.feature.topics.data.repository

import com.google.gson.Gson
import com.nihonix.mobile.core.network.util.safeApiCall
import com.nihonix.mobile.core.result.Resource
import com.nihonix.mobile.core.storage.TokenStorage
import com.nihonix.mobile.core.storage.UserStorage
import com.nihonix.mobile.feature.topics.data.api.TopicsLevelsApiService
import com.nihonix.mobile.feature.topics.data.dto.UpdateUserLevelRequestDto
import com.nihonix.mobile.feature.topics.domain.model.JlptLevel
import com.nihonix.mobile.feature.topics.domain.model.Topic
import com.nihonix.mobile.feature.topics.domain.model.UserLevel
import com.nihonix.mobile.feature.topics.domain.repository.TopicsLevelsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class TopicsLevelsRepositoryImpl(
    private val apiService: TopicsLevelsApiService,
    private val userStorage: UserStorage,
    private val tokenStorage: TokenStorage,
    private val gson: Gson
) : TopicsLevelsRepository {

    override fun getLevels(): Flow<Resource<List<JlptLevel>>> = flow {
        emit(Resource.Loading)
        val result = safeApiCall(
            gson = gson,
            apiCall = { apiService.getLevels() },
            transform = { list ->
                list.map { it.toDomain() }
                    .sortedWith(compareBy({ LEVEL_ORDER[it.code.uppercase()] ?: Int.MAX_VALUE }, { it.code }))
            }
        )

        when (result) {
            is Resource.Success -> {
                if (result.data.isEmpty()) {
                    emit(Resource.Empty)
                } else {
                    emit(result)
                }
            }
            else -> emit(result)
        }
    }.flowOn(Dispatchers.IO)

    override fun getLevelByCode(code: String): Flow<Resource<JlptLevel>> = flow {
        val normalizedCode = code.trim().uppercase()
        if (normalizedCode.isBlank()) {
            emit(Resource.Error("Level code cannot be empty"))
            return@flow
        }

        emit(Resource.Loading)
        val result = safeApiCall(
            gson = gson,
            apiCall = { apiService.getLevelByCode(normalizedCode) },
            transform = { it.toDomain() }
        )
        emit(result)
    }.flowOn(Dispatchers.IO)

    override fun getUserLevel(): Flow<Resource<UserLevel>> = flow {
        if (!tokenStorage.hasAccessToken()) {
            emit(Resource.Error("Authentication required", statusCode = 401))
            return@flow
        }

        emit(Resource.Loading)
        val result = safeApiCall(
            gson = gson,
            apiCall = { apiService.getUserLevel() },
            transform = { it.toDomain() }
        )

        if (result is Resource.Success) {
            syncUserLevelToStorage(result.data.levelCode)
        }
        emit(result)
    }.flowOn(Dispatchers.IO)

    override fun updateUserLevel(levelCode: String): Flow<Resource<UserLevel>> = flow {
        if (!tokenStorage.hasAccessToken()) {
            emit(Resource.Error("Authentication required", statusCode = 401))
            return@flow
        }

        val normalizedCode = levelCode.trim().uppercase()
        if (normalizedCode !in ALLOWED_LEVEL_CODES) {
            emit(Resource.Error("Level code must be one of: N5, N4, N3, N2, N1"))
            return@flow
        }

        emit(Resource.Loading)
        val result = safeApiCall(
            gson = gson,
            apiCall = { apiService.updateUserLevel(UpdateUserLevelRequestDto(normalizedCode)) },
            transform = { dto ->
                val updatedCode = dto.extractUpdatedLevelCode(normalizedCode)
                val fallbackUserId = userStorage.getUser()?.id.orEmpty()
                val mapped = dto.toUserLevelDomain(fallbackUserId)
                if (mapped.levelCode.isNullOrBlank()) {
                    mapped.copy(levelCode = updatedCode)
                } else {
                    mapped
                }
            }
        )

        if (result is Resource.Success) {
            syncUserLevelToStorage(result.data.levelCode)
        }
        emit(result)
    }.flowOn(Dispatchers.IO)

    override fun getTopics(levelCode: String?): Flow<Resource<List<Topic>>> = flow {
        emit(Resource.Loading)
        val normalizedLevel = levelCode?.trim()?.takeIf { it.isNotBlank() }?.uppercase()
        val result = safeApiCall(
            gson = gson,
            apiCall = { apiService.getTopics(normalizedLevel) },
            transform = { list ->
                list.map { it.toDomain() }
                    .sortedBy { it.order }
            }
        )

        when (result) {
            is Resource.Success -> {
                if (result.data.isEmpty()) {
                    emit(Resource.Empty)
                } else {
                    emit(result)
                }
            }
            else -> emit(result)
        }
    }.flowOn(Dispatchers.IO)

    override fun getTopicDetail(identifier: String): Flow<Resource<Topic>> = flow {
        val trimmed = identifier.trim()
        if (trimmed.isBlank()) {
            emit(Resource.Error("Topic identifier cannot be empty"))
            return@flow
        }

        emit(Resource.Loading)
        val result = safeApiCall(
            gson = gson,
            apiCall = { apiService.getTopicDetail(trimmed) },
            transform = { it.toDomain() }
        )
        emit(result)
    }.flowOn(Dispatchers.IO)

    private fun syncUserLevelToStorage(levelCode: String?) {
        val cachedUser = userStorage.getUser() ?: return
        if (cachedUser.levelCode != levelCode) {
            userStorage.saveUser(cachedUser.copy(levelCode = levelCode))
        }
    }

    companion object {
        private val LEVEL_ORDER = mapOf(
            "N5" to 0,
            "N4" to 1,
            "N3" to 2,
            "N2" to 3,
            "N1" to 4
        )
        private val ALLOWED_LEVEL_CODES = setOf("N5", "N4", "N3", "N2", "N1")
    }
}
