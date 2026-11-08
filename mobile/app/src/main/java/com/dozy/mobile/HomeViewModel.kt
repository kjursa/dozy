package com.dozy.mobile

import com.kjursa.composeuikit.mvi.MviViewModel

internal data class HomeCharacter(
    val name: String,
    val image: Int,
)

internal data class HomeUiState(
    val dateLabel: String = "TUESDAY, 14 OCTOBER",
    val greeting: String = "Good evening!",
    val prompt: String = "Ready for a new adventure?",
    val stories: List<Story> =
        listOf(
            Story("Oliver and the Secret Pirate Island", "6 min read", R.drawable.story_pirates),
            Story("The Mystery of the Missing Moon", "10 min read", R.drawable.story_moon),
            Story("A Journey Through the Enchanted Forest", "8 min read", R.drawable.story_forest),
        ),
    val characters: List<HomeCharacter> =
        listOf(
            HomeCharacter("Oliver", R.drawable.character_oliver),
            HomeCharacter("Luna", R.drawable.character_luna),
            HomeCharacter("Captain Finn", R.drawable.character_finn),
        ),
)

internal sealed interface HomeAction {
    data object CreateStoryClicked : HomeAction
    data class StorySelected(val story: Story) : HomeAction
    data object SeeAllStoriesClicked : HomeAction
    data class CharacterSelected(val character: HomeCharacter) : HomeAction
    data object AddCharacterClicked : HomeAction
    data object NotificationsClicked : HomeAction
}

internal sealed interface HomeEffect {
    data object CreateStoryRequested : HomeEffect
    data class OpenStory(val story: Story) : HomeEffect
    data object SeeAllStoriesRequested : HomeEffect
    data class OpenCharacter(val character: HomeCharacter) : HomeEffect
    data object AddCharacterRequested : HomeEffect
    data object OpenNotifications : HomeEffect
}

internal class HomeViewModel : MviViewModel<HomeUiState, HomeAction, HomeEffect>(HomeUiState()) {
    override fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.CreateStoryClicked -> sendEffect(HomeEffect.CreateStoryRequested)
            is HomeAction.StorySelected -> sendEffect(HomeEffect.OpenStory(action.story))
            HomeAction.SeeAllStoriesClicked -> sendEffect(HomeEffect.SeeAllStoriesRequested)
            is HomeAction.CharacterSelected -> sendEffect(HomeEffect.OpenCharacter(action.character))
            HomeAction.AddCharacterClicked -> sendEffect(HomeEffect.AddCharacterRequested)
            HomeAction.NotificationsClicked -> sendEffect(HomeEffect.OpenNotifications)
        }
    }
}
