package io.nekohasekai.sagernet.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.ui.MainActivity
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class LoginActivity : AppCompatActivity() {
    private val API = "https://congdong4g.com/api/v1"
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = getSharedPreferences("cd4g", MODE_PRIVATE)
        if (!prefs.getString("auth_data", null).isNullOrEmpty()) {
            goMain()
            return
        }
        
        setContentView(R.layout.activity_cd4g_login)
        
        findViewById<Button>(R.id.btnLogin).setOnClickListener { doLogin() }
        findViewById<TextView>(R.id.tvRegister).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun doLogin() {
        val email = findViewById<EditText>(R.id.etEmail).text.toString().trim()
        val pass = findViewById<EditText>(R.id.etPassword).text.toString().trim()
        
        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Nhập email và mật khẩu", Toast.LENGTH_SHORT).show()
            return
        }
        
        findViewById<ProgressBar>(R.id.progressBar).visibility = View.VISIBLE
        findViewById<Button>(R.id.btnLogin).isEnabled = false
        
        scope.launch {
            try {
                val res = withContext(Dispatchers.IO) {
                    val url = URL("$API/passport/auth/login")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.doOutput = true
                    conn.outputStream.write("""{"email":"$email","password":"$pass"}""".toByteArray())
                    conn.inputStream.bufferedReader().readText()
                }
                
                val json = JSONObject(res)
                val auth = json.optJSONObject("data")?.optString("auth_data")
                
                if (!auth.isNullOrEmpty()) {
                    getSharedPreferences("cd4g", MODE_PRIVATE).edit()
                        .putString("auth_data", auth)
                        .putString("email", email)
                        .apply()
                    
                    loadUserInfo(auth)
                    loadSubscription(auth)
                    
                    Toast.makeText(this@LoginActivity, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                    goMain()
                } else {
                    Toast.makeText(this@LoginActivity, json.optString("message", "Sai email/mật khẩu"), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
                findViewById<Button>(R.id.btnLogin).isEnabled = true
            }
        }
    }

    private suspend fun loadUserInfo(auth: String) {
        withContext(Dispatchers.IO) {
            try {
                val res = URL("$API/user/info?auth_data=$auth").readText()
                val data = JSONObject(res).optJSONObject("data")
                if (data != null) {
                    getSharedPreferences("cd4g", MODE_PRIVATE).edit()
                        .putLong("expired_at", data.optLong("expired_at", 0))
                        .putLong("transfer_enable", data.optLong("transfer_enable", 0))
                        .putLong("upload", data.optLong("u", 0))
                        .putLong("download", data.optLong("d", 0))
                        .apply()
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private suspend fun loadSubscription(auth: String) {
        withContext(Dispatchers.IO) {
            try {
                val res = URL("$API/user/getSubscribe?auth_data=$auth").readText()
                val subUrl = JSONObject(res).optJSONObject("data")?.optString("subscribe_url")
                if (!subUrl.isNullOrEmpty()) {
                    getSharedPreferences("cd4g", MODE_PRIVATE).edit()
                        .putString("subscribe_url", subUrl)
                        .apply()
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun goMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
