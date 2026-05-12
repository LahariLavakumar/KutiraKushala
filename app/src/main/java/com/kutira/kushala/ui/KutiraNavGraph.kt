package com.kutira.kushala.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.google.firebase.auth.FirebaseAuth
import com.kutira.kushala.data.model.QuoteStatus
import com.kutira.kushala.data.model.Quote
import com.kutira.kushala.data.repository.FirestoreRepository
import com.kutira.kushala.ui.auth.AuthScreen
import com.kutira.kushala.ui.auth.AuthViewModel
import com.kutira.kushala.ui.business.*
import com.kutira.kushala.ui.quotes.*
import com.kutira.kushala.ui.wishlist.*

sealed class Screen(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Businesses : Screen("businesses", "Discover", Icons.Default.Store)
    object Wishlist   : Screen("wishlist",   "Wishlist", Icons.Default.FavoriteBorder)
    object Orders     : Screen("orders",     "Orders",   Icons.Default.ShoppingCart)
    object Profile    : Screen("profile",    "Profile",  Icons.Default.Person)
}

@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KutiraNavGraph() {
    val authViewModel = remember { AuthViewModel() }
    val currentUser by authViewModel.currentUser.collectAsState()

    if (currentUser == null) {
        AuthScreen(viewModel = authViewModel)
        return
    }

    val auth = FirebaseAuth.getInstance()
    val repo = remember { FirestoreRepository() }

    var userRole by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            try {
                val profile = repo.getUserProfile(currentUser!!.uid)
                userRole = profile?.role ?: "Collector"
            } catch (e: Exception) {
                userRole = "Collector"
            }
        } else {
            userRole = null
        }
    }

    if (userRole == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val isMaker = userRole == "Maker"

    val navController     = rememberNavController()
    val businessViewModel = remember { BusinessViewModel() }
    val wishlistViewModel = remember { WishlistViewModel() }
    val quoteViewModel    = remember { QuoteViewModel() }

    val bottomItems = if (isMaker) {
        listOf(Screen.Businesses, Screen.Orders, Screen.Profile)
    } else {
        listOf(Screen.Businesses, Screen.Wishlist, Screen.Orders, Screen.Profile)
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDest = navBackStackEntry?.destination
                bottomItems.forEach { screen ->
                    NavigationBarItem(
                        icon     = { Icon(screen.icon, null) },
                        label    = { Text(screen.label) },
                        selected = currentDest?.hierarchy?.any { it.route == screen.route } == true,
                        onClick  = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController,
            startDestination = Screen.Businesses.route,
            modifier = Modifier.padding(innerPadding)
        ) {

            // ── Business list / customer catalogue ────────────────────────
            composable(Screen.Businesses.route) {
                BusinessListScreen(
                    viewModel          = businessViewModel,
                    onBusinessClick    = { navController.navigate("business/$it") },
                    onAddBusinessClick = { navController.navigate("business/create") },
                    isMaker            = isMaker
                )
            }

            // ── Create a new business (Maker only) ────────────────────────
            composable("business/create") {
                BusinessFormScreen(
                    existingBusiness = null,
                    viewModel        = businessViewModel,
                    onBack           = { navController.popBackStack() }
                )
            }

            // ── Business detail ──────────────────────────────────────────
            composable("business/{businessId}") { backStack ->
                val businessId = backStack.arguments?.getString("businessId") ?: return@composable

                BusinessDetailScreen(
                    businessId        = businessId,
                    viewModel         = businessViewModel,
                    wishlistViewModel = wishlistViewModel,
                    onBack            = { navController.popBackStack() },
                    onCheckout        = { product, quantity ->
                        val uid   = auth.currentUser?.uid   ?: return@BusinessDetailScreen
                        val email = auth.currentUser?.email ?: ""
                        val biz   = businessViewModel.uiState.value.selectedBusiness
                            ?: return@BusinessDetailScreen
                        quoteViewModel.placeQuote(
                            Quote(
                                buyerId     = uid,
                                sellerId    = biz.ownerId,
                                businessId  = businessId,
                                productId   = product.id,
                                productName = product.name,
                                buyerEmail  = email,
                                quantity    = quantity,
                                unitPrice   = product.wholesalePrice,
                                totalPrice  = product.wholesalePrice * quantity,
                                status      = QuoteStatus.Pending.name
                            )
                        )
                    },
                    currentUserId  = auth.currentUser?.uid ?: "",
                    onEditBusiness = { navController.navigate("business/$businessId/edit") },
                    onAddProduct   = { navController.navigate("business/$businessId/product/create") }
                )
            }

            // ── Edit existing business (owner only) ───────────────────────
            composable("business/{businessId}/edit") { backStack ->
                val businessId = backStack.arguments?.getString("businessId") ?: return@composable
                val uiState by businessViewModel.uiState.collectAsState()
                BusinessFormScreen(
                    existingBusiness = uiState.selectedBusiness,
                    viewModel        = businessViewModel,
                    onBack           = { navController.popBackStack() }
                )
            }

            // ── Add product (owner only) ───────────────────────────────────
            composable("business/{businessId}/product/create") { backStack ->
                val businessId = backStack.arguments?.getString("businessId") ?: return@composable
                ProductFormScreen(
                    businessId = businessId,
                    viewModel  = businessViewModel,
                    onBack     = { navController.popBackStack() }
                )
            }

            // ── Wishlist (Collector only) ─────────────────────────────────
            composable(Screen.Wishlist.route) {
                WishlistScreen(viewModel = wishlistViewModel)
            }

            // ── Orders / quotes ───────────────────────────────────────────
            composable(Screen.Orders.route) {
                OrdersScreen(viewModel = quoteViewModel, isMaker = isMaker)
            }

            // ── Profile ───────────────────────────────────────────────────
            composable(Screen.Profile.route) {
                ProfileScreen(auth = auth, onSignOut = { authViewModel.signOut() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(auth: FirebaseAuth, onSignOut: () -> Unit) {
    val user = auth.currentUser
    Scaffold(topBar = { TopAppBar(title = { Text("Profile") }) }) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(user?.email ?: "", style = MaterialTheme.typography.titleMedium)
            if (user?.isEmailVerified == true) {
                Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                    Text("Email Verified")
                }
            } else {
                Text(
                    "⚠\uFE0F Email not verified — recommended for security.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick  = onSignOut,
                colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Sign Out") }
        }
    }
}
