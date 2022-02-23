package com.angcyo.dsladapter.demo

import android.widget.Toast
import com.angcyo.dsladapter.DslAdapterStatusItem
import com.angcyo.dsladapter.DslLoadMoreItem

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/10/16
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
class LoadMoreActivity : BaseRecyclerActivity() {

    override fun getBaseLayoutId(): Int {
        return R.layout.activity_load_more
    }

    override fun onInitBaseLayoutAfter() {
        super.onInitBaseLayoutAfter()

        dslAdapter.render {
            setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_LOADING)
        }

        dslViewHolder.postDelay(1000) {
            dslAdapter.render {
                setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_NONE)
                来点数据()
                setLoadMoreEnable(true)
            }
        }

        initLoadMore()
    }

    override fun onRefresh() {
        super.onRefresh()

        dslAdapter.render {
            resetItem(listOf())
            来点数据()
        }
    }

    var loadPage = 0
    private fun initLoadMore() {
        dslAdapter.dslLoadMoreItem.onLoadMore = {
            Toast.makeText(this, "加载更多", Toast.LENGTH_SHORT).show()

            it.postDelay(300L) {
                dslAdapter.render {

                    loadPage++
                    if (loadPage == 2) {
                        //模拟加载失败
                        setLoadMore(DslLoadMoreItem.LOAD_MORE_ERROR)
                    } else if (loadPage > 3) {
                        //模拟没有更多
                        setLoadMore(DslLoadMoreItem.LOAD_MORE_NO_MORE)
                    } else {
                        来点数据()

//                    for (i in 0..0) {
//                        dslAdapter.dslItem(R.layout.item_text_layout) {
//                            itemBind = { itemHolder, itemPosition, _ ->
//                                itemHolder.v<TextView>(R.id.text_view).text =
//                                    "新增的数据! 文本位置:$itemPosition"
//                            }
//                        }
//                    }

                        setLoadMore(DslLoadMoreItem.LOAD_MORE_NORMAL)
                    }
                }
            }
        }
        dslViewHolder.click(R.id.load_more_enable) {
            loadPage = 0
            dslAdapter.render {
                setLoadMoreEnable(it.isSelected)
            }
            it.isSelected = !it.isSelected
        }
        dslViewHolder.click(R.id.load_more_error) {
            loadPage = 0
            dslAdapter.render {
                setLoadMore(DslLoadMoreItem.LOAD_MORE_ERROR)
            }
        }
        dslViewHolder.click(R.id.load_more_no) {
            loadPage = 0
            dslAdapter.render {
                setLoadMore(DslLoadMoreItem.LOAD_MORE_NO_MORE)
            }
        }
    }
}
