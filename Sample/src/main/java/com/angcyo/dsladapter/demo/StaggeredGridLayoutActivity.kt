package com.angcyo.dsladapter.demo

import android.graphics.Color
import android.os.SystemClock
import android.widget.Toast
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.angcyo.dsladapter.*
import com.angcyo.dsladapter.dsl.dslImageItem
import java.util.*

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019-10-30
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
class StaggeredGridLayoutActivity : BaseRecyclerActivity() {

    override fun onInitBaseLayoutAfter() {
        super.onInitBaseLayoutAfter()

        recyclerView.layoutManager = StaggeredGridLayoutManager(3, RecyclerView.VERTICAL)

        DragCallbackHelper().apply {
            attachToRecyclerView(recyclerView)
            //开启横向侧滑删除
            itemSwipeFlag = FLAG_HORIZONTAL

            onClearView = { recyclerView, viewHolder ->

                /*
                 * [DslAdapter]默认刷新数据是通过[Diff]实现的,
                 * 所以[thisAreItemsTheSame][thisAreContentsTheSame],
                 * 将会影响[notifyItemChanged]的调用.
                 *
                 * 更好的做法应该是通过[thisAreContentsTheSame]控制是否需要刷新界面.
                 *
                 * 这里强制刷界面.如果界面不受[position]的影响, 就可以不用刷新界面.
                 * */

                recyclerView.post {
                    if (_dragHappened) {
                        dslAdapter.updateAllItem()
                    }
                }
            }

            onItemMoveChanged = { fromList, toList, fromPosition, toPosition ->
                L.i(buildString {
                    append(
                        when (fromList) {
                            dslAdapter.headerItems -> "从头部列表"
                            dslAdapter.dataItems -> "从列表"
                            dslAdapter.footerItems -> "从底部列表"
                            else -> ""
                        }
                    )
                    append("[$fromPosition] -> ")

                    append(
                        when (toList) {
                            dslAdapter.headerItems -> "到头部列表"
                            dslAdapter.dataItems -> "到列表"
                            dslAdapter.footerItems -> "到底部列表"
                            else -> ""
                        }
                    )
                    append("[$toPosition]")
                })
            }

            /**如果是快速的侧滑删除, [clearView] 可能无法被执行, 所以对[Swipe]特殊处理以下*/
            onSelectedChanged = { _, actionState ->
                if (actionState == ItemTouchHelper.ACTION_STATE_IDLE && _swipeHappened) {
                    dslAdapter.updateAllItem()
                }
            }
        }

        loadData()

        Toast.makeText(this, "长按拖拽, 左右侧滑删除", Toast.LENGTH_LONG).show()
    }

    override fun onRefresh() {
        super.onRefresh()
        loadData()
    }

    fun loadData() {
        renderAdapter {
            clearAllItems()

            setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_LOADING)
            val random = Random(SystemClock.uptimeMillis())

            dslViewHolder.postDelay(1000) {
                setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_NONE)

                changeHeaderItems {
                    for (i in 0..2) {
                        it.add(ColorItem().apply {
                            itemText = "头部:原位置$i"
                            itemTag = itemText
                            itemColor = randomColor(random)
                            margin()
                        })
                    }
                }

                dslImageItem {
                    itemSpanCount = -1
                    margin()
                }

                dslImageItem {
                    itemSpanCount = -1
                    margin()
                }

                for (i in 0..10) {
                    dslItem(ColorItem()) {
                        itemText = "原位置$i"
                        itemTag = itemText
                        itemColor = randomColor(random)
                        margin()
                    }
                }

                changeFooterItems {
                    for (i in 0..2) {
                        it.add(ColorItem().apply {
                            itemText = "尾部:原位置$i"
                            itemTag = itemText
                            itemColor = randomColor(random)
                            margin()
                        })
                    }
                }

                //来点数据()
            }

        }
    }

    fun DslAdapterItem.margin() {
        marginVertical(4 * dpi, 2 * dpi)
        marginHorizontal(4 * dpi, 2 * dpi)
    }
}

fun randomColor(random: Random): Int {
    return randomColor(random, 120, 250)
}

/**
 * 随机颜色, 设置一个最小值, 设置一个最大值, 第三个值在这2者之间随机改变
 */
fun randomColor(random: Random, minValue: Int, maxValue: Int): Int {
    val list = mutableListOf<Int>()
    while (list.size < 3) {
        val a = minValue + random.nextInt(maxValue - minValue)
        list.add(a)
    }
    return Color.rgb(list[0], list[1], list[2])
}

class ColorItem : DslAdapterItem() {

    var itemColor = Color.WHITE

    var itemText = "文本"

    init {
        itemLayoutId = R.layout.item_color_item

        //Diff算法匹配内容是否一致
        thisAreContentsTheSame = { fromItem, newItem, oldItemPosition, newItemPosition ->
            if (newItem is ColorItem) {
                itemText == newItem.itemText
            } else {
                false
            }
        }
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem)
        itemHolder.itemView.apply {
            setBackgroundColor(itemColor)
            if (itemData == null) {
                itemData = if (itemPosition % 2 == 0) 200 * dpi else 150 * dpi
            }
            setHeight(itemData as Int)
        }
        itemHolder.tv(R.id.text_view)?.text =
            "${itemText}\nPosition:$itemPosition\nHeight:${itemData}"
    }
}