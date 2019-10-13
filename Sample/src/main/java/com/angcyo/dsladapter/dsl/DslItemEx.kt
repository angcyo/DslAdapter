package com.angcyo.dsladapter.dsl

import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.dslCustomItem


/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/08/09
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */

public fun DslAdapter.dslTextItem(config: DslTextItem.() -> Unit = {}) {
    dslCustomItem(DslTextItem(), config)
}

public fun DslAdapter.dslImageItem(config: DslImageItem.() -> Unit = {}) {
    dslCustomItem(DslImageItem(), config)
}