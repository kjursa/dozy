package com.dozy.mobile.feature.character.presentation.list

import com.dozy.mobile.R
import com.dozy.mobile.feature.character.domain.model.Character

internal fun Character.toCharacterListItem() = CharacterListItem(details.name.value, R.drawable.character_oliver)
