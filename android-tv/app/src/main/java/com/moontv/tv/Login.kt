package com.moontv.tv

import android.content.Context
import com.moontv.tv.net.NetworkModule
import com.moontv.tv.net.TokenStore

suspend fun loginWithPassword(context: Context, password: String): Boolean {
    val api = NetworkModule.createApi(context)
    val resp = api.loginToken(mapOf("password" to password))
    TokenStore(context).saveToken(resp.token)
    return true
}

suspend fun loginWithUser(context: Context, username: String, password: String): Boolean {
    val api = NetworkModule.createApi(context)
    val resp = api.loginToken(mapOf("username" to username, "password" to password))
    TokenStore(context).saveToken(resp.token)
    return true
}

