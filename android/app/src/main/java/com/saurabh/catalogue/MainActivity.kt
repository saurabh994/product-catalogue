package com.saurabh.catalogue

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.saurabh.catalogue.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint
import com.saurabh.catalogue.ui.theme.CatalogueTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CatalogueTheme {
                AppNavHost()
            }
        }
    }
}
