package com.tomk322.rootscope.scan

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.tomk322.rootscope.model.Row
import com.tomk322.rootscope.model.Section
import com.tomk322.rootscope.model.Tone
import java.util.Locale

/** Power state, read from the sticky battery broadcast plus BatteryManager properties. */
object BatteryScanner {

    private const val PERCENT = 100
    private const val DECI_DEGREES = 10f
    private const val HOT_DECI_CELSIUS = 450
    private const val MICRO_PER_MILLI = 1000

    fun scan(context: Context): Section {
        // ACTION_BATTERY_CHANGED is sticky, so a null receiver returns the last broadcast
        // synchronously - no registration, no permission.
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val manager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        fun extra(key: String) = intent?.getIntExtra(key, -1) ?: -1

        val level = extra(BatteryManager.EXTRA_LEVEL)
        val scale = extra(BatteryManager.EXTRA_SCALE)
        val percent = if (level >= 0 && scale > 0) level * PERCENT / scale else -1
        val tempDeci = extra(BatteryManager.EXTRA_TEMPERATURE)
        val voltage = extra(BatteryManager.EXTRA_VOLTAGE)
        val health = extra(BatteryManager.EXTRA_HEALTH)
        val currentNow = runCatching {
            manager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        }.getOrNull()

        return Section(
            title = "Battery",
            subtitle = if (percent >= 0) "$percent%" else "",
            rows = listOfNotNull(
                Row("Charge", if (percent >= 0) "$percent%" else "unknown", Tone.ACCENT),
                Row("Status", statusLabel(extra(BatteryManager.EXTRA_STATUS))),
                Row(
                    "Health",
                    healthLabel(health),
                    if (health == BatteryManager.BATTERY_HEALTH_GOOD) Tone.GOOD else Tone.WARN,
                ),
                Row("Power source", pluggedLabel(extra(BatteryManager.EXTRA_PLUGGED))),
                tempDeci.takeIf { it > 0 }?.let {
                    Row(
                        "Temperature",
                        String.format(Locale.US, "%.1f C", it / DECI_DEGREES),
                        if (it > HOT_DECI_CELSIUS) Tone.WARN else Tone.GOOD,
                    )
                },
                voltage.takeIf { it > 0 }?.let { Row("Voltage", "$it mV") },
                currentNow?.let { Row("Current now", "${it / MICRO_PER_MILLI} mA") },
                intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)?.let { Row("Technology", it) },
            ),
        )
    }

    private fun statusLabel(value: Int) = when (value) {
        BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
        BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
        BatteryManager.BATTERY_STATUS_FULL -> "Full"
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not charging"
        else -> "Unknown"
    }

    private fun healthLabel(value: Int) = when (value) {
        BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheating"
        BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over voltage"
        BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Unspecified failure"
        else -> "Unknown"
    }

    private fun pluggedLabel(value: Int) = when (value) {
        BatteryManager.BATTERY_PLUGGED_AC -> "AC"
        BatteryManager.BATTERY_PLUGGED_USB -> "USB"
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
        0 -> "Battery"
        else -> "Unknown"
    }
}
