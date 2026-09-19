package com.aa.duanju.feature.player

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer

class AAVideoPlayer : StandardGSYVideoPlayer {
    constructor(context: Context) : super(context)
    constructor(context: Context, fullFlag: Boolean?) : super(context, fullFlag)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    var onSingleTapListener: (() -> Unit)? = null

    override fun onClickUiToggle(event: MotionEvent?) {
        dismissGestureUi()
        onSingleTapListener?.invoke()
    }

    override fun onTouch(view: View?, event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_UP || event?.action == MotionEvent.ACTION_CANCEL) {
            dismissGestureUi()
        }
        return super.onTouch(view, event)
    }

    fun showUi() {
        dismissGestureUi()
        changeUiToPlayingShow()
    }

    fun hideUi() {
        dismissGestureUi()
        changeUiToPlayingClear()
    }

    private fun dismissGestureUi() {
        runCatching {
            dismissProgressDialog()
            dismissVolumeDialog()
            dismissBrightnessDialog()
        }
    }

    override fun getLayoutId(): Int = com.shuyu.gsyvideoplayer.R.layout.video_layout_standard
}
