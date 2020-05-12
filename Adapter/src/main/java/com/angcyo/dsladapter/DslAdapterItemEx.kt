package com.angcyo.dsladapter

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.LayoutRes
import androidx.core.math.MathUtils.clamp
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import com.angcyo.dsladapter.internal.DslHierarchyChangeListenerWrap
import kotlin.math.min

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/01/22
 */

//<editor-fold desc="DslAdapterItem结构转换">

/**
 * 将list结构体, 打包成dslItem
 * */
fun List<Any>.toDslItemList(
    @LayoutRes layoutId: Int = -1,
    config: DslAdapterItem.() -> Unit = {}
): MutableList<DslAdapterItem> {
    return toDslItemList(DslAdapterItem::class.java, layoutId, config)
}

fun List<Any>.toDslItemList(
    dslItem: Class<out DslAdapterItem>,
    @LayoutRes layoutId: Int = -1,
    config: DslAdapterItem.() -> Unit = {}
): MutableList<DslAdapterItem> {
    return toDslItemList(itemFactory = { _, item ->
        dslItem.newInstance().apply {
            if (layoutId != -1) {
                itemLayoutId = layoutId
            }
            config()
        }
    })
}

fun List<Any>.toDslItemList(
    itemBefore: (itemList: MutableList<DslAdapterItem>, index: Int, item: Any) -> Unit = { _, _, _ -> },
    itemFactory: (index: Int, item: Any) -> DslAdapterItem,
    itemAfter: (itemList: MutableList<DslAdapterItem>, index: Int, item: Any) -> Unit = { _, _, _ -> }
): MutableList<DslAdapterItem> {
    return toAnyList(itemBefore, { index, any ->
        val item = itemFactory(index, any)
        item.itemData = any
        item
    }, itemAfter)
}

fun <T> List<Any>.toAnyList(
    itemBefore: (itemList: MutableList<T>, index: Int, item: Any) -> Unit = { _, _, _ -> },
    itemFactory: (index: Int, item: Any) -> T,
    itemAfter: (itemList: MutableList<T>, index: Int, item: Any) -> Unit = { _, _, _ -> }
): MutableList<T> {
    val result = mutableListOf<T>()

    forEachIndexed { index, any ->
        itemBefore(result, index, any)
        val item = itemFactory(index, any)
        result.add(item)
        itemAfter(result, index, any)
    }
    return result
}

//</editor-fold desc="DslAdapterItem结构转换">


//<editor-fold desc="分割线操作扩展">

fun DslAdapterItem.setTopInsert(insert: Int, leftOffset: Int = 0, rightOffset: Int = 0) {
    itemTopInsert = insert
    itemRightOffset = rightOffset
    itemLeftOffset = leftOffset
}

fun DslAdapterItem.setBottomInsert(insert: Int, leftOffset: Int = 0, rightOffset: Int = 0) {
    itemBottomInsert = insert
    itemRightOffset = rightOffset
    itemLeftOffset = leftOffset
}

fun DslAdapterItem.setLeftInsert(insert: Int, topOffset: Int = 0, bottomOffset: Int = 0) {
    itemLeftInsert = insert
    itemBottomOffset = bottomOffset
    itemTopOffset = topOffset
}

fun DslAdapterItem.setRightInsert(insert: Int, topOffset: Int = 0, bottomOffset: Int = 0) {
    itemRightInsert = insert
    itemBottomOffset = bottomOffset
    itemTopOffset = topOffset
}

fun DslAdapterItem.margin(margin: Int, color: Int = Color.TRANSPARENT) {
    itemLeftInsert = margin
    itemRightInsert = margin
    itemTopInsert = margin
    itemBottomInsert = margin

    itemLeftOffset = 0
    itemRightOffset = 0
    itemTopOffset = 0
    itemBottomOffset = 0

    onlyDrawOffsetArea = false
    itemDecorationColor = color
}

