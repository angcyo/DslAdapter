package com.angcyo.dsladapter.dsl

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.DslViewHolder
import com.angcyo.dsladapter.containsPayload
import com.angcyo.dsladapter.demo.MainActivity
import com.angcyo.dsladapter.demo.R

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/10/16
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
class DslDemoItem : DslAdapterItem() {

    var itemText: CharSequence? = null
        set(value) {
            _oldText = field
            field = value
        }

    //存储旧数据
    var _oldText: CharSequence? = null

    init {
        itemLayoutId = R.layout.item_demo_list
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
        itemHolder.tv(R.id.text_view)?.text = itemText

        if (payloads.containsPayload(MainActivity.TAG_UPDATE_DATA)) {
            //识别到刷新标识
            itemHolder.tv(R.id.text_view)?.apply {
                text = "from:$_oldText\nto:${itemText}"
                animate().rotationBy(360f).setDuration(1000).start()
            }
        }
    }
}