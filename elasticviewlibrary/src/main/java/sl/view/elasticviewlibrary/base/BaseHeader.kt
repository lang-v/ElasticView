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

class BaseHeader(private val context: Context, offset: Int) : ElasticView.HeaderAdapter(offset) {
    private lateinit var view: View
    private val icon by lazy { view.findViewById<ImageView>(R.id.img) }
    private val progressBar by lazy { view.findViewById<ProgressBar>(R.id.progressBar) }
    private val text by lazy { view.findViewById<TextView>(R.id.text) }

    //icon的方向
    private val DIRECTION_DOWN = true
    private val DIRECTION_UP = false
    private var direction = DIRECTION_DOWN

    override fun getContentView(viewGroup: ViewGroup): View {
        view = LayoutInflater.from(context).inflate(R.layout.base_layout, viewGroup, false)
        return view
    }
    override fun scrollProgress(progress: Int) {}


    override fun pullToDo() {
        if (direction == DIRECTION_UP) {
            direction = DIRECTION_DOWN
        }
        icon.rotation = 0f
        text.text = "继续下拉更新"
        super.pullToDo()
    }

    override fun releaseToDo() {
        if (direction == DIRECTION_DOWN) {
            icon.rotation = 180F
            direction = DIRECTION_UP
        }
        text.text = "释放刷新"
        super.releaseToDo()
    }

    override fun onDo() {
        text.text = "正在更新"
        progressBar.visibility = View.VISIBLE
        icon.rotation = 0f
        icon.visibility = View.INVISIBLE
        super.onDo()
    }
    override fun overDo(msg:String) {
        text.text = msg
        progressBar.visibility = View.INVISIBLE
        icon.visibility = View.VISIBLE
        direction = DIRECTION_DOWN
        super.overDo(msg)
    }
}