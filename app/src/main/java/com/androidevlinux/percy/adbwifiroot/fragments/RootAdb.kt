package com.androidevlinux.percy.adbwifiroot.fragments

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import com.androidevlinux.percy.adbwifiroot.R
import com.androidevlinux.percy.adbwifiroot.activities.MainActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.root_adb_fragment.*
import java.io.IOException
import java.io.OutputStreamWriter


/**
 * Created by percy on 04/11/2017.
 */

class RootAdb : Fragment() {
    private val txtTitle by lazy { activity!!.findViewById<View>(R.id.txtTitle) as AppCompatTextView }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for fragment
        return inflater.inflate(R.layout.root_adb_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        turn_on_adb_wifi.setOnCheckedChangeListener{ _, isChecked ->
            if (checkWiFi()) {
                if (isChecked) {
                    enableWifiAdb(isChecked)
                    changeState()
                    Snackbar.make(turn_on_adb_wifi, "ADB WiFi Turned On", Snackbar.LENGTH_SHORT).show()
                } else {
                    enableWifiAdb(isChecked)
                    changeState()
                    Snackbar.make(turn_on_adb_wifi, "ADB WiFi Turned Off", Snackbar.LENGTH_SHORT).show()
                }
            } else{
                turn_on_adb_wifi.isChecked = false
                Snackbar.make(turn_on_adb_wifi, "Connect To WiFi First", Snackbar.LENGTH_SHORT).show()
            }
        }
        changeState()
        txtTitle.text = activity?.resources?.getString(R.string.adb_root)
    }

    private fun changeState() {
        if (turn_on_adb_wifi.isChecked) txt_ip.text =  resources.getText(R.string.adb_connect).toString().plus(" " + getIP() + ":5555") else {
            txt_ip.text = resources.getText(R.string.adb_connect_0)
        }
        notify(txt_ip.text.toString())
    }

    private fun notify(toString: String) {
        val builder = Notification.Builder(activity)
        val notificationIntent = Intent(activity, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(activity, 0, notificationIntent, 0)
        builder.setSmallIcon(R.drawable.ic_stat_adb)
                .setContentTitle(toString)
                .setContentIntent(pendingIntent)
        val notificationManager = activity?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        @Suppress("DEPRECATION")
        val notification = builder.notification
        notificationManager.notify(R.drawable.ic_stat_adb, notification)
    }

    private fun enableWifiAdb(enable: Boolean): Boolean {
        var process: Process? = null
        var os: OutputStreamWriter? = null

        try {
            process = Runtime.getRuntime().exec("su")
            os = OutputStreamWriter(process!!.outputStream)
            os.write("setprop service.adb.tcp.port " + (if (enable) "5555" else "-1") + "\n")
            os.write("stop adbd\n")
            os.write("start adbd\n")
            os.write("exit\n")
            os.flush()
            process.waitFor()
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        } catch (e: InterruptedException) {
            e.printStackTrace()
            return false
        } finally {
            try {
                os?.close()
                process?.destroy()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
        return true
    }

    private fun checkWiFi(): Boolean {
        val cm = activity?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        if (activeNetwork != null) { // connected to the internet
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (cm.getNetworkCapabilities(cm.activeNetwork).hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    // connected to wifi
                    //Snackbar.make(turn_on_adb_wifi, activeNetwork.typeName, Snackbar.LENGTH_SHORT).show()
                    return true
                } else if (cm.getNetworkCapabilities(cm.activeNetwork).hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    // connected to the mobile provider's data plan
                    //Snackbar.make(turn_on_adb_wifi, activeNetwork.typeName, Snackbar.LENGTH_SHORT).show()
                    return false
                }
            } else {
                if (activeNetwork.type == ConnectivityManager.TYPE_WIFI) {
                    // connected to wifi
                    //Snackbar.make(turn_on_adb_wifi, activeNetwork.typeName, Snackbar.LENGTH_SHORT).show()
                    return true
                } else if (activeNetwork.type == ConnectivityManager.TYPE_MOBILE) {
                    // connected to the mobile provider's data plan
                    //Snackbar.make(turn_on_adb_wifi, activeNetwork.typeName, Snackbar.LENGTH_SHORT).show()
                    return false
                }
            }
        } else {
            // not connected to the internet
            return false
        }
        return false
    }

    private fun getIP(): String {
        val wifiMgr = activity?.applicationContext?.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiMgr.connectionInfo
        val ip = wifiInfo.ipAddress
        return String.format("%d.%d.%d.%d", ip and 0xff, ip shr 8 and 0xff, ip shr 16 and 0xff, ip shr 24 and 0xff)
    }
}
