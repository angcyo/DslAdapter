package com.angcyo.dsladapter.dsl

import android.widget.TextView
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.DslViewHolder
import com.angcyo.dsladapter.className
import com.angcyo.dsladapter.demo.R

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/08/09
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
class DslTextItem : DslAdapterItem() {

    var itemText: CharSequence? = null

    init {
        itemLayoutId = R.layout.item_text_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem)
        itemHolder.v<TextView>(R.id.text_view)?.text =
            itemText ?: "文本位置:$itemPosition ${className()}"
    }
}