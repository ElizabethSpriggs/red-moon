/*
 * Copyright (c) 2016  Marien Raat <marienraat@riseup.net>
 * Copyright (c) 2017  Stephen Michel <s@smichel.me>
 * SPDX-License-Identifier: GPL-3.0+
 */
package com.jmstudios.redmoon.filter

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat

import com.jmstudios.redmoon.R

import com.jmstudios.redmoon.MainActivity
import com.jmstudios.redmoon.securesuspend.CurrentAppMonitor
import com.jmstudios.redmoon.securesuspend.WhitelistChangeReceiver
import com.jmstudios.redmoon.util.*


class Notification(
        private val context: Context,
        private val appMonitor: CurrentAppMonitor) {
    fun build(isOn: Boolean) : Notification {
        return NotificationCompat.Builder(context).apply {
            // Set notification appearance
            setSmallIcon(R.drawable.notification_icon_half_moon)
            color    = ContextCompat.getColor(appContext, R.color.color_primary)
            priority = Notification.PRIORITY_MIN

            if (belowAPI(24)) { setContentTitle(getString(R.string.app_name)) }
            setSubText(activeProfile.name)

            // Open Red Moon when tapping notification body
            val mainIntent = intent(MainActivity::class).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            setContentIntent(activityPI(ACTION_SETTINGS, mainIntent))

            addAction(R.drawable.ic_stop, R.string.notification_action_stop,
                      servicePI(ACTION_STOP, Command.OFF.intent))

            if (isOn) {
                setContentText(getString(R.string.notification_status_running))
                if (appMonitor.monitoring) {
                    val addWL = WhitelistChangeReceiver.intent(true)
                    addAction(R.drawable.ic_pause, R.string.notification_action_whitelist_add,
                              broadcastPI(ACTION_WHITELIST, addWL))
                } else {
                    addAction(R.drawable.ic_pause, R.string.notification_action_pause,
                              servicePI(ACTION_TOGGLE, Command.PAUSE.intent))
                }
            } else {
                setContentText(getString(R.string.notification_status_paused))
                if (appMonitor.monitoring) {
                    val rmWL = WhitelistChangeReceiver.intent(false)
                    addAction(R.drawable.ic_play, R.string.notification_action_whitelist_remove,
                              broadcastPI(ACTION_UNWHITELIST, rmWL))
                } else {
                    addAction(R.drawable.ic_play, R.string.notification_action_resume,
                              servicePI(ACTION_TOGGLE, Command.ON.intent))
                }
            }

            val nextProfileIntent = intent(NextProfileCommandReceiver::class)
            addAction(R.drawable.ic_skip_next_white_36dp,
                      R.string.notification_action_next_filter,
                      broadcastPI(NEXT_PROFILE, nextProfileIntent))
        }.build()
    }

    //region wrappers for readability
    private fun servicePI(code: Int, intent: Intent) =
            PendingIntent.getService(context, code, intent, PendingIntent.FLAG_UPDATE_CURRENT)

    private fun activityPI(code: Int, intent: Intent) =
            PendingIntent.getActivity(context, code, intent, PendingIntent.FLAG_UPDATE_CURRENT)

    private fun broadcastPI(code: Int, intent: Intent) =
            PendingIntent.getBroadcast(context, code, intent, 0)

    private fun NotificationCompat.Builder.addAction(icon: Int, title: Int, intent: PendingIntent) =
            addAction(icon, getString(title), intent)
    //endregion

    companion object : Logger() {
        // Request codes
        private const val ACTION_SETTINGS = 1000
        private const val ACTION_TOGGLE = 3000
        private const val NEXT_PROFILE = 4000
        private const val ACTION_STOP = 5000
        private const val ACTION_WHITELIST = 6000
        private const val ACTION_UNWHITELIST = 7000
    }
}
