package com.example.game

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sound.SnakeSoundManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

data class GridPoint(val x: Int, val y: Int)

enum class Direction {
    UP, DOWN, LEFT, RIGHT;

    fun isOpposite(other: Direction): Boolean = when (this) {
        UP -> other == DOWN
        DOWN -> other == UP
        LEFT -> other == RIGHT
        RIGHT -> other == LEFT
    }
}

enum class GameState {
    MENU, RUNNING, PAUSED, GAME_OVER
}

enum class Difficulty {
    EASY, MEDIUM, HARD;

    fun getStartSpeedMs(): Long = when (this) {
        EASY -> 240L
        MEDIUM -> 160L
        HARD -> 100L
    }

    fun getAccelerationMs(): Long = when (this) {
        EASY -> 6L
        MEDIUM -> 4L
        HARD -> 3L
    }

    fun getMinSpeedMs(): Long = when (this) {
        EASY -> 100L
        MEDIUM -> 60L
        HARD -> 40L
    }
}

data class SnakeUiState(
    val snake: List<GridPoint> = emptyList(),
    val direction: Direction = Direction.RIGHT,
    val food: GridPoint = GridPoint(5, 5),
    val foodType: FoodType = FoodType.REGULAR,
    val gameState: GameState = GameState.MENU,
    val score: Int = 0,
    val highHighScore: Int = 0,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val isSoundMuted: Boolean = false
)

enum class FoodType {
    REGULAR, GOLD;

    fun points(): Int = when (this) {
        REGULAR -> 10
        GOLD -> 30
    }
}

class SnakeGameEngine(application: Application) : AndroidViewModel(application) {

    companion object {
        const val GRID_COLUMNS = 20
        const val GRID_ROWS = 28
        private const val PREFS_NAME = "snake_prefs"
        private const val PREF_HIGH_SCORE_PREFIX = "high_score_"
    }

    private val sharedPrefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val soundManager = SnakeSoundManager(application)

    private val _uiState = MutableStateFlow(SnakeUiState())
    val uiState: StateFlow<SnakeUiState> = _uiState.asStateFlow()

    private var gameJob: Job? = null
    private var currentSpeedMs: Long = Difficulty.MEDIUM.getStartSpeedMs()

    // Flag to lock direction updates to prevent double-press collision (e.g. clicking down then left in same tick)
    private var directionInputLocked = false

    init {
        loadHighScore()
        _uiState.update { it.copy(isSoundMuted = soundManager.isCurrentlyMuted()) }
    }

    fun setDifficulty(difficulty: Difficulty) {
        _uiState.update { it.copy(difficulty = difficulty) }
        loadHighScore()
    }

    private fun loadHighScore() {
        val key = "$PREF_HIGH_SCORE_PREFIX${_uiState.value.difficulty.name}"
        val score = sharedPrefs.getInt(key, 0)
        _uiState.update { it.copy(highHighScore = score) }
    }

    private fun saveHighScore(score: Int) {
        val key = "$PREF_HIGH_SCORE_PREFIX${_uiState.value.difficulty.name}"
        if (score > _uiState.value.highHighScore) {
            sharedPrefs.edit().putInt(key, score).apply()
            _uiState.update { it.copy(highHighScore = score) }
        }
    }

    fun toggleMute() {
        val isMuted = soundManager.toggleMute()
        _uiState.update { it.copy(isSoundMuted = isMuted) }
    }

    fun handleDirectionInput(newDirection: Direction) {
        if (directionInputLocked) return
        val currentDirection = _uiState.value.direction
        if (currentDirection.isOpposite(newDirection) || currentDirection == newDirection) return

        _uiState.update { it.copy(direction = newDirection) }
        directionInputLocked = true // Lock inputs until next tick registers
    }

    fun startGame() {
        val diff = _uiState.value.difficulty
        currentSpeedMs = diff.getStartSpeedMs()
        
        // Start in the center of the grid moving right
        val initialSnake = listOf(
            GridPoint(GRID_COLUMNS / 2, GRID_ROWS / 2),
            GridPoint(GRID_COLUMNS / 2 - 1, GRID_ROWS / 2),
            GridPoint(GRID_COLUMNS / 2 - 2, GRID_ROWS / 2)
        )

        directionInputLocked = false

        _uiState.update {
            it.copy(
                snake = initialSnake,
                direction = Direction.RIGHT,
                gameState = GameState.RUNNING,
                score = 0
            )
        }

        spawnFood(initialSnake)
        startGameLoop()
    }

