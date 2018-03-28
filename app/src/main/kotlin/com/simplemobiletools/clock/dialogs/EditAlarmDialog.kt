package com.simplemobiletools.clock.dialogs

import android.app.TimePickerDialog
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.support.v7.app.AlertDialog
import android.widget.TextView
import com.simplemobiletools.clock.R
import com.simplemobiletools.clock.activities.SimpleActivity
import com.simplemobiletools.clock.extensions.colorLeftDrawable
import com.simplemobiletools.clock.extensions.config
import com.simplemobiletools.clock.extensions.dbHelper
import com.simplemobiletools.clock.extensions.getFormattedTime
import com.simplemobiletools.clock.models.Alarm
import com.simplemobiletools.commons.extensions.*
import kotlinx.android.synthetic.main.dialog_edit_alarm.view.*

class EditAlarmDialog(val activity: SimpleActivity, val alarm: Alarm, val callback: () -> Unit) {
    private val view = activity.layoutInflater.inflate(R.layout.dialog_edit_alarm, null)
    private val textColor = activity.config.textColor

    init {
        updateAlarmTime()

        view.apply {
            edit_alarm_time.setOnClickListener {
                TimePickerDialog(context, context.getDialogTheme(), timeSetListener, alarm.timeInMinutes / 60, alarm.timeInMinutes % 60, context.config.use24HourFormat).show()
            }

            edit_alarm_sound.colorLeftDrawable(textColor)
            edit_alarm_sound.text = alarm.soundTitle
            edit_alarm_sound.setOnClickListener {
                SelectAlarmSoundDialog(activity, alarm.soundUri, AudioManager.STREAM_ALARM) {
                    if (it != null) {
                        alarm.soundTitle = it.title
                        alarm.soundUri = it.uri
                        edit_alarm_sound.text = it.title
                    }
                }
            }

            edit_alarm_vibrate.colorLeftDrawable(textColor)
            edit_alarm_vibrate.isChecked = alarm.vibrate
            edit_alarm_vibrate_holder.setOnClickListener {
                edit_alarm_vibrate.toggle()
                alarm.vibrate = edit_alarm_vibrate.isChecked
            }

            edit_alarm_label_image.applyColorFilter(textColor)
            edit_alarm_label.setText(alarm.label)

            val dayLetters = activity.resources.getStringArray(R.array.week_day_letters).toList() as ArrayList<String>
            val dayIndexes = arrayListOf(0, 1, 2, 3, 4, 5, 6)
            if (activity.config.isSundayFirst) {
                dayIndexes.moveLastItemToFront()
            }

            dayIndexes.forEach {
                val pow = Math.pow(2.0, it.toDouble()).toInt()
                val day = activity.layoutInflater.inflate(R.layout.alarm_day, edit_alarm_days_holder, false) as TextView
                day.text = dayLetters[it]

                val isDayChecked = alarm.days and pow != 0
                day.background = getProperDayDrawable(isDayChecked)

                day.setTextColor(if (isDayChecked) context.config.backgroundColor else textColor)
                day.setOnClickListener {
                    val selectDay = alarm.days and pow == 0
                    if (selectDay) {
                        alarm.days = alarm.days.addBit(pow)
                    } else {
                        alarm.days = alarm.days.removeBit(pow)
                    }
                    day.background = getProperDayDrawable(selectDay)
                    day.setTextColor(if (selectDay) context.config.backgroundColor else textColor)
                }

                edit_alarm_days_holder.addView(day)
            }
        }

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
                    activity.setupDialogStuff(view, this) {
                        getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                            if (alarm.days == 0) {
                                activity.toast(R.string.no_days_selected)
                                return@setOnClickListener
                            }

                            alarm.label = view.edit_alarm_label.value

                            if (alarm.id == 0) {
                                if (!activity.dbHelper.insertAlarm(alarm)) {
                                    activity.toast(R.string.unknown_error_occurred)
                                }
                            } else {
                                if (!activity.dbHelper.updateAlarm(alarm)) {
                                    activity.toast(R.string.unknown_error_occurred)
                                }
                            }
                            callback()
                            dismiss()
                        }
                    }
                }
    }

    private val timeSetListener = TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
        alarm.timeInMinutes = hourOfDay * 60 + minute
        updateAlarmTime()
    }

    private fun updateAlarmTime() {
        view.edit_alarm_time.text = activity.getFormattedTime(alarm.timeInMinutes * 60, false, true)
    }

    private fun getProperDayDrawable(selected: Boolean): Drawable {
        val drawableId = if (selected) R.drawable.circle_background_filled else R.drawable.circle_background_stroke
        val drawable = activity.resources.getDrawable(drawableId)
        drawable.applyColorFilter(textColor)
        return drawable
    }
}
