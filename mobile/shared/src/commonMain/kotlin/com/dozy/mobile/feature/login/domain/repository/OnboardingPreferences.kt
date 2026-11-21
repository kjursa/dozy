package com.dozy.mobile.feature.login.domain.repository

interface OnboardingPreferences {
    fun hasSeenTutorial(): Boolean

    fun markTutorialSeen()
}
