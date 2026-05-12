package com.kutira.kushala.ui.wishlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kutira.kushala.ui.business.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(viewModel: WishlistViewModel) {
    val items by viewModel.items.collectAsState()

    Scaffold(
        topBar = { 
            LargeTopAppBar(
                title = { Text("My Wishlist", fontWeight = FontWeight.Bold) }
            ) 
        }
    ) { padding ->
        if (items.isEmpty()) {
            Box(Modifier.padding(padding)) {
                EmptyState(
                    icon = Icons.Outlined.FavoriteBorder,
                    title = "Your wishlist is empty",
                    description = "Browse artisan products and save your favorites here."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(12.dp), 
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (item.productImageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = item.productImageUrl, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(70.dp).clip(MaterialTheme.shapes.small), 
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.width(16.dp))
                            }
                            Column(Modifier.weight(1f)) {
                                Text(item.productName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("₹${item.productPrice} / unit", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(
                                onClick = { viewModel.removeFromWishlist(item.id) },
                                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Outlined.DeleteOutline, "Remove")
                            }
                        }
                    }
                }
            }
        }
    }
}
