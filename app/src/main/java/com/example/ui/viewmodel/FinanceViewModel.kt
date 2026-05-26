package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.BudgetConfig
import com.example.data.model.Transaction
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FinanceUiState(
    val transactions: List<Transaction> = emptyList(),
    val budgetConfig: BudgetConfig = BudgetConfig(currentFunds = 0.0),
    val totalExpenses: Double = 0.0,
    val totalFunds: Double = 0.0,
    val remainingBalance: Double = 0.0
)

class FinanceViewModel(context: Context) : ViewModel() {
    private val repository: FinanceRepository

    val uiState: StateFlow<FinanceUiState>

    init {
        val database = AppDatabase.getDatabase(context)
        val financeDao = database.financeDao()
        repository = FinanceRepository(financeDao)

        uiState = combine(
            repository.allTransactions,
            repository.budgetConfig
        ) { transactionList, config ->
            val totalExpenses = transactionList.filter { it.isExpense }.sumOf { it.amount }
            val totalIncome = transactionList.filter { !it.isExpense }.sumOf { it.amount }
            val baseFunds = config?.currentFunds ?: 0.0
            val totalFunds = baseFunds + totalIncome
            val remainingBalance = totalFunds - totalExpenses
            FinanceUiState(
                transactions = transactionList,
                budgetConfig = config ?: BudgetConfig(currentFunds = baseFunds),
                totalExpenses = totalExpenses,
                totalFunds = totalFunds,
                remainingBalance = remainingBalance
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FinanceUiState()
        )
    }

    fun updateInitialBudget(amount: Double) {
        viewModelScope.launch {
            repository.updateBudgetConfig(amount)
        }
    }

    fun addTransaction(title: String, amount: Double, category: String, isExpense: Boolean = true) {
        viewModelScope.launch {
            val transaction = Transaction(
                title = title.trim(),
                amount = amount,
                category = category,
                isExpense = isExpense
            )
            repository.insertTransaction(transaction)
        }
    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            repository.deleteTransactionById(id)
        }
    }
}

class FinanceViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FinanceViewModel(context.applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
