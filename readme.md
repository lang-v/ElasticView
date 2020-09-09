## ElasticView
***

### Usage

 - MainActivity.kt
```kotlin
class MainActivity : AppCompatActivity(), ElasticView.OnEventListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //设置头部 尾部适配器在适配器中生成头部、底部View
        list.setHeaderAdapter(BaseHeader(this,200))
        list.setFooterAdapter(BaseFooter(this,200))
        //设置事件监听
        list.setOnElasticViewEventListener(this)
    }

    /**
     * onRefresh 头部刷新
     * onLoad    尾部加载
    **/
    override fun onRefresh() {
        Thread{
            Thread.sleep(1500)
            runOnUiThread{
                list.headerRefreshStop("完成")
            }
        }.start()
    }

    override fun onLoad() {
        Thread{
            Thread.sleep(1500)
            runOnUiThread{
                list.footerLoadStop("加载完毕")
            }
        }.start()
    }
}
```
***
  - R.layout.activity_main.xml
```xml
<!--    ElasticView继承于LinearLayout 通过设置orientation来控制滑动方向-->
    <sl.view.elasticviewlibrary.ElasticView
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

    </sl.view.elasticviewlibrary.ElasticView>
```

***

