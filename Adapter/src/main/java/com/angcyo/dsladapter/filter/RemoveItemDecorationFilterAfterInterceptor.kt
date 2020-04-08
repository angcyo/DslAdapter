package com.angcyo.dsladapter.filter

import androidx.recyclerview.widget.RecyclerView
import com.angcyo.dsladapter.DslAdapterItem

/**
 * 移除收尾Item的分割线
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/06
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
class RemoveItemDecorationFilterAfterInterceptor : BaseFilterAfterInterceptor() {

    var orientation: Int = RecyclerView.VERTICAL

    var removeConfig: (chain: FilterAfterChain, index: Int, dslAdapterItem: DslAdapterItem) -> Unit =
        { _, _, _ ->

        }

    override fun intercept(chain: FilterAfterChain): List<DslAdapterItem> {
        chain.requestList.firstOrNull()?.apply {
            if (orientation == RecyclerView.VERTICAL) {
                itemTopInsert = 0
            } else {
                itemLeftInsert = 0
            }
        }
        chain.requestList.lastOrNull()?.apply {
            if (orientation == RecyclerView.VERTICAL) {
                itemBottomInsert = 0
            } else {
                itemRightInsert = 0
            }
        }

        chain.requestList.forEachIndexed { index, dslAdapterItem ->
            removeConfig(chain, index, dslAdapterItem)
        }

        return chain.requestList
    }
}