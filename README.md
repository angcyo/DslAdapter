# DslAdapter
`Dsl` 的形式使用 `RecyclerView.Adapter`, 支持情感图状态切换, 加载更多, 多类型`Item`等

所有`Item`继承自`DslAdapterItem`即可在`DslAdapter`中使用.

# 特性
1. `全网最轻量`的`多类型Item`实现方法
2. 支持`情感图状`态(空布局 加载中 错误异常等)切换 完美支持自定义扩展
3. 支持`加载更多` 完美支持自定义扩展
4. 支持分组`折叠` (类似QQ联系人好友分组,展开和折叠的效果) 
5. 支持Item`悬停` (类似QQ联系人好友分组,悬停的效果)
6. 支持常规的`分割线` (一会儿占满屏幕 一会儿有点边距的效果)
7. 支持单`选/多选` (支持固定选项)
8. 支持某一个Item `定向更新`多个其他Item
9. 支持`群组`功能 (指定连续的几个相同/不同的item为一组)
10. 支持`滑动选择`(手指拖拽, 就可以选中范围内item)
11. 支持`拖拽排序` `侧滑删除` (注意不是侧滑菜单)


# 功能介绍

请访问[WIKI文档](https://github.com/angcyo/DslAdapter/wiki)查看详情说明.

## 分组折叠

折叠功能需要属性`itemIsGroupHead=true`和`itemGroupExtend`的配置

`itemGroupExtend=true`:展开

`itemGroupExtend=false`:折叠

折叠的`Item`, 会在`itemIsGroupHead=true`到下一个`itemIsGroupHead=true`之间的`Item`


```kotlin
dslItem(R.layout.item_group_head) {
    itemIsGroupHead = true
    onItemBindOverride = { itemHolder, itemPosition, adapterItem ->
        itemHolder.tv(R.id.fold_button).text =
            if (itemGroupExtend) "折叠 $itemPosition" else "展开 $itemPosition"

        itemHolder.click(R.id.fold_button) {
            itemGroupExtend = !itemGroupExtend
        }
    }
}
```

## 悬停

属性`itemIsHover=true`开启即可, 需要`HoverItemDecoration`的支持.

```kotlin
HoverItemDecoration().attachToRecyclerView(RecyclerView)
```

默认情况下, `itemIsGroupHead=true`时, 会自动开启`itemIsHover`, 如果不符合, 需要手动关闭`itemIsHover=false`

[关于HoverItemDecoration更多信息](https://github.com/angcyo/HoverItemDecoration)

## 分割线

需要`DslItemDecoration`的支持.

```kotlin
RecyclerView.addItemDecoration(DslItemDecoration())
```

分割线相关的属性:

```kotlin
//控制item上下左右的间距
itemTopInsert
itemLeftInsert
itemRightInsert
itemBottomInsert

//控制分割线只绘制offset区域
onlyDrawOffsetArea
itemTopOffset
itemLeftOffset
itemRightOffset
itemBottomOffset

//分割线的颜色和自定义的Drawable
itemDecorationColor
itemDecorationDrawable
```

示例代码:

```kotlin
dslItem(DslDemoItem()) {
    itemText = "顶部的分割线是红色"
    itemTopInsert = 8 * dpi
    itemDecorationColor = Color.RED //控制分割线的颜色
}

dslItem(DslDemoItem()) {
    itemText = "只绘制偏移量的分割线"
    itemTopInsert = 8 * dpi
    itemLeftOffset = 60 * dpi
    itemDecorationColor = Color.BLUE
    onlyDrawOffsetArea = true
}

dslItem(DslDemoItem()) {
    itemText = "自定义Drawable的分割线"
    itemBottomInsert = 20 * dpi
    itemDecorationDrawable = resources.getDrawable(R.drawable.shape_decoration)
}

dslItem(DslDemoItem()) {
    itemText = "上下都有的分割线"
    itemTopInsert = 8 * dpi
    itemBottomInsert = 8 * dpi
    itemDecorationColor = Color.GREEN
}
```

## 单选/多选

单选/多选的功能由`ItemSelectorHelper`提供,通过`DslAdapter`成员变量`itemSelectorHelper`访问.

**选择模式切换:**

```kotlin
//目前支持 单选/多选 
dslAdapter.itemSelectorHelper.selectorModel = MODEL_NORMAL
dslAdapter.itemSelectorHelper.selectorModel = MODEL_SINGLE
dslAdapter.itemSelectorHelper.selectorModel = MODEL_MULTI

```

**固定选项:**

```kotlin
itemSelectorHelper.fixedSelectorItemList = fixedItemList
```

**全部选择/取消:**

```kotlin
dslAdapter.itemSelectorHelper.selectorAll(SelectorParams(selector = !isSelectorAll))

```
**选择/取消:**

```kotlin
itemSelectorHelper.selector(
    SelectorParams(
        dslAdapterItem,
        select,
        notify = true,
        notifyItemChange = true,
        updateItemDepend = notifyUpdate
    )
)

```

**参数说明**

```kotlin
data class SelectorParams(
    //目标
    var item: DslAdapterItem? = null,
    //操作
    var selector: Boolean = true,
    //事件通知
    var notify: Boolean = true,

    /**
     * 是否需要回调[_itemSelectorChange]
     * [com.angcyo.dsladapter.ItemSelectorHelper._selectorInner]
     * */
    var notifyItemChange: Boolean = true,

    /**
     * 传递给
     * [com.angcyo.dsladapter.DslAdapterItem._itemSelectorChange]
     * */
    var updateItemDepend: Boolean = false,

    //额外自定义的扩展数据
    var extend: Any? = null,

    var _useFilterList: Boolean = true
)
```

![](https://gitee.com/angcyo/DslAdapter/raw/master/png/selector.png)

## 滑动选择

滑动选择使用`RecyclerView.OnItemTouchListener`实现.

需要组件`SlidingSelectorHelper`的支持.

开启也特别简单:

```kotlin
recyclerView.addOnItemTouchListener(SlidingSelectorHelper(applicationContext, dslAdapter))

```

在界面上, 长按. 即可进入`滑动选择模式`, 手指在`顶部or底部`会触发滑动.

离`顶部or底部`距离越近, 会智能滑动提速.

## 定向更新

重写属性`isItemInUpdateList`, 用来决定需要收到更新通知的`DslAdapterItem`

当触发`updateItemDepend`方法时, 所有需要更新的`DslAdapterItem` 会收到`onItemUpdateFrom`的函数回调.


## 群组功能

属性`itemGroups`, 方法`isItemInGroups`

为指定`DslAdapterItem`指定分组属性.

```
itemGroups = mutableListOf("group${i + 1}")
```

通过动态计算属性`itemGroupParams`,根据位置设置不同的背景

代码示例:

```kotlin
dslItem(R.layout.item_group_head) {
    itemIsGroupHead = true //启动分组折叠
    itemIsHover = false //关闭悬停
    itemGroups = mutableListOf("group${i + 1}")
    itemTopInsert = 10 * dpi

    onItemBindOverride = { itemHolder, itemPosition, adapterItem ->
        itemHolder.tv(R.id.fold_button).text =
            if (itemGroupExtend) "折叠 $itemPosition" else "展开 $itemPosition"

        itemHolder.tv(R.id.text_view).text = "分组${i + 1}"

        itemHolder.click(R.id.fold_button) {
            itemGroupExtend = !itemGroupExtend
        }

        //根据位置设置不同的背景
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
    }
}

```

在`线性布局`中的表现:

![](https://gitee.com/angcyo/DslAdapter/raw/master/png/group.png)

在`网格布局`中的表现 (具有`线性布局`的所有属性, 并具有`专属网格`属性):

![](https://gitee.com/angcyo/DslAdapter/raw/master/png/grid.png)

## 拖拽排序/侧滑删除

此功能需要`DragCallbackHelper`的支持.

```
DragCallbackHelper().attachToRecyclerView(recyclerView)
```

属性`itemDragFlag` `itemSwipeFlag` 可以控制激活的方向.


![](https://gitee.com/angcyo/DslAdapter/raw/master/png/drag1.png)

![](https://gitee.com/angcyo/DslAdapter/raw/master/png/swipe1.png)

## 基础功能 1.多类型使用方式

### 1.1 方式1(推荐)

继承 `DslAdapterItem`  重写 `itemLayoutId`  重写 `onItemBind` 搞定.

```kotlin
class DslTextItem : DslAdapterItem() {
    init {
        itemLayoutId = R.layout.item_text_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem)
        itemHolder.v<TextView>(R.id.text_view).text = "文本位置:$itemPosition"
    }
}
```

### 1.2 方式2

调用扩展方法`dslItem` 传入`layoutId`参数 实现 `onItemBindOverride` 搞定.

```kotlin
dslAdapter.dslItem(R.layout.item_text_layout) {
      onItemBindOverride = { itemHolder, itemPosition, _ ->
          itemHolder.v<TextView>(R.id.text_view).text = "文本位置:$itemPosition"
      }
  }
```

## 基础功能 2.情感图自定义

继承`DslAdapterStatusItem` 将对象设置给`DslAdapter`的`dslAdapterStatusItem`变量 即可.


**有一种很简单的方式:**
1. 继承`DslAdapterStatusItem`
2. 设置对应`情感状态`需要展示的`状态布局`即可.

```kotlin
open class CustomStatusItem : DslAdapterStatusItem() {
    init {
        itemStateLayoutMap[ADAPTER_STATUS_LOADING] = R.layout.base_loading_layout
        itemStateLayoutMap[ADAPTER_STATUS_ERROR] = R.layout.base_error_layout
        itemStateLayoutMap[ADAPTER_STATUS_EMPTY] = R.layout.base_empty_layout
    }
}

```

想要更多控制, 可以重写`_onBindStateLayout`方法

```kotlin
override fun _onBindStateLayout(itemHolder: DslViewHolder, state: Int) {
    super._onBindStateLayout(itemHolder, state)
    
    if (state == ADAPTER_STATUS_LOADING) {
        itemHolder.v<TextView>(R.id.text_view).text = "精彩即将呈现..."
    }
}
```

如果还想更多的控制, 可以重写`onItemBind`方法.

更多的一切, 都可以完全控制. 请查看`Demo`源码


## 基础功能 3.加载更多自定义
继承`DslLoadMoreItem` 将对象设置给`DslAdapter`的`dslLoadMoreItem`变量 即可.

和情感图的自定义`如出一辙`, 完全可以`如法炮制`, 这里不介绍了.

```kotlin
open class CustomLoadMoreItem : DslLoadMoreItem() {
    init {
        itemStateLayoutMap[ADAPTER_LOAD_NORMAL] = R.layout.base_loading_layout
        itemStateLayoutMap[ADAPTER_LOAD_LOADING] = R.layout.base_loading_layout
        itemStateLayoutMap[ADAPTER_LOAD_NO_MORE] = R.layout.base_no_more_layout
        itemStateLayoutMap[ADAPTER_LOAD_ERROR] = R.layout.base_error_layout
        itemStateLayoutMap[ADAPTER_LOAD_RETRY] = R.layout.base_error_layout
    }
}
```


# 最终使用代码


```kotlin
open fun renderAdapter(render: DslAdapter.() -> Unit) {
    dslAdapter.render()
}

```

```kotlin
renderAdapter {
    //设置情感图状态, loading
    setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_LOADING)

    /**
     * 扩展方式 追加[DslAdapterItem]
     * */
    dslItem(DslDemoItem()) {
        itemText = "情感图状态使用示例"
        onItemClick = {
            start(AdapterStatusActivity::class.java)
        }
        itemTopInsert = 2 * dpi //控制顶部分割线的高度
    }

    dslItem(DslDemoItem()) {
        itemText = "加载更多使用示例"
        onItemClick = {
            start(LoadMoreActivity::class.java)
        }
        itemTopInsert = 4 * dpi
    }

    /**
     * [invoke]运算符重载方式 追加[DslAdapterItem]
     * */
    DslDemoItem()() {
        itemText = "群组(线性布局)功能示例"
        onItemClick = {
            start(GroupDemoActivity::class.java)
        }
        itemTopInsert = 4 * dpi
    }

    DslDemoItem()() {
        itemText = "群组(网格布局)功能示例"
        onItemClick = {
            start(GroupGridDemoActivity::class.java)
        }
        itemTopInsert = 4 * dpi
    }

    DslDemoItem()() {
        itemText = "单选/多选示例"
        onItemClick = {
            start(SelectorDemoActivity::class.java)
        }
        itemTopInsert = 4 * dpi
    }

    DslDemoItem()() {
        itemText = "StaggeredGridLayout"
        onItemClick = {
            start(StaggeredGridLayoutActivity::class.java)
        }
        itemTopInsert = 4 * dpi
    }

    renderEmptyItem()

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

    //模拟网络延迟
    dslViewHolder.postDelay(1000) {
        //设置情感图状态, 正常
        setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_NONE)
    }
}
```

你也可以像下面这样:


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
            onItemBindOverride = { itemHolder, itemPosition, _ ->
                itemHolder.v<TextView>(R.id.text_view).text = "文本位置:$itemPosition"
            }
        }

        for (j in 0..0) {
            //2种使用item的方式, 喜欢哪种方式, 就用哪一种
            dslAdapter.dslImageItem()
            dslAdapter.dslItem(R.layout.item_image_layout) {
                onItemBindOverride = { itemHolder, itemPosition, _ ->
                    itemHolder.v<TextView>(R.id.text_view).text = "文本位置:$itemPosition"
                }
            }
        }
    }
}

```

更多代码, 请查看`Demo`源码.

# 依赖使用方法

## 1.JitPack

根 `build.gradle`

```kotlin
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```

```kotlin
dependencies {
    implementation 'com.github.angcyo:DslAdapter:xxx'
}
```

快照版本:(能够保持最新代码)

```kotlin
dependencies {
    implementation 'com.github.angcyo:DslAdapter:master-SNAPSHOT'
}
```

发布的版本号,可以在这里查看. [点击查看](https://github.com/angcyo/DslAdapter/tags)

## 2.下载源码

```kotlin
git clone https://github.com/angcyo/DslAdapter.git --depth=1
```

拷贝包 `com.angcyo.dsladapter.dsl` 下的`所有文件`到您的工程.

关于更新:

如果未修改源码, 可以直接覆盖.
如果修改了源码 可以使用 AS 提供的 `Compare with Clipboard` 功能 进行比对合并.

加油 你可以的!

# 备注

更多使用方式, 请下载源码`build`安装体验.

或者扫码安装

![](https://gitee.com/angcyo/DslAdapter/raw/master/png/qrcode1.png)

---
**群内有`各(pian)种(ni)各(jin)样(qun)`的大佬,等你来撩.**

# 联系作者

[点此QQ对话](http://wpa.qq.com/msgrd?v=3&uin=664738095&site=qq&menu=yes)  `该死的空格`    [点此快速加群](https://shang.qq.com/wpa/qunwpa?idkey=cbcf9a42faf2fe730b51004d33ac70863617e6999fce7daf43231f3cf2997460)

![](https://gitee.com/angcyo/res/raw/master/code/all_in1.jpg)

![](https://gitee.com/angcyo/res/raw/master/code/all_in2.jpg)
  
