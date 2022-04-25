package com.angcyo.dsladapter.demo

import android.graphics.Color
import android.graphics.Paint
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.widget.FrameLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.angcyo.dsladapter.*
import com.angcyo.dsladapter.dsl.DslTextItem
import com.angcyo.item.DslGridItem
import com.angcyo.widget.span.span
import kotlin.random.Random.Default.nextInt

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/12/23
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class GridFillDemoActivity : BaseRecyclerActivity() {

    var orientation = RecyclerView.VERTICAL

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_orientation, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_horizontal -> {
                if (orientation != RecyclerView.HORIZONTAL) {
                    orientation = RecyclerView.HORIZONTAL
                    dslAdapter.clearAllItems()
                    onInitBaseLayoutAfter()
                    true
                } else {
                    false
                }
            }
            R.id.action_vertical -> {
                if (orientation != RecyclerView.VERTICAL) {
                    orientation = RecyclerView.VERTICAL
                    dslAdapter.clearAllItems()
                    onInitBaseLayoutAfter()
                    true
                } else {
                    false
                }
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onInitBaseLayoutAfter() {
        val spanCount = 4
        val spanSizeLookup: GridLayoutManager.SpanSizeLookup

        recyclerView.layoutManager = GridLayoutManager(this, spanCount, orientation, false).apply {
            spanSizeLookup = dslSpanSizeLookup(dslAdapter)
        }

        dslAdapter.render {
            setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_LOADING)
        }
        dslViewHolder.postDelay(1000) {

            renderAdapter {
                setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_NONE)
                val groupSize = nextInt(1, 10)
                for (groupIndex in 0 until groupSize) {
                    val group = listOf("group$groupIndex")
                    DslTextItem()() {
                        itemIsGroupHead = true //启动分组折叠
                        itemLayoutId =
                            if (orientation.isHorizontal()) R.layout.item_group_head_horizontal else R.layout.item_group_head
                        itemSpanCount = -1
                        itemText = "最近使用"
                        itemGroups = group
                        itemTag = "group$groupIndex"
                        onSetItemOffset = {
                            itemGroupParams.apply {
                                if (isEdgeGroupTop()) {
                                    if (orientation.isHorizontal()) {
                                        it.left = 10 * dpi
                                    } else {
                                        it.top = 10 * dpi
                                    }
                                }
                            }
                        }
                        itemBindOverride = { itemHolder, itemPosition, adapterItem, payloads ->
                            itemGroupParams.apply {
                                itemHolder.tv(R.id.text_view)?.apply {
                                    paintFlags = Paint.FAKE_BOLD_TEXT_FLAG
                                    setPadding(15 * dpi, 0, 0, 0)
                                    val lm = layoutParams
                                    if (lm is FrameLayout.LayoutParams) {
                                        lm.gravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
                                        layoutParams = lm
                                    }
                                    text =
                                        "$itemText (第${edgeGridParams?.currentSpanParams?.spanGroupIndex}行)"
                                }

                                if (orientation.isHorizontal()) {
                                    when {
                                        isOnlyOne() -> {
                                            itemHolder.itemView.setBackgroundResource(R.drawable.shape_group_all)
                                        }
                                        isEdgeGroupTop() -> {
                                            itemHolder.itemView.setBackgroundResource(R.drawable.shape_group_header_horizontal)
                                        }
                                        else -> {
                                            itemHolder.itemView.setBackgroundColor(
                                                resources.getColor(
                                                    R.color.lib_white
                                                )
                                            )
                                        }
                                    }
                                } else {
                                    when {
                                        isOnlyOne() -> {
                                            itemHolder.itemView.setBackgroundResource(R.drawable.shape_group_all)
                                        }
                                        isEdgeGroupTop() -> {
                                            itemHolder.itemView.setBackgroundResource(R.drawable.shape_group_header)
                                        }
                                        else -> {
                                            itemHolder.itemView.setBackgroundColor(
                                                resources.getColor(
                                                    R.color.lib_white
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            itemHolder.tv(R.id.fold_button)?.text =
                                if (itemGroupExtend) "折叠 $itemPosition" else "展开 $itemPosition"

                            itemHolder.click(R.id.fold_button) {
                                itemGroupExtend = !itemGroupExtend
                            }
                        }
                    }

                    val itemSize = nextInt(1, 10)
                    for (itemIndex in 0 until itemSize) {
                        DslGridItem()() {
                            itemIcon = R.drawable.ic_launcher_round
                            if (orientation.isHorizontal()) {
                                itemLayoutId = R.layout.dsl_grid_item_horizontal
                            }
                            itemText = "文本"
                            itemGroups = group
                            itemTag = "grid$itemIndex"
                            itemBindOverride = { itemHolder, itemPosition, adapterItem, payloads ->
                                resetGridItemBackground(itemHolder, orientation)
                                itemGroupParams.apply {
                                    itemHolder.tv(R.id.lib_text_view)?.textSize = 8f
                                    itemHolder.tv(R.id.lib_text_view)?.text = span {
                                        append(itemTag)
                                        appendln()
                                        append("C:")
                                        append(" 行:${edgeGridParams?.currentSpanParams?.spanGroupIndex}")
                                        append(" 列:${edgeGridParams?.currentSpanParams?.spanIndex}")
                                        append(" 占:${edgeGridParams?.currentSpanParams?.spanSize}")
                                        appendln()
                                        append("F:")
                                        append(" 行:${edgeGridParams?.firstSpanParams?.spanGroupIndex}")
                                        append(" 列:${edgeGridParams?.firstSpanParams?.spanIndex}")
                                        append(" 占:${edgeGridParams?.firstSpanParams?.spanSize}")
                                        appendln()
                                        append("L:")
                                        append(" 行:${edgeGridParams?.lastSpanParams?.spanGroupIndex}")
                                        append(" 列:${edgeGridParams?.lastSpanParams?.spanIndex}")
                                        append(" 占:${edgeGridParams?.lastSpanParams?.spanSize}")
                                    }
                                }
                            }
                        }
                    }
                    fillEmptyItem(itemSize, spanCount) {
                        itemGroups = group
                        itemBindOverride = { itemHolder, itemPosition, adapterItem, payloads ->
                            resetGridItemBackground(itemHolder, orientation)
                            itemHolder.itemView.setWidthHeight(-2, -2)
                        }
                    }
                }
            }
        }
    }

    fun DslAdapter.fillEmptyItem(
        dataListSize: Int,
        spanCount: Int,
        action: DslAdapterItem.() -> Unit = {}
    ) {
        val fillCount = spanCount - dataListSize % spanCount
        if (fillCount != spanCount) {
            for (i in 0 until (spanCount - dataListSize % spanCount)) {
                renderEmptyItem(-2, Color.WHITE) {
                    action()
                }
            }
        }
    }

    fun DslAdapterItem.resetGridItemBackground(itemHolder: DslViewHolder, orientation: Int) {
        itemGroupParams.apply {
            when {
                isOnlyOne() -> {
                    itemHolder.itemView.setBackgroundResource(R.drawable.shape_group_all)
                }
                isGroupFirstColumn() && isGroupLastRow() -> {
                    if (orientation.isHorizontal()) {
                        itemHolder.itemView.setBackgroundResource(R.drawable.shape_right_top)
                    } else {
                        itemHolder.itemView.setBackgroundResource(R.drawable.shape_left_bottom)
                    }
                }
                isGroupLastColumn() && isGroupLastRow() -> {
                    itemHolder.itemView.setBackgroundResource(R.drawable.shape_right_bottom)
                }
                else -> {
                    itemHolder.itemView.setBackgroundColor(resources.getColor(R.color.lib_white))
                }
            }
        }
    }

    fun Int.isHorizontal() = this == RecyclerView.HORIZONTAL
}