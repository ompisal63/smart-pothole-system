package com.example.smartpothole
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.smartpothole.ui.theme.SmartPotholeTheme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit


data class Complaint(
    val complaint_id: String,
    val full_name: String,
    val mobile: String,
    val location_description: String,
    val status: String,
    val timestamp: String
)

data class ComplaintDetailResponse(
    val authority_id: String,
    val complaint: ComplaintDetail,
    val media: MediaInfo,
    val workflow: WorkflowInfo
)

data class ComplaintDetail(
    val complaint_id: String,
    val full_name: String,
    val email: String,
    val mobile: String,
    val latitude: String,
    val longitude: String,
    val location_description: String,
    val timestamp: String,
    val status: String,
    val assigned_to: String,
    val assigned_by: String,
    val assigned_at: String,
    val last_updated: String
)

data class MediaInfo(
    val image_url: String
)

data class WorkflowInfo(
    val allowed_status: List<String>,
    val allowed_assignees: List<String>
)


enum class ComplaintFilter {
    ALL, TODAY, OPEN, RESOLVED
}


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SmartPotholeTheme {
                AppNavHost()
            }
        }
    }
}
@Composable
fun AppNavHost() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "selection"
    ) {

        composable("selection") {
            SelectionScreen(
                onUserClick = { navController.navigate("user") },
                onAuthorityClick = { navController.navigate("authority") }
            )
        }

        composable("user") {
            UserScreen(
                onBack = { navController.popBackStack() },
                onRaiseComplaint = { uri ->
                    navController.navigate(
                        "complaint/${Uri.encode(uri.toString())}"
                    )
                }
            )
        }

        composable("authority") {
            AuthorityLoginScreen(
                onBack = { navController.popBackStack() },
                onLoginSuccess = {
                    navController.navigate("authority_dashboard") {
                        popUpTo("authority") { inclusive = true }
                    }
                }
            )
        }


        composable("authority_dashboard") {
            AuthorityDashboardScreen(
                onLogout = {
                    navController.navigate("selection") {
                        popUpTo(0)
                    }
                },
                onOpenComplaint = { complaintId ->
                    navController.navigate("complaint_detail/$complaintId")
                }
            )
        }


        composable(
            route = "complaint_detail/{complaintId}",
            arguments = listOf(
                androidx.navigation.navArgument("complaintId") {
                    type = androidx.navigation.NavType.StringType
                }
            )
        ) { backStackEntry ->
            val complaintId =
                backStackEntry.arguments?.getString("complaintId") ?: ""

            AuthorityComplaintDetailScreen(
                complaintId = complaintId,
                onBack = { navController.popBackStack() }
            )
        }


        composable(
            route = "complaint/{imageUri}",
            arguments = listOf(
                androidx.navigation.navArgument("imageUri") {
                    type = androidx.navigation.NavType.StringType
                }
            )
        ) { backStackEntry ->
            val imageUri =
                backStackEntry.arguments?.getString("imageUri")

            ComplaintScreen(
                onBack = { navController.popBackStack() },
                imageUri = imageUri,
                onSubmitSuccess = { complaintId ->
                    navController.navigate(
                        "success/${Uri.encode(complaintId)}"
                    )
                },
                onSubmitStart = {
                    navController.navigate("processing")
                }
            )
        }

        // 🟡 NEW: PROCESSING SCREEN
        composable("processing") {
            ProcessingScreen()
        }

        // 🟢 NEW: SUCCESS SCREEN
        composable(
            route = "success/{complaintId}",
            arguments = listOf(
                androidx.navigation.navArgument("complaintId") {
                    type = androidx.navigation.NavType.StringType
                }
            )
        ) { backStackEntry ->
            val complaintId =
                backStackEntry.arguments?.getString("complaintId") ?: ""

            SuccessScreen(
                complaintId = complaintId,
                onGoHome = {
                    navController.navigate("selection") {
                        popUpTo(0)
                    }
                }
            )
        }
    }
}
@Composable
fun SelectionScreen(
    onUserClick: () -> Unit,
    onAuthorityClick: () -> Unit
) {

    val alphaAnim = remember { Animatable(0f) }
    val offsetAnim = remember { Animatable(30f) }

    LaunchedEffect(Unit) {
        alphaAnim.animateTo(
            1f,
            animationSpec = tween(800, easing = FastOutSlowInEasing)
        )
        offsetAnim.animateTo(
            0f,
            animationSpec = tween(800, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF020617),
                        Color(0xFF020617),
                        Color(0xFF0B1220)
                    )
                )
            )
    ) {

        // Subtle top glow (flagship touch)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x3314B8A6),
                            Color.Transparent
                        ),
                        radius = 600f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
                .graphicsLayer {
                    alpha = alphaAnim.value
                    translationY = offsetAnim.value
                },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Brand Title
            Text(
                text = "Smart Pothole",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "AI-powered Road Safety Platform",
                fontSize = 14.sp,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Primary CTA
            PremiumButton(
                text = "Continue as User",
                onClick = onUserClick,
                backgroundColor = Color(0xFF0F766E),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Secondary CTA
            OutlinedButton(
                onClick = onAuthorityClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFE5E7EB)
                )
            ) {
                Text(
                    text = "Authority Login",
                    fontSize = 17.sp
                )
            }
        }
    }
}
@Composable
fun AuthorityLoginScreen(
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var authorityId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf<String?>(null) }
    var isLoggingIn by remember { mutableStateOf(false) }
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "←",
            color = Color.White,
            fontSize = 22.sp,
            modifier = Modifier.clickable { onBack() }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF020617),
                        Color(0xFF020617)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(
                    color = Color(0xFF020617),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Authority Access",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Restricted infrastructure portal",
                fontSize = 13.sp,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(top = 6.dp, bottom = 28.dp)
            )

            OutlinedTextField(
                value = authorityId,
                onValueChange = { authorityId = it },
                placeholder = { Text("Authority ID") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF334155),
                    unfocusedBorderColor = Color(0xFF1E293B),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )


            Spacer(modifier = Modifier.height(16.dp))

            var passwordVisible by remember { mutableStateOf(false) }
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("Password") },
                singleLine = true,
                visualTransformation =
                    if (passwordVisible)
                        androidx.compose.ui.text.input.VisualTransformation.None
                    else
                        PasswordVisualTransformation(),

                trailingIcon = {
                    Text(
                        text = if (passwordVisible) "🙈" else "👁",
                        modifier = Modifier.clickable {
                            passwordVisible = !passwordVisible
                        }
                    )
                },

                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF334155),
                    unfocusedBorderColor = Color(0xFF1E293B),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )


            Spacer(modifier = Modifier.height(28.dp))

            AnimatedVisibility(
                visible = loginError != null,
                enter = fadeIn() + slideInVertically()
            ) {
                Text(
                    text = loginError ?: "",
                    color = Color(0xFFEF4444),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Button(
                onClick = {
                    if (isLoggingIn) return@Button
                    isLoggingIn = true
                    loginError = null

                    val client = OkHttpClient()

                    val json = JSONObject().apply {
                        put("authority_id", authorityId)
                        put("password", password)
                    }

                    val body = json.toString()
                        .toRequestBody("application/json".toMediaType())

                    val request = Request.Builder()
                        .url("http://172.20.10.3:8000/authority/login")
                        .post(body)
                        .build()

                    client.newCall(request).enqueue(object : Callback {

                        override fun onFailure(call: Call, e: IOException) {
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                loginError = "Network error. Try again."
                                isLoggingIn = false
                            }
                        }

                        override fun onResponse(call: Call, response: Response) {
                            val responseBody = response.body?.string()

                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                if (!response.isSuccessful || responseBody == null) {
                                    loginError = "Incorrect authority ID or password"
                                    isLoggingIn = false
                                    return@post
                                }

                                val token = JSONObject(responseBody)
                                    .getString("access_token")

                                val prefs = context.getSharedPreferences(
                                    "AUTH",
                                    android.content.Context.MODE_PRIVATE
                                )

                                prefs.edit().putString("TOKEN", token).apply()
                                isLoggingIn = false
                                onLoginSuccess()
                            }
                        }
                    })
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E293B)
                )
            ) {
                Text(
                    text = "Login",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScreen(
    onBack: () -> Unit,
    onRaiseComplaint: (Uri) -> Unit
)
{
    var analysisText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var isAnalyzing by remember { mutableStateOf(false) }
    var confidence by remember { mutableStateOf<Float?>(null) }
    var isPothole by remember { mutableStateOf<Boolean?>(null) }
    var showResult by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val photoUri = remember { mutableStateOf<Uri?>(null) }
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedImageUri = photoUri.value
        }
    }
    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {

                val photoFile = File(
                    context.cacheDir,
                    "camera_${System.currentTimeMillis()}.jpg"
                )

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    photoFile
                )

                photoUri.value = uri
                cameraLauncher.launch(uri)
            }
        }
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImageUri = uri
    }

    var showPicker by remember { mutableStateOf(false)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF020617),
                        Color(0xFF0B1220)
                    )
                )
            )
    ) {

        // 🔝 Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "←",
                color = Color.White,
                fontSize = 22.sp,
                modifier = Modifier.clickable { onBack() }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Upload Image",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // 👇 Screen Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "Upload Road Image",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "Help us detect potholes using AI",
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(top = 6.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF020617)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {

                    Box(modifier = Modifier.fillMaxSize()) {

                        // Image / Placeholder (already done)
                        if (selectedImageUri == null) {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("📷", fontSize = 32.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No image selected",
                                    color = Color(0xFF64748B),
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            Text(
                                text = "✕",
                                color = Color.White,
                                fontSize = 18.sp,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp)
                                    .clickable {
                                        selectedImageUri = null
                                        confidence = null
                                    }
                            )
                        }

                        // 🔥 ANALYZING OVERLAY
                        if (isAnalyzing) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.55f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = Color.White)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = analysisText,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                }

                Spacer(modifier = Modifier.height(28.dp))

                // Upload button (will open options later)
                if (selectedImageUri == null) {
                    Button(
                        onClick = { showPicker = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0F766E)
                        )
                    ) {
                        Text(
                            text = "Upload Image",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }


                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        isAnalyzing = true
                        analysisText = "Analyzing image using AI…"

                        analyzeImageWithModel(
                            context = context,
                            imageUri = selectedImageUri!!,
                            onResult = { conf, pothole ->
                                confidence = conf
                                isPothole = pothole
                                showResult = true
                                isAnalyzing = false
                            },
                            onError = { err ->
                                android.util.Log.e("MODEL", err)
                                isAnalyzing = false   // stop spinner
                            }
                        )
                    },
                    enabled = selectedImageUri != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("Analyze Image")
                }




                Spacer(modifier = Modifier.height(20.dp))


                if (showResult && confidence != null) {

                    val conf = confidence!!
                    val verdictColor =
                        if (isPothole == true) Color(0xFF16A34A)
                        else Color(0xFFDC2626)

                    Spacer(modifier = Modifier.height(24.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
                        border = BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            // ✅ MAIN VERDICT
                            Text(
                                text = if (isPothole == true) "Pothole Detected" else "No Pothole Detected",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = verdictColor
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // ✅ SUPPORTING CONFIDENCE (only once)
                            Text(
                                text = "AI confidence ${(conf * 100).toInt()}%",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // ✅ CLEAN PROGRESS BAR
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .background(Color(0xFF1E293B), RoundedCornerShape(6.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(conf.coerceIn(0f, 1f))
                                        .height(6.dp)
                                        .background(verdictColor, RoundedCornerShape(6.dp))
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // ✅ DECISION NOTE (aligned, calm)
                            Text(
                                text = if (isPothole == true)
                                    "Road damage detected. You may proceed to raise a complaint."
                                else
                                    "No significant road damage detected in this image.",
                                fontSize = 14.sp,
                                color = Color(0xFFCBD5E1),
                                textAlign = TextAlign.Center
                            )

                            // ✅ PRIMARY ACTION (FLAGSHIP)
                            if (isPothole == true) {
                                Spacer(modifier = Modifier.height(22.dp))

                                if (isPothole == true) {
                                    Button(
                                        onClick = {
                                            selectedImageUri?.let {
                                                onRaiseComplaint(it)
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF16A34A)
                                        )
                                    ) {
                                        Text("Raise Complaint")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }




    if (showPicker) {
        ModalBottomSheet(
            onDismissRequest = { showPicker = false },
            containerColor = Color(0xFF020617)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                Text(
                    text = "Upload Image",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Divider(color = Color(0xFF1E293B))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showPicker = false

                            val photoFile = File(
                                context.cacheDir,
                                "camera_${System.currentTimeMillis()}.jpg"
                            )

                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                photoFile
                            )

                            photoUri.value = uri
                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📷", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Capture using Camera",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showPicker = false
                            galleryLauncher.launch("image/*")
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🖼️", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Choose from Gallery",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
fun PremiumButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color
) {
    val scale = remember { Animatable(1f) }

    Button(
        onClick = onClick,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 8.dp,
            pressedElevation = 2.dp
        ),
        interactionSource = remember {
            MutableInteractionSource()
        }
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
    }

    LaunchedEffect(Unit) {
        scale.snapTo(1f)
    }
}

fun analyzeImageWithModel(
    context: android.content.Context,
    imageUri: Uri,
    onResult: (Float, Boolean) -> Unit,
    onError: (String) -> Unit
) {
    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val file = File(context.cacheDir, "upload.jpg")

    context.contentResolver.openInputStream(imageUri)?.use { input ->
        file.outputStream().use { output ->
            input.copyTo(output)
        }
    }

    val requestBody = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart(
            "file",
            file.name,
            file.asRequestBody("image/jpeg".toMediaType())
        )
        .build()

    val request = Request.Builder()
        .url("http://172.20.10.3:8000/predict")
        .post(requestBody)
        .build()

    client.newCall(request).enqueue(object : Callback {

        override fun onFailure(call: Call, e: IOException) {
            (android.os.Handler(android.os.Looper.getMainLooper())).post {
                onError(e.message ?: "Network error")
            }
        }

        override fun onResponse(call: Call, response: Response) {
            val body = response.body?.string()

            if (!response.isSuccessful || body == null) {
                onError("Server error")
                return
            }

            val json = JSONObject(body)
            val confidence = json.getDouble("confidence").toFloat()
            val isPothole = json.getBoolean("is_pothole")

            (android.os.Handler(android.os.Looper.getMainLooper())).post {
                onResult(confidence, isPothole)
            }

        }
    })
}

@Composable
fun ComplaintScreen(
    onBack: () -> Unit,
    imageUri: String?,
    onSubmitStart: () -> Unit,
    onSubmitSuccess: (String) -> Unit
)
{
    val topBarHeight = 64.dp
    val context = LocalContext.current
    var isSubmitting by remember { mutableStateOf(false) }
    var selectedLatLng by remember { mutableStateOf<LatLng?>(null) }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var locationDescription by remember { mutableStateOf("") }
    val isFormValid =
        fullName.isNotBlank() &&
                mobile.length == 10 &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
                selectedLatLng != null &&
                locationDescription.isNotBlank()


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF020617),
                        Color(0xFF0B1220)
                    )
                )
            )
    ) {

        // 🔝 Top bar (FIXED, NON-SCROLLING)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(topBarHeight)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "←",
                color = Color.White,
                fontSize = 22.sp,
                modifier = Modifier.clickable { onBack() }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Road Damage Complaint",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // 🔽 SCROLLABLE CONTENT (THIS WAS BROKEN)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = topBarHeight,
                    start = 20.dp,
                    end = 20.dp
                ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {

            // 🔹 Complaint ID
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Complaint ID",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "Will be generated after submission",
                            fontSize = 13.sp,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
            }
            // 🔹 User Details
            item { SectionTitle("User Details") }

            item {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    placeholder = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF16A34A),
                        focusedBorderColor = Color(0xFF16A34A),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedPlaceholderColor = Color(0xFF94A3B8),
                        unfocusedPlaceholderColor = Color(0xFF64748B)
                    )
                )
            }

            item {
                val isMobileValid = mobile.length == 10
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { input ->
                        if (input.length <= 10 && input.all { it.isDigit() }) {
                            mobile = input
                        }
                    },
                    leadingIcon = {
                        Text(
                            text = "+91",
                            color = Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    },
                    placeholder = { Text("Mobile number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    isError = mobile.isNotEmpty() && !isMobileValid,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        disabledTextColor = Color.White,
                        errorTextColor = Color.White,

                        cursorColor = Color(0xFF16A34A),

                        focusedBorderColor = Color(0xFF16A34A),
                        unfocusedBorderColor = Color(0xFF334155),
                        errorBorderColor = Color(0xFFDC2626),

                        focusedPlaceholderColor = Color(0xFF94A3B8),
                        unfocusedPlaceholderColor = Color(0xFF64748B)
                    )
                )
                if (mobile.isNotEmpty() && !isMobileValid) {
                    Text(
                        text = "Enter valid 10-digit mobile number",
                        color = Color(0xFFDC2626),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            item {
                val isEmailValid = remember(email) {
                    android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.trim() },
                    placeholder = { Text("Email address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    isError = email.isNotEmpty() && !isEmailValid,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        disabledTextColor = Color.White,
                        errorTextColor = Color.White,

                        cursorColor = Color(0xFF16A34A),

                        focusedBorderColor = Color(0xFF16A34A),
                        unfocusedBorderColor = Color(0xFF334155),
                        errorBorderColor = Color(0xFFDC2626),

                        focusedPlaceholderColor = Color(0xFF94A3B8),
                        unfocusedPlaceholderColor = Color(0xFF64748B)
                    )
                )
                if (email.isNotEmpty() && !isEmailValid) {
                    Text(
                        text = "Enter a valid email address",
                        color = Color(0xFFDC2626),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

            }
            // 🔹 Location
            item { SectionTitle("Select Location") }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(460.dp) // BIG MAP
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            BorderStroke(1.dp, Color(0xFF1E293B)),
                            RoundedCornerShape(16.dp)
                        )
                ) {
                    SelectableMap(
                        selectedLatLng = selectedLatLng,
                        onLocationSelected = { latLng ->
                            selectedLatLng = latLng
                        }
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = locationDescription,
                    onValueChange = { locationDescription = it },
                    placeholder = { Text("Exact location description") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF16A34A),
                        focusedBorderColor = Color(0xFF16A34A),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedPlaceholderColor = Color(0xFF94A3B8),
                        unfocusedPlaceholderColor = Color(0xFF64748B)
                    )
                )

                selectedLatLng?.let {
                    Text(
                        text = "Latitude: ${it.latitude}, Longitude: ${it.longitude}",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }



            // 🔹 Image Preview
            item { SectionTitle("AI-Verified Image") }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        imageUri?.let {
                            AsyncImage(
                                model = Uri.parse(it),
                                contentDescription = "AI verified road image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }


                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(10.dp)
                                .background(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Image verified by AI analysis",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                }
            }

            // 🔹 Submit Button (INLINE, PROFESSIONAL)
            item {
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = {

                            if (isSubmitting) return@Button
                            isSubmitting = true

                            val latLng = selectedLatLng ?: return@Button
                            val image = imageUri ?: return@Button

                            // Show processing screen
                            onSubmitStart()

                            submitComplaintToBackend(
                                context = context,
                                fullName = fullName,
                                email = email,
                                mobile = mobile,
                                latitude = latLng.latitude.toString(),
                                longitude = latLng.longitude.toString(),
                                locationDescription = locationDescription,
                                imageUri = Uri.parse(image),

                                onSuccess = { backendComplaintId ->
                                    isSubmitting = false
                                    onSubmitSuccess(backendComplaintId)
                                },

                                onError = { error ->
                                    isSubmitting = false
                                    android.util.Log.e("COMPLAINT", error)
                                }
                            )
                        },
                        enabled = isFormValid,
                        modifier = Modifier
                            .width(220.dp) // 👈 professional size
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF16A34A),
                            disabledContainerColor = Color(0xFF1E293B)
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 3.dp,
                            pressedElevation = 1.dp
                        )
                    ) {
                        Text(
                            text = "Submit Complaint",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }

    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.White
    )
}

@Composable
fun SelectableMap(
    selectedLatLng: LatLng?,
    onLocationSelected: (LatLng) -> Unit
) {

    val context = LocalContext.current

    var locating by remember { mutableStateOf(false) }


    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            // nothing here yet, just request permission
        }


    val locationPermissionGranted =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    var searchText by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        searchText = ""
    }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var searchResults by remember { mutableStateOf<List<NominatimResult>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val defaultLocation = LatLng(19.0760, 72.8777) // Mumbai

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 13f)
    }

    Box(modifier = Modifier.fillMaxSize()) {

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = false
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = true,
                myLocationButtonEnabled = false,
                scrollGesturesEnabled = true,
                zoomGesturesEnabled = true,
                tiltGesturesEnabled = false
            ),
            onMapClick = { latLng ->
                onLocationSelected(latLng)
            }
        ) {
            selectedLatLng?.let {
                Marker(
                    state = MarkerState(it),
                    title = "Selected Location"
                )
            }
        }


        // 🔍 SEARCH BAR OVERLAY (TOP)
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(0.95f),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
            border = BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { query ->
                    searchText = query

                    searchJob?.cancel()
                    searchJob = scope.launch {
                        kotlinx.coroutines.delay(500) // debounce

                        if (query.length >= 3) {
                            searchLocationNominatim(query) { results ->
                                searchResults = results
                            }
                        } else {
                            searchResults = emptyList()
                        }
                    }
                },
                placeholder = { Text("Search area, landmark…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF16A34A),
                    unfocusedBorderColor = Color(0xFF1E293B),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedPlaceholderColor = Color(0xFF94A3B8),
                    unfocusedPlaceholderColor = Color(0xFF64748B)
                )
            )
        }

        if (searchResults.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 64.dp)
                    .fillMaxWidth(0.95f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 220.dp)
                ) {
                    items(searchResults.size) { index ->
                        val place = searchResults[index]

                        Text(
                            text = place.display_name,
                            color = Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val lat = place.lat.toDouble()
                                    val lon = place.lon.toDouble()
                                    val latLng = LatLng(lat, lon)

                                    onLocationSelected(latLng)

                                    scope.launch {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(latLng, 16f)
                                        )
                                    }

                                    searchResults = emptyList()
                                }

                                .padding(14.dp)
                        )

                        if (index != searchResults.lastIndex) {
                            Divider(color = Color(0xFF1E293B))
                        }
                    }
                }

            }
        }

        // ✅ NOW align() IS LEGAL
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {


        }

        if (locating) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Using your current location…",
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }
        // 📍 Custom Current Location Button (BOTTOM-LEFT)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 16.dp)
                .size(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable {
                    if (locationPermissionGranted) {
                        locating = true
                        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                            locating = false
                            location?.let {
                                val latLng = LatLng(it.latitude, it.longitude)
                                onLocationSelected(latLng)
                                scope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(latLng, 17f)
                                    )
                                }
                            }
                        }
                    } else {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "📍",
                fontSize = 20.sp,
                color = Color.White
            )
        }

    }

    DisposableEffect(Unit) {
        onDispose {
            searchJob?.cancel()
        }
    }
}

