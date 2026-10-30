package com.pandyabhavik.snakes.ui

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pandyabhavik.snakes.core.Game
import com.pandyabhavik.snakes.core.Layout
import com.pandyabhavik.snakes.core.Rules
import com.pandyabhavik.snakes.core.Turn
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.SecureRandom

private val Context.store: DataStore<Preferences> by preferencesDataStore("snakes")
private val GAME_KEY = stringPreferencesKey("game")
private val NAME1_KEY = stringPreferencesKey("name1")
private val NAME2_KEY = stringPreferencesKey("name2")
private val VS_PHONE_KEY = booleanPreferencesKey("vsPhone")
private val SOUND_KEY = booleanPreferencesKey("sound")
private val HAPTICS_KEY = booleanPreferencesKey("haptics")
private val THEME_KEY = stringPreferencesKey("theme")
private val PIECE_KEY = stringPreferencesKey("piece")
private val COLOR1_KEY = stringPreferencesKey("color1")
private val COLOR2_KEY = stringPreferencesKey("color2")
private val DICE_KEY = stringPreferencesKey("dice")

/** How the table looks and sounds. */
data class Settings(
    val soundOn: Boolean = true,
    val hapticsOn: Boolean = true,
    val theme: Theme = Theme.JADE,
    val piece: PieceStyle = PieceStyle.ORB,
    val colors: List<PieceColor> = listOf(PieceColor.RUBY, PieceColor.SAPPHIRE),
    val dice: DiceStyle = DiceStyle.IVORY,
)

/** Who is playing. With [vsPhone] the second seat is the phone, which rolls for itself. */
data class Players(val names: List<String> = listOf("Player 1", "Player 2"), val vsPhone: Boolean = false) {
    fun name(i: Int): String = if (i == 1 && vsPhone) "Phone" else names[i].ifBlank { "Player ${i + 1}" }
    fun isPhone(i: Int): Boolean = vsPhone && i == 1
}

data class UiState(
    val game: Game = Game(),
    val players: Players = Players(),
    val settings: Settings = Settings(),
    val loaded: Boolean = false,
    /** True on the very first launch, so the screen can open the new-game sheet. */
    val firstRun: Boolean = false,
    /** The roll being shown, if any; [turnId] changes with every roll and every new game. */
    val turn: Turn? = null,
    val turnId: Int = 0,
    /** A roll is being animated: the die can't be tapped until the piece has settled. */
    val busy: Boolean = false,
) {
    val phoneToRoll: Boolean get() = players.isPhone(game.current) && !game.isOver
}

/**
 * One game at a time, saved after every roll, so closing the app and coming back resumes it. The view
 * model decides each roll at once; the screen then animates it and reports back with [turnShown].
 */
class GameViewModel(private val app: Application) : ViewModel() {
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state
    private val random = SecureRandom()
    private var phoneJob: Job? = null

    init {
        viewModelScope.launch {
            val prefs = app.store.data.first()
            val saved = prefs[GAME_KEY]?.let(Game::deserialize)
            val settings = Settings(
                soundOn = prefs[SOUND_KEY] ?: true,
                hapticsOn = prefs[HAPTICS_KEY] ?: true,
                theme = Theme.byId(prefs[THEME_KEY]),
                piece = PieceStyle.byId(prefs[PIECE_KEY]),
                colors = listOf(PieceColor.byId(prefs[COLOR1_KEY], PieceColor.RUBY), PieceColor.byId(prefs[COLOR2_KEY], PieceColor.SAPPHIRE)),
                dice = DiceStyle.byId(prefs[DICE_KEY]),
            )
            val players = Players(listOf(prefs[NAME1_KEY] ?: "Player 1", prefs[NAME2_KEY] ?: "Player 2"), prefs[VS_PHONE_KEY] ?: false)
            _state.value = UiState(game = saved ?: Game(), players = players, settings = settings, loaded = true, firstRun = saved == null)
            schedulePhone()
        }
    }

    /** Roll the die for whoever's turn it is. Taps are ignored while the phone has the turn. */
    fun roll(byPhone: Boolean = false) {
        val s = _state.value
        if (!s.loaded || s.busy || s.game.isOver) return
        if (s.phoneToRoll != byPhone) return
        val (next, turn) = s.game.play(1 + random.nextInt(6))
        _state.update { it.copy(game = next, turn = turn, turnId = it.turnId + 1, busy = true, firstRun = false) }
        save(next)
    }

    /** The screen has finished animating roll [id]. */
    fun turnShown(id: Int) {
        if (_state.value.turnId != id) return
        _state.update { it.copy(busy = false) }
        schedulePhone()
    }

    private fun schedulePhone() {
        phoneJob?.cancel()
        if (!_state.value.phoneToRoll) return
        phoneJob = viewModelScope.launch {
            delay(850)
            roll(byPhone = true)
        }
    }

    fun newGame(layout: Layout, rules: Rules, players: Players) {
        val game = Game(layout = layout, rules = rules)
        _state.update { it.copy(game = game, players = players, turn = null, turnId = it.turnId + 1, busy = false, firstRun = false) }
        save(game)
        viewModelScope.launch {
            app.store.edit {
                it[NAME1_KEY] = players.names[0]
                it[NAME2_KEY] = players.names[1]
                it[VS_PHONE_KEY] = players.vsPhone
            }
        }
        schedulePhone()
    }

    fun playAgain() = newGame(_state.value.game.layout, _state.value.game.rules, _state.value.players)

    fun dismissFirstRun() = _state.update { it.copy(firstRun = false) }

    fun setSound(on: Boolean) = setting(SOUND_KEY, on) { copy(soundOn = on) }
    fun setHaptics(on: Boolean) = setting(HAPTICS_KEY, on) { copy(hapticsOn = on) }
    fun setTheme(theme: Theme) = setting(THEME_KEY, theme.id) { copy(theme = theme) }
    fun setPiece(piece: PieceStyle) = setting(PIECE_KEY, piece.id) { copy(piece = piece) }
    fun setDice(dice: DiceStyle) = setting(DICE_KEY, dice.id) { copy(dice = dice) }

    /** Gives [player] the colour; if the other player had it, they swap, so the pieces never match. */
    fun setColor(player: Int, color: PieceColor) {
        val colors = _state.value.settings.colors.toMutableList()
        val other = 1 - player
        if (colors[other] == color) colors[other] = colors[player]
        colors[player] = color
        _state.update { it.copy(settings = it.settings.copy(colors = colors)) }
        viewModelScope.launch {
            app.store.edit {
                it[COLOR1_KEY] = colors[0].id
                it[COLOR2_KEY] = colors[1].id
            }
        }
    }

    private fun <T> setting(key: Preferences.Key<T>, value: T, change: Settings.() -> Settings) {
        _state.update { it.copy(settings = it.settings.change()) }
        viewModelScope.launch { app.store.edit { it[key] = value } }
    }

    private fun save(game: Game) {
        viewModelScope.launch { app.store.edit { it[GAME_KEY] = game.serialize() } }
    }

    companion object {
        fun factory(app: Application): ViewModelProvider.Factory = viewModelFactory { initializer { GameViewModel(app) } }
    }
}
