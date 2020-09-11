package sl.view.elasticviewlibrary

/**
 * ElasticView 布局内仅有一个View。
 * headerView和footerView用户设置适配器时添加
 * 拦截所有触摸事件不靠谱，太多不确定因素
 * 还是用NestedScrollParent
 * 设置orientation以确定布局纵横滑动
 */
import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.NestedScrollingParent2
import androidx.core.view.ViewCompat
import kotlin.math.abs

class ElasticView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    LinearLayout(context, attrs, defStyleAttr), NestedScrollingParent2 {
    private val TAG = "ElasticView"

    //header
    private var headerAdapter: HeaderAdapter? = null

    //footer
    private var footerAdapter: FooterAdapter? = null

    //弹回的动画时间
    private var animTimeLong = 200L
    private var animTimeShort = animTimeLong / 2

    //阻尼系数
    private var damping = 0.5f
    private var dampingTemp = 0.5f

    //阻尼系数是否需要逐减
    private var isDecrement = true

    //标记布局是否移动
    private var isMove = false

    //标记是否出fling状态
    private var isFling = false

    //当前是否允许fling
    private var allowFling = true

//    //弹回动画锁
//    private val lock = ReentrantLock()

    //事件监听
    private var listener: OnEventListener? = null

    //加载完成的回调
    private val refreshCallBack = object : PullCallBack {
        override fun over() {
            headerAdapter!!.isDoing = false
            postDelayed({
                springBack(getScrollOffset(), 300)
            }, 300)
        }
    }
    private val loadCallBack = object : PullCallBack {
        override fun over() {
            footerAdapter!!.isDoing = false
            postDelayed({
                springBack(getScrollOffset(), 300)
            }, 300)
        }
    }

    init {
        //禁止裁剪布局,使得在页面外的view依然能显示
        this.clipChildren = false
        this.clipToPadding = false
    }


    //处理嵌套滑动
    override fun onNestedScrollAccepted(child: View, target: View, axes: Int, type: Int) {}

    //此处无须判断child类型
    override fun onStartNestedScroll(child: View, target: View, axes: Int, type: Int): Boolean {
        //判断当前滑动方向是否与ElasticView设置的滑动方向一致 一致则返回true
        return (axes and when (orientation) {
            HORIZONTAL -> ViewCompat.SCROLL_AXIS_HORIZONTAL
            else -> ViewCompat.SCROLL_AXIS_VERTICAL
        }) != 0
    }