data class NominatimResult(
    val display_name: String,
    val lat: String,
    val lon: String
)
fun searchLocationNominatim(
    query: String,
    onResult: (List<NominatimResult>) -> Unit
) {
    val url =
        "https://nominatim.openstreetmap.org/search" +
                "?q=${java.net.URLEncoder.encode(query, "UTF-8")}" +
                "&format=json" +
                "&addressdetails=1" +
                "&limit=5" +
                "&countrycodes=in" +
                "&accept-language=en"

    val request = Request.Builder()
        .url(url)
        .header("User-Agent", "SmartPotholeApp")
        .build()

    sharedHttpClient.newCall(request).enqueue(object : Callback {

        override fun onFailure(call: Call, e: IOException) {
            if (!call.isCanceled()) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    onResult(emptyList())
                }
            }
        }

        override fun onResponse(call: Call, response: Response) {

            if (!response.isSuccessful) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    onResult(emptyList())
                }
                return
            }

            val body = response.body?.string() ?: return

            if (!body.trim().startsWith("[")) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    onResult(emptyList())
                }
                return
            }

            val jsonArray = org.json.JSONArray(body)
            val results = mutableListOf<NominatimResult>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                results.add(
                    NominatimResult(
                        display_name = obj.getString("display_name"),
                        lat = obj.getString("lat"),
                        lon = obj.getString("lon")
                    )
                )
            }

            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onResult(results)
            }
        }
    })
}
private val sharedHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
}

