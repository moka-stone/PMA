package com.spetsian.pmacalculator.passkey

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.spetsian.pmacalculator.R
import com.spetsian.pmacalculator.databinding.ActivityPassKeyChangeBinding

/**
 * Смена PIN: старый PIN или биометрия, затем новый PIN дважды.
 */
class PassKeyChangeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPassKeyChangeBinding
    private val repo by lazy { PassKeyRepository(this) }
    private var oldVerifiedByBiometric = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPassKeyChangeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (canUseStrongBiometric()) {
            binding.btnOldBiometric.visibility = android.view.View.VISIBLE
        }

        binding.btnOldBiometric.setOnClickListener {
            showBiometricAuth(
                title = getString(R.string.passkey_biometric_title),
                subtitle = getString(R.string.passkey_biometric_subtitle_change),
                onSuccess = {
                    oldVerifiedByBiometric = true
                    binding.tilOldPin.isEnabled = false
                    binding.edtOldPin.setText("")
                    Toast.makeText(this, R.string.passkey_can_enter_new_pin, Toast.LENGTH_SHORT)
                        .show()
                }
            )
        }

        binding.btnSave.setOnClickListener { onSave() }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun onSave() {
        if (!oldVerifiedByBiometric) {
            val old = binding.edtOldPin.text?.toString().orEmpty()
            if (!PassKeyRepository.isValidPinFormat(old)) {
                Toast.makeText(this, R.string.passkey_invalid_pin, Toast.LENGTH_SHORT).show()
                return
            }
            if (!repo.verifyPin(old)) {
                Toast.makeText(this, R.string.passkey_wrong_pin, Toast.LENGTH_SHORT).show()
                return
            }
        }

        val newPin = binding.edtNewPin.text?.toString().orEmpty()
        val confirm = binding.edtConfirm.text?.toString().orEmpty()
        if (!PassKeyRepository.isValidPinFormat(newPin)) {
            Toast.makeText(this, R.string.passkey_invalid_pin, Toast.LENGTH_SHORT).show()
            return
        }
        if (newPin != confirm) {
            Toast.makeText(this, R.string.passkey_pins_mismatch, Toast.LENGTH_SHORT).show()
            return
        }

        repo.updatePin(newPin)
        Toast.makeText(this, R.string.passkey_pin_changed, Toast.LENGTH_SHORT).show()
        finish()
    }
}
