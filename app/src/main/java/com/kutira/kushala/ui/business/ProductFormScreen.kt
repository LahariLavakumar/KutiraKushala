package com.kutira.kushala.ui.business

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kutira.kushala.data.model.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormScreen(
    businessId: String,
    viewModel: BusinessViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var name        by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price       by remember { mutableStateOf("") }
    var moq         by remember { mutableStateOf("1") }
    var tagsRaw     by remember { mutableStateOf("") }   // comma-separated
    var imageUri    by remember { mutableStateOf<Uri?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imageUri = uri
    }

    // Navigate back after successful creation
    val wasLoading = remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isActionLoading, uiState.error) {
        if (wasLoading.value && !uiState.isActionLoading && uiState.error == null) {
            onBack()
        }
        wasLoading.value = uiState.isActionLoading
    }

    // Populate description from AI generation
    LaunchedEffect(uiState.aiDescription) {
        if (uiState.aiDescription.isNotBlank()) description = uiState.aiDescription
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Product") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Product Name *") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = tagsRaw, onValueChange = { tagsRaw = it },
                label = { Text("Category Tags  (comma-separated)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )

            // AI description generation
            OutlinedButton(
                onClick = {
                    viewModel.generateProductDescription(
                        productName = name.ifBlank { "product" },
                        category    = tagsRaw.ifBlank { "general" },
                        keywords    = tagsRaw
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AutoAwesome, null)
                Spacer(Modifier.width(8.dp))
                Text("Generate AI Description")
            }

            OutlinedTextField(
                value = description, onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(), minLines = 4,
                placeholder = { Text("Tap 'Generate AI Description' or write manually") }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = price, onValueChange = { price = it },
                    label = { Text("Wholesale Price (₹) *") },
                    modifier = Modifier.weight(1f), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = moq, onValueChange = { moq = it },
                    label = { Text("Min. Order Qty *") },
                    modifier = Modifier.weight(1f), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            OutlinedButton(
                onClick = { picker.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Image, null)
                Spacer(Modifier.width(8.dp))
                Text(if (imageUri != null) "Image selected ✓" else "Pick Product Image")
            }

            uiState.error?.let { err ->
                Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    val tags = tagsRaw.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    val product = Product(
                        name             = name.trim(),
                        description      = description.trim(),
                        wholesalePrice   = price.toDoubleOrNull() ?: 0.0,
                        minOrderQuantity = moq.toDoubleOrNull() ?: 1.0,
                        categoryTags     = tags
                    )
                    viewModel.createProduct(businessId, product, imageUri)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isActionLoading
            ) {
                if (uiState.isActionLoading) {
                    CircularProgressIndicator(
                        Modifier.size(20.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Add Product to Catalogue")
                }
            }
        }
    }
}
