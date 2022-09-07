package com.angcyo.dsladapter

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AnimRes
import androidx.annotation.AnimatorRes
import androidx.annotation.AnyThread
import androidx.annotation.CallSuper
import androidx.core.math.MathUtils.clamp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.recyclerview.widget.*
import com.angcyo.dsladapter.SwipeMenuHelper.Companion.SWIPE_MENU_TYPE_DEFAULT
import com.angcyo.dsladapter.SwipeMenuHelper.Companion.SWIPE_MENU_TYPE_FLOWING
import com.angcyo.dsladapter.annotation.*
import com.angcyo.dsladapter.internal.ThrottleClickListener
import java.lang.ref.WeakReference
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/05/07
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */

typealias ItemAction = (DslAdapterItem) -> Unit

typealias ItemSelectAction = (selectorParams: SelectorParams) -> Unit

typealias ItemUpdateDependAction = (FilterParams) -> Unit

typealias ItemBindAction = (
    itemHolder: DslViewHolder,
    itemPosition: Int,
    adapterItem: DslAdapterItem,
    payloads: List<Any>
) -> Unit

typealias HolderBindAction = (
    itemHolder: DslViewHolder,
    itemPosition: Int
) -> Unit

open class DslAdapterItem : LifecycleOwner {

    companion object {
        /**负载,请求刷新部分界面*/
        const val PAYLOAD_UPDATE_PART = 0x0001

        /**负载,强制更新媒体, 比如图片*/
        const val PAYLOAD_UPDATE_MEDIA = 0x002

        /**负载,请求更新[itemGroupExtend]*/
        const val PAYLOAD_UPDATE_EXTEND = 0x004

        /**负载,请求更新[itemHidden]*/
        const val PAYLOAD_UPDATE_HIDDEN = 0x008

        /**负载,请求更新[itemIsSelected]*/
        const val PAYLOAD_UPDATE_SELECT = 0x010

        /**占满宽度的item*/
        const val FULL_ITEM = -1

        /**节流间隔时长*/
        var DEFAULT_THROTTLE_INTERVAL = ThrottleClickListener.DEFAULT_THROTTLE_INTERVAL
    }

    /**适配器, 自动赋值
     * [com.angcyo.dsladapter.DslAdapter._updateAdapterItems]
     * [com.angcyo.dsladapter.DslAdapter.onBindViewHolder]
     * */
    var itemDslAdapter: DslAdapter? = null

    //<editor-fold desc="update操作">

    /**[com.angcyo.dsladapter.DslAdapter.notifyItemChanged]*/
    @AnyThread
    @UpdateByNotify
    open fun updateAdapterItem(payload: Any? = PAYLOAD_UPDATE_PART, useFilterList: Boolean = true) {
        if (itemDslAdapter?._recyclerView?.isComputingLayout == true || !isMain()) {
            //L.w("跳过操作! [RecyclerView]正在计算布局, 请不要在RecyclerView正在布局时, 更新Item. ")
            itemDslAdapter?._recyclerView?.post {
                updateAdapterItem(payload, useFilterList)
            }
            return
        }
        itemDslAdapter?.notifyItemChanged(this, payload, useFilterList).elseNull {
            L.w("跳过操作! updateAdapterItem需要[itemDslAdapter],请赋值.")
        }
    }

    /**移除[item]*/
    @UpdateFlag
    open fun removeAdapterItem() {
        itemDslAdapter?.apply {
            removeItemFromAll(this@DslAdapterItem)
        }.elseNull {
            L.w("跳过操作! updateAdapterItem需要[itemDslAdapter],请赋值.")
        }
    }

    /**负载更新, 通常用于更新媒体item*/
    @UpdateByDiff
    open fun updateItemDependPayload(payload: Any? = mediaPayload()) {
        updateItemDepend(
            FilterParams(
                fromDslAdapterItem = this,
                updateDependItemWithEmpty = false,
                payload = payload
            )
        )
    }

    /**
     * 通过diff更新
     * @param notifyUpdate 是否需要触发 [Depend] 关系链.
     *
     * [isItemInUpdateList]
     * [itemUpdateFrom]
     * */
    @UpdateByDiff
    open fun updateItemDepend(
        filterParams: FilterParams = FilterParams(
            fromDslAdapterItem = this,
            updateDependItemWithEmpty = false,
            payload = PAYLOAD_UPDATE_PART
        )
    ) {
        itemDslAdapter?.updateItemDepend(filterParams).elseNull {
            L.w("跳过操作! updateItemDepend需要[itemDslAdapter],请赋值.")
        }
    }

    /**更新选项*/
    open fun updateItemSelector(select: Boolean, notifyUpdate: Boolean = false) {
        itemDslAdapter?.itemSelectorHelper?.selector(
            SelectorParams(
                this,
                select.toSelectOption(),
                notifySelectListener = true,
                notifyItemSelectorChange = true,
                updateItemDepend = notifyUpdate
            )
        ).elseNull {
            L.w("跳过操作! updateItemSelector需要[itemDslAdapter],请赋值.")
        }
    }

    /**更新选项*/
    open fun updateItemSelect(
        select: Boolean,
        selectorParams: SelectorParams = SelectorParams(
            this,
            select.toSelectOption(),
            _useFilterList = false
        )
    ) {
        itemDslAdapter?.itemSelectorHelper?.selector(selectorParams).elseNull {
            L.w("跳过操作! updateItemSelector需要[itemDslAdapter],请赋值.")
        }
    }

    /**有依赖时, 才更新
     * [updateItemDepend]*/
    @UpdateByDiff
    @UpdateByNotify
    open fun updateItemOnHaveDepend(
        updateSelf: Boolean = true,
        filterParams: FilterParams = FilterParams(
            fromDslAdapterItem = this,
            updateDependItemWithEmpty = false,
            payload = PAYLOAD_UPDATE_PART
        )
    ) {
        val notifyChildFormItemList = mutableListOf<DslAdapterItem>()
        itemDslAdapter?.getUpdateDependItemListFrom(this)?.let {
            notifyChildFormItemList.addAll(it)
        }

        if (notifyChildFormItemList.isNotEmpty()) {
            updateItemDepend(filterParams)
        } else if (updateSelf) {
            //否则更新自己
            updateAdapterItem(filterParams.payload)
        }
    }

