package com.jianguo.bilibili.note.navigation

import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cc.shinichi.library.ImagePreview
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jianguo.bilibili.note.R
import com.jianguo.bilibili.note.bean.CardItemBean
import com.jianguo.bilibili.note.viewmodel.CommentCardViewModel
import com.jianguo.bilibili.note.viewmodel.FreshViewModel
import kotlinx.coroutines.delay


/**
 * 类别：评论卡片页面
 * 坚果
 * qq 1051888852
 * create 2023/4/9 16:48
 */

//评论页面
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CommentPageComposable(navController: NavController, bvid: String) {
    //获取评论下拉刷新viewModel
    val freshViewModel: FreshViewModel = viewModel()
    if(bvid.contains("BV")){
        //设置请求的bv号
        freshViewModel.bvid = bvid
    }else{
        freshViewModel.bvid = ""
        freshViewModel.oid = bvid.toLong()
    }
    //获取评论卡片viewModel
    val commentCardViewModel: CommentCardViewModel = viewModel()
    //将评论卡片viewModel赋值给下拉刷新viewModel以进行赋值操作
    freshViewModel.commentCardViewModel = commentCardViewModel
    //将流转化为状态赋值给下拉刷新的状态
    val isRefreshing by freshViewModel.isRefreshing.collectAsState()
    //下拉动画过渡效果
    val background by animateColorAsState(
        targetValue = freshViewModel.background,
        animationSpec = tween(1000), label = ""
    )
    //下拉刷新状态
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            freshViewModel.refresh(
                true
            )
        },
        refreshThreshold = 50.dp
    )
    //控制最后一个卡片（没有更多评论）的透明度达到控制显隐
    val lastItem by remember {
        mutableStateOf(freshViewModel.lastItem)
    }
    /*remember {
        //使用remember方法避免此方法因重组再次执行。（只在进入评论页面时候执行一次)
        freshViewModel.refresh(
            false
        )
        false
    }*/
    //获取评论卡片viewModel
    //val commentCardViewModel : CommentCardViewModel = viewModel()
    //1.获取评论卡片viewModel中的flow评论数据流
    //2.flow流由CardItemBean的list集合组成
    //3.每个集合代表更新的评论卡片
    val commentCardViewFlow = commentCardViewModel.commentCardFlow
    //将数据流转化为state状态(List<CardItemBean>)
    /**
     * 这里将commentCardFlow单独从ViewModel中获取出来并赋值给列表，
     * 前面也就可以将CommentCardViewModel赋值给FreshViewModel
     * 来达到在FreshViewModel中对CommentCardViewModel的flow进行操作。
     * 向viewModel中的属性赋值达到调用其他属性的效果亲测可行，目前最有效的方法。
     */
    val commentCardViewFlowListState by
    commentCardViewFlow.collectAsState()
    //新建一个状态判断是否滑动到底部
    /*var isLast by remember {
        mutableStateOf(commentCardViewModel.isLast)
    }*/
    //数据加载时滑动会导致列表卡在刷新前的一项不可向下滑动，很影响手感
    //在viewModel中定义一个变量控制滑动，加载中不可滑动，加载后延迟100毫秒打开滑动
    //用collectAsState()将canScroll状态flow变量转化为一个普通状态并赋值给列表
    val canScroll = freshViewModel.canScroll.collectAsState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            //.statusBarsPadding()
            .pullRefresh(pullRefreshState)  //下拉刷新
            .background(background),  //背景随下拉刷新改变
        //contentAlignment = Alignment.BottomEnd
    ) {
        val lazyListState = rememberLazyListState() //监听滑动状态
        val context = LocalContext.current
        //使用items构建一个列表页面，并留出沉浸式系统栏的空间
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            state = lazyListState,
            contentPadding = PaddingValues(8.dp),
            //控制列表是否可滑动
            userScrollEnabled = canScroll.value
        ) {
            //1.构建卡片列表，将转化为state状态的flow传入
            //2.若flow数量发生改变，则触发items的重组
            //3.items会解析flowState状态
            //将List<CardItemBean>解析为CardItemBean
            //4.ForEach遍历一样将解析的CardItemBean传入卡片中
            //评论卡片函数CommentItem会对CardItemBean再解析
            //最后构成一个不变的独立的评论卡片。
            /*lastCardAlpha = if(commentCardViewFlowListState.isNotEmpty()){
                1F
            }else{
                0F
            }*/
            itemsIndexed(commentCardViewFlowListState) { index, item ->
                //val havePhoto = Random.nextBoolean() //随机生成false和true其中一个值
                //模拟笔记图片
                //val photoNum = Random.nextInt(0, 10) // 生成 0~9 之间的整数
                CommentItem(item)
                //提前加载数据
                /*LaunchedEffect(commentCardViewFlowListState.size) {
                    isLast = commentCardViewFlowListState.size - index < 2
                    if(isLast){
                        if (!freshViewModel.haveNoteComment) {
                            *//*freshViewModel
                                .refresh(false)
                            //Log.e("GetVideoMessage","first")
                        } else {*//*
                            Toast.makeText(context, "没有更多评论了", Toast.LENGTH_SHORT).show()
                        }
                    }
                }*/
            }
            //滑动到底部再加载~提前加载bug一堆不建议用
            /*if (isLast) {*/
                item {
                    if (freshViewModel.haveNoteComment) {
                        Box(
                            //shape = RoundedCornerShape(2.dp), 圆角大小
                            modifier = Modifier
                                .fillMaxWidth()
                                //.clickable(onClick = {})
                                .height(lastItem),
                            contentAlignment = Alignment.Center
                            //.alpha(1F),
                        ){
                            CircularProgressIndicator(color = Color.White)
                            LaunchedEffect(Unit) {
                                //协程导致数据添加次序不一致，会对数据呈现有影响，延迟400毫秒即可
                                delay(400)
                                freshViewModel
                                    .refresh(false)
                            }
                        }
                    }else{
                        //Toast.makeText(context, "没有更多评论了", Toast.LENGTH_SHORT).show()
                        Card(
                            //shape = RoundedCornerShape(2.dp), 圆角大小
                            elevation = 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                //.clickable(onClick = {})
                                .height(lastItem),
                            //.alpha(1F),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    //评论内容
                                    Text(text = "没有更多评论咯~ (゜-゜)つロ")
                                }
                            }
                        }
                    }
                }

        }
        Row(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 28.dp, end = 16.dp)
                .navigationBarsPadding()
        ) {
            var hotTimeButtonText by remember {
                mutableStateOf("按时间")
            }
            //按热度排序/按时间排序按钮
            ExtendedFloatingActionButton(
                onClick = {
                          if(hotTimeButtonText == "按热度"){
                              hotTimeButtonText = "按时间"
                              freshViewModel.mode = 1 //热度1，时间0
                              Toast.makeText(context, "当前为按热度排序", Toast.LENGTH_SHORT).show()
                          }else{
                              hotTimeButtonText = "按热度"
                              freshViewModel.mode = 0
                              Toast.makeText(context, "当前为按时间排序", Toast.LENGTH_SHORT).show()
                          }
                    freshViewModel.refresh(
                        true
                    )
                },
                //给悬浮按钮留出系统栏的空间
                /*modifier = Modifier
                    .padding(28.dp)
                    .navigationBarsPadding(),*/
                //icon = { Icon(Icons.Filled.Person, null) },
                text = { Text(text = hotTimeButtonText) },
                backgroundColor = Color(0xFF54F0AC),
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            )
            Spacer(modifier = Modifier.padding(horizontal = 6.dp))
            //返回按钮
            ExtendedFloatingActionButton(
                onClick = {
                    //清除所有节点，再入栈writeVideoBvPage初始页面
                    navController.navigate("writeVideoBvPage") {
                        popUpTo("writeVideoBvPage") { inclusive = true }
                    }
                },
                //给悬浮按钮留出系统栏的空间
                /*modifier = Modifier
                    .navigationBarsPadding(),
                    .align(Alignment.BottomEnd)*/
                icon = { Icon(Icons.Filled.ArrowBack, null) },
                text = { Text(text = "返回") },
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            )
        }

        //下拉刷新
        PullRefreshIndicator(
            isRefreshing,
            pullRefreshState,
            Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding(),
            contentColor =
            if (background == Color.LightGray) Color.Black else background
        )
    }
}

