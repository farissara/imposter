package com.example.imposter
import kotlinx.coroutines.delay

import androidx.compose.ui.text.style.TextAlign

import androidx.activity.compose.setContent
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*

import androidx.compose.ui.unit.dp

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.imposter.ui.theme.ImposterTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.updateTransition
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.firebase.database.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.FirebaseApp


val auth = FirebaseAuth.getInstance()

fun signInAnonymously() {
    auth.signInAnonymously().addOnCompleteListener { task ->
        if (task.isSuccessful) {
            val user = auth.currentUser
            println("Signed in as: ${user?.uid}")
        } else {
            println("Authentication failed: ${task.exception}")
        }
    }
}

class MainActivity : ComponentActivity() {
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance().reference
        FirebaseApp.initializeApp(this) // Initialize Firebase
        setContent { /* Your Compose UI here */ }
        signInAnonymously() // Call the authentication function
        setContent {
            ImposterTheme {
                AppContent(database)
            }
        }
    }
}


val database = FirebaseDatabase.getInstance().reference

fun createGame(playerName: String): String {
    val gameId = database.child("games").push().key!! // Generate a unique game ID

    // Initial structure for the game
    val initialGameData = mapOf(
        "state" to "Waiting", // Initial game state
        "category" to "", // Category will be set later
        "word" to "", // Word will be set later
        "votes" to emptyMap<String, String>() // Initialize empty votes map
    )

    database.child("games").child(gameId).setValue(initialGameData) // Create game in Firebase

    // Add the player who created the game
    addPlayerToGame(gameId, playerName)

    return gameId // Return the gameId for other players to join
}

fun addPlayerToGame(gameId: String, playerName: String) {
    val playerId = database.child("games").child(gameId).child("players").push().key!! // Generate a unique player ID
    val playerData = mapOf(
        "name" to playerName,
        "role" to "Waiting" // Role will be assigned later
    )
    database.child("games").child(gameId).child("players").child(playerId).setValue(playerData) // Add player data
}

fun joinGame(gameId: String, playerName: String) {
    val playerId = database.child("games").child(gameId).child("players").push().key!!
    val playerData = mapOf(
        "name" to playerName,
        "role" to "Waiting"
    )
    database.child("games").child(gameId).child("players").child(playerId).setValue(playerData)
}
@Composable
fun JoinGameScreen(
    database: DatabaseReference,
    onJoinSuccess: (String) -> Unit, // Callback with the gameId
    onBack: () -> Unit
) {
    var gameIdInput by remember { mutableStateOf("") }
    var playerNameInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Join a Game", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        // Input for Game ID
        TextField(
            value = gameIdInput,
            onValueChange = { gameIdInput = it },
            label = { Text("Enter Game ID") },
            modifier = Modifier.fillMaxWidth()
        )

        // Input for Player Name
        TextField(
            value = playerNameInput,
            onValueChange = { playerNameInput = it },
            label = { Text("Enter Your Name") },
            modifier = Modifier.fillMaxWidth()
        )

        // Error Message
        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = Color.Red)
        }

        // Join Game Button
        Button(
            onClick = {
                if (gameIdInput.isNotEmpty() && playerNameInput.isNotEmpty()) {
                    val playerId = database.child("games").child(gameIdInput).child("players").push().key
                    if (playerId != null) {
                        val playerData = mapOf("name" to playerNameInput, "role" to "Waiting")
                        database.child("games").child(gameIdInput).child("players").child(playerId)
                            .setValue(playerData)
                            .addOnSuccessListener {
                                onJoinSuccess(gameIdInput) // Navigate to Lobby after joining
                            }
                            .addOnFailureListener { error ->
                                errorMessage = "Failed to join game: ${error.message}"
                            }
                    } else {
                        errorMessage = "Failed to generate player ID."
                    }
                } else {
                    errorMessage = "Both Game ID and Name are required!"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Join Game")
        }

        // Back Button
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}

@Composable
fun MainMenuScreen(
    onHostGame: () -> Unit,
    onJoinGame: () -> Unit,
    onPlayLocally: () -> Unit // Callback for Play Locally
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Main Menu", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        // Button to host a game
        Button(onClick = onHostGame, modifier = Modifier.fillMaxWidth()) {
            Text("Host a Game")
        }

        // Button to join a game
        Button(onClick = onJoinGame, modifier = Modifier.fillMaxWidth()) {
            Text("Join a Game")
        }

        // Button to play locally
        Button(onClick = onPlayLocally, modifier = Modifier.fillMaxWidth()) {
            Text("Play Locally")
        }
    }
}
@Composable
fun PlayLocallyScreen(onBack: () -> Unit) {
    var playerNames by remember { mutableStateOf(listOf("Player1", "Player2")) } // Example local players
    var roles by remember { mutableStateOf(listOf("Detective - Word: Forest", "Imposter")) } // Example roles
    var currentPlayerIndex by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Local Game", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        // Show current player's role
        Text("Current Player: ${playerNames[currentPlayerIndex]}", fontSize = 20.sp)
        Text("Role: ${roles[currentPlayerIndex]}", fontSize = 20.sp)

        Spacer(modifier = Modifier.height(16.dp))

        // Button to move to the next player
        Button(onClick = {
            if (currentPlayerIndex < playerNames.size - 1) {
                currentPlayerIndex++
            } else {
                currentPlayerIndex = 0 // Loop back to the first player
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Next Player")
        }

        // Back Button
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back to Main Menu")
        }
    }
}