fun DslAdapterItem.marginVertical(top: Int, bottom: Int = 0, color: Int = Color.TRANSPARENT) {
    itemLeftOffset = 0
    itemRightOffset = 0
    itemTopInsert = top
    itemBottomInsert = bottom
    onlyDrawOffsetArea = false
    itemDecorationColor = color
}

fun DslAdapterItem.marginHorizontal(left: Int, right: Int = 0, color: Int = Color.TRANSPARENT) {
    itemTopOffset = 0
    itemBottomOffset = 0

    itemLeftInsert = left
    itemRightInsert = right
    onlyDrawOffsetArea = false
    itemDecorationColor = color
}

/**仅绘制左边区域的分割线*/
fun DslAdapterItem.drawLeft(
    offsetLeft: Int,
    insertTop: Int = 1 * dpi,
    color: Int = Color.WHITE
) {
    itemLeftOffset = offsetLeft
    itemRightOffset = 0

    itemTopInsert = insertTop
    itemBottomInsert = 0

    onlyDrawOffsetArea = true
    itemDecorationColor = color
}

//</editor-fold desc="分割线操作扩展">

//<editor-fold desc="操作扩展">

fun DslAdapterItem.itemIndexPosition(dslAdapter: DslAdapter? = null) =
    (dslAdapter ?: itemDslAdapter)?.getValidFilterDataList()?.indexOf(this)
        ?: RecyclerView.NO_POSITION

fun DslAdapterItem.itemViewHolder(recyclerView: RecyclerView?): DslViewHolder? {
    val position = itemIndexPosition()
    return if (position != RecyclerView.NO_POSITION) {
        recyclerView?.findViewHolderForAdapterPosition(position) as? DslViewHolder
    } else {
        null
    }
}

fun DslAdapterItem.isItemAttached(): Boolean {
    return lifecycle.currentState == Lifecycle.State.RESUMED
}

/**提供和[DslAdapter]相同的使用方式, 快速创建[DslAdapterItem]集合*/
fun renderItemList(render: DslAdapter.() -> Unit): List<DslAdapterItem> {
    return DslAdapter().run {
        render()
        adapterItems
    }
}

//</editor-fold desc="操作扩展">

//<editor-fold desc="更新指定的Item">

fun DslAdapter.removeHeaderItem(itemTag: String?, item: DslAdapterItem? = null) {
    val target = item ?: findItemByTag(itemTag, false)
    if (target == null) {
        L.w("移除的目标不存在")
    } else {
        changeHeaderItems {
            it.remove(target)
        }
    }
}

fun DslAdapter.removeItem(itemTag: String?, item: DslAdapterItem? = null) {
    val target = item ?: findItemByTag(itemTag, false)
    if (target == null) {
        L.w("移除的目标不存在")
    } else {
        changeDataItems {
            it.remove(target)
        }
    }
}

fun DslAdapter.removeFooterItem(itemTag: String?, item: DslAdapterItem? = null) {
    val target = item ?: findItemByTag(itemTag, false)
    if (target == null) {
        L.w("移除的目标不存在")
    } else {
        changeFooterItems {
            it.remove(target)
        }
    }
}

/**
 * 更新或者插入指定的Item
 * 如果目标item已存在, 则更新Item, 否则创建新的插入
 * */
inline fun <reified Item : DslAdapterItem> DslAdapter.updateOrInsertItem(
    itemTag: String? /*需要更新的Item*/,
    insertIndex: Int = 0 /*当需要插入时, 插入到列表中的位置*/,
    crossinline updateOrCreateItem: (oldItem: Item) -> Item?
    /*返回null, 则会删除对应的[oldItem], 返回与[oldItem]不一样的item, 则会替换原来的[oldItem]*/
) {
    changeDataItems {
        _updateOrInsertItem(it, itemTag, insertIndex, updateOrCreateItem)
    }
}

