package com.saurabh.catalogue.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.saurabh.catalogue.ui.category.CategoryScreen
import com.saurabh.catalogue.ui.enquiry.EnquiryScreen
import com.saurabh.catalogue.ui.enquiry.ThankYouScreen
import com.saurabh.catalogue.ui.home.HomeScreen
import com.saurabh.catalogue.ui.product.ProductScreen
import com.saurabh.catalogue.ui.search.SearchScreen

/**
 * App navigation graph. Screens receive navigation callbacks (not the NavController), so they stay
 * reusable and testable. Route arguments reach ViewModels through SavedStateHandle.
 */
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val openProduct: (String) -> Unit = { id -> navController.navigate(ProductRoute(id)) }
    val back: () -> Unit = { navController.popBackStack() }

    NavHost(navController = navController, startDestination = HomeRoute, modifier = modifier) {
        composable<HomeRoute> {
            HomeScreen(
                onProductClick = openProduct,
                onCategoryClick = { category -> navController.navigate(CategoryRoute(category.slug, category.name)) },
                onSearch = { query -> navController.navigate(SearchRoute(query)) },
            )
        }
        composable<CategoryRoute> { CategoryScreen(onBack = back, onProductClick = openProduct) }
        composable<SearchRoute> { SearchScreen(onBack = back, onProductClick = openProduct) }
        composable<ProductRoute> {
            ProductScreen(
                onBack = back,
                onEnquire = { id, title -> navController.navigate(EnquiryRoute(id, title)) },
            )
        }
        composable<EnquiryRoute> {
            EnquiryScreen(
                onBack = back,
                onSent = { title ->
                    // Replace the form with Thank you, so Back can't return to an already-sent form.
                    navController.navigate(ThankYouRoute(title)) {
                        popUpTo<EnquiryRoute> { inclusive = true }
                    }
                },
            )
        }
        composable<ThankYouRoute> { entry ->
            ThankYouScreen(
                productTitle = entry.toRoute<ThankYouRoute>().productTitle,
                onDone = { navController.popBackStack(HomeRoute, inclusive = false) },
            )
        }
    }
}
