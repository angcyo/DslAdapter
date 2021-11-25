package com.angcyo.dsladapter

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


/**
 * 群组/分组 助手
 *
 * https://github.com/angcyo/DslAdapter/wiki/%E7%BE%A4%E7%BB%84%E5%8A%9F%E8%83%BD
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/10/16
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */

/**查找与[dslAdapterItem]相同分组的所有[DslAdapterItem]
 * [dslAdapterItem] 查询的目标
 * [useFilterList] 数据源
 * [lm] 指定外部的布局管理*/
fun DslAdapter.findItemGroupParams(
    dslAdapterItem: DslAdapterItem,
    useFilterList: Boolean = true,
    lm: RecyclerView.LayoutManager? = null
): ItemGroupParams {
    val allItemList = getDataList(useFilterList)
    val groupList = mutableListOf<ItemGroupParams>()

    //目标分组信息, 需要返回的数据
    var anchorParams: ItemGroupParams? = null

    var nextItem: DslAdapterItem? = null
    var nextParams: ItemGroupParams? = null
    var nextGroupItems = mutableListOf<DslAdapterItem>()

    for (i in allItemList.indices) {
        val item = allItemList[i]
        if (i == 0 || nextItem == item) {
            //保存之前
            nextParams?.apply {
                indexInGroup = nextGroupItems.indexOf(dslAdapterItem)
                groupIndex = groupList.indexOf(this)
                //edgeGridParams, 暂不初始化
            }

            //新建组
            nextParams = ItemGroupParams().apply {
                nextGroupItems = mutableListOf()
                nextGroupItems.add(item)

                groupList.add(this)

                this.groupList = groupList
                this.groupItems = nextGroupItems
                currentAdapterItem = item
            }

            //目标
            if (item == dslAdapterItem) {
                anchorParams = nextParams
            }

            for (j in i + 1 until allItemList.count()) {
                val otherItem = allItemList.getOrNull(j)
                if (otherItem != null) {
                    //目标
                    if (otherItem == dslAdapterItem) {
                        anchorParams = nextParams
                    }

                    if (item.isInGroupItem(otherItem)) {
                        nextGroupItems.add(otherItem)
                    } else {
                        nextItem = otherItem
                        break
                    }
                }
            }
        }
    }
    anchorParams?.currentAdapterItem = dslAdapterItem
    //最后一个
    nextParams?.apply {
        indexInGroup = nextGroupItems.indexOf(currentAdapterItem!!)
        groupIndex = groupList.indexOf(this)
    }

    /*var interruptGroup = false
    var findAnchor = false*/

    //分组数据计算
    /*for (i in allItemList.indices) {
        val targetItem = allItemList[i]

        if (!interruptGroup) {
            when {
                targetItem == dslAdapterItem -> {
                    params.groupItems.add(targetItem)
                    findAnchor = true
                }
                dslAdapterItem.isItemInGroups(targetItem) -> params.groupItems.add(targetItem)
                //如果不是连续的, 则中断分组
                findAnchor -> interruptGroup = true
            }
        }

        if (interruptGroup) {
            break
        }
    }

    anchorParams.indexInGroup = anchorParams.groupItems.indexOf(dslAdapterItem)*/

    //网格边界计算
    val layoutManager = lm ?: _recyclerView?.layoutManager

    if (anchorParams != null && layoutManager is GridLayoutManager) {
        layoutManager.apply {
            val itemPosition = allItemList.indexOf(dslAdapterItem)

            val groupItemList = anchorParams.groupItems

            val spanSizeLookup = spanSizeLookup ?: GridLayoutManager.DefaultSpanSizeLookup()

            //当前位置
            val currentSpanParams =
                getSpanParams(spanSizeLookup, itemPosition, spanCount, anchorParams.indexInGroup)

            //下一个的信息
            val nextItemPosition: Int = itemPosition + 1
            val nextSpanParams = if (allItemList.size > nextItemPosition) {
                getSpanParams(
                    spanSizeLookup,
                    nextItemPosition,
                    spanCount,
                    groupItemList.indexOf(allItemList[nextItemPosition])
                )
            } else {
                SpanParams()
            }

            //分组第一个
            val firstItemPosition = allItemList.indexOf(groupItemList.firstOrNull())
            val firstSpanParams = if (firstItemPosition == -1) {
                SpanParams()
            } else {
                getSpanParams(
                    spanSizeLookup,
                    firstItemPosition,
                    spanCount,
                    groupItemList.indexOf(allItemList[firstItemPosition])
                )
            }

            //分组最后一个
            val lastItemPosition = allItemList.indexOf(groupItemList.lastOrNull())
            val lastSpanParams = if (lastItemPosition == -1) {
                SpanParams()
            } else {
                getSpanParams(
                    spanSizeLookup,
                    lastItemPosition,
                    spanCount,
                    groupItemList.indexOf(allItemList[lastItemPosition])
                )
            }

            //adapter第一个
            val firstPosition = 0
            val firstParams = if (allItemList.isEmpty()) {
                SpanParams()
            } else {
                getSpanParams(
                    spanSizeLookup,
                    firstPosition,
                    spanCount,
                    groupItemList.indexOf(allItemList[firstPosition])
                )
            }

            //adapter最后一个
            val lastPosition = allItemList.lastIndex
            val lastParams = if (allItemList.isEmpty()) {
                SpanParams()
            } else {
                getSpanParams(
                    spanSizeLookup,
                    lastPosition,
                    spanCount,
                    groupItemList.indexOf(allItemList[lastPosition])
                )
            }

            anchorParams.edgeGridParams = EdgeGridParams(
                currentSpanParams, nextSpanParams,
                firstSpanParams, lastSpanParams,
                firstParams, lastParams
            )
        }
    } else if (layoutManager is LinearLayoutManager) {
        //todo
    } else if (layoutManager == null) {
        L.w("layoutManager is null")
    }

    return anchorParams ?: dslAdapterItem.createItemGroupParams()
}

