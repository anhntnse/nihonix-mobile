package com.nihonix.mobile.feature.notifications
import com.nihonix.mobile.core.network.dto.ApiResponseDto
import com.nihonix.mobile.core.network.interceptor.SessionRequest
import retrofit2.Response
import retrofit2.http.*
data class DeviceRegistration(val token:String,val enabled:Boolean)
data class DeviceBinding(val installation_id:String,val binding_id:String,val enabled:Boolean)
interface NotificationsApi{
 @PUT("devices/{id}")suspend fun register(@Path("id")id:String,@Body body:DeviceRegistration,@Tag session:SessionRequest):Response<ApiResponseDto<DeviceBinding>>
 @DELETE("devices/{id}")suspend fun remove(@Path("id")id:String,@Tag session:SessionRequest):Response<Unit>
}
