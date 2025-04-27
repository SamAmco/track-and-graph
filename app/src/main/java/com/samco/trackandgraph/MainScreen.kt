package com.samco.trackandgraph

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.DrawerValue
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalDrawer
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.doOnAttach
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import androidx.fragment.app.commitNow
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.theming.tngColors
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    activity: FragmentActivity,
    navBarConfig: State<NavBarConfig>,
) = TnGComposeTheme {

    //TODO add tutorial back
//    if (prefHelper.isFirstRun()) showTutorial()
//    else destroyTutorial()

    var navController by remember { mutableStateOf<NavController?>(null) }

    MainView(
        navBarConfig = navBarConfig,
        onUpClicked = {}
    ) { contentPadding ->

        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            factory = { context ->
                View.inflate(context, R.layout.nav_host_fragment, null)
            },
            update = {
                if (navController == null) {
                    navController = activity
                        .supportFragmentManager
                        .findFragmentById(R.id.nav_fragment)
                        ?.findNavController()
                }
            }
        )

    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MainView(
    navBarConfig: State<NavBarConfig>,
    onUpClicked: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalDrawer(
        drawerState = drawerState,
        drawerShape = CutCornerShape(0.dp),
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            Column(modifier = Modifier.width(300.dp)) { }
        },
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets.systemBarsIgnoringVisibility,
            topBar = {
                TopAppBar(
                    windowInsets = WindowInsets.statusBarsIgnoringVisibility,
                    navigationIcon = {
                        NavigationIcon(
                            navButtonStyle = navBarConfig.value.buttonStyle,
                            onClick = {
                                if (navBarConfig.value.buttonStyle == NavButtonStyle.UP) {
                                    onUpClicked()
                                } else {
                                    scope.launch { drawerState.open() }
                                }
                            }
                        )
                    },
                    title = {
                        Column(verticalArrangement = Arrangement.Center) {
                            navBarConfig.value.title?.let {
                                Text(text = it)
                            }
                            navBarConfig.value.subtitle?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.body2,
                                )
                            }
                        }
                    },
                    backgroundColor = MaterialTheme.tngColors.toolbarBackgroundColor,
                )
            },
            content = content,
        )
    }
}

@Composable
private fun NavigationIcon(
    navButtonStyle: NavButtonStyle,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        when (navButtonStyle) {
            NavButtonStyle.UP -> Icon(
                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                contentDescription = null,
            )

            NavButtonStyle.MENU -> Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = null,
            )
        }
    }
}

//private fun showTutorial() {
//    findViewById<ComposeView>(R.id.tutorialOverlay).setContent {
//        TutorialScreen { destroyTutorial() }
//    }
//}
//private fun destroyTutorial() {
//    val tutorialLayout = findViewById<ViewGroup>(R.id.tutorialOverlay)
//    tutorialLayout.removeAllViews()
//    prefHelper.setFirstRun(false)
//}
