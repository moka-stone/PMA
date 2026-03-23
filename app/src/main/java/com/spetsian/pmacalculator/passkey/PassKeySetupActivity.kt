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
import com.spetsian.pmacalculator.databinding.ActivityPassKeySetupBinding

class PassKeySetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPassKeySetupBinding
    private val repo by lazy { PassKeyRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPassKeySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        val isReset = intent.getBooleanExtra(EXTRA_IS_RESET, false)
        if (isReset) {
            binding.tvSetupTitle.setText(R.string.passkey_setup_title_reset)
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    finishAffinity()
                }
            }
        )

        binding.switchBiometric.isEnabled = canUseStrongBiometric()
        if (!canUseStrongBiometric()) {
            binding.switchBiometric.isChecked = false
        }

        binding.btnSave.setOnClickListener { onSaveClick() }
    }

    private fun onSaveClick() {
        val pin = binding.edtPin.text?.toString().orEmpty()
        val confirm = binding.edtConfirm.text?.toString().orEmpty()

        if (!PassKeyRepository.isValidPinFormat(pin)) {
            Toast.makeText(this, R.string.passkey_invalid_pin, Toast.LENGTH_SHORT).show()
            return
        }
        if (pin != confirm) {
            Toast.makeText(this, R.string.passkey_pins_mismatch, Toast.LENGTH_SHORT).show()
            return
        }

        repo.savePin(pin)
        val allowBio = binding.switchBiometric.isChecked && canUseStrongBiometric()
        repo.setBiometricUnlockEnabled(allowBio)
        if (binding.switchBiometric.isChecked && !canUseStrongBiometric()) {
            Toast.makeText(this, R.string.passkey_bio_unavailable_toast, Toast.LENGTH_SHORT).show()
        }

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    companion object {
        const val EXTRA_IS_RESET = "extra_is_reset"
    }
}
