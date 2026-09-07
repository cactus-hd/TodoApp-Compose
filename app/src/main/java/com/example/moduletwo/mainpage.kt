package com.example.moduletwo

import android.content.Context
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.security.MessageDigest

data class TodoItem(val text: String, val color: Color)

private const val PREFS_NAME = "notes_prefs"
private const val NOTES_KEY = "notes"
private const val PASSWORD_KEY = "password_hash"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Mainpage(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    val itemList = remember {
        mutableStateListOf<TodoItem>().apply {
            addAll(loadNotes(prefs))
        }
    }

    var isSheetOpen by remember { mutableStateOf(false) }
    var todoName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Color(0xFF0277BD)) }

    var hasPassword by remember {
        mutableStateOf(prefs.getString(PASSWORD_KEY, null) != null)
    }

    var isLocked by remember {
        mutableStateOf(hasPassword)
    }

    var showPasswordDialog by remember { mutableStateOf(false) }
    var showUnlockDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }

    val colorOptions = listOf(
        Color(0xFF0277BD),
        Color(0xFFFF8F00),
        Color(0xFF2E7D32),
        Color(0xFFAD1457),
        Color(0xFF4527A0),
        Color(0xFFD84315)
    )

    fun saveCurrentNotes() {
        saveNotes(prefs, itemList)
    }

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .padding(bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                itemsIndexed(
                    items = itemList,
                    key = { index, item -> "$index-${item.text}-${item.color.value}" }
                ) { index, item ->
                    NoteCard(
                        item = item,
                        locked = isLocked,
                        onDelete = {
                            itemList.removeAt(index)
                            saveCurrentNotes()
                        }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        if (!hasPassword) {
                            passwordInput = ""
                            confirmPasswordInput = ""
                            passwordError = ""
                            showPasswordDialog = true
                        } else if (isLocked) {
                            passwordInput = ""
                            passwordError = ""
                            showUnlockDialog = true
                        } else {
                            isLocked = true
                        }
                    },
                    modifier = Modifier.size(52.dp),
                    containerColor = Color(0xFF4D7CFE),
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.Lock,
                        contentDescription = if (isLocked) "Unlock notes" else "Lock notes"
                    )
                }

                FloatingActionButton(
                    onClick = {
                        isSheetOpen = true
                    },
                    modifier = Modifier.size(59.dp),
                    containerColor = Color(0xFF2AB295),
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add note"
                    )
                }
            }

            if (isSheetOpen) {
                ModalBottomSheet(
                    onDismissRequest = {
                        isSheetOpen = false
                    },
                    sheetState = rememberModalBottomSheetState()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Add New Note",
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            colorOptions.forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (selectedColor == color) 2.dp else 0.dp,
                                            color = if (selectedColor == color) Color.Black else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            selectedColor = color
                                        }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = todoName,
                            onValueChange = { todoName = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Ex: studying...") },
                            label = { Text("Enter your note") },
                            minLines = 3
                        )

                        Button(
                            onClick = {
                                if (todoName.isNotBlank()) {
                                    itemList.add(
                                        TodoItem(
                                            text = todoName.trim(),
                                            color = selectedColor
                                        )
                                    )
                                    saveCurrentNotes()
                                    todoName = ""
                                    isSheetOpen = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2AB295)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = 16.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Save Note")
                        }
                    }
                }
            }

            if (showPasswordDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showPasswordDialog = false
                    },
                    title = {
                        Text("Set Password")
                    },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = {
                                    passwordInput = it
                                    passwordError = ""
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Password") },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.size(10.dp))

                            OutlinedTextField(
                                value = confirmPasswordInput,
                                onValueChange = {
                                    confirmPasswordInput = it
                                    passwordError = ""
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Confirm password") },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true
                            )

                            AnimatedVisibility(passwordError.isNotEmpty()) {
                                Text(
                                    text = passwordError,
                                    color = Color(0xFFD32F2F),
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                when {
                                    passwordInput.length < 4 -> {
                                        passwordError = "Password must be at least 4 characters"
                                    }
                                    passwordInput != confirmPasswordInput -> {
                                        passwordError = "Passwords do not match"
                                    }
                                    else -> {
                                        prefs.edit()
                                            .putString(PASSWORD_KEY, hashPassword(passwordInput))
                                            .apply()
                                        hasPassword = true
                                        isLocked = true
                                        passwordInput = ""
                                        confirmPasswordInput = ""
                                        passwordError = ""
                                        showPasswordDialog = false
                                    }
                                }
                            }
                        ) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showPasswordDialog = false
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (showUnlockDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showUnlockDialog = false
                    },
                    title = {
                        Text("Unlock Notes")
                    },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = {
                                    passwordInput = it
                                    passwordError = ""
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Password") },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true
                            )

                            AnimatedVisibility(passwordError.isNotEmpty()) {
                                Text(
                                    text = passwordError,
                                    color = Color(0xFFD32F2F),
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val savedHash = prefs.getString(PASSWORD_KEY, null)
                                if (savedHash != null && hashPassword(passwordInput) == savedHash) {
                                    isLocked = false
                                    passwordInput = ""
                                    passwordError = ""
                                    showUnlockDialog = false
                                } else {
                                    passwordError = "Wrong password"
                                }
                            }
                        ) {
                            Text("Unlock")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                passwordInput = ""
                                passwordError = ""
                                showUnlockDialog = false
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun NoteCard(
    item: TodoItem,
    locked: Boolean,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = item.color
        ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color.Gray)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.text,
                modifier = Modifier
                    .weight(1f)
                    .blur(if (locked) 10.dp else 0.dp),
                fontSize = 16.sp
            )

            IconButton(
                onClick = onDelete
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete note"
                )
            }
        }
    }
}

private fun saveNotes(
    prefs: android.content.SharedPreferences,
    notes: List<TodoItem>
) {
    val serialized = notes.joinToString("\\n") {
        val encodedText = Base64.encodeToString(
            it.text.toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP
        )
        "${it.color.value}|$encodedText"
    }

    prefs.edit()
        .putString(NOTES_KEY, serialized)
        .apply()
}

private fun loadNotes(
    prefs: android.content.SharedPreferences
): List<TodoItem> {
    val saved = prefs.getString(NOTES_KEY, null) ?: return emptyList()

    return saved
        .split("\\n")
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val separatorIndex = line.indexOf("|")
            if (separatorIndex <= 0) {
                null
            } else {
                val colorValue = line.substring(0, separatorIndex).toLongOrNull()
                val encodedText = line.substring(separatorIndex + 1)

                if (colorValue == null) {
                    null
                } else {
                    runCatching {
                        val text = String(
                            Base64.decode(encodedText, Base64.NO_WRAP),
                            Charsets.UTF_8
                        )
                        TodoItem(
                            text = text,
                            color = Color(colorValue.toULong())
                        )
                    }.getOrNull()
                }
            }
        }
}

private fun hashPassword(password: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes = digest.digest(password.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
}
