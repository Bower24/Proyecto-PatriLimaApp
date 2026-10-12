package com.patrimonialima.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.patrimonialima.app.ui.navigation.PatrimonioLimaNavHost
import com.patrimonialima.app.ui.theme.PatrimonioLimaTheme

@Composable
fun PatrimonioLimaApp() {
    PatrimonioLimaTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()
            PatrimonioLimaNavHost(navController = navController)
        }
    }
}