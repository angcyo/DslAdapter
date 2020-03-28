package com.angcyo.dsladapter.demo

import android.graphics.Color
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import com.angcyo.dsladapter.*
import com.angcyo.dsladapter.ItemSelectorHelper.Companion.MODEL_MULTI
import com.angcyo.dsladapter.ItemSelectorHelper.Companion.MODEL_NORMAL
import com.angcyo.dsladapter.ItemSelectorHelper.Companion.MODEL_SINGLE
import com.angcyo.dsladapter.dsl.DslDemoItem
import kotlin.random.Random.Default.nextInt

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/10/16
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
class SelectorDemoActivity : BaseRecyclerActivity() {

    override fun getBaseLayoutId(): Int {
        return R.layout.activity_selector_demo
    }

    //固定选中
    val fixedItemList = mutableListOf<DslAdapterItem>()
    var isSelectorAll = false

    override fun onInitBaseLayoutAfter() {
        super.onInitBaseLayoutAfter()

        //滑动选择支持
        recyclerView.addOnItemTouchListener(SlidingSelectorHelper(applicationContext, dslAdapter))

        //单选/多选 监听
        dslAdapter.itemSelectorHelper.onItemSelectorListener = object :
            OnItemSelectorListener {
            override fun onSelectorItemChange(
                selectorItems: MutableList<DslAdapterItem>,
                selectorIndexList: MutableList<Int>,
                isSelectorAll: Boolean,
                selectorParams: SelectorParams
            ) {
                super.onSelectorItemChange(
                    selectorItems,
                    selectorIndexList,
                    isSelectorAll,
                    selectorParams
                )

                this@SelectorDemoActivity.isSelectorAll = isSelectorAll

                dslViewHolder.tv(R.id.tip_view)?.text = when {
                    isSelectorAll -> "全部选中, 共 ${selectorItems.size} 项"
                    selectorItems.isEmpty() -> "未选中"
                    else -> "选中: ${selectorItems.size} 项"
                }
            }
        }

        //控制按钮事件
        dslViewHolder.click(R.id.normal) {
            dslAdapter.itemSelectorHelper.selectorAll(SelectorParams(selector = false.toSelectOption()))
            dslAdapter.itemSelectorHelper.selectorModel = MODEL_NORMAL
        }
        dslViewHolder.click(R.id.single) {
            dslAdapter.itemSelectorHelper.selectorAll(SelectorParams(selector = false.toSelectOption()))
            dslAdapter.itemSelectorHelper.selectorModel = MODEL_SINGLE
        }
        dslViewHolder.click(R.id.multi) {
            dslAdapter.itemSelectorHelper.selectorAll(SelectorParams(selector = false.toSelectOption()))
            dslAdapter.itemSelectorHelper.selectorModel = MODEL_MULTI
        }
        dslViewHolder.click(R.id.all) {
            dslAdapter.itemSelectorHelper.selectorModel = MODEL_MULTI
            dslAdapter.itemSelectorHelper.selectorAll(SelectorParams(selector = (!isSelectorAll).toSelectOption()))
        }

        recyclerView.layoutManager =
            GridLayoutManager(this, 4).apply {
                dslSpanSizeLookup(dslAdapter)
            }

        //渲染adapter数据
        renderAdapter {
            //默认的选择模式
            itemSelectorHelper.selectorModel = MODEL_MULTI

            //切换到加载中...
            setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_LOADING)

            //模拟网络操作
            dslViewHolder.postDelay(1000) {
                setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_NONE)

                onRefresh()
            }
        }

        Toast.makeText(this, "长按Item, 可以滑动选择", Toast.LENGTH_LONG).show()
    }

    override fun onRefresh() {
        super.onRefresh()
        dslAdapter.resetItem(listOf())
        renderAdapter {

            for (i in 0..nextInt(160, 360)) {
                dslItem(DslDemoItem()) {

                    //初始化固定列表
                    if (i < 10 && i % 3 == 0) {
                        fixedItemList.add(this)
                    }

                    onSetItemOffset = {
                        val offset = 10 * dpi
                        it.set(0, offset, offset, 0)
                        itemGroupParams.apply {
                            if (isEdgeLeft()) {
                                it.left = 10 * dpi
                            }
                            if (isEdgeGroupBottom()) {
                                it.bottom = 10 * dpi
                            }
                        }
                    }
                    itemBindOverride = { itemHolder, itemPosition, adapterItem, _ ->
                        itemHolder.itemView.apply {
                            setBackgroundColor(
                                when {
                                    fixedItemList.contains(adapterItem) -> Color.GRAY
                                    itemIsSelected -> Color.GREEN
                                    else -> Color.WHITE
                                }
                            )
                        }
                        itemHolder.tv(R.id.text_view)?.apply {
                            height = 100 * dpi
                            text =
                                "选我 $itemPosition \n${if (itemIsSelected) "true" else "false"}"
                        }
                    }
                    itemClick = {
                        updateItemSelector(!itemIsSelected)
                    }
                }
            }

            onDispatchUpdatesAfterOnce = {
                itemSelectorHelper.fixedSelectorItemList = fixedItemList
            }
        }
    }
}
