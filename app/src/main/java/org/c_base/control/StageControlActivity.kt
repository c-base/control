package org.c_base.control

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.json.JSONObject

class StageControlActivity : AppCompatActivity() {

    val DMX_TOPIC = "dmx-mainhall/state"
    val broker = "tcp://c-beam:1883"
    val clientId = "control"
    val qos = 2
    val mqttClient by lazy { MqttClient(broker, clientId, MemoryPersistence()) }

    var currentStageState = false

    val initialStateMap = HashMap<String, Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        stage_light_button.setOnClickListener {


            currentStageState = !currentStageState

            try {
                val msg = initialStateMap.keys.map {
                    val newValue = if (currentStageState) 0 else initialStateMap[it]
                    return@map "{\"channel_id\":\"$it\",\"value\":$newValue}"
                }.joinToString(",")

                val message = MqttMessage("[$msg]".toByteArray())
                message.qos = qos
                mqttClient.publish(DMX_TOPIC, message)
            } catch (me: MqttException) {
                me.printStackTrace()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        try {
            val connOpts = MqttConnectOptions()
            connOpts.isCleanSession = true
            mqttClient.connect(connOpts)
            mqttClient.subscribe("dmx-mainhall/current_state") { _, message ->
                if (initialStateMap.isEmpty()) {
                    val jsonObject = JSONObject(message.toString())
                    jsonObject.keys().forEach {
                        initialStateMap[it] = jsonObject.getInt(it)
                    }
                }
            }
        } catch (me: MqttException) {

            me.printStackTrace()
        }

    }

    override fun onPause() {
        super.onPause()

        try {
            mqttClient.disconnect()
        } catch (me: MqttException) {
            me.printStackTrace()
        }
    }
}
