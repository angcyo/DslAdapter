package com.angcyo.dsladapter.internal

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.ViewGroup
import com.angcyo.dsladapter.dp
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 *
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

    /**需要绘制的文本*/
    var drawText: CharSequence? by MakeLayoutProperty(null)

    /**文本大小*/
    var textSize: Float by MakeLayoutProperty(14 * dp)

    /**文本绘制的宽度*/
    var textWidth: Int by MakeLayoutProperty(ViewGroup.LayoutParams.WRAP_CONTENT)

    /**文本颜色*/
    var textColor = Color.RED

    /**相对行间距，相对字体大小，1.5f表示行间距为1.5倍的字体高度。*/
    var spacingMult: Float by MakeLayoutProperty(1f)
    /**在基础行距上添加多少*/
    var spacingAdd: Float by MakeLayoutProperty(0f)
    var includePad: Boolean by MakeLayoutProperty(false)

    var alignment: Layout.Alignment? by MakeLayoutProperty(Layout.Alignment.ALIGN_NORMAL)

    var _paint: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    var _textLayout: Layout? = null

    fun makeLayout(): Layout {
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
        _paint.textSize = textSize
        val text = drawText ?: ""
        val width = if (textWidth >= 0) textWidth else _paint.measureText(text.toString()).toInt()

        _textLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(
                text, 0, text.length, _paint,
                width
            ).run {
                alignment?.run {
                    setAlignment(this)
                }
                setLineSpacing(spacingAdd, spacingMult)
                setIncludePad(includePad)
                build()
            }
        } else {
            StaticLayout(text, _paint, width, alignment, spacingMult, spacingAdd, includePad)
        }

        return _textLayout!!
    }

    fun onDraw(canvas: Canvas) {
        if (drawText.isNullOrEmpty()) {
            return
        }
        _paint.color = textColor

        /*
        * Layout在绘制的时候, (0, 0) 坐标是文本左上角
        * Canvas.drawText, (0, 0) 坐标是文本Baseline的位置
        * */
        (_textLayout ?: makeLayout()).draw(canvas)
    }
}