fun startGame(gameId: String, selectedCategory: String, word: String, imposterCount: Int) {
    database.child("games").child(gameId).child("players").get().addOnSuccessListener { snapshot ->
        val players = snapshot.children.map { it.key to it.child("name").value.toString() }
        val categoryWithIcon = predefinedCategories.first { it.name == selectedCategory }
        val roles = assignRoles(players.size, categoryWithIcon, imposterCount)


        players.forEachIndexed { index, player ->
            database.child("games").child(gameId).child("players").child(player.first!!)
                .child("role").setValue(roles[index])
        }

        database.child("games").child(gameId).child("category").setValue(selectedCategory)
        database.child("games").child(gameId).child("word").setValue(word)
        database.child("games").child(gameId).child("state").setValue("Questioning")
    }
}

fun observeGameState(gameId: String, onGameStateChanged: (String) -> Unit) {
    database.child("games").child(gameId).child("state").addValueEventListener(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val gameState = snapshot.getValue(String::class.java) ?: "Waiting"
            onGameStateChanged(gameState)
        }

        override fun onCancelled(error: DatabaseError) {
            println("Error: ${error.message}")
        }
    })
}

@Composable
fun AppContent(database: DatabaseReference) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.MainMenu)  }
    var gameId by remember { mutableStateOf("") } // Example game ID
    var playerName by remember { mutableStateOf("Player1") }
    var isHost by remember { mutableStateOf(true) }
    var playerNames by remember { mutableStateOf(listOf<String>()) }
    var roles by remember { mutableStateOf(listOf<String>()) }

