package com.example.mindweaverstudio.components.sidebar

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.example.mindweaverstudio.components.codeeditor.models.SidebarMenuItem
import com.example.mindweaverstudio.components.sidebar.SidebarStore.Msg
import com.example.mindweaverstudio.components.sidebar.SidebarStore.Msg.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing

class SidebarStoreFactory(
    private val storeFactory: StoreFactory,
) {

    fun create(): SidebarStore =
        object : SidebarStore, Store<SidebarStore.Intent, SidebarStore.State, SidebarStore.Label> by storeFactory.create(
            name = "SidebarStore",
            initialState = SidebarStore.State(),
            bootstrapper = SimpleBootstrapper(SidebarStore.Action.Init),
            executorFactory = ::ExecutorImpl,
            reducer = ReducerImpl
        ) {}

    private inner class ExecutorImpl : CoroutineExecutor<SidebarStore.Intent, SidebarStore.Action, SidebarStore.State, Msg, SidebarStore.Label>(
        mainContext = Dispatchers.Swing
    ) {
        override fun executeAction(action: SidebarStore.Action) = when(action) {
            SidebarStore.Action.Init -> {
                // Initialize any required resources
            }
        }

        override fun executeIntent(intent: SidebarStore.Intent) {
            when (intent) {
                SidebarStore.Intent.ToggleSidebar -> {
                    dispatch(SidebarToggled)
                }
                
                SidebarStore.Intent.CloseSidebar -> {
                    dispatch(SidebarClosed)
                }
                
                is SidebarStore.Intent.SelectMenuItem -> {
                    dispatch(MenuItemSelected(intent.menuItem))
                }
                
                is SidebarStore.Intent.ToggleMenuExpansion -> {
                    dispatch(MenuItemExpansionToggled(intent.menuItem))
                }
                
                is SidebarStore.Intent.ExecuteSubMenuAction -> {
                    publish(SidebarStore.Label.SubMenuActionRequested(intent.action, intent.menuItem))
                }
            }
        }
    }

    private object ReducerImpl : Reducer<SidebarStore.State, Msg> {
        override fun SidebarStore.State.reduce(msg: Msg): SidebarStore.State =
            when (msg) {
                is SidebarToggled -> copy(isVisible = !isVisible)
                is SidebarClosed -> copy(isVisible = false, selectedMenuItem = null, expandedMenuItems = emptySet())
                is MenuItemSelected -> copy(selectedMenuItem = msg.menuItem)
                is MenuItemExpansionToggled -> {
                    val updatedExpanded = if (expandedMenuItems.contains(msg.menuItem)) {
                        expandedMenuItems - msg.menuItem
                    } else {
                        expandedMenuItems + msg.menuItem
                    }
                    copy(expandedMenuItems = updatedExpanded)
                }
            }
    }
}