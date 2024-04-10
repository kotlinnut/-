package com.jianguo.bilibili.note.bean

//评论页面卡片Bean
data class CardItemBean(
    //用户名称
    val userName:String = "默认用户名",
    //用户头像
    val userHead:String = "https://i2.hdslb.com/bfs/face/0c84b9f4ad546d3f20324809d45fc439a2a8ddab.jpg@240w_240h_1c_1s.webp",
    //时间
    val time:String = "默认时间",
    //评论内容
    val content:String = "默认评论内容",
    //笔记图片集合
    val noteImageList:List<String> = listOf()
)