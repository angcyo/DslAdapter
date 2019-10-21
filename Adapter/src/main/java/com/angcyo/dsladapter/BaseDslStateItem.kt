package com.angcyo.dsladapter

import android.view.View

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/10/21
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
abstract class BaseDslStateItem : DslAdapterItem() {
    init {
        itemLayoutId = R.layout.item_base_state
    }

    /**
     * [key] 是状态
     * [value] 是对应的布局文件id
     * */
    val itemStateLayoutMap = hashMapOf<Int, Int>()

    /**当前的布局状态*/
    var itemState = -1
        set(value) {
            val old = field
            field = value
            _onItemStateChange(old, value)
        }

    var onItemStateChange: (from: Int, to: Int) -> Unit = { _, _ -> }

    var onBindStateLayout: (itemHolder: DslViewHolder, state: Int) -> Unit = { _, _ -> }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem)

        itemHolder.clear()

        val stateLayout = itemStateLayoutMap[itemState]
        itemHolder.group(R.id.item_wrap_layout).apply {
            if (stateLayout == null) {
                //没有状态对应的布局文件
                removeAllViews()
            } else {
                var inflate = true
                if (childCount > 0) {
                    val tagLayout = getChildAt(0).getTag(R.id.tag)
                    if (tagLayout == stateLayout) {
                        //已经存在相同状态的布局
                        inflate = false
                    } else {
                        removeAllViews()
                    }
                }

                //填充布局, 如果需要
                if (inflate) {
                    inflate(stateLayout, true)
                    val view = getChildAt(0)
                    view.visibility = View.VISIBLE
                    view.setTag(R.id.tag, stateLayout)
                }

                _onBindStateLayout(itemHolder, itemState)
            }
        }
    }

    open fun _onBindStateLayout(itemHolder: DslViewHolder, state: Int) {
        onBindStateLayout(itemHolder, state)
    }

    open fun _onItemStateChange(old: Int, value: Int) {
        if (old != value) {
            onItemStateChange(old, value)
        }
    }
}