    //</editor-fold desc="update操作">

    //<editor-fold desc="Grid相关属性">

    /**
     * 在 GridLayoutManager 中, 需要占多少个 span. [FULL_ITEM]表示满屏
     * [itemIsGroupHead]
     * [com.angcyo.dsladapter.DslAdapter.onViewAttachedToWindow]
     * 需要[dslSpanSizeLookup]支持.
     *
     * 在[StaggeredGridLayoutManager]中, 会使用[layoutParams.isFullSpan]的方式满屏
     *
     * [com.angcyo.dsladapter.DslAdapterItem.fullWidthItem]
     * [com.angcyo.dsladapter.AdapterLibEx.fullSpan]
     *
     * */
    var itemSpanCount = 1

    //</editor-fold>

    //<editor-fold desc="标准属性">

    /**item的布局类型, 当[itemLayoutId]一样, 只是内部的内容不一样时, 阔以使用.
     * 比如im聊天界面*/
    open var itemViewType: Int? = null

    /**布局的xml id, 必须设置.*/
    open var itemLayoutId: Int = -1

    /**附加的数据*/
    var itemData: Any? = null
        set(value) {
            field = value
            onSetItemData(value)
        }

    /**[itemData]*/
    open fun onSetItemData(data: Any?) {

    }

    /**强制指定item的宽高*/
    var itemWidth: Int = undefined_size
    var itemMinWidth: Int = undefined_size

    var itemHeight: Int = undefined_size
    var itemMinHeight: Int = undefined_size

    /**padding值*/
    var itemPaddingLeft: Int = undefined_size
    var itemPaddingRight: Int = undefined_size
    var itemPaddingTop: Int = undefined_size
    var itemPaddingBottom: Int = undefined_size

    /**指定item的背景*/
    var itemBackgroundDrawable: Drawable? = UndefinedDrawable()

    /**是否激活item, 目前只能控制click, longClick事件不被回调*/
    var itemEnable: Boolean = true
        set(value) {
            field = value
            onSetItemEnable(value)
        }

    /**[itemEnable]*/
    @UpdateByLocal
    open fun onSetItemEnable(enable: Boolean) {
        itemViewHolder()?.let {
            it.enable(it.itemView, enable, true)
            _initItemListener(it)
        }
    }

    /**唯一标识此item的值*/
    var itemTag: String? = null

    /**异常标识, 自定义数据*/
    var itemThrowable: Throwable? = null

    /**item存储数据使用*/
    var itemTags: SparseArray<Any?>? = null

    /**
     * 界面绑定入口
     * [DslAdapter.onBindViewHolder(com.angcyo.widget.DslViewHolder, int, java.util.List<? extends java.lang.Object>)]
     * */
    @ItemInitEntryPoint
    var itemBind: ItemBindAction = { itemHolder, itemPosition, adapterItem, payloads ->
        onItemBind(itemHolder, itemPosition, adapterItem, payloads)
        itemBindOverride(itemHolder, itemPosition, adapterItem, payloads)
        onItemBindAfter(itemHolder, itemPosition, adapterItem, payloads)
    }

    /**
     * 点击事件和长按事件封装
     * */
    var itemClick: ((View) -> Unit)? = null

    var itemLongClick: ((View) -> Boolean)? = null

    /**点击节流间隔时长*/
    var itemClickThrottleInterval: Long = DEFAULT_THROTTLE_INTERVAL

    //使用节流方式处理点击事件
    var _clickListener: View.OnClickListener? = ThrottleClickListener(action = { view ->
        notNull(itemClick, view) {
            itemClick?.invoke(view)
        }
    })

    var _longClickListener: View.OnLongClickListener? =
        View.OnLongClickListener { view -> itemLongClick?.invoke(view) ?: false }

    open fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        _initItemEnable(itemHolder)
        _initItemBackground(itemHolder)
        _initItemSize(itemHolder)
        _initItemPadding(itemHolder)
        _initItemListener(itemHolder)
        _initItemConfig(itemHolder, itemPosition, adapterItem, payloads)
        _initItemStyle(itemHolder)

