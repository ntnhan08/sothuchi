package com.example.data.repository

import com.example.data.dao.FinanceDao
import com.example.data.model.BudgetConfig
import com.example.data.model.Transaction
import kotlinx.coroutines.flow.Flow

class FinanceRepository(private val financeDao: FinanceDao) {
    val allTransactions: Flow<List<Transaction>> = financeDao.getAllTransactions()
    val budgetConfig: Flow<BudgetConfig?> = financeDao.getBudgetConfig()

    suspend fun insertTransaction(transaction: Transaction) {
        financeDao.insertTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Int) {
        financeDao.deleteTransactionById(id)
    }

    suspend fun updateBudgetConfig(funds: Double) {
        financeDao.insertBudgetConfig(BudgetConfig(currentFunds = funds))
    }
}
