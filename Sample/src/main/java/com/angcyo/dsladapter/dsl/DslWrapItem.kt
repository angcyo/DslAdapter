package com.angcyo.dsladapter.dsl

import android.view.View
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.DslViewHolder
import com.angcyo.dsladapter.demo.R

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/07/12
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class DslWrapItem : DslAdapterItem() {

    /**真正内容的布局id*/
    var itemContentLayoutId: Int? = null
        set(value) {
            field = value
            updateAdapterItem()
        }

    var itemText: CharSequence? = null

    init {
        itemLayoutId = R.layout.item_wrap_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        itemHolder.clear()

        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
        itemHolder.group(R.id.item_wrap_layout)?.apply {
            removeAllViews()
            itemContentLayoutId?.let {
                View.inflate(itemHolder.context, it, this)
            }
        }
        itemHolder.tv(R.id.text_view)?.text = itemText
    }
}