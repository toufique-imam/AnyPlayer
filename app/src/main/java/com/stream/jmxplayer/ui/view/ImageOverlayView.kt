package com.stream.jmxplayer.ui.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.mediarouter.app.MediaRouteButton
import com.stream.jmxplayer.R
import com.stream.jmxplayer.model.PlayerModel

class ImageOverlayView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attributeSet, defStyleAttr) {
//    var onShareClick: (PlayerModel) -> Unit = {}
    var onBackClick: () -> Unit = {}
//    var shareButton: ImageView
    var backButton: ImageView
    var castButton: MediaRouteButton
    var title: TextView

    init {
        View.inflate(context, R.layout.image_overlay, this)
//        shareButton = findViewById(R.id.posterOverlayShareButton)
        castButton = findViewById(R.id.posterOverlayCastButton)
        title = findViewById(R.id.posterOverlayDescriptionText)
        backButton = findViewById(R.id.posterOverlayBackButton)
        setBackgroundColor(Color.TRANSPARENT)
    }

    fun update(playerModel: PlayerModel) {
        title.text = playerModel.title

        backButton.setOnClickListener {
            onBackClick()
        }
    }
}