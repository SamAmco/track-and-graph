package com.samco.trackandgraph.main

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppBarViewModel @Inject constructor() : ViewModel() {

    private val navBarConfig = mutableStateOf(NavBarConfig())
    val navBarConfigState: State<NavBarConfig> get() = navBarConfig

    private val onActionTaken = Channel<Action>(1)
    val actionsTaken: ReceiveChannel<Action> = onActionTaken

    fun setNavBarConfig(navBarConfig: NavBarConfig) {
        this.navBarConfig.value = navBarConfig
    }

    fun onAction(action: Action) {
        viewModelScope.launch { onActionTaken.send(action) }
    }

    data class NavBarConfig(
        val title: String? = null,
        val subtitle: String? = null,
        val actions: List<Action> = emptyList(),
        val collapsedActions: CollapsedActions? = null
    )

    enum class NavButtonStyle { UP, MENU }

    data class CollapsedActions(
        val overflowIconId: Int,
        val actions: List<Action>,
    )

    sealed class Action(
        val titleId: Int,
        val iconId: Int,
    ) {
        object AddTracker : Action(
            titleId = R.string.tracker,
            iconId = R.drawable.add_icon
        )

        object ImportCSV : Action(
            titleId = R.string.importButton,
            iconId = R.drawable.import_icon
        )

        object ExportCSV : Action(
            titleId = R.string.exportButton,
            iconId = R.drawable.export_icon
        )

        object AddGroup : Action(
            titleId = R.string.group,
            iconId = R.drawable.add_icon
        )

        object AddGraphStat : Action(
            titleId = R.string.graph_or_stat,
            iconId = R.drawable.add_icon
        )

        object Info : Action(
            titleId = R.string.info,
            iconId = R.drawable.about_icon
        )

        object AddReminder : Action(
            titleId = R.string.add,
            iconId = R.drawable.add_icon
        )

        object AddGlobalNote : Action(
            titleId = R.string.add,
            iconId = R.drawable.add_icon
        )

        object Update : Action(
            titleId = R.string.update,
            iconId = R.drawable.edit_icon
        )
    }
}