package com.spetsian.pmacalculator.ui.history

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.spetsian.pmacalculator.R
import com.spetsian.pmacalculator.data.HistoryRepository
import com.spetsian.pmacalculator.databinding.ActivityHistoryBinding
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private val repository = HistoryRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val adapter = HistoryAdapter { item ->
            setResult(RESULT_OK, Intent().putExtra(EXTRA_SELECTED_EXPRESSION, item.expression))
            finish()
        }
        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = adapter

        loadHistory(adapter)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun loadHistory(adapter: HistoryAdapter) {
        binding.progressHistory.visibility = View.VISIBLE
        binding.rvHistory.visibility = View.GONE
        binding.tvEmptyHistory.visibility = View.GONE

        lifecycleScope.launch {
            val result = repository.loadHistory(DEFAULT_LIMIT)
            binding.progressHistory.visibility = View.GONE

            result.onSuccess { list ->
                if (list.isEmpty()) {
                    binding.tvEmptyHistory.visibility = View.VISIBLE
                    binding.tvEmptyHistory.setText(R.string.history_empty)
                    binding.rvHistory.visibility = View.GONE
                } else {
                    adapter.submitList(list)
                    binding.rvHistory.visibility = View.VISIBLE
                    binding.tvEmptyHistory.visibility = View.GONE
                }
            }.onFailure {
                binding.tvEmptyHistory.visibility = View.VISIBLE
                binding.tvEmptyHistory.setText(R.string.history_load_error)
                Toast.makeText(
                    this@HistoryActivity,
                    R.string.history_load_error,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    companion object {
        const val EXTRA_SELECTED_EXPRESSION = "extra_selected_expression"
        private const val DEFAULT_LIMIT = 200L

        fun createIntent(context: Context): Intent =
            Intent(context, HistoryActivity::class.java)
    }
}
