package com.angcyo.dsladapter

import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/10/16
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */

/**查找与[dslAdapterItem]相同分组的所有[DslAdapterItem]*/
fun DslAdapter.findItemGroupParams(dslAdapterItem: DslAdapterItem): ItemGroupParams {
    val params = ItemGroupParams()
    params.currentAdapterItem = dslAdapterItem

    val list = getValidFilterDataList()
    var interruptGroup = false
    var findAnchor = false

    //分组数据计算
    for (i in list.indices) {
        val newItem = list[i]

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
        val itemPosition = list.indexOf(dslAdapterItem)

        //itemPosition 在第几行, 0开始
        val spanGroupIndex: Int
        //itemPosition, 在当前行数中, 第几列, 0开始
        val spanIndex: Int
        //itemPosition, 在当前行数中, 占多少列
        val spanSize: Int

        //下一个位置的信息
        val nextItemPosition: Int = itemPosition + 1
        var nextSpanGroupIndex: Int = RecyclerView.NO_POSITION
        var nextSpanIndex: Int = RecyclerView.NO_POSITION
        var nextSpanSize: Int = RecyclerView.NO_POSITION

        //当前位置, 是否在分组中的第一行中
        val firstSpanGroupIndexInGroup: Boolean
        //当前位置, 是否在分组中的最后一行中
        val lastSpanGroupIndexInGroup: Boolean

        if (spanSizeLookup == null) {
            spanGroupIndex = itemPosition / spanCount
            spanIndex = itemPosition % spanCount
            spanSize = 1

            val firstItemPosition = list.indexOf(params.groupItems.firstOrNull())
            firstSpanGroupIndexInGroup = if (firstItemPosition == -1) {
                false
            } else {
                val firstSpanGroupIndex = firstItemPosition / spanCount
                firstSpanGroupIndex == spanGroupIndex
            }

            //是否在分组中的最后一行
            val lastItemPosition = list.indexOf(params.groupItems.lastOrNull())
            lastSpanGroupIndexInGroup = if (lastItemPosition == -1) {
                false
            } else {
                val lastSpanGroupIndex = lastItemPosition / spanCount
                lastSpanGroupIndex == spanGroupIndex
            }

            //下一个的信息
            if (list.size > nextItemPosition) {
                nextSpanGroupIndex = nextItemPosition / spanCount
                nextSpanIndex = nextItemPosition % spanCount
                nextSpanSize = 1
            }
        } else {
            spanGroupIndex = spanSizeLookup.getSpanGroupIndex(itemPosition, spanCount)
            spanIndex = spanSizeLookup.getSpanIndex(itemPosition, spanCount)
            spanSize = spanSizeLookup.getSpanSize(itemPosition)

            val firstItemPosition = list.indexOf(params.groupItems.firstOrNull())
            firstSpanGroupIndexInGroup = if (firstItemPosition == -1) {
                false
            } else {
                val firstSpanGroupIndex =
                    spanSizeLookup.getSpanGroupIndex(firstItemPosition, spanCount)
                firstSpanGroupIndex == spanGroupIndex
            }

            //是否在分组中的最后一行
            val lastItemPosition = list.indexOf(params.groupItems.lastOrNull())
            lastSpanGroupIndexInGroup = if (lastItemPosition == -1) {
                false
            } else {
                val lastSpanGroupIndex =
                    spanSizeLookup.getSpanGroupIndex(lastItemPosition, spanCount)
                lastSpanGroupIndex == spanGroupIndex
            }

            //下一个的信息
            if (list.size > nextItemPosition) {
                nextSpanGroupIndex = spanSizeLookup.getSpanGroupIndex(nextItemPosition, spanCount)
                nextSpanIndex = spanSizeLookup.getSpanIndex(nextItemPosition, spanCount)
                nextSpanSize = spanSizeLookup.getSpanSize(nextItemPosition)
            }
        }

        if (firstSpanGroupIndexInGroup) {
            edgeGroup = edgeGroup or EDGE_GROUP_TOP
        }
        if (lastSpanGroupIndexInGroup) {
            edgeGroup = edgeGroup or EDGE_GROUP_BOTTOM
        }

        if (spanIndex == 0) {
            //第0列, 肯定是在左边界
            edge = edge or EDGE_LEFT
            edgeGroup = edgeGroup or EDGE_LEFT

            if (params.indexInGroup == 0) {
                edgeGroup = edgeGroup or EDGE_TOP
                edgeGroup = edgeGroup or EDGE_LEFT_TOP
            }
            if (spanSize == spanCount) {
                edgeGroup = edgeGroup or EDGE_RIGHT
                edgeGroup = edgeGroup or EDGE_RIGHT_TOP
            }
            if (params.groupItems.size == 1) {
                edgeGroup = edgeGroup or EDGE_TOP
                edgeGroup = edgeGroup or EDGE_BOTTOM
                edgeGroup = edgeGroup or EDGE_LEFT_BOTTOM
                edgeGroup = edgeGroup or EDGE_RIGHT_BOTTOM
            }
            if (lastSpanGroupIndexInGroup) {
                //第0列, 又在同一组的最后一行
                edgeGroup = edgeGroup or EDGE_BOTTOM
                edgeGroup = edgeGroup or EDGE_LEFT_BOTTOM
            }
            if (spanSize == spanCount) {
                //占满一行
                edge = edge or EDGE_RIGHT
                edgeGroup = edgeGroup or EDGE_GROUP_TOP
                edgeGroup = edgeGroup or EDGE_RIGHT_TOP
            }
        }
        if (spanIndex == spanCount - 1) {
            //最后一列, 肯定是在右边界
            edge = edge or EDGE_RIGHT
            edgeGroup = edgeGroup or EDGE_RIGHT

            if (params.groupItems.size - 1 == itemPosition) {
                edgeGroup = edgeGroup or EDGE_RIGHT_BOTTOM
            }
        }
        if (spanGroupIndex == 0) {
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
            edgeGroup = edgeGroup or EDGE_RIGHT_BOTTOM
        }

        if (itemPosition == list.size - 1) {
            //最后一个, 肯定是在底边界
            edge = edge or EDGE_BOTTOM
            edgeGroup = edgeGroup or EDGE_BOTTOM
        }

        params.edgeGridParams = EdgeGridParams(
            edge,
            edgeGroup,
            itemPosition,
            spanGroupIndex,
            spanIndex,
            spanSize,
            nextItemPosition,
            nextSpanGroupIndex,
            nextSpanIndex,
            nextSpanSize
        )
    }

    params.edgeInGrid = edge
    params.edgeInGroup = edgeGroup

    return params
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

    var itemPosition: Int = RecyclerView.NO_POSITION,
    var spanGroupIndex: Int = RecyclerView.NO_POSITION,
    var spanIndex: Int = RecyclerView.NO_POSITION,
    var spanSize: Int = RecyclerView.NO_POSITION,

    var nextItemPosition: Int = RecyclerView.NO_POSITION,
    var nextSpanGroupIndex: Int = RecyclerView.NO_POSITION,
    var nextSpanIndex: Int = RecyclerView.NO_POSITION,
    var nextSpanSize: Int = RecyclerView.NO_POSITION
)

