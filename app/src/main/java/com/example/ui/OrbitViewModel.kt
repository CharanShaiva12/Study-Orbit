package com.example.ui

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

// --- Client representations of Room & Peers ---

data class StudyRoom(
    val id: String,
    val name: String,
    val centralTopic: String,
    val themeColor: Color,
    val orbitScale: Float,
    val bgGradient: List<Color>
)

data class PeerStudent(
    val id: String,
    val name: String,
    val status: String,
    val isStuck: Boolean = false,
    val studyDurationMinutes: Int = 0,
    var orbitalAngle: Float = 0f, // 0 to 360f
    val orbitalSpeed: Float = 1f,   // Speed of rotation
    val orbitRadius: Float = 100f,
    val avatarColor: Color,
    val isUser: Boolean = false
)

data class ArenaMessage(
    val id: String = UUID.randomUUID().toString(),
    val author: String,
    val content: String,
    val isEpiphany: Boolean = false,
    val epiphany: EpiphanyResponse? = null,
    val isSystem: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

class OrbitViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = EpiphanyRepository(db.savedEpiphanyDao())

    // --- Rooms definition ---
    val rooms = listOf(
        StudyRoom(
            id = "cosmic_library",
            name = "Cosmic Slate Library",
            centralTopic = "Quantum Mechanics & Wave Equations",
            themeColor = Color(0xFF64FFDA), // Mint Teal
            orbitScale = 1.0f,
            bgGradient = listOf(Color(0xFF070B13), Color(0xFF0F1524))
        ),
        StudyRoom(
            id = "cyber_cafe",
            name = "Neon Cyber Café",
            centralTopic = "Distributed Consensus & Raft Protocol",
            themeColor = Color(0xFFFF2E93), // Neon Pink
            orbitScale = 1.2f,
            bgGradient = listOf(Color(0xFF090514), Color(0xFF1B0C2B))
        ),
        StudyRoom(
            id = "forest_grove",
            name = "Zen Biome Grove",
            centralTopic = "Cellular Respiration & Krebs Cycle",
            themeColor = Color(0xFF81C784), // Organic Green
            orbitScale = 0.9f,
            bgGradient = listOf(Color(0xFF051208), Color(0xFF0B2411))
        )
    )

    // --- State Observables ---
    private val _selectedRoom = MutableStateFlow(rooms[0])
    val selectedRoom: StateFlow<StudyRoom> = _selectedRoom.asStateFlow()

    private val _peers = MutableStateFlow<List<PeerStudent>>(emptyList())
    val peers: StateFlow<List<PeerStudent>> = _peers.asStateFlow()

    private val _liveMessages = MutableStateFlow<List<ArenaMessage>>(emptyList())
    val liveMessages: StateFlow<List<ArenaMessage>> = _liveMessages.asStateFlow()

    // Pomodoro Timer: Synchronous core heartbeat
    private val _timerSecondsRemaining = MutableStateFlow(1500) // 25:00
    val timerSecondsRemaining: StateFlow<Int> = _timerSecondsRemaining.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(true)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _isBreakMode = MutableStateFlow(false)
    val isBreakMode: StateFlow<Boolean> = _isBreakMode.asStateFlow()

    // User State
    private val _userStatus = MutableStateFlow("Focusing 📚")
    val userStatus: StateFlow<String> = _userStatus.asStateFlow()

    private val _isUserStuck = MutableStateFlow(false)
    val isUserStuck: StateFlow<Boolean> = _isUserStuck.asStateFlow()

    private val _userStudyTimeMinutes = MutableStateFlow(12)
    val userStudyTimeMinutes: StateFlow<Int> = _userStudyTimeMinutes.asStateFlow()

    // Local DB epiphanies
    val savedEpiphanies: StateFlow<List<SavedEpiphany>> = repository.allSavedEpiphanies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Feedback Loader for Gemini
    private val _isGeneratingEpiphany = MutableStateFlow(false)
    val isGeneratingEpiphany: StateFlow<Boolean> = _isGeneratingEpiphany.asStateFlow()

    private val _lastGeneratedEpiphany = MutableStateFlow<EpiphanyResponse?>(null)
    val lastGeneratedEpiphany: StateFlow<EpiphanyResponse?> = _lastGeneratedEpiphany.asStateFlow()

    init {
        // Initialize room peers
        switchRoom(rooms[0])

        // Start Physics Engine - Real-time orbital movement loop!
        viewModelScope.launch {
            while (true) {
                delay(40) // ~25 FPS
                _peers.update { currentPeers ->
                    currentPeers.map { peer ->
                        val newAngle = (peer.orbitalAngle + peer.orbitalSpeed) % 360f
                        peer.copy(orbitalAngle = newAngle)
                    }
                }
            }
        }

        // Start Pomodoro timer countdown
        viewModelScope.launch {
            while (true) {
                delay(1000)
                if (_isTimerRunning.value) {
                    if (_timerSecondsRemaining.value > 0) {
                        _timerSecondsRemaining.value -= 1
                    } else {
                        // Switch mode synchronously!
                        _isBreakMode.value = !_isBreakMode.value
                        if (_isBreakMode.value) {
                            _timerSecondsRemaining.value = 300 // 5m break
                            triggerSystemMessage("Pomodoro completed! Entering a 5-minute virtual Space Walk break. Time to breathe.")
                        } else {
                            _timerSecondsRemaining.value = 1500 // 25m study
                            triggerSystemMessage("Break's over! Orbit core active. Focus engines firing up!")
                        }
                    }
                }
            }
        }

        // Simulating peer chat activities / triggers in real-time
        viewModelScope.launch {
            while (true) {
                delay(Random.nextLong(15000, 25000)) // Every 15-25 seconds, someone asks/talks
                generateSimulatedPeerEvent()
            }
        }
    }

    fun switchRoom(room: StudyRoom) {
        _selectedRoom.value = room
        // Reset and populate room peers
        val names = when (room.id) {
            "cosmic_library" -> listOf("Aria (Physics)", "Devon (Statics)", "Kai (Astrophysics)", "Zoe (Relativity)")
            "cyber_cafe" -> listOf("Liam (Rust)", "Sasha (Go-Lang)", "Nico (Networks)", "Mia (Distributed)")
            else -> listOf("Chloe (Organic)", "Elijah (Genetics)", "Sophia (BioTech)", "Lucas (Botany)")
        }

        // Avatar styling colors
        val colors = listOf(
            Color(0xFFFFB74D), Color(0xFF4FC3F7), Color(0xFFF06292),
            Color(0xFFBA68C8), Color(0xFF4DB6AC), Color(0xFFAED581)
        )

        val list = mutableListOf<PeerStudent>()
        // Always place the User on orbit
        list.add(
            PeerStudent(
                id = "user_node",
                name = "Me (Charan)",
                status = _userStatus.value,
                isStuck = _isUserStuck.value,
                studyDurationMinutes = _userStudyTimeMinutes.value,
                orbitalAngle = 0f,
                orbitalSpeed = 0.5f,
                orbitRadius = 75f,
                avatarColor = Color(0xFFFFF176),
                isUser = true
            )
        )

        names.forEachIndexed { idx, name ->
            val angle = (idx + 1) * 72f
            val isStuckInit = idx == 1 && Random.nextBoolean() // Let one peer be stuck initially
            list.add(
                PeerStudent(
                    id = "peer_$idx",
                    name = name,
                    status = if (isStuckInit) "Stuck 🆘" else "Deep Work 🌲",
                    isStuck = isStuckInit,
                    studyDurationMinutes = Random.nextInt(15, 120),
                    orbitalAngle = angle,
                    orbitalSpeed = 0.3f + Random.nextFloat() * 0.5f,
                    orbitRadius = 120f + idx * 30f,
                    avatarColor = colors[idx % colors.size]
                )
            )
        }

        _peers.value = list

        // Initialize welcome message for the room
        val welcomeMsg = ArenaMessage(
            author = "System Orbit",
            content = "You aligned with ${_selectedRoom.value.name}. Current orbit target: '${_selectedRoom.value.centralTopic}'. Connecting to 4 classmates...",
            isSystem = true
        )
        _liveMessages.value = listOf(welcomeMsg)
    }

    fun setUserStatus(status: String) {
        _userStatus.value = status
        val stuck = status.contains("🆘") || status.contains("Stuck")
        _isUserStuck.value = stuck

        _peers.update { current ->
            current.map { peer ->
                if (peer.isUser) {
                    peer.copy(status = status, isStuck = stuck)
                } else peer
            }
        }

        if (stuck) {
            triggerSystemMessage("Me (Charan) triggered a Distress Beacon! Ring glowing red. Asking Orbit peers for a concept summary.")
        } else {
            triggerSystemMessage("Me (Charan) changed status: '$status'")
        }
    }

    fun toggleUserStuck() {
        if (_isUserStuck.value) {
            setUserStatus("Focusing 📚")
        } else {
            setUserStatus("Stuck 🆘")
        }
    }

    fun sendSystemBroadcast(text: String) {
        val msg = ArenaMessage(
            author = "Charan",
            content = text
        )
        _liveMessages.update { listOf(msg) + it }
    }

    fun triggerSystemMessage(text: String) {
        val msg = ArenaMessage(
            author = "System Orbit",
            content = text,
            isSystem = true
        )
        _liveMessages.update { listOf(msg) + it }
    }

    // --- AI Seek Epiphany Action via Gemini ---
    fun requestStudyEpiphany(topic: String) {
        viewModelScope.launch {
            _isGeneratingEpiphany.value = true
            triggerSystemMessage("Tuning StudyOrbit antenna to query Gemini-3.5-Flash for '$topic'...")

            try {
                val epiphany = GeminiApiClient.getStudyEpiphany(topic)
                if (epiphany != null) {
                    _lastGeneratedEpiphany.value = epiphany
                    // Add Epiphany Card to the Live Arena Feed!
                    val epiphanyMsg = ArenaMessage(
                        author = "Gemini AI Oracle",
                        content = "Broadcasted a Study Epiphany for: '$topic'",
                        isEpiphany = true,
                        epiphany = epiphany
                    )
                    _liveMessages.update { listOf(epiphanyMsg) + it }
                    triggerSystemMessage("Epiphany locked! Broadcasted to feed. Anyone in this room can tap download to save it.")
                } else {
                    triggerSystemMessage("Antenna failed to sync. Gemini is unreachable. Saving offline-mode summary card!")
                }
            } catch (e: Exception) {
                triggerSystemMessage("Error requesting Epiphany: ${e.localizedMessage}")
            } finally {
                _isGeneratingEpiphany.value = false
            }
        }
    }

    // --- Save Epiphany to offline database (Room) ---
    fun saveEpiphanyToDatabase(topic: String, prompt: String, epiphany: EpiphanyResponse) {
        viewModelScope.launch {
            val entity = SavedEpiphany(
                topic = topic,
                prompt = prompt,
                coreSpark = epiphany.coreSpark,
                analogy = epiphany.analogy,
                cheatcardItems = epiphany.cheatcardItems.joinToString(";")
            )
            repository.insert(entity)
            triggerSystemMessage("Downloaded & Saved detailed epiphany card for '$topic' to local storage.")
        }
    }

    fun deleteSavedEpiphany(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
            triggerSystemMessage("Deleted stored epiphany card from local database.")
        }
    }

    // --- Real-time user interaction: Peer support / spark injection ---
    fun supportPeer(peerId: String) {
        _peers.update { current ->
            current.map { peer ->
                if (peer.id == peerId) {
                    // Send thank message in chat from peer
                    viewModelScope.launch {
                        delay(1200)
                        val recoveryMsg = ArenaMessage(
                            author = peer.name,
                            content = "❤️ Charan sent me a Focus Spark! Wow, feel unstuck now. Back to deep study!",
                        )
                        _liveMessages.update { listOf(recoveryMsg) + it }
                    }
                    peer.copy(isStuck = false, status = "Deep Work 🌲")
                } else peer
            }
        }
        triggerSystemMessage("You launched a real-time 'Focus Spark' to guide classmate!")
    }

    // --- Simulated Peer Dynamic Actions ---
    private fun generateSimulatedPeerEvent() {
        val activePeers = _peers.value.filter { !it.isUser }
        if (activePeers.isEmpty()) return

        val peer = activePeers[Random.nextInt(activePeers.size)]
        val dice = Random.nextInt(4)

        viewModelScope.launch {
            when (dice) {
                0 -> {
                    // Peer gets stuck!
                    _peers.update { current ->
                        current.map { p ->
                            if (p.id == peer.id) p.copy(isStuck = true, status = "Stuck 🆘") else p
                        }
                    }
                    val msg = ArenaMessage(
                        author = peer.name,
                        content = "🆘 I'm stuck on: '${_selectedRoom.value.centralTopic}'. If anyone has an epiphany or math tip, please trigger the Oracle!",
                    )
                    _liveMessages.update { listOf(msg) + it }
                }
                1 -> {
                    // Peer posts a resource or motivational quote
                    val phrases = listOf(
                        "Loving this background soundscape! So peaceful.",
                        "Consensus protocols are confusing, but we got this!",
                        "Finished my 2nd study orbit loop! Feel like a productivity planet.",
                        "Remember to drink water, co-studiers! Hydro orbital physics!"
                    )
                    val msg = ArenaMessage(
                        author = peer.name,
                        content = phrases[Random.nextInt(phrases.size)],
                    )
                    _liveMessages.update { listOf(msg) + it }
                }
                2 -> {
                    // Peer starts focusing intensely
                    _peers.update { current ->
                        current.map { p ->
                            if (p.id == peer.id) p.copy(isStuck = false, status = "Caffeinated ☕") else p
                        }
                    }
                    val msg = ArenaMessage(
                        author = peer.name,
                        content = "Just grabbed double espresso! Core power output at 200%. Let's crush this session!"
                    )
                    _liveMessages.update { listOf(msg) + it }
                }
                3 -> {
                    // Peer asks quick AI help
                    val promptOptions = when (_selectedRoom.value.id) {
                        "cosmic_library" -> "Schrodinger wave equations"
                        "cyber_cafe" -> "Distributed Consensus"
                        else -> "Citric Acid Cycle"
                    }
                    val msg = ArenaMessage(
                        author = peer.name,
                        content = "Can anyone generate a cheatcard summarizing '$promptOptions'? Tap Oracle button!"
                    )
                    _liveMessages.update { listOf(msg) + it }
                }
            }
        }
    }
}
