package com.moontv.tv.auth

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.moontv.tv.loginWithPassword
import com.moontv.tv.loginWithUser
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme { Surface(Modifier.fillMaxSize()) { LoginScreen(onDone = { finish() }) } }
        }
    }
}

@Composable
private fun LoginScreen(onDone: () -> Unit) {
    var mode by remember { mutableStateOf("local") } // local or user
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val ctx = androidx.compose.ui.platform.LocalContext.current as ComponentActivity

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("登录", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Row { 
            FilterChip(selected = mode == "local", onClick = { mode = "local" }, label = { Text("本地密码") })
            Spacer(Modifier.width(8.dp))
            FilterChip(selected = mode == "user", onClick = { mode = "user" }, label = { Text("用户名/密码") })
        }
        Spacer(Modifier.height(12.dp))
        if (mode == "user") {
            OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("用户名") })
            Spacer(Modifier.height(8.dp))
        }
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("密码") }, visualTransformation = PasswordVisualTransformation())
        Spacer(Modifier.height(12.dp))
        Button(onClick = {
            ctx.lifecycleScope.launch {
                val ok = if (mode == "local") {
                    runCatching { loginWithPassword(ctx, password) }.isSuccess
                } else {
                    runCatching { loginWithUser(ctx, username, password) }.isSuccess
                }
                Toast.makeText(ctx, if (ok) "登录成功" else "登录失败", Toast.LENGTH_SHORT).show()
                if (ok) onDone()
            }
        }) { Text("登录") }
    }
}

