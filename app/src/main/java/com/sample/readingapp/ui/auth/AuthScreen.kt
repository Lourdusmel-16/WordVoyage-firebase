package com.sample.readingapp.ui.auth

import android.util.Patterns
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.sample.readingapp.data.*
import com.sample.readingapp.theme.*
import com.sample.readingapp.ui.components.*
import com.sample.readingapp.util.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ---------------- AUTH SCREEN ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    auth: FirebaseAuth,
    pageBgColor: Color,
    pageTextColor: Color,
    accentColor: Color,
    readerTheme: ReaderTheme,
    onThemeChanged: (ReaderTheme) -> Unit,
    onLoginSuccess: (isAdmin: Boolean, username: String, ageGroup: AgeGroup) -> Unit,
    onRegisterSuccess: (isAdmin: Boolean, username: String, ageGroup: AgeGroup) -> Unit
) {
    var isSignUp by remember { mutableStateOf(false) }

    var usernameInput by remember { mutableStateOf("") }
    var selectedAgeGroup by remember { mutableStateOf(AgeGroup.GRADES_4_5) }
    var isAgeDropdownExpanded by remember { mutableStateOf(false) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var wantsAdminAccess by remember { mutableStateOf(false) }
    var enteredAdminPasscode by remember { mutableStateOf("") }
    var showThemeMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.dp, end = 6.dp)
        ) {
            OutlinedButton(
                onClick = { showThemeMenu = true },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.35f)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(34.dp)
            ) {
                IconLabel(readerTheme.title, readerTheme.icon, fontSize = 12.sp, color = pageTextColor)
            }

            DropdownMenu(
                expanded = showThemeMenu,
                onDismissRequest = { showThemeMenu = false }
            ) {
                ReaderTheme.values().forEach { theme ->
                    DropdownMenuItem(
                        text = { IconLabel(theme.title, theme.icon) },
                        onClick = {
                            onThemeChanged(theme)
                            showThemeMenu = false
                        }
                    )
                }
                MatchDeviceMenuItem(onThemeChanged = onThemeChanged, onDone = { showThemeMenu = false })
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(pageBgColor, RoundedCornerShape(22.dp))
                .border(1.5.dp, BrandShadeBorder, RoundedCornerShape(22.dp))
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(BrandShadeSoft, CircleShape)
                    .border(1.dp, BrandShadeMedium, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.MenuBook, contentDescription = null, tint = BrandShadeBold, modifier = Modifier.size(28.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = APP_DISPLAY_NAME,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = pageTextColor
            )
            Text(
                text = if (isSignUp) "Create your reader profile & start" else "Welcome back! Sign in to continue",
                fontSize = 13.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(18.dp))

            if (isSignUp) {
                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = { usernameInput = it; errorMessage = null },
                    label = { Text("Reader's Username") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = themedFieldColors(pageTextColor),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                ExposedDropdownMenuBox(
                    expanded = isAgeDropdownExpanded,
                    onExpandedChange = { isAgeDropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedAgeGroup.title,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Reading Grade Group") },
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = pageTextColor)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = themedFieldColors(pageTextColor),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = isAgeDropdownExpanded,
                        onDismissRequest = { isAgeDropdownExpanded = false }
                    ) {
                        AgeGroup.values().forEach { group ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(group.title, fontWeight = FontWeight.SemiBold)
                                        Text("Reading Level: ${group.badge}", fontSize = 12.sp, color = Color.Gray)
                                    }
                                },
                                onClick = {
                                    selectedAgeGroup = group
                                    isAgeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; errorMessage = null },
                label = { Text("Email Address") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = themedFieldColors(pageTextColor),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                label = { Text("Password (min 6 characters)") },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (showPassword) "Hide password" else "Show password",
                            tint = pageTextColor
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = themedFieldColors(pageTextColor),
                modifier = Modifier.fillMaxWidth()
            )

            if (isSignUp) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        if (it.isNotEmpty() && password.trim() != it.trim()) {
                            errorMessage = "Password mismatch"
                        } else {
                            errorMessage = null
                        }
                    },
                    label = { Text("Re-enter Password") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (showPassword) "Hide password" else "Show password",
                                tint = pageTextColor
                            )
                        }
                    },
                    isError = confirmPassword.isNotEmpty() && password.trim() != confirmPassword.trim(),
                    shape = RoundedCornerShape(12.dp),
                    colors = themedFieldColors(pageTextColor),
                    modifier = Modifier.fillMaxWidth()
                )

                if (confirmPassword.isNotEmpty() && password.trim() != confirmPassword.trim()) {
                    Text(
                        text = "Password mismatch",
                        color = Color(0xFFDC2626),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, start = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = wantsAdminAccess,
                    onCheckedChange = { wantsAdminAccess = it }
                )
                Text("Sign in as Admin", fontSize = 13.sp, color = pageTextColor)
            }

            if (wantsAdminAccess) {
                OutlinedTextField(
                    value = enteredAdminPasscode,
                    onValueChange = { enteredAdminPasscode = it; errorMessage = null },
                    label = { Text("Admin Passcode") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(12.dp),
                    colors = themedFieldColors(pageTextColor),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            errorMessage?.let {
                Text(text = it, color = Color(0xFFDC2626), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    val cleanEmail = email.trim()
                    val cleanPassword = password.trim()
                    val cleanUsername = usernameInput.trim()

                    if (isSignUp && cleanUsername.isEmpty()) {
                        errorMessage = "Please enter a username"
                        return@Button
                    }
                    if (cleanEmail.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
                        errorMessage = "Please enter a valid email address"
                        return@Button
                    }
                    if (cleanPassword.length < 6) {
                        errorMessage = "Password must be at least 6 characters"
                        return@Button
                    }
                    if (isSignUp && cleanPassword != confirmPassword.trim()) {
                        errorMessage = "Password mismatch"
                        return@Button
                    }
                    if (wantsAdminAccess && enteredAdminPasscode.trim().isEmpty()) {
                        errorMessage = "Incorrect Admin Passcode"
                        return@Button
                    }

                    val task = if (isSignUp) {
                        auth.createUserWithEmailAndPassword(cleanEmail, cleanPassword)
                    } else {
                        auth.signInWithEmailAndPassword(cleanEmail, cleanPassword)
                    }

                    task.addOnSuccessListener { authResult ->
                        val user = authResult.user
                        if (user != null) {
                            scope.launch {
                                // The admin passcode is NOT stored in the app. It is checked by Firestore security
                                // rules against a private config/admin document that no app can read (see SETUP.md).
                                var isVerifiedAdmin = false
                                if (wantsAdminAccess) {
                                    try {
                                        FirebaseFirestore.getInstance().collection("adminClaims").document(user.uid)
                                            .set(mapOf("code" to enteredAdminPasscode.trim())).await()
                                        isVerifiedAdmin = true
                                    } catch (e: Exception) {
                                        val denied = (e as? FirebaseFirestoreException)?.code ==
                                                FirebaseFirestoreException.Code.PERMISSION_DENIED
                                        if (isSignUp) {
                                            try { user.delete().await() } catch (_: Exception) { auth.signOut() }
                                        } else {
                                            auth.signOut()
                                        }
                                        errorMessage = if (denied) "Incorrect Admin Passcode"
                                        else "Could not check the Admin Passcode. Please check your internet connection."
                                        return@launch
                                    }
                                }
                                try {
                                    val userRef = FirebaseFirestore.getInstance().collection("users").document(user.uid)

                                    if (isSignUp) {
                                        val userData = hashMapOf(
                                            "username" to cleanUsername,
                                            "ageGroup" to selectedAgeGroup.name,
                                            "email" to cleanEmail,
                                            "isAdmin" to isVerifiedAdmin,
                                            "lastLogin" to System.currentTimeMillis()
                                        )
                                        userRef.set(userData, SetOptions.merge()).await()
                                        onRegisterSuccess(isVerifiedAdmin, cleanUsername, selectedAgeGroup)
                                    } else {
                                        val snap = userRef.get().await()
                                        val savedName = snap.getString("username") ?: cleanEmail.substringBefore("@")
                                        val savedAgeGroup = AgeGroup.fromString(snap.getString("ageGroup"))

                                        if (isVerifiedAdmin) {
                                            userRef.set(mapOf("isAdmin" to true), SetOptions.merge()).await()
                                        }

                                        onLoginSuccess(isVerifiedAdmin, savedName, savedAgeGroup)
                                    }
                                } catch (_: Exception) {
                                    if (isSignUp) onRegisterSuccess(isVerifiedAdmin, cleanUsername, selectedAgeGroup)
                                    else onLoginSuccess(isVerifiedAdmin, cleanEmail.substringBefore("@"), AgeGroup.GRADES_4_5)
                                }
                            }
                        }
                    }.addOnFailureListener { errorMessage = it.localizedMessage }
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (isSignUp) {
                    Text("Complete Sign Up", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                } else {
                    Text("Sign In", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            TextButton(onClick = { isSignUp = !isSignUp; errorMessage = null }) {
                Text(
                    if (isSignUp) "Already have an account? Sign In" else "New reader? Create an account",
                    color = BrandShadeBold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
