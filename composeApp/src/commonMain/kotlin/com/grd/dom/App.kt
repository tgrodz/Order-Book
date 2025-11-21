package com.grd.dom

import androidx.compose.foundation.background
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grd.dom.auth.AuthSession
import com.grd.dom.ui.Authscreen
import com.grd.dom.ui.TradingScreen

private enum class AppDestination { Auth, Trading }

@Composable
fun App(modifier: Modifier = Modifier) {
    MaterialTheme {
        Surface(modifier = modifier) {
            var session by remember { mutableStateOf<AuthSession?>(null) }
            var destination by rememberSaveable { mutableStateOf(AppDestination.Auth) }

            Crossfade(targetState = destination, modifier = Modifier.fillMaxSize()) { target ->
                when (target) {
                    AppDestination.Auth -> Authscreen(
                        modifier = Modifier.fillMaxSize(),
                        onAuthenticated = { authSession ->
                            session = authSession
                            destination = AppDestination.Trading
                        },
                        onSkip = { destination = AppDestination.Trading }
                    )

                    AppDestination.Trading -> TradingSessionContainer(
                        session = session,
                        onLogout = {
                            session = null
                            destination = AppDestination.Auth
                        },
                        onRemoteToggleAttempt = { desiredUseLocal ->
                            if (!desiredUseLocal && session == null) {
                                destination = AppDestination.Auth
                                false
                            } else {
                                true
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TradingSessionContainer(
    session: AuthSession?,
    onLogout: () -> Unit,
    onRemoteToggleAttempt: (Boolean) -> Boolean
) {
    TradingScreen(
        modifier = Modifier.fillMaxSize(),
        onRemoteToggleRequest = onRemoteToggleAttempt,
        forceLocalMode = session == null,
        loggedInLabel = session?.let { "Logged in as ${it.username}" },
        tokenLabel = session?.token,
        onLogoutClick = onLogout
    )
}

