package com.jianguo.bilibili.note.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.jianguo.bilibili.note.navigation.CommentPageComposable
import com.jianguo.bilibili.note.navigation.WebViewPage
import com.jianguo.bilibili.note.navigation.WriteVideoBvPageComposable
import com.jianguo.bilibili.note.ui.theme.笔记图片获取Theme
import com.jianguo.bilibili.note.util.BiliCommentUtil
import com.jianguo.bilibili.note.viewmodel.CommentCardViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //模拟器无法连接手机热点
        /*BiliCommentUtil().getComments(
            this,
            "BV17d4y1g7fu")*/
        // 设置装饰视图是否应适合WindowInsetsCompat的根级内容视图
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            笔记图片获取Theme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    //color = MaterialTheme.colors.background
                    color = MaterialTheme.colors.background
                ) {
                    //Greeting("Android")
                    //ParentTextFieldLayout()
                    // 因为系统状态栏被隐藏掉,需要创建一个自定义头部,高度为系统栏高度
                    AppNavigation() //导航视图
                }
            }
        }
    }
}

//导航页面
@Composable
fun AppNavigation(){
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "writeVideoBvPage"){
        //输入bv号的页面
        composable("writeVideoBvPage"){
            //设置系统栏颜色
            SetSystemBarColor(true)
            WriteVideoBvPageComposable(navController = navController)
        }
        //网络请求并显示评论列表的界面
        composable("commentPage/{bvid}"){
            //通过NavBackStackEntry获取参数传值
            val bvid = it.arguments?.getString("bvid") ?: "BV17d4y1g7fu"
            //设置系统栏颜色
            SetSystemBarColor(false)
            //模拟评论列表
            CommentPageComposable(navController = navController,
                bvid = bvid)
        }
        //输入bv号的页面
        composable("webViewPage/{https}"){
            val https = it.arguments?.getString("https") ?: "https://www.baidu.com"
            //设置系统栏颜色
            SetSystemBarColor(true)
            WebViewPage(navController = navController,https)
        }
    }
}

//设置系统栏颜色为透明，且用参数控制系统栏中的字体和图标的颜色
@Composable
fun SetSystemBarColor(iconsIsBlack:Boolean){
    val systemUiController = rememberSystemUiController()
    //val useDarkIcons = MaterialTheme.colors.isLight
    SideEffect {
        systemUiController.setStatusBarColor(
            color = Color.Transparent,
            darkIcons = iconsIsBlack //true就是显示黑色的图标和字体颜色
        )
    }
}
