package com.nigdroid.quantummessenger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nigdroid.quantummessenger.ui.MessagingViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MessagingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                MessagingScreen(viewModel)
            }
        }
    }
}

@Composable
fun MessagingScreen(viewModel: MessagingViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // ── Status bar ────────────────────────────────────────────────────────
        Text(
            text = "Status: ${uiState.connectionStatus}",
            style = MaterialTheme.typography.labelMedium
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "PQ Public Key: ${uiState.ownPublicKeyHex}...",
            style = MaterialTheme.typography.labelSmall
        )

        uiState.error?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Error: $it", color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Connect button (stub JWT for now) ─────────────────────────────────
        Button(onClick = {
            viewModel.connect("stub-jwt-token")  // Phase 5: real Supabase JWT
        }) {
            Text("Connect")
        }

        Spacer(modifier = Modifier.height(12.dp))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(12.dp))

        // ── Message list ──────────────────────────────────────────────────────
        if (uiState.messages.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No messages yet", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.messages) { msg ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "From: ${msg.senderId}",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = msg.plaintext,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
