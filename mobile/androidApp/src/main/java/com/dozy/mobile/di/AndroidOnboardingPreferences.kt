package com.dozy.mobile.di

import android.content.Context
import com.dozy.mobile.feature.login.domain.repository.OnboardingPreferences

internal class AndroidOnboardingPreferences(context: Context) : OnboardingPreferences {
    private val preferences = context.getSharedPreferences("dozy-onboarding", Context.MODE_PRIVATE)

    override fun hasSeenTutorial() = preferences.getBoolean("tutorialSeen", false)

    override fun markTutorialSeen() {
        preferences.edit().putBoolean("tutorialSeen", true).apply()
    }
}