//    LaunchedEffect(Unit) {
//        delay(3000L)
//        currentScreen = Screen.GameSetup
//    }

    when (currentScreen) {
        is Screen.MainMenu -> MainMenuScreen(
            onHostGame = {
                val newGameId = database.child("games").push().key!!
                database.child("games").child(newGameId).setValue(mapOf("state" to "Waiting"))
                gameId = newGameId
                currentScreen = Screen.Lobby
            },
            onJoinGame = {
                currentScreen = Screen.JoinGame
            },
            onPlayLocally = {
                currentScreen = Screen.PlayLocally // Navigate to local gameplay
            }
        )
        is Screen.JoinGame -> JoinGameScreen(
            database = database,
            onJoinSuccess = { joinedGameId ->
                gameId = joinedGameId
                currentScreen = Screen.Lobby
            },
            onBack = {
                currentScreen = Screen.MainMenu
            }
        )
        is Screen.Lobby -> LobbyScreen(
            gameId = gameId,
            database = database,
            playerName = "Player1",
            isHost = true,
            onStartGame = {
                database.child("games").child(gameId).child("state").setValue("Questioning")
            },
            currentScreenState = { screen -> currentScreen = screen }
        )
        is Screen.PlayLocally -> PlayLocallyScreen(
            onBack = {
                currentScreen = Screen.MainMenu
            }
        )
        Screen.Splash -> SplashScreen { currentScreen = Screen.GameSetup }
        Screen.GameSetup -> GameSetupScreen(
            onStartGame = { playerCount, selectedCategories, gameDuration, imposterCount ->
                val selectedCategory = selectedCategories.first() // Use CategoryWithIcon directly
                currentScreen = Screen.PlayerNames(playerCount, selectedCategory, imposterCount)
            },
            onShowGuide = { currentScreen = Screen.Guide }
        )

        is Screen.PlayerNames -> PlayerNamesScreen(
            screen = currentScreen as Screen.PlayerNames,
            database = database,
            onConfirm = { names ->
                playerNames = names
                database.child("players").setValue(names) // Save player names to Firebase

                val selectedCategory = (currentScreen as Screen.PlayerNames).selectedCategory
                val imposterCount = (currentScreen as Screen.PlayerNames).imposterCount
                roles = assignRoles(playerNames.size, selectedCategory, imposterCount)


                database.child("roles").setValue(roles) // Save roles to Firebase
                currentScreen = Screen.GetReady(playerIndex = 0, playerNames, roles)
            },
            onBack = { currentScreen = Screen.GameSetup }
        )

        is Screen.GetReady -> GetReadyScreen(
            screen = currentScreen as Screen.GetReady,
            onProceed = { nextPlayerIndex ->
                currentScreen = Screen.PlayerRole(nextPlayerIndex, playerNames, roles)
            }
        )



        is Screen.Voting -> VotingScreen(
            gameId = gameId,
            database = database,
            playerName = "Player1",
            playerId = "player1",
            playerList = listOf(Pair("player1", "Player1"), Pair("player2", "Player2")),
            onVoteSubmitted = {
                database.child("games").child(gameId).child("state").setValue("Results")
            }
        )
        is Screen.Results -> ResultsScreen(
            gameId = gameId,
            database = database,
            onRestartGame = { currentScreen = Screen.Lobby },
            onExitGame = { /* Handle exit */ }
        )

        is Screen.PlayerRole -> PlayerRoleScreen(currentScreen as Screen.PlayerRole) { nextPlayerIndex ->
            if (nextPlayerIndex < playerNames.size) {
                currentScreen = Screen.GetReady(nextPlayerIndex, playerNames, roles)
            } else {
                currentScreen = Screen.QuestionsTime(playerNames)
            }
        }
        is Screen.QuestionsTime -> QuestionsTimeScreen(
            playerNames = playerNames,
            onNext = {
                val questionOrder = generateQuestionOrder(playerNames.size)
                currentScreen = Screen.QuestionOrder(playerNames, questionOrder)
            }
        )
        is Screen.QuestionOrder -> QuestionOrderScreen(
            playerNames = playerNames,
            questionOrder = (currentScreen as Screen.QuestionOrder).questionOrder,
            onStartGame = { currentScreen = Screen.FreeQuestionsScreen }
        )
        is Screen.FreeQuestionsScreen -> FreeQuestionsScreen(onNext = {})
        Screen.Guide -> GuideScreen { currentScreen = Screen.GameSetup }
    }
}
fun submitVote(gameId: String, voterId: String, votedPlayerId: String) {
    database.child("games").child(gameId).child("votes").child(voterId).setValue(votedPlayerId) // Record vote
}
fun calculateVotes(gameId: String, onResult: (Map<String, Int>) -> Unit) {
    database.child("games").child(gameId).child("votes").get().addOnSuccessListener { snapshot ->
        val votes = snapshot.children.map { it.value.toString() }
        val voteCount = votes.groupingBy { it }.eachCount() // Count votes for each player
        onResult(voteCount)
    }
}
fun endGame(gameId: String, voteResults: Map<String, Int>, imposterIds: List<String>) {
    val impostersCaught = imposterIds.any { voteResults[it] == voteResults.values.maxOrNull() }
    val gameResult = if (impostersCaught) "Imposters Caught!" else "Imposters Win!"
    database.child("games").child(gameId).child("state").setValue("Results")
    database.child("games").child(gameId).child("result").setValue(gameResult)
}
@Composable
fun LobbyScreen(
    gameId: String,
    database: DatabaseReference,
    playerName: String,
    isHost: Boolean,
    onStartGame: () -> Unit,
    currentScreenState: (Screen) -> Unit
) {
    var players by remember { mutableStateOf<List<String>>(emptyList()) }

    // Observe players joining the game
    LaunchedEffect(gameId) {
        database.child("games").child(gameId).child("players")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    players = snapshot.children.map { it.child("name").value.toString() }
                }

                override fun onCancelled(error: DatabaseError) {
                    println("Error fetching players: ${error.message}")
                }
            })
    }

    // Observe game state changes
    LaunchedEffect(gameId) {
        database.child("games").child(gameId).child("state")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val state = snapshot.getValue(String::class.java)
                    currentScreenState(
                        when (state) {
                            "Questioning" -> Screen.PlayerRole(0, players, listOf("Detective", "Imposter"))
                            "Voting" -> Screen.Voting
                            "Results" -> Screen.Results
                            else -> Screen.Lobby
                        }
                    )
                }

                override fun onCancelled(error: DatabaseError) {
                    println("Error fetching game state: ${error.message}")
                }
            })
    }

    // Display the lobby UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Lobby", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Game ID: $gameId", fontSize = 16.sp)

        // Show list of players
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(players) { player ->
                Text(player, fontSize = 20.sp, modifier = Modifier.padding(8.dp))
            }
        }

        // Only the host sees the Start Game button
        if (isHost) {
            Button(onClick = onStartGame, modifier = Modifier.fillMaxWidth()) {
                Text("Start Game")
            }
        }
    }
}

