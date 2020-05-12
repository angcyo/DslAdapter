package com.angcyo.dsladapter.dsl

import com.angcyo.dsladapter.demo.R
import com.angcyo.item.DslButtonItem

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/05/11
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class DslMenuItem : DslButtonItem() {
    init {
        itemLayoutId = R.layout.app_item_menu
    }
}