package com.angcyo.dsladapter

import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView

/**
 * 群组/分组 助手
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/10/16
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */

/**查找与[dslAdapterItem]相同分组的所有[DslAdapterItem]*/
fun DslAdapter.findItemGroupParams(dslAdapterItem: DslAdapterItem): ItemGroupParams {
    val params = ItemGroupParams()
    params.currentAdapterItem = dslAdapterItem

    val allItemList = getValidFilterDataList()
    var interruptGroup = false
    var findAnchor = false

    //分组数据计算
    for (i in allItemList.indices) {
        val newItem = allItemList[i]

        if (!interruptGroup) {
            when {
                newItem == dslAdapterItem -> {
                    params.groupItems.add(newItem)
                    findAnchor = true
                }
                dslAdapterItem.isItemInGroups(newItem) -> params.groupItems.add(newItem)
                //如果不是连续的, 则中断分组
                findAnchor -> interruptGroup = true
            }
        }

        if (interruptGroup) {
            break
        }
    }

    params.indexInGroup = params.groupItems.indexOf(dslAdapterItem)

    //网格边界计算
    var edge = EDGE_NONE
    var edgeGroup = EDGE_NONE
    val gridLayoutManager = _recyclerView?.layoutManager as? GridLayoutManager
    gridLayoutManager?.apply {
        val itemPosition = allItemList.indexOf(dslAdapterItem)

        val groupList = if (params.groupItems.size <= 1) {
            //仅有自己
            allItemList
        } else {
            params.groupItems
        }

        val spanSizeLookup = spanSizeLookup ?: GridLayoutManager.DefaultSpanSizeLookup()

        //当前位置
        val currentSpanParams = getSpanParams(spanSizeLookup, itemPosition, spanCount)

        //下一个的信息
        val nextItemPosition: Int = itemPosition + 1
        val nextSpanParams = if (allItemList.size > nextItemPosition) {
            getSpanParams(spanSizeLookup, nextItemPosition, spanCount).apply {
                indexInGroup = groupList.indexOf(allItemList[nextItemPosition])
            }
        } else {
            SpanParams()
        }

        //分组第一个
        val firstItemPosition = allItemList.indexOf(groupList.firstOrNull())
        val firstSpanParams = if (firstItemPosition == -1) {
            SpanParams()
        } else {
            getSpanParams(spanSizeLookup, firstItemPosition, spanCount).apply {
                indexInGroup = groupList.indexOf(allItemList[firstItemPosition])
            }
        }

        //分组最后一个
        val lastItemPosition = allItemList.indexOf(groupList.lastOrNull())
        val lastSpanParams = if (lastItemPosition == -1) {
            SpanParams()
        } else {
            getSpanParams(spanSizeLookup, lastItemPosition, spanCount).apply {
                indexInGroup = groupList.indexOf(allItemList[lastItemPosition])
            }
        }

        if (firstSpanParams.spanGroupIndex == currentSpanParams.spanGroupIndex) {
            //分组的第一行
            edgeGroup = edgeGroup or EDGE_GROUP_TOP
        }
        if (lastSpanParams.spanGroupIndex == currentSpanParams.spanGroupIndex) {
            //分组的最后一行
            edgeGroup = edgeGroup or EDGE_GROUP_BOTTOM
        }

        if (currentSpanParams.isFirstSpan()) {
            //第0列, 肯定是在左边界
            edge = edge or EDGE_LEFT
            edgeGroup = edgeGroup or EDGE_LEFT

            if (params.indexInGroup == 0) {
                edgeGroup = edgeGroup or EDGE_TOP
                edgeGroup = edgeGroup or EDGE_LEFT_TOP
            }
            if (currentSpanParams.spanSize == spanCount) {
                edgeGroup = edgeGroup or EDGE_RIGHT
                edgeGroup = edgeGroup or EDGE_RIGHT_TOP
            }
            if (params.groupItems.size == 1) {
                edgeGroup = edgeGroup or EDGE_TOP
                edgeGroup = edgeGroup or EDGE_BOTTOM
                edgeGroup = edgeGroup or EDGE_LEFT_BOTTOM
                edgeGroup = edgeGroup or EDGE_RIGHT_BOTTOM
            }
            if (lastSpanParams.spanGroupIndex == currentSpanParams.spanGroupIndex) {
                //第0列, 又在同一组的最后一行
                edgeGroup = edgeGroup or EDGE_BOTTOM
                edgeGroup = edgeGroup or EDGE_LEFT_BOTTOM
            }
            if (currentSpanParams.spanSize == spanCount) {
                //占满一行
                edge = edge or EDGE_RIGHT
                edgeGroup = edgeGroup or EDGE_GROUP_TOP
                edgeGroup = edgeGroup or EDGE_RIGHT_TOP
            }
        }

        if (currentSpanParams.isLastSpan(spanCount)) {
            //最后一列, 肯定是在右边界
            edge = edge or EDGE_RIGHT
            edgeGroup = edgeGroup or EDGE_RIGHT
        }

        if (currentSpanParams.spanGroupIndex == 0) {
            //第0行, 肯定是在顶边界
            edge = edge or EDGE_TOP
            edgeGroup = edgeGroup or EDGE_TOP

            if (params.groupItems.size - 1 == itemPosition) {
                edgeGroup = edgeGroup or EDGE_LEFT_TOP
            }
        }

        if (params.indexInGroup == params.groupItems.size - 1) {
            //一组中的最后一个
            edgeGroup = edgeGroup or EDGE_RIGHT_BOTTOM
        }

        if (itemPosition == allItemList.size - 1) {
            //最后一个, 肯定是在底边界
            edge = edge or EDGE_BOTTOM
            edgeGroup = edgeGroup or EDGE_BOTTOM
        }

        params.edgeGridParams = EdgeGridParams(
            edge,
            edgeGroup,
            currentSpanParams, nextSpanParams,
            firstSpanParams, lastSpanParams
        )
    }

    params.edgeInGrid = edge
    params.edgeInGroup = edgeGroup

    return params
}

