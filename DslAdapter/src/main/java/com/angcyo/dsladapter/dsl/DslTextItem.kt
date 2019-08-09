package com.angcyo.dsladapter.dsl

import android.widget.TextView
import com.angcyo.dsladapter.R

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/08/09
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
class DslTextItem : DslAdapterItem() {
    init {
        itemLayoutId = R.layout.item_text_layout
    }

    override var itemBind: (itemHolder: DslViewHolder, itemPosition: Int, adapterItem: DslAdapterItem) -> Unit =
        { itemHolder, itemPosition, _ ->
            itemHolder.v<TextView>(R.id.text_view).text = "文本位置:$itemPosition"
        }
}