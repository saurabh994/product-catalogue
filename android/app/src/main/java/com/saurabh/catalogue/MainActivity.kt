package com.saurabh.catalogue

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.saurabh.catalogue.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint
import com.saurabh.catalogue.ui.theme.CatalogueTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate: shows the branded splash, then applies Theme.Catalogue.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CatalogueTheme {
                AppNavHost()
            }
        }
    }
}
