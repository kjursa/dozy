package com.dozy.mobile.feature.character.presentation.list

data class CharacterListItem(
    val name: String,
    val id: String,
    val description: String = "",
    val avatarUrl: String? = null,
)
