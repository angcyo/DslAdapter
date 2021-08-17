package com.angcyo.dsladapter.dsl

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.DslViewHolder
import com.angcyo.dsladapter.demo.R
import com.angcyo.dsladapter.demo.databinding.ItemViewBindingItemBinding
import com.angcyo.dsladapter.nowTime

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/08/17
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class DslViewBindingItem : DslAdapterItem() {

    var itemViewBindingItemBinding: ItemViewBindingItemBinding? = null

    init {
        itemLayoutId = R.layout.item_view_binding_item
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
        itemViewBindingItemBinding =
            itemViewBindingItemBinding ?: ItemViewBindingItemBinding.bind(itemHolder.itemView)

        itemViewBindingItemBinding?.apply {
            button.setOnClickListener {
                textView.animate().rotationBy(90f).setDuration(300).start()
                textView.text = "${nowTime()}"
            }
        }
    }
}