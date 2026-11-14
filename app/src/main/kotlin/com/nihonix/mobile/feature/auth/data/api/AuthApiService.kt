package com.nihonix.mobile.feature.auth.data.api

import com.nihonix.mobile.core.network.ApiConstants
import com.nihonix.mobile.core.network.dto.ApiResponseDto
import com.nihonix.mobile.feature.auth.data.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface AuthApiService {

    @POST("auth/register")
    @Headers("${ApiConstants.HEADER_NO_AUTH}: true")
    suspend fun register(
        @Body body: RegisterRequestDto
    ): Response<ApiResponseDto<RegisterResponseDto>>

    @POST("auth/login")
    @Headers("${ApiConstants.HEADER_NO_AUTH}: true")
    suspend fun login(
        @Body body: LoginRequestDto
    ): Response<ApiResponseDto<LoginResponseDto>>

    @POST("auth/refresh")
    @Headers("${ApiConstants.HEADER_NO_AUTH}: true")
    suspend fun refresh(
        @Body body: RefreshTokenRequestDto
    ): Response<ApiResponseDto<RefreshTokenResponseDto>>

    @POST("auth/logout")
    suspend fun logout(
        @Body body: LogoutRequestDto
    ): Response<ApiResponseDto<MessageResponseDto>>

    @POST("auth/me")
    suspend fun getProfile(): Response<ApiResponseDto<UserDto>>

    @POST("auth/forgot-password")
    @Headers("${ApiConstants.HEADER_NO_AUTH}: true")
    suspend fun forgotPassword(
        @Body body: ForgotPasswordRequestDto
    ): Response<ApiResponseDto<MessageResponseDto>>

    @POST("auth/verify-reset-code")
    @Headers("${ApiConstants.HEADER_NO_AUTH}: true")
    suspend fun verifyResetCode(
        @Body body: VerifyResetCodeRequestDto
    ): Response<ApiResponseDto<VerifyResetCodeResponseDto>>

    @POST("auth/reset-password")
    @Headers("${ApiConstants.HEADER_NO_AUTH}: true")
    suspend fun resetPassword(
        @Body body: ResetPasswordRequestDto
    ): Response<ApiResponseDto<MessageResponseDto>>

    @POST("auth/resend-code")
    @Headers("${ApiConstants.HEADER_NO_AUTH}: true")
    suspend fun resendCode(
        @Body body: ResendCodeRequestDto
    ): Response<ApiResponseDto<MessageResponseDto>>
}
