package com.jianguo.bilibili.note.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jianguo.bilibili.note.bean.CardItemBean
import com.jianguo.bilibili.note.util.BiliCommentUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random


/**
 * 类别：控制评论页面下拉刷新的显示
 * 坚果
 * qq 1051888852
 * create 2023/4/11 18:56
 */
class FreshViewModel : ViewModel(){

    var commentCardViewModel : CommentCardViewModel? = null
    private val _isRefreshing = MutableStateFlow(false) //创建一个Flow流
    private var colorPanelPosition = Random.nextInt(0, 4)
    private val colorPanel = listOf(
        Color(0xFFFABD25),
        Color(0xFF3949AB),
        Color(0xFFFF80AB),
        Color(0xFFD32F2F),


        /*Color.LightGray,
        Color(0xFF8BC34A),
        Color(0xFFFF8A80),
        Color(0xFF7CB342),
        Color(0xFF00897B),
        Color.DarkGray,*/
    )
    val isRefreshing : StateFlow<Boolean>
      get() = _isRefreshing.asStateFlow()
    var background by mutableStateOf(colorPanel[colorPanelPosition])

    var bvid = "BV17d4y1g7fu" //视频bv号
    var oid = 0.toLong()
    var next = 0 //页码变量
    var mode = 1 //排序方式，3按热度，2按时间
    var haveNoteComment = true

    var lastItem = 50.dp

    var canScroll = MutableStateFlow(true)

    //下拉刷新的逻辑
    fun refresh(
        /*//首次进入评论页面不改变背景颜色
        isFirstRefresh : Boolean,
        //非下拉刷新时不改变背景颜色
        isDropDownRefresh : Boolean*/
        changeBackgroundColor : Boolean //true则改变背景色
    ){
        /*Log.e(
            "GetVideoMessage",
            next.toString()
        )*/
        canScroll.value = false
        //使用viewModelScope.launch开启一个协程作用域
        viewModelScope.launch {
            _isRefreshing.emit(true) //向flow流中添加一个true值，代表正在刷新
            //delay(1000) //协程中延时一秒模拟网络请求
            if(changeBackgroundColor){
                /*//只有首次进入评论页面和非下拉刷新时候不改变背景颜色
                background = colorPanel[colorPanelPosition]*/
                //-----------上面是旧注释-----------
                //true则改背景色
                colorPanelPosition =
                    if(colorPanelPosition==3) 0 else ++colorPanelPosition
                background = colorPanel[colorPanelPosition]
                commentCardViewModel?.commentCardFlow?.value  = emptyList()
                //根据mode类型给next赋值
                /*next = if(mode == 1){
                    1 //别动，动了就炸
                }else{
                    0
                }*/
                next = 1
                haveNoteComment = true
            }else{
                /*//下拉刷新时改变背景颜色
                //使用评论页面的下拉刷新，改变背景颜色
                //控制背景色，按集合顺序依次显示
                colorPanelPosition =
                    if(colorPanelPosition==3) 0 else ++colorPanelPosition
                background = colorPanel[colorPanelPosition]*/
                //-----------上面是旧注释-----------
                //false则不改背景色
                background = colorPanel[colorPanelPosition]
                next++
            }
            Log.e(
                "GetVideoMessage",
                next.toString()
            )
            /**
             * 注意，接下来哥要开始秀操作了！
             */
            // 既然无法直接获取到CommentCardViewModel，那就定义为一个变量从外部传进来使用
            // 1.获取到CommentCardViewModel的commentCardFlow流，也就是评论数据集
            val commentCardFlow = commentCardViewModel?.commentCardFlow
            // 2.调用getComments方法返回一个评论集合
            val comments : MutableList<CardItemBean> = if(oid.toInt() == 0){
                //BV号模式
                BiliCommentUtil().getCommentsByBvid(
                    bvid = bvid,
                    next = next,
                    mode = mode
                )
            }else{
                //链接模式
                BiliCommentUtil().getCommentsByOid(
                    oid = oid,
                    next = next,
                    mode = mode
                )
            }
            //评论集合判断，后面设置是否仅获取笔记内容要用。
            if(comments.isNotEmpty()){
                //none:如果没有元素与给定谓词匹配，则返回true。这里意思是如果没有笔记评论
                //如果评论集合不为空但是没有笔记评论，就重新获取，直到获取到笔记评论或没有评论为止
                /*while(comments.none { it.noteImageList.isNotEmpty() }){
                    //如果没有笔记评论就重新获取，直到有为止
                    next++
                    comments = BiliCommentUtil().getComments(
                        bvid = bvid,
                        next = next,
                    )
                }*/
                lastItem = 0.dp
            }else{
                //没有评论
                haveNoteComment = false
                lastItem = 50.dp
            }
            // 3.合并初始评论数据集和后加入的评论数据集，默认为空
            val commentCardFlowMutableListPlus =
                commentCardFlow?.value?.plus(
                    //评论集合判断，后面设置是否仅获取笔记内容要用。
                    /*comments.filter {
                        //把笔记图片评论筛选出来
                        it.noteImageList.isNotEmpty()
                    }*/
                    comments
                )
                    ?:
                    emptyList()
            // 4.发出流，替换原有的评论集，也就是【原评论+刷新出来的新评论】构成的新评论数据集
            commentCardFlow?.emit(
                //BiliCommentUtil().getComments("BV17d4y1g7fu")
                commentCardFlowMutableListPlus
            )
            _isRefreshing.emit(false) //向flow流中添加一个false值，代表刷新结束
            delay(200)
            canScroll.value = true
        }
    }
}