        onItemBind(itemHolder, itemPosition, adapterItem)
    }

    @Deprecated("请使用4个参数的方法[onItemBind]")
    open fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem
    ) {
        //no op
    }

    /**动画支持, 不为0表示激活动画*/
    @AnimRes
    @AnimatorRes
    var itemAnimateRes: Int = 0

    open fun onItemBindAfter(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        //请注意缓存.
        //itemHolder.clear()
        _initItemAnimate(itemHolder)
    }

    /**动画延迟的时间, 也是动画是否已经执行过的标识
     * [com.angcyo.dsladapter.internal.AnimateDelayHandler]*/
    var _itemAnimateDelay: Long = -1

    /**
     * [android.animation.Animator]
     * [android.view.animation.Animation]*/
    open fun _initItemAnimate(itemHolder: DslViewHolder) {
        if (itemAnimateRes != 0) {
            _itemAnimateDelay =
                itemDslAdapter?.adapterItemAnimateDelayHandler?.computeAnimateDelay(this)
                    ?: _itemAnimateDelay
            //L.w("_itemAnimateDelay:$_itemAnimateDelay")
            if (_itemAnimateDelay >= 0) {
                val animation = animationOf(itemHolder.context, itemAnimateRes)
                if (animation != null) {
                    animation.startOffset = _itemAnimateDelay
                    itemHolder.itemView.startAnimation(animation)
                    _itemAnimateDelay = -2 //动画已执行标识
                } else {
                    val animator = animatorOf(itemHolder.context, itemAnimateRes)
                    if (animator != null) {
                        animator.setTarget(itemHolder.itemView)
                        animator.startDelay = _itemAnimateDelay
                        itemHolder.itemView.setAnimator(animator)
                        animator.start()
                        _itemAnimateDelay = -2 //动画已执行标识
                    }
                }
            }
        }
    }

    /**用于覆盖默认操作*/
    var itemBindOverride: ItemBindAction = { _, _, _, _ ->
    }

    /**
     * [DslAdapter.onViewAttachedToWindow]
     * */
    var itemViewAttachedToWindow: HolderBindAction = { itemHolder, itemPosition ->
        onItemViewAttachedToWindow(itemHolder, itemPosition)
    }

    /**
     * [DslAdapter.onViewDetachedFromWindow]
     * */
    var itemViewDetachedToWindow: HolderBindAction = { itemHolder, itemPosition ->
        onItemViewDetachedToWindow(itemHolder, itemPosition)
    }

    /**
     * [DslAdapter.onViewRecycled]
     * */
    var itemViewRecycled: HolderBindAction = { itemHolder, itemPosition ->
        onItemViewRecycled(itemHolder, itemPosition)
    }

    /**
     * 如果当前的[DslAdapterItem]在另一个[DslAdapterItem]内, 此属性用来保存父[DslAdapterItem]标识
     * [INestedRecyclerItem]*/
    var itemParentRef: WeakReference<DslAdapterItem>? = null

    //</editor-fold desc="标准属性">

    //<editor-fold desc="内部初始化">

    //初始化激活状态
    open fun _initItemEnable(itemHolder: DslViewHolder) {
        itemHolder.enable(itemHolder.itemView, itemEnable, true)
    }

    //初始化背景
    open fun _initItemBackground(itemHolder: DslViewHolder) {
        itemHolder.itemView.isSelected = itemIsSelected

        if (itemBackgroundDrawable !is UndefinedDrawable) {
            itemHolder.itemView.apply {
                setBgDrawable(itemBackgroundDrawable)
            }
        }
    }

    //初始化宽高
    open fun _initItemSize(itemHolder: DslViewHolder) {
        val itemView = itemHolder.itemView

        //初始化默认值
        if (itemMinWidth == undefined_size && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            itemMinWidth = itemView.minimumWidth
        }
        if (itemMinHeight == undefined_size && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            itemMinHeight = itemView.minimumHeight
        }
        //设置
        if (itemMinWidth != undefined_size) {
            itemView.minimumWidth = itemMinWidth
        }
        if (itemMinHeight != undefined_size) {
            itemView.minimumHeight = itemMinHeight
        }

        //初始化默认值
        if (itemWidth == undefined_size) {
            itemWidth = itemView.layoutParams?.width ?: itemWidth
        }
        if (itemHeight == undefined_size) {
            itemHeight = itemView.layoutParams?.height ?: itemHeight
        }
        //设置
        itemView.setWidthHeight(itemWidth, itemHeight)
    }

    //初始化事件
    open fun _initItemListener(itemHolder: DslViewHolder) {
        val clickListener = _clickListener
        if (itemClick == null || clickListener == null) {
            if (!itemEnable) {
                itemHolder.itemView.setOnClickListener(null)
            }
        } else {
            if (clickListener is ThrottleClickListener) {
                clickListener.throttleInterval = itemClickThrottleInterval
            }
            itemHolder.clickItem(clickListener)
        }

        if (itemLongClick == null || _longClickListener == null || !itemEnable) {
            itemHolder.itemView.isLongClickable = false
        } else {
            itemHolder.itemView.setOnLongClickListener(_longClickListener)
        }
    }

    open fun _initItemConfig(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
    }

    open fun _initItemStyle(itemHolder: DslViewHolder) {
        val lineView = itemHolder.view(R.id.lib_item_line_view)
        if (lineView != null) {
            val showLine = itemShowLastLineView
            if (showLine == null) {
                if (itemAutoHideLastLineView) {
                    lineView.gone(isLastPositionInGroup())
                }
            } else {
                lineView.visible(showLine)
            }
        }
    }

    //初始化padding
    open fun _initItemPadding(itemHolder: DslViewHolder) {
        if (itemPaddingLeft == undefined_size) {
            itemPaddingLeft = itemHolder.itemView.paddingLeft
        }
        if (itemPaddingRight == undefined_size) {
            itemPaddingRight = itemHolder.itemView.paddingRight
        }
        if (itemPaddingTop == undefined_size) {
            itemPaddingTop = itemHolder.itemView.paddingTop
        }
        if (itemPaddingBottom == undefined_size) {
            itemPaddingBottom = itemHolder.itemView.paddingBottom
        }
        itemHolder.itemView.setPadding(
            itemPaddingLeft,
            itemPaddingTop,
            itemPaddingRight,
            itemPaddingBottom
        )
    }

    //</editor-fold desc="内部初始化">

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
                itemSpanCount = FULL_ITEM
            }
        }

    /**
     * 当前分组是否[展开]
     * */
    @UpdateByDiff
    var itemGroupExtend: Boolean by UpdateDependProperty(
        true,
        listOf(PAYLOAD_UPDATE_PART, PAYLOAD_UPDATE_EXTEND)
    )

    /**是否需要隐藏item*/
    @UpdateByDiff
    var itemHidden: Boolean by UpdateDependProperty(
        false,
        listOf(PAYLOAD_UPDATE_PART, PAYLOAD_UPDATE_HIDDEN)
    )

    /**查找与[dslAdapterItem]相同分组的所有[DslAdapterItem]
     * [dslAdapterItem] 查询的目标
     * [useFilterList] 数据源
     * [lm] 指定外部的布局管理*/
    open fun findItemGroupParams(
        dslAdapter: DslAdapter,
        useFilterList: Boolean = true,
        lm: RecyclerView.LayoutManager? = null
    ): ItemGroupParams {
        val dslAdapterItem = this
        val allItemList = dslAdapter.getDataList(useFilterList)
        val groupList = mutableListOf<ItemGroupParams>()

        //目标分组信息, 需要返回的数据
        var anchorParams: ItemGroupParams? = null

        var nextItem: DslAdapterItem? = null
        var nextParams: ItemGroupParams? = null
        var nextGroupItems = mutableListOf<DslAdapterItem>()

        for (i in allItemList.indices) {
            val item = allItemList[i]
            if (i == 0 || nextItem == item) {
                //保存之前
                nextParams?.apply {
                    indexInGroup = nextGroupItems.indexOf(dslAdapterItem)
                    groupIndex = groupList.indexOf(this)
                    //edgeGridParams, 暂不初始化
                }

                //新建组
                nextParams = ItemGroupParams().apply {
                    nextGroupItems = mutableListOf()
                    nextGroupItems.add(item)

                    groupList.add(this)

                    this.groupList = groupList
                    this.groupItems = nextGroupItems
                    currentAdapterItem = item
                }

                //目标
                if (item == dslAdapterItem) {
                    anchorParams = nextParams
                }

                for (j in i + 1 until allItemList.count()) {
                    val otherItem = allItemList.getOrNull(j)
                    if (otherItem != null) {
                        //目标
                        if (otherItem == dslAdapterItem) {
                            anchorParams = nextParams
                        }

                        if (item.isInGroupItem(otherItem)) {
                            nextGroupItems.add(otherItem)
                        } else {
                            nextItem = otherItem
                            break
                        }
                    }
                }
            }
        }
        anchorParams?.currentAdapterItem = dslAdapterItem
        //最后一个
        nextParams?.apply {
            indexInGroup = nextGroupItems.indexOf(currentAdapterItem!!)
            groupIndex = groupList.indexOf(this)
        }

        /*var interruptGroup = false
        var findAnchor = false*/

        //分组数据计算
        /*for (i in allItemList.indices) {
            val targetItem = allItemList[i]

            if (!interruptGroup) {
                when {
                    targetItem == dslAdapterItem -> {
                        params.groupItems.add(targetItem)
                        findAnchor = true
                    }
                    dslAdapterItem.isItemInGroups(targetItem) -> params.groupItems.add(targetItem)
                    //如果不是连续的, 则中断分组
                    findAnchor -> interruptGroup = true
                }
            }

            if (interruptGroup) {
                break
            }
        }

        anchorParams.indexInGroup = anchorParams.groupItems.indexOf(dslAdapterItem)*/

        //网格边界计算
        val layoutManager = lm ?: dslAdapter._recyclerView?.layoutManager

        if (anchorParams != null && layoutManager is GridLayoutManager) {
            layoutManager.apply {
                val itemPosition = allItemList.indexOf(dslAdapterItem)

                val groupItemList = anchorParams.groupItems

                val spanSizeLookup = spanSizeLookup ?: GridLayoutManager.DefaultSpanSizeLookup()

                //当前位置
                val currentSpanParams =
                    getSpanParams(
                        spanSizeLookup,
                        itemPosition,
                        spanCount,
                        anchorParams.indexInGroup
                    )

                //下一个的信息
                val nextItemPosition: Int = itemPosition + 1
                val nextSpanParams = if (allItemList.size > nextItemPosition) {
                    getSpanParams(
                        spanSizeLookup,
                        nextItemPosition,
                        spanCount,
                        groupItemList.indexOf(allItemList[nextItemPosition])
                    )
                } else {
                    SpanParams()
                }

                //分组第一个
                val firstItemPosition = allItemList.indexOf(groupItemList.firstOrNull())
                val firstSpanParams = if (firstItemPosition == -1) {
                    SpanParams()
                } else {
                    getSpanParams(
                        spanSizeLookup,
                        firstItemPosition,
                        spanCount,
                        groupItemList.indexOf(allItemList[firstItemPosition])
                    )
                }

                //分组最后一个
                val lastItemPosition = allItemList.indexOf(groupItemList.lastOrNull())
                val lastSpanParams = if (lastItemPosition == -1) {
                    SpanParams()
                } else {
                    getSpanParams(
                        spanSizeLookup,
                        lastItemPosition,
                        spanCount,
                        groupItemList.indexOf(allItemList[lastItemPosition])
                    )
                }

                //adapter第一个
                val firstPosition = 0
                val firstParams = if (allItemList.isEmpty()) {
                    SpanParams()
                } else {
                    getSpanParams(
                        spanSizeLookup,
                        firstPosition,
                        spanCount,
                        groupItemList.indexOf(allItemList[firstPosition])
                    )
                }

                //adapter最后一个
                val lastPosition = allItemList.lastIndex
                val lastParams = if (allItemList.isEmpty()) {
                    SpanParams()
                } else {
                    getSpanParams(
                        spanSizeLookup,
                        lastPosition,
                        spanCount,
                        groupItemList.indexOf(allItemList[lastPosition])
                    )
                }

                anchorParams.edgeGridParams = EdgeGridParams(
                    currentSpanParams, nextSpanParams,
                    firstSpanParams, lastSpanParams,
                    firstParams, lastParams
                )
            }
        } else if (layoutManager is LinearLayoutManager) {
            //todo
        } else if (layoutManager == null) {
            L.w("layoutManager is null")
        }

        return anchorParams ?: dslAdapterItem.createItemGroupParams()
    }

    /**清理分组缓存
     * [com.angcyo.dsladapter.DslAdapter._updateAdapterItems]*/
    open fun clearItemGroupParamsCache() {
        _itemGroupParamsCache = null
    }

    /**当前的[DslAdapterItem]是否在分组中的最后一个*/
    open fun isLastPositionInGroup(useFilterList: Boolean = true): Boolean {
        val adapter = itemDslAdapter ?: return false
        return if (itemGroups.isEmpty()) {
            //简单分组
            val allItemList = adapter.getDataList(useFilterList)
            val index = allItemList.indexOf(this)
            allItemList.size - 1 == index
        } else {
            itemGroupParams.isLastPosition()
        }
    }

    //</editor-fold>

    //<editor-fold desc="悬停相关属性">

    /**
     * 是否需要悬停, 在使用了 [HoverItemDecoration] 时, 有效.
     * [itemIsGroupHead]
     * */
    var itemIsHover: Boolean = itemIsGroupHead

    //</editor-fold>

    //<editor-fold desc="表单/分割线配置">

    /**
     * 需要插入分割线的大小
     * */
    var itemTopInsert = 0
    var itemLeftInsert = 0
    var itemRightInsert = 0
    var itemBottomInsert = 0

    /**是否绘制分割线*/
    var itemDrawLeft: Boolean = true
    var itemDrawTop: Boolean = true
    var itemDrawRight: Boolean = true
    var itemDrawBottom: Boolean = true

    var itemDecorationColor = Color.TRANSPARENT

    /**更强大的分割线自定义, 在color绘制后绘制*/
    var itemDecorationDrawable: Drawable? = null

    /**
     * 仅绘制offset的区域
     * */
    var onlyDrawOffsetArea = false

    /**自动隐藏分组最后一个item的[R.id.lib_item_line_view]view*/
    var itemAutoHideLastLineView: Boolean = true

    /**是否强制隐藏/显示线*/
    var itemShowLastLineView: Boolean? = null

    /**不绘制分组最后一个item的底部分割线*/
    var noDrawLastItemDecoration: Boolean = true

    /**
     * 分割线绘制时的偏移
     * */
    var itemTopOffset = 0
    var itemLeftOffset = 0
    var itemRightOffset = 0
    var itemBottomOffset = 0

    /**可以覆盖设置分割线的边距*/
    var onSetItemOffset: ((outRect: Rect) -> Unit)? = null

    /**分割线入口 [DslItemDecoration]*/
    fun setItemOffsets(outRect: Rect) {
        outRect.set(itemLeftInsert, itemTopInsert, itemRightInsert, itemBottomInsert)
        onSetItemOffset?.invoke(outRect)
    }

    /**
     * 绘制不同方向的分割线时, 触发的回调, 可以用来设置不同方向分割线的颜色
     * */
    var eachDrawItemDecoration: (left: Int, top: Int, right: Int, bottom: Int) -> Unit =
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
        if (itemTopInsert > 0 && itemDrawTop) {
            if (onlyDrawOffsetArea) {
                //绘制左右区域
                if (itemLeftOffset > 0) {
                    drawRect.set(
                        itemView.left,
                        itemView.top - offsetRect.top,
                        itemView.left + itemLeftOffset,
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
                    itemView.left + itemLeftOffset,
                    itemView.top - offsetRect.top,
                    itemView.right - itemRightOffset,
                    itemView.top
                )
                canvas.drawRect(drawRect, paint)
                onDrawItemDecorationDrawable(canvas, drawRect)
            }
        }

        onlyDrawOffsetArea = drawOffsetArea
        eachDrawItemDecoration(0, 0, 0, itemBottomInsert)
        paint.color = itemDecorationColor
        if (itemBottomInsert > 0 && itemDrawBottom && !(noDrawLastItemDecoration && isLastPositionInGroup())) {
            if (onlyDrawOffsetArea) {
                //绘制左右区域
                if (itemLeftOffset > 0) {
                    drawRect.set(
                        itemView.left,
                        itemView.bottom,
                        itemView.left + itemLeftOffset,
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
                    itemView.left + itemLeftOffset,
                    itemView.bottom,
                    itemView.right - itemRightOffset,
                    itemView.bottom + offsetRect.bottom
                )
                canvas.drawRect(drawRect, paint)
                onDrawItemDecorationDrawable(canvas, drawRect)
            }
        }

        onlyDrawOffsetArea = drawOffsetArea
        eachDrawItemDecoration(itemLeftInsert, 0, 0, 0)
        paint.color = itemDecorationColor
        if (itemLeftInsert > 0 && itemDrawLeft) {
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
                    itemView.top + itemTopOffset,
                    itemView.left,
                    itemView.bottom - itemBottomOffset
                )
                canvas.drawRect(drawRect, paint)
                onDrawItemDecorationDrawable(canvas, drawRect)
            }
        }

        onlyDrawOffsetArea = drawOffsetArea
        eachDrawItemDecoration(0, 0, itemRightInsert, 0)
        paint.color = itemDecorationColor
        if (itemRightInsert > 0 && itemDrawRight) {
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
                    itemView.top + itemTopOffset,
                    itemView.right + offsetRect.right,
                    itemView.bottom - itemBottomOffset
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

    //</editor-fold desc="表单/分割线配置">

    //<editor-fold desc="Diff相关">

    /**标识当前的item是否被移除了, 被移除之后, 会影响[thisAreItemsTheSame]的比对*/
    var itemRemoveFlag: Boolean = false

    /**是否需要更新item,等同于[itemChanging], 但不会触发[itemChanged]
     * 在[diffResult]之后会被重置为[false]
     * * 默认为[true], 确保每次new的[DslAdapterItem]有机会更新数据
     * [itemChanging]*/
    var itemUpdateFlag: Boolean = true

    /**
     * 决定
     * [RecyclerView.Adapter.notifyItemInserted]
     * [RecyclerView.Adapter.notifyItemRemoved]
     * 的执行判断
     *
     * [fromItem] 由那个item触发的更新操作
     * [this] 旧item
     * [newItem] 新item
     *
     * 此方法的默认实现可能无法应对所有场景, 请自行覆盖重写
     *
     * @return true 表示2个item相同, false 表示不同
     * */
    var thisAreItemsTheSame: (
        fromItem: DslAdapterItem?, newItem: DslAdapterItem,
        oldItemPosition: Int, newItemPosition: Int
    ) -> Boolean = { fromItem, newItem, oldItemPosition, newItemPosition ->
        var result = this == newItem
        if (itemRemoveFlag) {
            result = false
        } else if (!result) {
            val thisItemClassname = this.className()
            val newItemClassName = newItem.className()
            //相同类名, 且布局类型相同的2个item, 不进行insert/remove操作
            result = if (thisItemClassname == newItemClassName) {
                if ((itemViewType ?: itemLayoutId) ==
                    (newItem.itemViewType ?: newItem.itemLayoutId)
                ) {
                    //布局一样
                    if (itemData == null && newItem.itemData == null) {
                        //未设置itemData时, 默认认为item是一样的
                        true
                    } else {
                        itemData == newItem.itemData
                    }
                } else {
                    false
                }
            } else {
                false
            }
        }
        result
    }

    /**
     * [RecyclerView.Adapter.notifyItemChanged]
     *
     * [fromItem] 由那个item触发的更新操作
     * [this] 旧item
     * [newItem] 新item
     *
     * 此方法的默认实现可能无法应对所有场景, 请自行覆盖重写
     *
     * @return true 表示2个item内容相同, false 表示内容不同, 则会触发[RecyclerView.Adapter.notifyItemChanged]
     * */
    var thisAreContentsTheSame: (
        fromItem: DslAdapterItem?, newItem: DslAdapterItem,
        oldItemPosition: Int, newItemPosition: Int
    ) -> Boolean = { fromItem, newItem, _, _ ->
        when {
            itemUpdateFlag || newItem.itemUpdateFlag -> false
            itemChanging || newItem.itemChanging -> false
            else -> if (itemData == null && newItem.itemData == null) {
                //未设置itemData时, 需要刷新界面的情况
                this == newItem
            } else {
                itemData == newItem.itemData
            }
        }
    }

    var thisGetChangePayload: (
        fromItem: DslAdapterItem?, filterPayload: Any?, newItem: DslAdapterItem,
        oldItemPosition: Int, newItemPosition: Int
    ) -> Any? = { _, filterPayload, _, _, _ ->
        filterPayload ?: PAYLOAD_UPDATE_PART
    }

    /**Diff计算完成之后的回调
     * [com.angcyo.dsladapter.DslDataFilter.UpdateTaskRunnable.onDiffResult]
     * [com.angcyo.dsladapter.DslAdapter.notifyItemChanged]
     * [com.angcyo.dsladapter.DslAdapter.notifyDataChanged]
     * [com.angcyo.dsladapter.DslAdapter.updateAllItem]
     * [com.angcyo.dsladapter.DslAdapter.updateItems]
     * */
    open fun diffResult(filterParams: FilterParams?, result: DiffUtil.DiffResult?) {
        itemChanging = false
        itemUpdateFlag = false
    }

    //</editor-fold desc="Diff相关">

    //<editor-fold desc="定向更新">

    /**标识此[Item]是否发生过改变, 可用于实现退出界面提示是否保存内容.*/
    var itemChanged = false
        set(value) {
            field = value
            if (value) {
                itemChangeListener(this)
                itemChangeListenerList.forEach { it(this) }
            }
        }

    /**[Item]是否正在改变, 会影响[thisAreContentsTheSame]的判断, 并且会在[Diff]计算完之后, 重置为[false]
     * [itemUpdateFlag]*/
    var itemChanging = false
        set(value) {
            field = value
            if (value) {
                itemChanged = true
            }
        }

    /**
     * 当[itemChanged]为true之后, 触发的回调.
     * 如果拦截了默认操作, 需要注意[updateItemDepend]方法的触发时机
     *
     * 提供一个可以完全被覆盖的方法*/
    var itemChangeListener: (DslAdapterItem) -> Unit = {
        onItemChangeListener(it)
    }

    /**其次, 提供一个可以被子类覆盖的方法*/
    @UpdateByDiff
    @UpdateByNotify
    open fun onItemChangeListener(item: DslAdapterItem) {
        //有依赖的item需要更新时, 再刷新界面
        updateItemOnHaveDepend()
    }

    /**[itemChangeListener]*/
    val itemChangeListenerList = mutableSetOf<ItemAction>()

    fun observeItemChange(action: ItemAction): ItemAction {
        itemChangeListenerList.add(action)
        return action
    }

    fun removeItemChangeObserver(action: ItemAction): Boolean {
        return itemChangeListenerList.remove(action)
    }

    /**
     * [checkItem] 是否需要关联到处理列表
     * [itemIndex] 分组折叠之后数据列表中的index
     *
     * 返回 true 时, [checkItem] 进行 [hide] 操作
     * */
    var isItemInHiddenList: (checkItem: DslAdapterItem, itemIndex: Int) -> Boolean =
        { _, _ -> false }

    /**
     * [checkItem]是否是在自己更新后, 通知的item列表里面, 如果是:那么[checkItem]的会触发[itemUpdateFrom]
     *
     * [itemIndex] 最终过滤之后数据列表中的index
     * 返回 true 时, [checkItem] 会收到 来自 [this] 的 [itemUpdateFrom] 触发的回调
     *
     * [itemUpdateFrom]
     * */
    var isItemInUpdateList: (checkItem: DslAdapterItem, itemIndex: Int) -> Boolean =
        { _, _ -> false }

    /**入口方法
     * [isItemInUpdateList]
     * 返回值表示, 是否需要更新自己
     * [com.angcyo.dsladapter.DslDataFilter.UpdateTaskRunnable.notifyUpdateDependItem]*/
    var itemUpdateFrom: (fromItem: DslAdapterItem) -> Boolean = {
        onItemUpdateFrom(it)
    }

    /**覆盖方法 [itemUpdateFrom]*/
    open fun onItemUpdateFrom(fromItem: DslAdapterItem): Boolean {
        return true
    }

    /**[itemUpdateFrom]
     * [com.angcyo.dsladapter.DslDataFilter.UpdateTaskRunnable.notifyUpdateDependItem]*/
    val itemUpdateFromListenerList = mutableSetOf<ItemAction>()

    fun observeItemUpdateFrom(action: ItemAction): ItemAction {
        itemUpdateFromListenerList.add(action)
        return action
    }

    fun removeItemUpdateFromObserver(action: ItemAction): Boolean {
        return itemUpdateFromListenerList.remove(action)
    }

    //</editor-fold desc="定向更新">

    //<editor-fold desc="单选/多选相关">

    /**是否选中, 需要 [ItemSelectorHelper.selectorModel] 的支持. */
    var itemIsSelected = false
        set(value) {
            val old = field
            field = value
            if (old != value) {
                onSetItemSelected(value)
            }
        }

    /**简单的互斥操作支持
     * [onSetItemSelected]*/
    var itemSingleSelectMutex: Boolean = false

    /**[otherItem]是否需要互斥*/
    var itemIsSelectMutexAction: (otherItem: DslAdapterItem) -> Boolean = {
        it.className() == this.className()
    }

    /**选中状态改变回调*/
    @UpdateByNotify
    open fun onSetItemSelected(select: Boolean) {
        if (select && itemSingleSelectMutex) {
            itemDslAdapter?.eachItem { index, dslAdapterItem ->
                if (dslAdapterItem != this && itemIsSelectMutexAction(dslAdapterItem)) {
                    //互斥操作
                    dslAdapterItem.itemIsSelected = false
                    dslAdapterItem.updateAdapterItem()
                }
            }
        }
    }

    /**是否 允许改变选中状态*/
    var isItemCanSelected: (fromSelector: Boolean, toSelector: Boolean) -> Boolean =
        { from, to -> from != to }

    /**监听item select改变事件*/
    var onItemSelectorChange: ItemSelectAction = {
        if (it.updateItemDepend) {
            updateItemOnHaveDepend()
        }
    }

    /**选中变化后触发
     * [com.angcyo.dsladapter.ItemSelectorHelper._selectorInner]*/
    open fun _itemSelectorChange(selectorParams: SelectorParams) {
        onItemSelectorChange(selectorParams)
        itemSelectListener.forEach { it(selectorParams) }
    }

    val itemSelectListener = mutableSetOf<ItemSelectAction>()

    fun observeItemSelect(action: ItemSelectAction): ItemSelectAction {
        itemSelectListener.add(action)
        return action
    }

    fun removeItemSelectObserver(action: ItemSelectAction): Boolean {
        return itemSelectListener.remove(action)
    }

    //</editor-fold desc="单选/多选相关">

    //<editor-fold desc="群组相关">

    /**动态计算的属性, 此属性会有性能消耗*/
    val itemGroupParams: ItemGroupParams
        get() = _itemGroupParamsCache ?: itemDslAdapter?.run { findItemGroupParams(this) }?.apply {
            _itemGroupParamsCache = this
        } ?: createItemGroupParams().apply {
            L.w("注意获取[itemGroupParams]时[itemDslAdapter]为null")
        }

    /**缓存*/
    var _itemGroupParamsCache: ItemGroupParams? = null

    /**所在的分组名, 只用来做快捷变量存储*/
    var itemGroups: List<String> = listOf()

    /**核心群组判断的方法
     * 目标[targetItem]是否和[this]属于同一组
     * */
    var isItemInGroups: (targetItem: DslAdapterItem) -> Boolean = { targetItem ->
        var result = if (itemGroups.isEmpty()) {
            //如果自身没有配置分组信息, 那么取相同类名, 布局id一样的item, 当做一组
            className() == targetItem.className() && itemLayoutId == targetItem.itemLayoutId
        } else {
            false
        }
        if (!result) {
            //自身具有sub list, 那么sub中的元素也属于当前组
            result = itemSubList.contains(targetItem)
        }
        if (!result) {
            //自身是子item, 则最近一层的parent
            itemParentList.lastOrNull()?.let { last ->
                result = last.isItemInGroups(targetItem)
            }
        }
        if (!result) {
            for (group in targetItem.itemGroups) {
                result = result || itemGroups.contains(group)

                if (result) {
                    break
                }
            }
        }
        result
    }

    //</editor-fold>

    //<editor-fold desc="拖拽相关">

    /**
     * 当前[DslAdapterItem]是否可以被拖拽.需要[DragCallbackHelper]的支持
     * [itemIsGroupHead]
     * [DragCallbackHelper.getMovementFlags]
     * */
    var itemDragEnable = true

    /**
     * 当前[DslAdapterItem]是否可以被侧滑删除.需要[DragCallbackHelper]的支持
     * */
    var itemSwipeEnable = true

    /**支持拖拽的方向, 0表示不开启拖拽
     * [ItemTouchHelper.LEFT]
     * [ItemTouchHelper.RIGHT]
     * [ItemTouchHelper.UP]
     * [ItemTouchHelper.DOWN]
     * */
    var itemDragFlag = -1

    /**支持滑动删除的方向, 0表示不开启滑动
     * [ItemTouchHelper.LEFT]
     * [ItemTouchHelper.RIGHT]
     * */
    var itemSwipeFlag = -1

    /**[dragItem]是否可以在此位置[this]放下*/
    var isItemCanDropOver: (dragItem: DslAdapterItem) -> Boolean = {
        itemDragEnable
    }

    //</editor-fold>

    //<editor-fold desc="侧滑菜单相关">

    /**用于控制打开or关闭菜单*/
    var _itemSwipeMenuHelper: SwipeMenuHelper? = null

    /**是否激活侧滑菜单.需要[SwipeMenuHelper]的支持*/
    var itemSwipeMenuEnable = true

    /**支持滑动菜单打开的手势方向.
     * [ItemTouchHelper.LEFT]
     * [ItemTouchHelper.RIGHT]
     * */
    var itemSwipeMenuFlag = ItemTouchHelper.LEFT

    /**滑动菜单滑动的方式*/
    var itemSwipeMenuType = SWIPE_MENU_TYPE_DEFAULT

    /**侧滑菜单滑动至多少距离, 重写此方法, 自行处理UI效果*/
    var itemSwipeMenuTo: (itemHolder: DslViewHolder, dX: Float, dY: Float) -> Unit =
        { itemHolder, dX, dY ->
            onItemSwipeMenuTo(itemHolder, dX, dY)
        }

    /**滑动菜单的宽度*/
    var itemSwipeWidth: (itemHolder: DslViewHolder) -> Int = {
        it.itemView.getChildOrNull(0).mW()
    }

    /**滑动菜单的高度*/
    var itemSwipeHeight: (itemHolder: DslViewHolder) -> Int = {
        it.itemView.getChildOrNull(0).mH()
    }

    /**请将menu, 布局在第1个child的位置, 并且布局的[left]和[top]都是0
     * 默认的UI效果就是, TranslationX.
     * 默认实现暂时只支持左/右滑动的菜单, 上/下滑动菜单不支持
     * */
    open fun onItemSwipeMenuTo(itemHolder: DslViewHolder, dX: Float, dY: Float) {
        val parent = itemHolder.itemView
        if (parent is ViewGroup && parent.childCount > 1) {
            //菜单最大的宽度, 用于限制滑动的边界
            val menuWidth = itemSwipeWidth(itemHolder)
            val tX = clamp(dX, -menuWidth.toFloat(), menuWidth.toFloat())
            parent.forEach { index, child ->
                if (index == 0) {
                    if (itemSwipeMenuType == SWIPE_MENU_TYPE_FLOWING) {
                        if (dX > 0) {
                            child.translationX = -menuWidth + tX
                        } else {
                            child.translationX = parent.mW() + tX
                        }
                    } else {
                        if (dX > 0) {
                            child.translationX = 0f
                        } else {
                            child.translationX = (parent.mW() - menuWidth).toFloat()
                        }
                    }
                } else {
                    child.translationX = tX
                }
            }
        }
    }

    //</editor-fold>

    //<editor-fold desc="Tree 树结构相关">

    /**
     * 折叠/展开 依旧使用[itemGroupExtend]控制
     * [itemLoadSubList] 加载完之后, 数据放在[itemSubList]
     *
     * 子项列表
     * [com.angcyo.dsladapter.internal.SubItemFilterInterceptor.loadSubItemList]
     * */
    var itemSubList: MutableList<DslAdapterItem> = mutableListOf()

    /**
     * 在控制[itemSubList]之前, 都会回调此方法.
     * 相当于hook了[itemSubList], 可以在[itemSubList]为空时, 展示[加载中Item]等
     *
     * [com.angcyo.dsladapter.internal.SubItemFilterInterceptor.loadSubItemList]
     * */
    var itemLoadSubList: () -> Unit = {}

    /**父级列表, 会自动赋值
     * [com.angcyo.dsladapter.internal.SubItemFilterInterceptor.loadSubItemList]*/
    var itemParentList: MutableList<DslAdapterItem> = mutableListOf()

    //</editor-fold>

    //<editor-fold desc="Lifecycle支持">

    val lifecycleRegistry = LifecycleRegistry(this)

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }

    /**请勿覆盖[itemViewAttachedToWindow]*/
    @CallSuper
    open fun onItemViewAttachedToWindow(itemHolder: DslViewHolder, itemPosition: Int) {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    /**请勿覆盖[itemViewDetachedToWindow]*/
    @CallSuper
    open fun onItemViewDetachedToWindow(itemHolder: DslViewHolder, itemPosition: Int) {
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    /**请勿覆盖[itemViewRecycled]*/
    @CallSuper
    open fun onItemViewRecycled(itemHolder: DslViewHolder, itemPosition: Int) {
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        }
        if (itemAnimateRes != 0) {
            itemHolder.itemView.cancelAnimator()
            itemHolder.itemView.clearAnimation()
        }
        itemHolder.clear()
    }

    //</editor-fold desc="Lifecycle支持">

}