@Composable
fun VotingScreen(
    gameId: String,
    database: DatabaseReference,
    playerName: String,
    playerId: String,
    playerList: List<Pair<String, String>>, // Pair of player ID and name
    onVoteSubmitted: () -> Unit
) {
    var selectedPlayerId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Voting Time", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        // List of players to vote on
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(playerList) { (id, name) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable { selectedPlayerId = id },
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(name, fontSize = 20.sp)
                    RadioButton(selected = selectedPlayerId == id, onClick = { selectedPlayerId = id })
                }
            }
        }

        // Submit vote button
        Button(
            onClick = {
                selectedPlayerId?.let { votedPlayerId ->
                    database.child("games").child(gameId).child("votes").child(playerId).setValue(votedPlayerId)
                    onVoteSubmitted()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedPlayerId != null // Enable only if a player is selected
        ) {
            Text("Submit Vote")
        }
    }
}
@Composable
fun ResultsScreen(
    gameId: String,
    database: DatabaseReference,
    onRestartGame: () -> Unit,
    onExitGame: () -> Unit
) {
    var result by remember { mutableStateOf<String>("Loading...") }

    // Observe game result
    LaunchedEffect(gameId) {
        database.child("games").child(gameId).child("result")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    result = snapshot.getValue(String::class.java) ?: "No result"
                }

                override fun onCancelled(error: DatabaseError) {
                    println("Error fetching result: ${error.message}")
                }
            })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Game Over", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(result, fontSize = 20.sp)

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = onRestartGame) {
                Text("Restart")
            }
            Button(onClick = onExitGame) {
                Text("Exit")
            }
        }
    }
}

