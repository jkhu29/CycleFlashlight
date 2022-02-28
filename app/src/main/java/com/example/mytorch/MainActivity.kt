package com.example.mytorch

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.concurrent.timerTask
import kotlin.math.abs

class MainActivity : AppCompatActivity() {
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraId: String
    private lateinit var handler: Handler
    private lateinit var timerCycle: Timer

    private var isTorchOn = false
    private var isCycleOn = false
    private var isCycleTimeMade = false
    private var torchCyclePeriodP = 50L
    private var torchCyclePeriodV = 50L
    private var numP = 0
    private var numV = 0
    private var torchCyclePeriod = 1000L
    private var torchCycleBin = "1100"
    private var timeArr = LongArray(1)

    private val torchCallback = object: CameraManager.TorchCallback() {
        override fun onTorchModeUnavailable(camera_id: String) {
            // super.onTorchModeUnavailable(camera_id)
            if (camera_id == cameraId) {
                Toast.makeText(this@MainActivity, "Torch is not available", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onTorchModeChanged(camera_id: String, camera_enabled: Boolean) {
            // super.onTorchModeChanged(camera_id, camera_enabled)
            if (camera_id == cameraId) {
                isTorchOn = camera_enabled
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        cameraManager.unregisterTorchCallback(torchCallback)
        super.onDestroy()
    }

    private fun init() {
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        handler = Handler()
        cameraManager.registerTorchCallback(torchCallback, handler)

        // editText, get the cycle time
        // button, make sure the number is got
        // textView, display the number
        setCycleTime()

        val cameraIds = getCameraIds()
        if (cameraIds.isNullOrEmpty()) {
            Toast.makeText(this, "Flashlight is not available, please check your CameraIds", Toast.LENGTH_SHORT).show()
        } else {
            cameraId = cameraIds[0]
            btn_switch.setOnClickListener { toggleTorchCycle() }
        }
    }

    private fun setCycleTime() {
        // set Time P
        editTextP.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                isCycleTimeMade = false
            }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                isCycleTimeMade = false
            }
            override fun afterTextChanged(p0: Editable?) {
                val content:String = p0.toString()
                button.setOnClickListener {
                    isCycleTimeMade = true
                }
                if (
                    "" != content.trim { it <= ' ' }
                    && content.trim { it <= ' ' }.isNotEmpty()
                ) {
                    val timeP = content.toLong()
                    torchCyclePeriodP = if (timeP in 5..50) { timeP } else { 50L }
                    displayCycleTime()
                }
            }
        })

        // set Time V
        editTextV.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                isCycleTimeMade = false
            }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                isCycleTimeMade = false
            }
            override fun afterTextChanged(p0: Editable?) {
                val content:String = p0.toString()
                button.setOnClickListener {
                    isCycleTimeMade = true
                }
                if (
                    "" != content.trim { it <= ' ' }
                    && content.trim { it <= ' ' }.isNotEmpty()
                ) {
                    val timeV = content.toLong()
                    torchCyclePeriodV = if (timeV in 5..50) { timeV } else { 50L }
                    displayCycleTime()
                }
            }
        })

        // set Binary Code
        editTextBin.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                isCycleTimeMade = false
            }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                isCycleTimeMade = false
            }
            override fun afterTextChanged(p0: Editable?) {
                val content:String = p0.toString()
                button.setOnClickListener {
                    isCycleTimeMade = true
                }
                if (
                    "" != content.trim { it <= ' ' }
                    && content.trim { it <= ' ' }.isNotEmpty()
                ) {
                    torchCycleBin = content
                    torchCycleBin.filterNot {
                        it != '1' || it != '0'
                    }
                    numP = torchCycleBin.count{it == '1'}
                    numV = torchCycleBin.count{it == '0'}
                    if (numP + numV != 0)
                        torchCyclePeriod = (2 * torchCyclePeriodP + torchCyclePeriodV) * numP +
                                2 * torchCyclePeriodP * numV
                    displayCycleTime()
                }
            }
        })
    }

    @SuppressLint("SetTextI18n")
    private fun displayCycleTime() {
        textView.text = "P: $torchCyclePeriodP, V: $torchCyclePeriodV;\n" +
                "CycleTime: $torchCyclePeriod\n" +
                "NumP: $numP, NumV: $numV"
    }

    private fun toggleTorchCycle() {
        isCycleOn = !isCycleOn
        btn_switch.setBackgroundResource(
            if (isCycleOn) {
                R.drawable.torch_turn_on
            } else {
                R.drawable.torch_turn_off
            }
        )
        try {
            if (isCycleOn) {
                timerCycle = Timer()
                timeArr = LongArray(torchCycleBin.count() * 2 + 1)
                var cnt = 0
                timeArr[cnt] = 0
                for (i in torchCycleBin.indices) {
                    if (i > 0) {
                        if (torchCycleBin[i] == '0') {
                            timeArr[cnt+1] = timeArr[cnt] + torchCyclePeriodP
                            cnt += 1
                            timeArr[cnt+1] = timeArr[cnt] + torchCyclePeriodP
                            cnt += 1
                        } else {
                            if (torchCycleBin[i] == '1') {
                                timeArr[cnt+1] = timeArr[cnt] + torchCyclePeriodP + torchCyclePeriodV
                                cnt += 1
                                timeArr[cnt+1] = timeArr[cnt] + torchCyclePeriodP
                                cnt += 1
                            }
                        }
                    }
                }
                val timerCycleTask: TimerTask = object : TimerTask() {
                    override fun run() {
                        val time = System.currentTimeMillis()
                        for (i in timeArr.indices) {
                            while (true) {
                                if (abs(System.currentTimeMillis() - timeArr[i] - time) <= 2L) {
                                    toggleTorch()
                                    break
                                } else {
                                    if (System.currentTimeMillis() > torchCyclePeriod + time + 2L)
                                        break
                                }
                            }
                        }

                        if (isTorchOn) {
                            toggleTorch()
                        }
                    }
                }
                timerCycle.schedule(timerCycleTask, 0, torchCyclePeriod)
                Thread.sleep(10L)
            }
            else {
                timerCycle.cancel()
                timerCycle.purge()
                if (isTorchOn)
                    toggleTorch()
            }
        } catch (exception: CameraAccessException) {
            Toast.makeText(this, "Cycle Switch is not OK", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleTorch() {
        try {
            cameraManager.setTorchMode(cameraId, !isTorchOn)
        } catch (exception: CameraAccessException) {
            Toast.makeText(this, getToggleTorchErrorMessage(), Toast.LENGTH_SHORT).show()
        }
    }

    private fun getToggleTorchErrorMessage(): String {
        return if (isTorchOn) "Cannot Turn Off" else "Cannot Turn On"
    }

    private fun getCameraIds(): Array<String> {
        return try {
            cameraManager.cameraIdList
        } catch (exception: CameraAccessException) {
            arrayOf()
        }
    }

}