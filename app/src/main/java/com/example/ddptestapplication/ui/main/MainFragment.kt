package com.example.ddptestapplication.ui.main

import android.app.AlertDialog
import android.content.Context
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.ddptestapplication.R
import im.delight.android.ddp.Meteor
import kotlinx.android.synthetic.main.main_fragment.*

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(
            this,
            MainViewModelFactory(requireActivity(), "ws://192.168.1.1/websocket")
        ).get(MainViewModel::class.java)

        viewModel.connected.observe(this, Observer {
            message.text = if (it) "connected" else "disconnected"
            if (it) {
                loginButton.isEnabled = true
            }
        })

        viewModel.isLoggedIn.observe(this, Observer {
            message.text = if (it) "logged in with ${viewModel.user.value?.id}" else "Not logged in"
            if (it) startChatButton.isEnabled = true
        })

        viewModel.isChat.observe(this, Observer {
            message.text = if (it) "Chat in started " else "Not yet"
            if (it) sendMessageButton.isEnabled = true
        })

        viewModel.error.observe(this, Observer {
            showErrorAlertDialog(it)
        })

        viewModel.connect()

        loginButton.setOnClickListener {
            viewModel.login("test@rice.com", "123456")
        }

        startChatButton.setOnClickListener {
            viewModel.startChat("dTsuuQZimJtBnK7cN")
        }

        sendMessageButton.setOnClickListener {
            viewModel.sendMessage("CpgFZFDBaPk9tkbQA", "test")
        }
    }

    fun showErrorAlertDialog(e: Exception) {
        AlertDialog.Builder(requireActivity())
            .setTitle("Error")
            .setMessage(e.message)
            .setPositiveButton("Ok") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .create().show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.logoutAndDisconnect()
    }

    class MainViewModelFactory(val context: Context, val url: String) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return MainViewModel(
                Meteor(context, url),
                context.getSharedPreferences("DEFAULT", Context.MODE_PRIVATE)
            ) as T
        }
    }
}