package com.example.qceqapp.uis.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qceqapp.data.model.Entities
import com.example.qceqapp.data.network.Service
import kotlinx.coroutines.launch
import java.net.UnknownHostException
import java.net.SocketTimeoutException
import retrofit2.HttpException

class LoginViewModel : ViewModel() {

    private val service = Service()

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    private val _loginSuccess = MutableLiveData<Entities.LoginResponse?>()
    val loginSuccess: LiveData<Entities.LoginResponse?> get() = _loginSuccess

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _errorMessage.value = "Please enter both username and password."
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val result = service.userLogin(username, password)

                result.onSuccess { response ->
                    _isLoading.value = false
                    _loginSuccess.value = response
                }.onFailure { error ->
                    _isLoading.value = false
                    _errorMessage.value = mapError(error)
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = mapError(e)
            }
        }
    }

    private fun mapError(e: Throwable): String {
        return when (e) {
            is UnknownHostException -> "No internet connection. Please check your network."
            is SocketTimeoutException -> "Server is taking too long to respond. Try again later."
            is HttpException -> when (e.code()) {
                400 -> "Invalid request. Please check your data."
                401 -> "Incorrect username or password."
                403 -> "Access denied. Contact support."
                404 -> "Service unavailable. Please try again later."
                in 500..599 -> "Server error. Please try again in a few minutes."
                else -> "Unexpected server error (${e.code()})."
            }
            else -> e.message ?: "An unexpected error occurred."
        }
    }
}
