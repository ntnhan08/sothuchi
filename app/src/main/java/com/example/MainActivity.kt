package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Transaction
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceUiState
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.FinanceViewModelFactory
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                BudgetApp()
            }
        }
    }
}

// Financial categories metadata
val categories = listOf(
    CategoryUi("Ăn uống", Icons.Default.Restaurant, Color(0xFFF28B82)),
    CategoryUi("Di chuyển", Icons.Default.DirectionsCar, Color(0xFFAECBFA)),
    CategoryUi("Mua sắm", Icons.Default.ShoppingCart, Color(0xFF81C995)),
    CategoryUi("Nhà cửa", Icons.Default.Home, Color(0xFFFDE293)),
    CategoryUi("Giải trí", Icons.Default.SportsEsports, Color(0xFFD7AEEF)),
    CategoryUi("Sức khỏe", Icons.Default.MedicalServices, Color(0xFF80CBC4)),
    CategoryUi("Học tập", Icons.Default.School, Color(0xFFFFCC80)),
    CategoryUi("Khác", Icons.Default.Category, Color(0xFFE8EAED))
)

data class CategoryUi(
    val name: String,
    val icon: ImageVector,
    val color: Color
)

// Currency helpers
val vndFormatSymbols = java.text.DecimalFormatSymbols(Locale.US).apply {
    groupingSeparator = '.'
}
val vndFormatter = DecimalFormat("#,###", vndFormatSymbols)

fun formatVnd(amount: Double): String {
    return try {
        synchronized(vndFormatter) {
            vndFormatter.format(amount) + " ₫"
        }
    } catch (e: Exception) {
        "0 ₫"
    }
}

fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return formatter.format(date)
}

@Composable
fun BudgetApp() {
    val context = LocalContext.current.applicationContext
    val viewModel: FinanceViewModel = viewModel(
        factory = FinanceViewModelFactory(context)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf("Tất cả") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MatteGold,
                contentColor = RichBlack,
                modifier = Modifier.testTag("add_transaction_fab").padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Thêm Ghi Chép",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
        ) {
            item {
                HeaderSection()
            }

                item {
                    LuxurySummaryCard(
                        uiState = uiState,
                        onEditBudgetClicked = { showBudgetDialog = true }
                    )
                }

                item {
                    BudgetTrackerVisuals(uiState = uiState)
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Giao Dịch Gần Đây",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                item {
                    FilterChipsSection(
                        selectedFilter = selectedCategoryFilter,
                        onFilterSelected = { selectedCategoryFilter = it }
                    )
                }

                val filteredTransactions = if (selectedCategoryFilter == "Tất cả") {
                    uiState.transactions
                } else {
                    uiState.transactions.filter { it.category == selectedCategoryFilter }
                }

                if (filteredTransactions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = "Empty",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                    modifier = Modifier.size(80.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Chưa có giao dịch",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(
                        items = filteredTransactions,
                        key = { _, tx -> tx.id }
                    ) { _, tx ->
                        TransactionRowItem(
                            transaction = tx,
                            onDelete = { viewModel.deleteTransaction(tx.id) }
                        )
                    }
                }
            }
        }

    if (showBudgetDialog) {
        SetBudgetDialog(
            currentBudget = uiState.budgetConfig.currentFunds,
            onDismiss = { showBudgetDialog = false },
            onConfirm = { amount ->
                viewModel.updateInitialBudget(amount)
                showBudgetDialog = false
            }
        )
    }

    if (showAddDialog) {
        AddTransactionDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, amount, category, isExpense ->
                viewModel.addTransaction(title, amount, category, isExpense)
                showAddDialog = false
            }
        )
    }
}



