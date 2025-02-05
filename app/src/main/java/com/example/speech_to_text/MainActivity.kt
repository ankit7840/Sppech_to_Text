package com.example.speech_to_text

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var tvResult: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button

    private val requestCodeAudioPermission = 1
    private var isListening = false
    private var isUserStopped = false
    private val fullSpeechText = StringBuilder() // Stores complete sentence

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        tvResult = findViewById(R.id.tvResult)

        btnStart.isEnabled = true
        btnStop.isEnabled = false

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

        // Initialize Speech Recognizer
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        } else {
            Toast.makeText(
                this,
                "Speech recognition is not available on this device.",
                Toast.LENGTH_LONG
            ).show()
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
                Log.d("SpeechRecognizer", "Speech has ended")
//                if (!isUserStopped) restartListening() // Continue after speech stops
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
//                tvResult.text = "Error: $errorMessage"
                Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_SHORT).show()

                // If there's an error, stop listening and finalize the sentence
                    stopListening()
//                  restartListening()

            }

            // onPartialResults() is called continuously while the user is speaking
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val currentText = matches[0].trim()

                    // Show real-time text as the user speaks
                    val temporaryText = fullSpeechText.toString() + " " + currentText
                    tvResult.text = temporaryText.trim()

                    Log.d("SpeechRecognizer", "Partial Result: $currentText")
                }
            }

            // onResults() stores the final result after speech pauses
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val finalText = matches[0].trim()

                    // Append the recognized speech to the full text
                    fullSpeechText.append(" ").append(finalText)
//                    tvResult.text = fullSpeechText.toString().trim() // Display the full sentence

                    Log.d("SpeechRecognizer", "Final Result: $finalText")
                }

                // Restart recognition for continuous speech
//                if (!isUserStopped) restartListening()
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
                isUserStopped = false
                fullSpeechText.clear() // Reset previous text
                startListening()
            } else {
                Toast.makeText(this, "Permission not granted!", Toast.LENGTH_SHORT).show()
            }
        }

        // Stop Button
        btnStop.setOnClickListener {
            stopListening()
        }
    }

    private fun startListening() {
        if (!isListening) {

            tvResult.text=""
            isUserStopped=false
            btnStart.isEnabled = false
            btnStop.isEnabled = true

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)

                // Increased silence timeout to handle longer pauses
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 10000)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 10000)
            }

            speechRecognizer.startListening(intent)
            isListening = true
            Log.d("SpeechRecognizer", "Started listening...")
        }
    }

    private fun stopListening() {
        if (isListening) {
            isUserStopped = true // Prevent auto-restart

            // Cancel ongoing recognition
            speechRecognizer.cancel()
            speechRecognizer.stopListening()
            isListening = false


            tvResult.text = fullSpeechText.toString().trim()
            btnStart.isEnabled = true
            btnStop.isEnabled = false

            Log.d("SpeechRecognizer", "Stopped listening")
        }
    }

//    private fun restartListening() {
//        if (!isUserStopped) {
//            Handler(Looper.getMainLooper()).postDelayed({
//                startListening()
//            },500) // Restart after 1.5 sec
//        }
//    }

    override fun onDestroy() {
        super.onDestroy()
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
            Log.d("SpeechRecognizer", "Speech recognizer destroyed.")
        }
    }
}


