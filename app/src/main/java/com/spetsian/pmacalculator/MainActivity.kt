package com.spetsian.pmacalculator

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.spetsian.pmacalculator.databinding.ActivityMainBinding

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.spetsian.pmacalculator.data.HistoryRepository
import com.spetsian.pmacalculator.passkey.PassKeyChangeActivity
import com.spetsian.pmacalculator.passkey.PassKeyUnlockActivity
import com.spetsian.pmacalculator.ui.history.HistoryActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var clipboardManager: ClipboardManager
    private lateinit var vibrator: Vibrator
    private lateinit var calculator: CalculatorLogic
    private val historyRepository = HistoryRepository()

    private val openHistoryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val expr = result.data?.getStringExtra(HistoryActivity.EXTRA_SELECTED_EXPRESSION)
        if (!expr.isNullOrBlank()) {
            calculator.setExpression(expr)
            updateDisplay()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val (expr, decimal, bracket) = calculator.getState()
        outState.putString("calc_text", expr)
        outState.putBoolean("decimal_flag", decimal)
        outState.putBoolean("bracket_flag", bracket)
        outState.putString("second_history", binding.secondHistory?.text?.toString())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val expr = savedInstanceState.getString("calc_text") ?: ""
        val decimal = savedInstanceState.getBoolean("decimal_flag", false)
        val bracket = savedInstanceState.getBoolean("bracket_flag", false)
        calculator.setState(expr, decimal, bracket)
        updateDisplay()
        savedInstanceState.getString("second_history")?.let { binding.secondHistory?.text = it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        initFirebaseAuth()
        showLastHistoryExpression()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbarMain?.inflateMenu(R.menu.menu_main)
        binding.toolbarMain?.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_change_pass -> {
                    startActivity(Intent(this, PassKeyChangeActivity::class.java))
                    true
                }
                R.id.action_lock -> {
                    startActivity(Intent(this, PassKeyUnlockActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }

        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        calculator = CalculatorLogic()


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

//        binding.calcText.text = ""

        with(binding) {
            calcText.setOnLongClickListener {
                copyResult()
                true
            }
            btn0.setOnClickListener { calculator.addDigit('0'); updateDisplay() }
            btn1.setOnClickListener { calculator.addDigit('1'); updateDisplay() }
            btn2.setOnClickListener { calculator.addDigit('2'); updateDisplay() }
            btn3.setOnClickListener { calculator.addDigit('3'); updateDisplay() }
            btn4.setOnClickListener { calculator.addDigit('4'); updateDisplay() }
            btn5.setOnClickListener { calculator.addDigit('5'); updateDisplay() }
            btn6.setOnClickListener { calculator.addDigit('6'); updateDisplay() }
            btn7.setOnClickListener { calculator.addDigit('7'); updateDisplay() }
            btn8.setOnClickListener { calculator.addDigit('8'); updateDisplay() }
            btn9.setOnClickListener { calculator.addDigit('9'); updateDisplay() }
            btnDelete.setOnClickListener { calculator.deleteLast(); updateDisplay() }
            btnNegative.setOnClickListener { calculator.toggleNegative(); updateDisplay() }
            btnPercent.setOnClickListener { calculator.addOperation('%'); updateDisplay() }
            btnDivide.setOnClickListener { calculator.addOperation('÷'); updateDisplay() }
            btnMultiply.setOnClickListener { calculator.addOperation('×'); updateDisplay() }
            btnMinus.setOnClickListener { calculator.addOperation('-'); updateDisplay() }
            btnPlus.setOnClickListener { calculator.addOperation('+'); updateDisplay() }
            btnDecimal.setOnClickListener { calculator.addDecimal(); updateDisplay() }
            btnEquals.setOnClickListener { onEquals() }
            btnOtherOptions.setOnClickListener {
                openHistoryLauncher.launch(HistoryActivity.createIntent(this@MainActivity))
            }
        }
    }

    private fun updateDisplay() {
        binding.calcText.text = calculator.getDisplayText()
        vibrator.vibrate(100)
    }

    private fun onEquals() {
        val expressionBeforeEval = binding.calcText.text.toString()

        when (val result = calculator.calculate()) {
            is CalculatorLogic.CalcResult.Success -> {
                updateDisplay()

                saveHistory(expressionBeforeEval, result.displayText)
            }
            is CalculatorLogic.CalcResult.Error -> {
                binding.calcText.text = result.message
            }
        }
    }

    private fun copyResult() {
        val text = binding.calcText.text.toString()
        if (text.isNotEmpty()) {
            clipboardManager.setPrimaryClip(ClipData.newPlainText("calculator", text))
            Toast.makeText(this, "Скопировано", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Нечего копировать", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initFirebaseAuth() {
        lifecycleScope.launch {
            historyRepository.ensureSignedInAnonymously()
                .onFailure {
                    Toast.makeText(this@MainActivity, "Firebase auth error", Toast.LENGTH_SHORT).show()
                }
                .onSuccess {
                    showLastHistoryExpression()
                }
        }
    }

    private fun saveHistory(expression: String, result: String) {
        lifecycleScope.launch {
            historyRepository.saveHistory(expression, result)
                .onSuccess {
                    // В верхнем блоке всегда показываем последнее выражение из истории.
                    binding.secondHistory?.text = expression
                }
                .onFailure {
                    Toast.makeText(this@MainActivity, "Не удалось сохранить историю", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showLastHistoryExpression() {
        lifecycleScope.launch {
            historyRepository.loadHistory(1)
                .onSuccess { list ->
                    val latestExpression = list.firstOrNull()?.expression.orEmpty()
                    if (latestExpression.isNotBlank()) {
                        binding.secondHistory?.text = latestExpression
                    }
                }
        }
    }
}
