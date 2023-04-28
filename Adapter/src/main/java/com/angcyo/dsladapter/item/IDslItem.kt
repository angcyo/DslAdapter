package com.angcyo.dsladapter.item

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.DslViewHolder
import com.angcyo.dsladapter.annotation.ItemConfig
import com.angcyo.dsladapter.isFullWidthItem

/**
 * 空实现, 方便查找实现类
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/07/16
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
interface IDslItem {

    /**统一初始化入口*/
    fun initItemConfig(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        //default
    }

    /**是否是占满宽度的item*/
    fun isFullWidthItem(item: DslAdapterItem): Boolean {
        return item.isFullWidthItem()
    }
}

/** 基类, [IDslItem]的配置类 */
@ItemConfig
interface IDslItemConfig {
}