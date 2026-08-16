package dev.lai.runtime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dev.lai.runtime.ui.LaiApp
import dev.lai.runtime.ui.MainViewModel
import dev.lai.runtime.ui.theme.LaiTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LaiTheme {
                LaiApp(viewModel = viewModel)
            }
        }
    }
}