const val EDGE_NONE = 0x00
//在网格布局的左边界
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

/**边界扩展方法*/
fun ItemGroupParams.isEdgeLeft(): Boolean = edgeInGrid.have(EDGE_LEFT)

fun ItemGroupParams.isEdgeRight(): Boolean = edgeInGrid.have(EDGE_RIGHT)
fun ItemGroupParams.isEdgeTop(): Boolean = edgeInGrid.have(EDGE_TOP)
fun ItemGroupParams.isEdgeBottom(): Boolean = edgeInGrid.have(EDGE_BOTTOM)
fun ItemGroupParams.isEdgeHorizontal(): Boolean = isEdgeLeft() && isEdgeRight()
fun ItemGroupParams.isEdgeVertical(): Boolean = isEdgeTop() && isEdgeBottom()

fun ItemGroupParams.isEdgeGroupLeftTop(): Boolean = edgeInGroup.have(EDGE_LEFT_TOP)
fun ItemGroupParams.isEdgeGroupRightTop(): Boolean = edgeInGroup.have(EDGE_RIGHT_TOP)
fun ItemGroupParams.isEdgeGroupLeftBottom(): Boolean = edgeInGroup.have(EDGE_LEFT_BOTTOM)
fun ItemGroupParams.isEdgeGroupRightBottom(): Boolean = edgeInGroup.have(EDGE_RIGHT_BOTTOM)

fun ItemGroupParams.isEdgeGroupTop(): Boolean = edgeInGroup.have(EDGE_GROUP_TOP)
fun ItemGroupParams.isEdgeGroupBottom(): Boolean = edgeInGroup.have(EDGE_GROUP_BOTTOM)

fun ItemGroupParams.isEdgeGroupHorizontal(): Boolean =
    (isEdgeGroupLeftTop() && isEdgeGroupRightTop()) || (isEdgeGroupLeftBottom() && isEdgeGroupRightBottom())

fun ItemGroupParams.isEdgeGroupVertical(): Boolean =
    (isEdgeGroupLeftTop() && isEdgeGroupLeftBottom()) || (isEdgeGroupRightTop() && isEdgeGroupRightBottom())