fun submitComplaintToBackend(
    context: android.content.Context,
    fullName: String,
    email: String,
    mobile: String,
    latitude: String,
    longitude: String,
    locationDescription: String,
    imageUri: Uri,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {

    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val imageFile = File(context.cacheDir, "complaint_image.jpg")
    context.contentResolver.openInputStream(imageUri)?.use { input ->
        imageFile.outputStream().use { output ->
            input.copyTo(output)
        }
    }

    val requestBody = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("full_name", fullName)
        .addFormDataPart("email", email)
        .addFormDataPart("mobile", mobile)
        .addFormDataPart("latitude", latitude)
        .addFormDataPart("longitude", longitude)
        .addFormDataPart("location_description", locationDescription)
        .addFormDataPart(
            "image",
            imageFile.name,
            imageFile.asRequestBody("image/jpeg".toMediaType())
        )
        .build()

    val request = Request.Builder()
        .url("http://172.20.10.3:8000/authority/complaint")

        .post(requestBody)
        .build()

    client.newCall(request).enqueue(object : Callback {

        override fun onFailure(call: Call, e: IOException) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onError(e.message ?: "Network error")
            }
        }

        override fun onResponse(call: Call, response: Response) {
            val body = response.body?.string()

            if (!response.isSuccessful || body == null) {
                onError("Server error")
                return
            }

            val json = JSONObject(body)
            val complaintId = json.getString("complaint_id")

            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onSuccess(complaintId)
            }
        }
    })
}

