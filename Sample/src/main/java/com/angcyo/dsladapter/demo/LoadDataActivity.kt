package com.angcyo.dsladapter.demo

import com.angcyo.dsladapter.DslAdapterStatusItem
import com.angcyo.dsladapter.data.updateSingleData
import com.angcyo.dsladapter.dpi
import com.angcyo.dsladapter.dsl.DslImageItem
import com.angcyo.dsladapter.dsl.DslTextItem
import com.angcyo.dsladapter.nowTime

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020-04-07
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
class LoadDataActivity : BaseRecyclerActivity() {

    override fun getBaseLayoutId(): Int {
        return R.layout.activity_load_data
    }

    override fun onInitBaseLayoutAfter() {
        super.onInitBaseLayoutAfter()

        dslAdapter.dslLoadMoreItem.onLoadMore = {
            dslViewHolder.postDelay(1000) {
                loadData(loadPage + 1)
            }
        }
        dslAdapter.dslAdapterStatusItem.onRefresh = {
            onRefresh()
        }

        dslAdapter.render {
            setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_LOADING)
        }
    }

    override fun onRefresh() {
        super.onRefresh()
        dslViewHolder.postDelay(1000) {
            loadData(1)
        }
    }

    /**加载数据的页码*/
    var loadPage = 1

    /**当前请求的页码*/
    var requestPage = 1

    fun loadData(page: Int) {
        requestPage = page

        //模拟返回的数据
        val result = mutableListOf<String>()

        /*for (i in 0..nextInt(18, 30)) {
            result.add("列表数据-> requestPage:$page data:$i")
        }*/

        if (page <= 2) {
            for (i in 0..19) {
                result.add("列表数据-> requestPage:$page data:$i")
            }
        }

        if (page == 1) {
            dslAdapter.changeHeaderItems {
                if (it.isEmpty()) {
                    it.add(DslTextItem().apply {
                        itemText = "头部数据1"
                    })
                    it.add(DslImageItem().apply {
                        itemText = "头部数据2"
                    })
                } else {
                    (it[0] as DslTextItem).apply {
                        itemText = "头部数据1 刷新完成:${nowTime()}"
                        updateAdapterItem()
                    }
                    (it[1] as DslImageItem).apply {
                        itemText = "头部数据2 刷新完成:${nowTime()}"
                        updateAdapterItem()
                    }
                }
            }
        }

        loadPage = requestPage

        dslAdapter.updateSingleData<DslTextItem>(result, page, 20) { data ->
            itemTopInsert = 2 * dpi
            itemText = data?.toString()
        }
    }
}
