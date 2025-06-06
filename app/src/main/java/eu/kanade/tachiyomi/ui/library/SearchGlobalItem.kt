package eu.kanade.tachiyomi.ui.library

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.MaterialTextButtonBinding
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder

class SearchGlobalItem : AbstractFlexibleItem<SearchGlobalItem.Holder>() {
    var string = ""

    override fun getLayoutRes(): Int = R.layout.material_text_button

    override fun createViewHolder(
        view: View,
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
    ): Holder {
        view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            width = MATCH_PARENT
        }
        return Holder(view, adapter)
    }

    override fun isSelectable(): Boolean = false

    override fun isSwipeable(): Boolean = false

    override fun isDraggable(): Boolean = false

    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: Holder,
        position: Int,
        payloads: MutableList<Any>,
    ) {
        holder.bind(string)
        val layoutParams = holder.itemView.layoutParams as? StaggeredGridLayoutManager.LayoutParams
        layoutParams?.isFullSpan = true
    }

    override fun equals(other: Any?): Boolean = this === other

    override fun hashCode(): Int = -100

    class Holder(
        val view: View,
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
    ) : BaseFlexibleViewHolder(view, adapter, true) {
        private val binding = MaterialTextButtonBinding.bind(view)

        init {
            binding.button.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                width = MATCH_PARENT
            }
            binding.button.setOnClickListener {
                val query = (adapter.getItem(flexibleAdapterPosition) as SearchGlobalItem).string
                (adapter as? LibraryCategoryAdapter)?.libraryListener?.globalSearch(query)
            }
        }

        fun bind(string: String) {
            binding.button.text = view.context.getString(R.string.search_globally, string)
        }

        override fun onLongClick(view: View?): Boolean {
            super.onLongClick(view)
            return false
        }
    }
}
