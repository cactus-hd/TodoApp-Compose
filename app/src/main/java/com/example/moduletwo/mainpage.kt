package com.example.moduletwo

import android.content.Context
import android.content.SharedPreferences
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class TodoItem(val id: Long, val text: String, val color: Color)

private const val PREFS_NAME = "notes_prefs"
private const val NOTES_KEY = "notes"
private const val NOTES_ENCRYPTED_KEY = "notes_encrypted"
private const val PASSWORD_KEY = "password_hash"
private const val SALT_KEY = "password_salt"
private const val GCM_IV_LENGTH = 12

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Mainpage(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    val initialPasswordHash = remember {
        try {
            prefs.getString(PASSWORD_KEY, null)
        } catch (e: Exception) {
            null
        }
    }

    var hasPassword by remember { mutableStateOf(initialPasswordHash != null) }
    var isLocked by remember { mutableStateOf(initialPasswordHash != null) }
    var cryptoKey by remember { mutableStateOf<SecretKey?>(null) }

    val itemList = remember {
        val list = mutableStateListOf<TodoItem>()
        if (initialPasswordHash == null) {
            list.addAll(safeLoad(prefs, null))
        }
        list
    }

    var isSheetOpen by remember { mutableStateOf(false) }
    var todoName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Color(0xFF0277BD)) }

    var showPasswordDialog by remember { mutableStateOf(false) }
    var showUnlockDialog by remember { mutableStateOf(false) }
    var showRemovePasswordDialog by remember { mutableStateOf(false) }

    var passwordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }

    var removePasswordInput by remember { mutableStateOf("") }
    var removePasswordError by remember { mutableStateOf("") }

    val colorOptions = listOf(
        Color(0xFF363636),
        Color(0xFF0277BD),
        Color(0xFFFF8F00),
        Color(0xFF2E7D32),
        Color(0xFFAD1457),
        Color(0xFF4527A0),
        Color(0xFFD84315)
    )

    val sheetState = rememberModalBottomSheetState()

    fun persist() {
        safeSave(prefs, itemList, cryptoKey)
    }

    Scaffold(modifier = modifier.fillMaxSize()) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (itemList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (hasPassword && isLocked)
                            "Notes are locked.\nTap the lock to unlock."
                        else
                            "No notes yet.\nTap Create to add one.",
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                        .padding(bottom = 140.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val leftItems = itemList.filterIndexed { index, _ -> index % 2 == 0 }
                        val rightItems = itemList.filterIndexed { index, _ -> index % 2 == 1 }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            leftItems.forEach { item ->
                                NoteCard(
                                    item = item,
                                    locked = isLocked,
                                    onDelete = {
                                        itemList.removeAll { it.id == item.id }
                                        persist()
                                    }
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rightItems.forEach { item ->
                                NoteCard(
                                    item = item,
                                    locked = isLocked,
                                    onDelete = {
                                        itemList.removeAll { it.id == item.id }
                                        persist()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (hasPassword && !isLocked) {
                    FloatingActionButton(
                        onClick = {
                            removePasswordInput = ""
                            removePasswordError = ""
                            showRemovePasswordDialog = true
                        },
                        modifier = Modifier.size(52.dp),
                        containerColor = Color(0xFFB71C1C),
                        contentColor = Color.White
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove password"
                        )
                    }
                }

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
                            cryptoKey = null
                            itemList.clear()
                        }
                    },
                    modifier = Modifier.size(52.dp),
                    containerColor = Color(0xFF525252),
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = if (isLocked) "Unlock" else "Lock"
                    )
                }

                FloatingActionButton(
                    onClick = {
                        if (hasPassword && cryptoKey == null) {
                            passwordInput = ""
                            passwordError = ""
                            showUnlockDialog = true
                        } else {
                            isSheetOpen = true
                        }
                    },
                    containerColor = Color.DarkGray,
                    contentColor = Color.White
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Create,
                            contentDescription = "Create"
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create")
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                }
            }

            if (isSheetOpen) {
                ModalBottomSheet(
                    onDismissRequest = { isSheetOpen = false },
                    sheetState = sheetState
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
                                        .clickable { selectedColor = color }
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
                                            id = System.nanoTime(),
                                            text = todoName.trim(),
                                            color = selectedColor
                                        )
                                    )
                                    persist()
                                    todoName = ""
                                    isSheetOpen = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00AAB9)
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
                    onDismissRequest = { showPasswordDialog = false },
                    title = { Text("Set Password") },
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

                            Spacer(modifier = Modifier.height(10.dp))

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
                                        try {
                                            val salt = ByteArray(16)
                                            SecureRandom().nextBytes(salt)
                                            val key = deriveKey(passwordInput, salt)

                                            prefs.edit()
                                                .putString(PASSWORD_KEY, hashPassword(passwordInput))
                                                .putString(
                                                    SALT_KEY,
                                                    Base64.encodeToString(salt, Base64.NO_WRAP)
                                                )
                                                .apply()

                                            cryptoKey = key
                                            hasPassword = true
                                            isLocked = false
                                            safeSave(prefs, itemList, key)

                                            passwordInput = ""
                                            confirmPasswordInput = ""
                                            passwordError = ""
                                            showPasswordDialog = false
                                        } catch (e: Exception) {
                                            passwordError = "Could not secure notes on this device"
                                        }
                                    }
                                }
                            }
                        ) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPasswordDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (showUnlockDialog) {
                AlertDialog(
                    onDismissRequest = { showUnlockDialog = false },
                    title = { Text("Unlock Notes") },
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
                                val savedHash = try {
                                    prefs.getString(PASSWORD_KEY, null)
                                } catch (e: Exception) {
                                    null
                                }

                                if (savedHash != null && hashPassword(passwordInput) == savedHash) {
                                    try {
                                        val salt = getOrCreateSalt(prefs)
                                        val key = deriveKey(passwordInput, salt)
                                        cryptoKey = key

                                        val loaded = safeLoad(prefs, key)
                                        itemList.clear()
                                        itemList.addAll(loaded)

                                        isLocked = false
                                        passwordInput = ""
                                        passwordError = ""
                                        showUnlockDialog = false
                                    } catch (e: Exception) {
                                        passwordError = "Could not read notes"
                                    }
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

            if (showRemovePasswordDialog) {
                AlertDialog(
                    onDismissRequest = { showRemovePasswordDialog = false },
                    title = { Text("Remove Password") },
                    text = {
                        Column {
                            Text(
                                text = "Enter the current password. Your notes will be saved without encryption.",
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )

                            OutlinedTextField(
                                value = removePasswordInput,
                                onValueChange = {
                                    removePasswordInput = it
                                    removePasswordError = ""
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Current password") },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true
                            )

                            AnimatedVisibility(removePasswordError.isNotEmpty()) {
                                Text(
                                    text = removePasswordError,
                                    color = Color(0xFFD32F2F),
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val savedHash = try {
                                    prefs.getString(PASSWORD_KEY, null)
                                } catch (e: Exception) {
                                    null
                                }

                                if (savedHash != null && hashPassword(removePasswordInput) == savedHash) {
                                    try {
                                        safeSave(prefs, itemList, null)
                                        prefs.edit()
                                            .remove(PASSWORD_KEY)
                                            .remove(SALT_KEY)
                                            .apply()

                                        hasPassword = false
                                        isLocked = false
                                        cryptoKey = null

                                        removePasswordInput = ""
                                        removePasswordError = ""
                                        showRemovePasswordDialog = false
                                    } catch (e: Exception) {
                                        removePasswordError = "Could not remove password"
                                    }
                                } else {
                                    removePasswordError = "Wrong password"
                                }
                            }
                        ) {
                            Text("Remove")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                removePasswordInput = ""
                                removePasswordError = ""
                                showRemovePasswordDialog = false
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
        colors = CardDefaults.cardColors(containerColor = item.color),
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

            if (!locked) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete note"
                    )
                }
            }
        }
    }
}

private fun safeSave(
    prefs: SharedPreferences,
    notes: List<TodoItem>,
    key: SecretKey?
) {
    try {
        val serialized = notes.joinToString("\n") { item ->
            val argb = item.color.toArgb().toLong() and 0xFFFFFFFFL
            val encodedText = Base64.encodeToString(
                item.text.toByteArray(Charsets.UTF_8),
                Base64.NO_WRAP
            )
            "${item.id}|$argb|$encodedText"
        }

        if (key != null) {
            val encrypted = try {
                encryptText(serialized, key)
            } catch (e: Exception) {
                serialized
            }
            prefs.edit()
                .putString(NOTES_KEY, encrypted)
                .putBoolean(NOTES_ENCRYPTED_KEY, encrypted != serialized)
                .apply()
        } else {
            prefs.edit()
                .putString(NOTES_KEY, serialized)
                .putBoolean(NOTES_ENCRYPTED_KEY, false)
                .apply()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun safeLoad(
    prefs: SharedPreferences,
    key: SecretKey?
): List<TodoItem> {
    return try {
        val stored = prefs.getString(NOTES_KEY, null) ?: return emptyList()
        val isEncrypted = prefs.getBoolean(NOTES_ENCRYPTED_KEY, false)

        val serialized = if (isEncrypted) {
            val k = key ?: return emptyList()
            try {
                decryptText(stored, k)
            } catch (e: Exception) {
                return emptyList()
            }
        } else {
            stored
        }

        serialized
            .split("\n")
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                try {
                    val parts = line.split("|", limit = 3)
                    if (parts.size != 3) return@mapNotNull null

                    val id = parts[0].toLongOrNull() ?: System.nanoTime()
                    val argb = parts[1].toLongOrNull() ?: return@mapNotNull null
                    val text = String(
                        Base64.decode(parts[2], Base64.NO_WRAP),
                        Charsets.UTF_8
                    )
                    TodoItem(id = id, text = text, color = Color(argb.toInt()))
                } catch (e: Exception) {
                    null
                }
            }
    } catch (e: Exception) {
        emptyList()
    }
}

private fun deriveKey(password: String, salt: ByteArray): SecretKey {
    val spec = PBEKeySpec(password.toCharArray(), salt, 10_000, 128)
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
    return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
}

private fun encryptText(plain: String, key: SecretKey): String {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    val iv = ByteArray(GCM_IV_LENGTH)
    SecureRandom().nextBytes(iv)
    cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
    val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
    return Base64.encodeToString(iv + encrypted, Base64.NO_WRAP)
}

private fun decryptText(data: String, key: SecretKey): String {
    val raw = Base64.decode(data, Base64.NO_WRAP)
    if (raw.size <= GCM_IV_LENGTH) throw IllegalArgumentException("bad data")
    val iv = raw.copyOfRange(0, GCM_IV_LENGTH)
    val encrypted = raw.copyOfRange(GCM_IV_LENGTH, raw.size)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
    return String(cipher.doFinal(encrypted), Charsets.UTF_8)
}

private fun getOrCreateSalt(prefs: SharedPreferences): ByteArray {
    val stored = prefs.getString(SALT_KEY, null)
    if (stored != null) {
        return Base64.decode(stored, Base64.NO_WRAP)
    }
    val salt = ByteArray(16)
    SecureRandom().nextBytes(salt)
    prefs.edit()
        .putString(SALT_KEY, Base64.encodeToString(salt, Base64.NO_WRAP))
        .apply()
    return salt
}

private fun hashPassword(password: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes = digest.digest(password.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
}