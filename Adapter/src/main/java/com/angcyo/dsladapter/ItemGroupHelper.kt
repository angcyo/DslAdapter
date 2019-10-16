package com.angcyo.dsladapter

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

    for (newItem in this.getValidFilterDataList()) {
        if (newItem == dslAdapterItem || dslAdapterItem.isItemInGroups(newItem)) {
            params.groupItems.add(newItem)
        }
    }

    params.indexInGroup = params.groupItems.indexOf(dslAdapterItem)

    return params
}

data class ItemGroupParams(
    var indexInGroup: Int = RecyclerView.NO_POSITION,
    var currentAdapterItem: DslAdapterItem? = null,
    var groupItems: MutableList<DslAdapterItem> = mutableListOf()
)

/**仅有一个*/
fun ItemGroupParams.isOnlyOne(): Boolean = groupItems.size == 1

/**是否是第一个位置*/
fun ItemGroupParams.isFirstPosition(): Boolean = indexInGroup == 0 && currentAdapterItem != null

/**是否是最后一个位置*/
fun ItemGroupParams.isLastPosition(): Boolean =
    currentAdapterItem != null && indexInGroup == groupItems.lastIndex