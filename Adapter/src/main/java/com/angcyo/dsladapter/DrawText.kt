package com.angcyo.dsladapter

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/11/06
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
class DrawText {
    var _textLayout: Layout? = null

    /**需要绘制的文本*/
    var drawText: CharSequence? = null

    /**文本大小*/
    var textSize = 14 * dp

    /**文本颜色*/
    var textColor = Color.RED

    var _paint: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)

    fun makeLayout(): Layout? {
        if (drawText.isNullOrEmpty()) {
            return _textLayout
        }
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
        _textLayout = StaticLayout(
            drawText, _paint, _paint.measureText(drawText.toString()).toInt(),
            Layout.Alignment.ALIGN_NORMAL,
            1.0f, 0f, false
        )
        return _textLayout
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
        (_textLayout ?: makeLayout())?.draw(canvas)
    }
}