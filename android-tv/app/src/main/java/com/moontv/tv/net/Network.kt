package com.moontv.tv.net

import android.content.Context
import android.content.SharedPreferences
import com.moontv.tv.ApiConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class TokenStore(context: Context) {
    private val sp: SharedPreferences = runCatching {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "token_store_secure",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }.getOrElse {
        context.getSharedPreferences("token_store_fallback", Context.MODE_PRIVATE)
    }

    fun saveToken(token: String) { sp.edit().putString("token", token).apply() }
    fun getToken(): String? = sp.getString("token", null)
    fun clearToken() { sp.edit().remove("token").apply() }
}

class AuthInterceptor(private val tokenStore: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val token = tokenStore.getToken()
        val original: Request = chain.request()
        val builder = original.newBuilder()
        if (!token.isNullOrBlank()) {
            builder.addHeader("Authorization", "Bearer $token")
        }
        return chain.proceed(builder.build())
    }
}

object NetworkModule {
    fun createOkHttp(context: Context): OkHttpClient {
        val tokenStore = TokenStore(context)
        val logger = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStore))
            .addInterceptor { chain ->
                val resp = chain.proceed(chain.request())
                if (resp.code == 401) {
                    tokenStore.clearToken()
                }
                resp
            }
            .addInterceptor(logger)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun createApi(context: Context): ApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .client(createOkHttp(context))
            .build()
        return retrofit.create(ApiService::class.java)
    }
}