fun getSpanParams(
    spanSizeLookup: GridLayoutManager.SpanSizeLookup,
    itemPosition: Int,
    spanCount: Int,
    indexInGroup: Int = -1
): SpanParams {

    val spanParams = SpanParams(indexInGroup = indexInGroup)
    spanParams.spanCount = spanCount
    spanParams.itemPosition = itemPosition
    spanParams.spanGroupIndex = spanSizeLookup.getSpanGroupIndex(itemPosition, spanCount)
    spanParams.spanIndex = spanSizeLookup.getSpanIndex(itemPosition, spanCount)
    spanParams.spanSize = spanSizeLookup.getSpanSize(itemPosition)

    return spanParams
}

/**[DslAdapterItem]分组信息. 分组依据[DslAdapterItem.isItemInGroups]*/
data class ItemGroupParams(

    /**所有分组的信息*/
    var groupList: List<ItemGroupParams> = listOf(),

    /**在第几组中*/
    var groupIndex: Int = RecyclerView.NO_POSITION,

    /**当前[item]在一组中的索引值[index]*/
    var indexInGroup: Int = RecyclerView.NO_POSITION,

    var currentAdapterItem: DslAdapterItem? = null,

    /**一组中的所有[item]*/
    var groupItems: List<DslAdapterItem> = listOf(),

    /**网格系统中的edge信息*/
    var edgeGridParams: EdgeGridParams = EdgeGridParams()
)

fun DslAdapterItem.createItemGroupParams(): ItemGroupParams {
    val params = ItemGroupParams()
    val groupList = mutableListOf<ItemGroupParams>()
    val groupItemList = mutableListOf<DslAdapterItem>()

    params.currentAdapterItem = this

    params.groupList = groupList
    params.groupItems = groupItemList

    params.indexInGroup = groupItemList.indexOf(this)
    params.groupIndex = groupList.indexOf(params)

    return params
}

/**网格分组边界[Edge]信息*/
data class EdgeGridParams(

    //当前位置的参数
    var currentSpanParams: SpanParams = SpanParams(),
    //下一个位置的参数
    var nextSpanParams: SpanParams = SpanParams(),

    //分组中第一个位置的数据
    var firstSpanParams: SpanParams = SpanParams(),

    //分组中最后一个位置的数据
    var lastSpanParams: SpanParams = SpanParams(),

    //adapter第一个位置的数据
    var firstParams: SpanParams = SpanParams(),

    //adapter最后一个位置的数据
    var lastParams: SpanParams = SpanParams()
)

/**网格分组中[Span]的信息*/
data class SpanParams(
    //androidx.recyclerview.widget.GridLayoutManager.getSpanCount
    var spanCount: Int = 1,
    //当前的位置, 在适配器中的索引
    var itemPosition: Int = RecyclerView.NO_POSITION,
    //当前的索引在群组中
    var indexInGroup: Int = RecyclerView.NO_POSITION,
    //在第几行
    var spanGroupIndex: Int = RecyclerView.NO_POSITION,
    //在第几列
    var spanIndex: Int = RecyclerView.NO_POSITION,
    //占多少列
    var spanSize: Int = RecyclerView.NO_POSITION
)

//是否是首列
fun SpanParams.isFirstSpan(): Boolean = spanIndex == 0

//是否是尾列
fun SpanParams.isLastSpan(): Boolean = spanIndex + spanSize == spanCount

//<editor-fold desc="线性布局中的分组信息扩展方法">

/**一组中,仅有一个*/
fun ItemGroupParams.isOnlyOne(): Boolean = groupItems.size == 1

/**是否是一组中的第一个位置*/
fun ItemGroupParams.isFirstPosition(): Boolean = indexInGroup == 0 && currentAdapterItem != null

/**是否是一组中的最后一个位置*/
fun ItemGroupParams.isLastPosition(): Boolean =
    currentAdapterItem != null && indexInGroup == groupItems.lastIndex

/**在第一组中*/
fun ItemGroupParams.isFirstGroup(): Boolean = groupList.count() > 0 && groupIndex == 0

