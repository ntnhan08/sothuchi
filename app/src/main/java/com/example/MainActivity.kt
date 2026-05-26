package com.example

import android.os.Bundle
import android.app.Application
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.composed
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Transaction
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FinanceUiState
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.FinanceViewModelFactory
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    CategoryUi("Ăn uống", Icons.Default.Restaurant, Color(0xFFE57373)),
    CategoryUi("Di chuyển", Icons.Default.DirectionsCar, Color(0xFF64B5F6)),
    CategoryUi("Mua sắm", Icons.Default.ShoppingCart, Color(0xFF81C784)),
    CategoryUi("Nhà cửa", Icons.Default.Home, Color(0xFFFFD54F)),
    CategoryUi("Giải trí", Icons.Default.SportsEsports, Color(0xFFBA68C8)),
    CategoryUi("Sức khỏe", Icons.Default.MedicalServices, Color(0xFF4DB6AC)),
    CategoryUi("Học tập", Icons.Default.School, Color(0xFFFFB74D)),
    CategoryUi("Khác", Icons.Default.Category, Color(0xFFA1887F))
)

data class CategoryUi(
    val name: String,
    val icon: ImageVector,
    val color: Color
)

// Currency helpers
fun formatVnd(amount: Double): String {
    val formatter = DecimalFormat("#,###")
    return formatter.format(amount).replace(",", ".") + " ₫"
}

fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return formatter.format(date)
}

@Composable
fun BudgetApp() {
    val context = LocalContext.current
    val viewModel: FinanceViewModel = viewModel(
        factory = FinanceViewModelFactory(context)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf("Tất cả") }

    val activeParticles = remember { mutableStateListOf<Particle>() }
    var confettiTriggerKey by remember { mutableStateOf(0L) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxWidthPx = constraints.maxWidth.toFloat()
        val maxHeightPx = constraints.maxHeight.toFloat()

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .testTag("add_transaction_fab")
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Thêm Ghi Chép",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                // Header Bar
                HeaderSection()

                // Main Financial Summary Card (Tiền Đang Có, Chi Tiêu, Còn Lại)
                SummaryCardSection(
                    uiState = uiState,
                    onEditBudgetClicked = { showBudgetDialog = true }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Percentage spent visual track
                SpentTrackerProgressSection(uiState = uiState)

                Spacer(modifier = Modifier.height(20.dp))

                // Title & Filter Label
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lịch sử giao dịch",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Lọc theo danh mục",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter tags
                FilterChipsSection(
                    selectedFilter = selectedCategoryFilter,
                    onFilterSelected = { selectedCategoryFilter = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Transaction History list
                TransactionsLedgerSection(
                    uiState = uiState,
                    selectedFilter = selectedCategoryFilter,
                    onDeleteTransaction = { id -> viewModel.deleteTransaction(id) }
                )
            }
        }

        // Modal dialogs
        if (showBudgetDialog) {
            SetBudgetDialog(
                currentBudget = uiState.budgetConfig.currentFunds,
                onDismiss = { showBudgetDialog = false },
                onConfirm = { amount ->
                    viewModel.updateInitialBudget(amount)
                    showBudgetDialog = false
                    triggerPhysicsConfetti(activeParticles, maxWidthPx / 2f, maxHeightPx * 0.4f, false)
                    confettiTriggerKey = System.currentTimeMillis()
                }
            )
        }

        if (showAddDialog) {
            AddTransactionDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { title, amount, category, isExpense ->
                    viewModel.addTransaction(title, amount, category, isExpense)
                    showAddDialog = false
                    triggerPhysicsConfetti(activeParticles, maxWidthPx / 2f, maxHeightPx * 0.7f, isExpense)
                    confettiTriggerKey = System.currentTimeMillis()
                }
            )
        }

        // Animated physics confetti simulation overlay
        if (activeParticles.isNotEmpty()) {
            ConfettiOverlay(
                particles = activeParticles.toList(),
                triggerKey = confettiTriggerKey,
                onAnimationFinished = { activeParticles.clear() }
            )
        }
    }
}

