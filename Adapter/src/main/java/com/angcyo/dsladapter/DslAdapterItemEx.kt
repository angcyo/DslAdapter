package com.angcyo.dsladapter

import android.graphics.Color
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView

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

//</editor-fold desc="操作扩展">