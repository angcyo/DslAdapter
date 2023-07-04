package com.angcyo.dsladapter.dsl

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.DslViewHolder
import com.angcyo.dsladapter.demo.R
import com.angcyo.dsladapter.demo.databinding.ItemBindTextLayoutBinding

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/07/14
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class DslBindingTextItem : DslAdapterItem() {

    init {
        itemLayoutId = R.layout.item_bind_text_layout
    }

    override fun onSetItemData(data: Any?) {
        super.onSetItemData(data)
        updateAdapterItem()
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.binding<ItemBindTextLayoutBinding>()?.data = itemData as? BindingData
    }
}