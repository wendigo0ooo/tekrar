package com.wendigo.tekrar

import android.content.Context
import android.content.Intent

object StudyRequest {

    const val EXTRA_START_STUDY = "com.wendigo.tekrar.extra.START_STUDY"
    const val EXTRA_STUDY_LIMIT = "com.wendigo.tekrar.extra.STUDY_LIMIT"
    const val DEFAULT_LIMIT = 10

    fun activityIntent(context: Context, limit: Int = DEFAULT_LIMIT): Intent =
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_START_STUDY, true)
            putExtra(EXTRA_STUDY_LIMIT, limit)
        }
}
