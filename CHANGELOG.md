# 2021-6-29

`2.6.2`

- 修复`DragCallbackHelper`和`SwipeMenuHelper`的手势冲突

# 2020-12-23

`2.6.0`

- 调整`分组边界算法`, `ItemGroupParams`参数将受到影响

# 2020-10-29

`2.5.5`

- 修复`updateData`时索引计算
- 新增`UpdateDataConfig`的`updateSize`回调配置

# 2020-7-6

`2.5.1`

- 修复拖拽排序, 数据源的问题(头, 中, 尾)
+ 新增拖拽排序后的回调
+ 新增滑动删除后的回调
- 调整一些代码结构

# 2020-6-24

`2.5.0`

- 新增`Page`数据刷新, 数据加载更多控制
- 新增`UpdateDataConfig`, 扩展方法`loadDataEnd`支持页面列表数据加载, 自动处理刷新/加载更多/异常/之间的切换.
- 新增一些常用扩展方法
- 调整默认[itemClick]加入节流处理, 防止暴力点击.
- 调整了一些变量名称
- 调整了一些注释
- 调整了一些细节

# 2020-5-13

`2.4.2`

- 新增`侧滑菜单`
- 优化侧滑流畅度
- 优化侧滑体验

# 2020-5-9

`2.3.0`

- 调整`Diff`相关方法的参数
- 调整`thisAreItemsTheSame`判断逻辑

# 2020-4-28

`2.2.7`

- 添加RecyclerView快速设置DslAdapter的扩展方法
- 新增`BatchLoad`批量过滤加载
- 修复`updateOrInsertItem`方法临界值判断
- 全局可配的`shakeDelay`默认时长

# 2020-4-8

`2.2.3`

- 新增 定向更新(不存在则插入)指定item的方法

# 2020-4-8

`2.2.2`

- 重命名`FilterInterceptor->IFilterInterceptor`
- 重命名`FilterAfterInterceptor->IFilterAfterInterceptor`
- `IFilterInterceptor` `IFilterAfterInterceptor` 支持 `enable` 属性


# 2020-04-07

`2.2.1`

- 提供一些`update`方法, 用于轻量差异更新相同类型列表数据.
- 新增数据加载示例Demo

# 2020-03-28

`2.2.0`

- 修复一些已知位置, 调整一些命名.
- 新增一些常用的扩展方法, 一些属性修改回调
- `DslAdapterItem` 新增常用属性 `itemWidth/Height` `itemMinWidth/Height` `itemPadding` `itemBackgroundDrawable` `itemEnable`
- `DslDataFilter` 共享线程池
- 新增`updateData`扩展方法, 方便将`List`数据, 渲染进`DslAdapter`
- 新增`FilterAfterInterceptor`子类`MaxItemCountFilterAfterInterceptor`, 用于控制`RecycleView`最大显示数量.

# 2020-3-12

`2.1.0`

- `DslAdapterItem` 支持 `LifecycleOwner`, 提供`STARTED` `RESUMED` `DESTROYED` 3种状态
- `dispatchUpdates`支持`list`
- 调整了一些命名规则
- 全面支持了`payload`的更新方式
- 加入了一些更实用的扩展函数
- `DslDataFilter`重构了`抖动``节流`的处理方法
- `DslDataFilter` 拆分`前置` `中置` `后置` `过滤`拦截器
- `DslDataFilter` 加入了`过滤后`拦截器

# 2020-02-05

`2.0.0`

注意:本次更新内容有点多, `API`调整过大, 有问题欢迎联系我!

- 支持`payloads`更新方式
- 100% `kotlin` 代码(`DslViewHolder` `L`)
- 调整`DslAdapterStatusItem` `DslLoadMoreItem`实现方式 
- 调整`DslDataFilter`逻辑
- 调整其他代码结构

# 2020-1-2

`1.4.1`

- `min sdk` 调整为 12
- 修复`DslAdapterItem` `itemHidden` 属性不生效的问题
- 移除一些`open`修饰