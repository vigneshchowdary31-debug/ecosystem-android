package com.ecosystem.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.ecosystem.core.designsystem.theme.CompanionAppTheme
import com.ecosystem.feature.pairing.presentation.PairingViewModel
import com.ecosystem.feature.pairing.presentation.screen.PairingScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompanionAppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppContent()
                }
            }
        }
    }
}

/** Release builds show only pairing. Debug builds add the developer tools as extra tabs. */
@Composable
private fun AppContent() {
    val pairingViewModel: PairingViewModel = hiltViewModel()
    val debugTabs = DebugTools.tabs()
    if (debugTabs.isEmpty()) {
        PairingScreen(viewModel = pairingViewModel)
        return
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val titles = listOf("Pair Device") + debugTabs.map { it.title }
    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = selectedTab) {
            titles.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
            }
        }
        if (selectedTab == 0) {
            PairingScreen(viewModel = pairingViewModel)
        } else {
            debugTabs[selectedTab - 1].content()
        }
    }
}
