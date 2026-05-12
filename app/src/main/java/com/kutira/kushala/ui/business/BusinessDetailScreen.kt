package com.kutira.kushala.ui.business

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.StarHalf
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.kutira.kushala.data.model.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessDetailScreen(
    businessId: String,
    viewModel: BusinessViewModel,
    wishlistViewModel: com.kutira.kushala.ui.wishlist.WishlistViewModel,
    onBack: () -> Unit,
    onCheckout: (Product, Double) -> Unit,
    currentUserId: String,
    onEditBusiness: () -> Unit = {},
    onAddProduct: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val business = uiState.selectedBusiness
    val isOwner = business?.ownerId == currentUserId

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(businessId) { viewModel.loadBusiness(businessId) }

    var showQuantityDialog by remember { mutableStateOf<Product?>(null) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = { Text(business?.name ?: "Business Details") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    if (isOwner) {
                        IconButton(onClick = onEditBusiness) { Icon(Icons.Default.Edit, "Edit") }
                        IconButton(onClick = onAddProduct) { Icon(Icons.Default.AddCircle, "Add Product") }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        if (uiState.isDetailLoading || business == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    BusinessHeader(business)
                }

                item {
                    BusinessInfoSection(
                        business = business, 
                        isOwner = isOwner,
                        onToggleOrders = { isChecked ->
                            viewModel.updateBusiness(businessId, business.copy(isAcceptingOrders = isChecked), isSilent = true)
                        }
                    )
                }

                item {
                    Text(
                        "Product Catalog",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                    )
                }

                if (uiState.products.isEmpty()) {
                    item {
                        Column(
                            Modifier.fillMaxWidth().padding(48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Inventory2, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(Modifier.height(12.dp))
                            Text("No products listed yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(uiState.products, key = { it.id }) { product ->
                        ProductCard(
                            product = product,
                            isOwner = isOwner,
                            isAcceptingOrders = business.isAcceptingOrders,
                            onWishlist = { wishlistViewModel.addToWishlist(business, product) },
                            onBuyNow = { showQuantityDialog = product },
                            onDelete = { viewModel.deleteProduct(businessId, product.id) }
                        )
                    }
                }
            }
        }
    }

    if (showQuantityDialog != null) {
        val prod = showQuantityDialog!!
        QuantitySelectionDialog(
            product = prod,
            onDismiss = { showQuantityDialog = null },
            onConfirm = { qty ->
                onCheckout(prod, qty)
                showQuantityDialog = null
            }
        )
    }
}

@Composable
fun BusinessHeader(business: com.kutira.kushala.data.model.Business) {
    Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
        if (business.imageUrl.isNotBlank()) {
            AsyncImage(
                model = business.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Factory, null, Modifier.size(80.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)), startY = 300f)
            )
        )
        SuggestionChip(
            onClick = {},
            label = { Text(if (business.isAcceptingOrders) "Open for Orders" else "Closed") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            colors = SuggestionChipDefaults.suggestionChipColors(
                containerColor = if (business.isAcceptingOrders) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                labelColor = Color.White
            )
        )
    }
}

@Composable
fun BusinessInfoSection(
    business: com.kutira.kushala.data.model.Business,
    isOwner: Boolean,
    onToggleOrders: (Boolean) -> Unit
) {
    Column(Modifier.padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("About the Workshop", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (isOwner) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    var checked by remember(business.id, business.isAcceptingOrders) { 
                        mutableStateOf(business.isAcceptingOrders) 
                    }
                    Switch(
                        checked = checked,
                        onCheckedChange = { 
                            checked = it
                            onToggleOrders(it)
                        }
                    )
                    Text("Accepting Orders", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LocationOn, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(business.location, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Category, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(business.category, style = MaterialTheme.typography.bodyMedium)
        }
        
        if (business.teamDetails.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Text("Artisan Story", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(business.teamDetails, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductCard(
    product: Product,
    isOwner: Boolean,
    isAcceptingOrders: Boolean,
    onWishlist: () -> Unit,
    onBuyNow: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.padding(12.dp)) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = null,
                modifier = Modifier.size(110.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                
                RatingBar(rating = 4.8, votes = 15)
                
                Text("₹${product.wholesalePrice}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                
                Text(product.description, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                Spacer(Modifier.height(12.dp))
                
                if (isOwner) {
                    Button(onClick = onDelete, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.error), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                        Icon(Icons.Default.Delete, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Remove")
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onWishlist) { Icon(Icons.Default.FavoriteBorder, null, tint = MaterialTheme.colorScheme.primary) }
                        Button(
                            onClick = onBuyNow,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp),
                            enabled = isAcceptingOrders
                        ) {
                            Text(if (isAcceptingOrders) "Buy Now" else "Paused", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RatingBar(rating: Double, votes: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        repeat(5) { index ->
            val icon = when {
                index + 1 <= rating -> Icons.Filled.Star
                index + 0.5 <= rating -> Icons.AutoMirrored.Outlined.StarHalf
                else -> Icons.Outlined.StarOutline
            }
            Icon(icon, null, Modifier.size(14.dp), tint = Color(0xFFFFB300))
        }
        Spacer(Modifier.width(4.dp))
        Text("($votes)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun QuantitySelectionDialog(
    product: Product,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var quantity by remember { mutableStateOf(product.minOrderQuantity) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Select Quantity", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                
                Text(product.name, style = MaterialTheme.typography.titleMedium)
                Text("₹${product.wholesalePrice} per unit", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                
                Spacer(Modifier.height(24.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (quantity > product.minOrderQuantity) quantity-- }) {
                        Icon(Icons.Default.RemoveCircleOutline, null)
                    }
                    Text(quantity.toInt().toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
                    IconButton(onClick = { quantity++ }) {
                        Icon(Icons.Default.AddCircleOutline, null)
                    }
                }
                Text("Minimum order: ${product.minOrderQuantity.toInt()} units", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)

                Spacer(Modifier.height(32.dp))
                
                Button(
                    onClick = { onConfirm(quantity) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Go to Checkout")
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    }
}
