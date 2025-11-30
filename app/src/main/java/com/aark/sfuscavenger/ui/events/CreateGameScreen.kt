package com.aark.sfuscavenger.ui.events

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.style.TextDecoration.Companion.Underline
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aark.sfuscavenger.BuildConfig
import com.aark.sfuscavenger.data.models.Task
import com.aark.sfuscavenger.ui.theme.AppColors
import com.aark.sfuscavenger.ui.theme.Beige
import com.aark.sfuscavenger.ui.theme.Black
import com.aark.sfuscavenger.ui.theme.DarkOrange
import com.aark.sfuscavenger.ui.theme.LightBeige
import com.aark.sfuscavenger.ui.theme.Maroon
import com.aark.sfuscavenger.ui.theme.White
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun CreateGameScreen(navController: NavController, gameId: String? = null, vm: CreateGameViewModel = viewModel()) {
    val selectedTab = remember { mutableStateOf(0) }
    val gameCreated = vm.gameCreated.collectAsState()

    LaunchedEffect(gameId) {
        if (gameId != null) {
            vm.loadGame(gameId)
        }
    }

    LaunchedEffect(gameCreated.value) {
        if (gameCreated.value) {
            navController.popBackStack()
        }
    }

    CreateGameScreenContent(
        selectedTabIndex = selectedTab.value,
        onTabSelected = { selectedTab.value = it },
        navController = navController,
        modifier = Modifier.background(Beige),
        vm = vm,
        isEditMode = gameId != null
    )
}

@Composable
private fun CreateGameScreenContent(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier,
    vm: CreateGameViewModel,
    isEditMode: Boolean = false
) {
    Column(
        modifier = modifier.fillMaxSize()
    ){
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = LightBeige,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = Maroon
                )
            }
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { onTabSelected(0) },
                text = { Text("Game", color = Black)}
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { onTabSelected(1) },
                text = { Text("Tasks", color = Black)}
            )
        }

        when (selectedTabIndex) {
            0 -> GameTab(navController = navController, vm, isEditMode = isEditMode, modifier = Modifier.fillMaxSize().padding(16.dp))
            1 -> TasksTab(navController = navController, vm, modifier = Modifier.fillMaxSize().padding(16.dp))
        }
    }
}

@Composable
private fun GameTab(navController: NavController,
                    vm: CreateGameViewModel,
                    isEditMode: Boolean = false,
                    modifier: Modifier = Modifier
) {
    val game = vm.game.collectAsState()
    val loading = vm.loading.collectAsState()
    val error = vm.error.collectAsState()

    var isScheduled by remember { mutableStateOf(game.value.startTime != null)}

    LaunchedEffect(game.value.startTime) {
        isScheduled = game.value.startTime != null
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Game Name
                TextField(
                    value = game.value.name,
                    onValueChange = { vm.updateName(it) },
                    label = { Text("Game Name*", color = Maroon, fontWeight = Bold) },
                    placeholder = { Text("Enter game name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = LightBeige,
                        unfocusedContainerColor = LightBeige,
                    ),
                    singleLine = true
                )
            }

            item {
                // Description
                TextField(
                    value = game.value.description,
                    onValueChange = { vm.updateDescription(it) },
                    label = { Text("Description", color = Maroon, fontWeight = Bold) },
                    placeholder = { Text("Enter game description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = LightBeige,
                        unfocusedContainerColor = LightBeige,
                    ),
                    maxLines = 5
                )
            }

            item {
                // Location Picker
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = buildAnnotatedString {
                            append("Select Event Location")
                            withStyle(style = SpanStyle(color = Maroon)) { append("*") }
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Black
                    )
                    PlacePickerButton(vm)
                }
            }

            item {
                // Join Mode Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Private Game (Join by Code)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Black
                    )

                    Switch(
                        checked = game.value.joinMode == "code",
                        onCheckedChange = { isChecked ->
                            vm.updateJoinMode(if (isChecked) "code" else "open")
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = White,
                            checkedTrackColor = Maroon,
                            uncheckedThumbColor = Maroon,
                            uncheckedTrackColor = LightBeige
                        ),
                        modifier = Modifier.scale(0.8f)
                    )
                }
            }

            item {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Start Time",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Black
                    )

                    Switch(
                        checked = isScheduled,
                        onCheckedChange = { checked ->
                            isScheduled = checked
                            if (!checked) {
                                vm.updateStartTime(null)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = White,
                            checkedTrackColor = Maroon,
                            uncheckedThumbColor = Maroon,
                            uncheckedTrackColor = LightBeige
                        ),
                        modifier = Modifier.scale(0.8f)
                    )
                }

            }

            item {
                if (game.value.startTime != null || isScheduled) {
                    DateTimePicker(
                        selectedTime = game.value.startTime,
                        onTimeSelected = { timestamp ->
                            vm.updateStartTime(timestamp)
                        }
                    )
                }
            }


            if (error.value != null) {
                item {
                    Text(
                        text = error.value ?: "",
                        color = DarkOrange,
                        fontSize = 14.sp
                    )
                }
            }

        }

        // Save Button
        Button(
            onClick = { vm.saveGame() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Maroon,
                contentColor = White
            ),
            enabled = !loading.value && game.value.name.isNotBlank()
        ) {
            Text(
                text = when {
                    loading.value -> "Saving..."
                    isEditMode -> "Update"
                    else -> "Save"
                },
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = LightBeige,
                contentColor = Black
            ),
            enabled = !loading.value
        ){
            Text(
                text = "Cancel",
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

    }

}

