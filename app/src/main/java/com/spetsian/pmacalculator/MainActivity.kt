package com.spetsian.pmacalculator

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.spetsian.pmacalculator.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("calc_text", binding.calcText.text.toString())
        outState.putString("first_history", binding.firstHistory?.text?.toString())
        outState.putString("second_history", binding.secondHistory?.text?.toString())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        savedInstanceState.getString("calc_text")?.let {
            binding.calcText.text = it
        }
        savedInstanceState.getString("first_history")?.let {
            binding.firstHistory?.text = it
        }
        savedInstanceState.getString("second_history")?.let {
            binding.secondHistory?.text = it
        }
    }
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val numbers: String = "0123456789"
        val operations: String = "+-×÷"
        var decimalFlag: Boolean = false
        var bracketFlag: Boolean = false

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        with(binding){

            calcText.text = ""
        }

        fun addNegative () {
            if(binding.calcText.text.length > 0) {
                var tempText = binding.calcText.text
                val lastNumber = tempText.takeLastWhile { it.isDigit() }
                if (lastNumber.length > 0) {
                    bracketFlag = true
                    if (lastNumber.length == tempText.length) {
                        binding.calcText.text = buildString {
                            append("(-")
                            append(lastNumber)
                            append(')')
                        }

                    } else {
                        tempText = buildString {
                            append(tempText)
                            delete((tempText.length - lastNumber.length), tempText.length)
                        }
                        if (tempText.last() == '-') {
                            binding.calcText.text = buildString {
                                append(tempText)
                                delete((tempText.length - 1), tempText.length)
                                append("+")
                                append(lastNumber)
                            }
                        } else {
                            binding.calcText.text = buildString {
                                append(tempText)
                                append("(-")
                                append(lastNumber)
                                append(')')

                            }
                        }
                    }
                } else if (tempText.last() == ')') {
                    bracketFlag = false
                    tempText = buildString {
                        append(tempText)
                        delete(tempText.length - 1, tempText.length)
                    }
                    val tempLastNumber = tempText.takeLastWhile { it.isDigit() }
                    tempText = buildString {
                        append(tempText)
                        delete((tempText.length - tempLastNumber.length - 2), tempText.length)
                    }
                    binding.calcText.text = buildString {
                        append(tempText)
                        append(tempLastNumber)
                    }
                }
            }
        }

        fun addSymbol(number: Char) {
            if(bracketFlag != true) {
                binding.calcText.text = buildString {
                    append(binding.calcText.text)
                    append(number)
                }
            }

        }
        fun deleteSymbol() {
            with(binding) {
                if(calcText.text.length>0) {
                    if(calcText.text.last() =='.') decimalFlag = false
                    if(calcText.text.last() ==')') addNegative()
                    else {
                        calcText.text = buildString {
                            append(calcText.text)
                            delete((calcText.text.length - 1), calcText.text.length)
                        }
                    }
                }
            }
        }
        fun checkOperation(operation: Char) {
            if(binding.calcText.text.length > 0) {
                val temp: Char = binding.calcText.text[binding.calcText.text.length - 1]
                decimalFlag = false
                bracketFlag = false

                if (temp in numbers) {
                    addSymbol(operation)
                } else if (temp == '%' && operation != '%' || temp == ')') addSymbol(operation)
                else if (operation == '-' && temp in "×÷") {
                    addSymbol(operation)
                } else if (binding.calcText.text[binding.calcText.text.length - 2] in "×÷") {
                    deleteSymbol()
                    deleteSymbol()
                    addSymbol(operation)
                } else {
                    deleteSymbol()
                    addSymbol(operation)
                }

            }
        }
        fun addDecimal() {
            if(decimalFlag!=true && binding.calcText.text.length>0) {
                val temp: Char = binding.calcText.text[binding.calcText.text.length - 1]
                if (temp in numbers) {
                    addSymbol('.')
                    decimalFlag = true
                } else {
                    addSymbol('0')
                    addSymbol('.')
                    decimalFlag = true
                }
            }
        }

        fun parseExpression(expression: CharSequence): Pair<List<Double>, List<Char>> {
            val numbers = mutableListOf<Double>()
            val operators = mutableListOf<Char>()

            val text = expression.toString().replace(" ", "") // убираем пробелы

            if (text.isEmpty()) return Pair(numbers, operators)

            var i = 0
            val length = text.length

            while (i < length) {
                val currentChar = text[i]

                if (currentChar.isDigit() || currentChar == '.' ||
                    (currentChar == '-' && (i == 0 || text[i-1] in "+-*/" || text[i-1] == '('))) {

                    val numberStr = StringBuilder()

                    if (currentChar == '-') {
                        numberStr.append('-')
                        i++
                    }
                    while (i < length && (text[i].isDigit() || text[i] == '.')) {
                        numberStr.append(text[i])
                        i++
                    }
                    if (i < length && text[i] == ')') {
                        i++
                    }

                    try {
                        numbers.add(numberStr.toString().toDouble())
                    } catch (e: NumberFormatException) { }
                }

                // Обработка операторов
                else if (currentChar in setOf('+', '-', '×', '÷', '%')) {
                    if(currentChar == '%')
                    {
                        var temp = numbers.last()
                        numbers.removeAt(numbers.lastIndex)
                        temp *= 0.01
                        numbers.add(temp)
                        i++
                    }
                    else {
                        operators.add(currentChar)
                        i++
                    }
                }

                else if (currentChar == '(') {
                    if (i + 1 < length && text[i + 1] == '-') {
                        i++
                    } else {
                        i++
                    }
                }
                else if (currentChar == ')') {
                    i++
                }
                else {
                    i++
                }
            }

            return Pair(numbers, operators)
        }
        fun addResult() {
            val (numbers, operators) = parseExpression(binding.calcText.text)
            binding.firstHistory?.text = buildString { append(numbers) }
            binding.secondHistory?.text = buildString { append(operators) }
            if (numbers.size == operators.size + 1) {
                // Создаем копии списков для работы
                val tempNumbers = numbers.toMutableList()
                val tempOperators = operators.toMutableList()

                // 1. Сначала выполняем умножение и деление
                var i = 0
                while (i < tempOperators.size) {
                    when (tempOperators[i]) {
                        '×', '÷' -> {
                            val left = tempNumbers[i]
                            val right = tempNumbers[i + 1]
                            val result = if (tempOperators[i] == '×') {
                                left * right
                            } else {
                                if (right != 0.0) left / right else Double.NaN
                            }

                            // Заменяем левое число результатом и удаляем правое число и оператор
                            tempNumbers[i] = result
                            tempNumbers.removeAt(i + 1)
                            tempOperators.removeAt(i)
                            // i не увеличиваем, так как удалили элемент
                        }
                        else -> i++ // Пропускаем + и - на этом этапе
                    }
                }

                // 2. Затем выполняем сложение и вычитание
                var result = tempNumbers[0]
                for (j in tempOperators.indices) {
                    when (tempOperators[j]) {
                        '+' -> result += tempNumbers[j + 1]
                        '-' -> result -= tempNumbers[j + 1]
                    }
                }
                var res: Int = 0
                if(result > result.toInt()) {
                    binding.calcText.text = result.toString()

                }
                else {
                    res = result.toInt()
                    binding.calcText.text = res.toString()
                }

                decimalFlag = true
            } else {
                binding.calcText.text = "Ошибка"
            }
        }



        with(binding) {
            btn0.setOnClickListener {
                addSymbol('0')
            }
            btn1.setOnClickListener {
                addSymbol('1')
            }
            btn2.setOnClickListener {
                addSymbol('2')
            }
            btn3.setOnClickListener {
                addSymbol('3')
            }
            btn4.setOnClickListener {
                addSymbol('4')
            }
            btn5.setOnClickListener {
                addSymbol('5')
            }
            btn6.setOnClickListener {
                addSymbol('6')
            }
            btn7.setOnClickListener {
                addSymbol('7')
            }
            btn8.setOnClickListener {
                addSymbol('8')
            }
            btn9.setOnClickListener {
                addSymbol('9')
            }
            btnDelete.setOnClickListener {
                deleteSymbol()
            }
            btnNegative.setOnClickListener {
                addNegative()
            }

            btnPercent.setOnClickListener {
                checkOperation('%')
            }
            btnDivide.setOnClickListener {
                checkOperation('÷')
            }
            btnMultiply.setOnClickListener {
                checkOperation('×')
            }
            btnMinus.setOnClickListener {
                checkOperation('-')
            }
            btnPlus.setOnClickListener {
                checkOperation('+')
            }
            btnDecimal.setOnClickListener {
                addDecimal()
            }
            btnEquals.setOnClickListener {
                addResult()
            }

        }









//        val calcText: TextView = findViewById<TextView>(R.id.calcText)
//        calcText.text = "убубу"
//        val firstH: TextView = findViewById<TextView>(R.id.firstHistory)
//        firstH.text = "11111111"
//        val secH: TextView = findViewById<TextView>(R.id.secondHistory)
//        secH.text = "2222222222"




    }
}