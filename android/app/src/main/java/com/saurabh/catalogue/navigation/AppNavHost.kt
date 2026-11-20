package com.saurabh.catalogue.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.saurabh.catalogue.ui.common.MessageView

/**
 * Navigation shell. Each destination is a placeholder until its screen is built (Phase 3b),
 * so the graph and argument passing are in place from the start.
 */
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = HomeRoute, modifier = modifier) {
        composable<HomeRoute> { MessageView("Home") }
        composable<CategoryRoute> { entry -> MessageView("Category: ${entry.toRoute<CategoryRoute>().name}") }
        composable<SearchRoute> { entry -> MessageView("Search: ${entry.toRoute<SearchRoute>().query}") }
        composable<ProductRoute> { entry -> MessageView("Product ${entry.toRoute<ProductRoute>().id}") }
        composable<EnquiryRoute> { entry -> MessageView("Enquiry: ${entry.toRoute<EnquiryRoute>().productTitle}") }
        composable<ThankYouRoute> { MessageView("Thank you") }
    }
}
