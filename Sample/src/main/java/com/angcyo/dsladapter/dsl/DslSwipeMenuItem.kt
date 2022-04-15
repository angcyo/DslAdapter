package com.angcyo.dsladapter.dsl

import android.widget.Toast
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.DslViewHolder
import com.angcyo.dsladapter.SwipeMenuHelper
import com.angcyo.dsladapter.demo.R
import com.angcyo.dsladapter.resetDslItem
import com.angcyo.item.DslTextInfoItem
import com.angcyo.item._color
import com.angcyo.item.base.LibInitProvider

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/05/11
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class DslSwipeMenuItem : DslTextInfoItem() {

    init {
        itemLayoutId = R.layout.app_item_swipe_menu
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemSwipeMenuType =
            if (itemPosition % 2 == 0) SwipeMenuHelper.SWIPE_MENU_TYPE_DEFAULT else SwipeMenuHelper.SWIPE_MENU_TYPE_FLOWING

        itemHolder.group(R.id.menu_wrap_layout)?.resetDslItem(
            if (itemPosition % 2 == 0) {
                mutableListOf(DslMenuItem().apply {
                    itemButtonText = "删除"
                    configButtonStyle {
                        gradientStyle(_color(R.color.error))
                    }
                    itemClick = {
                        toast("删除")
                        closeSwipeMenu(itemHolder)
                        deleteItem(itemHolder)
                    }
                }, DslMenuItem().apply {
                    itemButtonText = "取消置顶"
                    configButtonStyle {
                        gradientStyle(_color(R.color.info))
                    }
                    itemClick = {
                        toast("取消置顶")
                        closeSwipeMenu(itemHolder)
                    }
                }, DslMenuItem().apply {
                    itemButtonText = "标记未读"
                    configButtonStyle {
                        gradientStyle(_color(R.color.success))
                    }
                    itemClick = {
                        toast("标记未读")
                        closeSwipeMenu(itemHolder)
                    }
                })
            } else {
                mutableListOf(DslMenuItem().apply {
                    itemButtonText = "删除"
                    configButtonStyle {
                        gradientStyle(_color(R.color.error))
                    }
                    itemClick = {
                        toast("删除")
                        closeSwipeMenu(itemHolder)
                        deleteItem(itemHolder)
                    }
                }, DslMenuItem().apply {
                    itemButtonText = "已置顶"
                    configButtonStyle {
                        gradientStyle(_color(R.color.info))
                    }
                    itemClick = {
                        toast("已置顶")
                        closeSwipeMenu(itemHolder)
                    }
                })
            }
        )

        itemHolder.click(R.id.lib_content_wrap_layout) {
            _itemSwipeMenuHelper?.toggleSwipeMenu(itemHolder)
        }
    }

    fun closeSwipeMenu(itemHolder: DslViewHolder) {
        _itemSwipeMenuHelper?.closeSwipeMenu(itemHolder)
    }

    fun deleteItem(itemHolder: DslViewHolder) {
        if (itemHolder.adapterPosition % 2 == 0) {
            itemHolder.postDelay(300) {
                itemDslAdapter?.removeItem(this)
            }
        } else {
            itemDslAdapter?.removeItem(this)
        }
    }

    fun toast(text: CharSequence) {
        Toast.makeText(LibInitProvider.contentProvider, text, Toast.LENGTH_SHORT).show()
    }
}