//// Function to assign roles
//fun assignRoles(playerCount: Int, selectedCategory: CategoryWithIcon, imposterCount: Int): List<String> {
//    val roles = MutableList(playerCount) { "Detective" }
//    val imposterIndices = (0 until playerCount).shuffled().take(imposterCount)
//    for (index in imposterIndices) {
//        roles[index] = "Imposter"
//    }
//    val selectedWord = selectedCategory.words.random()
//    for (i in roles.indices) {
//        if (roles[i] != "Imposter") {
//            roles[i] = "Detective\nWord: $selectedWord"
//        }
//    }
//    return roles
//}
fun assignRoles(playerCount: Int, selectedCategory: CategoryWithIcon, imposterCount: Int): List<String> {
    try {
        val selectedWord = selectedCategory.words.random()
        val roles = MutableList(playerCount) { "Detective - Word: $selectedWord" }

        // Randomly assign imposters
        val imposterIndices = (0 until playerCount).shuffled().take(imposterCount)
        imposterIndices.forEach { roles[it] = "Imposter" }

        return roles
    } catch (e: Exception) {
        println("Error in assignRoles: ${e.message}")
        throw e
    }
}






// Composables for each screen
@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        content = { innerPadding ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    // Display the logo image (replace with your logo)
                    Image(
                        painter = painterResource(id = R.drawable.mafia1), // Replace with your logo resource
                        contentDescription = "Game Logo",
                        modifier = Modifier.size(500.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Display the game name
                    Text(
                        text = "",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                LaunchedEffect(Unit) {
                    delay(3000L)
                    onTimeout() // Call the callback to navigate to the GameSetup
                }
            }
        }
    )
}

val predefinedCategories = listOf(
    CategoryWithIcon("Locations", mutableListOf("Theatre", "Forest", "School", "Bar", "Airport", "Circus", "Hospital"), R.drawable.ic_location),
    CategoryWithIcon("Animals", mutableListOf("Cat", "Dog", "Lion", "Tiger", "Kangaroo", "Rabbit", "Snake", "Eagle", "Giraffe"), R.drawable.ic_animal),
    CategoryWithIcon("Sports", mutableListOf("Tennis Court", "Pool", "Stadium", "Ice Hockey Arena", "Golf Course", "Football Field"), R.drawable.ic_sports),
    CategoryWithIcon("Time Travel", mutableListOf("Ancient Greece", "Ancient Rome", "Stone Age", "World War II", "1990's", "2000's"), R.drawable.ic_timetravel)
)

@Composable
fun GameSetupScreen(onStartGame: (Int, List<CategoryWithIcon>, Int?, Int) -> Unit, onShowGuide: () -> Unit) {
    // Game setup UI with sliders for player count, game duration, and imposter count
    var selectedCategory by remember { mutableStateOf<CategoryWithIcon?>(null) }
    var playerCount by remember { mutableIntStateOf(3) }
    var gameDuration by remember { mutableIntStateOf(5) } // Default to 5 minutes
    var isGameDurationEnabled by remember { mutableStateOf(true) } // Toggle for enabling/disabling game duration
    var imposterCount by remember { mutableIntStateOf(1) } // Default to 1 imposter

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Game Logo
            Image(
                painter = painterResource(id = R.drawable.mafia1),
                contentDescription = "Game Logo",
                modifier = Modifier
                    .size(200.dp)
                    .padding(bottom = 16.dp)
            )

            // Category Selection from Predefined List with Icons
            Text("Select a Category", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            CategoryIconRow(categories = predefinedCategories, selectedCategory = selectedCategory) {
                selectedCategory = it
            }

            selectedCategory?.let {
                Text("Selected Category: ${it.name}", fontWeight = FontWeight.Bold)
            }

            // Number of Players Slider
            Text("Number of Players: $playerCount")
            Slider(
                value = playerCount.toFloat(),
                onValueChange = { playerCount = it.toInt() },
                valueRange = 3f..10f,
                steps = 7,
                colors = SliderDefaults.colors(
                    thumbColor = Color.Red,
                    activeTrackColor = Color.Red
                )
            )

            // Show Imposter selection if player count is 6 or more
            if (playerCount >= 6) {
                Text("Number of Imposters: $imposterCount")
                Slider(
                    value = imposterCount.toFloat(),
                    onValueChange = { imposterCount = it.toInt() },
                    valueRange = 1f..2f,  // 1 or 2 imposters
                    steps = 1,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.Red,
                        activeTrackColor = Color.Red
                    )
                )
            }

            // Toggle for enabling/disabling game duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Enable Game Duration")
                Switch(
                    checked = isGameDurationEnabled,
                    onCheckedChange = { isGameDurationEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Red,
                        uncheckedThumbColor = Color.Gray
                    )
                )
            }

            // Game Duration Slider (only enabled if the toggle is ON)
            Text("Game Duration: ${if (isGameDurationEnabled) "$gameDuration minutes" else "Disabled"}")
            Slider(
                value = gameDuration.toFloat(),
                onValueChange = { if (isGameDurationEnabled) gameDuration = it.toInt() },
                valueRange = 1f..10f,  // Min 1 minute, Max 10 minutes
                steps = 9,
                enabled = isGameDurationEnabled, // Disable the slider when game duration is off
                colors = SliderDefaults.colors(
                    thumbColor = if (isGameDurationEnabled) Color.Red else Color.Gray,
                    activeTrackColor = if (isGameDurationEnabled) Color.Red else Color.Gray
                )
            )

            // Start Game Button
            Button(
                onClick = {
                    selectedCategory?.let {
                        val finalGameDuration = if (isGameDurationEnabled) gameDuration else null // Pass null if game duration is disabled
                        onStartGame(playerCount, listOf(it), finalGameDuration, imposterCount) // Include imposterCount in the game setup
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red,
                    contentColor = Color.White
                )
            ) {
                Text("Next")
            }
        }

        // How to Play Button in the top-right corner
        Button(
            onClick = { onShowGuide() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Red,
                contentColor = Color.White
            )
        ) {
            Text("How to Play", color = Color.White)
        }
    }
}





