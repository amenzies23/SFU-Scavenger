package com.aark.sfuscavenger.ui.login

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.aark.sfuscavenger.ui.theme.SFUScavengerTheme
import com.aark.sfuscavenger.R
import com.aark.sfuscavenger.ui.theme.Beige
import com.aark.sfuscavenger.ui.theme.Black
import com.aark.sfuscavenger.ui.theme.Maroon
import com.aark.sfuscavenger.ui.theme.Orange
import com.aark.sfuscavenger.ui.theme.Purple40
import com.aark.sfuscavenger.ui.theme.PurpleGrey40
import com.aark.sfuscavenger.ui.theme.PurpleGrey80
import com.aark.sfuscavenger.ui.theme.White
import kotlinx.coroutines.sync.Mutex

class SignIn : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SFUScavengerTheme {
            }
        }
    }
}

@Composable
fun SignInScreen(
    navController: NavHostController,
    loading: Boolean,
    error: String?,
    onLogin: (String, String) -> Unit,
    onGoToSignUp: () -> Unit)
{

    var emailValue by remember {
        mutableStateOf("")
    }

    var passwordValue by remember {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Beige)
            .padding(20.dp)
            .padding(top = 20.dp)
    ) {
        Text(
            text = "Welcome,",
            color = Maroon,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Sign in to continue!",
//            style = MaterialTheme.typography.titleMedium,
            fontSize = 20.sp,
            color = Black,
            fontWeight = FontWeight.Bold
        )
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "SFU Scavenger Logo",
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.CenterHorizontally),
            contentScale = ContentScale.Fit

        )
        Text(
            text = "SFU Scavenger",
            color = Black,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(60.dp))

        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = "Email",
                color = Black,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            TextField(
                value = emailValue,
                onValueChange = { newValue ->
                    emailValue = newValue
                },
                placeholder = {
                    Text(
                        text = "Enter your email",
                        color = Black
                    )
                },
                shape = RoundedCornerShape(10.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = White,
                    unfocusedContainerColor = White,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        shape = RoundedCornerShape(10.dp),
                        width = 1.dp,
                        color = Maroon
                    )

            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Password",
                color = Black,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            TextField(
                value = passwordValue,
                onValueChange = { newValue ->
                    passwordValue = newValue
                },
                placeholder = {
                    Text(
                        text = "Password",
                        color = Black
                    )
                },
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(10.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = White,
                    unfocusedContainerColor = White,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        shape = RoundedCornerShape(10.dp),
                        width = 1.dp,
                        color = Maroon
                    )

            )

            Spacer(modifier = Modifier.height(25.dp))

            Button(
                onClick = {onLogin(emailValue, passwordValue)},
                enabled = !loading && emailValue.isNotBlank() && passwordValue.length >= 6,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Maroon,
                    contentColor = White
                ),
                modifier = Modifier
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 0.dp)

            ) {
                Text(if (loading) "Signing in..." else "Sign In",
                    fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,

            ) {
                Text(
                    text = "Don't have an account? ",
                    color = Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Sign Up",
                    color = Maroon,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clickable (
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ){ onGoToSignUp()}
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SignInPreview() {
    val nav = rememberNavController()
    SFUScavengerTheme {
        SignInScreen(
            navController = nav,
            loading = false,
            error = null,
            onLogin = { _, _ -> },
            onGoToSignUp = {}
        )
    }
}