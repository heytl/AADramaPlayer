package com.aa.duanju.ui.player

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer

class AAVideoPlayer : StandardGSYVideoPlayer {
    constructor(context: Context) : super(context)
    constructor(context: Context, fullFlag: Boolean?) : super(context, fullFlag)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    // 增加单击回调
    var onSingleTapListener: (() -> Unit)? = null

    // 单击 UI 切换的回调，GSY 内部手势检测到单击时触发
    override fun onClickUiToggle(e: MotionEvent) {
        onSingleTapListener?.invoke()
    }

    fun showUi() {
        changeUiToPlayingShow()
    }

    fun hideUi() {
        changeUiToPlayingClear()
    }
    
    override fun getLayoutId(): Int {
        return com.shuyu.gsyvideoplayer.R.layout.video_layout_standard
    }
}