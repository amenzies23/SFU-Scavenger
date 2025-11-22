package com.aark.sfuscavenger.ui.create

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp

@Composable
fun CreateScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.padding(16.dp)) {

        Text("Create Game")

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                loading = true
                error = null

                val uid = auth.currentUser?.uid
                if (uid == null) {
                    error = "Not logged in"
                    loading = false
                    return@Button
                }

                val newGame = mapOf(
                    "name" to "Test Game",
                    "ownerId" to uid,
                    "status" to "draft",
                    "createdAt" to Timestamp.now()
                )

                db.collection("games")
                    .add(newGame)
                    .addOnSuccessListener { docRef ->
                        loading = false
                        navController.navigate("lobby/${docRef.id}")
                    }
                    .addOnFailureListener { e ->
                        loading = false
                        error = e.message
                    }
            }
        ) {
            Text("Create Test Game")
        }

        if (loading) {
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator()
        }

        if (error != null) {
            Spacer(Modifier.height(16.dp))
            Text("Error: $error", color = MaterialTheme.colorScheme.error)
        }
    }
}
