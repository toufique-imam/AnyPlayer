package com.stream.jmxplayer.ui.view

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.view.*
import android.view.View.OnClickListener
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.*
import com.stream.jmxplayer.R
import com.stream.jmxplayer.model.MediaPlayerControl
import com.stream.jmxplayer.utils.GlobalFunctions.Companion.logger
import java.lang.ref.WeakReference
import java.util.*

class VideoControlView : FrameLayout {
    companion object {
        const val TAG = "VideoControlView"
        val sDefaultTimeout = 3000
        val FADE_OUT = 1
        val SHOW_PROGRESS = 2

        class MessageHandler(view: VideoControlView) : Handler(Looper.myLooper()!!) {
            val mView = WeakReference(view)
            override fun handleMessage(msg: Message) {
                val view = mView.get()
                if (view?.mPlayer == null) return
                var pos = 0
                when (msg.what) {
                    FADE_OUT -> view.hide()
                    SHOW_PROGRESS -> {
                        pos = view.setProgress()
                        if (!view.mDragging && view.mShowing && view.mPlayer!!.isPlaying) {
                            val msgN = obtainMessage(SHOW_PROGRESS)
                            sendMessageDelayed(msgN, (1000 - (pos % 1000)).toLong())
                        }

                    }
                }
            }
        }
    }

    private val mHandler: Handler = MessageHandler(this)

    private var mPlayer: MediaPlayerControl? = null
    private var mContext: Context
    private var mAnchor: ViewGroup? = null
    private var mRoot: View? = null

    lateinit var mProgressbar: ProgressBar
    private lateinit var mEndTime: TextView
    private lateinit var mCurrentTime: TextView
    lateinit var mTitleView: TextView
    lateinit var mDescriptionView: TextView
    lateinit var mLanguageView: TextView
    private var mShowing = false
    private var mDragging = false
    private var mUseFastForward = false
    private var mFromXml = false

    var mFormatBuilder = StringBuilder()
    var mFormatter = Formatter(mFormatBuilder, Locale.getDefault())
    var mPauseButton: ImageButton? = null
    var mFfwdButton: ImageButton? = null
    var mRewButton: ImageButton? = null

    lateinit var backButton: ImageButton

    constructor(context: Context) : this(context, true)

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {
        mRoot = null
        mContext = context
        mUseFastForward = true
        mFromXml = true
    }

    constructor(context: Context, useFastForward: Boolean) : super(context) {
        mContext = context
        mUseFastForward = useFastForward
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        mRoot?.let { initControllerView(it) }
    }


    /**
     * Set the view that acts as the anchor for the control view.
     * This can for example be a VideoView, or your Activity's main view.
     * @param view The view to which to anchor the controller when it is visible.
     */
    fun setAnchorView(view: ViewGroup) {
        logger(TAG, "setAnchorView")
        mAnchor = view
        val frameParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        removeAllViews()
        val v = makeControllerView()
        addView(v, frameParams)
    }

    /**
     * Create the view that holds the widgets that control playback.
     * Derived classes can override this to create their own.
     * @return The controller view.
     * @hide This doesn't work as advertised
     */
    private fun makeControllerView(): View {
        val inflate = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        mRoot = inflate.inflate(R.layout.vlc_player_view_style, null)

        initControllerView(mRoot!!)
        return mRoot!!
    }

    private fun initControllerView(view: View) {
        mPauseButton = view.findViewById(R.id.vlc_pause)
        mPauseButton?.setOnClickListener(mPauseListener)

        mFfwdButton = view.findViewById(R.id.vlc_forward)
        mFfwdButton?.setOnClickListener(mFfwdListener)
        if (!mFromXml)
            mFfwdButton?.visibility = if (mUseFastForward) View.VISIBLE else View.GONE

        mRewButton = view.findViewById(R.id.vlc_rew)
        mRewButton?.setOnClickListener(mRewListener)
        if (!mFromXml)
            mRewButton?.visibility = if (mUseFastForward) View.VISIBLE else View.GONE



        mProgressbar = view.findViewById(R.id.mediacontroller_progress)
        if (mProgressbar is SeekBar) {
            val seeker = mProgressbar as SeekBar
            seeker.setOnSeekBarChangeListener(mSeekListener)
            seeker.max = 1000
        }
        mProgressbar.max = 1000

        mEndTime = view.findViewById(R.id.vlc_duration)
        mCurrentTime = view.findViewById(R.id.vlc_position)
        //titles
        mTitleView = view.findViewById(R.id.textView_title_vlc)
        mDescriptionView = view.findViewById(R.id.textView_desc_vlc)
        mLanguageView = view.findViewById(R.id.textView_language_vlc)

        backButton = view.findViewById(R.id.vlc_back)
        //installPrevNextListeners()
    }

