package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.EpiphanyResponse
import com.example.data.SavedEpiphany
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyOrbitScreen(
    viewModel: OrbitViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val selectedRoom by viewModel.selectedRoom.collectAsState()
    val peers by viewModel.peers.collectAsState()
    val liveMessages by viewModel.liveMessages.collectAsState()
    val timerRemaining by viewModel.timerSecondsRemaining.collectAsState()
    val isBreakMode by viewModel.isBreakMode.collectAsState()
    val userStatus by viewModel.userStatus.collectAsState()
    val isUserStuck by viewModel.isUserStuck.collectAsState()
    val isGeneratingEpiphany by viewModel.isGeneratingEpiphany.collectAsState()
    val savedEpiphanies by viewModel.savedEpiphanies.collectAsState()

    var activeTab by remember { mutableStateOf("orbit") } // "orbit", "feed", "archive"
    var selectedPeerIdForAction by remember { mutableStateOf<String?>(null) }
    var promptInputText by remember { mutableStateOf("") }
    
    // Smooth background color shifting based on room and Pomodoro state
    val targetBgStart = if (isBreakMode) Color(0xFF020E1A) else selectedRoom.bgGradient[0]
    val targetBgEnd = if (isBreakMode) Color(0xFF0F1E33) else selectedRoom.bgGradient[1]
    
    val bgStartColor by animateColorAsState(targetBgStart, animationSpec = tween(1000))
    val bgEndColor by animateColorAsState(targetBgEnd, animationSpec = tween(1000))

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        containerColor = Color(0xFF0F172A), // Full slate foundation
        bottomBar = {
            // High-fidelity glassy frosted navigation bar
            NavigationBar(
                containerColor = Color(0xBE0F172A),
                tonalElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, Color(0x1BFFFFFF)), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                NavigationBarItem(
                    selected = activeTab == "orbit",
                    onClick = { activeTab = "orbit" },
                    icon = { Icon(Icons.Default.Refresh, contentDescription = "Orbit Map") },
                    label = { Text("Study Orbit", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = selectedRoom.themeColor,
                        selectedTextColor = selectedRoom.themeColor,
                        indicatorColor = selectedRoom.themeColor.copy(alpha = 0.18f),
                        unselectedIconColor = Color.LightGray.copy(alpha = 0.6f),
                        unselectedTextColor = Color.LightGray.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("nav_orbit")
                )
                NavigationBarItem(
                    selected = activeTab == "feed",
                    onClick = { activeTab = "feed" },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Arena") },
                    label = { Text("Live Arena", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = selectedRoom.themeColor,
                        selectedTextColor = selectedRoom.themeColor,
                        indicatorColor = selectedRoom.themeColor.copy(alpha = 0.18f),
                        unselectedIconColor = Color.LightGray.copy(alpha = 0.6f),
                        unselectedTextColor = Color.LightGray.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("nav_feed")
                )
                NavigationBarItem(
                    selected = activeTab == "archive",
                    onClick = { activeTab = "archive" },
                    icon = { Icon(Icons.Default.Star, contentDescription = "Archive") },
                    label = { Text("Epiphanies", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = selectedRoom.themeColor,
                        selectedTextColor = selectedRoom.themeColor,
                        indicatorColor = selectedRoom.themeColor.copy(alpha = 0.18f),
                        unselectedIconColor = Color.LightGray.copy(alpha = 0.6f),
                        unselectedTextColor = Color.LightGray.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("nav_archive")
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .padding(paddingValues)
        ) {
            // Elegant Frosted Glass Blurry Sphere Layers in the Background
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Top-Left Glowing Indigo Orbit Sphere
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x3D4F46E5), Color.Transparent),
                        center = Offset(0f, 0f),
                        radius = size.width * 0.85f
                    ),
                    center = Offset(0f, 0f),
                    radius = size.width * 0.85f
                )
                // Bottom-Right Glowing Rose Orbit Sphere
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x2CE11D48), Color.Transparent),
                        center = Offset(size.width * 0.9f, size.height * 0.9f),
                        radius = size.width * 0.85f
                    ),
                    center = Offset(size.width * 0.9f, size.height * 0.9f),
                    radius = size.width * 0.85f
                )
            }

            // Header Room Capsules
            Column(modifier = Modifier.fillMaxSize()) {
                // Room Capsules Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    viewModel.rooms.forEach { room ->
                        val isSelected = selectedRoom.id == room.id
                        val borderAlpha = if (isSelected) 0.6f else 0.12f
                        val capsuleColor = if (isSelected) room.themeColor.copy(alpha = 0.22f) else Color(0x0CFFFFFF)

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(capsuleColor)
                                .border(1.dp, Color.White.copy(alpha = borderAlpha), RoundedCornerShape(24.dp))
                                .clickable {
                                    viewModel.switchRoom(room)
                                    selectedPeerIdForAction = null
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(room.themeColor)
                                )
                                Text(
                                    text = room.name,
                                    color = if (isSelected) Color.White else Color.LightGray.copy(alpha = 0.6f),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // Active Workspace based on current active tab
                Crossfade(
                    targetState = activeTab,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    animationSpec = tween(400)
                ) { tab ->
                    when (tab) {
                        "orbit" -> OrbitWorkspace(
                            selectedRoom = selectedRoom,
                            peers = peers,
                            timerRemaining = timerRemaining,
                            isBreakMode = isBreakMode,
                            userStatus = userStatus,
                            isUserStuck = isUserStuck,
                            onStatusChange = { viewModel.setUserStatus(it) },
                            onToggleStuck = { viewModel.toggleUserStuck() },
                            selectedPeerId = selectedPeerIdForAction,
                            onPeerClick = { selectedPeerIdForAction = it },
                            onSupportPeer = {
                                viewModel.supportPeer(it)
                                selectedPeerIdForAction = null
                            }
                        )
                        "feed" -> LiveArenaWorkspace(
                            selectedRoom = selectedRoom,
                            messages = liveMessages,
                            isGenerating = isGeneratingEpiphany,
                            promptText = promptInputText,
                            onPromptChange = { promptInputText = it },
                            onQueryOracle = {
                                if (promptInputText.trim().isNotEmpty()) {
                                    viewModel.requestStudyEpiphany(promptInputText)
                                    promptInputText = ""
                                }
                            },
                            onDownloadEpiphany = { topic, content ->
                                viewModel.saveEpiphanyToDatabase(topic, topic, content)
                            }
                        )
                        "archive" -> ArchiveWorkspace(
                            savedEpiphanies = savedEpiphanies,
                            themeColor = selectedRoom.themeColor,
                            onDelete = { viewModel.deleteSavedEpiphany(it) }
                        )
                    }
                }
            }

            // Global Loading overlay when Gemini is generating
            AnimatedVisibility(
                visible = isGeneratingEpiphany,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xE005080E)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        CircularProgressIndicator(
                            color = selectedRoom.themeColor,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(64.dp)
                        )
                        
                        var tickerText by remember { mutableStateOf("Aligning tracking antenna...") }
                        LaunchedEffect(Unit) {
                            val statuses = listOf(
                                "Aligning orbital receptors...",
                                "Synching with Gemini-3.5-Flash Core...",
                                "Analyzing focus topic queries...",
                                "Synthesizing mental model analogies...",
                                "Structuring diagnostic reference cards..."
                            )
                            var idx = 0
                            while (true) {
                                delay(2200)
                                idx = (idx + 1) % statuses.size
                                tickerText = statuses[idx]
                            }
                        }

                        Text(
                            text = "Consulting StudyOrbit Oracle",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = tickerText,
                            color = selectedRoom.themeColor,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Generating dynamic visual epiphanies for your entire co-studying orbit group.",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 1. COMPONENT: INTERACTIVE ORBIT WORKSPACE
// ==========================================
@Composable
fun OrbitWorkspace(
    selectedRoom: StudyRoom,
    peers: List<PeerStudent>,
    timerRemaining: Int,
    isBreakMode: Boolean,
    userStatus: String,
    isUserStuck: Boolean,
    onStatusChange: (String) -> Unit,
    onToggleStuck: () -> Unit,
    selectedPeerId: String?,
    onPeerClick: (String?) -> Unit,
    onSupportPeer: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pomodoro Clock Core Widget (Pulsing Orbit Core)
        Box(
            modifier = Modifier
                .padding(top = 16.dp)
                .size(110.dp)
                .clip(CircleShape)
                .background(Color(0x12FFFFFF))
                .border(BorderStroke(1.dp, Color(0x35FFFFFF)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val minutes = timerRemaining / 60
            val seconds = timerRemaining % 60
            val timeString = String.format("%02d:%02d", minutes, seconds)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isBreakMode) "SPACE WALK" else "FOCUS ENGINE",
                    color = if (isBreakMode) Color(0xFF4FC3F7) else selectedRoom.themeColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = timeString,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Orbit Core Sync",
                    color = Color.LightGray,
                    fontSize = 9.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Center Indicator Information Text
        Text(
            text = selectedRoom.centralTopic,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp),
            textAlign = TextAlign.Center
        )
        Text(
            text = "Active study solar map. Orbiting dots are real-time classmate paths.",
            color = Color.Gray,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp),
            textAlign = TextAlign.Center
        )

        // The Orbit Map (Interactive Canvas)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val pulsingAnim = rememberInfiniteTransition()
            val scalePulse by pulsingAnim.animateFloat(
                initialValue = 0.85f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2200, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )

            // Setup real-time responsive drawing path
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(peers) {
                        detectTapGestures { offset ->
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f
                            
                            // Let's identify if player tapped near any student orbit coordinates
                            var tappedPeer: PeerStudent? = null
                            for (peer in peers) {
                                val radius = peer.orbitRadius * selectedRoom.orbitScale * 0.72f
                                val rad = Math.toRadians(peer.orbitalAngle.toDouble())
                                val pX = centerX + radius * cos(rad).toFloat()
                                val pY = centerY + radius * sin(rad).toFloat()

                                val dist = Math.hypot((offset.x - pX).toDouble(), (offset.y - pY).toDouble())
                                if (dist < 28f * density) { // 28dp sensitivity area
                                    tappedPeer = peer
                                    break
                                }
                            }
                            if (tappedPeer != null) {
                                onPeerClick(tappedPeer.id)
                            } else {
                                onPeerClick(null)
                            }
                        }
                    }
            ) {
                val cx = size.width / 2f
                val cy = size.height / 2f

                // Draw central sun
                drawCircle(
                    color = selectedRoom.themeColor.copy(alpha = 0.1f * scalePulse),
                    radius = 48.dp.toPx(),
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = selectedRoom.themeColor,
                    radius = 16.dp.toPx(),
                    center = Offset(cx, cy)
                )
                // Core ring
                drawCircle(
                    color = Color.White,
                    radius = 16.dp.toPx(),
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Draw primary orbital tracks
                val distances = listOf(75f, 120f, 150f, 180f)
                distances.forEach { dist ->
                    drawCircle(
                        color = selectedRoom.themeColor.copy(alpha = 0.12f),
                        radius = dist * selectedRoom.orbitScale * 0.72f * density,
                        center = Offset(cx, cy),
                        style = Stroke(
                            width = 1.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                }

                // Draw individual students planetary nodes on Canvas
                peers.forEach { peer ->
                    val radius = peer.orbitRadius * selectedRoom.orbitScale * 0.72f * density
                    val rad = Math.toRadians(peer.orbitalAngle.toDouble())
                    val px = cx + radius * cos(rad).toFloat()
                    val pYCoord = cy + radius * sin(rad).toFloat()

                    // Draw beacon rays if peer is stuck Custom SOS visual
                    if (peer.isStuck) {
                        drawCircle(
                            color = Color(0xFFFF5252).copy(alpha = 0.25f * scalePulse),
                            radius = 24.dp.toPx(),
                            center = Offset(px, pYCoord)
                        )
                        drawCircle(
                            color = Color(0xFFFF5252).copy(alpha = 0.45f),
                            radius = 16.dp.toPx(),
                            center = Offset(px, pYCoord),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    } else if (peer.isUser) {
                        drawCircle(
                            color = Color(0xFFFFF176).copy(alpha = 0.25f * scalePulse),
                            radius = 20.dp.toPx(),
                            center = Offset(px, pYCoord)
                        )
                    }

                    // Base circle node
                    drawCircle(
                        color = peer.avatarColor,
                        radius = 11.dp.toPx(),
                        center = Offset(px, pYCoord)
                    )

                    // Overlay border
                    drawCircle(
                        color = if (peer.isUser) Color.White else Color(0x99000000),
                        radius = 11.dp.toPx(),
                        center = Offset(px, pYCoord),
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    // Draw Initial letter in the node center
                    drawIntoCanvas { canvas ->
                        val paint = android.graphics.Paint().apply {
                            color = if (peer.isStuck) 0xFFFFEB3B.toInt() else android.graphics.Color.WHITE
                            textSize = 8.dp.toPx()
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        val nameChar = peer.name.firstOrNull()?.toString() ?: "S"
                        canvas.nativeCanvas.drawText(
                            nameChar,
                            px,
                            pYCoord + 3.dp.toPx(),
                            paint
                        )
                    }
                }
            }
        }

        // Tapped Peer Context Action Drawer Card
        AnimatedVisibility(
            visible = selectedPeerId != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            val peer = peers.find { it.id == selectedPeerId }
            if (peer != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x1F94A3B8)),
                    border = BorderStroke(1.dp, Color(0x35FFFFFF)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .testTag("peer_action_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Icon representation
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(peer.avatarColor.copy(alpha = 0.2f))
                                .border(2.dp, peer.avatarColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = peer.name.first().toString(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = peer.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Status: ${peer.status}",
                                color = if (peer.isStuck) Color(0xFFFFA726) else Color.LightGray,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Focused: ${peer.studyDurationMinutes}m in Orbit loop",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }

                        // Spark action trigger button
                        if (peer.isStuck && !peer.isUser) {
                            Button(
                                onClick = { onSupportPeer(peer.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Favorite,
                                        contentDescription = "Spark",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text("Spark", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            IconButton(onClick = { onPeerClick(null) }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Personal Student Workspace Panel (Status tuning)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0x12FFFFFF)),
            border = BorderStroke(1.dp, Color(0x22FFFFFF)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "My Personal Orbit Control",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Status: $userStatus",
                            color = if (isUserStuck) Color(0xFFFF5252) else selectedRoom.themeColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Stuck toggles distress ring neon color to peers.",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = onToggleStuck,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isUserStuck) Color(0xFF4CAF50) else Color(0xFFFF5252)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("stuck_sos_button")
                    ) {
                        Text(
                            text = if (isUserStuck) "Reset to Normal" else "🆘 Stuck!",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Quick Status Capsules
                Text(
                    text = "Broadcast mood change to Orbit room:",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val statuses = listOf("Focusing 📚", "Caffeinated ☕", "Brain Melted 🌪️", "Offline Rest 💫")
                    statuses.forEach { status ->
                        val isCurrent = userStatus == status
                        val capsuleBg = if (isCurrent) selectedRoom.themeColor.copy(alpha = 0.25f) else Color(0x0CFFFFFF)
                        val capsuleBorder = if (isCurrent) selectedRoom.themeColor else Color(0x1AFFFFFF)

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(capsuleBg)
                                .border(1.dp, capsuleBorder, RoundedCornerShape(12.dp))
                                .clickable { onStatusChange(status) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(status, fontSize = 10.sp, color = if (isCurrent) Color.White else Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. COMPONENT: LIVE ARENA FEED & ORACLE
// ==========================================
@Composable
fun LiveArenaWorkspace(
    selectedRoom: StudyRoom,
    messages: List<ArenaMessage>,
    isGenerating: Boolean,
    promptText: String,
    onPromptChange: (String) -> Unit,
    onQueryOracle: () -> Unit,
    onDownloadEpiphany: (String, EpiphanyResponse) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0x14FFFFFF)),
            border = BorderStroke(1.dp, Color(0x22FFFFFF)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Star, contentDescription = "Spark", tint = selectedRoom.themeColor)
                Column {
                    Text(
                        text = "Live Epiphany Feed",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ask complex student queries, and summon Gemini to generate analogic epiphany summaries.",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Message Feed Area
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            reverseLayout = false,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { msg ->
                if (msg.isSystem) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = msg.content,
                            color = selectedRoom.themeColor.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .background(Color(0x1BFFFFFF), RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                } else if (msg.isEpiphany && msg.epiphany != null) {
                    EpiphanyBroadcastCard(
                        msg = msg,
                        selectedRoom = selectedRoom,
                        onDownload = { onDownloadEpiphany(msg.content, msg.epiphany) }
                    )
                } else {
                    ChatBubbleMessage(msg = msg)
                }
            }
        }

        // Footer Input Query bar for Gemini Oracle
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xCC0F172A)),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            border = BorderStroke(1.dp, Color(0x2BFFFFFF)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = promptText,
                    onValueChange = onPromptChange,
                    placeholder = { Text("Ask Oracle: e.g. Raft states...", color = Color.Gray) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0x11FFFFFF),
                        unfocusedContainerColor = Color(0x0AFFFFFF),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("query_input_text")
                )

                Button(
                    onClick = onQueryOracle,
                    colors = ButtonDefaults.buttonColors(containerColor = selectedRoom.themeColor),
                    shape = RoundedCornerShape(12.dp),
                    enabled = promptText.trim().isNotEmpty() && !isGenerating,
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("summon_oracle_button")
                ) {
                    Text("Oracle", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ChatBubbleMessage(msg: ArenaMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.author == "Charan") Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    color = if (msg.author == "Charan") Color(0x3B64FFDA) else Color(0x1AFFFFFF),
                    shape = RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (msg.author == "Charan") 12.dp else 2.dp,
                        bottomEnd = if (msg.author == "Charan") 2.dp else 12.dp
                    )
                )
                .border(
                    BorderStroke(0.5.dp, if (msg.author == "Charan") Color(0x4D64FFDA) else Color(0x26FFFFFF)),
                    shape = RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (msg.author == "Charan") 12.dp else 2.dp,
                        bottomEnd = if (msg.author == "Charan") 2.dp else 12.dp
                    )
                )
                .padding(12.dp)
        ) {
            Text(
                text = msg.author,
                color = if (msg.author == "Charan") Color(0xFF64FFDA) else Color(0xFFFFB74D),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = msg.content,
                color = Color.White,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun EpiphanyBroadcastCard(
    msg: ArenaMessage,
    selectedRoom: StudyRoom,
    onDownload: () -> Unit
) {
    val ep = msg.epiphany ?: return
    var wasSaved by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x12FFFFFF)),
        border = BorderStroke(1.dp, selectedRoom.themeColor.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("epiphany_broadcast_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = "AI", tint = selectedRoom.themeColor, modifier = Modifier.size(16.dp))
                    Text(
                        text = "STUDY ORBIT EPIPHANY",
                        color = selectedRoom.themeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = "Live Shared",
                    color = Color.Gray,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // The Spark definer block
            Text(
                text = ep.coreSpark,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Analogy block with speech bubble feel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x11FFFFFF), RoundedCornerShape(12.dp))
                    .border(0.5.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        text = "💡 COGNITIVE ANALOGY",
                        color = Color(0xFFFFD54F),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = ep.analogy,
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Cheatcard point elements
            Text(
                text = "⚡ QUICK CHEATCARD CHECKLIST",
                color = selectedRoom.themeColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            ep.cheatcardItems.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("•", color = selectedRoom.themeColor, fontWeight = FontWeight.Bold)
                    Text(text = item, color = Color.White, fontSize = 11.sp, lineHeight = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Requested by: ${msg.author}",
                    color = Color.Gray,
                    fontSize = 10.sp
                )

                if (!wasSaved) {
                    Button(
                        onClick = {
                            onDownload()
                            wasSaved = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = selectedRoom.themeColor),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Download", modifier = Modifier.size(12.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add to Orbit Archive", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Saved", tint = Color.Green, modifier = Modifier.size(16.dp))
                        Text("Saved Offline", color = Color.Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. COMPONENT: ARCHIVE WORKSPACE (ROOM PERSISTENCE)
// ==========================================
@Composable
fun ArchiveWorkspace(
    savedEpiphanies: List<SavedEpiphany>,
    themeColor: Color,
    onDelete: (Int) -> Unit
) {
    if (savedEpiphanies.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = "Empty",
                modifier = Modifier.size(64.dp),
                tint = Color.DarkGray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Orbital Archive is Empty",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Summon the AI Oracle on any study topic and download your epiphany cards to view them offline in this library locker.",
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Orbit Saved Archives (${savedEpiphanies.size})",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Room Stored",
                    color = themeColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(savedEpiphanies) { item ->
                    SavedEpiphanyDetailCard(item = item, themeColor = themeColor, onDelete = { onDelete(item.id) })
                }
            }
        }
    }
}

@Composable
fun SavedEpiphanyDetailCard(
    item: SavedEpiphany,
    themeColor: Color,
    onDelete: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x12FFFFFF)),
        border = BorderStroke(1.dp, Color(0x22FFFFFF)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .testTag("saved_epiphany_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.topic.uppercase(),
                        color = themeColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.coreSpark,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 18.sp
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFFF5252).copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Divider(color = Color(0x1FFFFFFF), modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = "💡 ANALOGICAL EXPLANATION",
                        color = Color(0xFFFFD54F),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.analogy,
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "⚡ ACTION CHEATCARD SUMMARY",
                        color = themeColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    val itemsList = item.cheatcardItems.split(";")
                    itemsList.forEach { point ->
                        if (point.trim().isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("•", color = themeColor)
                                Text(text = point, color = Color.White, fontSize = 11.sp, lineHeight = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
