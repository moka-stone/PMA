package com.spetsian.pmacalculator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.spetsian.pmacalculator.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var clipboardManager: ClipboardManager
    private lateinit var calculator: CalculatorLogic

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val (expr, decimal, bracket) = calculator.getState()
        outState.putString("calc_text", expr)
        outState.putBoolean("decimal_flag", decimal)
        outState.putBoolean("bracket_flag", bracket)
        outState.putString("first_history", binding.firstHistory?.text?.toString())
        outState.putString("second_history", binding.secondHistory?.text?.toString())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val expr = savedInstanceState.getString("calc_text") ?: ""
        val decimal = savedInstanceState.getBoolean("decimal_flag", false)
        val bracket = savedInstanceState.getBoolean("bracket_flag", false)
        calculator.setState(expr, decimal, bracket)
        updateDisplay()
        savedInstanceState.getString("first_history")?.let { binding.firstHistory?.text = it }
        savedInstanceState.getString("second_history")?.let { binding.secondHistory?.text = it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        calculator = CalculatorLogic()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.calcText.text = ""

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
        }
    }

    private fun updateDisplay() {
        binding.calcText.text = calculator.getDisplayText()
    }

    private fun onEquals() {
        when (val result = calculator.calculate()) {
            is CalculatorLogic.CalcResult.Success -> {
                binding.firstHistory?.text = result.historyLine1
                binding.secondHistory?.text = result.historyLine2
                updateDisplay()
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
}
