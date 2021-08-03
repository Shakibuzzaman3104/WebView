package com.saeverything.saeverything

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.android.material.snackbar.Snackbar
import com.saeverything.saeverything.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var doubleBackToExitPressedOnce = false
    companion object {
        var netState: NetState? = null
    }
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val networkMonitor = NetworkMonitor()
        val connectivityManager =
                getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        try {
            connectivityManager.unregisterNetworkCallback(networkMonitor)
        } catch (e: java.lang.Exception) {
            Log.d(
                    "tester",
                    "NetworkCallback for Wi-fi was not registered or already unregistered"
            )
        }
        val network = NetworkRequest.Builder().addTransportType(
                NetworkCapabilities.TRANSPORT_WIFI
        ).addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR).build()
        connectivityManager.registerNetworkCallback(network, networkMonitor)

        //showPdf()

        if (!isOnline())
            Toast.makeText(applicationContext, "No Internet connection !!", Toast.LENGTH_LONG).show()

        binding.refresh.setOnRefreshListener {
            binding.webView.reload()
            binding.refresh.isRefreshing = false
        }


    }

    @Suppress("DEPRECATION")
    fun isOnline(): Boolean {
        val connectivityManager =
                applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }


    @SuppressLint("SetJavaScriptEnabled")
    private fun showWebsite() {
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.builtInZoomControls = true
        binding.webView.settings.displayZoomControls = false
        binding.webView.isVerticalScrollBarEnabled = false
        binding.webView.isHorizontalScrollBarEnabled = false

        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                binding.progress.visibility = View.VISIBLE
                binding.webView.loadUrl(
                        "javascript:(function() { " +
                                "document.querySelector('[role=\"toolbar\"]').remove();})()"
                )
                binding.webView.setOnLongClickListener { true }
                binding.webView.isLongClickable = false
                binding.webView.isHapticFeedbackEnabled = false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                binding.webView.loadUrl(
                        "javascript:(function() { " +
                                "document.querySelector('[role=\"toolbar\"]').remove();})()"
                )
                binding.progress.visibility = View.INVISIBLE
                super.onPageFinished(view, url)
            }

            override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
            ) {
                view?.loadUrl("about:blank");
                Toast.makeText(
                        applicationContext,
                        "Error occurred, please check network connectivity",
                        Toast.LENGTH_SHORT
                ).show()
                binding.progress.visibility = View.INVISIBLE
                super.onReceivedError(view, request, error)
            }

        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    inner class NetworkMonitor : ConnectivityManager.NetworkCallback() {
        override fun onLost(network: Network) {
            super.onLost(network)
            netState = NetState.LOST
            showSnackBar()
        }

        override fun onUnavailable() {
            showSnackBar()
            netState = NetState.UNAVAILABLE
            super.onUnavailable()
        }

        override fun onLosing(network: Network, maxMsToLive: Int) {
            super.onLosing(network, maxMsToLive)
            netState = NetState.LOSING
        }

        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            netState = NetState.AVAILABLE
            binding.webView.post {
                showWebsite()
                binding.webView.loadUrl("Your website here")
            }
        }
    }

    fun showSnackBar() {
        val snackbar = Snackbar
                .make(binding.webView, "No Internet !! Try again ", Snackbar.LENGTH_LONG)
                .setAction(
                        "RETRY"
                ) { view: View? -> }
        snackbar.setActionTextColor(Color.RED)
        snackbar.show()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    if (binding.webView.canGoBack()) {
                        binding.webView.goBack()
                    } else {
                        if (doubleBackToExitPressedOnce) {
                            super.onBackPressed()
                            return false
                        }

                        this.doubleBackToExitPressedOnce = true
                        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show()

                        Handler().postDelayed(Runnable { doubleBackToExitPressedOnce = false }, 2000)
                    }
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }


   /* override fun onBackPressed() {

    }*/

}