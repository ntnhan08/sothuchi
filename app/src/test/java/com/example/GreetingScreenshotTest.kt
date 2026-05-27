package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FinanceUiState
import com.example.data.model.BudgetConfig
import com.example.data.model.Transaction
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        Scaffold { innerPadding ->
          Column(modifier = Modifier.padding(innerPadding).padding(horizontal = 16.dp)) {
            HeaderSection()
            LuxurySummaryCard(
              uiState = FinanceUiState(
                transactions = listOf(
                  Transaction(id = 1, title = "Ăn trưa", amount = 35000.0, category = "Ăn uống"),
                  Transaction(id = 2, title = "Đổ xăng", amount = 50000.0, category = "Di chuyển")
                ),
                budgetConfig = BudgetConfig(currentFunds = 500000.0),
                totalExpenses = 85000.0,
                remainingBalance = 415000.0
              ),
              onEditBudgetClicked = {}
            )
            BudgetTrackerVisuals(
              uiState = FinanceUiState(
                transactions = emptyList(),
                budgetConfig = BudgetConfig(currentFunds = 500000.0),
                totalExpenses = 85000.0,
                remainingBalance = 415000.0
              )
            )
          }
        }
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
