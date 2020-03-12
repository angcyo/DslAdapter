package com.angcyo.dsladapter

/**
 * [RecyclerView.Adapter] 加载更多实现
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/08/09
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
open class DslLoadMoreItem : BaseDslStateItem() {

    companion object {
        /**正常状态, 等待加载更多*/
        const val ADAPTER_LOAD_NORMAL = 0

        /**加载更多中*/
        const val ADAPTER_LOAD_LOADING = 1

        /**无更多*/
        const val ADAPTER_LOAD_NO_MORE = 2

        /**加载失败*/
        const val ADAPTER_LOAD_ERROR = 10

        /**加载失败, 自动重试中*/
        const val _ADAPTER_LOAD_RETRY = 11
    }

    /**是否激活加载更多, 默认关闭*/
    override var itemStateEnable: Boolean = false
        set(value) {
            field = value
            itemState = ADAPTER_LOAD_NORMAL
        }

    /**加载更多回调*/
    var onLoadMore: (DslViewHolder) -> Unit = {
        L.i("[DslLoadMoreItem] 触发加载更多")
    }

    //是否已经在加载更多
    var _isLoadMore = false

    init {
        itemStateLayoutMap[ADAPTER_LOAD_NORMAL] = R.layout.base_loading_layout
        itemStateLayoutMap[ADAPTER_LOAD_LOADING] = R.layout.base_loading_layout
        itemStateLayoutMap[ADAPTER_LOAD_NO_MORE] = R.layout.base_no_more_layout
        itemStateLayoutMap[ADAPTER_LOAD_ERROR] = R.layout.base_error_layout
        itemStateLayoutMap[_ADAPTER_LOAD_RETRY] = R.layout.base_error_layout

        thisAreContentsTheSame = { _, _ ->
            false
        }
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem
    ) {
        itemHolder.itemView.setWidthHeight(-1, -2)
        super.onItemBind(itemHolder, itemPosition, adapterItem)
    }

    override fun _onBindStateLayout(itemHolder: DslViewHolder, state: Int) {
        super._onBindStateLayout(itemHolder, state)

        if (itemStateEnable) {
            if (itemState == ADAPTER_LOAD_NORMAL || itemState == ADAPTER_LOAD_LOADING) {
                _notifyLoadMore(itemHolder)
            } else if (itemState == ADAPTER_LOAD_ERROR) {
                itemHolder.clickItem {
                    if (itemState == ADAPTER_LOAD_ERROR || itemState == _ADAPTER_LOAD_RETRY) {
                        //失败的情况下, 点击触发重新加载
                        _notifyLoadMore(itemHolder)
                        updateAdapterItem()
                    }
                }
            } else {
                itemHolder.itemView.isClickable = false
            }
        } else {
            itemHolder.itemView.isClickable = false
        }
    }

    open fun _notifyLoadMore(itemHolder: DslViewHolder) {
        itemState = ADAPTER_LOAD_LOADING
        if (!_isLoadMore) {
            _isLoadMore = true
            itemHolder.post { onLoadMore(itemHolder) }
        }
    }

    override fun _onItemStateChange(old: Int, value: Int) {
        if (old != value && value != ADAPTER_LOAD_LOADING) {
            _isLoadMore = false
        }
        super._onItemStateChange(old, value)
    }

    override fun onItemViewDetachedToWindow(itemHolder: DslViewHolder, itemPosition: Int) {
        super.onItemViewDetachedToWindow(itemHolder, itemPosition)
        if (itemStateEnable) {
            //加载失败时, 下次是否还需要加载更多?
            if (itemState == ADAPTER_LOAD_ERROR) {
                itemState =
                    _ADAPTER_LOAD_RETRY
            }
        }
    }
}