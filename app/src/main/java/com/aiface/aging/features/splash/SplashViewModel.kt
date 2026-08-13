package com.aiface.aging.features.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiface.aging.data.local.PreferenceManager
import com.aiface.aging.domain.model.RegisterResult
import com.aiface.aging.domain.model.Resource
import com.aiface.aging.domain.usecase.GetAccessTokenUseCase
import com.aiface.aging.domain.usecase.RegisterUserUseCase
import com.aiface.aging.shared.Constants
import com.aiface.aging.utils.DeviceIdManager
import com.aiface.aging.utils.TokenExpiryParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class RegisterState {
    data object Idle : RegisterState()
    data object Loading : RegisterState()
    /** Existing local token is still valid — no network auth call. */
    data object AlreadyRegistered : RegisterState()
    /** Fresh register or Get Token succeeded. */
    data object Success : RegisterState()
    data class Error(val message: String) : RegisterState()
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val preferenceManager: PreferenceManager,
    private val registerUserUseCase: RegisterUserUseCase,
    private val getAccessTokenUseCase: GetAccessTokenUseCase,
    private val deviceIdManager: DeviceIdManager,
) : ViewModel() {

    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState.asStateFlow()

    private var authJob: Job? = null

    fun isRegistrationComplete(): Boolean {
        return _registerState.value is RegisterState.Success ||
            _registerState.value is RegisterState.AlreadyRegistered
    }

    /**
     * Splash entry:
     * 1) No token → Register
     * 2) Valid token → reuse (no API)
     * 3) Expired token → Get Token (fallback Register if user missing)
     */
    fun ensureAccessToken(force: Boolean = false) {
        if (
            !force &&
            (_registerState.value is RegisterState.Success ||
                _registerState.value is RegisterState.AlreadyRegistered)
        ) {
            return
        }
        if (!force && _registerState.value is RegisterState.Loading) return

        authJob?.cancel()
        authJob = viewModelScope.launch {
            try {
                // APP_NAME switch (e.g. face-aging → beauty_camera) invalidates old session.
                val storedAppName = preferenceManager.appName.first()
                if (!storedAppName.isNullOrBlank() &&
                    storedAppName != Constants.APP_NAME
                ) {
                    preferenceManager.clearAuthSession()
                }

                if (!force && preferenceManager.hasValidAccessToken()) {
                    _registerState.value = RegisterState.AlreadyRegistered
                    return@launch
                }

                _registerState.value = RegisterState.Loading
                val deviceId = deviceIdManager.getDeviceId()
                val existingToken = preferenceManager.userToken.first()

                val result = if (existingToken.isNullOrBlank()) {
                    registerWithRetry(deviceId)
                } else {
                    // Token present but expired (or force) → Get Token first.
                    refreshTokenWithFallbackRegister(deviceId)
                }

                when (result) {
                    is AuthOutcome.Success -> {
                        persistSession(result.data)
                        _registerState.value = RegisterState.Success
                    }
                    is AuthOutcome.ValidLocal -> {
                        _registerState.value = RegisterState.AlreadyRegistered
                    }
                    is AuthOutcome.Failure -> {
                        _registerState.value = RegisterState.Error(result.message)
                    }
                }
            } catch (e: Exception) {
                _registerState.value =
                    RegisterState.Error(e.message ?: "Unexpected auth error")
            }
        }
    }

    /** @deprecated Prefer [ensureAccessToken]. Kept for call-site compatibility. */
    fun registerUser(force: Boolean = false) = ensureAccessToken(force)

    fun retryRegistration() {
        ensureAccessToken(force = true)
    }

    private suspend fun registerWithRetry(deviceId: String): AuthOutcome {
        var lastError = "Registration failed"
        repeat(MAX_AUTH_ATTEMPTS) { attempt ->
            when (val outcome = collectAuthCall { registerUserUseCase(deviceId) }) {
                is AuthOutcome.Success -> {
                    return outcome
                }
                is AuthOutcome.Failure -> {
                    lastError = outcome.message
                    if (isUserAlreadyExists(outcome.message)) {
                        // Device already registered → Get Token instead of failing.
                        when (val refresh = collectAuthCall { getAccessTokenUseCase(deviceId) }) {
                            is AuthOutcome.Success -> return refresh
                            is AuthOutcome.Failure -> lastError = refresh.message
                            is AuthOutcome.ValidLocal -> return refresh
                        }
                    }
                }
                is AuthOutcome.ValidLocal -> return outcome
            }
            if (attempt < MAX_AUTH_ATTEMPTS - 1) {
                delay(AUTH_RETRY_DELAY_MS * (attempt + 1))
            }
        }
        return AuthOutcome.Failure(lastError)
    }

    private suspend fun refreshTokenWithFallbackRegister(deviceId: String): AuthOutcome {
        var lastError = "Failed to get access token"
        repeat(MAX_AUTH_ATTEMPTS) { attempt ->
            when (val refresh = collectAuthCall { getAccessTokenUseCase(deviceId) }) {
                is AuthOutcome.Success -> return refresh
                is AuthOutcome.Failure -> {
                    lastError = refresh.message
                    if (isUserNotFound(refresh.message)) {
                        return registerWithRetry(deviceId)
                    }
                }
                is AuthOutcome.ValidLocal -> return refresh
            }
            if (attempt < MAX_AUTH_ATTEMPTS - 1) {
                delay(AUTH_RETRY_DELAY_MS * (attempt + 1))
            }
        }
        // Last resort: clear stale session and register again.
        preferenceManager.clearAuthSession()
        return when (val registered = registerWithRetry(deviceId)) {
            is AuthOutcome.Success,
            is AuthOutcome.ValidLocal,
            -> registered
            is AuthOutcome.Failure -> AuthOutcome.Failure(lastError)
        }
    }

    private suspend fun collectAuthCall(
        call: suspend () -> kotlinx.coroutines.flow.Flow<Resource<RegisterResult>>,
    ): AuthOutcome {
        var outcome: AuthOutcome = AuthOutcome.Failure("Empty auth response")
        call().collect { result ->
            when (result) {
                is Resource.Success -> {
                    val data = result.data
                    outcome = if (data != null && data.token.isNotBlank()) {
                        AuthOutcome.Success(data)
                    } else {
                        AuthOutcome.Failure("Empty token in response")
                    }
                }
                is Resource.Error -> {
                    outcome = AuthOutcome.Failure(result.message ?: "Auth request failed")
                }
                is Resource.Loading -> {
                    _registerState.value = RegisterState.Loading
                }
            }
        }
        return outcome
    }

    private suspend fun persistSession(data: RegisterResult) {
        val expiresAt = data.expiresAtMillis
            ?: TokenExpiryParser.resolveExpiresAtMillis(null, null)
        preferenceManager.saveAuthSession(
            token = data.token,
            userId = data.userId,
            deviceId = data.deviceId,
            appName = data.appName,
            expiresAtMillis = expiresAt,
        )
    }

    private fun isUserAlreadyExists(message: String?): Boolean {
        val m = message?.lowercase().orEmpty()
        return m.contains("already exists") ||
            m.contains("already registered") ||
            m.contains("user exists") ||
            m.contains("duplicate")
    }

    private fun isUserNotFound(message: String?): Boolean {
        val m = message?.lowercase().orEmpty()
        return m.contains("not found") ||
            m.contains("no user") ||
            m.contains("does not exist") ||
            m.contains("unknown user") ||
            m.contains("404")
    }

    private sealed class AuthOutcome {
        data class Success(val data: RegisterResult) : AuthOutcome()
        data object ValidLocal : AuthOutcome()
        data class Failure(val message: String) : AuthOutcome()
    }

    companion object {
        private const val MAX_AUTH_ATTEMPTS = 3
        private const val AUTH_RETRY_DELAY_MS = 1_500L
    }
}
