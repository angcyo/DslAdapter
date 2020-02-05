package com.angcyo.dsladapter.demo

import android.graphics.Color
import android.os.SystemClock
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import android.widget.Toast
import androidx.recyclerview.widget.ItemTouchHelper
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

        recyclerView.layoutManager =
            StaggeredGridLayoutManager(
                3,
                RecyclerView.VERTICAL
            )

        DragCallbackHelper().apply {
            attachToRecyclerView(recyclerView)
            //开启横向侧滑删除
            itemSwipeFlag = FLAG_HORIZONTAL

            onClearView = { _, _ ->

                /*
                 * [DslAdapter]默认刷新数据是通过[Diff]实现的,
                 * 所以[thisAreItemsTheSame][thisAreContentsTheSame],
                 * 将会影响[notifyItemChanged]的调用.
                 *
                 * 更好的做法应该是通过[thisAreContentsTheSame]控制是否需要刷新界面.
                 *
                 * 这里强制刷界面.如果界面不受[position]的影响, 就可以不用刷新界面.
                 * */
                if (_dragHappened) {
                    dslAdapter.updateAllItem()
                }
            }

            /**如果是快速的侧滑删除, [clearView] 可能无法被执行, 所以对[Swipe]特殊处理以下*/
            onSelectedChanged = { _, actionState ->
                if (actionState == ItemTouchHelper.ACTION_STATE_IDLE && _swipeHappened) {
                    dslAdapter.updateAllItem()
                }
            }
        }

        renderAdapter {
            setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_LOADING)

            dslViewHolder.postDelay(1000) {
                setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_NONE)

                dslImageItem {
                    itemSpanCount = -1
                    marginVertical(4 * dpi, 2 * dpi)
                    marginHorizontal(4 * dpi, 2 * dpi)
                }

                dslImageItem {
                    itemSpanCount = -1
                    marginVertical(4 * dpi, 2 * dpi)
                    marginHorizontal(4 * dpi, 2 * dpi)
                }

                val random = Random(SystemClock.uptimeMillis())
                for (i in 0..100) {
                    dslItem(ColorItem()) {
                        itemText = "原位置$i"
                        itemTag = itemText
                        itemColor = randomColor(random)
                        marginVertical(4 * dpi, 2 * dpi)
                        marginHorizontal(4 * dpi, 2 * dpi)
                    }
                }

                来点数据()
            }

        }

        Toast.makeText(this, "长按拖拽, 左右侧滑删除", Toast.LENGTH_LONG).show()
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
    init {
        itemLayoutId = R.layout.item_color_item
    }

    var itemColor = Color.WHITE

    var itemText = "文本"

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