package com.jianguo.bilibili.note.navigation

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState

/**
 * 类别：动态笔记
 * 坚果
 * qq 1051888852
 * create 2023/7/5 15:28
 */
@Composable
fun WebViewPage(navController: NavController, https:String){
    val state = rememberWebViewState(https.replace("\\","/"))
    WebView( state,modifier = Modifier.fillMaxSize())
    val id = state.lastLoadedUrl
        ?.split("?")
        ?.get(0)
        ?.split("dynamic/")
        ?.get(1)
    if (id != null) {
        if(id.length>10) {
            //remember避免重组！避免重组！避免重组！避免重组！避免重组！避免重组！避免重组！
            val a = remember {
                Log.d("牛逼", id.toString())
                //清除所有节点，再入栈writeVideoBvPage初始页面
                navController.navigate("writeVideoBvPage") {
                    popUpTo("writeVideoBvPage") { inclusive = true }
                }
                navController.navigate("commentPage/$id")
                0
            }
        }
    }
}