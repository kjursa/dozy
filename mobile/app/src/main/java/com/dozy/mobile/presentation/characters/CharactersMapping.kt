package com.dozy.mobile.presentation.characters

import com.dozy.mobile.R
import com.dozy.mobile.domain.character.Character

internal fun Character.toCharacterListItem() = CharacterListItem(details.name.value, R.drawable.character_oliver)
