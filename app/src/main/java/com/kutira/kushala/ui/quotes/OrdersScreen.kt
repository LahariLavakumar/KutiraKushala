package com.kutira.kushala.ui.quotes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kutira.kushala.data.model.Quote
import com.kutira.kushala.data.model.QuoteStatus
import com.kutira.kushala.ui.business.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(viewModel: QuoteViewModel, isMaker: Boolean) {
    val buyerQuotes by viewModel.buyerQuotes.collectAsState()
    val sellerQuotes by viewModel.sellerQuotes.collectAsState()

    LaunchedEffect(Unit) {
        if (isMaker) viewModel.loadSellerQuotes() else viewModel.loadBuyerQuotes()
    }

    Scaffold(
        topBar = { 
            LargeTopAppBar(
                title = { Text(if (isMaker) "Incoming Orders" else "My Orders", fontWeight = FontWeight.Bold) }
            ) 
        }
    ) { padding ->
        val quoteList = if (isMaker) sellerQuotes else buyerQuotes

        if (quoteList.isEmpty()) {
            Box(Modifier.padding(padding)) {
                EmptyState(
                    icon = Icons.AutoMirrored.Outlined.Assignment,
                    title = "No orders yet",
                    description = if (isMaker) "List products and wait for buyer inquiries." else "Browse artisan products and place your first order."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp), 
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(quoteList, key = { it.id }) { quote ->
                    QuoteCard(
                        quote = quote,
                        isMaker = isMaker,
                        onUpdateStatus = { status -> viewModel.updateOrderStatus(quote.id, status) },
                        onCancel = { viewModel.cancelOrder(quote.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun QuoteCard(
    quote: Quote,
    isMaker: Boolean,
    onUpdateStatus: (QuoteStatus) -> Unit,
    onCancel: () -> Unit
) {
    val status = runCatching { QuoteStatus.valueOf(quote.status) }.getOrDefault(QuoteStatus.Pending)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    quote.productName, 
                    style = MaterialTheme.typography.titleLarge, 
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                StatusChip(status)
            }
            
            Spacer(Modifier.height(8.dp))
            
            Row {
                Column(Modifier.weight(1f)) {
                    Text("Quantity: ${quote.quantity.toInt()} units", style = MaterialTheme.typography.bodyMedium)
                    Text("Total: ₹${quote.totalPrice}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                if (isMaker && quote.buyerEmail.isNotBlank()) {
                    Text("Buyer: ${quote.buyerEmail.substringBefore("@")}...", style = MaterialTheme.typography.bodySmall)
                }
            }

            if (quote.trackingNumber.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalShipping, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("${quote.trackingCarrier} – ${quote.trackingNumber}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (isMaker) {
                val nextStatus = when (status) {
                    QuoteStatus.Pending -> QuoteStatus.Processing
                    QuoteStatus.Processing -> QuoteStatus.Shipped
                    QuoteStatus.Shipped -> QuoteStatus.Delivered
                    else -> null
                }
                nextStatus?.let { next ->
                    Button(
                        onClick = { onUpdateStatus(next) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Mark as ${next.name}")
                    }
                }
            } else {
                if (status == QuoteStatus.Pending) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Cancel Order")
                    }
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: QuoteStatus) {
    val (color, label) = when (status) {
        QuoteStatus.Pending -> MaterialTheme.colorScheme.secondaryContainer to "Pending"
        QuoteStatus.Processing -> MaterialTheme.colorScheme.tertiaryContainer to "Processing"
        QuoteStatus.Shipped -> MaterialTheme.colorScheme.primaryContainer to "Shipped"
        QuoteStatus.Delivered -> MaterialTheme.colorScheme.primary to "Delivered"
        QuoteStatus.Cancelled -> MaterialTheme.colorScheme.errorContainer to "Cancelled"
    }
    SuggestionChip(
        onClick = {}, 
        label = { Text(label) },
        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = color)
    )
}