    /**
     * Show the controller on screen. It will go away
     * automatically after 3 seconds of inactivity.
     */
    fun show() {
        show(sDefaultTimeout)
    }

    /**
     * Show the controller on screen. It will go away
     * automatically after 'timeout' milliseconds of inactivity.
     * @param timeout The timeout in milliseconds. Use 0 to show
     * the controller until hide() is called.
     */
    fun show(timeout: Int) {
        if (!mShowing && mAnchor != null) {
            setProgress()
            if (mPauseButton != null)
                mPauseButton?.requestFocus()

            disableUnsupportedButtons()
            val tlp = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            )
            mAnchor?.addView(this, tlp)
            mShowing = true
        }
        updatePausePlay()
        // updateFullScreen();

        // cause the progress bar to be updated even if mShowing
        // was already true.  This happens, for example, if we're
        // paused with the progress bar showing the user hits play.
        mHandler.sendEmptyMessage(SHOW_PROGRESS)
        val msg = mHandler.obtainMessage(FADE_OUT)
        if (timeout != 0) {
            mHandler.removeMessages(FADE_OUT)
            mHandler.sendMessageDelayed(msg, timeout.toLong())
        }
    }

    fun isShowing(): Boolean {
        return mShowing
    }

    fun setMediaPlayer(player: MediaPlayerControl?) {
        mPlayer = player
        updatePausePlay()
        //updateFullScreen()
    }

    /**
     * Disable pause or seek buttons if the stream cannot be paused or seeked.
     * This requires the control interface to be a MediaPlayerControlExt
     */
    private fun disableUnsupportedButtons() {
        if (mPlayer == null) return
        try {
            if (!mPlayer!!.canPause()) {
                mPauseButton?.isEnabled = false
            }
            if (!mPlayer!!.canSeekBackward()) {
                mRewButton?.isEnabled = false
            }
            if (!mPlayer!!.canSeekForward()) {
                mFfwdButton?.isEnabled = false
            }
        } catch (ex: Exception) {
            logger(TAG, ex.message)
        }
    }

    /**
     * Remove the controller from the screen.
     */
    fun hide() {
        if (mAnchor == null) return
        try {
            mAnchor!!.removeView(this)
            mHandler.removeMessages(SHOW_PROGRESS)
        } catch (ex: Exception) {
            logger(TAG, ex.message)
        }
        mShowing = false
    }

    fun stringForTime(timeMs: Int): String {
        val totalSeconds = timeMs / 1000
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60 % 60
        val hours = totalSeconds / 3600
        mFormatBuilder.setLength(0)
        return if (hours > 0) {
            mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString()
        } else {
            mFormatter.format("%02d:%02d", minutes, seconds).toString()
        }
    }

    fun setProgress(): Int {
        if (mPlayer == null || mDragging) {
            return 0
        }
        val position = mPlayer!!.currentPosition
        val duration = mPlayer!!.duration
        if (duration > 0) {
            val pos = 1000L * position / duration
            mProgressbar.progress = pos.toInt()
        }
        val percent = mPlayer!!.bufferPercentage
        mProgressbar.secondaryProgress = percent * 10

        mEndTime.text = stringForTime(duration)
        mCurrentTime.text = stringForTime(position)

        return position

    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        show(sDefaultTimeout)
        return true
    }

    override fun onTrackballEvent(event: MotionEvent?): Boolean {
        show(sDefaultTimeout)
        return false
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if (mPlayer == null) return true

        val keyCode = event?.keyCode
        val uniqueDown = event?.repeatCount == 0 && event.action == KeyEvent.ACTION_DOWN

        if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK
            || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            || keyCode == KeyEvent.KEYCODE_SPACE
        ) {
            if (uniqueDown) {
                doPauseResume()
                show(sDefaultTimeout)
                mPauseButton?.requestFocus()
            }
            return true
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
            if (uniqueDown && !mPlayer!!.isPlaying) {
                mPlayer!!.start()
                updatePausePlay()
                show(sDefaultTimeout)
            }
            return true
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
            || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE
        ) {
            if (uniqueDown && mPlayer!!.isPlaying) {
                mPlayer!!.pause()
                updatePausePlay()
                show(sDefaultTimeout)
            }
            return true
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
            || keyCode == KeyEvent.KEYCODE_VOLUME_UP
            || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE
        ) {
            // don't show the controls for volume adjustment
            return super.dispatchKeyEvent(event)
        } else if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
            if (uniqueDown) {
                hide()
            }
            return true
        }

        show(sDefaultTimeout)
        return super.dispatchKeyEvent(event)
    }

    private val mPauseListener = OnClickListener {
        doPauseResume()
        show(sDefaultTimeout)
    }

    fun updatePausePlay() {
        if (mRoot == null || mPlayer == null) return

        if (mPlayer!!.isPlaying) {
            mPauseButton?.setImageResource(R.drawable.ic_baseline_pause_circle_filled_24)
            mPauseButton?.contentDescription = "pause"
        } else {
            mPauseButton?.contentDescription = "play"
            mPauseButton?.setImageResource(R.drawable.ic_baseline_play_circle_filled_24)
        }
    }

    private fun doPauseResume() {
        if (mPlayer == null) {
            return
        }

        if (mPlayer!!.isPlaying) {
            mPlayer!!.pause()
        } else {
            mPlayer!!.start()
        }
        updatePausePlay()
    }
    // There are two scenarios that can trigger the seekbar listener to trigger:
    //
    // The first is the user using the touchpad to adjust the posititon of the
    // seekbar's thumb. In this case onStartTrackingTouch is called followed by
    // a number of onProgressChanged notifications, concluded by onStopTrackingTouch.
    // We're setting the field "mDragging" to true for the duration of the dragging
    // session to avoid jumps in the position in case of ongoing playback.
    //
    // The second scenario involves the user operating the scroll ball, in this
    // case there WON'T BE onStartTrackingTouch/onStopTrackingTouch notifications,
    // we will simply apply the updated position without suspending regular updates.

    private val mSeekListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            if (mPlayer == null) return
            if (!fromUser) {
                // We're not interested in programmatically generated changes to
                // the progress bar's position.
                return
            }
            val duration = mPlayer!!.duration * 1L
            val newPosition = (duration * progress) / 1000L

            logger(TAG, "onProgressChanged $duration $newPosition $progress")
            mPlayer!!.seekTo(newPosition.toInt())
            mCurrentTime.text = stringForTime(newPosition.toInt())
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
            show(3600000)
            mDragging = true

            // By removing these pending progress messages we make sure
            // that a) we won't update the progress while the user adjusts
            // the seekbar and b) once the user is done dragging the thumb
            // we will post one of these messages to the queue again and
            // this ensures that there will be exactly one message queued up.
            mHandler.removeMessages(SHOW_PROGRESS)
        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            mDragging = false
            setProgress()
            updatePausePlay()
            show(sDefaultTimeout)

            // Ensure that progress is properly updated in the future,
            // the call to show() does not guarantee this because it is a
            // no-op if we are already showing.
            mHandler.sendEmptyMessage(SHOW_PROGRESS)
        }
    }

    override fun setEnabled(enabled: Boolean) {
        if (mPauseButton != null) {
            mPauseButton?.isEnabled = enabled
            mFfwdButton?.isEnabled = enabled
            mRewButton?.isEnabled = enabled

            mProgressbar.isEnabled = enabled
        }
        disableUnsupportedButtons()
        super.setEnabled(enabled)
    }

    override fun onInitializeAccessibilityEvent(event: AccessibilityEvent?) {
        super.onInitializeAccessibilityEvent(event)
        event?.className = VideoControlView::class.java.name
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo?) {
        super.onInitializeAccessibilityNodeInfo(info)
        info?.className = VideoControlView::class.java.name
    }

    private val mRewListener = OnClickListener {
        if (mPlayer == null) return@OnClickListener
        var pos = mPlayer!!.currentPosition
        pos -= 15000
        mPlayer!!.seekTo(pos)
        setProgress()
        show(sDefaultTimeout)
    }
    private val mFfwdListener = OnClickListener {
        if (mPlayer == null) return@OnClickListener
        var pos = mPlayer!!.currentPosition
        pos += 15000
        mPlayer!!.seekTo(pos)
        setProgress()
        show(sDefaultTimeout)
    }

}