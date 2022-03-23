package com.angcyo.dsladapter.dsl

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.DslViewHolder
import com.angcyo.dsladapter.demo.R
import kotlin.random.Random

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/03/23
 */
class DslUpdateParentItem : DslAdapterItem() {

    var itemUpdateData: String? = null

    init {
        itemTag = "DslUpdateParentItem"
        itemLayoutId = R.layout.item_update_parent_layout

        //关键点: 哪些item需要收到更新
        isItemInUpdateList = { checkItem, itemIndex ->
            checkItem is DslUpdateChildItem
        }
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.click(R.id.button) {
            val int = Random.nextInt(1, Int.MAX_VALUE)
            itemUpdateData = "更新传递的数据(${int})"
            itemHolder.tv(R.id.text)?.text = "已通知数据更新($int)"

            //关键点: 触发更新
            updateItemDepend()
        }
    }

}