    /**
     * @param consumed 记录parent消耗的距离，consumed[0]-->X  [1]-->y
     * 如果parent消耗完，那么child就不会继续处理了
     */
    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        val scrollOffset = getScrollOffset()
//        判断是否滑动到边界,滑动到边界的情况交给parent处理
        if (canScroll(target, dx = dx, dy = dy)) {
            if (type == ViewCompat.TYPE_TOUCH) {
                isMove = true
                allowFling = false
            } else if (type == ViewCompat.TYPE_NON_TOUCH) {
                /**
                 * 这个判断很 重要
                 */
                if (!allowFling)return
                isFling = true
                allowFling = true
                if (abs(scrollOffset) >= 100 || abs(dx+dy)*dampingTemp < 10) {
                    allowFling = false//禁止fling
                    springBack(scrollOffset, animTimeShort)
                    return
                }
            }
            //吃掉所有位移
            consumed[0] = dx
            consumed[1] = dy
            scrollBy((dx * dampingTemp).toInt(), (dy * dampingTemp).toInt())
            calcDamping()
        } else {//此处有两种情况 一是未到边界的滑动，二是已经移动过布局，但是现在开始反向滑动了
            if (scrollOffset != 0) {
                val temp = if (orientation == VERTICAL) dy * damping else dx * damping
                //防止越界，如果数据越界就设为边界值
                val offset =
                    if (scrollOffset <= 0 && temp > -scrollOffset) -scrollOffset
                    else if (scrollOffset >= 0 && temp < -scrollOffset) -scrollOffset
                    else temp.toInt()
                scrollBy(offset, offset)
                calcDamping()
                consumed[0] = dx
                consumed[1] = dy
            }
        }
    }

    override fun onNestedScroll(
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int
    ) {}



    //private var isFirstStopScrollOnTouch = false
    /**
     * 子view停止滑动
     * 这个方法在整个过程中会被调用3次
     * 滑动之前 手指离开屏幕 fling结束
     * 手指离开屏幕时是滑动由drag变成fling开始惯性滑动
     */
    override fun onStopNestedScroll(target: View, type: Int) {
        //这是最后一次调用此方法
        if (type == ViewCompat.TYPE_NON_TOUCH) {
            allowFling = true
            isFling = false
            return
        }
        val scrollOffset = getScrollOffset()
        if (!isMove) return
        isMove = false
        //达到加载条件
        if (headerAdapter != null && scrollOffset < 0 && scrollOffset <= -headerAdapter!!.offset) {
            springBack(headerAdapter!!.offset + scrollOffset, animTimeShort)
            if (isLoading()) return
            headerAdapter!!.isDoing = true
            headerAdapter!!.onDo()
            listener?.onRefresh()
            return
        }
        //达到加载条件
        if (footerAdapter != null && scrollOffset > 0 && scrollOffset >= footerAdapter!!.offset) {
            springBack(scrollOffset - footerAdapter!!.offset, animTimeShort)
            if (isLoading()) return
            footerAdapter!!.isDoing = true
            footerAdapter!!.onDo()
            listener?.onLoad()
            return
        }
        springBack(scrollOffset, animTimeLong)
    }

    override fun scrollBy(x: Int, y: Int) {
        //根据布局选择移动水平垂直
        if (orientation == VERTICAL) {
            super.scrollBy(0, y)
        } else {
            super.scrollBy(x, 0)
        }
        if (isLoading()) return
        val scrollOffset = getScrollOffset()
        //更新控件header，footer状态
        if (scrollOffset < 0) {
            if (headerAdapter == null) return
            if (-scrollOffset <= headerAdapter!!.offset) {
                headerAdapter!!.scrollProgress(-scrollOffset)
                headerAdapter!!.pullToDo()
            } else {
                headerAdapter!!.releaseToDo()
            }
        } else {
            if (footerAdapter == null) return
            if (scrollOffset <= footerAdapter!!.offset) {
                footerAdapter!!.scrollProgress(-scrollOffset)
                footerAdapter!!.pullToDo()
            } else {
                footerAdapter!!.releaseToDo()
            }
        }
    }

    /**
     * 根据布局orientation属性判断横向纵向滑动是否触及边缘
     */
    private fun canScroll(child: View, direction: Int = 0, dx: Int = 0, dy: Int = 0): Boolean {
        //canScrollVertically(1)滑动到底部返回false，canScrollVertically(-1)滑动到顶部返回false
        //canScrollHorizontally(1)滑动到右侧底部返回false，canScrollHorizontally(-1)滑动到左侧顶部返回false
        return if (orientation == VERTICAL) {
            if (dy == 0)
                !child.canScrollVertically(direction)
            else
                !child.canScrollVertically(dy)
        } else {
            if (dx == 0)
                !child.canScrollHorizontally(direction)
            else
                !child.canScrollHorizontally(dx)
        }
    }


    /**
     * 调用头部刷新
     */
    fun headerRefresh() {
        if (headerAdapter == null) return
        if (!isLoading()) {
            scrollBy(0, -headerAdapter!!.offset)
            headerAdapter!!.isDoing = true
            headerAdapter!!.onDo()
            listener?.onRefresh()
        }
    }

    /**
     * 停止刷新动画
     */
    fun headerRefreshStop(msg: String) {
        if (headerAdapter == null) return
        headerAdapter!!.overDo(msg)
    }

    /**
     * 调用底部加载
     */
    fun footerLoad() {
        if (footerAdapter == null) return
        if (!isLoading()) {
            scrollBy(0, footerAdapter!!.offset)
            footerAdapter!!.isDoing = true
            footerAdapter!!.onDo()
            listener?.onLoad()
        }
    }

    /**
     * 停止加载动画
     */
    fun footerLoadStop(msg: String) {
        if (footerAdapter == null) return
        footerAdapter!!.overDo(msg)
    }

    /**
     * 获取X或者Y的滚动值
     */
    private fun getScrollOffset(): Int {
        val offset = if (orientation == VERTICAL)
            scrollY
        else
            scrollX
        return offset
    }

    //判断当前是否处于刷新加载状态
    private fun isLoading(): Boolean {
        return ((headerAdapter != null && headerAdapter!!.isDoing)
                || (footerAdapter != null && footerAdapter!!.isDoing))
    }

    /**
     * 设置滑动阻尼系数
     * @param isDecrement 是否随距离增大阻尼系数
     */
    fun setDamping(damping: Float, isDecrement: Boolean = true) {
        this.damping = damping
        this.dampingTemp = damping
        this.isDecrement = isDecrement
    }

    /**
     * @param time 设置弹回动画的执行时间
     */
    fun setAnimTime(time: Long) {
        animTimeLong = time
    }

    //计算阻尼变化
    private fun calcDamping() {
        if (!isDecrement)return
        //val offset = abs(getScrollXY()).toDouble()
        //双曲正切函数(e^x-e^(-x))/(e^x+e^(-x)),随着x递增，y从零开始增加无限趋近于0
        //dampingTemp = damping * (1-((exp(offset) - exp(-offset))/(exp(offset) + exp(-offset)))).toFloat()
        var count = (abs(getScrollOffset()) / animTimeShort).toInt()
        if (count == 0) {
            count = 1
        }
        dampingTemp = damping / count
    }

    private var animator: ValueAnimator? = null

    //弹回动画
    @Synchronized
    private fun springBack(offset: Int, animTime: Long) {
        animator = if (animator != null) {
            animator!!.cancel()
            val tmp =-
                if (isLoading()) {
                    getScrollOffset() +
                            if (headerAdapter != null &&  headerAdapter!!.isDoing) headerAdapter!!.offset
                            else if (footerAdapter != null && footerAdapter!!.isDoing) -footerAdapter!!.offset
                            else -offset
                }else
                    offset
            ValueAnimator.ofInt(0, tmp)
        } else
            ValueAnimator.ofInt(0, -offset)
        animator!!.duration = animTime
        val scrollOffset = getScrollOffset()
        animator!!.addUpdateListener { animation ->
            if (orientation == VERTICAL)
                scrollTo(scrollX, scrollOffset + animation.animatedValue as Int)
            else
                scrollTo(scrollOffset + animation.animatedValue as Int, scrollY)
        }
        animator!!.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {}

            //只有在动画加载完成后才调用刷新
            override fun onAnimationEnd(animation: Animator?) {
                animator = null
//                lock.unlock()
            }

            //动画取消不执行
            override fun onAnimationCancel(animation: Animator?) {
                animator = null
//                lock.unlock()
            }

            override fun onAnimationStart(animation: Animator?) {}
        })
        post { animator?.start() }
    }

    fun setHeaderAdapter(adapter: HeaderAdapter) {
        headerAdapter = adapter
        headerAdapter!!.setPullCallBack(refreshCallBack)
        addHeaderView(headerAdapter!!.getContentView(this))
    }

    private fun addHeaderView(view: View) {
        if (childCount >= 3) {
            throw IllegalArgumentException("ElasticView only support three childViews")
        }
        val layoutParams =
            if (orientation == VERTICAL) LayoutParams(
                LayoutParams.MATCH_PARENT,
                headerAdapter!!.offset
            )
            else LayoutParams(headerAdapter!!.offset, LayoutParams.MATCH_PARENT)

        if (orientation == VERTICAL)
            layoutParams.topMargin = -headerAdapter!!.offset
        else
            layoutParams.leftMargin = -headerAdapter!!.offset
        post {
            addView(view, 0, layoutParams)//最底层
        }
    }

    fun setFooterAdapter(adapter: FooterAdapter) {
        footerAdapter = adapter
        footerAdapter!!.setPullCallBack(loadCallBack)
        addFooterView(footerAdapter!!.getContentView(this))
    }

    //相比于HeaderView，footerView处于middleView的最后，无须设置marginTop
    private fun addFooterView(view: View) {
        if (childCount == 3) {
            throw IllegalArgumentException("ElasticView only support three childViews")
        }
        val layoutParams =
            if (orientation == VERTICAL) LayoutParams(
                LayoutParams.MATCH_PARENT,
                footerAdapter!!.offset
            )
            else LayoutParams(footerAdapter!!.offset, LayoutParams.MATCH_PARENT)

        if (orientation == VERTICAL)
            layoutParams.bottomMargin = -footerAdapter!!.offset
        else
            layoutParams.rightMargin = -footerAdapter!!.offset
        post {
            addView(view, childCount,layoutParams)
        }
    }

    /**
     * 设置事件监听 监听刷新 加载
     */
    fun setOnElasticViewEventListener(listener: OnEventListener) {
        this.listener = listener
    }

    /**
     * 适配器基类   Header Footer都是派生于它
     */
    abstract class BaseAdapter(val offset: Int) {
        var isDoing = false
        lateinit var callBack: PullCallBack

        /**在这里生成一个view，这个view将会被放置到列表主体的尾部*/
        abstract fun getContentView(viewGroup: ViewGroup): View

        /**设置回调,通过它通知ElasticView加载完成，执行完成动画*/
        fun setPullCallBack(callBack: PullCallBack) {
            this.callBack = callBack
        }

        /**
         * 滑动进度
         * @param progress in 0 .. offset
         */
        open fun scrollProgress(progress: Int) {}

        /**继续滑动加载*/
        open fun pullToDo() {}

        /**释放加载*/
        open fun releaseToDo() {}

        /**加载中*/
        open fun onDo() {
            isDoing = true
        }

        /**加载完成,通知ElasticView播放完成动画*/
        open fun overDo(msg: String) {
            callBack.over()
            isDoing = false
        }

    }

    abstract class HeaderAdapter(offset: Int) : BaseAdapter(offset) {}
    abstract class FooterAdapter(offset: Int) : BaseAdapter(offset) {}

    interface PullCallBack {
        fun over()
    }


    interface OnEventListener {
        //下拉刷新
        fun onRefresh()

        //上拉加载
        fun onLoad()
    }

}