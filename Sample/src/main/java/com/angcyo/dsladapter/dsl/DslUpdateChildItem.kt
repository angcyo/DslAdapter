package com.angcyo.dsladapter.dsl

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.DslViewHolder
import com.angcyo.dsladapter.demo.R

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/03/23
 */
class DslUpdateChildItem : DslAdapterItem() {

    var itemUpdateDataFrom: String? = null
    var itemUpdateTagFrom: String? = null

    init {
        itemLayoutId = R.layout.item_update_child_layout

        //关键点: 收到了来自item的更新
        itemUpdateFrom = {
            if (it is DslUpdateParentItem) {
                itemUpdateTagFrom = it.itemTag
                itemUpdateDataFrom = it.itemUpdateData
            }
            true
        }
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.gone(R.id.text, itemUpdateDataFrom.isNullOrEmpty())
        itemHolder.tv(R.id.text)?.text = "收到来自item:${itemUpdateTagFrom}的数据:${itemUpdateDataFrom}"
    }

}