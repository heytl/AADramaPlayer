package com.aa.duanju.feature.player

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer

class AAVideoPlayer : StandardGSYVideoPlayer {
    constructor(context: Context) : super(context)
    constructor(context: Context, fullFlag: Boolean?) : super(context, fullFlag)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    var onSingleTapListener: (() -> Unit)? = null

    init {
        applyNavBarInsets()
    }

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

    // 彻底屏蔽 GSY 自带中央按钮：任何 UI 状态切换都不让它露出来，
    // 中央图标改由 item_video.xml 里的 centerIcon 接管。
    override fun changeUiToNormal() {
        super.changeUiToNormal()
        hideStartButton()
    }

    override fun changeUiToPreparingShow() {
        super.changeUiToPreparingShow()
        hideStartButton()
    }

    override fun changeUiToPlayingShow() {
        super.changeUiToPlayingShow()
        hideStartButton()
    }

    override fun changeUiToPauseShow() {
        super.changeUiToPauseShow()
        hideStartButton()
    }

    override fun changeUiToPlayingBufferingShow() {
        super.changeUiToPlayingBufferingShow()
        hideStartButton()
    }

    override fun changeUiToCompleteShow() {
        super.changeUiToCompleteShow()
        hideStartButton()
    }

    private fun hideStartButton() {
        startButton?.visibility = View.GONE
    }

    /**
     * 底部控制栏避开三大金刚键：edge-to-edge 下导航栏会压住底部，
     * 给 GSY 的底部控制容器和细进度条加上导航栏高度的 bottomMargin。
     * 手势导航时导航栏高度为 0，不影响。
     */
    private fun applyNavBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            val navBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            mBottomContainer?.updateBottomMargin(navBottom)
            mBottomProgressBar?.updateBottomMargin(navBottom)
            insets
        }
        ViewCompat.requestApplyInsets(this)
    }

    private fun View.updateBottomMargin(bottom: Int) {
        (layoutParams as? ViewGroup.MarginLayoutParams)?.let { lp ->
            if (lp.bottomMargin != bottom) {
                lp.bottomMargin = bottom
                layoutParams = lp
            }
        }
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
