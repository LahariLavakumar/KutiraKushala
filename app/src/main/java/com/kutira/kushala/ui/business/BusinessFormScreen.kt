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
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kutira.kushala.data.model.Business

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessFormScreen(
    existingBusiness: Business? = null,
    viewModel: BusinessViewModel,
    onBack: () -> Unit
) {
    val isEdit = existingBusiness != null
    val uiState by viewModel.uiState.collectAsState()

    var name        by remember { mutableStateOf(existingBusiness?.name ?: "") }
    var location    by remember { mutableStateOf(existingBusiness?.location ?: "") }
    var category    by remember { mutableStateOf(existingBusiness?.category ?: "") }
    var contact     by remember { mutableStateOf(existingBusiness?.contactNumber ?: "") }
    var capacity    by remember { mutableStateOf(existingBusiness?.capacity?.toString() ?: "") }
    var capUnit     by remember { mutableStateOf(existingBusiness?.capacityUnit ?: "") }
    var teamDetails by remember { mutableStateOf(existingBusiness?.teamDetails ?: "") }
    var accepting   by remember { mutableStateOf(existingBusiness?.isAcceptingOrders ?: true) }
    var imageUri    by remember { mutableStateOf<Uri?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imageUri = uri
    }

    // Go back automatically after a successful save (loading goes false and no error)
    val wasLoading = remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isActionLoading, uiState.error) {
        if (wasLoading.value && !uiState.isActionLoading && uiState.error == null) {
            onBack()
        }
        wasLoading.value = uiState.isActionLoading
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit Business" else "Add Business") },
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
                label = { Text("Business Name *") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = location, onValueChange = { location = it },
                label = { Text("Location *") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = category, onValueChange = { category = it },
                label = { Text("Category *  (e.g. Textiles, Food)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = contact, onValueChange = { contact = it },
                label = { Text("Contact Number *") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = capacity, onValueChange = { capacity = it },
                    label = { Text("Capacity") },
                    modifier = Modifier.weight(1f), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = capUnit, onValueChange = { capUnit = it },
                    label = { Text("Unit  (e.g. pieces/day)") },
                    modifier = Modifier.weight(1.5f), singleLine = true
                )
            }
            OutlinedTextField(
                value = teamDetails, onValueChange = { teamDetails = it },
                label = { Text("Team Details") },
                modifier = Modifier.fillMaxWidth(), minLines = 3
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = accepting, onCheckedChange = { accepting = it })
                Spacer(Modifier.width(8.dp))
                Text("Currently accepting orders")
            }

            OutlinedButton(
                onClick = { picker.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Image, null)
                Spacer(Modifier.width(8.dp))
                Text(if (imageUri != null) "Image selected ✓" else "Pick Business Image")
            }

            uiState.error?.let { err ->
                Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    val biz = Business(
                        id          = existingBusiness?.id ?: "",
                        name        = name.trim(),
                        location    = location.trim(),
                        category    = category.trim(),
                        contactNumber = contact.trim(),
                        capacity    = capacity.toDoubleOrNull() ?: 0.0,
                        capacityUnit = capUnit.trim(),
                        teamDetails = teamDetails.trim(),
                        isAcceptingOrders = accepting,
                        imageUrl    = existingBusiness?.imageUrl ?: ""
                    )
                    if (isEdit) {
                        viewModel.updateBusiness(existingBusiness!!.id, biz)
                    } else {
                        viewModel.createBusiness(biz, imageUri)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isActionLoading
            ) {
                if (uiState.isActionLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (isEdit) "Save Changes" else "Create Business")
                }
            }
        }
    }
}