@Composable
fun HeaderSection() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "TÀI SẢN",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                color = MatteGold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Quản lý chi tiêu",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = CharcoalGray,
            border = BorderStroke(1.dp, MatteGold.copy(alpha = 0.3f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Diamond,
                    contentDescription = "Premium",
                    tint = MatteGold,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun LuxurySummaryCard(
    uiState: FinanceUiState,
    onEditBudgetClicked: () -> Unit
) {
    val brush = Brush.linearGradient(
        colors = listOf(
            CharcoalGray,
            RichBlack,
            CharcoalGray
        ),
        start = Offset(0f, 0f),
        end = Offset(1000f, 1000f)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(brush)
            .clickable(onClick = onEditBudgetClicked)
            .testTag("budget_setting_card_button")
            .border(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(MatteGold.copy(alpha = 0.5f), Color.Transparent, MatteGold.copy(alpha = 0.2f))
                ),
                RoundedCornerShape(32.dp)
            ),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TỔNG SỐ DƯ",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MatteGold,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = formatVnd(uiState.remainingBalance),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Black
                ),
                color = if (uiState.remainingBalance >= 0) MaterialTheme.colorScheme.onBackground else Color(0xFFEF4444)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = Color(0xFF81C995),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "THU NHẬP",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatVnd(uiState.totalFunds),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF81C995)
                    )
                }

                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                        .align(Alignment.CenterVertically)
                )

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "CHI TIÊU",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            tint = Color(0xFFF28B82),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatVnd(uiState.totalExpenses),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFF28B82)
                    )
                }
            }
        }
    }
}

// No-op for removed composable

@Composable
fun BudgetTrackerVisuals(uiState: FinanceUiState) {
    if (uiState.budgetConfig.currentFunds <= 0) return

    val safePct = if (uiState.budgetConfig.currentFunds > 0.0 && uiState.budgetConfig.currentFunds.isFinite()) {
        (uiState.totalExpenses / uiState.budgetConfig.currentFunds).coerceIn(0.0, 1.0)
    } else {
        0.0
    }
    
    val pct = if (safePct.isNaN()) 0.0 else safePct
    val animatedPct by animateFloatAsState(
        targetValue = pct.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessVeryLow
        ),
        label = "budget_progress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalGray.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Ngân sách an toàn",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                val percentageStr = (animatedPct * 100).toInt().toString() + "%"
                Text(
                    text = "Đã sử dụng $percentageStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                if (pct > 0.8) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Cảnh báo: Sắp vượt quá ngân sách!",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFF28B82)
                    )
                }
            }

            // Beautiful circular progress
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(80.dp)
            ) {
                Canvas(modifier = Modifier.size(80.dp)) {
                    val strokeWidth = 8.dp.toPx()
                    drawArc(
                        color = Platinum.copy(alpha = 0.1f),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )
                    
                    val color = when {
                        animatedPct > 0.8f -> Color(0xFFF28B82)
                        animatedPct > 0.5f -> MatteGold
                        else -> Color(0xFF81C995)
                    }

                    drawArc(
                        color = color,
                        startAngle = 135f,
                        sweepAngle = 270f * animatedPct,
                        useCenter = false,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = "${(animatedPct * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
fun FilterChipsSection(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    val filters = listOf("Tất cả") + categories.map { it.name }
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(filters) { filter ->
            val isSelected = filter == selectedFilter
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) MatteGold else CharcoalGray,
                animationSpec = tween(300)
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) RichBlack else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                animationSpec = tween(300)
            )

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onFilterSelected(filter) },
                color = bgColor,
                border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)) else null
            ) {
                Text(
                    text = filter,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                    color = textColor
                )
            }
        }
    }
}

