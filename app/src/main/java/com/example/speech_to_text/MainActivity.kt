package com.example.speech_to_text

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
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
    private val fullSpeechText = StringBuilder() // Stores store dynamic as well as final   sentence

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

        // Initialize Speech recognisher is available it will create a instance of speech recogniser  otherwise toast will show
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
            }

            override fun onBeginningOfSpeech() {
            }

            override fun onRmsChanged(rmsdB: Float) {
            }

            override fun onBufferReceived(buffer: ByteArray?) {
            }

            override fun onEndOfSpeech() {
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
                // if any error is cause we will get onError callback and toast nmessage while be showned
                //ignore client error because it is called when when we cliked stop button
                Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_SHORT).show()
                stopListening()
            }


            // onPartialResults() is called continuously while the user is speaking and updating the dynamic text
            private var lastRecognizedText = ""
            override fun onPartialResults(partialResults: Bundle?) {
                //taking only last String  from  list<string> partial_results
                val matches =
                    partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

                if (!matches.isNullOrEmpty()) {
                    val latestText = matches.last().trim()

                    // Extract only new words by removing overlapping parts
                    val newWords = latestText.removePrefix(lastRecognizedText).trim()

                    if (newWords.isNotEmpty()) {
                        fullSpeechText.append(" ").append(newWords) // Append only new words
                    }

                    lastRecognizedText = latestText // Store last full recognized phrase
                    tvResult.text = fullSpeechText.toString().trim()

                }
            }

            // onResults() stores the final result after speech pauses in this case we donot need it
            override fun onResults(results: Bundle?) {
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
                //toast meesage to show permisson not granted
                Toast.makeText(this, "Permission not granted!", Toast.LENGTH_SHORT).show()
            }
        }

        // Stop Button
        btnStop.setOnClickListener {
            stopListening()
        }
    }


    //auxilary functions
    private fun startListening() {
        if (!isListening) {
            tvResult.text = ""
            isUserStopped = false
            btnStart.isEnabled = false
            btnStop.isEnabled = true

            // below code is to add the additional parameters to the intent
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)

                // Increased silence timeout to handle longer pauses
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 10000)
                putExtra(
                    RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                    10000
                )
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