public fun getSpanParams(
    spanSizeLookup: GridLayoutManager.SpanSizeLookup,
    itemPosition: Int,
    spanCount: Int
): SpanParams {

    val spanParams = SpanParams()
    spanParams.itemPosition = itemPosition
    spanParams.spanGroupIndex = spanSizeLookup.getSpanGroupIndex(itemPosition, spanCount)
    spanParams.spanIndex = spanSizeLookup.getSpanIndex(itemPosition, spanCount)
    spanParams.spanSize = spanSizeLookup.getSpanSize(itemPosition)

    return spanParams
}

data class ItemGroupParams(
    var indexInGroup: Int = RecyclerView.NO_POSITION,
    var currentAdapterItem: DslAdapterItem? = null,
    var groupItems: MutableList<DslAdapterItem> = mutableListOf(),

    /**仅在使用了[android.support.v7.widget.GridLayoutManager]时有效*/
    var edgeInGrid: Int = EDGE_NONE,
    var edgeInGroup: Int = EDGE_NONE,
    var edgeGridParams: EdgeGridParams = EdgeGridParams()
)

data class EdgeGridParams(

    /**相对于整个网格系统的边界*/
    var edgeInGrid: Int = EDGE_NONE,

    /**在同一组中的边界*/
    var edgeInGroup: Int = EDGE_NONE,

    //当前位置的参数
    var currentSpanParams: SpanParams = SpanParams(),
    //下一个位置的参数
    var nextSpanParams: SpanParams = SpanParams(),

    //分组中第一个位置的数据
    var firstSpanParams: SpanParams = SpanParams(),
    //分组中最后一个位置的数据
    var lastSpanParams: SpanParams = SpanParams()
)

data class SpanParams(
    //当前的位置
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
fun SpanParams.isLastSpan(spanCount: Int): Boolean = spanIndex + spanSize == spanCount

const val EDGE_NONE = 0x00
//左边界
const val EDGE_LEFT = 0x01
//顶边界
const val EDGE_TOP = 0x02
//右边界
const val EDGE_RIGHT = 0x04
//底边界
const val EDGE_BOTTOM = 0x08

//[edgeInGroup]特有的边界, 左上, 右上, 左下, 右下
const val EDGE_LEFT_TOP = 0x10
const val EDGE_RIGHT_TOP = 0x20
const val EDGE_LEFT_BOTTOM = 0x40
const val EDGE_RIGHT_BOTTOM = 0x80
//在一组中的顶部
const val EDGE_GROUP_TOP = 0x100
//在一组中的底部
const val EDGE_GROUP_BOTTOM = 0x200

/**仅有一个*/
fun ItemGroupParams.isOnlyOne(): Boolean = groupItems.size == 1

/**是否是第一个位置*/
fun ItemGroupParams.isFirstPosition(): Boolean = indexInGroup == 0 && currentAdapterItem != null

/**是否是最后一个位置*/
fun ItemGroupParams.isLastPosition(): Boolean =
    currentAdapterItem != null && indexInGroup == groupItems.lastIndex


/**网格布局, 边界扩展方法*/
//是否在4条边上
fun ItemGroupParams.isEdgeLeft(): Boolean = edgeInGrid.have(EDGE_LEFT)
fun ItemGroupParams.isEdgeRight(): Boolean = edgeInGrid.have(EDGE_RIGHT)
fun ItemGroupParams.isEdgeTop(): Boolean = edgeInGrid.have(EDGE_TOP)
fun ItemGroupParams.isEdgeBottom(): Boolean = edgeInGrid.have(EDGE_BOTTOM)

//全屏占满整个一行
fun ItemGroupParams.isEdgeHorizontal(): Boolean = isEdgeLeft() && isEdgeRight()
//全屏占满整个一列
fun ItemGroupParams.isEdgeVertical(): Boolean = isEdgeTop() && isEdgeBottom()

//是否在4个角
fun ItemGroupParams.isEdgeGroupLeftTop(): Boolean = edgeInGroup.have(EDGE_LEFT_TOP)
fun ItemGroupParams.isEdgeGroupRightTop(): Boolean = edgeInGroup.have(EDGE_RIGHT_TOP)
fun ItemGroupParams.isEdgeGroupLeftBottom(): Boolean = edgeInGroup.have(EDGE_LEFT_BOTTOM)
fun ItemGroupParams.isEdgeGroupRightBottom(): Boolean = edgeInGroup.have(EDGE_RIGHT_BOTTOM)

//在一组中的第一行
fun ItemGroupParams.isEdgeGroupTop(): Boolean = edgeInGroup.have(EDGE_GROUP_TOP)
//在一组中的最后一行
fun ItemGroupParams.isEdgeGroupBottom(): Boolean = edgeInGroup.have(EDGE_GROUP_BOTTOM)

//占满整个一行(允许非全屏)
fun ItemGroupParams.isEdgeGroupHorizontal(): Boolean =
    (isEdgeGroupLeftTop() && isEdgeGroupRightTop()) || (isEdgeGroupLeftBottom() && isEdgeGroupRightBottom())

//占满整个一列(允许非全屏)
fun ItemGroupParams.isEdgeGroupVertical(): Boolean =
    (isEdgeGroupLeftTop() && isEdgeGroupLeftBottom()) || (isEdgeGroupRightTop() && isEdgeGroupRightBottom())


