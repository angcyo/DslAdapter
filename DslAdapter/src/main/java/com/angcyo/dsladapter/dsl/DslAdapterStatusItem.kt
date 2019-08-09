package com.angcyo.dsladapter.dsl

import android.widget.TextView
import com.angcyo.dsladapter.R

/**
 * [DslAdapter] 中, 控制情感图显示状态的 [Item]
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/08/09
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
open class DslAdapterStatusItem : DslAdapterItem() {
    init {
        itemLayoutId = R.layout.item_adapter_status
    }

    companion object {
        const val ADAPTER_STATUS_NONE = -1
        const val ADAPTER_STATUS_EMPTY = 0
        const val ADAPTER_STATUS_LOADING = 1
        const val ADAPTER_STATUS_ERROR = 2
    }

    var itemAdapterStatus: Int = ADAPTER_STATUS_NONE

    override var itemBind: (itemHolder: DslViewHolder, itemPosition: Int, adapterItem: DslAdapterItem) -> Unit =
        { itemHolder, _, _ ->

            /*具体逻辑, 自行处理*/
            itemHolder.v<TextView>(R.id.text_view).text = "情感图状态: ${when (itemAdapterStatus) {
                ADAPTER_STATUS_EMPTY -> "空数据"
                ADAPTER_STATUS_LOADING -> "加载中"
                ADAPTER_STATUS_ERROR -> "加载异常"
                else -> "未知状态"
            }}"
        }

    /**返回[true] 表示不需要显示情感图, 即显示[Adapter]原本的内容*/
    open fun isNoStatus() = itemAdapterStatus == ADAPTER_STATUS_NONE
}