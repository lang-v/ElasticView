package sl.view.elasticviewlibrary.base

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import sl.view.elasticviewlibrary.ElasticView
import sl.view.elasticviewlibrary.R

class BaseFooter(private val context: Context, offset:Int):ElasticView.FooterAdapter(offset) {
    private lateinit var view: View
    private val icon by lazy { view.findViewById<ImageView>(R.id.footerImg) }
    private val progressBar by lazy { view.findViewById<ProgressBar>(R.id.footerProgressBar) }
    private val text by lazy { view.findViewById<TextView>(R.id.footerText) }

    //icon的方向
    private val DIRECTION_DOWN = true
    private val DIRECTION_UP = false
    private var direction = DIRECTION_DOWN

    override fun getContentView(viewGroup: ViewGroup): View {
        view = LayoutInflater.from(context).inflate(R.layout.base_layout_footer, viewGroup, false)
        return view
    }
    override fun scrollProgress(progress: Int) {}


    override fun pullToDo() {
        if (direction == DIRECTION_DOWN) {
            direction = DIRECTION_UP
        }
        icon.rotation = 180f
        text.text = "继续上拉加载更多"
        super.pullToDo()
    }

    override fun releaseToDo() {
        if (direction == DIRECTION_UP) {
            icon.rotation = 0F
            direction = DIRECTION_DOWN
        }
        text.text = "释放开始加载"
        super.releaseToDo()
    }

    override fun onDo() {
        text.text = "正在加载中"
        progressBar.visibility = View.VISIBLE
        icon.rotation = 180f
        icon.visibility = View.INVISIBLE
        super.onDo()
    }
    override fun overDo(msg:String) {
        text.text = msg
        progressBar.visibility = View.INVISIBLE
        icon.visibility = View.VISIBLE
        direction = DIRECTION_UP
        super.overDo(msg)
    }
}