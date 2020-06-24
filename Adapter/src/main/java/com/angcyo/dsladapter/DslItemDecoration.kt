package com.angcyo.dsladapter

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 *
 * 基础分割线支持
 *
 * https://github.com/angcyo/DslAdapter/wiki/%E5%88%86%E5%89%B2%E7%BA%BF
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/05/10
 * Copyright (canvas) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
open class DslItemDecoration(
    val init: (that: DslItemDecoration) -> Unit = { _ -> },
    val onDrawOver: (that: DslItemDecoration, canvas: Canvas, parent: RecyclerView, state: RecyclerView.State, paint: Paint) -> Unit = { _, _, _, _, _ -> },
    val onDraw: (that: DslItemDecoration, canvas: Canvas, parent: RecyclerView, state: RecyclerView.State, paint: Paint) -> Unit = { _, _, _, _, _ -> },
    val getItemOffsets: (that: DslItemDecoration, outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) -> Unit = { _, _, _, _, _ -> }
) : RecyclerView.ItemDecoration() {

    val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textSize = 12 * dp
    }

    val _tempDrawRect = Rect()
    val _tempRect = Rect()

    /**
     * 将3个方法, 合一调用. 通过参数, 来区分是那一个方法.
     *
     * outRect 不为空时, 是 getItemOffsets 方法
     * canvas 不为空时, 是 onDrawOver onDraw
     * isOverDraw 控制是否是 onDrawOver
     * */
    open var eachItemDoIt: (
        canvas: Canvas?, parent: RecyclerView, state: RecyclerView.State, outRect: Rect?,
        beforeViewHolder: DslViewHolder?,
        viewHolder: DslViewHolder,
        afterViewHolder: DslViewHolder?,
        isOverDraw: Boolean
    ) -> Unit =
        { canvas, parent, state, outRect,
          beforeViewHolder, viewHolder, afterViewHolder,
          isOverDraw ->
            onEachItemDoIt(
                canvas,
                parent,
                state,
                outRect,
                beforeViewHolder,
                viewHolder,
                afterViewHolder,
                isOverDraw
            )
        }

    init {
        init.invoke(this)
    }

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        onDrawOver.invoke(this, canvas, parent, state, paint)
        parent.eachChildRViewHolder { beforeViewHolder, viewHolder, afterViewHolder ->
            eachItemDoIt.invoke(
                canvas,
                parent,
                state,
                null,
                beforeViewHolder,
                viewHolder,
                afterViewHolder,
                true
            )
        }
    }

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        onDraw.invoke(this, canvas, parent, state, paint)
        parent.eachChildRViewHolder { beforeViewHolder, viewHolder, afterViewHolder ->
            eachItemDoIt.invoke(
                canvas,
                parent,
                state,
                null,
                beforeViewHolder,
                viewHolder,
                afterViewHolder,
                false
            )
        }
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        getItemOffsets.invoke(this, outRect, view, parent, state)
        parent.eachChildRViewHolder(view) { beforeViewHolder, viewHolder, afterViewHolder ->
            eachItemDoIt.invoke(
                null,
                parent,
                state,
                outRect,
                beforeViewHolder,
                viewHolder,
                afterViewHolder,
                false
            )
        }
    }

    open fun onEachItemDoIt(
        canvas: Canvas?, parent: RecyclerView, state: RecyclerView.State,
        outRect: Rect?,
        beforeViewHolder: DslViewHolder?,
        viewHolder: DslViewHolder,
        afterViewHolder: DslViewHolder?,
        isOverDraw: Boolean
    ) {
        val adapterPosition = viewHolder.adapterPosition
        if (parent.adapter is DslAdapter && adapterPosition != RecyclerView.NO_POSITION) {
            (parent.adapter as? DslAdapter)?.getItemData(adapterPosition)?.let { item ->

                //设置分割线占坑大小
                outRect?.let {
                    item.setItemOffsets(it)
                }

                if (state.isPreLayout || state.willRunSimpleAnimations()) {
                } else {
                    canvas?.let {
                        if (!isOverDraw) {
                            //绘制分割线
                            _tempRect.clear()
                            item.setItemOffsets(_tempRect)
                            item.draw(
                                it,
                                paint,
                                viewHolder.itemView,
                                _tempRect,
                                parent.adapter?.itemCount ?: 0,
                                adapterPosition,
                                _tempDrawRect
                            )
                        }
                    }
                }
            }
        }
    }

    //<editor-fold desc="操作方法">

    /** 调用此方法, 安装分割线 */
    fun attachToRecyclerView(recyclerView: RecyclerView?) {
        recyclerView?.addItemDecoration(this)
    }

    /**卸载分割线*/
    fun detachedFromRecyclerView(recyclerView: RecyclerView?) {
        recyclerView?.removeItemDecoration(this)
    }

    //</editor-fold desc="操作方法">

}