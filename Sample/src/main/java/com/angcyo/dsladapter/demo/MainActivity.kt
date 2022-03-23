package com.angcyo.dsladapter.demo

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.view.Gravity
import android.widget.TextView
import com.angcyo.dsladapter.*
import com.angcyo.dsladapter.dsl.*
import com.angcyo.dsladapter.filter.batchLoad

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/10/16
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
class MainActivity : BaseRecyclerActivity() {

    companion object {
        val TAG_UPDATE_DATA = "update_data"
    }

    override fun onInitBaseLayoutAfter() {
        super.onInitBaseLayoutAfter()

        renderAdapter {
            //设置情感图状态, loading
            setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_LOADING)

            /**
             * 扩展方式 追加[DslAdapterItem]
             * */
            dslItem(DslDemoItem()) {
                itemText = "情感图状态使用示例"
                itemClick = {
                    start(AdapterStatusActivity::class.java)
                }
                itemTopInsert = 2 * dpi //控制顶部分割线的高度
            }

            dslItem(DslDemoItem()) {
                itemText = "加载更多使用示例"
                itemClick = {
                    start(LoadMoreActivity::class.java)
                }
                itemTopInsert = 4 * dpi
            }

            /**
             * [invoke]运算符重载方式 追加[DslAdapterItem]
             * */
            DslDemoItem()() {
                itemText = "群组(线性布局)功能示例"
                itemClick = {
                    start(GroupDemoActivity::class.java)
                }
                itemTopInsert = 4 * dpi
            }

            DslDemoItem()() {
                itemText = "群组(网格布局)功能示例"
                itemClick = {
                    start(GroupGridDemoActivity::class.java)
                }
                itemTopInsert = 4 * dpi
            }

            DslDemoItem()() {
                itemText = "网格,群组,填充功能示例"
                itemClick = {
                    start(GridFillDemoActivity::class.java)
                }
                itemTopInsert = 4 * dpi
            }

            DslDemoItem()() {
                itemText = "单选/多选示例"
                itemClick = {
                    start(SelectorDemoActivity::class.java)
                }
                itemTopInsert = 4 * dpi
            }

            DslDemoItem()() {
                itemText = "StaggeredGridLayout(拖拽/滑动删除示例)"
                itemClick = {
                    start(StaggeredGridLayoutActivity::class.java)
                }
                itemTopInsert = 4 * dpi
            }

            DslDemoItem()() {
                itemText = "树结构使用示例"
                itemClick = {
                    start(TreeDemoActivity::class.java)
                }
                itemTopInsert = 4 * dpi
            }

            DslDemoItem()() {
                itemText = "APP数据加载示例"
                itemClick = {
                    start(LoadDataActivity::class.java)
                }
                itemTopInsert = 4 * dpi
                itemLeftOffset = 200 * dpi
                onlyDrawOffsetArea = true
                itemDecorationColor = Color.MAGENTA
            }

            DslDemoItem()() {
                itemText = "侧滑菜单使用示例"
                itemClick = {
                    start(SwipeMenuActivity::class.java)
                }
                itemTopInsert = 4 * dpi
                itemRightOffset = 200 * dpi
                onlyDrawOffsetArea = true
                itemDecorationColor = Color.GREEN
            }

            DslDemoItem()() {
                itemText = "侧滑菜单+拖拽使用示例"
                itemClick = {
                    start(SwipeDragActivity::class.java)
                }
                itemTopInsert = 4 * dpi
            }

            DslDemoItem()() {
                itemText = "测试数据动态更新"
                itemTag = TAG_UPDATE_DATA //标识item, 方便后续find
                itemBackgroundDrawable = null
                itemBindOverride = { itemHolder, _, _, _ ->
                    itemHolder.tv(R.id.text_view)?.gravity = Gravity.CENTER
                }
                itemClick = {
                    updateData()
                }
            }

            renderEmptyItem()

            dslItem(R.layout.item_demo_list) {
                itemBindOverride = { itemHolder, _, _, _ ->
                    itemHolder.itemView.setBackgroundColor(Color.TRANSPARENT)
                    itemHolder.tv(R.id.text_view)?.apply {
                        gravity = Gravity.CENTER
                        text = "以下是分割线展示"
                    }
                }
            }

            /**
             * [plus]运算符重载方式 追加[DslAdapterItem]
             * */
            this + DslDemoItem().apply {
                itemText = "顶部的分割线是红色"
                itemTopInsert = 8 * dpi
                itemDecorationColor = Color.RED //控制分割线的颜色
            } + DslDemoItem().apply {
                itemText = "只绘制偏移量的分割线"
                itemTopInsert = 8 * dpi
                itemLeftOffset = 60 * dpi
                itemDecorationColor = Color.BLUE
                onlyDrawOffsetArea = true
            } + DslDemoItem().apply {
                itemText = "自定义Drawable的分割线"
                itemBottomInsert = 20 * dpi
                itemDecorationDrawable = resources.getDrawable(R.drawable.shape_decoration)
            } + DslDemoItem().apply {
                itemText = "上下都有的分割线"
                itemTopInsert = 8 * dpi
                itemBottomInsert = 8 * dpi
                itemDecorationColor = Color.GREEN
            }

            /**
             * [minus]运算符重载方式, 移除[DslAdapterItem]
             * */
            this - DslAdapterItem() - DslAdapterItem() - DslAdapterItem() - DslAdapterItem()

            DslWrapItem()() {
                itemContentLayoutId = R.layout.item_text_layout
                itemText = "点击演示动态 add/remove view"
                itemClick = {
                    itemContentLayoutId = if (itemContentLayoutId == R.layout.item_text_layout) {
                        R.layout.item_group_head
                    } else {
                        R.layout.item_text_layout
                    }
                }
            }

            this + DslViewBindingItem()

            this + DslUpdateParentItem() + DslUpdateChildItem() + DslUpdateChildItem()

            renderEmptyItem()

            batchLoad()

            //模拟网络延迟
            dslViewHolder.postDelay(1000) {
                //设置情感图状态, 正常
                render {
                    setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_NONE)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateData()
    }

    fun updateData() {
        dslAdapter.findItemByTag(TAG_UPDATE_DATA)?.apply {
            if (this is DslDemoItem) {
                itemText = "数据刷新:${System.currentTimeMillis()}"
                updateAdapterItem(TAG_UPDATE_DATA) /*使用[payload]标识此次刷新动作*/
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
            itemBindOverride = { itemHolder, itemPosition, adapterItem, _ ->
                itemHolder.tv(R.id.fold_button)?.text =
                    if (itemGroupExtend) "折叠 $itemPosition" else "展开 $itemPosition"

                itemHolder.click(R.id.fold_button) {
                    itemGroupExtend = !itemGroupExtend
                }
            }
        }

        //2种使用item的方式, 喜欢哪种方式, 就用哪一种
        dslAdapter.dslTextItem()
        dslAdapter.dslItem(R.layout.item_text_layout) {
            itemBindOverride = { itemHolder, itemPosition, _, _ ->
                itemHolder.v<TextView>(R.id.text_view)?.text = "文本位置:$itemPosition"
            }
        }

        for (j in 0..0) {
            //2种使用item的方式, 喜欢哪种方式, 就用哪一种
            dslAdapter.dslImageItem()
            dslAdapter.dslItem(R.layout.item_image_layout) {
                itemBindOverride = { itemHolder, itemPosition, _, _ ->
                    itemHolder.v<TextView>(R.id.text_view)?.text = "文本位置:$itemPosition"
                }
            }
        }
    }
}