data class CategoryWithIcon(
    val name: String,
    val words: MutableList<String>,
    val iconResId: Int // Icon resource ID
)

@Composable
fun CategoryIconRow(
    categories: List<CategoryWithIcon>,
    selectedCategory: CategoryWithIcon?,
    onCategorySelected: (CategoryWithIcon) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        categories.forEach { category ->
            IconToggleButton(
                checked = category == selectedCategory,
                onCheckedChange = {
                    onCategorySelected(category)
                }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = category.iconResId),
                        contentDescription = category.name,
                        modifier = Modifier.size(64.dp),
                        tint = if (category == selectedCategory) Color.Red else Color.Black // Red if selected, Gray if not
                    )
                    Text(
                        text = category.name,
                        color = if (category == selectedCategory) Color.Red else Color.Black // Text is red if selected
                    )
                }
            }
        }
    }
}



@Composable
fun PlayerNamesScreen(
    screen: Screen.PlayerNames,
    database: DatabaseReference,
    onConfirm: (List<String>) -> Unit,
    onBack: () -> Unit
) {
    var playerNames = remember { mutableStateListOf(*Array(screen.playerCount) { "" }) }
    val playerCount = screen.playerCount
    database.child("players").addValueEventListener(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            playerNames.clear()
            snapshot.children.mapNotNullTo(playerNames) { it.getValue(String::class.java) }
        }

        override fun onCancelled(error: DatabaseError) {}
    })

    val focusManager = LocalFocusManager.current
    val focusRequesters = List(playerCount) { FocusRequester() }

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Back Button
        Button(
            onClick = { onBack() },
            modifier = Modifier
                .width(100.dp)
                .height(48.dp)
                .align(Alignment.TopStart)
                .padding(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Red,
                contentColor = Color.White
            )
        ) {
            Text("Back", fontSize = 15.sp)
        }

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 64.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Enter Player Names", fontSize = 24.sp, fontWeight = FontWeight.Bold)

            playerNames.forEachIndexed { index, name ->
                TextField(
                    value = name,
                    onValueChange = { playerNames[index] = it },
                    label = { Text("Player ${index + 1} Name") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = if (index < playerCount - 1) ImeAction.Next else ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            if (index < playerCount - 1) {
                                focusRequesters[index + 1].requestFocus()
                            } else {
                                focusManager.clearFocus()
                            }
                        },
                        onDone = {
                            focusManager.clearFocus()
                        }
                    )
                )
            }

            Button(
                onClick = { onConfirm(playerNames) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red,
                    contentColor = Color.White
                )
            ) {
                Text("Confirm Names")
            }
        }
    }
}