@Composable
fun ProcessingScreen() {
    BackHandler(enabled = true) {
        // Disable back button during processing
    }

    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020617))
            .graphicsLayer { this.alpha = alpha.value },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            CircularProgressIndicator(
                color = Color(0xFF16A34A),
                strokeWidth = 4.dp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Registering your complaint",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Please wait while we securely submit your details",
                fontSize = 13.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SuccessScreen(
    complaintId: String,
    onGoHome: () -> Unit
) {

    val scale = remember { Animatable(0.8f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 500,
                easing = FastOutSlowInEasing
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020617)),
        contentAlignment = Alignment.Center
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "✅",
                fontSize = 54.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Complaint Registered",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Complaint ID",
                fontSize = 13.sp,
                color = Color(0xFF94A3B8)
            )

            Text(
                text = complaintId,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF16A34A)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "A confirmation email has been sent to your registered email address.",
                fontSize = 14.sp,
                color = Color(0xFFCBD5E1),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            Button(
                onClick = onGoHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF16A34A)
                )
            ) {
                Text(
                    text = "Go to Home",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
@Composable
fun AuthorityDashboardScreen(
    onLogout: () -> Unit,
    onOpenComplaint: (String) -> Unit
) {
    // 🔐 AUTH GUARD — PASTE STARTS HERE
    val context = LocalContext.current

    val prefs = context.getSharedPreferences(
        "AUTH",
        android.content.Context.MODE_PRIVATE
    )
    val token = prefs.getString("TOKEN", null)

    if (token == null) {
        LaunchedEffect(Unit) {
            onLogout()
        }
        return
    }
    // 🔐 AUTH GUARD — PASTE ENDS HERE val context = LocalContext.current

    var complaints by remember { mutableStateOf<List<Complaint>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(ComplaintFilter.ALL) }

    LaunchedEffect(Unit) {
        fetchComplaints(
            context = context,
            onSuccess = {
                complaints = it
                isLoading = false
            },
            onError = {
                isLoading = false
            }
        )
    }

    val searchFiltered = complaints.filter { c ->
        c.complaint_id.contains(searchQuery, ignoreCase = true) ||
                c.full_name.contains(searchQuery, ignoreCase = true) ||
                c.mobile.contains(searchQuery)
    }

    val filteredComplaints = searchFiltered.filter { c ->
        when (selectedFilter) {
            ComplaintFilter.ALL -> true
            ComplaintFilter.OPEN -> c.status == "OPEN"
            ComplaintFilter.RESOLVED -> c.status == "RESOLVED"
            ComplaintFilter.TODAY ->
                c.timestamp.startsWith(java.time.LocalDate.now().toString())
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020617))
    ) {

        // 🔝 APP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Authority Dashboard",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    Text(
                        text = "⟳",
                        color = Color.White,
                        fontSize = 18.sp,
                        modifier = Modifier.clickable {
                            isLoading = true

                            fetchComplaints(
                                context = context,
                                onSuccess = {
                                    complaints = it
                                    isLoading = false
                                },
                                onError = {
                                    isLoading = false
                                }
                            )
                        }
                    )

                    Text(
                        text = "Logout",
                        color = Color(0xFFEF4444),
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { onLogout() }
                    )
                }
            }
        }

        // 🔍 SEARCH BAR
        // 🔍 SEARCH BAR (PREMIUM)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF020617)
            ),
            border = BorderStroke(1.dp, Color(0xFF16A34A))
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        "Search by Complaint ID or Name ",
                        color = Color(0xFF94A3B8)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFF16A34A)
                )
            )
        }


        Spacer(modifier = Modifier.height(12.dp))

        // 🏷 FILTER ROW
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DashboardFilterChip("All", selectedFilter == ComplaintFilter.ALL) {
                selectedFilter = ComplaintFilter.ALL
            }
            DashboardFilterChip("Today", selectedFilter == ComplaintFilter.TODAY) {
                selectedFilter = ComplaintFilter.TODAY
            }
            DashboardFilterChip("Open", selectedFilter == ComplaintFilter.OPEN) {
                selectedFilter = ComplaintFilter.OPEN
            }
            DashboardFilterChip("Resolved", selectedFilter == ComplaintFilter.RESOLVED) {
                selectedFilter = ComplaintFilter.RESOLVED
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 📋 TABLE HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text("No", modifier = Modifier.weight(0.2f), color = Color(0xFF94A3B8), fontSize = 12.sp)
            Text("Complaint ID", modifier = Modifier.weight(0.6f), color = Color(0xFF94A3B8), fontSize = 12.sp)
            Text("Status", modifier = Modifier.weight(0.2f), color = Color(0xFF94A3B8), fontSize = 12.sp, textAlign = TextAlign.End)
        }


        Divider(color = Color(0xFF1E293B), modifier = Modifier.padding(vertical = 8.dp))

        // 📦 LIST
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(filteredComplaints.size) { index ->
                    DashboardComplaintRow(
                        srNo = index + 1,
                        complaint = filteredComplaints[index],
                        onClick = onOpenComplaint
                    )
                }
            }
        }
    }
}
@Composable
fun DashboardFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) Color(0xFF16A34A) else Color(0xFF1E293B)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 13.sp
        )
    }
}
@Composable
fun DashboardComplaintRow(
    srNo: Int,
    complaint: Complaint,
    onClick: (String) -> Unit
) {
    val statusColor = when (complaint.status) {
        "OPEN" -> Color(0xFFEF4444)
        "RESOLVED" -> Color(0xFF22C55E)
        else -> Color(0xFFF59E0B)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(complaint.complaint_id) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {

        // MAIN ROW (TABLE STYLE)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // No column
            Text(
                text = srNo.toString(),
                modifier = Modifier.weight(0.15f),
                color = Color(0xFF94A3B8),
                fontSize = 12.sp
            )

            // Complaint ID column
            Text(
                text = complaint.complaint_id,
                modifier = Modifier.weight(0.55f),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            // Status badge column
            Box(
                modifier = Modifier
                    .weight(0.30f),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            statusColor.copy(alpha = 0.15f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = complaint.status,
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // SUB ROW
        Text(
            text = "Reported by ${complaint.full_name}",
            fontSize = 12.sp,
            color = Color(0xFF94A3B8)
        )
    }

    // DIVIDER BETWEEN ROWS (CRITICAL)
    Divider(
        color = Color(0xFF1E293B),
        thickness = 1.dp
    )
}

fun fetchComplaints(
    context: android.content.Context,
    onSuccess: (List<Complaint>) -> Unit,
    onError: (String) -> Unit
) {
    val prefs = context.getSharedPreferences("AUTH", android.content.Context.MODE_PRIVATE)
    val token = prefs.getString("TOKEN", null)

    if (token == null) {
        onError("Unauthorized")
        return
    }

    val request = Request.Builder()
        .url("http://172.20.10.3:8000/authority/complaints")
        .addHeader("Authorization", "Bearer $token")
        .get()
        .build()

    OkHttpClient().newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            onError("Network error")
        }

        override fun onResponse(call: Call, response: Response) {
            val body = response.body?.string()

            if (!response.isSuccessful || body == null) {
                onError("Server error")
                return
            }

            val json = JSONObject(body)
            val list = json.getJSONArray("complaints")
            val result = mutableListOf<Complaint>()

            for (i in 0 until list.length()) {
                val o = list.getJSONObject(i)
                result.add(
                    Complaint(
                        complaint_id = o.getString("complaint_id"),
                        full_name = o.getString("full_name"),
                        mobile = o.getString("mobile"),
                        location_description = o.getString("location_description"),
                        status = o.optString("status", "OPEN"),
                        timestamp = o.getString("timestamp")
                    )
                )
            }

            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onSuccess(result)
            }
        }
    })
}
fun fetchComplaintDetail(
    context: android.content.Context,
    complaintId: String,
    onSuccess: (ComplaintDetailResponse) -> Unit,
    onError: (String) -> Unit
) {
    val prefs = context.getSharedPreferences("AUTH", android.content.Context.MODE_PRIVATE)
    val token = prefs.getString("TOKEN", null)

    if (token == null) {
        onError("Unauthorized")
        return
    }

    val request = Request.Builder()
        .url("http://172.20.10.3:8000/authority/complaint/$complaintId")
        .addHeader("Authorization", "Bearer $token")
        .get()
        .build()

    OkHttpClient().newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            onError("Network error")
        }

        override fun onResponse(call: Call, response: Response) {
            val body = response.body?.string() ?: return

            if (!response.isSuccessful) {
                onError("Server error")
                return
            }

            val json = JSONObject(body)
            val complaintJson = json.getJSONObject("complaint")
            val mediaJson = json.getJSONObject("media")
            val workflowJson = json.getJSONObject("workflow")

            val detail = ComplaintDetail(
                complaint_id = complaintJson.getString("complaint_id"),
                full_name = complaintJson.getString("full_name"),
                email = complaintJson.getString("email"),
                mobile = complaintJson.getString("mobile"),
                latitude = complaintJson.getString("latitude"),
                longitude = complaintJson.getString("longitude"),
                location_description = complaintJson.getString("location_description"),
                timestamp = complaintJson.getString("timestamp"),
                status = complaintJson.getString("status"),
                assigned_to = complaintJson.optString("assigned_to"),
                assigned_by = complaintJson.optString("assigned_by"),
                assigned_at = complaintJson.optString("assigned_at"),
                last_updated = complaintJson.optString("last_updated")
            )

            val responseObj = ComplaintDetailResponse(
                authority_id = json.getString("authority_id"),
                complaint = detail,
                media = MediaInfo(mediaJson.getString("image_url")),
                workflow = WorkflowInfo(
                    allowed_status = workflowJson.getJSONArray("allowed_status")
                        .let { arr -> List(arr.length()) { arr.getString(it) } },
                    allowed_assignees = workflowJson.getJSONArray("allowed_assignees")
                        .let { arr -> List(arr.length()) { arr.getString(it) } }
                )
            )

            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onSuccess(responseObj)
            }
        }
    })
}

