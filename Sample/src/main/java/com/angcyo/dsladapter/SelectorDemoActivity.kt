package com.angcyo.dsladapter

import android.support.v7.widget.GridLayoutManager
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

    override fun onInitBaseLayoutAfter() {
        super.onInitBaseLayoutAfter()

        val spanCount = 4
        val spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (!dslAdapter.dslAdapterStatusItem.isNoStatus() ||
                    dslAdapter.getItemData(position)?.itemIsGroupHead == true
                ) {
                    spanCount
                } else {
                    1
                }
            }
        }

        recyclerView.layoutManager = GridLayoutManager(this, spanCount).apply {
            this.spanSizeLookup = spanSizeLookup
        }

        dslAdapter.setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_LOADING)

        dslViewHolder.postDelay(1000) {
            dslAdapter.setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_NONE)

            renderAdapter {

                for (i in 0..4) {
                    dslItem(R.layout.item_group_head) {
                        itemIsGroupHead = true //启动分组折叠
                        itemIsHover = false //关闭悬停
                        itemGroups = mutableListOf("group${i + 1}")
                        itemTopInsert = 10 * dpi

                        onItemBindOverride = { itemHolder, itemPosition, adapterItem ->
                            itemHolder.tv(R.id.fold_button).text =
                                if (itemGroupExtend) "折叠 $itemPosition" else "展开 $itemPosition"

                            val params =
                                itemHolder.itemView.layoutParams as? GridLayoutManager.LayoutParams

                            itemHolder.tv(R.id.text_view).text =
                                "分组${i + 1}" +
                                        "    :${itemHolder.adapterPosition}/${itemHolder.layoutPosition}" +
                                        "    :${params?.spanIndex}/${params?.spanSize}" +
                                        "    :${spanSizeLookup.getSpanIndex(
                                            itemPosition,
                                            spanCount
                                        )}/${spanSizeLookup.getSpanSize(itemPosition)}/${spanSizeLookup.getSpanGroupIndex(
                                            itemPosition,
                                            spanCount
                                        )}"

                            itemHolder.click(R.id.fold_button) {
                                itemGroupExtend = !itemGroupExtend
                            }

                            itemGroupParams.apply {
                                if (isOnlyOne()) {
                                    itemHolder.itemView
                                        .setBackgroundResource(R.drawable.shape_group_all)
                                } else if (isFirstPosition()) {
                                    itemHolder.itemView
                                        .setBackgroundResource(R.drawable.shape_group_header)
                                } else {
                                    itemHolder.itemView
                                        .setBackgroundColor(resources.getColor(R.color.colorAccent))
                                }
                            }
                        }
                    }

                    for (j in 0..nextInt(4, 14)) {
                        dslItem(DslDemoItem()) {
                            itemGroups = mutableListOf("group${i + 1}")
                            itemText = "我是第${i + 1}组的第 $j 条数据"

                            onItemBindOverride = { itemHolder, itemPosition, _ ->
                                itemGroupParams.apply {
                                    if (isLastPosition()) {
                                        itemHolder.itemView
                                            .setBackgroundResource(R.drawable.shape_group_footer)
                                    } else {
                                        itemHolder.itemView
                                            .setBackgroundColor(resources.getColor(R.color.default_base_white))
                                    }
                                }

                                itemHolder.tv(R.id.text_view).text =
                                    "${spanSizeLookup.getSpanIndex(
                                        itemPosition,
                                        spanCount
                                    )}/${spanSizeLookup.getSpanSize(itemPosition)}/${spanSizeLookup.getSpanGroupIndex(
                                        itemPosition,
                                        spanCount
                                    )}"
                            }
                        }
                    }
                }
            }
        }
    }
}