@Composable
private fun TasksTab(navController: NavController,
                    vm: CreateGameViewModel,
                    modifier: Modifier = Modifier
) {
    val tasks = vm.tasks.collectAsState()
    val game = vm.game.collectAsState()

    // Show message if game isn't saved yet
    if (game.value.id.isBlank()) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Save the game first before adding tasks",
                fontSize = 16.sp,
                color = DarkOrange
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize())
    {
        // Task list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tasks.value, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    onSave = { updatedTask -> vm.updateTask(updatedTask) },
                    onDelete = { vm.deleteTask(task.id) }
                )
            }

            item {
                AddTask(
                    onAdd = { newTask -> vm.addTask(newTask) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskCard(
    task: Task,
    onSave: (Task) -> Unit,
    onDelete: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(task.name) }
    var description by remember { mutableStateOf(task.description) }
    var points by remember { mutableStateOf(task.points.toString()) }
    var type by remember { mutableStateOf(task.type) }
    var answer by remember { mutableStateOf(task.value ?: "") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LightBeige, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header with Edit/Delete buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isEditing) "Edit Task" else task.name,
                fontWeight = Bold,
                fontSize = 18.sp,
                color = Black,
                modifier = Modifier.weight(1f)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isEditing) {
                    TextButton(
                        onClick = {
                            name = task.name
                            description = task.description
                            points = task.points.toString()
                            type = task.type
                            answer = task.value ?: ""
                            isEditing = false
                        }
                    ) {
                        Text("Cancel", color = Black)
                    }
                    TextButton(
                        onClick = {
                            val finalValue = when (type) {
                                "text" -> if (answer.isNotBlank()) answer else null
                                "qr" -> task.id // Set to task ID for QR
                                else -> null
                            }
                            onSave(
                                task.copy(
                                    name = name,
                                    description = description,
                                    points = points.toIntOrNull() ?: 0,
                                    type = type,
                                    value = finalValue
                                )
                            )
                            isEditing = false
                        },
                        enabled = name.isNotBlank()
                    ) {
                        Text("Save", color = Maroon)
                    }
                } else {
                    TextButton(onClick = { isEditing = true }) {
                        Text("Edit", color = Maroon)
                    }
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = DarkOrange)
                    }
                }
            }
        }

        // Show fields when editing, otherwise show summary
        if (isEditing) {
            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Task Name*", color = Maroon, fontWeight = Bold) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = White,
                    unfocusedContainerColor = White,
                ),
                singleLine = true
            )

            TextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description", color = Maroon, fontWeight = Bold) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = White,
                    unfocusedContainerColor = White,
                ),
                maxLines = 3
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = points,
                    onValueChange = { if (it.all { c -> c.isDigit() } || it.isEmpty()) points = it },
                    label = { Text("Points", color = Maroon, fontWeight = Bold) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = White,
                        unfocusedContainerColor = White,
                    ),
                    singleLine = true
                )

                var expanded by remember { mutableStateOf(false) }
                val taskTypes = listOf("text", "photo", "qr")

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    TextField(
                        value = type.uppercase(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type", color = Maroon, fontWeight = Bold) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedContainerColor = White,
                            unfocusedContainerColor = White,
                        ),
                        singleLine = true
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        taskTypes.forEach { taskType ->
                            DropdownMenuItem(
                                text = { Text(taskType.uppercase()) },
                                onClick = {
                                    type = taskType
                                    // Clear answer when switching away from text
                                    if (taskType != "text") {
                                        answer = ""
                                    }
                                    expanded = false
                                }
                            )
                        }
                    }
                }

            }

            // Answer field - only show for text tasks
            if (type == "text") {
                TextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text("Expected Answer", color = Maroon, fontWeight = Bold) },
                    placeholder = { Text("Enter the correct answer") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = White,
                        unfocusedContainerColor = White,
                    ),
                    singleLine = true
                )
            }

        } else {
            // Summary view
            if (description.isNotBlank()) {
                Text(
                    text = task.description,
                    fontSize = 14.sp,
                    color = Black
                )
            }
            Text(
                text = "Points: ${task.points} • ${task.type.uppercase()}",
                fontSize = 12.sp,
                color = Maroon
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimePicker(
    selectedTime: Timestamp?,
    onTimeSelected: (Timestamp) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val calendar = remember {
        Calendar.getInstance().apply {
            selectedTime?.toDate()?.let { time = it }
        }
    }

    val dateFormat = remember {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
    }
    val timeFormat = remember {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Start Date & Time",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Maroon
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Date Button
            Button(
                onClick = { showDatePicker = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LightBeige,
                    contentColor = Black
                )
            ) {
                Text(
                    text = selectedTime?.let { dateFormat.format(it.toDate()) } ?: "Select Date",
                    fontSize = 14.sp
                )
            }

            // Time Button
            Button(
                onClick = { showTimePicker = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LightBeige,
                    contentColor = Black
                )
            ) {
                Text(
                    text = selectedTime?.let { timeFormat.format(it.toDate()) } ?: "Select Time",
                    fontSize = 14.sp
                )
            }
        }
    }

    if (showDatePicker) {
        val initialDateMillis = Calendar.getInstance().apply {
            timeInMillis = calendar.timeInMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialDateMillis
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                            utcCal.timeInMillis = millis

                            calendar.set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                            calendar.set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                            calendar.set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))

                            onTimeSelected(Timestamp(Date(calendar.timeInMillis)))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = calendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = calendar.get(Calendar.MINUTE)
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time") },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        calendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        calendar.set(Calendar.MINUTE, timePickerState.minute)
                        onTimeSelected(Timestamp(Date(calendar.timeInMillis)))
                        showTimePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTask(
    onAdd: (Task) -> Unit
) {
    var isAdding by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var points by remember { mutableStateOf("10") }
    var type by remember { mutableStateOf("photo") }
    var answer by remember { mutableStateOf("") }

    if (!isAdding) {
        Button(
            onClick = { isAdding = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Maroon,
                contentColor = White
            )
        ) {
            Text("Add Task +", modifier = Modifier.padding(vertical = 8.dp))
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(LightBeige, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "New Task",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Black
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            isAdding = false
                            name = ""
                            description = ""
                            points = "10"
                            type = "photo"
                        }
                    ) {
                        Text("Cancel", color = Black)
                    }
                    TextButton(
                        onClick = {
                            val finalValue = when (type) {
                                "text" -> if (answer.isNotBlank()) answer else null
                                else -> null // QR will be handled by repository after task is created
                            }
                            onAdd(
                                Task(
                                    name = name,
                                    description = description,
                                    points = points.toIntOrNull() ?: 10,
                                    type = type,
                                    value = finalValue
                                )
                            )
                            isAdding = false
                            name = ""
                            description = ""
                            points = "67"
                            type = "text"
                            answer = ""
                        },
                        enabled = name.isNotBlank()
                    ) {
                        Text("Add", color = Maroon)
                    }
                }
            }

            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Task Name*", color = Maroon, fontWeight = Bold) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = White,
                    unfocusedContainerColor = White,
                ),
                singleLine = true
            )

            TextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description", color = Maroon, fontWeight = Bold) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = White,
                    unfocusedContainerColor = White,
                ),
                maxLines = 3
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = points,
                    onValueChange = { if (it.all { c -> c.isDigit() } || it.isEmpty()) points = it },
                    label = { Text("Points", color = Maroon, fontWeight = Bold) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = White,
                        unfocusedContainerColor = White,
                    ),
                    singleLine = true
                )

                var expanded by remember { mutableStateOf(false) }
                val taskTypes = listOf("text", "photo", "qr")

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    TextField(
                        value = type.uppercase(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type", color = Maroon, fontWeight = Bold) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedContainerColor = White,
                            unfocusedContainerColor = White,
                        ),
                        singleLine = true
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        taskTypes.forEach { taskType ->
                            DropdownMenuItem(
                                text = { Text(taskType.uppercase()) },
                                onClick = {
                                    type = taskType
                                    if (taskType != "text") {
                                        answer = ""
                                    }
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Answer field - only show for text tasks
            if (type == "text") {
                TextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text("Expected Answer", color = Maroon, fontWeight = Bold) },
                    placeholder = { Text("Enter the correct answer") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = White,
                        unfocusedContainerColor = White,
                    ),
                    singleLine = true
                )
            }

        }
    }
}

@Composable
private fun PlacePickerButton(vm: CreateGameViewModel) {
    val context = LocalContext.current

    // Initialize Places
    LaunchedEffect(Unit) {
        if (!Places.isInitialized()) {
            Places.initialize(context.applicationContext, BuildConfig.MAPS_API_KEY)
        }
    }

    val fields = listOf(Place.Field.ID,
        Place.Field.NAME,
        Place.Field.LAT_LNG,
        Place.Field.ADDRESS
    )

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            try {
                val place = Autocomplete.getPlaceFromIntent(data!!)
                val latLng = place.latLng
                if (latLng != null) {
                    // convert to Firestore GeoPoint and update VM
                    val geo = GeoPoint(latLng.latitude, latLng.longitude)
                    vm.updateLocation(geo)
                }
            } catch (e: Exception) {
            }
        }
    }

    Button(
        onClick = {
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .build(context)
            launcher.launch(intent)
        },
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Maroon,
            contentColor = White
        ),
        modifier = Modifier.height(30.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Select Location",
            tint = White,
            modifier = Modifier.size(25.dp)
        )
    }
}

@Composable
private fun RequiredLabel(text: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "*", color = Maroon, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}




















