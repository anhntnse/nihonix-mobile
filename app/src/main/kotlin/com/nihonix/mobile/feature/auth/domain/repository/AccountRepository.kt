package com.nihonix.mobile.feature.auth.domain.repository

import com.nihonix.mobile.core.result.Resource
import com.nihonix.mobile.feature.auth.domain.model.LearningSettings
import com.nihonix.mobile.feature.auth.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun requestVerification(email: String): Flow<Resource<Int>>
    fun verifyEmail(email: String, code: String): Flow<Resource<Unit>>
    fun updateProfile(username: String, fullName: String): Flow<Resource<User>>
    fun getSettings(): Flow<Resource<LearningSettings>>
    fun updateSettings(settings: LearningSettings): Flow<Resource<LearningSettings>>
    fun uploadAvatar(bytes: ByteArray, mimeType: String): Flow<Resource<User>>
    fun deleteAccount(): Flow<Resource<Unit>>
}
