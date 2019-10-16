package com.angcyo.dsladapter.dsl

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.DslViewHolder
import com.angcyo.dsladapter.R

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/10/16
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
class DslDemoItem : DslAdapterItem() {
    init {
        itemLayoutId = R.layout.item_demo_list
    }

    var itemText: CharSequence? = null

    var onItemClick: () -> Unit = { }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem)
        itemHolder.tv(R.id.text_view).text = itemText
        itemHolder.clickItem {
            onItemClick()
        }
    }
}