inline fun <reified Item : DslAdapterItem> DslAdapter.updateOrInsertHeaderItem(
    itemTag: String?,
    insertIndex: Int = 0,
    crossinline updateOrCreateItem: (oldItem: Item) -> Item?
) {
    changeHeaderItems {
        _updateOrInsertItem(it, itemTag, insertIndex, updateOrCreateItem)
    }
}

inline fun <reified Item : DslAdapterItem> DslAdapter.updateOrInsertFooterItem(
    itemTag: String?,
    insertIndex: Int = 0,
    crossinline updateOrCreateItem: (oldItem: Item) -> Item?
) {

    changeFooterItems {
        _updateOrInsertItem(it, itemTag, insertIndex, updateOrCreateItem)
    }
}

inline fun <reified Item : DslAdapterItem> DslAdapter._updateOrInsertItem(
    itemList: MutableList<DslAdapterItem>,
    itemTag: String? /*需要更新的Item*/,
    insertIndex: Int = 0 /*当需要插入时, 插入到列表中的位置*/,
    crossinline updateOrCreateItem: (oldItem: Item) -> Item?
    /*返回null, 则会删除对应的[oldItem], 返回与[oldItem]不一样的item, 则会替换原来的[oldItem]*/
) {

    //查找已经存在的item
    val findItem = findItemByTag(itemTag, false)

    val oldItem: Item

    //不存在, 或者存在的类型不匹配, 则创建新item
    oldItem = if (findItem == null || findItem !is Item) {
        Item::class.java.newInstance()
    } else {
        findItem
    }

    //回调处理
    val newItem = updateOrCreateItem(oldItem)
    newItem?.itemTag = itemTag

    if (findItem == null && newItem == null) {
        return
    }

    itemList.let {
        if (newItem == null) {
            //需要移除处理
            if (findItem != null) {
                it.remove(findItem)
            }
        } else {
            if (findItem == null) {
                //需要insert处理
                it.add(clamp(insertIndex, 0, it.size), newItem)
            } else {
                //需要更新处理
                findItem.itemChanging = true
                val indexOf = it.indexOf(findItem)
                if (indexOf != -1) {
                    it[indexOf] = newItem
                }
            }
        }
    }
}

//</editor-fold desc="更新指定的Item">

//<editor-fold desc="Dsl吸附">

fun View.dslViewHolder(): DslViewHolder {
    return this.run {
        var _tag = getTag(R.id.lib_tag_dsl_view_holder)
        if (_tag is DslViewHolder) {
            _tag
        } else {
            _tag = tag
            if (_tag is DslViewHolder) {
                _tag
            } else {
                DslViewHolder(this).apply {
                    setDslViewHolder(this)
                }
            }
        }
    }
}

fun View?.tagDslViewHolder(): DslViewHolder? {
    return this?.run {
        var _tag = getTag(R.id.lib_tag_dsl_view_holder)
        if (_tag is DslViewHolder) {
            _tag
        } else {
            _tag = tag
            if (_tag is DslViewHolder) {
                _tag
            } else {
                null
            }
        }
    }
}

fun View?.tagDslAdapterItem(): DslAdapterItem? {
    return this?.run {
        val tag = getTag(R.id.lib_tag_dsl_adapter_item)
        if (tag is DslAdapterItem) {
            tag
        } else {
            null
        }
    }
}

fun View?.setDslViewHolder(dslViewHolder: DslViewHolder?) {
    this?.setTag(R.id.lib_tag_dsl_view_holder, dslViewHolder)
}

fun View?.setDslAdapterItem(dslAdapterItem: DslAdapterItem?) {
    this?.setTag(R.id.lib_tag_dsl_adapter_item, dslAdapterItem)
}

//</editor-fold desc="Dsl吸附">

//</editor-fold desc="DslAdapterItem操作">

fun ViewGroup.appendDslItem(items: List<DslAdapterItem>) {
    items.forEach {
        appendDslItem(it)
    }
}

fun ViewGroup.appendDslItem(dslAdapterItem: DslAdapterItem): DslViewHolder {
    return addDslItem(dslAdapterItem)
}

