package com.example.mindweaverstudio.components.sidebar

import com.arkivanov.mvikotlin.core.store.Store
import com.example.mindweaverstudio.components.codeeditor.models.SidebarMenuItem

interface SidebarStore : Store<SidebarStore.Intent, SidebarStore.State, SidebarStore.Label> {

    data class State(
        val isVisible: Boolean = false,
        val selectedMenuItem: SidebarMenuItem? = null,
        val expandedMenuItems: Set<SidebarMenuItem> = emptySet()
    )

    sealed class Intent {
        data object ToggleSidebar : Intent()
        data class SelectMenuItem(val menuItem: SidebarMenuItem) : Intent()
        data class ToggleMenuExpansion(val menuItem: SidebarMenuItem) : Intent()
        data class ExecuteSubMenuAction(val action: String, val menuItem: SidebarMenuItem) : Intent()
        data object CloseSidebar : Intent()
    }

    sealed class Label {
        data class SubMenuActionRequested(val action: String, val menuItem: SidebarMenuItem) : Label()
    }

    sealed interface Action {
        data object Init : Action
    }

    sealed class Msg {
        data object SidebarToggled : Msg()
        data object SidebarClosed : Msg()
        data class MenuItemSelected(val menuItem: SidebarMenuItem) : Msg()
        data class MenuItemExpansionToggled(val menuItem: SidebarMenuItem) : Msg()
    }
}