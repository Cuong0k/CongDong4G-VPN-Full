package io.nekohasekai.sagernet.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import io.nekohasekai.sagernet.R
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class RegisterActivity : AppCompatActivity() {
    private val API = "https://congdong4g.com/api/v1"
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cd4g_register)
        
        findViewById<Button>(R.id.btnRegister).setOnClickListener { doRegister() }
        findViewById<TextView>(R.id.tvLogin).setOnClickListener { finish() }
    }

    private fun doRegister() {
        val email = findViewById<EditText>(R.id.etEmail).text.toString().trim()
        val pass = findViewById<EditText>(R.id.etPassword).text.toString().trim()
        val confirm = findViewById<EditText>(R.id.etConfirmPassword).text.toString().trim()
        
        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
            return
        }
        if (pass != confirm) {
            Toast.makeText(this, "Mật khẩu không khớp", Toast.LENGTH_SHORT).show()
            return
        }
        
        findViewById<ProgressBar>(R.id.progressBar).visibility = View.VISIBLE
        findViewById<Button>(R.id.btnRegister).isEnabled = false
        
        scope.launch {
            try {
                val res = withContext(Dispatchers.IO) {
                    val url = URL("$API/passport/auth/register")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.doOutput = true
                    conn.outputStream.write("""{"email":"$email","password":"$pass"}""".toByteArray())
                    conn.inputStream.bufferedReader().readText()
                }
                
                val json = JSONObject(res)
                if (json.optJSONObject("data") != null) {
                    Toast.makeText(this@RegisterActivity, "Đăng ký thành công!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@RegisterActivity, json.optString("message", "Đăng ký thất bại"), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@RegisterActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
                findViewById<Button>(R.id.btnRegister).isEnabled = true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
