package com.angcyo.dsladapter.demo

import android.widget.Toast
import com.angcyo.dsladapter.*
import com.angcyo.dsladapter.dsl.DslDemoItem
import kotlin.random.Random

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/10/16
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
class GroupDemoActivity : BaseRecyclerActivity() {

    override fun onInitBaseLayoutAfter() {
        super.onInitBaseLayoutAfter()

        recyclerView.setPadding(10 * dpi, 0, 10 * dpi, 0)

        dslAdapter.render {
            setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_LOADING)
        }

        dslViewHolder.postDelay(1000) {

            renderAdapter {
                setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_NONE)

                for (i in 0..Random.nextInt(2, 6)) {
                    dslItem(R.layout.item_group_head) {
                        itemIsGroupHead = true //启动分组折叠
                        itemIsHover = false //关闭悬停
                        itemGroups = mutableListOf("group${i + 1}")
                        itemTopInsert = 10 * dpi

                        itemBindOverride = { itemHolder, itemPosition, adapterItem, _ ->
                            itemHolder.tv(R.id.fold_button)?.text =
                                if (itemGroupExtend) "折叠 $itemPosition" else "展开 $itemPosition"

                            itemHolder.tv(R.id.text_view)?.text = "分组${i + 1}"

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

                    for (j in 0..Random.nextInt(4, 14)) {
                        dslItem(DslDemoItem()) {
                            itemGroups = mutableListOf("group${i + 1}")
                            itemText = "我是第${i + 1}组的第 $j 条数据"

                            itemBindOverride = { itemHolder, _, _, _ ->
                                itemGroupParams.apply {
                                    if (isLastPosition()) {
                                        itemHolder.itemView
                                            .setBackgroundResource(R.drawable.shape_group_footer)
                                    } else {
                                        itemHolder.itemView
                                            .setBackgroundColor(resources.getColor(R.color.default_base_white))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /*var refreshCount = 0

    override fun onRefresh() {
        Toast.makeText(this, "刷新", Toast.LENGTH_SHORT).show()
        dslViewHolder.postDelay(1000) {
            if (refreshCount++ % 2 == 0) {
                refreshLayout.isRefreshing = false
            } else {
                dslAdapter.render {
                    clearItems()
                    refreshLayout.isRefreshing = false
                }
                Toast.makeText(this, "请重试", Toast.LENGTH_SHORT).show()
            }
        }
    }*/
}
