package com.aiface.aging.di

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.aiface.aging.BuildConfig
import com.aiface.aging.data.remote.ApiService
import com.aiface.aging.data.remote.AuthInterceptor
import com.aiface.aging.data.remote.BlendingApiService
import com.aiface.aging.data.remote.FaceSwapApiService
import com.aiface.aging.data.remote.FaceSwapAuthInterceptor
import com.aiface.aging.data.remote.FramesApiService
import com.aiface.aging.data.remote.TokenAuthenticator
import com.aiface.aging.shared.Constants
import com.aiface.aging.utils.XILLI_BASE_URL
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor { message ->
            android.util.Log.d("OkHttp", message)
        }.apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    @Provides
    @Singleton
    fun provideChuckerInterceptor(@ApplicationContext context: Context): ChuckerInterceptor {
        return ChuckerInterceptor.Builder(context).build()
    }

    @Provides
    @Singleton
    @Named("new")
    fun provideNewOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
        loggingInterceptor: HttpLoggingInterceptor,
        chuckerInterceptor: ChuckerInterceptor,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .addInterceptor(loggingInterceptor)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(chuckerInterceptor)
                }
            }
            .connectTimeout(Constants.DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(Constants.DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(Constants.DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("new")
    fun provideNewRetrofit(@Named("new") okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.NEW_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideNewApiService(@Named("new") retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    /**
     * Plain client for register / get-token during 401 recovery.
     * No [TokenAuthenticator] — avoids recursive refresh loops.
     */
    @Provides
    @Singleton
    @Named("authPlain")
    fun provideAuthPlainOkHttpClient(
        authInterceptor: AuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor,
        chuckerInterceptor: ChuckerInterceptor,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(chuckerInterceptor)
                }
            }
            .connectTimeout(Constants.DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(Constants.DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(Constants.DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("authPlain")
    fun provideAuthPlainRetrofit(@Named("authPlain") okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.NEW_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("authPlain")
    fun provideAuthPlainApiService(@Named("authPlain") retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    @Provides
    @Singleton
    @Named("xilli")
    fun provideXilliOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        chuckerInterceptor: ChuckerInterceptor,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(chuckerInterceptor)
                }
            }
            .connectTimeout(Constants.DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(Constants.DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(Constants.DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("xilli")
    fun provideXilliRetrofit(@Named("xilli") okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(XILLI_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideBlendingApiService(@Named("xilli") retrofit: Retrofit): BlendingApiService {
        return retrofit.create(BlendingApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideFramesApiService(@Named("xilli") retrofit: Retrofit): FramesApiService {
        return retrofit.create(FramesApiService::class.java)
    }

    @Provides
    @Singleton
    @Named("faceswap")
    fun provideFaceSwapOkHttpClient(
        faceSwapAuthInterceptor: FaceSwapAuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor,
        chuckerInterceptor: ChuckerInterceptor,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(faceSwapAuthInterceptor)
            .addInterceptor(loggingInterceptor)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(chuckerInterceptor)
                }
            }
            .connectTimeout(Constants.FACE_SWAP_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(Constants.FACE_SWAP_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(Constants.FACE_SWAP_TIMEOUT, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("faceswap")
    fun provideFaceSwapRetrofit(@Named("faceswap") okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.FACE_SWAP_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideFaceSwapApiService(@Named("faceswap") retrofit: Retrofit): FaceSwapApiService {
        return retrofit.create(FaceSwapApiService::class.java)
    }
}
