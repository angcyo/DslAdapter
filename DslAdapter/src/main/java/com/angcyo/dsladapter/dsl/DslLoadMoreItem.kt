package com.angcyo.dsladapter.dsl

import android.widget.TextView
import com.angcyo.dsladapter.R

/**
 * [Adapter] 加载更多实现
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/08/09
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
open class DslLoadMoreItem : DslAdapterItem() {
    init {
        itemLayoutId = R.layout.item_load_more
    }

    companion object {
        const val ADAPTER_LOAD_NORMAL = 0
        const val ADAPTER_LOAD_LOADING = 1
        const val ADAPTER_LOAD_ERROR = 2
        const val ADAPTER_LOAD_NO_MORE = 3
    }

    /**是否激活加载更多*/
    var itemEnableLoadMore = true
        set(value) {
            field = value
            itemLoadMoreStatus = ADAPTER_LOAD_NORMAL
        }

    /**加载更多当前的状态*/
    var itemLoadMoreStatus: Int = ADAPTER_LOAD_NORMAL

    /**加载更多回调*/
    var onLoadMore: (DslViewHolder) -> Unit = {}

    override var itemBind: (itemHolder: DslViewHolder, itemPosition: Int, adapterItem: DslAdapterItem) -> Unit =
        { itemHolder, _, _ ->

            /*具体逻辑, 自行处理*/
            itemHolder.v<TextView>(R.id.text_view).text = "加载更多: ${when (itemLoadMoreStatus) {
                ADAPTER_LOAD_NORMAL -> "加载更多中..."
                ADAPTER_LOAD_LOADING -> "加载更多中..."
                ADAPTER_LOAD_ERROR -> "加载异常"
                ADAPTER_LOAD_NO_MORE -> "我是有底线的"
                else -> "未知状态"
            }}"

            if (itemEnableLoadMore) {
                if (itemLoadMoreStatus == ADAPTER_LOAD_NORMAL) {
                    //错误和正常的情况下, 才触发加载跟多
                    itemLoadMoreStatus = ADAPTER_LOAD_LOADING
                    onLoadMore(itemHolder)
                }
            }
        }

    override var onItemViewDetachedToWindow: (itemHolder: DslViewHolder) -> Unit = {
        if (itemEnableLoadMore) {
            //加载失败时, 下次是否还需要加载更多?
            if (itemLoadMoreStatus == ADAPTER_LOAD_ERROR) {
                itemLoadMoreStatus = ADAPTER_LOAD_NORMAL
            }
        }
    }
}