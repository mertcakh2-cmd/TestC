package com.ubuntu.terminal

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when {
            intent.getBooleanExtra("open_terminal", false) -> {
                openTerminal()
            }

            intent.getBooleanExtra("open_settings", false) -> {
                openWelcome(settings = true)
            }

            else -> {
                openWelcome()
            }
        }

        finish()
    }

    private fun openWelcome(settings: Boolean = false) {
        val intent = Intent(this, WelcomeScreen::class.java)
        intent.putExtra("open_settings", settings)
        startActivity(intent)
    }

    private fun openTerminal() {
        startActivity(Intent(this, MakeTerminal::class.java))
    }

    companion object {
        fun launchTerminal(activity: Activity) {
            activity.startActivity(
                Intent(activity, MainActivity::class.java).apply {
                    putExtra("open_terminal", true)
                }
            )
        }

        fun launchSettings(activity: Activity) {
            activity.startActivity(
                Intent(activity, MainActivity::class.java).apply {
                    putExtra("open_settings", true)
                }
            )
        }
    }
}
