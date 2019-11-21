package com.angcyo.dsladapter

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.support.annotation.LayoutRes
import android.support.v7.widget.RecyclerView
import android.view.View
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/05/07
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
open class DslAdapterItem {

    /**适配器*/
    var itemDslAdapter: DslAdapter? = null

    /**[notifyItemChanged]*/
    open fun updateAdapterItem(useFilterList: Boolean = true) {
        if (itemDslAdapter == null) {
            L.e("updateAdapterItem需要[itemDslAdapter], 请赋值.")
        }
        itemDslAdapter?.notifyItemChanged(this, useFilterList)
    }

    //<editor-fold desc="Grid相关属性">

    /**
     * 在 GridLayoutManager 中, 需要占多少个 span. -1表示满屏
     * [itemIsGroupHead]
     * */
    var itemSpanCount = 1

    //</editor-fold>

    //<editor-fold desc="标准属性">

    /**布局的xml id, 必须设置.*/
    open var itemLayoutId: Int = -1

    /**附加的数据*/
    var itemData: Any? = null

    /**唯一标识此item的值*/
    var itemTag: String? = null

    /**
     * 界面绑定入口
     * [DslAdapter.onBindView]
     * */
    open var itemBind: (itemHolder: DslViewHolder, itemPosition: Int, adapterItem: DslAdapterItem) -> Unit =
        { itemHolder, itemPosition, adapterItem ->
            onItemBind(itemHolder, itemPosition, adapterItem)
            onItemBindOverride(itemHolder, itemPosition, adapterItem)
        }

    /**
     * 点击事件和长按事件封装
     * */
    var onItemClick: ((View) -> Unit)? = null
    var onItemLongClick: ((View) -> Boolean)? = null

    var _clickListener: View.OnClickListener? = object : View.OnClickListener {
        override fun onClick(view: View?) {
            notNull(onItemClick, view) {
                onItemClick?.invoke(view!!)
            }
        }
    }

    var _longClickListener: View.OnLongClickListener? =
        View.OnLongClickListener { view -> onItemLongClick?.invoke(view) ?: false }

