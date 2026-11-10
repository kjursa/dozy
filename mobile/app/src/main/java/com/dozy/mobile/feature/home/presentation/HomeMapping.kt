package com.dozy.mobile.feature.home.presentation

import com.dozy.mobile.R
import com.dozy.mobile.feature.character.domain.Character

internal fun Character.toHomeCharacter() = HomeCharacter(details.name.value, R.drawable.character_oliver)