@Composable
fun GetReadyScreen(
    screen: Screen.GetReady,
    onProceed: (Int) -> Unit
) {
    val playerIndex = screen.playerIndex
    val playerName = screen.playerNames[playerIndex]
    val transition =
        updateTransition(true, label = "RoleScreenTransition")

    val alpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 600) },
        label = "AlphaAnimation"
    ) { if (it) 1f else 0f }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .graphicsLayer(alpha = alpha) // Apply fade-in animation
            .clickable(onClick = { onProceed(playerIndex) }), // Handle tap on the Box
        contentAlignment = Alignment.Center
    ) {
        // Card with role details
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f),
            border = BorderStroke(2.dp, Color.White), // White outline
            shape = RoundedCornerShape(8.dp), // Rounded corners
            elevation = CardDefaults.cardElevation(8.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Get Ready, $playerName!", fontSize = 24.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(32.dp))

                Text("Tap To See Your Role")
            }
        }
    }
}


@Composable
fun PlayerRoleScreen(
    screen: Screen.PlayerRole,
    onNextPlayer: (Int) -> Unit
) {
    val playerIndex = screen.playerIndex
    val playerName = screen.playerNames.getOrNull(playerIndex) ?: "Unknown Player"
    val role = screen.roles.getOrNull(playerIndex) ?: "Unknown Role"

    // Determine border and font color based on the player's role
    val borderColor = if (role.contains("Imposter")) Color.Red else Color.Black
    val fontColor = if (role.contains("Imposter")) Color.Red else Color.Black
    // Conditionally select the image based on the role
    val imageResource = if (role.contains("Imposter")) {
        R.drawable.ic_imposter // Imposter image
    } else {
        R.drawable.ic_detective // Detective image
    }

    val transition =
        updateTransition(true, label = "RoleScreenTransition")

    val alpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 600) },
        label = "AlphaAnimation"
    ) { if (it) 1f else 0f }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .graphicsLayer(alpha = alpha) // Apply fade-in animation
            .clickable(onClick = { onNextPlayer(playerIndex + 1) }), // Handle tap on the Box
        contentAlignment = Alignment.Center
    ) {
        // Card with role details
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f),
            border = BorderStroke(2.dp, borderColor), // Use conditional border color
            shape = RoundedCornerShape(8.dp), // Rounded corners
            elevation = CardDefaults.cardElevation(8.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Display the role image without any color filter
                Image(
                    painter = painterResource(id = imageResource), // Image resource for the role
                    contentDescription = "Role Icon", // Content description for accessibility
                    modifier = Modifier.size(200.dp), // Set the size of the image
                    contentScale = ContentScale.Crop // Scale the image to fill the size
                )

                Spacer(modifier = Modifier.height(100.dp))

                Text(
                    text = "Your Role: $role",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = fontColor // Apply conditional font color
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = if (playerIndex == screen.playerNames.size - 1) "Start Game" else "Next Player",
                    color = fontColor // Apply conditional font color to the action text as well
                )
            }
        }
    }
}




@Composable
fun QuestionsTimeScreen(playerNames: List<String>, onNext: () -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        content = { innerPadding ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Questions Time",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red,
                        fontStyle = FontStyle.Italic
                    )

                    Text(
                        text = "Each person is going to ask the other a question related to the topic. Press Next to find out who is asking who.",
                        fontSize = 18.sp,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onNext,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Next")
                    }
                }
            }
        }
    )
}


