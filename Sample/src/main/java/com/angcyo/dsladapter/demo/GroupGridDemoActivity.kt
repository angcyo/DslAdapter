package com.angcyo.dsladapter.demo

import androidx.recyclerview.widget.GridLayoutManager
import com.angcyo.dsladapter.*
import com.angcyo.dsladapter.dsl.DslDemoItem
import kotlin.random.Random.Default.nextInt

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/10/16
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
class GroupGridDemoActivity : BaseRecyclerActivity() {

    override fun onInitBaseLayoutAfter() {

        val spanCount = 4
        val spanSizeLookup: GridLayoutManager.SpanSizeLookup

        recyclerView.layoutManager = GridLayoutManager(this, spanCount).apply {
            spanSizeLookup = dslSpanSizeLookup(dslAdapter)
        }

        dslAdapter.render {
            setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_LOADING)
        }

        dslViewHolder.postDelay(1000) {

            renderAdapter {
                setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_NONE)
                for (i in 0..4) {
                    dslItem(R.layout.item_group_head) {
                        itemIsGroupHead = true //启动分组折叠
                        itemGroups = mutableListOf("group${i + 1}")
                        itemTopInsert = 10 * dpi

                        onSetItemOffset = {
                            itemGroupParams.apply {
                                if (isEdgeLeft()) {
                                    it.left = 10 * dpi
                                }
                                if (isEdgeRight()) {
                                    it.right = 10 * dpi
                                }
                                if (isEdgeGroupBottom()) {
                                    it.bottom = 10 * dpi
                                }
                            }
                        }

                        itemBindOverride = { itemHolder, itemPosition, adapterItem, _ ->
                            itemHolder.tv(R.id.fold_button)?.text =
                                if (itemGroupExtend) "折叠 $itemPosition" else "展开 $itemPosition"

                            itemHolder.tv(R.id.text_view)?.text =
                                "分组${i + 1}" +
                                        "    :${itemHolder.adapterPosition}/${itemHolder.layoutPosition}" +
                                        "    :${
                                            spanSizeLookup.getSpanIndex(
                                                itemPosition,
                                                spanCount
                                            )
                                        }/${spanSizeLookup.getSpanSize(itemPosition)}/${
                                            spanSizeLookup.getSpanGroupIndex(
                                                itemPosition,
                                                spanCount
                                            )
                                        }"

                            itemHolder.click(R.id.fold_button) {
                                itemGroupExtend = !itemGroupExtend
                            }

                            itemGroupParams.apply {
                                if (isOnlyOne()) {
                                    itemHolder.itemView
                                        .setBackgroundResource(R.drawable.shape_group_all)
                                } else if (isEdgeGroupTop()) {
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

                            onSetItemOffset = {
                                itemGroupParams.apply {
                                    if (isEdgeLeft()) {
                                        it.left = 10 * dpi
                                    }
                                    if (isEdgeRight()) {
                                        it.right = 10 * dpi
                                    }
                                    if (isEdgeGroupBottom()) {
                                        it.bottom = 10 * dpi
                                    }
                                }
                            }

                            itemBindOverride = { itemHolder, itemPosition, _, _ ->
                                itemGroupParams.apply {

                                    when {
                                        isEdgeGroupHorizontal() -> itemHolder.itemView
                                            .setBackgroundResource(R.drawable.shape_group_footer)
                                        isEdgeGroupLeftBottom() -> itemHolder.itemView
                                            .setBackgroundResource(R.drawable.shape_left_bottom)
                                        isEdgeGroupRightBottom() -> itemHolder.itemView
                                            .setBackgroundResource(R.drawable.shape_right_bottom)
                                        else -> itemHolder.itemView
                                            .setBackgroundColor(resources.getColor(R.color.default_base_white))
                                    }
                                }

                                itemHolder.tv(R.id.text_view)?.text =
                                    "${
                                        spanSizeLookup.getSpanIndex(
                                            itemPosition,
                                            spanCount
                                        )
                                    }/${spanSizeLookup.getSpanSize(itemPosition)}/${
                                        spanSizeLookup.getSpanGroupIndex(
                                            itemPosition,
                                            spanCount
                                        )
                                    }"
                            }
                        }
                    }
                }
            }
        }
    }
}