//评论列表，两个参数分别代表是否包含笔记图片和笔记图片的数量
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CommentItem(commentCardViewFlowListState: CardItemBean) {
    val cm = LocalClipboardManager.current //剪切板上下文
    val context = LocalContext.current //运行环境context上下文
    Card(
        //shape = RoundedCornerShape(2.dp), 圆角大小
        elevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onLongClick = {
                    //长按卡片复制评论内容
                    //触发长按事件后执行复制功能
                    cm.setText(AnnotatedString(commentCardViewFlowListState.content))
                    Toast.makeText(context,"评论内容已复制！",Toast.LENGTH_SHORT).show()
                },
        onClick ={ /*....*/ })
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                //用户信息
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(commentCardViewFlowListState.userHead)
                            .crossfade(true)
                            .build(),
                        contentDescription = "",
                        modifier = Modifier
                            .clip(CircleShape)
                            .size(40.dp)
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))

                    Column(modifier = Modifier.fillMaxSize()) {
                        //用户名称
                        Text(
                            commentCardViewFlowListState.userName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        //时间
                        Text(commentCardViewFlowListState.time, fontSize = 14.sp)
                    }
                }
                Spacer(modifier = Modifier.padding(vertical = 4.dp))
                //评论内容
                Text(text = commentCardViewFlowListState.content)
                /*Image(painter = painterResource(id = R.mipmap.bili),
                    contentDescription = "",
                    modifier = Modifier
                        .size(100.dp))*/
                Box(
                    Modifier.height(
                        when (commentCardViewFlowListState.noteImageList.size) {
                            0 -> 0.dp
                            1, 2, 3 -> 140.dp
                            4, 5, 6 -> 280.dp
                            7, 8, 9 -> 420.dp
                            else -> 0.dp
                        }
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    val mContext = LocalContext.current
                    //实现九宫格笔记图片
                    LazyVerticalGrid(
                        //contentPadding = PaddingValues(8.dp),
                        columns = GridCells.Fixed(3)
                    ) {
                        items(commentCardViewFlowListState.noteImageList.size) {
                            Box(modifier = Modifier
                                .size(120.dp)
                                .padding(4.dp)
                                .clickable(onClick = {
                                    imagePreviewDownload(
                                        commentCardViewFlowListState.noteImageList,
                                        it,
                                        mContext
                                    )
                                })
                            ) {
                                val image = if (!commentCardViewFlowListState.noteImageList[it].contains("https")){
                                    commentCardViewFlowListState.noteImageList[it].replace("http","https")
                                }else{
                                    commentCardViewFlowListState.noteImageList[it]
                                }
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(image)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "",
                                    //设置centerCrop属性
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.padding(vertical = 4.dp))

}

//图片预览框架
fun imagePreviewDownload(urlList: List<String>, position: Int, context: Context) {
    val httpsUrlList = urlList.filter {
        !it.contains("https")
    }.map{
            it.replace("http","https")
    }
    // 最简单的调用，即可实现大部分需求，如需定制，可参考下一步的自定义代码：
    ImagePreview
        .instance
        // 上下文，必须是activity，不需要担心内存泄漏，本框架已经处理好；
        .setContext(context)
        // 设置从第几张开始看（索引从0开始）
        .setIndex(position)
        //==========================================
        // 有三种设置数据集合的方式，根据自己的需求进行三选一：
        // 1：第一步生成的imageInfo List
        //.setImageInfoList(imageInfoList)
        // 2：直接传url List
        .setImageList(httpsUrlList as MutableList<String>)
        // 3：只有一张图片的情况，可以直接传入这张图片的url
        //.setImage(String image)
        //==========================================
        //.setShowCloseButton(true) //显示关闭按钮
        .setShowErrorToast(true)  //加载失败Toast
        .setFolderName("哔哩哔哩笔记图片下载")  //下载文件夹名
        .setDownIconResId(R.drawable.ic_download)  //下载按钮图片
        // 全自动模式：WiFi原图，流量下默认普清，可点击按钮查看原图(默认)
        //.setLoadStrategy(ImagePreview.LoadStrategy.Auto)
        // 开启预览
        .start()
    //Log.d("mmmcard",urlList.toString())
    /*
    原地址：https://album.biliimg.com/bfs/new_dyn/9604761992542efaed232992357c397a151567105.jpg
    现地址：http://i0.hdslb.com/bfs/new_dyn/8eb8e11483cc320cde98c0cf5bc712b538453374.png
    将http改成https即可。
     */
    // 默认配置为：
    //      显示顶部进度指示器、
    //      显示右侧下载按钮、
    //      隐藏关闭按钮、
    //      开启点击图片关闭、
    //      开启下拉图片关闭、
    //      加载策略为全自动
}