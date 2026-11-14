package com.nihonix.mobile.feature.auth.data.api

import com.nihonix.mobile.core.network.ApiConstants
import com.nihonix.mobile.core.network.dto.ApiResponseDto
import com.nihonix.mobile.feature.auth.data.dto.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface AccountApiService {
    @POST("auth/request-email-verification")
    @Headers("${ApiConstants.HEADER_NO_AUTH}: true")
    suspend fun requestVerification(@Body body: VerificationEmailDto): Response<ApiResponseDto<VerificationDeliveryDto>>

    @POST("auth/verify-email")
    @Headers("${ApiConstants.HEADER_NO_AUTH}: true")
    suspend fun verifyEmail(@Body body: VerifyEmailRequestDto): Response<ApiResponseDto<MessageResponseDto>>

    @PATCH("users/me/profile")
    suspend fun updateProfile(@Body body: UpdateProfileRequestDto): Response<ApiResponseDto<UserDto>>

    @GET("users/me/settings")
    suspend fun getSettings(): Response<ApiResponseDto<LearningSettingsDto>>

    @PATCH("users/me/settings")
    suspend fun updateSettings(@Body body: LearningSettingsDto): Response<ApiResponseDto<LearningSettingsDto>>

    @Multipart @POST("users/me/avatar")
    suspend fun uploadAvatar(@Part avatar: MultipartBody.Part): Response<ApiResponseDto<AvatarResponseDto>>

    @DELETE("users/{id}")
    suspend fun deleteAccount(@Path("id") id: String): Response<Unit>
}
