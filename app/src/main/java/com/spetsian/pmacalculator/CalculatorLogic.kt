package com.spetsian.pmacalculator

class CalculatorLogic {

    private val expression = StringBuilder()
    private var decimalFlag = false
    private var bracketFlag = false

    private val numbers: CharSequence get() = "0123456789"

    fun getDisplayText(): CharSequence = expression

    fun setState(expr: String, decimal: Boolean, bracket: Boolean) {
        expression.clear()
        expression.append(expr)
        decimalFlag = decimal
        bracketFlag = bracket
    }

    fun getState(): Triple<String, Boolean, Boolean> =
        Triple(expression.toString(), decimalFlag, bracketFlag)

    fun addDigit(digit: Char) {
        if (bracketFlag) return
        expression.append(digit)
    }

    fun addOperation(operation: Char) {
        if (expression.isEmpty()) return
        val temp = expression.last()
        decimalFlag = false
        bracketFlag = false

        when {
            temp in numbers -> expression.append(operation)
            (temp == '%' && operation != '%') || temp == ')' -> expression.append(operation)
            operation == '-' && temp in "×÷" -> expression.append(operation)
            expression.length >= 2 && expression[expression.length - 2] in "×÷" -> {
                expression.deleteCharAt(expression.length - 1)
                expression.deleteCharAt(expression.length - 1)
                expression.append(operation)
            }
            else -> {
                expression.deleteCharAt(expression.length - 1)
                expression.append(operation)
            }
        }
    }

    fun deleteLast() {
        if (expression.isEmpty()) return
        if (expression.last() == '.') decimalFlag = false
        if (expression.last() == ')') {
            toggleNegative()
            return
        }
        expression.deleteCharAt(expression.length - 1)
    }

    fun toggleNegative() {
        if (expression.isEmpty()) return
        val tempText = expression.toString()
        val lastNumber = tempText.takeLastWhile { it.isDigit() || it == '.' }
        if (lastNumber.isNotEmpty()) {
            bracketFlag = true
            if (lastNumber.length == tempText.length) {
                expression.clear()
                expression.append("(-")
                expression.append(lastNumber)
                expression.append(')')
            } else {
                expression.delete(expression.length - lastNumber.length, expression.length)
                val newEnd = expression.toString()
                if (newEnd.last() == '-') {
                    expression.deleteCharAt(expression.length - 1)
                    expression.append('+')
                    expression.append(lastNumber)
                } else {
                    expression.append("(-")
                    expression.append(lastNumber)
                    expression.append(')')
                }
            }
        } else if (tempText.last() == ')') {
            bracketFlag = false
            expression.deleteCharAt(expression.length - 1)
            val tempLastNumber = expression.toString().takeLastWhile { it.isDigit() || it == '.' }
            expression.delete(expression.length - tempLastNumber.length - 2, expression.length)
            expression.append(tempLastNumber)
        }
    }

    fun addDecimal() {
        if (decimalFlag || expression.isEmpty()) return
        val temp = expression.last()
        if (temp in numbers) {
            expression.append('.')
            decimalFlag = true
        } else {
            expression.append('0')
            expression.append('.')
            decimalFlag = true
        }
    }

    fun clear() {
        expression.clear()
        decimalFlag = false
        bracketFlag = false
    }

    fun calculate(): CalcResult {
        val (nums, ops) = parseExpression(expression)
        val history1 = nums.toString()
        val history2 = ops.toString()

        if (nums.size != ops.size + 1) {
            return CalcResult.Error("Ошибка")
        }

        val tempNumbers = nums.toMutableList()
        val tempOperators = ops.toMutableList()

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
                    tempNumbers[i] = result
                    tempNumbers.removeAt(i + 1)
                    tempOperators.removeAt(i)
                }
                else -> i++
            }
        }

        var result = tempNumbers[0]
        for (j in tempOperators.indices) {
            when (tempOperators[j]) {
                '+' -> result += tempNumbers[j + 1]
                '-' -> result -= tempNumbers[j + 1]
            }
        }

        val displayText = if (result != result.toInt().toDouble()) {
            result.toString()
        } else {
            result.toInt().toString()
        }
        if (result != result.toInt().toDouble()) decimalFlag = true

        expression.clear()
        expression.append(displayText)

        return CalcResult.Success(displayText, history1, history2)
    }

    private fun parseExpression(expression: CharSequence): Pair<List<Double>, List<Char>> {
        val numbers = mutableListOf<Double>()
        val operators = mutableListOf<Char>()
        val text = expression.toString().replace(" ", "")
        if (text.isEmpty()) return Pair(numbers, operators)

        var i = 0
        val length = text.length

        while (i < length) {
            val currentChar = text[i]

            if (currentChar.isDigit() || currentChar == '.' || currentChar == 'E' ||
                (currentChar == '-' && (i == 0 || text[i - 1] in "+-*/×÷" || text[i - 1] == '('))
            ) {
                val numberStr = StringBuilder()
                if (currentChar == '-') {
                    numberStr.append('-')
                    i++
                }
                while (i < length && (text[i].isDigit() || text[i] == '.' || text[i] == 'E')) {
                    numberStr.append(text[i])
                    i++
                }
                if (i < length && text[i] == ')') i++
                try {
                    numbers.add(numberStr.toString().toDouble())
                } catch (_: NumberFormatException) { }
            } else if (currentChar in setOf('+', '-', '×', '÷', '%')) {
                if (currentChar == '%') {
                    if (numbers.isNotEmpty()) {
                        var temp = numbers.last()
                        numbers.removeAt(numbers.lastIndex)
                        temp *= 0.01
                        numbers.add(temp)
                    }
                    i++
                } else {
                    operators.add(currentChar)
                    i++
                }
            } else if (currentChar == '(') {
                i++
            } else if (currentChar == ')') {
                i++
            } else {
                i++
            }
        }

        return Pair(numbers, operators)
    }

    sealed class CalcResult {
        data class Success(
            val displayText: String,
            val historyLine1: String,
            val historyLine2: String
        ) : CalcResult()
        data class Error(val message: String) : CalcResult()
    }
}
