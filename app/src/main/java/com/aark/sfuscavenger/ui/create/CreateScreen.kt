package com.aark.sfuscavenger.ui.create

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun CreateScreen(navController: NavController) {
    // State for text fields
    var eventName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var maxPlayer by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("Start Time") }
    var endTime by remember { mutableStateOf("End Time") }

    val context = LocalContext.current
    // Time picker dialogs
    val startTimePicker = remember {
        TimePickerDialog(
            context,
            { _, hour: Int, minute: Int ->
                startTime = String.format("%02d:%02d", hour, minute)
            },
            12, 0, true
        )
    }

    val endTimePicker = remember {
        TimePickerDialog(
            context,
            { _, hour: Int, minute: Int ->
                endTime = String.format("%02d:%02d", hour, minute)
            },
            12, 0, true
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3ECE7))
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        Text(
            text = "Create a game",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier
                .padding(bottom = 32.dp)
                .align(Alignment.Start)
        )

        // Event Name
        CreateInputField(
            label = "Event Name",
            value = eventName,
            onValueChange = { eventName = it }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Start/End Time pickers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CreateTimePickerBox(
                label = startTime,
                onClick = { startTimePicker.show() },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp)
            )
            CreateTimePickerBox(
                label = endTime,
                onClick = { endTimePicker.show() },
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Description
        CreateInputField(
            label = "Description",
            value = description,
            onValueChange = { description = it }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Max Player
        CreateInputField(
            label = "Max Player",
            value = maxPlayer,
            onValueChange = { maxPlayer = it }
        )

        Spacer(modifier = Modifier.height(20.dp))

        CreateTextField("Private or Public")

        Spacer(modifier = Modifier.height(48.dp))

        // Create button
        Button(
            onClick = {
                // connect to ViewModel later
                println("Creating game: $eventName, $description, $maxPlayer, $startTime → $endTime")
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA6192E)),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(52.dp)
        ) {
            Text(
                text = "Create Game",
                color = Color.White,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CreateInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.Black) },
        shape = RoundedCornerShape(50),
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFD3C5BB), RoundedCornerShape(50))
            .padding(horizontal = 4.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFFD3C5BB),
            unfocusedContainerColor = Color(0xFFD3C5BB),
            cursorColor = Color(0xFFA6192E),
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent
        ),
        textStyle = LocalTextStyle.current.copy(color = Color.Black)
    )
}

@Composable
fun CreateTimePickerBox(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFD3C5BB), RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.Black,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CreateTextField(label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFD3C5BB), RoundedCornerShape(50))
            .padding(vertical = 12.dp, horizontal = 20.dp)
    ) {
        Text(
            text = label,
            color = Color.Black,
            fontSize = 16.sp,
            textAlign = TextAlign.Start
        )
    }
}
