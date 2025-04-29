package com.example.agrohive_1

import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private var progress = 0
    private val frameImages = listOf(
        R.drawable.frame1,
        R.drawable.frame2,
        R.drawable.frame3,
        R.drawable.frame4
    )
    private var currentFrameIndex = 0
    private lateinit var progressBarLine: View
    private lateinit var progressText: TextView
    private lateinit var logo: ImageView
    private lateinit var imageView: ImageView
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var auth: FirebaseAuth

    private var networkSpeedFactor = 50

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        Log.d("SplashActivity", "onCreate: Splash started")

        auth = FirebaseAuth.getInstance()
        connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        logo = findViewById(R.id.logo)
        progressBarLine = findViewById(R.id.progressBarLine)
        progressText = findViewById(R.id.progressText)
        imageView = findViewById(R.id.imageView)

        logo.visibility = View.VISIBLE
        progressBarLine.visibility = View.VISIBLE
        progressText.visibility = View.VISIBLE
        imageView.visibility = View.VISIBLE

        startFrameAnimation()
        checkNetworkAndAdjustSpeed()
        startProgressBar()
    }

    private fun checkNetworkAndAdjustSpeed() {
        val currentNetwork = connectivityManager.activeNetwork
        networkSpeedFactor = if (currentNetwork == null) 50 else 30

        connectivityManager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                networkSpeedFactor = 30
            }
            override fun onLost(network: Network) {
                networkSpeedFactor = 50
            }
        })
    }

    private fun startProgressBar() {
        val maxWidth = resources.displayMetrics.widthPixels - 100

        val updateProgress = object : Runnable {
            override fun run() {
                if (progress <= 100) {
                    val newWidth = (maxWidth * progress) / 100
                    val params = progressBarLine.layoutParams
                    params.width = newWidth
                    progressBarLine.layoutParams = params
                    progressText.text = getString(R.string.progress_percentage, progress)
                    progress++

                    if (progress <= 100) {
                        progressBarLine.postDelayed(this, networkSpeedFactor.toLong())
                    } else {
                        val user = auth.currentUser
                        Log.d("SplashActivity", "Progress complete, user: $user")
                        val intent = if (user != null) {
                            Intent(this@SplashActivity, HomepageActivity::class.java)
                        } else {
                            Intent(this@SplashActivity, AuthActivity::class.java)
                        }
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        Log.d("SplashActivity", "Starting intent: $intent")
                        startActivity(intent)
                        finish()
                    }
                }
            }
        }
        progressBarLine.post(updateProgress)
    }

    private fun startFrameAnimation() {
        val frameUpdateTask = object : Runnable {
            override fun run() {
                imageView.setImageResource(frameImages[currentFrameIndex])
                currentFrameIndex = (currentFrameIndex + 1) % frameImages.size
                imageView.postDelayed(this, 1000)
            }
        }
        imageView.post(frameUpdateTask)
    }
}