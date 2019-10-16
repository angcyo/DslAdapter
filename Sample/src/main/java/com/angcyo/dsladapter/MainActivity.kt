package com.angcyo.dsladapter

import android.app.Activity
import android.content.Intent
import android.widget.TextView
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

        dslAdapter.setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_LOADING)

        dslAdapter.dslItem(DslDemoItem()) {
            itemText = "情感图状态使用示例"
            onItemClick = {
                start(AdapterStatusActivity::class.java)
            }
        }

        dslAdapter.dslItem(DslDemoItem()) {
            itemText = "加载更多使用示例"
            onItemClick = {
                start(LoadMoreActivity::class.java)
            }
        }

        dslViewHolder.postDelay(1000) {
            dslAdapter.setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_NONE)
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
            itemBind = { itemHolder, itemPosition, adapterItem ->
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
            itemBind = { itemHolder, itemPosition, _ ->
                itemHolder.v<TextView>(R.id.text_view).text = "文本位置:$itemPosition"
            }
        }

        for (j in 0..0) {
            //2种使用item的方式, 喜欢哪种方式, 就用哪一种
            dslAdapter.dslImageItem()
            dslAdapter.dslItem(R.layout.item_image_layout) {
                itemBind = { itemHolder, itemPosition, _ ->
                    itemHolder.v<TextView>(R.id.text_view).text = "文本位置:$itemPosition"
                }
            }
        }
    }
}