    private fun startGameLoop() {
        gameJob?.cancel()
        gameJob = viewModelScope.launch {
            while (uiState.value.gameState == GameState.RUNNING) {
                delay(currentSpeedMs)
                gameStep()
            }
        }
    }

    fun pauseGame() {
        if (_uiState.value.gameState == GameState.RUNNING) {
            _uiState.update { it.copy(gameState = GameState.PAUSED) }
            gameJob?.cancel()
        }
    }

    fun resumeGame() {
        if (_uiState.value.gameState == GameState.PAUSED) {
            _uiState.update { it.copy(gameState = GameState.RUNNING) }
            startGameLoop()
        }
    }

    private fun gameStep() {
        directionInputLocked = false // Unlock directions
        val state = _uiState.value
        val head = state.snake.firstOrNull() ?: return
        val currentDir = state.direction

        // Calculate next head coordinate
        val nextHead = when (currentDir) {
            Direction.UP -> GridPoint(head.x, head.y - 1)
            Direction.DOWN -> GridPoint(head.x, head.y + 1)
            Direction.LEFT -> GridPoint(head.x - 1, head.y)
            Direction.RIGHT -> GridPoint(head.x + 1, head.y)
        }

        // Check Wall or Body Collision
        if (isOutOfBound(nextHead) || isCollidingWithBody(nextHead, state.snake)) {
            triggerGameOver()
            return
        }

        val collectedFood = nextHead == state.food
        val newSnake = ArrayList<GridPoint>()
        newSnake.add(nextHead)
        
        if (collectedFood) {
            soundManager.playEatSound()
            newSnake.addAll(state.snake) // Growth - Keep entire body

            val pointsEarned = state.foodType.points()
            val newScore = state.score + pointsEarned
            
            _uiState.update { it.copy(score = newScore) }
            saveHighScore(newScore)

            // Adjust snake speed intervals (accelerate slightly)
            val diff = state.difficulty
            val newSpeed = (currentSpeedMs - diff.getAccelerationMs()).coerceAtLeast(diff.getMinSpeedMs())
            currentSpeedMs = newSpeed

            spawnFood(newSnake)
        } else {
            // Normal slide - Discard final tail segment
            newSnake.addAll(state.snake.dropLast(1))
        }

        _uiState.update { it.copy(snake = newSnake) }
    }

    private fun spawnFood(snakeBody: List<GridPoint>) {
        val emptyCells = mutableListOf<GridPoint>()
        for (col in 0 until GRID_COLUMNS) {
            for (row in 0 until GRID_ROWS) {
                val point = GridPoint(col, row)
                if (!snakeBody.contains(point)) {
                    emptyCells.add(point)
                }
            }
        }

        val spawnedPoint = if (emptyCells.isNotEmpty()) {
            emptyCells[Random.nextInt(emptyCells.size)]
        } else {
            GridPoint(0, 0) // Board is full, extremely rare
        }

        // 15% chance to generate a special GOLD bonus cherry
        val foodType = if (Random.nextFloat() < 0.15f) FoodType.GOLD else FoodType.REGULAR

        _uiState.update {
            it.copy(
                food = spawnedPoint,
                foodType = foodType
            )
        }
    }

    private fun isOutOfBound(point: GridPoint): Boolean {
        return point.x < 0 || point.x >= GRID_COLUMNS || point.y < 0 || point.y >= GRID_ROWS
    }

    private fun isCollidingWithBody(head: GridPoint, snake: List<GridPoint>): Boolean {
        // Checking head overlap with any existing segments, skip the very last segment since it is moving
        return snake.dropLast(1).contains(head)
    }

    private fun triggerGameOver() {
        soundManager.playGameOverSound()
        gameJob?.cancel()
        _uiState.update { it.copy(gameState = GameState.GAME_OVER) }
    }

    fun exitToMenu() {
        gameJob?.cancel()
        _uiState.update { it.copy(gameState = GameState.MENU) }
    }

    override fun onCleared() {
        soundManager.release()
        super.onCleared()
    }
}