    open fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem
    ) {
        if (onItemClick == null || _clickListener == null) {
            itemHolder.itemView.isClickable = false
        } else {
            itemHolder.clickItem(_clickListener)
        }

        if (onItemLongClick == null || _longClickListener == null) {
            itemHolder.itemView.isLongClickable = false
        } else {
            itemHolder.itemView.setOnLongClickListener(_longClickListener)
        }
    }

    /**用于覆盖默认操作*/
    open var onItemBindOverride: (itemHolder: DslViewHolder, itemPosition: Int, adapterItem: DslAdapterItem) -> Unit =
        { _, _, _ ->

        }

    /**
     * [DslAdapter.onViewAttachedToWindow]
     * */
    open var onItemViewAttachedToWindow: (itemHolder: DslViewHolder) -> Unit = {

    }

    /**
     * [DslAdapter.onViewDetachedFromWindow]
     * */
    open var onItemViewDetachedToWindow: (itemHolder: DslViewHolder) -> Unit = {

    }

    //</editor-fold>


    //<editor-fold desc="分组相关属性">

    /**
     * 当前item, 是否是分组的头, 设置了分组, 默认会开启悬停
     *
     * 如果为true, 哪里折叠此分组是, 会 伪删除 这个分组头, 到下一个分组头 中间的 data
     * */
    var itemIsGroupHead = false
        set(value) {
            field = value
            if (value) {
                itemIsHover = true
                itemDragEnable = false
                itemSpanCount = -1
            }
        }

    /**
     * 当前分组是否[展开]
     * */
    var itemGroupExtend: Boolean by UpdateDependProperty(true)

    /**是否需要隐藏item*/
    var itemHidden: Boolean by UpdateDependProperty(false)

    //</editor-fold>

    //<editor-fold desc="悬停相关属性">

    /**
     * 是否需要悬停, 在使用了 [HoverItemDecoration] 时, 有效.
     * [itemIsGroupHead]
     * */
    var itemIsHover = itemIsGroupHead

    //</editor-fold>

    //<editor-fold desc="表单 分割线配置">

    /**
     * 需要插入分割线的大小
     * */
    var itemTopInsert = 0
    var itemLeftInsert = 0
    var itemRightInsert = 0
    var itemBottomInsert = 0

    var itemDecorationColor = Color.TRANSPARENT

    /**更强大的分割线自定义, 在color绘制后绘制*/
    var itemDecorationDrawable: Drawable? = null

    /**
     * 仅绘制offset的区域
     * */
    var onlyDrawOffsetArea = false

    /**
     * 分割线绘制时的偏移
     * */
    var itemTopOffset = 0
    var itemLeftOffset = 0
    var itemRightOffset = 0
    var itemBottomOffset = 0

    /**可以覆盖设置分割线的边距*/
    var onSetItemOffset: (rect: Rect) -> Unit = {}

    fun setItemOffsets(rect: Rect) {
        rect.set(itemLeftInsert, itemTopInsert, itemRightInsert, itemBottomInsert)
        onSetItemOffset(rect)
    }

    fun setTopInsert(insert: Int, leftOffset: Int = 0, rightOffset: Int = 0) {
        itemTopInsert = insert
        itemRightOffset = rightOffset
        itemLeftOffset = leftOffset
    }

    fun setBottomInsert(insert: Int, leftOffset: Int = 0, rightOffset: Int = 0) {
        itemBottomInsert = insert
        itemRightOffset = rightOffset
        itemLeftOffset = leftOffset
    }

    fun setLeftInsert(insert: Int, topOffset: Int = 0, bottomOffset: Int = 0) {
        itemLeftInsert = insert
        itemBottomOffset = bottomOffset
        itemTopOffset = topOffset
    }

    fun setRightInsert(insert: Int, topOffset: Int = 0, bottomOffset: Int = 0) {
        itemRightInsert = insert
        itemBottomOffset = bottomOffset
        itemTopOffset = topOffset
    }

    fun marginVertical(top: Int, bottom: Int = 0, color: Int = Color.TRANSPARENT) {
        itemLeftOffset = 0
        itemRightOffset = 0
        itemTopInsert = top
        itemBottomInsert = bottom
        onlyDrawOffsetArea = false
        itemDecorationColor = color
    }

    fun marginHorizontal(left: Int, right: Int = 0, color: Int = Color.TRANSPARENT) {
        itemTopOffset = 0
        itemBottomOffset = 0

        itemLeftInsert = left
        itemRightInsert = right
        onlyDrawOffsetArea = false
        itemDecorationColor = color
    }

    /**
     * 绘制不同方向的分割线时, 触发的回调, 可以用来设置不同方向分割线的颜色
     * */
    open var eachDrawItemDecoration: (left: Int, top: Int, right: Int, bottom: Int) -> Unit =
        { _, _, _, _ ->

        }

    /**自定义绘制*/
    var onDraw: ((
        canvas: Canvas,
        paint: Paint,
        itemView: View,
        offsetRect: Rect,
        itemCount: Int,
        position: Int,
        drawRect: Rect
    ) -> Unit)? = null

    /**
     * 分割线支持需要[DslItemDecoration]
     * */
    open fun draw(
        canvas: Canvas,
        paint: Paint,
        itemView: View,
        offsetRect: Rect,
        itemCount: Int,
        position: Int,
        drawRect: Rect
    ) {
        //super.draw(canvas, paint, itemView, offsetRect, itemCount, position)

        onDraw?.let {
            it(canvas, paint, itemView, offsetRect, itemCount, position, drawRect)
            return
        }

        eachDrawItemDecoration(0, itemTopInsert, 0, 0)
        paint.color = itemDecorationColor
        val drawOffsetArea = onlyDrawOffsetArea
        if (itemTopInsert > 0) {
            if (onlyDrawOffsetArea) {
                //绘制左右区域
                if (itemLeftOffset > 0) {
                    drawRect.set(
                        itemView.left,
                        itemView.top - offsetRect.top,
                        itemLeftOffset,
                        itemView.top
                    )
                    canvas.drawRect(drawRect, paint)
                    onDrawItemDecorationDrawable(canvas, drawRect)
                }
                if (itemRightOffset > 0) {
                    drawRect.set(
                        itemView.right - itemRightOffset,
                        itemView.top - offsetRect.top,
                        itemView.right,
                        itemView.top
                    )
                    canvas.drawRect(drawRect, paint)
                    onDrawItemDecorationDrawable(canvas, drawRect)
                }
            } else {
                drawRect.set(
                    itemView.left,
                    itemView.top - offsetRect.top,
                    itemView.right,
                    itemView.top
                )
                canvas.drawRect(drawRect, paint)
                onDrawItemDecorationDrawable(canvas, drawRect)
            }
        }

        onlyDrawOffsetArea = drawOffsetArea
        eachDrawItemDecoration(0, 0, 0, itemBottomInsert)
        paint.color = itemDecorationColor
        if (itemBottomInsert > 0) {
            if (onlyDrawOffsetArea) {
                //绘制左右区域
                if (itemLeftOffset > 0) {
                    drawRect.set(
                        itemView.left,
                        itemView.bottom,
                        itemLeftOffset,
                        itemView.bottom + offsetRect.bottom
                    )
                    canvas.drawRect(drawRect, paint)
                    onDrawItemDecorationDrawable(canvas, drawRect)
                }
                if (itemRightOffset > 0) {
                    drawRect.set(
                        itemView.right - itemRightOffset,
                        itemView.bottom,
                        itemView.right,
                        itemView.bottom + offsetRect.bottom
                    )
                    canvas.drawRect(drawRect, paint)
                    onDrawItemDecorationDrawable(canvas, drawRect)
                }
            } else {
                drawRect.set(
                    itemView.left,
                    itemView.bottom,
                    itemView.right,
                    itemView.bottom + offsetRect.bottom
                )
                canvas.drawRect(drawRect, paint)
                onDrawItemDecorationDrawable(canvas, drawRect)
            }
        }

        onlyDrawOffsetArea = drawOffsetArea
        eachDrawItemDecoration(itemLeftInsert, 0, 0, 0)
        paint.color = itemDecorationColor
        if (itemLeftInsert > 0) {
            if (onlyDrawOffsetArea) {
                //绘制上下区域
                if (itemTopOffset > 0) {
                    drawRect.set(
                        itemView.left - offsetRect.left,
                        itemView.top,
                        itemView.left,
                        itemTopOffset
                    )
                    canvas.drawRect(drawRect, paint)
                    onDrawItemDecorationDrawable(canvas, drawRect)
                }
                if (itemBottomOffset < 0) {
                    drawRect.set(
                        itemView.left - offsetRect.left,
                        itemView.bottom - itemBottomOffset,
                        itemView.left,
                        itemView.bottom
                    )
                    canvas.drawRect(drawRect, paint)
                    onDrawItemDecorationDrawable(canvas, drawRect)
                }
            } else {
                drawRect.set(
                    itemView.left - offsetRect.left,
                    itemView.top,
                    itemView.left,
                    itemView.bottom
                )
                canvas.drawRect(drawRect, paint)
                onDrawItemDecorationDrawable(canvas, drawRect)
            }
        }

        onlyDrawOffsetArea = drawOffsetArea
        eachDrawItemDecoration(0, 0, itemRightInsert, 0)
        paint.color = itemDecorationColor
        if (itemRightInsert > 0) {
            if (onlyDrawOffsetArea) {
                //绘制上下区域
                if (itemTopOffset > 0) {
                    drawRect.set(
                        itemView.right,
                        itemView.top,
                        itemView.right + offsetRect.right,
                        itemTopOffset
                    )
                    canvas.drawRect(drawRect, paint)
                    onDrawItemDecorationDrawable(canvas, drawRect)
                }
                if (itemBottomOffset < 0) {
                    drawRect.set(
                        itemView.right,
                        itemView.bottom - itemBottomOffset,
                        itemView.right + offsetRect.right,
                        itemView.bottom
                    )
                    canvas.drawRect(drawRect, paint)
                    onDrawItemDecorationDrawable(canvas, drawRect)
                }
            } else {
                drawRect.set(
                    itemView.right,
                    itemView.top,
                    itemView.right + offsetRect.right,
                    itemView.bottom
                )
                canvas.drawRect(drawRect, paint)
                onDrawItemDecorationDrawable(canvas, drawRect)
            }
        }
        onlyDrawOffsetArea = drawOffsetArea
    }

    var onDrawItemDecorationDrawable: (canvas: Canvas, rect: Rect) -> Unit = { canvas, rect ->
        itemDecorationDrawable?.let {
            it.setBounds(rect.left, rect.top, rect.right, rect.bottom)
            it.draw(canvas)
        }
    }

    //</editor-fold desc="表单 分割线配置">

    //<editor-fold desc="Diff 相关">

    /**[Item]是否发生过改变*/
    var itemChanged = false

    /**[Item]是否正在改变, 会影响[thisAreContentsTheSame]的判断, 并且会在[Diff]计算完之后, 设置为`false`*/
    var itemChanging = false
        set(value) {
            field = value
            if (value) {
                itemChanged = true
            }
        }

    /**
     * 决定
     * [RecyclerView.Adapter.notifyItemInserted]
     * [RecyclerView.Adapter.notifyItemRemoved]
     * 的执行
     * */
    open var thisAreItemsTheSame: (fromItem: DslAdapterItem?, newItem: DslAdapterItem) -> Boolean =
        { _, newItem -> this == newItem }

    /**
     * [RecyclerView.Adapter.notifyItemChanged]
     * */
    open var thisAreContentsTheSame: (fromItem: DslAdapterItem?, newItem: DslAdapterItem) -> Boolean =
        { fromItem, newItem ->
            when {
                itemChanging -> false
                fromItem == null -> this == newItem
                else -> this != fromItem && this == newItem
            }
        }

    /**
     * [checkItem] 是否需要关联到处理列表
     * [itemIndex] 分组折叠之后数据列表中的index
     *
     * 返回 true 时, [checkItem]  进行 [hide] 操作
     * */
    open var isItemInHiddenList: (checkItem: DslAdapterItem, itemIndex: Int) -> Boolean =
        { _, _ -> false }

    /**
     * [itemIndex] 最终过滤之后数据列表中的index
     * 返回 true 时, [checkItem] 会收到 来自 [this] 的 [onItemUpdateFromInner] 触发的回调
     * */
    open var isItemInUpdateList: (checkItem: DslAdapterItem, itemIndex: Int) -> Boolean =
        { _, _ -> false }

    /**
     * 通过diff更新
     * @param notifyUpdate 是否需要触发 [Depend] 关系链.
     * */
    open fun updateItemDepend(
        filterParams: FilterParams = FilterParams(
            this,
            updateDependItemWithEmpty = false
        )
    ) {
        if (itemDslAdapter == null) {
            L.e("updateItemDepend需要[itemDslAdapter], 请赋值.")
        }
        itemDslAdapter?.updateItemDepend(filterParams)
    }

    /**更新选项*/
    open fun updateItemSelector(select: Boolean, notifyUpdate: Boolean = false) {
        if (itemDslAdapter == null) {
            L.e("updateItemSelector需要[itemDslAdapter], 请赋值.")
        }
        itemDslAdapter?.itemSelectorHelper?.selector(
            SelectorParams(
                this,
                select.toSelectOption(),
                notify = true,
                notifyItemChange = true,
                updateItemDepend = notifyUpdate
            )
        )
    }

    var onItemUpdateFrom: (fromItem: DslAdapterItem) -> Unit = {}

    open fun onItemUpdateFromInner(fromItem: DslAdapterItem) {
        onItemUpdateFrom(fromItem)
    }

    //</editor-fold desc="Diff 相关">

    //<editor-fold desc="单选, 多选相关">

    /**是否选中, 需要 [ItemSelectorHelper.selectorModel] 的支持. */
    var itemIsSelected = false

    /**是否 允许被选中*/
    var isItemCanSelected: (fromSelector: Boolean, toSelector: Boolean) -> Boolean =
        { from, to -> from != to }

    var onItemSelectorChange: (selectorParams: SelectorParams) -> Unit = {
        if (it.updateItemDepend) {
            updateItemDepend()
        }
    }

    /**选中变化后触发*/
    open fun _itemSelectorChange(selectorParams: SelectorParams) {
        onItemSelectorChange(selectorParams)
    }

    val itemIndexPosition
        get() = itemDslAdapter?.getValidFilterDataList()?.indexOf(this) ?: RecyclerView.NO_POSITION

    //</editor-fold desc="单选, 多选相关">

    //<editor-fold desc="群组相关">

    /**动态计算的属性*/
    val itemGroupParams: ItemGroupParams
        get() =
            itemDslAdapter?.findItemGroupParams(this) ?: ItemGroupParams(
                0,
                this,
                mutableListOf(this)
            )

    /**所在的分组名, 只用来做快捷变量存储*/
    var itemGroups = mutableListOf<String>()

    /**核心群组判断的方法*/
    var isItemInGroups: (newItem: DslAdapterItem) -> Boolean = {
        var result = false
        for (group in it.itemGroups) {
            result = result || itemGroups.contains(group)

            if (result) {
                break
            }
        }
        result
    }

    //</editor-fold>

    //<editor-fold desc="拖拽相关">

    /**
     * 当前[DslAdapterItem]是否可以被拖拽
     * [itemIsGroupHead]
     * [DragCallbackHelper.getMovementFlags]
     * */
    var itemDragEnable = true

    /**
     * 当前[DslAdapterItem]是否可以被侧滑删除
     * */
    var itemSwipeEnable = true

    /**[dragItem]是否可以在此位置[this]放下*/
    var isItemCanDropOver: (dragItem: DslAdapterItem) -> Boolean = {
        itemDragEnable
    }

    //</editor-fold>

    //<editor-fold desc="Tree 树结构相关">

    /**
     * 折叠/展开 依旧使用[itemGroupExtend]控制
     *
     * 子项列表
     * */
    var itemSubList = mutableListOf<DslAdapterItem>()

    /**
     * 在控制[itemSubList]之前, 都会回调此方法.
     * 相当于hook了[itemSubList], 可以在[itemSubList]为空时, 展示[加载中Item]等
     * */
    var onItemLoadSubList: () -> Unit = {}

    /**父级列表, 会自动赋值*/
    var itemParentList = mutableListOf<DslAdapterItem>()

    //</editor-fold>
}

