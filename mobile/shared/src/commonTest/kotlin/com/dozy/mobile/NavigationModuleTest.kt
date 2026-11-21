package com.dozy.mobile

import com.dozy.mobile.feature.login.data.FakeLoginRepository
import com.dozy.mobile.feature.login.domain.repository.OnboardingPreferences
import com.dozy.mobile.feature.login.presentation.LoginUiState
import com.dozy.mobile.feature.login.presentation.LoginViewModel
import com.dozy.mobile.navigation.AppNavigator
import com.dozy.mobile.navigation.AppScreens
import com.dozy.mobile.navigation.NavigationHostOptions
import com.dozy.mobile.navigation.navigationModule
import com.kjursa.foundation.compose.mvi.StatePersistence
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class NavigationModuleTest {
    @Test
    fun resolvesRouteBindingsWithHostParametersAndOneNavigator() {
        val app = koinApplication { modules(navigationModule) }
        try {
            val navigator = app.koin.get<AppNavigator>()
            val login = LoginViewModel(navigator, MemoryLoginState(), FakeLoginRepository(), TestOnboardingPreferences())
            val options = NavigationHostOptions(null, false)
            val first = app.koin.get<AppScreens> { parametersOf(login, options) }
            val second = app.koin.get<AppScreens> { parametersOf(login, options) }
            assertNotSame(first, second)
            assertNotSame(first.places, second.places)
            assertSame(navigator, app.koin.get<AppNavigator>())
            assertSame(navigator, login.navigator)
        } finally {
            app.close()
        }
    }

    private class MemoryLoginState : StatePersistence<LoginUiState> {
        override fun restore(defaultState: LoginUiState) = defaultState

        override fun save(state: LoginUiState) = Unit
    }
}

private class TestOnboardingPreferences : OnboardingPreferences {
    override fun hasSeenTutorial() = false

    override fun markTutorialSeen() = Unit
}