@Composable
fun TransactionRowItem(
    transaction: Transaction,
    onDelete: () -> Unit
) {
    val categoryUi = categories.firstOrNull { it.name == transaction.category }
        ?: CategoryUi("Khác", Icons.Default.Category, Platinum)

    Card(
        modifier = Modifier.fillMaxWidth().testTag("transaction_item_${transaction.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalGray.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(16.dp),
                color = categoryUi.color.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = categoryUi.icon,
                        contentDescription = transaction.category,
                        tint = categoryUi.color,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaction.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = categoryUi.color.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.size(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatDate(transaction.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val isExp = transaction.isExpense
                Text(
                    text = if (isExp) "-${formatVnd(transaction.amount)}" else "+${formatVnd(transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = if (isExp) Color(0xFFF28B82) else Color(0xFF81C995)
                )
                
                var showConfirm by remember { mutableStateOf(false) }
                
                if (showConfirm) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Check, contentDescription = "Xác nhận", tint = Color(0xFFF28B82))
                    }
                } else {
                    IconButton(onClick = { showConfirm = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Xoá", tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}

// ... SetBudgetDialog and AddTransactionDialog follow standard material 3 implementation
// Just keeping the UI smooth without excessive transform logic that breaks
// ... To avoid long generation, they are simplified below

@Composable
fun SetBudgetDialog(
    currentBudget: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var budgetInput by remember { mutableStateOf(if (currentBudget > 0) currentBudget.toLong().toString() else "") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quản lý tài sản", fontWeight = FontWeight.Bold, color = MatteGold) },
        text = {
            Column {
                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { input -> 
                        val cleanInput = input.replace(".", "").replace(",", "").replace("-", "")
                        if (cleanInput.isEmpty() || cleanInput.all { it.isDigit() }) {
                            budgetInput = cleanInput
                            showError = false
                        }
                    },
                    label = { Text("Số dư khả dụng") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MatteGold,
                        focusedLabelColor = MatteGold,
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("budget_amount_input")
                )
                if(showError) {
                    Text("Số tiền không hợp lệ", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val amount = budgetInput.toDoubleOrNull() ?: -1.0
                    if (amount >= 0) onConfirm(amount) else showError = true 
                },
                modifier = Modifier.testTag("confirm_budget_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MatteGold, contentColor = RichBlack)
            ) { Text("CẬP NHẬT", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("HỦY", color = MaterialTheme.colorScheme.onBackground) }
        },
        containerColor = CharcoalGray,
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var isExpense by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf(categories.first().name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isExpense) "Thêm khoản chi" else "Thêm thu nhập", fontWeight = FontWeight.Bold, color = MatteGold)
        },
        text = {
            Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TabRow(
                    selectedTabIndex = if (isExpense) 0 else 1,
                    containerColor = Color.Transparent,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[if(isExpense) 0 else 1]), color = MatteGold)
                    }
                ) {
                    Tab(
                        selected = isExpense,
                        onClick = { isExpense = true },
                        text = { Text("Chi tiêu", color = if(isExpense) MatteGold else MaterialTheme.colorScheme.onBackground) }
                    )
                    Tab(
                        selected = !isExpense,
                        onClick = { isExpense = false },
                        text = { Text("Thu nhập", color = if(!isExpense) MatteGold else MaterialTheme.colorScheme.onBackground) }
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Mô tả") },
                    modifier = Modifier.fillMaxWidth().testTag("transaction_title_input"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MatteGold, focusedLabelColor = MatteGold)
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input -> 
                        val cleanInput = input.replace(".", "").replace(",", "").replace("-", "")
                        if (cleanInput.isEmpty() || cleanInput.all { it.isDigit() }) {
                            amountText = cleanInput
                        }
                    },
                    label = { Text("Số tiền") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("transaction_amount_input"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MatteGold, focusedLabelColor = MatteGold)
                )
                
                Text("Chọn danh mục", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        val isSelected = category.name == selectedCategory
                        Surface(
                            modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { selectedCategory = category.name },
                            color = if (isSelected) category.color else CharcoalGray,
                            border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha=0.2f)) else null
                        ) {
                            Text(
                                text = category.name,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = if (isSelected) RichBlack else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val amount = amountText.toDoubleOrNull() ?: -1.0
                    if (title.isNotEmpty() && amount > 0) {
                        onConfirm(title, amount, selectedCategory, isExpense)
                    }
                },
                modifier = Modifier.testTag("confirm_transaction_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MatteGold, contentColor = RichBlack)
            ) { Text("THÊM XONG", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("HỦY", color = MaterialTheme.colorScheme.onBackground) }
        },
        containerColor = CharcoalGray,
        shape = RoundedCornerShape(28.dp)
    )
}