fun ViewGroup.addDslItem(dslAdapterItem: DslAdapterItem, index: Int = -1): DslViewHolder {
    setOnHierarchyChangeListener(DslHierarchyChangeListenerWrap())
    val itemView = inflate(dslAdapterItem.itemLayoutId, false)
    val dslViewHolder = DslViewHolder(itemView)
    itemView.tag = dslViewHolder

    itemView.setDslViewHolder(dslViewHolder)
    itemView.setDslAdapterItem(dslAdapterItem)

    dslAdapterItem.itemBind(dslViewHolder, childCount - 1, dslAdapterItem, emptyList())

    //头分割线的支持
    if (this is LinearLayout) {
        if (this.orientation == LinearLayout.VERTICAL) {
            if (dslAdapterItem.itemTopInsert > 0) {
                addView(
                    View(context).apply { setBackgroundColor(dslAdapterItem.itemDecorationColor) },
                    LinearLayout.LayoutParams(-1, dslAdapterItem.itemTopInsert).apply {
                        leftMargin = dslAdapterItem.itemLeftOffset
                        rightMargin = dslAdapterItem.itemRightOffset
                    })
            }
        } else {
            if (dslAdapterItem.itemLeftInsert > 0) {
                addView(
                    View(context).apply { setBackgroundColor(dslAdapterItem.itemDecorationColor) },
                    LinearLayout.LayoutParams(dslAdapterItem.itemTopInsert, -1).apply {
                        topMargin = dslAdapterItem.itemTopOffset
                        bottomMargin = dslAdapterItem.itemBottomOffset
                    })
            }
        }
    }
    addView(itemView, index)
    //尾分割线的支持
    if (this is LinearLayout) {
        if (this.orientation == LinearLayout.VERTICAL) {
            if (dslAdapterItem.itemBottomInsert > 0) {
                addView(
                    View(context).apply { setBackgroundColor(dslAdapterItem.itemDecorationColor) },
                    LinearLayout.LayoutParams(-1, dslAdapterItem.itemBottomInsert).apply {
                        leftMargin = dslAdapterItem.itemLeftOffset
                        rightMargin = dslAdapterItem.itemRightOffset
                    })
            }
        } else {
            if (dslAdapterItem.itemRightInsert > 0) {
                addView(
                    View(context).apply { setBackgroundColor(dslAdapterItem.itemDecorationColor) },
                    LinearLayout.LayoutParams(dslAdapterItem.itemRightInsert, -1).apply {
                        topMargin = dslAdapterItem.itemTopOffset
                        bottomMargin = dslAdapterItem.itemBottomOffset
                    })
            }
        }
    }
    return dslViewHolder
}

fun ViewGroup.resetDslItem(items: List<DslAdapterItem>) {
    val childSize = childCount
    val itemSize = items.size

    //需要替换的child索引
    val replaceIndexList = mutableListOf<Int>()

    //更新已存在的Item
    for (i in 0 until min(childSize, itemSize)) {
        val childView = getChildAt(i)
        val dslItem = items[i]

        val tag = childView.getTag(R.id.tag)
        if (tag is Int && tag == dslItem.itemLayoutId) {
            //相同布局, 则使用缓存
            val dslViewHolder = childView.dslViewHolder()
            dslItem.itemBind(dslViewHolder, i, dslItem, emptyList())
        } else {
            //不同布局, 删除原先的view, 替换成新的
            replaceIndexList.add(i)
        }
    }

    //替换不相同的Item
    replaceIndexList.forEach { i ->
        val dslItem = items[i]

        removeViewAt(i)
        addDslItem(dslItem, i)
    }

    //移除多余的item
    for (i in itemSize until childSize) {
        removeViewAt(i)
    }

    //追加新的Item
    for (i in childSize until itemSize) {
        val dslItem = items[i]
        addDslItem(dslItem)
    }
}
//<editor-fold desc="DslAdapterItem操作">