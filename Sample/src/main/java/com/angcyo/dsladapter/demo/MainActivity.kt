package com.angcyo.dsladapter.demo

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.widget.TextView
import com.angcyo.dsladapter.*
import com.angcyo.dsladapter.dsl.DslDemoItem
import com.angcyo.dsladapter.dsl.dslImageItem
import com.angcyo.dsladapter.dsl.dslTextItem

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/10/16
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
class MainActivity : BaseRecyclerActivity() {

    override fun onInitBaseLayoutAfter() {
        super.onInitBaseLayoutAfter()

        renderAdapter {
            setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_LOADING)

            dslItem(DslDemoItem()) {
                itemText = "情感图状态使用示例"
                onItemClick = {
                    start(AdapterStatusActivity::class.java)
                }
                itemTopInsert = 2 * dpi //控制顶部分割线的高度
            }

            dslItem(DslDemoItem()) {
                itemText = "加载更多使用示例"
                onItemClick = {
                    start(LoadMoreActivity::class.java)
                }
                itemTopInsert = 4 * dpi
            }

            dslItem(DslDemoItem()) {
                itemText = "群组(线性布局)功能示例"
                onItemClick = {
                    start(GroupDemoActivity::class.java)
                }
                itemTopInsert = 4 * dpi
            }

            dslItem(DslDemoItem()) {
                itemText = "群组(网格布局)功能示例"
                onItemClick = {
                    start(GroupGridDemoActivity::class.java)
                }
                itemTopInsert = 4 * dpi
            }

            dslItem(DslDemoItem()) {
                itemText = "单选/多选示例"
                onItemClick = {
                    start(SelectorDemoActivity::class.java)
                }
                itemTopInsert = 4 * dpi
            }

            dslItem(DslDemoItem()) {
                itemText = "StaggeredGridLayout"
                onItemClick = {
                    start(StaggeredGridLayoutActivity::class.java)
                }
                itemTopInsert = 4 * dpi
            }

            renderEmptyItem()

            dslItem(DslDemoItem()) {
                itemText = "顶部的分割线是红色"
                itemTopInsert = 8 * dpi
                itemDecorationColor = Color.RED //控制分割线的颜色
            }

            dslItem(DslDemoItem()) {
                itemText = "只绘制偏移量的分割线"
                itemTopInsert = 8 * dpi
                itemLeftOffset = 60 * dpi
                itemDecorationColor = Color.BLUE
                onlyDrawOffsetArea = true
            }

            dslItem(DslDemoItem()) {
                itemText = "自定义Drawable的分割线"
                itemBottomInsert = 20 * dpi
                itemDecorationDrawable = resources.getDrawable(R.drawable.shape_decoration)
            }

            dslItem(DslDemoItem()) {
                itemText = "上下都有的分割线"
                itemTopInsert = 8 * dpi
                itemBottomInsert = 8 * dpi
                itemDecorationColor = Color.GREEN
            }

            dslViewHolder.postDelay(1000) {
                setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_NONE)
            }
        }
    }
}

fun Activity.start(cls: Class<*>) {
    val intent = Intent(this, cls)
    startActivity(intent)
}

fun DslAdapter.来点数据() {
    val dslAdapter = this
    for (i in 0..5) {

        dslAdapter.dslItem(R.layout.item_group_head) {
            itemIsGroupHead = true
            onItemBindOverride = { itemHolder, itemPosition, adapterItem ->
                itemHolder.tv(R.id.fold_button).text =
                    if (itemGroupExtend) "折叠 $itemPosition" else "展开 $itemPosition"

                itemHolder.click(R.id.fold_button) {
                    itemGroupExtend = !itemGroupExtend
                }
            }
        }

        //2种使用item的方式, 喜欢哪种方式, 就用哪一种
        dslAdapter.dslTextItem()
        dslAdapter.dslItem(R.layout.item_text_layout) {
            onItemBindOverride = { itemHolder, itemPosition, _ ->
                itemHolder.v<TextView>(R.id.text_view).text = "文本位置:$itemPosition"
            }
        }

        for (j in 0..0) {
            //2种使用item的方式, 喜欢哪种方式, 就用哪一种
            dslAdapter.dslImageItem()
            dslAdapter.dslItem(R.layout.item_image_layout) {
                onItemBindOverride = { itemHolder, itemPosition, _ ->
                    itemHolder.v<TextView>(R.id.text_view).text = "文本位置:$itemPosition"
                }
            }
        }
    }
}