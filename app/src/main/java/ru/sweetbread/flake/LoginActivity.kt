package ru.sweetbread.flake

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import ru.sweetbread.flake.ui.theme.FlakeTheme
import splitties.activities.start
import splitties.toast.toast

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlakeTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Main(
                        { login, password -> applyLogin (login, password) },
                        { login, password -> applySignup(login, password) }
                    )
                }
            }
        }
    }

    private fun applyLogin(login: String, password: String) {
        runBlocking {
            val response =
                client.post("$baseurl/auth/login") {
                    setBody(
                        JSONObject()
                            .put("login", login)
                            .put("password", password)
                            .toString()
                    )
                    contentType(ContentType.Application.Json)
                }

            if (response.status == HttpStatusCode.OK) {
                val json = JSONObject(response.bodyAsText())
                val token = json.getString("token")
                self = json.getJSONObject("user")

                val editor = getSharedPreferences("Account", 0).edit()
                editor.putString("token", token)
                editor.apply()

                start<MainActivity>()
            } else {
                toast("${resources.getString(R.string.error)}: ${response.bodyAsText()}")
                finish()
                startActivity(intent)
            }
        }
    }

    private fun applySignup(login: String, password: String) {
        runBlocking {
            val response = client.post("$baseurl/auth/register") {
                setBody(
                    JSONObject()
                        .put("login", login)
                        .put("username", login)
                        .put("password", password)
                        .put("locale", "RU")
                        .toString()
                )
                contentType(ContentType.Application.Json)
            }
            if (response.status == HttpStatusCode.OK) {
                val json = JSONObject(response.bodyAsText())
                val token = json.getString("token")
                self = json.getJSONObject("user")

                val editor = getSharedPreferences("Account", 0).edit()
                editor.putString("token", token)
                editor.apply()

                start<MainActivity>()
            } else {
                TODO("Raise errors in dependence of error code in body")
            }
        }
    }
}

@Composable
private fun Main(loginFun: (String, String) -> Unit, signFun: (String, String) -> Unit) {
    var toLogin by remember { mutableStateOf(true) }
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = CenterHorizontally
    ) {
        AnimatedVisibility(toLogin) {
            Column (
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Column {
                    OutlinedTextField(
                        singleLine = true,
                        value = login,
                        onValueChange = { login = it },
                        label = { Text(stringResource(R.string.login)) }
                    )

                    OutlinedTextField(
                        singleLine = true,
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                }

                Button(
                    enabled = (login.contains(Regex("[0-9a-z._-]{5,20}"))) and (password.length in 4..30),
                    onClick = { loginFun(login, password) }
                ) { Text(stringResource(R.string.login_button_text)) }
            }
        }

        AnimatedVisibility(!toLogin) {
            Column (
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Column {
                    OutlinedTextField(
                        singleLine = true,
                        value = login,
                        onValueChange = { login = it },
                        label = { Text(stringResource(R.string.login)) },
                        supportingText = {
                            if (login.length !in 5..30)
                                Text(
                                    stringResource(id = R.string.login_length_limits),
                                    color = MaterialTheme.colorScheme.error
                                )
                            else if (!(login.contains(Regex("[0-9a-z._-]+"))))
                                Text(
                                    stringResource(id = R.string.login_regex_limits),
                                    color = MaterialTheme.colorScheme.error
                                )
                        },
                        isError = ((login.length !in 5..30) or (!(login.contains(Regex("[0-9a-z._-]+")))))
                    )

                    OutlinedTextField(
                        singleLine = true,
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        supportingText = {
                            if (password.length !in 4..40)
                                Text(
                                    stringResource(id = R.string.password_length_limit),
                                    color = MaterialTheme.colorScheme.error
                                )
                        },
                        isError = (password.length !in 4..40)
                    )

                    OutlinedTextField(
                        singleLine = true,
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text(stringResource(R.string.confirm_password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = password != confirmPassword,
                        supportingText = {
                            if (password != confirmPassword)
                                Text(
                                    stringResource(id = R.string.same_pass),
                                    color = MaterialTheme.colorScheme.error
                                )
                        }
                    )
                }

                Button(
                    enabled = (login.contains(Regex("[0-9a-z._-]{5,20}"))) and (password.length in 4..30),
                    onClick = { signFun(login, password) }
                ) { Text(stringResource(R.string.signup_button_text)) }
            }
        }

            Button (
                onClick = { toLogin = !toLogin; confirmPassword = "" },
                Modifier
                    .fillMaxWidth(0.5f)
                    .widthIn(max = 160.dp)
            ) { Text(stringResource(id = if (toLogin) R.string.signup_button_text else R.string.login_button_text )) }
    }

}

@Preview(showBackground = true)
@Composable
fun LoginPreview() {
    FlakeTheme {
        Main(
            { _, _ -> run { } },
            { _, _ -> run { } }
        )
    }
}