package com.nigdroid.quantummessenger.presentation.ui.screen.auth

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.nigdroid.quantummessenger.presentation.ui.background.AnimatedMeshGradientBackground
import com.nigdroid.quantummessenger.presentation.viewmodel.auth.AuthState
import com.nigdroid.quantummessenger.presentation.viewmodel.auth.AuthViewModel
import com.nigdroid.quantummessenger.util.Constants
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID
import kotlin.math.sin

@Composable
fun AuthScreen(
    onAuthSuccess: (String) -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            onAuthSuccess((authState as AuthState.Success).userId)
        }
    }

    val handleGoogleSignIn = {
        coroutineScope.launch {
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(Constants.GOOGLE_CLIENT_ID)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                handleSignInResult(result, viewModel)
            } catch (e: GetCredentialException) {
                Log.e("AuthScreen", "Google Error: ${e.message}")
                // Show the specific error message to help identify if it's SHA-1 or Consent Screen
                val errorMsg = when {
                    e.message?.contains("7") == true -> "Network Error: Check Internet or Device Time"
                    e.message?.contains("10") == true -> "Developer Console Error: Check SHA-1 and Package Name"
                    e.message?.contains("16") == true -> "Cancelled by user"
                    else -> e.message ?: "Sign-in failed"
                }
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e("AuthScreen", "General Error: ${e.message}", e)
                Toast.makeText(context, "An error occurred: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        AnimatedMeshGradientBackground(modifier = Modifier.fillMaxSize(), isDarkTheme = false)

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AuthHeader()
            Spacer(modifier = Modifier.height(64.dp))

            when (val state = authState) {
                is AuthState.Idle -> IdleContent(onGoogleClick = { handleGoogleSignIn() })
                is AuthState.Authenticating -> LoadingView("Authenticating with Google...")
                is AuthState.GeneratingKeys -> LoadingView("Generating Secure Identity...")
                is AuthState.Uploading -> LoadingView("Securing your account...")
                is AuthState.Success -> SuccessContent()
                is AuthState.Error -> ErrorContent(state.message) { viewModel.retryLogin() }
            }
        }
    }
}

private fun handleSignInResult(result: GetCredentialResponse, viewModel: AuthViewModel) {
    val credential = result.credential
    if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
        // Pass the token to the viewmodel. Nonce is null as it's skipped in Supabase.
        viewModel.signInWithGoogle(
            idToken = googleIdTokenCredential.idToken,
            email = googleIdTokenCredential.id,
            nonce = null
        )
    }
}

@Composable
private fun AuthHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Quantum Messenger", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Post-Quantum Secure Messaging", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

@Composable
private fun IdleContent(onGoogleClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Protect your conversations with post-quantum encryption. Connect with Google to begin.",
            textAlign = TextAlign.Center, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(48.dp))
        Card(
            onClick = onGoogleClick,
            modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(28.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Continue with Google", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun LoadingView(text: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(strokeWidth = 3.dp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SuccessContent() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("✓", fontSize = 80.sp, color = Color(0xFF4CAF50))
        Text("Identity Secured", fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("⚠️", fontSize = 64.sp)
        Text("Auth Error", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(message, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onRetry) { Text("Try Again") }
    }
}

@Composable
private fun CryptographicMeshAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val offset1 by infiniteTransition.animateFloat(0f, 360f, infiniteRepeatable(tween(10000, easing = LinearEasing)), label = "")
    val offset2 by infiniteTransition.animateFloat(0f, 360f, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "")
    Box(modifier = modifier.background(Brush.radialGradient(listOf(Color(0xFF6366FF).copy(alpha = 0.4f + sin((offset1 * Math.PI).toFloat() / 180f) * 0.1f), Color(0xFF00BCD4).copy(alpha = 0.3f + sin((offset2 * Math.PI).toFloat() / 180f) * 0.1f), Color(0xFF8E6FA8).copy(alpha = 0.2f)), center = Offset(0.5f + sin((offset1 * Math.PI / 180.0)).toFloat() * 0.2f, 0.5f + sin((offset2 * Math.PI / 180.0)).toFloat() * 0.2f), radius = 600f)))
}
