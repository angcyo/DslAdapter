package com.angcyo.dsladapter.filter

import com.angcyo.dsladapter.DslAdapterItem

/**
 * 最大显示数量控制
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/25
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
open class MaxItemCountFilterAfterInterceptor : BaseFilterAfterInterceptor() {

    var maxItemCount = Int.MAX_VALUE

    override fun intercept(chain: FilterAfterChain): List<DslAdapterItem> {
        if (chain.requestList.size >= maxItemCount) {
            return chain.requestList.subList(0, maxItemCount)
        }
        return chain.requestList
    }
}