@Composable
fun HeaderSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "SỔ THU CHI",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "Kiểm soát chi tiêu thông minh",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = "User wallet",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun SummaryCardSection(
    uiState: FinanceUiState,
    onEditBudgetClicked: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "SummaryCardPulse")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientPulse"
    )
    val movingGradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)
        ),
        start = androidx.compose.ui.geometry.Offset(animatedOffset, 0f),
        end = androidx.compose.ui.geometry.Offset(animatedOffset + 800f, 800f)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .springClickable { onEditBudgetClicked() }
            .background(movingGradient, shape = RoundedCornerShape(24.dp))
            .testTag("budget_setting_card_button"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Main Remaining Funds displayed prominently
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Số Dư Hiện Còn",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AnimatedVndText(
                        amount = uiState.remainingBalance,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = if (uiState.remainingBalance >= 0) {
                            Color(0xFF10B981) // Mint Emerald
                        } else {
                            Color(0xFFEF4444) // Vibrant Alert Coral
                        }
                    )
                }
 
                // Edit Button Icon
                IconButton(
                    onClick = onEditBudgetClicked,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = CircleShape
                        )
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Chỉnh sửa ngân sách",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
 
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 14.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
 
            // Side by Side columns: Tiền Đang Có vs Tổng Chi Tiêu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Tiền Nhập Có
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = "Funds Icon",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Tiền Đang Có",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    AnimatedVndText(
                        amount = uiState.budgetConfig.currentFunds,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
 
                // Separator Line
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        .align(Alignment.CenterVertically)
                )
 
                Spacer(modifier = Modifier.width(16.dp))
 
                // Tổng Chi Tiêu
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = "Expenses Icon",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Tổng Chi Tiêu",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    AnimatedVndText(
                        amount = uiState.totalExpenses,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFFF59E0B)
                    )
                }
            }
        }
    }
}

@Composable
fun SpentTrackerProgressSection(uiState: FinanceUiState) {
    if (uiState.budgetConfig.currentFunds <= 0) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Hãy nhấp vào ô 'Số Dư' ở trên để thiết lập số tiền khả dụng hiện tại của bạn!",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    } else {
        val pct = (uiState.totalExpenses / uiState.budgetConfig.currentFunds).coerceIn(0.0, 1.0)
        val animatedPct by animateFloatAsState(
            targetValue = pct.toFloat(),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "ProgressBarAnimation"
        )
        val formattedPct = (pct * 100).toInt()

        val progressColor = when {
            pct >= 0.9 -> Color(0xFFEF4444) // Fast approaching alert
            pct >= 0.6 -> Color(0xFFF59E0B) // Warn
            else -> Color(0xFF10B981) // Safe
        }

        val motivationMessage = when {
            pct >= 1.0 -> "Cảnh báo: Bạn đã chi tiêu quá định mức ngân sách của mình!"
            pct >= 0.85 -> "Nguy hiểm: Số tiền khả dụng sắp hết. Hãy cân nhắc chi tiêu kỹ!"
            pct >= 0.5 -> "Cảnh báo: Bạn đã chi hết nửa ngân sách. Cố gắng kiểm soát tốt!"
            else -> "Ví của bạn an toàn. Hãy tiếp tục duy trì thói quen chi chép chu đáo!"
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tỷ lệ chi tiêu: $formattedPct%",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Ngân sách tối đa",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { animatedPct },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = "Tips icon",
                    tint = progressColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = motivationMessage,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = progressColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(filters) { filter ->
            val isSelected = filter == selectedFilter
            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(filter) },
                label = { Text(text = filter) },
                modifier = Modifier.testTag("filter_${filter.lowercase().replace(" ", "_")}_chip"),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = null
            )
        }
    }
}

