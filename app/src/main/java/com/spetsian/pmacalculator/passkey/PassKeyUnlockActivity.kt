package com.spetsian.pmacalculator.passkey

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.spetsian.pmacalculator.MainActivity
import com.spetsian.pmacalculator.R
import com.spetsian.pmacalculator.databinding.ActivityPassKeyUnlockBinding

class PassKeyUnlockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPassKeyUnlockBinding
    private val repo by lazy { PassKeyRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPassKeyUnlockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    finishAffinity()
                }
            }
        )

        val showBio = repo.isBiometricUnlockEnabled() && canUseStrongBiometric()
        binding.btnBiometric.visibility =
            if (showBio) android.view.View.VISIBLE else android.view.View.GONE

        binding.tvForgot.visibility =
            if (canUseStrongBiometric()) android.view.View.VISIBLE else android.view.View.GONE

        binding.btnUnlock.setOnClickListener { tryUnlockWithPin() }
        binding.btnBiometric.setOnClickListener { tryUnlockWithBiometric() }
        binding.tvForgot.setOnClickListener { onForgotClick() }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun tryUnlockWithPin() {
        val pin = binding.edtPin.text?.toString().orEmpty()
        if (!PassKeyRepository.isValidPinFormat(pin)) {
            Toast.makeText(this, R.string.passkey_invalid_pin, Toast.LENGTH_SHORT).show()
            return
        }
        if (repo.verifyPin(pin)) {
            goToMain()
        } else {
            Toast.makeText(this, R.string.passkey_wrong_pin, Toast.LENGTH_SHORT).show()
        }
    }

    private fun tryUnlockWithBiometric() {
        showBiometricAuth(
            title = getString(R.string.passkey_biometric_title),
            subtitle = getString(R.string.passkey_biometric_subtitle_unlock),
            onSuccess = { goToMain() }
        )
    }

    private fun onForgotClick() {
        if (!canUseStrongBiometric()) {
            Toast.makeText(this, R.string.passkey_reset_need_biometric, Toast.LENGTH_LONG).show()
            return
        }
        showBiometricAuth(
            title = getString(R.string.passkey_biometric_title),
            subtitle = getString(R.string.passkey_biometric_subtitle_reset),
            onSuccess = {
                repo.clearPin()
                startActivity(
                    Intent(this, PassKeySetupActivity::class.java).apply {
                        putExtra(PassKeySetupActivity.EXTRA_IS_RESET, true)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                )
                finish()
            }
        )
    }
}
