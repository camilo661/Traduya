package com.traduya

import android.content.Context
import android.content.Intent
import com.traduya.core.telecom.CallUiLauncher
import com.traduya.feature.incall.InCallActivity
import javax.inject.Inject

class InCallScreenLauncher @Inject constructor() : CallUiLauncher {
    override fun inCallIntent(context: Context): Intent = Intent(context, InCallActivity::class.java)
}
