# DslAdapter
Dsl 的形式使用 RecyclerView.Adapter, 支持情感图状态切换, 加载更多, 多类型Item等

# 特性
- 1.全网最轻量的多类型Item实现方法
- 2.支持情感图状态(空布局 加载中 错误异常等)切换 完美支持自定义扩展
- 3.支持加载更多 完美支持自定义扩展

# 即将支持

- [x] 分组折叠 (类似QQ联系人好友分组,展开和折叠的效果)
- [x] Item悬停 (类似QQ联系人好友分组,悬停的效果)
- [x] 常规的分割线 (一会儿占满屏幕 一会儿有点边距的效果)
- [ ] 支持单选/多选
- [x] 支持某一个Item 定向更新多个其他Item
- [x] 支持群组功能 (指定连续的几个相同/不同的item为一组)

# 功能介绍

## 群组功能

![](https://raw.githubusercontent.com/angcyo/DslAdapter/master/png/group.png)

使用动态计算属性`itemGroupParams`,根据位置设置不同的背景

```kotlin
itemGroupParams.apply {
    if (isOnlyOne()) {
        itemHolder.itemView
            .setBackgroundResource(R.drawable.shape_group_all)
    } else if (isFirstPosition()) {
        itemHolder.itemView
            .setBackgroundResource(R.drawable.shape_group_header)
    } else {
        itemHolder.itemView
            .setBackgroundColor(resources.getColor(R.color.colorAccent))
    }
}
```

# 使用
# 1. 多类型使用方式

## 1.1 方式1(推荐)

继承 `DslAdapterItem`  重写 `itemLayoutId`  实现 `itemBind` 搞定.

```kotlin
class DslTextItem : DslAdapterItem() {
    init {
        itemLayoutId = R.layout.item_text_layout
    }

    override var itemBind: (itemHolder: DslViewHolder, itemPosition: Int, adapterItem: DslAdapterItem) -> Unit =
        { itemHolder, itemPosition, _ ->
            itemHolder.v<TextView>(R.id.text_view).text = "文本位置:$itemPosition"
        }
}
```

## 1.2 方式2

调用扩展方法`dslItem` 传入`layoutId`参数 实现 `itemBind` 搞定.

```kotlin
dslAdapter.dslItem(R.layout.item_text_layout) {
          itemBind = { itemHolder, itemPosition, _ ->
              itemHolder.v<TextView>(R.id.text_view).text = "文本位置:$itemPosition"
          }
      }
```

# 2. 情感图自定义

继承`DslAdapterStatusItem` 将对象设置给`DslAdapter`的`dslAdapterStatusItem`变量 即可.

```kotlin
open class DslAdapterStatusItem : DslAdapterItem() {
    init {
        itemLayoutId = R.layout.item_adapter_status
    }

    companion object {
        const val ADAPTER_STATUS_NONE = -1
        const val ADAPTER_STATUS_EMPTY = 0
        const val ADAPTER_STATUS_LOADING = 1
        const val ADAPTER_STATUS_ERROR = 2
    }

    var itemAdapterStatus: Int = ADAPTER_STATUS_NONE

    override var itemBind: (itemHolder: DslViewHolder, itemPosition: Int, adapterItem: DslAdapterItem) -> Unit =
        { itemHolder, _, _ ->

            /*具体逻辑, 自行处理*/
            itemHolder.v<TextView>(R.id.text_view).text = "情感图状态: ${when (itemAdapterStatus) {
                ADAPTER_STATUS_EMPTY -> "空数据"
                ADAPTER_STATUS_LOADING -> "加载中"
                ADAPTER_STATUS_ERROR -> "加载异常"
                else -> "未知状态"
            }}"
        }

    /**返回[true] 表示不需要显示情感图, 即显示[Adapter]原本的内容*/
    open fun isNoStatus() = itemAdapterStatus == ADAPTER_STATUS_NONE
}

```

# 3. 加载更多自定义
继承`DslLoadMoreItem` 将对象设置给`DslAdapter`的`dslLoadMoreItem`变量 即可.

```kotlin
open class DslLoadMoreItem : DslAdapterItem() {
    init {
        itemLayoutId = R.layout.item_load_more
    }

    companion object {
        const val ADAPTER_LOAD_NORMAL = 0
        const val ADAPTER_LOAD_LOADING = 1
        const val ADAPTER_LOAD_ERROR = 2
        const val ADAPTER_LOAD_NO_MORE = 3
    }

    /**是否激活加载更多*/
    var itemEnableLoadMore = true
        set(value) {
            field = value
            itemLoadMoreStatus = ADAPTER_LOAD_NORMAL
        }

    /**加载更多当前的状态*/
    var itemLoadMoreStatus: Int = ADAPTER_LOAD_NORMAL

    /**加载更多回调*/
    var onLoadMore: (DslViewHolder) -> Unit = {}

    override var itemBind: (itemHolder: DslViewHolder, itemPosition: Int, adapterItem: DslAdapterItem) -> Unit =
        { itemHolder, _, _ ->

            /*具体逻辑, 自行处理*/
            itemHolder.v<TextView>(R.id.text_view).text = "加载更多: ${when (itemLoadMoreStatus) {
                ADAPTER_LOAD_NORMAL -> "加载更多中..."
                ADAPTER_LOAD_LOADING -> "加载更多中..."
                ADAPTER_LOAD_ERROR -> "加载异常"
                ADAPTER_LOAD_NO_MORE -> "我是有底线的"
                else -> "未知状态"
            }}"

            if (itemEnableLoadMore) {
                if (itemLoadMoreStatus == ADAPTER_LOAD_NORMAL) {
                    //错误和正常的情况下, 才触发加载跟多
                    itemLoadMoreStatus = ADAPTER_LOAD_LOADING
                    onLoadMore(itemHolder)
                }
            }
        }

    override var onItemViewDetachedToWindow: (itemHolder: DslViewHolder) -> Unit = {
        if (itemEnableLoadMore) {
            //加载失败时, 下次是否还需要加载更多?
            if (itemLoadMoreStatus == ADAPTER_LOAD_ERROR) {
                itemLoadMoreStatus = ADAPTER_LOAD_NORMAL
            }
        }
    }
}

```

# 最终使用代码

```kotlin
private fun initLayout() {
    dslViewHolder.v<RecyclerView>(R.id.recycler_view).apply {
        layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        adapter = dslAdapter
    }

    dslAdapter.来点数据()
} 

private fun DslAdapter.来点数据() {
    val dslAdapter = this
    for (i in 0..10) {
        //2种使用item的方式, 喜欢哪种方式, 就用哪一种
        dslAdapter.dslTextItem()
        dslAdapter.dslItem(R.layout.item_text_layout) {
            itemBind = { itemHolder, itemPosition, _ ->
                itemHolder.v<TextView>(R.id.text_view).text = "文本位置:$itemPosition"
            }
        }

        for (j in 0..0) {
            //2种使用item的方式, 喜欢哪种方式, 就用哪一种
            dslAdapter.dslImageItem()
            dslAdapter.dslItem(R.layout.item_image_layout) {
                itemBind = { itemHolder, itemPosition, _ ->
                    itemHolder.v<TextView>(R.id.text_view).text = "文本位置:$itemPosition"
                }
            }
        }
    }
}

```

# 使用方法

## 1.JitPack

根 `build.gradle`
```
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```

```
dependencies {
    implementation 'com.github.angcyo:DslAdapter:xxx'
}
```

快照版本:(能够保持最新代码)
```
dependencies {
    implementation 'com.github.angcyo:DslAdapter:master-SNAPSHOT'
}
```

## 2.下载源码

```
git clone https://github.com/angcyo/DslAdapter.git --depth=1
```

拷贝包 `com.angcyo.dsladapter.dsl` 下的`所有文件`到您的工程.

关于更新:

如果未修改源码, 可以直接覆盖.
如果修改了源码 可以使用 AS 提供的 `Compare with Clipboard` 功能 进行比对合并.

加油 你可以的!

---
**群内有`各(pian)种(ni)各(jin)样(qun)`的大佬,等你来撩.**

# 联系作者
[点此快速加群](https://shang.qq.com/wpa/qunwpa?idkey=cbcf9a42faf2fe730b51004d33ac70863617e6999fce7daf43231f3cf2997460)

> 请使用QQ扫码加群, 小伙伴们都在等着你哦!

![](https://raw.githubusercontent.com/angcyo/res/master/image/qq/qq_group_code.png)

> 关注我的公众号, 每天都能一起玩耍哦!

![](https://raw.githubusercontent.com/angcyo/res/master/image/weixin/%E8%AE%A2%E9%98%85%E5%8F%B7_%E4%BA%8C%E7%BB%B4%E7%A0%81/qrcode_for_gh_59fa6d9a51d8_258_8cm.jpg)

 
  
