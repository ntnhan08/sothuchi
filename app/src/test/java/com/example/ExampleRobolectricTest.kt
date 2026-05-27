package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Sổ Thu Chi", appName)
  }

  @Test
  fun testBudgetApp_BudgetFlow() {
    composeTestRule.setContent {
      MyApplicationTheme {
        BudgetApp()
      }
    }

    // Verify initial clean state with Empty State
    composeTestRule.onNodeWithText("TÀI SẢN").assertExists()

    // Trigger setting budget
    composeTestRule.onNodeWithTag("budget_setting_card_button").performClick()

    // Input some budget text
    composeTestRule.onNodeWithTag("budget_amount_input").performTextInput("15000000")

    // Confirm the dialog
    composeTestRule.onNodeWithTag("confirm_budget_button").performClick()

    // Wait until the remaining balance displays matching value (15.000.000 ₫)
    composeTestRule.waitUntil(5000) {
      try {
        composeTestRule.onNodeWithText("15.000.000 ₫").assertExists()
        true
      } catch (e: AssertionError) {
        false
      }
    }
  }

  @Test
  fun testBudgetApp_AddTransactionFlow() {
    composeTestRule.setContent {
      MyApplicationTheme {
        BudgetApp()
      }
    }

    // Open add transaction modal
    composeTestRule.onNodeWithTag("add_transaction_fab").performClick()

    // Enter name
    composeTestRule.onNodeWithTag("transaction_title_input").performTextInput("Mua đồ gia dụng")

    // Enter price
    composeTestRule.onNodeWithTag("transaction_amount_input").performTextInput("230000")

    // Confirm it
    composeTestRule.onNodeWithTag("confirm_transaction_button").performClick()
  }
}