@Composable
fun QuestionOrderScreen(playerNames: List<String>, questionOrder: List<Pair<Int, Int>>, onStartGame: () -> Unit) {
    var currentQuestionIndex by remember { mutableStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        content = { innerPadding ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Title
                    Text(
                        text = "Questions Time",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red,
                        fontStyle = FontStyle.Italic
                    )

                    // Display only the current question pair
                    val (askerIndex, responderIndex) = questionOrder[currentQuestionIndex]
                    Text(
                        text = "${playerNames[askerIndex]} will ask ${playerNames[responderIndex]}",
                        fontSize = 18.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Next Button
                    Button(
                        onClick = {
                            // If it's the last question, start the game, else move to the next question
                            if (currentQuestionIndex < questionOrder.size - 1) {
                                currentQuestionIndex += 1
                            } else {
                                onStartGame() // Navigate or start game
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red,
                            contentColor = Color.White
                        )
                    ) {
                        Text(if (currentQuestionIndex < questionOrder.size - 1) "Next" else "Continue")
                    }
                }
            }
        }
    )
}


@Composable
fun FreeQuestionsScreen(onNext: () -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        content = { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Centered Text content
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Questions Time",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red,
                        fontStyle = FontStyle.Italic
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "All players ask each other randomly now",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center, // Center-align text
                        maxLines = 2, // Allow wrapping to two lines if needed
                        modifier = Modifier.fillMaxWidth(0.8f) // Limit width to make centering easier
                    )
                }

                // Button aligned to the bottom of the screen
                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp), // Add padding to avoid the button touching the screen edge
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red,
                        contentColor = Color.White
                    )
                ) {
                    Text("Finish and go to voting")
                }
            }
        }
    )
}


@Composable
fun GuideScreen(onBack: () -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "How to Play",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "1. The game will assign words depending on the category to all players, except one.\n" +
                            "2. The player without the word is the imposter.\n" +
                            "3. The players will start asking each other questions.\n" +
                            "4. The imposter must blend in and try to figure out the word.\n" +
                            "5. At the end, everyone votes on who they think the imposter is.\n" +
                            "6. The players that guess it correctly gain a point.\n" +
                            "7. The imposter also gets a chance to guess the word based on the information gathered during the game.\n" +
                            "8. The imposter gains a point if guessed correctly."
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onBack() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red, // Set the button background color to red
                        contentColor = Color.White  // Set the text color to white
                    )
                ) {
                    Text("Back")
                }

            }
        }
    )
}
fun assignLocalRoles(playerCount: Int): List<String> {
    val roles = MutableList(playerCount) { "Detective - Word: Forest" } // Example word
    val imposterIndex = (0 until playerCount).random() // Randomly assign imposter
    roles[imposterIndex] = "Imposter"
    return roles
}


fun generateQuestionOrder(playerCount: Int): List<Pair<Int, Int>> {
    return (0 until playerCount).map { it to (it + 1) % playerCount }
}

// Data classes for the game
sealed class Screen {
    object Lobby : Screen()
    object Voting : Screen()
    object MainMenu : Screen() // Represents the main menu
    object JoinGame : Screen()
    object PlayLocally : Screen()
    object Results : Screen()
    data class PlayerRole(val playerIndex: Int, val playerNames: List<String>, val roles: List<String>) : Screen()
    object Splash : Screen()
    object GameSetup : Screen()
    data class PlayerNames(val playerCount: Int, val selectedCategory: CategoryWithIcon, val imposterCount: Int) : Screen()
    data class GetReady(val playerIndex: Int, val playerNames: List<String>, val roles: List<String>) : Screen()
    object Guide : Screen()
    data class QuestionOrder(val playerNames: List<String>, val questionOrder: List<Pair<Int, Int>>) : Screen()

    // Change QuestionsTime to a data class to accept parameters
    data class QuestionsTime(val playerNames: List<String>) : Screen()
    object FreeQuestionsScreen : Screen()
}


@Composable
fun CategoryDropdown(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        TextButton(onClick = { expanded = true }) {
            Text(text = selectedCategory)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category) },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun GameSetupScreenPreview() {
    ImposterTheme {
        GameSetupScreen(
            onStartGame = {playerCount, categories, gameDuration, imposterCount ->
                // Mock action for starting the game (you can leave it empty or add mock logic)
            },
            onShowGuide = {
                // Mock action for showing the guide
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    ImposterTheme {
        SplashScreen { }
    }
}
