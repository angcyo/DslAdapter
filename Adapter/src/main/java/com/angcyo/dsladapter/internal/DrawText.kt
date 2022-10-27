package com.angcyo.dsladapter.internal

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.ViewGroup
import java.lang.Float.max
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * 使用[StaticLayout]进行文本绘制, 支持[SpannableString] 支持换行
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/11/06
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
class DrawText {

    class MakeLayoutProperty<T>(var value: T) : ReadWriteProperty<DrawText, T> {
        override fun getValue(thisRef: DrawText, property: KProperty<*>): T = value

        override fun setValue(thisRef: DrawText, property: KProperty<*>, value: T) {
            this.value = value
            thisRef._textLayout = null
        }
    }

    /**画笔*/
    var textPaint: TextPaint by MakeLayoutProperty(TextPaint(Paint.ANTI_ALIAS_FLAG))

    /**需要绘制的文本*/
    var drawText: CharSequence? by MakeLayoutProperty(null)

    /**文本允许绘制的宽度*/
    var textWidth: Int by MakeLayoutProperty(ViewGroup.LayoutParams.WRAP_CONTENT)

    /**相对行间距，相对字体大小，1.5f表示行间距为1.5倍的字体高度。*/
    var spacingMult: Float by MakeLayoutProperty(1f)

    /**在基础行距上添加多少*/
    var spacingAdd: Float by MakeLayoutProperty(0f)
    var includePad: Boolean by MakeLayoutProperty(false)

    var alignment: Layout.Alignment? by MakeLayoutProperty(Layout.Alignment.ALIGN_NORMAL)

    var _textLayout: StaticLayout? = null

    //---

    fun getWidth() = makeLayout().width

    fun getHeight() = makeLayout().height

    fun getBounds(rect: Rect): Rect {
        val layout = makeLayout()
        rect.set(0, 0, layout.width, layout.height)
        return rect
    }

    //---

    /**重新创建[StaticLayout]*/
    fun makeLayout(): StaticLayout {
        val width = if (textWidth >= 0) textWidth else Int.MAX_VALUE
        val layout = _makeLayout(width)
        _textLayout = layout

        if (textWidth >= 0) {
            //no  op
        } else {
            //重新赋值宽度
            var maxWidth = 0f
            for (line in 0 until layout.lineCount) {
                maxWidth = max(layout.getLineWidth(line), maxWidth)
            }
            //wrap_content 重新计算宽度
            _textLayout = _makeLayout(maxWidth.toInt())
        }

        return _textLayout!!
    }

    /**[width]宽度决定了行数 需要>=0*/
    fun _makeLayout(width: Int): StaticLayout {
        //StaticLayout 只能用一次.

        /**
         * CharSequence source : 需要分行的字符串
         * int bufstart : 需要分行的字符串从第几的位置开始
         * int bufend : 需要分行的字符串到哪里结束
         * TextPaint paint : 画笔对象
         * int outerwidth : layout的宽度，超出时换行
         * Alignment align : layout的对其方式，有ALIGN_CENTER， ALIGN_NORMAL， ALIGN_OPPOSITE 三种
         * float spacingmult : 相对行间距，相对字体大小，1.5f表示行间距为1.5倍的字体高度。
         * float spacingadd : 在基础行距上添加多少
         * boolean includepad,
         * TextUtils.TruncateAt ellipsize : 从什么位置开始省略
         * int ellipsizedWidth : 超过多少开始省略
         * */
        val text = drawText ?: ""
        val layout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(
                text, 0, text.length, textPaint,
                width
            ).run {
                alignment?.run {
                    setAlignment(this)
                }
                setLineSpacing(spacingAdd, spacingMult)
                setIncludePad(includePad)
                //setMaxLines()
                //setEllipsize()
                build()
            }
        } else {
            StaticLayout(text, textPaint, width, alignment, spacingMult, spacingAdd, includePad)
        }
        return layout
    }

    /**开始绘制*/
    fun onDraw(canvas: Canvas) {
        if (drawText.isNullOrEmpty()) {
            return
        }
        /*
        * Layout在绘制的时候, (0, 0) 坐标是文本左上角
        * Canvas.drawText, (0, 0) 坐标是文本Baseline的位置
        * */
        (_textLayout ?: makeLayout()).draw(canvas)
    }
}