package com.jianguo.bilibili.note.navigation

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import androidx.navigation.NavController


/**
 * 类别：Welcome页面（输入视频BV号）
 * 坚果
 * qq 1051888852
 * create 2023/4/9 16:47
 */

//主布局控制输入框居中显示
@Composable
fun WriteVideoBvPageComposable(navController: NavController){
    var bvid by remember { mutableStateOf("") }
    var isButtonClicked by remember { mutableStateOf(false) }
    var buttonText by remember { mutableStateOf("获取评论列表") }
    val context = LocalContext.current
    Box(
        //contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        /*TextButton(modifier = Modifier.align(Alignment.TopEnd).padding(top = 14.dp, end = 28.dp).statusBarsPadding(),
            onClick = {
                context.startActivity(Intent(context, About::class.java))
            }) {
            Text(text = "软件关于", fontSize = 18.sp)
        }*/
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .align(Alignment.Center)
        ) {
            // 这里放置需要居中对齐的组件
            OutlinedTextField(
                value = bvid ,
                modifier = Modifier
                    .fillMaxWidth()
                    //.height(150.dp)
                    .padding(16.dp),
                onValueChange = {
                    if(it.length-bvid.length>5){
                        //如果一次性变化量大于5个字符就是复制粘贴操作，先显示文字，再自动触发获取评论列表
                        bvid = it.trim()
                        buttonText = "正在加载中..."
                        if ((bvid.isNotBlank()
                                    && bvid.length>5
                                ) && !isButtonClicked
                        ) {
                            isButtonClicked = true // 自动点击按钮
                            //将按钮的点击事件放在这里就可以自动执行了
                            if(bvid.contains("BV")||bvid.contains("bv")){
                                navController.navigate("commentPage/${bvid.replace(" ","")}")
                            }else if(bvid.contains("b23.tv")){
                                navController.navigate("webViewPage/${bvid.replace("/","\\")}")
                            }else{
                                Toast.makeText(context, "BV号或链接不正确", Toast.LENGTH_SHORT).show()
                                //清除所有节点，再入栈writeVideoBvPage初始页面
                                navController.navigate("writeVideoBvPage") {
                                    popUpTo("writeVideoBvPage") { inclusive = true }
                                }
                            }
                        }
                    }else{
                        //如果是纯手打字的操作就赋值，显示文字
                        bvid = it.trim()
                    }
                },
                textStyle = TextStyle(
                    fontSize = 36.sp,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 3,
                label = {
                    Text(text = "视频请直接简介复制BV号，动态请直接右上角复制链接，电脑网页右键图片审查元素即可获得图片链接。",
                        style = TextStyle(fontSize = 15.sp)
                    )
                }
            )
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(start = 64.dp, end = 64.dp),
                //点击跳转到评论列表页面
                onClick = {
                    //isButtonClicked = true
                    buttonText = "正在加载中..."
                    if ((bvid.isNotBlank() && bvid.length>5) && !isButtonClicked
                    ) {
                        isButtonClicked = true // 自动点击按钮
                        //将按钮的点击事件放在这里就可以自动执行了
                        if(bvid.contains("BV")||bvid.contains("bv")){
                            navController.navigate("commentPage/${bvid.replace(" ","")}")
                        }else if(bvid.contains("b23.tv")){
                            navController.navigate("webViewPage/${bvid}")
                        }else{
                            Toast.makeText(context, "BV号或链接不正确", Toast.LENGTH_SHORT).show()
                            //清除所有节点，再入栈writeVideoBvPage初始页面
                            navController.navigate("writeVideoBvPage") {
                                popUpTo("writeVideoBvPage") { inclusive = true }
                            }
                        }
                    }
                },
                enabled = bvid.isNotBlank()&&bvid.length>5
            ) {
                Text(text = buttonText
                    ,style = TextStyle(fontSize = 20.sp)
                )
            }
        }
        Text(
            text = "我的b站个人主页",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 48.dp)
                .clickable {
                    val uri: Uri = Uri.parse("https://b23.tv/oNmaQKJ")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    context.startActivity(intent)
                },
            color = Color(0xFF039BE5),
            //fontWeight = FontWeight.Bold, // 字体加粗
            textDecoration = TextDecoration.Underline, //下划线
        )
    }
}
