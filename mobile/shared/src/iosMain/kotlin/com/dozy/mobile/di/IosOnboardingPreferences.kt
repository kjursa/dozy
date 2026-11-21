package com.dozy.mobile.di

import com.dozy.mobile.feature.login.domain.repository.OnboardingPreferences
import platform.Foundation.NSUserDefaults

internal class IosOnboardingPreferences : OnboardingPreferences {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun hasSeenTutorial() = defaults.boolForKey("dozy-tutorial-seen")

    override fun markTutorialSeen() {
        defaults.setBool(true, "dozy-tutorial-seen")
    }
}
