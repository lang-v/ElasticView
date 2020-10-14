## ElasticView
***
### 介绍

我们所用到的列表（ListView、RecyclerView、ScrollView、NestedScrollView等等），都是日常开发中经常会使用到的，但是这些列表类在滑动到底部或者顶部时并没有很好的动画效果，只有一层波纹效果，会让我们的App瞬间掉了一个档次。如果列表滑动到底部或者顶部还能够继续滑动并且在松开手指后能够将视图弹回来，那么效果就会好狠多了。要实现这个功能就需要了解Google的[NestedScrollingParent](https://developer.android.google.cn/reference/androidx/core/view/NestedScrollingParent)、[NestedScrollingChild](https://developer.android.google.cn/reference/androidx/core/view/NestedScrollingChild),另外这个嵌套滑动套件已经更新到了[NestedScrollingParent3](https://developer.android.google.cn/reference/androidx/core/view/NestedScrollingParent3)、[NestedScrollingChild3](https://developer.android.google.cn/reference/androidx/core/view/NestedScrollingChild3)。这里用到的是NestedScrollingparent2。
通过ElasticView可以做到列表上下滑动的弹性效果，还可以设置上拉加载更多、下拉刷新，并且加载动画可以通过设置Adapter来自定义动画和动画播放顺序。代码很短，适合新手看一看，欢迎大家提出建议。
此外这个库提供了基础的头部适配器（BaseHeader）和尾部适配器（BaseFooter），实现了简单的动画效果，可以根据这两个类自定义适配器。

演示安装包在目录ElasticLayoutDemo下

### 用法

 - 导入ElasticView
 [![](https://www.jitpack.io/v/lang-v/ElasticView.svg)](https://www.jitpack.io/#lang-v/ElasticView)
 
Step 1. Add it in your root build.gradle at the end of repositories:
```
    allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```
	

Step 2. Add the dependency

```
	dependencies {
	        implementation 'com.github.lang-v:ElasticView:1.0.2'
	}
```

*** 

 - MainActivity.kt
```kotlin
class MainActivity : AppCompatActivity(), ElasticView.OnEventListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val list:ElasticView = findViewById(R.id.list)// list 就是ElasticView控件实例
        //设置头部 尾部适配器在适配器中生成头部、底部View
        list.setHeaderAdapter(BaseHeader(this,200))
        list.setFooterAdapter(BaseFooter(this,200))//如果没有设置adapter 那么这个列表就只是单纯的弹性视图
        list.setDamping(0.3,true)//设置阻尼系数，是否递减
        list.setAnimTime(300)//设置弹回动画时间 0.3秒
        //设置事件监听
        list.setOnElasticViewEventListener(this)
        list.isRefreshing = true//手动调用刷新
        list.isRefreshing = false//停止刷新
    
        //并没有实现手动调用上拉加载
        list.isLoading = false//停止上拉加载
    }

    /**
     * onRefresh 头部刷新
     * onLoad    尾部加载
    **/
    override fun onRefresh() {
        Thread{
            Thread.sleep(1500)
            runOnUiThread{
                list.isRefreshing = false//停止刷新
            }
        }.start()
    }

    override fun onLoad() {
        Thread{
            Thread.sleep(1500)
            runOnUiThread{
                list.isLoading = false//停止加载
            }
        }.start()
    }
}
```
***
  - R.layout.activity_main.xml
```xml
<!--    ElasticView继承于LinearLayout 通过设置orientation来控制滑动方向-->
    <sl.view.elasticviewlibrary.ElasticLayout
        android:id="@+id/list"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical">

<!--    ElasticView 需要嵌套实现了NestedScroll套件的View 作为列表-->
<!--    不一定需要NestedScrollView 比如RecyclerView也可以-->
        <androidx.core.widget.NestedScrollView
            android:layout_width="match_parent"
            android:layout_height="wrap_content">
<!--           列表内容 -->
        </androidx.core.widget.NestedScrollView>

    </sl.view.elasticviewlibrary.ElasticLayout>
```

***