/**在最后一组中*/
fun ItemGroupParams.isLastGroup(): Boolean =
    groupList.count() > 0 && groupIndex == groupList.lastIndex

//</editor-fold desc="线性布局中的分组信息扩展方法">

//<editor-fold desc="网格布局中的边界扩展方法">

/**网格布局, 边界扩展方法*/
//是否在4条边上

/**在网格的左边*/
fun ItemGroupParams.isEdgeLeft(): Boolean = edgeGridParams.currentSpanParams.spanIndex == 0

/**在网格的右边*/
fun ItemGroupParams.isEdgeRight(): Boolean = edgeGridParams.currentSpanParams.run {
    spanIndex + spanSize == spanCount
}

/**在网格的上边*/
fun ItemGroupParams.isEdgeTop(): Boolean = edgeGridParams.currentSpanParams.spanGroupIndex == 0

/**在网格的下边*/
fun ItemGroupParams.isEdgeBottom(): Boolean = edgeGridParams.run {
    currentSpanParams.spanGroupIndex != RecyclerView.NO_POSITION &&
            lastParams.spanGroupIndex == currentSpanParams.spanGroupIndex
}

/**全屏占满网格整个一行*/
fun ItemGroupParams.isEdgeHorizontal(): Boolean = isEdgeLeft() && isEdgeRight()

/**全屏占满网格整个一列*/
fun ItemGroupParams.isEdgeVertical(): Boolean = isEdgeTop() && isEdgeBottom()

//</editor-fold desc="网格布局中的边界扩展方法">

//<editor-fold desc="细粒度 分组边界扩展">

//是否在分组4个角上

/**是否在分组左上角*/
fun ItemGroupParams.isEdgeGroupLeftTop(): Boolean = edgeGridParams.run {
    currentSpanParams.spanIndex == 0 && currentSpanParams.spanGroupIndex == firstSpanParams.spanGroupIndex
}

/**是否在分组右上角*/
fun ItemGroupParams.isEdgeGroupRightTop(): Boolean = edgeGridParams.run {
    isEdgeRight() && isEdgeGroupTop()
}

/**是否在分组左下角*/
fun ItemGroupParams.isEdgeGroupLeftBottom(): Boolean = edgeGridParams.run {
    isEdgeLeft() && isEdgeGroupBottom()
}

/**是否在一组中的最后一个位置*/
fun ItemGroupParams.isLastInGroup() = indexInGroup == groupItems.lastIndex

/**是否在分组右下角*/
fun ItemGroupParams.isEdgeGroupRightBottom(): Boolean = edgeGridParams.run {
    isLastInGroup() && isEdgeGroupBottom()
}

/**在一组中的第一行中*/
fun ItemGroupParams.isEdgeGroupTop(): Boolean = edgeGridParams.run {
    currentSpanParams.spanGroupIndex != RecyclerView.NO_POSITION &&
            currentSpanParams.spanGroupIndex == firstSpanParams.spanGroupIndex
}

/**在一组中的最后一行中*/
fun ItemGroupParams.isEdgeGroupBottom(): Boolean = edgeGridParams.run {
    currentSpanParams.spanGroupIndex != RecyclerView.NO_POSITION &&
            currentSpanParams.spanGroupIndex == lastSpanParams.spanGroupIndex
}

/**占满整个一行(允许非全屏)*/
fun ItemGroupParams.isEdgeGroupHorizontal(): Boolean =
    (isEdgeGroupLeftTop() && isEdgeGroupRightTop()) || (isEdgeGroupLeftBottom() && isEdgeGroupRightBottom())

/**占满整个一列(允许非全屏)*/
fun ItemGroupParams.isEdgeGroupVertical(): Boolean =
    (isEdgeGroupLeftTop() && isEdgeGroupLeftBottom()) || (isEdgeGroupRightTop() && isEdgeGroupRightBottom())

/**在一组中的第一列上, 支持横竖布局*/
fun ItemGroupParams.isGroupFirstColumn(): Boolean = edgeGridParams.run {
    currentSpanParams.spanIndex == firstSpanParams.spanIndex
}

/**在一组中的最后一列上, 支持横竖布局, 如果这一列未满, 也属于最后一列*/
fun ItemGroupParams.isGroupLastColumn(): Boolean = edgeGridParams.run {
    currentSpanParams.spanIndex == lastSpanParams.spanIndex
}

/**在一组中的第一行上, 支持横竖布局*/
fun ItemGroupParams.isGroupFirstRow(): Boolean = edgeGridParams.run {
    currentSpanParams.spanGroupIndex != RecyclerView.NO_POSITION &&
            currentSpanParams.spanGroupIndex == firstSpanParams.spanGroupIndex
}

/**在一组中的最后一行上, 支持横竖布局*/
fun ItemGroupParams.isGroupLastRow(): Boolean = edgeGridParams.run {
    currentSpanParams.spanGroupIndex != RecyclerView.NO_POSITION &&
            currentSpanParams.spanGroupIndex == lastSpanParams.spanGroupIndex
}

//</editor-fold desc="细粒度 分组边界扩展">