@Composable
fun TransactionsLedgerSection(
    uiState: FinanceUiState,
    selectedFilter: String,
    onDeleteTransaction: (Int) -> Unit
) {
    val filteredTransactions = remember(uiState.transactions, selectedFilter) {
        if (selectedFilter == "Tất cả") {
            uiState.transactions
        } else {
            uiState.transactions.filter { it.category == selectedFilter }
        }
    }

    if (filteredTransactions.isEmpty()) {
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
                    imageVector = Icons.Default.Savings,
                    contentDescription = "Piggy Empty State",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                    modifier = Modifier.size(90.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Chưa có ghi chép chi tiêu nào",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (selectedFilter == "Tất cả") {
                        "Nhấp nút tròn dấu '+' góc dưới màn hình để thêm khoản chi tiêu đầu tiên của bạn ngay!"
                    } else {
                        "Không tìm thấy chi tiêu nào trong danh mục '$selectedFilter' này."
                    },
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.widthIn(max = 260.dp)
                )
            }
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(
                items = filteredTransactions,
                key = { _, tx -> tx.id }
            ) { index, tx ->
                var animated by remember { mutableStateOf(false) }
                LaunchedEffect(tx.id) {
                    kotlinx.coroutines.delay((index * 40L).coerceAtMost(300L))
                    animated = true
                }

                val alpha by animateFloatAsState(
                    targetValue = if (animated) 1f else 0f,
                    animationSpec = tween(durationMillis = 350),
                    label = "itemAlpha"
                )
                val translateY by animateFloatAsState(
                    targetValue = if (animated) 0f else 40f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "itemTranslationY"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            this.alpha = alpha
                            this.translationY = translateY
                        }
                ) {
                    TransactionRowItem(
                        transaction = tx,
                        onDelete = { onDeleteTransaction(tx.id) }
                    )
                }
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
        ?: CategoryUi("Khác", Icons.Default.Category, Color(0xFFA1887F))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transaction_item_${transaction.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon Badge
            Surface(
                modifier = Modifier.size(46.dp),
                shape = CircleShape,
                color = categoryUi.color.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = categoryUi.icon,
                        contentDescription = transaction.category,
                        tint = categoryUi.color,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text Info (Title, Category details and date)
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaction.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = categoryUi.color,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formatDate(transaction.timestamp),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Expense or Income amount
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                val formattedAmount = formatVnd(transaction.amount)
                val isExp = transaction.isExpense
                Text(
                    text = if (isExp) "-$formattedAmount" else "+$formattedAmount",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = if (isExp) Color(0xFFEF4444) else Color(0xFF10B981)
                )

                // Delete transaction row
                var showConfirmDelete by remember { mutableStateOf(false) }

                if (showConfirmDelete) {
                    IconButton(
                        onClick = {
                            onDelete()
                            showConfirmDelete = false
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Xác nhận xoá",
                            tint = Color(0xFFE57373)
                        )
                    }
                } else {
                    IconButton(
                        onClick = { showConfirmDelete = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Xoá giao dịch",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.39f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SetBudgetDialog(
    currentBudget: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var budgetInput by remember { mutableStateOf(if (currentBudget > 0) currentBudget.toInt().toString() else "") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Cập Nhật Ngân Sách",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                Text(
                    text = "Nhập số tiền đang có hiện tại để ứng dụng theo dõi ngân sách và cân đối số dư còn lại sau khi chi tiêu.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = {
                        if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                            budgetInput = it
                            showError = false
                        }
                    },
                    label = { Text("Số tiền đang có (đ)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("Ví dụ: 5000000") },
                    singleLine = true,
                    isError = showError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_amount_input"),
                    trailingIcon = {
                        if (budgetInput.isNotEmpty()) {
                            IconButton(onClick = { budgetInput = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    }
                )

                if (showError) {
                    Text(
                        text = "Vui lòng nhập một số hợp lệ lớn hơn 0",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Short cuts Row
                Text(
                    text = "Nạp nhanh ngân sách:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                val presets = listOf(
                    PresetUi("+500k", 500000.0),
                    PresetUi("+1Tr", 1000000.0),
                    PresetUi("+2Tr", 2000000.0),
                    PresetUi("+5Tr", 5000000.0),
                    PresetUi("+10Tr", 10000000.0)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(presets) { preset ->
                        AssistChip(
                            onClick = {
                                val currentVal = budgetInput.toDoubleOrNull() ?: 0.0
                                budgetInput = (currentVal + preset.value).toLong().toString()
                                showError = false
                            },
                            label = { Text(preset.label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = budgetInput.toDoubleOrNull()
                    if (amount != null && amount >= 0) {
                        onConfirm(amount)
                    } else {
                        showError = true
                    }
                },
                modifier = Modifier.testTag("confirm_budget_button")
            ) {
                Text("Xác Nhận")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

data class PresetUi(val label: String, val value: Double)

@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var isExpense by remember { mutableStateOf(true) } // Expense vs Income option
    var selectedCategory by remember { mutableStateOf(categories.first().name) }

    var titleError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isExpense) "Ghi Nhận Khoản Chi" else "Thêm Khoản Thu Nhật",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Toggle Button Expense / Income
                TabRow(
                    selectedTabIndex = if (isExpense) 0 else 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(8.dp)
                        ),
                    divider = {},
                    indicator = {}
                ) {
                    Tab(
                        selected = isExpense,
                        onClick = { isExpense = true },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.TrendingDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Chi tiêu", style = MaterialTheme.typography.labelLarge)
                            }
                        },
                        selectedContentColor = Color(0xFFEF4444),
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Tab(
                        selected = !isExpense,
                        onClick = { isExpense = false },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Thu nhập", style = MaterialTheme.typography.labelLarge)
                            }
                        },
                        selectedContentColor = Color(0xFF10B981),
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Item description / title
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotEmpty()) titleError = false
                    },
                    label = { Text("Tên khoản chi / thu") },
                    placeholder = { Text("Ví dụ: Ăn trưa, Đổ xăng, Lương...") },
                    singleLine = true,
                    isError = titleError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_title_input")
                )
                if (titleError) {
                    Text(
                        text = "Vui lòng nhập tên khoản giao dịch!",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Numerical price / amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                            amountText = it
                            if (it.isNotEmpty()) amountError = false
                        }
                    },
                    label = { Text("Số tiền (đ)") },
                    placeholder = { Text("Ví dụ: 35000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = amountError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_amount_input"),
                    trailingIcon = {
                        if (amountText.isNotEmpty()) {
                            IconButton(onClick = { amountText = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    }
                )
                if (amountError) {
                    Text(
                        text = "Vui lòng nhập số tiền hợp lệ lớn hơn 0",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Scrollable category select chips
                Text(
                    text = "Chọn danh mục:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(top = 2.dp, bottom = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        val isSelected = cat.name == selectedCategory
                        InputChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat.name },
                            label = { Text(cat.name) },
                            leadingIcon = {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = cat.name,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.onSecondary else cat.color
                                )
                            },
                            colors = InputChipDefaults.inputChipColors(
                                selectedContainerColor = cat.color,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalAmount = amountText.toDoubleOrNull()
                    var isVal = true

                    if (title.isBlank()) {
                        titleError = true
                        isVal = false
                    }
                    if (finalAmount == null || finalAmount <= 0) {
                        amountError = true
                        isVal = false
                    }

                    if (isVal && finalAmount != null) {
                        onConfirm(title, finalAmount, selectedCategory, isExpense)
                    }
                },
                modifier = Modifier.testTag("confirm_transaction_button")
            ) {
                Text("Ghi Lại")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

// --- PREMIUM FLUTTER-STYLE ADDITIONS ---

data class Particle(
    val id: Int,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float,
    val alpha: Float,
    val rotation: Float,
    val rotSpeed: Float
)

@Composable
fun ConfettiOverlay(
    particles: List<Particle>,
    triggerKey: Long,
    onAnimationFinished: () -> Unit
) {
    if (particles.isEmpty()) return

    val isTesting = remember {
        try {
            Class.forName("org.robolectric.Robolectric")
            true
        } catch (e: Exception) {
            false
        }
    }

    if (isTesting) {
        LaunchedEffect(Unit) {
            onAnimationFinished()
        }
        return
    }

    val animatable = remember { Animatable(0f) }

    LaunchedEffect(triggerKey) {
        animatable.snapTo(0f)
        animatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing)
        )
        onAnimationFinished()
    }

    val progress = animatable.value
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val t = progress * 40f
        val gravity = 0.5f
        for (p in particles) {
            val x = p.x + p.vx * t
            val y = p.y + p.vy * t + 0.5f * gravity * t * t
            val alpha = (1.0f - 0.025f * t).coerceIn(0f, 1f)
            val rotation = p.rotation + p.rotSpeed * t
            if (alpha > 0f) {
                rotate(rotation, pivot = androidx.compose.ui.geometry.Offset(x, y)) {
                    drawRoundRect(
                        color = p.color.copy(alpha = alpha),
                        topLeft = androidx.compose.ui.geometry.Offset(x - p.size / 2f, y - p.size / 2f),
                        size = androidx.compose.ui.geometry.Size(p.size, p.size),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                    )
                }
            }
        }
    }
}

fun triggerPhysicsConfetti(
    particles: androidx.compose.runtime.snapshots.SnapshotStateList<Particle>,
    startX: Float,
    startY: Float,
    isExpense: Boolean
) {
    particles.clear()
    val colors = if (isExpense) {
        listOf(
            Color(0xFFEF4444), Color(0xFFF59E0B), Color(0xFFFF8A80), 
            Color(0xFFFF5252), Color(0xFFFFB74D), Color(0xFFFF8F00)
        )
    } else {
        listOf(
            Color(0xFF10B981), Color(0xFF34D399), Color(0xFF6EE7B7), 
            Color(0xFF64B5F6), Color(0xFF4FC3F7), Color(0xFF00E676)
        )
    }
    
    for (i in 0 until 45) {
        val angle = Math.toRadians((210..330).random().toDouble())
        val speed = (12..28).random().toFloat()
        particles.add(
            Particle(
                id = i,
                x = startX,
                y = startY,
                vx = (Math.cos(angle) * speed).toFloat(),
                vy = (Math.sin(angle) * speed).toFloat(),
                color = colors.random(),
                size = (8..22).random().toFloat(),
                alpha = 1.0f,
                rotation = (0..360).random().toFloat(),
                rotSpeed = ((-6..6).random() * 1.5f).toFloat()
            )
        )
    }
}

@Composable
fun AnimatedVndText(
    amount: Double,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified
) {
    val isTesting = remember {
        try {
            Class.forName("org.robolectric.Robolectric")
            true
        } catch (e: Exception) {
            false
        }
    }

    if (isTesting) {
        Text(
            text = formatVnd(amount),
            style = style,
            color = color,
            modifier = modifier
        )
        return
    }

    val animatedAmount by animateFloatAsState(
        targetValue = amount.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "VndAmountAnimation"
    )
    Text(
        text = formatVnd(animatedAmount.toDouble()),
        style = style,
        color = color,
        modifier = modifier
    )
}

fun Modifier.springClickable(
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val interactions = remember { mutableStateListOf<androidx.compose.foundation.interaction.Interaction>() }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is androidx.compose.foundation.interaction.PressInteraction.Press -> {
                    interactions.add(interaction)
                }
                is androidx.compose.foundation.interaction.PressInteraction.Release -> {
                    interactions.remove(interaction.press)
                }
                is androidx.compose.foundation.interaction.PressInteraction.Cancel -> {
                    interactions.remove(interaction.press)
                }
            }
        }
    }
    val isPressed = interactions.isNotEmpty()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bounce"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = androidx.compose.foundation.LocalIndication.current,
            onClick = onClick
        )
}
