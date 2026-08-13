package com.aiface.aging.data.remote


import com.aiface.aging.data.model.CategoryResponse
import com.aiface.aging.data.model.CurrentSubscriptionResponse
import com.aiface.aging.data.model.GenerateResponse
import com.aiface.aging.data.model.GenerationsResponse
import com.aiface.aging.data.model.NewCategoryResponse
import com.aiface.aging.data.model.NewGenerateResponse
import com.aiface.aging.data.model.PlansResponse
import com.aiface.aging.data.model.Post
import com.aiface.aging.data.model.PurchaseSubscriptionRequest
import com.aiface.aging.data.model.RegisterRequest
import com.aiface.aging.data.model.RegisterResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface ApiService {

    @POST("api/users/")
    suspend fun registerUser(
        @Body request: RegisterRequest,
    ): Response<RegisterResponse>

    /**
     * Get / refresh access token for an existing device user.
     * Call only when local token is missing/expired — not on every app open.
     */
    @POST("api/users/token/")
    suspend fun getAccessToken(
        @Body request: RegisterRequest,
    ): Response<RegisterResponse>


    @GET("api/catalog/public/categories/")
    suspend fun getPublicCategories(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("include_empty") includeEmpty: Boolean = false,
    ): Response<NewCategoryResponse>


    @Multipart
    @POST("api/generation/image/generate/")
    suspend fun generateImage(
        @Part parts: List<MultipartBody.Part>,
    ): Response<NewGenerateResponse>

    /** Returns the current user's subscription (credits remaining, status, expiry). */
    @GET("api/subscriptions/current/")
    suspend fun getCurrentSubscription(): Response<CurrentSubscriptionResponse>

    /** Returns all available subscription plans for this app. */
    @GET("api/plans/")
    suspend fun getPlans(): Response<PlansResponse>

    /**
     * Activates a subscription plan for the current user.
     * Call this after a successful Google Play purchase to grant backend credits.
     */
    @POST("api/subscriptions/purchase/")
    suspend fun purchasePlan(
        @Body request: PurchaseSubscriptionRequest,
    ): Response<CurrentSubscriptionResponse>

    /** Daily check-in state keyed by device id (survives clear-data re-register). */
    @GET("api/credits/checkin/")
    suspend fun getCheckInState(
        @Query("device_id") deviceId: String,
    ): Response<com.aiface.aging.data.model.CheckInStateResponse>

    @POST("api/credits/checkin/claim/")
    suspend fun claimCheckIn(
        @Body request: com.aiface.aging.data.model.CheckInClaimRequest,
    ): Response<com.aiface.aging.data.model.CheckInClaimResponse>
}
