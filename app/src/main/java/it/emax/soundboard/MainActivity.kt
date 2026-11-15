package it.emax.soundboard

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    private val padCount = 16
    private val bankCount = 8
    private var bank = 0
    private var selectedPad = 0
    private lateinit var bankLabel: TextView
    private lateinit var statusLabel: TextView
    private lateinit var padGrid: GridLayout
    private val padButtons = mutableListOf<Button>()
    private val players = mutableMapOf<Int, MediaPlayer>()
    private val audioFiles = mutableMapOf<String, String>()
    private val padNames = mutableMapOf<String, String>()

    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) importAudio(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = 0xFF10131A.toInt()
        window.navigationBarColor = 0xFF10131A.toInt()
        loadSettings()
        buildUi()
    }

    private fun key(bank: Int, pad: Int) = "$bank:$pad"

    private fun loadSettings() {
        val prefs = getSharedPreferences("soundboard", MODE_PRIVATE)
        for (b in 0 until bankCount) for (p in 0 until padCount) {
            val k = key(b, p)
            prefs.getString("file_$k", null)?.let { audioFiles[k] = it }
            prefs.getString("name_$k", null)?.let { padNames[k] = it }
        }
    }

    private fun savePad(k: String) {
        getSharedPreferences("soundboard", MODE_PRIVATE).edit()
            .putString("file_$k", audioFiles[k])
            .putString("name_$k", padNames[k])
            .apply()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(0xFF10131A.toInt())
        }

        val title = TextView(this).apply {
            text = "EMAX  SOUNDboard"
            textSize = 23f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(12))
        }
        root.addView(title)

        bankLabel = TextView(this).apply {
            textSize = 16f
            setTextColor(0xFFB8C5D9.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(10))
        }
        root.addView(bankLabel)

        val bankRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        bankRow.addView(actionButton("◀ BANCO") { changeBank(-1) }, weighted())
        bankRow.addView(actionButton("BANCO ▶") { changeBank(1) }, weighted())
        root.addView(bankRow)

        padGrid = GridLayout(this).apply {
            columnCount = 4
            rowCount = 4
            useDefaultMargins = false
        }
        val gridParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ).apply { topMargin = dp(12); bottomMargin = dp(12) }
        root.addView(padGrid, gridParams)

        for (p in 0 until padCount) {
            val button = Button(this).apply {
                textSize = 12f
                isAllCaps = false
                setTextColor(0xFFFFFFFF.toInt())
                setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF26364B.toInt()))
                setOnClickListener {
                    selectedPad = p
                    playPad(p)
                }
                setOnLongClickListener {
                    selectedPad = p
                    picker.launch(arrayOf("audio/*"))
                    true
                }
            }
            padButtons.add(button)
            val lp = GridLayout.LayoutParams().apply {
                width = 0
                height = 0
                columnSpec = GridLayout.spec(p % 4, 1f)
                rowSpec = GridLayout.spec(p / 4, 1f)
                setMargins(dp(3), dp(3), dp(3), dp(3))
            }
            padGrid.addView(button, lp)
        }

        val stop = actionButton("■  STOP TUTTO") { stopAll() }.apply {
            textSize = 18f
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFB52D3A.toInt()))
        }
        root.addView(stop, LinearLayout.LayoutParams(-1, dp(58)).apply { bottomMargin = dp(8) })

        statusLabel = TextView(this).apply {
            text = "Tieni premuto un pad per caricare un audio"
            textSize = 12f
            setTextColor(0xFF9EABC0.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(4))
        }
        root.addView(statusLabel)
        setContentView(root)
        refreshPads()
    }

    private fun weighted() = LinearLayout.LayoutParams(0, dp(48), 1f).apply {
        setMargins(dp(3), 0, dp(3), 0)
    }

    private fun actionButton(label: String, action: () -> Unit) =
        Button(this).apply {
            text = label
            textSize = 13f
            isAllCaps = false
            setOnClickListener { action() }
        }

    private fun refreshPads() {
        bankLabel.text = "BANCO ${bank + 1} / $bankCount"
        for (p in 0 until padCount) {
            val k = key(bank, p)
            val name = padNames[k] ?: "PAD ${p + 1}"
            val loaded = audioFiles.containsKey(k)
            padButtons[p].text = (if (loaded) "● " else "＋ ") + name
            padButtons[p].setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(if (loaded) 0xFF245C59.toInt() else 0xFF26364B.toInt())
            )
        }
    }

    private fun changeBank(delta: Int) {
        bank = (bank + delta + bankCount) % bankCount
        selectedPad = 0
        refreshPads()
    }

    private fun importAudio(uri: Uri) {
        val p = selectedPad
        val k = key(bank, p)
        try {
            val displayName = "bank${bank}_pad${p}_${System.currentTimeMillis()}.audio"
            val destination = File(filesDir, displayName)
            contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Impossibile leggere il file selezionato" }
                FileOutputStream(destination).use { output -> input.copyTo(output) }
            }
            audioFiles[k] = destination.absolutePath
            padNames[k] = "SUONO ${p + 1}"
            savePad(k)
            refreshPads()
            statusLabel.text = "Audio caricato nel pad ${p + 1}"
        } catch (e: Exception) {
            Toast.makeText(this, "Errore importazione: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun playPad(pad: Int) {
        val k = key(bank, pad)
        val path = audioFiles[k]
        if (path == null || !File(path).exists()) {
            statusLabel.text = "Pad ${pad + 1}: tieni premuto per caricare un audio"
            return
        }
        try {
            players.remove(pad)?.let { old -> try { old.stop() } catch (_: Exception) {}; old.release() }
            val player = MediaPlayer()
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            player.setDataSource(path)
            player.setOnCompletionListener { mp ->
                players.remove(pad)
                mp.release()
            }
            player.setOnErrorListener { mp, _, _ ->
                players.remove(pad)
                mp.release()
                statusLabel.text = "Errore nella riproduzione"
                true
            }
            players[pad] = player
            player.prepare()
            player.start()
            statusLabel.text = "In riproduzione: ${padNames[k] ?: "PAD ${pad + 1}"}"
        } catch (e: Exception) {
            players.remove(pad)?.release()
            statusLabel.text = "Errore audio: ${e.message}"
        }
    }

    private fun stopAll() {
        players.values.toList().forEach {
            try { if (it.isPlaying) it.stop() } catch (_: Exception) {}
            it.release()
        }
        players.clear()
        statusLabel.text = "Riproduzione arrestata"
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_PAGE_DOWN -> { changeBank(1); return true }
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_PAGE_UP -> { changeBank(-1); return true }
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_SPACE -> { playPad(selectedPad); return true }
            KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_BACK -> { stopAll(); return true }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        stopAll()
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