fun updateComplaint(
    context: android.content.Context,
    complaintId: String,
    status: String,
    assignee: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val prefs = context.getSharedPreferences("AUTH", android.content.Context.MODE_PRIVATE)
    val token = prefs.getString("TOKEN", null)

    if (token == null) {
        onError("Unauthorized")
        return
    }

    val body = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("status", status)
        .addFormDataPart("assigned_to", assignee)
        .build()

    val request = Request.Builder()
        .url("http://172.20.10.3:8000/authority/complaint/$complaintId")
        .addHeader("Authorization", "Bearer $token")
        .patch(body)
        .build()

    OkHttpClient().newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            onError("Network error")
        }

        override fun onResponse(call: Call, response: Response) {
            if (!response.isSuccessful) {
                onError("Update failed")
                return
            }

            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onSuccess()
            }
        }
    })
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorityComplaintDetailScreen(
    complaintId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var detail by remember { mutableStateOf<ComplaintDetailResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var assigneeExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }
    var selectedAssignee by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf("") }
    var updating by remember { mutableStateOf(false) }
    var showSaved by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        fetchComplaintDetail(
            context = context,
            complaintId = complaintId,
            onSuccess = {
                detail = it
                selectedAssignee = it.complaint.assigned_to
                selectedStatus = it.complaint.status
                isLoading = false
            },
            onError = { isLoading = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020617))
    ) {

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
            return
        }

        detail?.let { data ->

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // 🔝 HEADER
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "←",
                            color = Color.White,
                            fontSize = 22.sp,
                            modifier = Modifier.clickable { onBack() }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Case ${data.complaint.complaint_id}",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 🖼️ EVIDENCE IMAGE
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        AsyncImage(
                            model = "http://172.20.10.3:8000${data.media.image_url}",
                            contentDescription = "Complaint Evidence",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                // 📍 LOCATION
                item {
                    InfoCard(title = "Location") {
                        InfoRow("Description", data.complaint.location_description)
                        val mapUrl =
                            "https://www.google.com/maps?q=${data.complaint.latitude},${data.complaint.longitude}"

                        InfoRow(
                            label = "Location",
                            value = "Open in Google Maps",
                            onClick = {
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    Uri.parse(mapUrl)
                                )
                                context.startActivity(intent)
                            }
                        )

                    }
                }
                // 👤 REPORTER DETAILS
                item {
                    InfoCard(title = "Reporter Details") {
                        InfoRow("Name", data.complaint.full_name)
                        InfoRow("Mobile", data.complaint.mobile)
                        InfoRow("Email", data.complaint.email)
                    }
                }
                // ⚙️ CASE CONTROL
                item {
                    InfoCard(title = "Case Control") {

                        // STATUS DROPDOWN
                        ExposedDropdownMenuBox(
                            expanded = statusExpanded,
                            onExpandedChange = { statusExpanded = !statusExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedStatus,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Status") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(statusExpanded)
                                },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            ExposedDropdownMenu(
                                expanded = statusExpanded,
                                onDismissRequest = { statusExpanded = false }
                            ) {
                                data.workflow.allowed_status.forEach { status ->
                                    DropdownMenuItem(
                                        text = { Text(status) },
                                        onClick = {
                                            selectedStatus = status
                                            statusExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // ASSIGNEE DROPDOWN
                        ExposedDropdownMenuBox(
                            expanded = assigneeExpanded,
                            onExpandedChange = { assigneeExpanded = !assigneeExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedAssignee.ifBlank { "Unassigned" },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Assigned To") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(assigneeExpanded)
                                },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            ExposedDropdownMenu(
                                expanded = assigneeExpanded,
                                onDismissRequest = { assigneeExpanded = false }
                            ) {
                                data.workflow.allowed_assignees.forEach { name ->
                                    DropdownMenuItem(
                                        text = { Text(name) },
                                        onClick = {
                                            selectedAssignee = name
                                            assigneeExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                updating = true
                                showSaved = false

                                updateComplaint(
                                    context = context,
                                    complaintId = complaintId,
                                    status = selectedStatus,
                                    assignee = selectedAssignee,
                                    onSuccess = {
                                        updating = false
                                        showSaved = true
                                    },
                                    onError = {
                                        updating = false
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !updating
                        ) {
                            Text(if (updating) "Updating…" else "Save Changes")
                        }
                        if (showSaved) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Changes saved successfully",
                                color = Color(0xFF22C55E),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun InfoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = Color(0xFF94A3B8),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) {
                onClick?.invoke()
            },
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFF94A3B8), fontSize = 13.sp)
        Text(
            value,
            color = if (onClick != null) Color(0xFF38BDF8) else Color.White,
            fontSize = 13.sp
        )
    }
    Spacer(modifier = Modifier.height(6.dp))
}
