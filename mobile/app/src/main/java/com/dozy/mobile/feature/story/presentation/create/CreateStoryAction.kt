package com.dozy.mobile.feature.story.presentation.create

internal sealed interface CreateStoryAction {
    data class Setting(
        val id: String?,
    ) : CreateStoryAction

    data class AddHero(
        val id: String,
    ) : CreateStoryAction

    data class Hero(
        val id: String,
    ) : CreateStoryAction

    data class Length(
        val index: Int,
    ) : CreateStoryAction

    data class Request(
        val value: String,
    ) : CreateStoryAction

    data class Ending(
        val value: String,
    ) : CreateStoryAction

    data class Automatic(
        val enabled: Boolean,
    ) : CreateStoryAction

    data object ReloadCharacters : CreateStoryAction

    data object Submit : CreateStoryAction

    data object ChoosePlace : CreateStoryAction
}
