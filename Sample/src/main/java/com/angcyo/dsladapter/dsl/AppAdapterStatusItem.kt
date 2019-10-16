package com.angcyo.dsladapter.dsl

import android.widget.TextView
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.DslAdapterStatusItem
import com.angcyo.dsladapter.DslViewHolder
import com.angcyo.dsladapter.R

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/10/16
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
class AppAdapterStatusItem : DslAdapterStatusItem() {
    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem)

        if (itemAdapterStatus == ADAPTER_STATUS_LOADING) {
            itemHolder.v<TextView>(R.id.text_view).text = "精彩即将呈现..."
        }
    }
}