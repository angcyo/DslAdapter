package com.angcyo.dsladapter.internal

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView

/**
 * 此布局会占满RecycleView第一屏的底部所有空间
 *
 *
 * Email:angcyo@126.com
 *
 * @author angcyo
 * @date 2018/10/09
 */
class RecyclerBottomLayout(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    var _layoutMeasureHeight = -1

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        if (heightMode != MeasureSpec.EXACTLY) {
            _layoutMeasureHeight = measuredHeight
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val parent = parent
        //Log.w("angcyo", "layout:" + top + " " + bottom);
        var callSuper = true
        if (parent is RecyclerView) {
            if (parent.scrollState != RecyclerView.SCROLL_STATE_IDLE) {
                //滚动过程不处理
                super.onLayout(changed, left, top, right, bottom)
                return
            }

            val layoutParams = layoutParams as RecyclerView.LayoutParams
            val parentHeight = parent.measuredHeight
            //只处理第一屏
            if (parent.computeVerticalScrollOffset() == 0 &&
                top < parentHeight /*布局有部分展示了*/ &&
                bottom > top
            ) {
                if (bottom + layoutParams.bottomMargin != parentHeight) { //布局未全部展示
                    //当前布局在RecyclerView的第一屏(没有任何滚动的状态), 并且底部没有显示全.
                    var spaceHeight = parentHeight - top - layoutParams.bottomMargin
                    var handle: Boolean
                    if (_layoutMeasureHeight > 0) {
                        handle =
                            spaceHeight - layoutParams.topMargin - layoutParams.bottomMargin > _layoutMeasureHeight
                        if (!handle) { //如果缓存了布局, 会出现此情况. 高度变高后, 无法回退到真实高度
                            if (_layoutMeasureHeight != measuredHeight) {
                                spaceHeight = _layoutMeasureHeight
                                handle = true
                            }
                        }
                    } else {
                        handle =
                            spaceHeight - layoutParams.topMargin - layoutParams.bottomMargin > bottom - top
                    }
                    if (handle) { //剩余空间足够大, 同时也解决了动态隐藏导航栏带来的BUG
                        callSuper = false
                        layoutParams.height = spaceHeight
                        setLayoutParams(layoutParams)
                        post {
                            //Log.e("angcyo", "重置高度:" + layoutParams.height);
                            val adapter = parent.adapter
                            if (adapter != null) {
                                adapter.notifyItemChanged(layoutParams.viewAdapterPosition)
                            } else {
                                requestLayout()
                            }
                        }
                    }
                }
            }
        }
        if (callSuper) {
            super.onLayout(changed, left, top, right, bottom)
        }
    }
}