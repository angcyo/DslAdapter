package com.angcyo.dsladapter.demo

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.DslViewHolder
import com.angcyo.dsladapter.dpi
import kotlin.random.Random.Default.nextInt

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/11/21
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
class TreeDemoActivity : BaseRecyclerActivity() {

    override fun onInitBaseLayoutAfter() {
        super.onInitBaseLayoutAfter()
        renderAdapter {
            for (i in 0..nextInt(5, 20)) {
                DslTreeItem()() {
                    itemText = "顶级目录$i"

                    onItemLoadSubList = loadSubList(this)
                }
            }
        }
    }

    fun loadSubList(treeItem: DslTreeItem): () -> Unit {
        return {
            treeItem.itemIsLoadSub = true
            if (treeItem.itemSubList.isEmpty()) {
                //加载中
                treeItem.itemSubList.add(DslTreeLoadItem())

                //模拟延迟
                dslViewHolder.postDelay(1000) {
                    treeItem.itemSubList.clear()
                    for (i in 0..nextInt(0, 10)) {
                        treeItem.itemSubList.add(DslTreeItem().apply {
                            itemText = "子目录$i"

                            //到达一定数量数, 模拟无子目录的情况
                            if (treeItem.itemParentList.size > 0 && i > 4) {
                                onItemLoadSubList = {
                                    if (!itemIsLoadSub && itemSubList.isEmpty()) {
                                        //加载中
                                        itemSubList.add(DslTreeLoadItem())

                                        dslViewHolder.postDelay(1000) {
                                            itemSubList.clear()
                                            updateItemDepend()
                                        }
                                    }

                                    itemIsLoadSub = true
                                }
                            } else if (treeItem.itemParentList.size > 0 && i > 2) {
                                itemIsLoadSub = true
                            } else {
                                onItemLoadSubList = this@TreeDemoActivity.loadSubList(this)
                            }
                        })
                    }
                    treeItem.updateItemDepend()
                }
            }
        }
    }
}

class DslTreeItem : DslAdapterItem() {
    init {
        itemLayoutId = R.layout.item_tree_layout
        itemGroupExtend = false
        itemBottomInsert = 1 * dpi
        itemDecorationColor = Color.parseColor("#D5D5D5")

        itemClick = {
            itemGroupExtend = !itemGroupExtend
        }
    }

    var itemText: CharSequence? = null

    //是否加载过子项, 用来控制箭头是否需要展示
    var itemIsLoadSub = false

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem)

        itemHolder.tv(R.id.text_view)?.text = itemText

        //箭头角度控制
        itemHolder.v<View>(R.id.icon_view)?.apply {
            rotation = if (itemGroupExtend) {
                90f
            } else {
                0f
            }

            visibility = if (itemIsLoadSub && itemSubList.isEmpty()) {
                View.INVISIBLE
            } else {
                View.VISIBLE
            }
        }

        //展开or关闭动画控制
        itemHolder.clickItem {
            itemHolder.v<View>(R.id.icon_view)?.run {
                animate()
                    .setDuration(300)
                    .rotation(if (itemGroupExtend) -90f else 90f)
                    .start()
            }
            itemGroupExtend = !itemGroupExtend
        }

        //缩进控制
        itemHolder.v<View>(R.id.icon_view)?.apply {
            (layoutParams as? ViewGroup.MarginLayoutParams)?.run {
                leftMargin = itemParentList.size * 20 * dpi
                layoutParams = this
            }
        }

        //父级展示
        if (itemParentList.isEmpty()) {
            itemHolder.gone(R.id.dark_view)
        } else {
            itemHolder.visible(R.id.dark_view)

            buildString {
                itemParentList.forEach {
                    (it as? DslTreeItem)?.let { item ->
                        append("/")
                        append(item.itemText)
                    }
                }
                itemHolder.tv(R.id.dark_view)?.text = this
            }
        }
    }
}

class DslTreeLoadItem : DslAdapterItem() {
    init {
        itemLayoutId = R.layout.item_tree_load_layout
    }
}