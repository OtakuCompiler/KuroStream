package com.kurostream.legacyui.anistream.ui.settings

sealed class SettingItem {
    abstract val key: String

    data class Header(
        val title: String
    ) : SettingItem() {
        override val key: String = "header_$title"
    }

    data class Toggle(
        override val key: String,
        val title: String,
        val subtitle: String,
        val isChecked: Boolean
    ) : SettingItem()

    data class Navigate(
        override val key: String,
        val title: String,
        val subtitle: String
    ) : SettingItem()

    data class Action(
        override val key: String,
        val title: String,
        val subtitle: String
    ) : SettingItem()

    data class ThemeSelector(
        override val key: String,
        val title: String,
        val subtitle: String,
        val currentTheme: String
    ) : SettingItem()

    data class Dropdown(
        override val key: String,
        val title: String,
        val subtitle: String,
        val options: List<String>,
        val selectedIndex: Int
    ) : SettingItem()
}
