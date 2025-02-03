package com.example.speech_to_text
import android.R.attr.action
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Button
import android.widget.TextView
import android.Manifest
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale


class MainActivity : AppCompatActivity() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var tvResult: TextView
    private val requestCodeAudioPermission = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnStop = findViewById<Button>(R.id.btnStop)
        tvResult = findViewById(R.id.tvResult)

        // Check for RECORD_AUDIO permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                requestCodeAudioPermission
            )
        }

        // Check if Speech Recognition is available
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.d("SpeechRecognizer", "Speech recognition is available.")
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        } else {
            Log.d("SpeechRecognizer", "Speech recognition is NOT available.")
            Toast.makeText(this, "Speech recognition is not available on this device. Please check your system settings.", Toast.LENGTH_LONG).show()
            // Disable Start button
            btnStart.isEnabled = false
            return
        }

        // Set up RecognitionListener
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                tvResult.text = "Listening..."
                Log.d("SpeechRecognizer", "Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                Log.d("SpeechRecognizer", "Speech has begun")
            }

            override fun onRmsChanged(rmsdB: Float) {
                Log.d("SpeechRecognizer", "RMS changed: $rmsdB")
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                Log.d("SpeechRecognizer", "Buffer received")
            }

            override fun onEndOfSpeech() {
                tvResult.text = "Processing..."
                Log.d("SpeechRecognizer", "Speech has ended")
            }

            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer is busy"
                    else -> "Unknown error occurred"
                }
                Log.d("SpeechRecognizer", "Error: $errorMessage")
                tvResult.text = "Error: $errorMessage"
                Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_SHORT).show()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    tvResult.text = matches[0]
                    Log.d("SpeechRecognizer", "Results: ${matches[0]}")
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                Log.d("SpeechRecognizer", "Partial results received")
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                Log.d("SpeechRecognizer", "Event: $eventType")
            }
        })

        // Start Button
        btnStart.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
            ) {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now")

                }
                Log.d("SpeechRecognizer", "Starting speech recognition...")
                speechRecognizer.startListening(intent)
            } else {
                Toast.makeText(this, "Permission not granted!", Toast.LENGTH_SHORT).show()
            }
        }

        // Stop Button
        btnStop.setOnClickListener {
            Log.d("SpeechRecognizer", "Stopping speech recognition...")
            speechRecognizer.stopListening()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestCodeAudioPermission && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("SpeechRecognizer", "Permission granted.")
        } else {
            Log.d("SpeechRecognizer", "Permission denied.")
            Toast.makeText(this, "Microphone permission is required for speech recognition.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
            Log.d("SpeechRecognizer", "Speech recognizer destroyed.")
        }
    }
}
