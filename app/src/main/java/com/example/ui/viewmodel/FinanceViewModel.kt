package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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
    val remainingBalance: Double = 0.0
)

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: FinanceRepository

    val uiState: StateFlow<FinanceUiState>

    init {
        val database = AppDatabase.getDatabase(application)
        val financeDao = database.financeDao()
        repository = FinanceRepository(financeDao)

        uiState = combine(
            repository.allTransactions,
            repository.budgetConfig
        ) { transactionList, config ->
            val totalExpenses = transactionList.filter { it.isExpense }.sumOf { it.amount }
            val currentFunds = config?.currentFunds ?: 0.0
            val remainingBalance = currentFunds - totalExpenses
            FinanceUiState(
                transactions = transactionList,
                budgetConfig = config ?: BudgetConfig(currentFunds = 0.0),
                totalExpenses = totalExpenses,
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
