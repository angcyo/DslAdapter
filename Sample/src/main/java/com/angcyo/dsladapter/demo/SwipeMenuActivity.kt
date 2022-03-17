package com.angcyo.dsladapter.demo

import android.os.SystemClock
import androidx.recyclerview.widget.ItemTouchHelper
import com.angcyo.dsladapter.DslAdapterStatusItem
import com.angcyo.dsladapter.SwipeMenuHelper
import com.angcyo.dsladapter.SwipeMenuHelper.Companion.SWIPE_MENU_TYPE_DEFAULT
import com.angcyo.dsladapter.SwipeMenuHelper.Companion.SWIPE_MENU_TYPE_FLOWING
import com.angcyo.dsladapter.dpi
import com.angcyo.dsladapter.dsl.DslSwipeMenuItem
import com.angcyo.dsladapter.dsl.DslTextItem
import kotlin.random.Random

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/05/12
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
open class SwipeMenuActivity : BaseRecyclerActivity() {

    var swipeMenuHelper: SwipeMenuHelper? = null

    override fun onInitBaseLayoutAfter() {
        super.onInitBaseLayoutAfter()
        dslAdapter.dslAdapterStatusItem.onRefresh = {
            onRefresh()
        }
        dslAdapter.render {
            setLoadMoreEnable(false)
            setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_LOADING)
        }
        swipeMenuHelper = SwipeMenuHelper.install(dslViewHolder.v(R.id.base_recycler_view))
    }

    override fun onRefresh() {
        super.onRefresh()

        dslAdapter.render {
            setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_NONE)

            clearItems()

            DslTextItem()() {
                itemText = "头部数据1(无侧滑菜单)"
                itemSwipeMenuEnable = false
            }
            DslTextItem()() {
                itemText = "头部数据2(无侧滑菜单)"
                itemSwipeMenuEnable = false
            }

            for (i in 0..Random.nextInt(2, 100)) {
                DslSwipeMenuItem()() {
                    itemSwipeMenuFlag = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
                    when (i) {
                        2 -> {
                            itemInfoText = "支持动态设置菜单"
                        }
                        3 -> {
                            itemInfoText = "支持菜单滑动展开的2种形式"
                        }
                        4 -> {
                            itemSwipeMenuType = SWIPE_MENU_TYPE_DEFAULT
                            itemInfoText = "SWIPE_MENU_TYPE_DEFAULT\n默认样式, 菜单固定在底部"
                        }
                        5 -> {
                            itemSwipeMenuType = SWIPE_MENU_TYPE_FLOWING
                            itemInfoText = "SWIPE_MENU_TYPE_FLOWING\n菜单跟随内容一起滑动"
                        }
                        6 -> {
                            itemInfoText = "支持单独设置左滑/右滑打开菜单"
                        }
                        7 -> {
                            itemInfoText = "仅支持左滑"
                            itemSwipeMenuFlag = ItemTouchHelper.LEFT
                        }
                        8 -> {
                            itemInfoText = "仅支持右滑"
                            itemSwipeMenuFlag = ItemTouchHelper.RIGHT
                        }
                        9 -> {
                            itemInfoText = "关闭滑动"
                            itemSwipeMenuEnable = false
                        }
                        else -> {
                            itemInfoText = "支持左右滑动的侧滑菜单...$i"
                        }
                    }
                    itemDarkText = "${SystemClock.uptimeMillis()}"
                    itemTopInsert = 1 * dpi
                }
            }

            DslTextItem()() {
                itemText = "尾部数据1(无侧滑菜单)"
                itemSwipeMenuEnable = false
            }
            DslTextItem()() {
                itemText = "尾部数据2(无侧滑菜单)"
                itemSwipeMenuEnable = false
            }
        }
    }
}