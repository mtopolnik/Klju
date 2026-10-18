package org.mtopol.klju

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import org.mtopol.klju.ui.KljuTheme
import org.mtopol.klju.ui.KljuApp

class MainActivity : ComponentActivity() {

    private val viewModel: KljuViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // The top bar is burgundy in both themes, so the status bar always uses light icons.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        setContent {
            KljuTheme {
                KljuApp(viewModel)
            }
        }
    }
}
