
package com.example.celltracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.*
import android.telephony.*
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var telephonyManager: TelephonyManager
    private val handler = Handler(Looper.getMainLooper())
    private val interval: Long = 10000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager

        ActivityCompat.requestPermissions(this, arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        ), 1)

        startRepeatingTask()
    }

    private fun startRepeatingTask() {
        handler.post(object : Runnable {
            override fun run() {
                fetchAndSendCellInfo()
                handler.postDelayed(this, interval)
            }
        })
    }

    private fun fetchAndSendCellInfo() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val cellInfoList = telephonyManager.allCellInfo
        for (cellInfo in cellInfoList) {
            if (cellInfo is CellInfoLte) {
                val cid = cellInfo.cellIdentity.ci
                val mnc = cellInfo.cellIdentity.mnc
                val mcc = cellInfo.cellIdentity.mcc
                val rsrp = cellInfo.cellSignalStrength.rsrp

                val json = JSONObject().apply {
                    put("cell_id", cid)
                    put("mnc", mnc)
                    put("mcc", mcc)
                    put("rsrp", rsrp)
                }

                sendToFlaskServer(json)
                break
            }
        }
    }

    private fun sendToFlaskServer(data: JSONObject) {
        val url = "http://192.168.0.118:5000/upload"
        val request = JsonObjectRequest(
            com.android.volley.Request.Method.POST,
            url,
            data,
            { response -> Log.d("HTTP", "Success: $response") },
            { error -> Log.e("HTTP", "Error: $error") }
        )
        Volley.newRequestQueue(this).add(request)
    }
}
