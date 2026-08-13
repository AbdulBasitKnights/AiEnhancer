package com.aiface.aging.features.noti

import android.app.KeyguardManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.aiface.aging.R
import com.aiface.aging.SplashActivity
import com.aiface.aging.databinding.ActivityLockScreenBinding
import com.aiface.aging.utils.FirebaseLogUtils
import java.util.Locale

class LockScreenActivity : AppCompatActivity() {

    private var binding: ActivityLockScreenBinding? = null


    private val handler = Handler(Looper.getMainLooper())
    private val runnable = object : Runnable {
        override fun run() {
          checkUnlock()
            Log.d("xxxtimer", "run")
            handler.postDelayed(this, 1000)
        }
    }

    fun startRepeating() {
        handler.post(runnable)
    }

    fun checkUnlock() {
        val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        if (!keyguardManager.isKeyguardLocked){

                val intent = Intent(this@LockScreenActivity, SplashActivity::class.java)
                startActivity(intent)

        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLockScreenBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        // ✅ These flags will show Activity even if device is locked
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
      //  window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

      //  handleUnlock()

        FirebaseLogUtils.logEvent(
            "Notification lock is shown on the lock screen",
            "Lockscreen noti"
        )

        binding?.touchToUnlock?.setOnClickListener {

            FirebaseLogUtils.logEvent(
                "User clicks the lock screen notification",
                "Lockscreen noti"
            )
            requestUnlock()
        }

        binding?.btnTry?.setOnClickListener {
                val intent = Intent(this@LockScreenActivity, SplashActivity::class.java)
                startActivity(intent)

            FirebaseLogUtils.logEvent(
                "User opens the home screen from the lock screen notification",
                "Lockscreen noti"
            )
        }


        startRepeating()

        Handler(Looper.getMainLooper()).postDelayed({
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }, 10_000)










    }



    @RequiresApi(Build.VERSION_CODES.O)
    private fun requestUnlock() {
        val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager

        if (keyguardManager.isKeyguardLocked) {
            keyguardManager.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() {
                    super.onDismissSucceeded()


                }
            })
        } else {
            // Already unlocked → Go to home directly
                val intent = Intent(this@LockScreenActivity, SplashActivity::class.java)
                startActivity(intent)

        }
       // finish()
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleUnlock() {
        val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager

        if (keyguardManager.isKeyguardLocked) {
            // 🔥 This will show PIN/Pattern/Face Unlock → after unlock → onDismissSucceeded will be called
            keyguardManager.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() {
                    super.onDismissSucceeded()

                        val intent = Intent(this@LockScreenActivity, SplashActivity::class.java)
                        startActivity(intent)

                    // ✅ Finish LockScreenActivity
                  //  finish()
                }
            })
        }
    }



    override fun onDestroy() {
        super.onDestroy()
        Log.d("xxxtimer", "stop")
        handler.removeCallbacks(runnable)
    }

}