/**属性改变时, 通过diff更新列表*/
@UpdateByDiff
class UpdateDependProperty<T>(
    var value: T,
    val payload: Any? = DslAdapterItem.PAYLOAD_UPDATE_PART
) : ReadWriteProperty<DslAdapterItem, T> {
    override fun getValue(thisRef: DslAdapterItem, property: KProperty<*>): T = value

    override fun setValue(thisRef: DslAdapterItem, property: KProperty<*>, value: T) {
        val old = this.value
        this.value = value
        if (old != value) {
            thisRef.itemUpdateFlag = true
            thisRef.updateItemDepend(
                FilterParams(thisRef, updateDependItemWithEmpty = true, payload = payload)
            )
        }
    }
}

/**属性改变时, 通过[notifyItemChangedPayload]更新列表*/
@UpdateByNotify
class UpdateAdapterProperty<T>(
    var value: T,
    val payload: Any? = DslAdapterItem.PAYLOAD_UPDATE_PART
) : ReadWriteProperty<DslAdapterItem, T> {

    override fun getValue(thisRef: DslAdapterItem, property: KProperty<*>): T = value

    override fun setValue(thisRef: DslAdapterItem, property: KProperty<*>, value: T) {
        val old = this.value
        this.value = value
        if (old != value) {
            thisRef.itemUpdateFlag = true
            thisRef.updateAdapterItem(payload)
        }
    }
}

/**存储tag数据*/
fun DslAdapterItem.putTag(key: Int, value: Any?) {
    val tags = itemTags ?: SparseArray()
    tags.put(key, value)
    itemTags = tags
}

/**获取tag数据*/
fun DslAdapterItem.getTag(key: Int): Any? = itemTags?.get(key)