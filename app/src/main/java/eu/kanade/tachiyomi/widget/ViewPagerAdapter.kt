package eu.kanade.tachiyomi.widget

import android.view.View
import android.view.ViewGroup

abstract class ViewPagerAdapter : androidx.viewpager.widget.PagerAdapter() {
    protected abstract fun createView(
        container: ViewGroup,
        position: Int,
    ): View

    protected open fun destroyView(
        container: ViewGroup,
        position: Int,
        view: View,
    ) {
    }

    override fun instantiateItem(
        container: ViewGroup,
        position: Int,
    ): Any {
        val view = createView(container, position)
        container.addView(view)
        return view
    }

    override fun destroyItem(
        container: ViewGroup,
        position: Int,
        obj: Any,
    ) {
        val view = obj as View
        destroyView(container, position, view)
        container.removeView(view)
    }

    override fun isViewFromObject(
        view: View,
        obj: Any,
    ): Boolean = view === obj

    interface PositionableView {
        val item: Any
    }
}
