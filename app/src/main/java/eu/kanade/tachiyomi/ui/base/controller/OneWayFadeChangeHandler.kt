package eu.kanade.tachiyomi.ui.base.controller

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.changehandler.AnimatorChangeHandler
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler

/**
 * A variation of [FadeChangeHandler] that only fades in.
 */
class OneWayFadeChangeHandler : AnimatorChangeHandler {
    constructor()
    constructor(removesFromViewOnPush: Boolean) : super(removesFromViewOnPush)
    constructor(duration: Long) : super(duration)
    constructor(duration: Long, removesFromViewOnPush: Boolean) : super(
        duration,
        removesFromViewOnPush,
    )

    override fun getAnimator(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean,
        toAddedToContainer: Boolean,
    ): Animator {
        val animator = AnimatorSet()
        if (to != null) {
            val start: Float = if (toAddedToContainer) 0F else to.alpha
            animator.play(ObjectAnimator.ofFloat(to, View.ALPHA, start, 1f))
        }

        if (from != null && (!isPush || removesFromViewOnPush)) {
            container.removeView(from)
        }
        return animator
    }

    override fun resetFromView(from: View) {
        from.alpha = 1f
    }

    override fun copy(): ControllerChangeHandler = OneWayFadeChangeHandler(animationDuration, removesFromViewOnPush)
}
