package com.dozy.mobile.presentation.home

import com.dozy.mobile.R
import com.dozy.mobile.domain.character.Character

internal fun Character.toHomeCharacter() = HomeCharacter(details.name.value, R.drawable.character_oliver)
