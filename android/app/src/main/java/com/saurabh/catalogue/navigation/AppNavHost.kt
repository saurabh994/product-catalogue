package com.saurabh.catalogue.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.saurabh.catalogue.ui.common.MessageView
import com.saurabh.catalogue.ui.home.HomeScreen

/**
 * App navigation graph. Screens receive navigation callbacks (not the NavController), so they stay
 * reusable and testable. Destinations still showing MessageView are built later in Phase 3b.
 */
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = HomeRoute, modifier = modifier) {
        composable<HomeRoute> {
            HomeScreen(onProductClick = { id -> navController.navigate(ProductRoute(id)) })
        }
        composable<CategoryRoute> { entry -> MessageView("Category: ${entry.toRoute<CategoryRoute>().name}") }
        composable<SearchRoute> { entry -> MessageView("Search: ${entry.toRoute<SearchRoute>().query}") }
        composable<ProductRoute> { entry -> MessageView("Product ${entry.toRoute<ProductRoute>().id}") }
        composable<EnquiryRoute> { entry -> MessageView("Enquiry: ${entry.toRoute<EnquiryRoute>().productTitle}") }
        composable<ThankYouRoute> { MessageView("Thank you") }
    }
}
