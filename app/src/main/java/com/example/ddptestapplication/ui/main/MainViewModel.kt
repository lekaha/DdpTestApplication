package com.example.ddptestapplication.ui.main

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.GsonBuilder
import im.delight.android.ddp.Meteor
import im.delight.android.ddp.MeteorCallback
import im.delight.android.ddp.ResultListener
import im.delight.android.ddp.SubscribeListener
import java.lang.Exception
import java.lang.IllegalArgumentException

class MainViewModel(val meteor: Meteor, val preferences: SharedPreferences) : ViewModel(), MeteorCallback {
    private val gson = GsonBuilder().create()
    private val messageSubscribeListener = object: SubscribeListener {
        override fun onSuccess() {
            Log.d("MainViewModel", "messageSubscribeListener onSuccess")
            isChat.value = true
        }

        override fun onError(error: String?, reason: String?, details: String?) {
            Log.d("MainViewModel", "messageSubscribeListener onError")
            isChat.value = false
        }
    }

    val connected: MutableLiveData<Boolean> = MutableLiveData(false)
    val isLoggedIn: MutableLiveData<Boolean> = MutableLiveData(false)
    val isChat: MutableLiveData<Boolean> = MutableLiveData(false)
    val user: MutableLiveData<User> = MutableLiveData()
    val error: MutableLiveData<Exception> = MutableLiveData()

    init {
        meteor.addCallback(this)
    }

    fun connect() {
        meteor.connect()
    }

    fun logoutAndDisconnect() {
        meteor.logout()
        meteor.disconnect()
    }

    fun startChat(userId: String) {
        meteor.call("startChat", arrayOf(userId), object: ResultListener {
            override fun onSuccess(result: String?) {
                Log.d("MainViewModel", result)
                val message = gson.fromJson(result, Message::class.java)
                meteor.subscribe("messages", arrayOf(message._id, 0), messageSubscribeListener)
            }

            override fun onError(e: String?, reason: String?, details: String?) {
                error.value = IllegalArgumentException("$e: $reason")
            }
        })
    }

    fun sendMessage(chatId: String, message: String) {
        meteor.call("addMessage", arrayOf("text", chatId, message), object: ResultListener {
            override fun onSuccess(result: String?) {
                Log.d("MainViewModel", result)
            }

            override fun onError(e: String?, reason: String?, details: String?) {
                error.value = IllegalArgumentException("$e: $reason")
            }
        })
    }

    fun login(email: String, password: String) {
        meteor.loginWithEmail(email, password, object: ResultListener {
            override fun onSuccess(result: String?) {
                result?.apply {
                    val userObj = gson.fromJson(result, User::class.java)
                    user.value = userObj
                    preferences.edit().putString(KEY_USER_TOKEN, userObj.token).apply()
                }
                isLoggedIn.value = true
            }

            override fun onError(e: String?, reason: String?, details: String?) {
                error.value = IllegalArgumentException("$e: $reason")
                isLoggedIn.value = false
            }
        })
    }

    override fun onConnect(signedInAutomatically: Boolean) {
        connected.value = true
    }

    override fun onDataAdded(collectionName: String?, documentID: String?, newValuesJson: String?) {

    }

    override fun onDataRemoved(collectionName: String?, documentID: String?) {

    }

    override fun onException(e: Exception?) {
        e?.let {
            error.value = it
        }
    }

    override fun onDisconnect() {
        connected.value = false
    }

    override fun onDataChanged(
        collectionName: String?,
        documentID: String?,
        updatedValuesJson: String?,
        removedValuesJson: String?
    ) {
        Log.d("MainViewModel", "$collectionName ($documentID): $updatedValuesJson")
    }

    companion object {
        const val KEY_USER_TOKEN = "KEY_USER_TOKEN"
    }
}

data class User(
    val id: String,
    val token: String,
//    val tokenExpires: String,
    val type: String
)

data class Message(
    val _id: String,
    val memberIds: List<String>
)