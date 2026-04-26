package com.aa.duanju.ui.player

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer

class AAVideoPlayer : StandardGSYVideoPlayer {
    constructor(context: Context) : super(context)
    constructor(context: Context, fullFlag: Boolean?) : super(context, fullFlag)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    // 增加单击回调
    var onSingleTapListener: (() -> Unit)? = null

    // 单击 UI 切换的回调，GSY 内部手势检测到单击时触发
    override fun onClickUiToggle(e: MotionEvent?) {
        // 先清理可能残留在屏幕上的手势 UI
        dismissAllDialogs()
        onSingleTapListener?.invoke()
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        // 当手指抬起或手势取消时，强制隐藏所有手势弹窗，防止概率性残留
        if (event?.action == MotionEvent.ACTION_UP || event?.action == MotionEvent.ACTION_CANCEL) {
            dismissAllDialogs()
        }
        return super.onTouch(v, event)
    }

    private fun dismissAllDialogs() {
        try {
            dismissProgressDialog()
            dismissVolumeDialog()
            dismissBrightnessDialog()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun showUi() {
        dismissAllDialogs()
        changeUiToPlayingShow()
    }

    fun hideUi() {
        dismissAllDialogs()
        changeUiToPlayingClear()
    }
    
    override fun getLayoutId(): Int {
        return com.shuyu.gsyvideoplayer.R.layout.video_layout_standard
    }
}