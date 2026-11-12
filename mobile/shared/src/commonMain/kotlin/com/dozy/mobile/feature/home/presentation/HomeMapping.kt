package com.dozy.mobile.feature.home.presentation

import com.dozy.mobile.feature.character.domain.model.Character

fun Character.toHomeCharacter() = HomeCharacter(details.name.value, id.value, avatar?.url?.value)