class UpdateDependProperty<T>(var value: T) : ReadWriteProperty<DslAdapterItem, T> {
    override fun getValue(thisRef: DslAdapterItem, property: KProperty<*>): T = value

    override fun setValue(thisRef: DslAdapterItem, property: KProperty<*>, value: T) {
        val old = this.value
        this.value = value
        if (old != value) {
            thisRef.updateItemDepend()
        }
    }
}

/**
 * 将list结构体, 打包成dslItem
 * */
public fun List<Any>.toDslItemList(
    @LayoutRes layoutId: Int = -1,
    config: DslAdapterItem.() -> Unit = {}
): MutableList<DslAdapterItem> {
    return toDslItemList(DslAdapterItem::class.java, layoutId, config)
}

public fun List<Any>.toDslItemList(
    dslItem: Class<out DslAdapterItem>,
    @LayoutRes layoutId: Int = -1,
    config: DslAdapterItem.() -> Unit = {}
): MutableList<DslAdapterItem> {
    return toDslItemList(itemFactory = { _, item ->
        dslItem.newInstance().apply {
            if (layoutId != -1) {
                itemLayoutId = layoutId
            }
            config()
        }
    })
}

public fun List<Any>.toDslItemList(
    itemBefore: (itemList: MutableList<DslAdapterItem>, index: Int, item: Any) -> Unit = { _, _, _ -> },
    itemFactory: (index: Int, item: Any) -> DslAdapterItem,
    itemAfter: (itemList: MutableList<DslAdapterItem>, index: Int, item: Any) -> Unit = { _, _, _ -> }
): MutableList<DslAdapterItem> {
    return toAnyList(itemBefore, { index, any ->
        val item = itemFactory(index, any)
        item.itemData = any
        item
    }, itemAfter)
}

public fun <T> List<Any>.toAnyList(
    itemBefore: (itemList: MutableList<T>, index: Int, item: Any) -> Unit = { _, _, _ -> },
    itemFactory: (index: Int, item: Any) -> T,
    itemAfter: (itemList: MutableList<T>, index: Int, item: Any) -> Unit = { _, _, _ -> }
): MutableList<T> {
    val result = mutableListOf<T>()

    forEachIndexed { index, any ->
        itemBefore(result, index, any)
        val item = itemFactory(index, any)
        result.add(item)
        itemAfter(result, index, any)
    }
    return result
}