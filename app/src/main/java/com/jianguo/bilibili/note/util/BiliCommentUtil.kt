package com.jianguo.bilibili.note.util

import android.annotation.SuppressLint
import android.util.JsonReader
import android.util.Log
import com.alibaba.fastjson2.JSON
import com.jianguo.bilibili.note.bean.CardItemBean
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


/**
 * 类别：Ktor网络请求工具类
 * 坚果
 * qq 1051888852
 * create 2023/4/11 20:00
 */
class BiliCommentUtil {
    /*
    1.获取视频信息
    2.获取评论
    3.提供按热度与按时间排序方法
     */
    //Retrofit构造封装
    private fun getRetrofit(): Retrofit {
        val client = OkHttpClient.Builder()
            /* .connectTimeout(15000, TimeUnit.MILLISECONDS)  //预留足够时间连接服务器
             .readTimeout(15000, TimeUnit.MILLISECONDS)  //预留足够时间处理数据，否则偶尔出现超时java.net.SocketTimeoutException: timeout
 */.build()

        val factory = GsonConverterFactory.create()

        return Retrofit.Builder()
            .baseUrl("https://api.bilibili.com")
            .client(client)
            .addConverterFactory(factory)
            .build()
    }

    //获取评论列表，返回一个评论集合mutableListOf<CardItemBean>()，
    //要用withContext(Dispatchers.IO)获取协程返回值，所以此方法要标明为suspend挂起函数
    suspend fun getCommentsByBvid(
        //视频的BV号
        bvid: String,
        //页码
        next: Int,
        //排序方式
        mode:Int
    ): MutableList<CardItemBean> = withContext(Dispatchers.IO) {
        /**
         * 这里直接用Retrofit的execute()接body()和string()获取返回数据，不要用带有callback接口实现的
         * 辣鸡接口，狗都不用，直接用阿里的FastJSON解析数据把该拿的拿出来，数据取完直接打包返回，不用管那么多
         */
        val commentCardListUP = mutableListOf<CardItemBean>()
        val commentCardList = mutableListOf<CardItemBean>()
        val retrofit = getRetrofit()
        val getVideoInfoService = retrofit  //获取Retrofit
            .create(GetVideoInfoService::class.java)  //调用Service
            .getVideoInfo(bvid)  //调用方法获取视频详细信息
        val getVideoInfoServiceResponseBody =
            getVideoInfoService.execute().body()?.string() //获取返回的视频详细信息JSON
        //获取视频信息的调试输出，此方法已竣工，直接注释掉
        /*Log.e("GetVideoMessage",
            getVideoInfoServiceResponseBody)*/
        //从视频详细信息JSON中获取aid，这个aid就是评论接口的oid参数，然后用aid获取评论区的评论集合
        //加个非空判断，避免逆天用户瞎填bv号闪退
            val aid = JSON.parseObject(
                getVideoInfoServiceResponseBody
            )?.getJSONObject("data")
                ?.getJSONObject("View")
                ?.getLong("aid")
            /*Log.e(
                "GetVideoMessage",
                aid.toString()
            )*/
        if(aid!=null){
            //获取热门接口，后续再加个按时间排列，对应B站的两种获取评论方式
            val getHotComment = retrofit.create(GetVideoInfoService::class.java)
                .getHotComment(
                    type = 1,
                    aid = aid,
                    next = next,
                    mode = mode
                )  //具体接口详细参数看这个
            //https://github.com/SocialSisterYi/bilibili-API-collect/blob/master/docs/comment/list.md
            //到这里已经拿到了热评的JSON，只需要把评论集合提取出来再解析就行
            val commentsData = JSON.parseObject(
                getHotComment.execute().body()?.string().toString()
            ).getJSONObject("data")
            //方法竣工，注释掉测试语句
            /*Log.e(
                "GetVideoMessage",
                commentsData.toString()
            )*/
            //构建一个可变列表，包含评论数据实体的     ↑这里先放到最上面方便判断是否为空↑
            //val commentCardList = mutableListOf<CardItemBean>()
            /*Log.e(
                "GetVideoMessage",
                next.toString()
            )*/


            //2023.5.20 获取置顶评论
            if(next==1){
                val repliesUP = commentsData?.getJSONArray("top_replies")
                if(repliesUP?.size != 0){
                    repliesUP?.forEach {
                        //评论卡片
                        val commentCard = JSON.parseObject(
                            it.toString()
                        )
                        //用户名
                        val userName =
                            commentCard
                                .getJSONObject("member")
                                .getString("uname")
                        //用户头像
                        val userHead = commentCard
                            .getJSONObject("member")
                            .getString("avatar")
                        //评论时间
                        val userTime = timeStamp2Date(
                            commentCard
                                .getLong("ctime").toString()
                        )
                        /*Log.e("GetVideoMessage",
                            commentCard
                                .getLong("ctime").toString())*/
                        //评论内容
                        val content = "【置顶评论】"+commentCard
                            .getJSONObject("content")
                            .getString("message")
                        //笔记json
                        val noteObject = commentCard
                            .getJSONObject("content")
                            .getJSONObject("rich_text")
                        val noteImageList: MutableList<String> = mutableListOf()
                        if(noteObject!=null){
                            //旧版笔记图片集合
                            //将笔记图片的json集合转化为string集合
                            noteObject.getJSONObject("note")
                                ?.getJSONArray("images")
                                ?.forEach {
                                        image->
                                    noteImageList.add(image.toString())
                                }
                        }else{
                            //新版笔记集合
                            commentCard
                                .getJSONObject("content")
                                .getJSONArray("pictures")
                                ?.forEach { pictures ->
                                    noteImageList.add(
                                        JSON.parseObject(pictures.toString())
                                            .getString("img_src")
                                    )
                                }
                        }
                        commentCardListUP.add(
                            CardItemBean(
                                userName = userName,
                                userHead = userHead,
                                time = userTime,
                                content = content,
                                noteImageList = noteImageList,
                            )
                        )
                        /*//只给数据流添加包含笔记图片的评论，需求第一
                        if(noteImageList.isNotEmpty()){
                            commentCardList.add(
                                CardItemBean(
                                    userName = userName,
                                    userHead = userHead,
                                    time = userTime,
                                    content = content,
                                    noteImageList = noteImageList,
                                )
                            )
                        }*/
                    }
                }
            }


            //直接遍历评论集，把每条评论里的重要信息塞进去，一个个打包
            val replies = commentsData?.getJSONArray("replies")
            if(replies?.size != 0){
                replies?.forEach {
                    //评论卡片
                    val commentCard = JSON.parseObject(
                        it.toString()
                    )
                    //用户名
                    val userName =
                        commentCard
                            .getJSONObject("member")
                            .getString("uname")
                    //用户头像
                    val userHead = commentCard
                        .getJSONObject("member")
                        .getString("avatar")
                    //评论时间
                    val userTime = timeStamp2Date(
                        commentCard
                            .getLong("ctime").toString()
                    )
                    /*Log.e("GetVideoMessage",
                        commentCard
                            .getLong("ctime").toString())*/
                    //评论内容
                    val content = commentCard
                        .getJSONObject("content")
                        .getString("message")
                    //笔记json
                    val noteObject = commentCard
                        .getJSONObject("content")
                        .getJSONObject("rich_text")
                    val noteImageList: MutableList<String> = mutableListOf()
                    if(noteObject!=null){
                        //旧版笔记图片集合
                        //将笔记图片的json集合转化为string集合
                        noteObject.getJSONObject("note")
                            ?.getJSONArray("images")
                            ?.forEach {
                                    image->
                                noteImageList.add(image.toString())
                            }
                    }else{
                        //新版笔记集合
                        commentCard
                            .getJSONObject("content")
                            .getJSONArray("pictures")
                            ?.forEach { pictures ->
                                noteImageList.add(
                                    JSON.parseObject(pictures.toString())
                                        .getString("img_src")
                                )

                            }
                    }
                   /* Log.d("jianguo666", userName +
                            noteImageList.toString())*/
                    commentCardList.add(
                        CardItemBean(
                            userName = userName,
                            userHead = userHead,
                            time = userTime,
                            content = content,
                            noteImageList = noteImageList,
                        )
                    )
                    /*//只给数据流添加包含笔记图片的评论，需求第一
                    if(noteImageList.isNotEmpty()){
                        commentCardList.add(
                            CardItemBean(
                                userName = userName,
                                userHead = userHead,
                                time = userTime,
                                content = content,
                                noteImageList = noteImageList,
                            )
                        )
                    }*/
                }
            }
        }
        //withContext需要一个返回值，到这里评论集合已经打包完了，直接作为返回值即可。
        commentCardListUP.plus(commentCardList).toMutableList()
    }
    suspend fun getCommentsByOid(
        //视频的BV号
        oid: Long,
        //页码
        next: Int,
        //排序方式
        mode:Int
    ): MutableList<CardItemBean> = withContext(Dispatchers.IO) {
            /**
             * 这里直接用Retrofit的execute()接body()和string()获取返回数据，不要用带有callback接口实现的
             * 辣鸡接口，狗都不用，直接用阿里的FastJSON解析数据把该拿的拿出来，数据取完直接打包返回，不用管那么多
             */
            val commentCardListUP = mutableListOf<CardItemBean>()
            val commentCardList = mutableListOf<CardItemBean>()

            //获取视频信息的调试输出，此方法已竣工，直接注释掉
            /*Log.e("GetVideoMessage",
                getVideoInfoServiceResponseBody)*/
            //从视频详细信息JSON中获取aid，这个aid就是评论接口的oid参数，然后用aid获取评论区的评论集合
            //加个非空判断，避免逆天用户瞎填bv号闪退
        //11纯文本17图文
        val type = listOf(11,17,100)
        for(i in type) {
            if(i==100){
                val retrofit = getRetrofit()
                //新版图文动态，获取oid字段的接口如下：
                //https://api.bilibili.com/x/polymer/web-dynamic/v1/detail?timezone_offset=-480&id=814859852739248216&features=itemOpusStyle
                val getDynamicOid = retrofit.create(GetVideoInfoService::class.java)
                    .getDynamicOid(id = oid)
                val json = getDynamicOid.execute().body()?.string().toString()
                Log.d("牛逼333",json)
                if(json.contains("\"code\":-404")){
                    break
                }else{
                    //新版图文动态的oid
                    val newOid = JSON.parseObject(
                        json
                    ).getJSONObject("data").getJSONObject("item").getJSONObject("basic")
                        .getString("comment_id_str")
                    //获取热门接口，后续再加个按时间排列，对应B站的两种获取评论方式
                    val getHotComment = retrofit.create(GetVideoInfoService::class.java)
                        .getHotComment(
                            type = 11, //新版图文动态用的11
                            aid = newOid.toLong(),
                            next = next,
                            mode = mode
                        )  //具体接口详细参数看这个
                    Log.d("牛逼333", newOid.toString())
                    //https://github.com/SocialSisterYi/bilibili-API-collect/blob/master/docs/comment/list.md
                    //到这里已经拿到了热评的JSON，只需要把评论集合提取出来再解析就行
                    val json2 = getHotComment.execute().body()?.string().toString()
                    //有数据就添加，没有直接下一轮循环
                    if(json2.contains("\"code\":-404")){
                        break
                    }else{
                        //数据不为空，则获取笔记列表
                        val commentsData = JSON.parseObject(
                            json2
                        ).getJSONObject("data")

                        //2023.5.20 获取置顶评论
                        if(next==1){
                            val repliesUP = commentsData?.getJSONArray("top_replies")
                            if(repliesUP?.size != 0){
                                repliesUP?.forEach {
                                    //评论卡片
                                    val commentCard = JSON.parseObject(
                                        it.toString()
                                    )
                                    //用户名
                                    val userName =
                                        commentCard
                                            .getJSONObject("member")
                                            .getString("uname")
                                    //用户头像
                                    val userHead = commentCard
                                        .getJSONObject("member")
                                        .getString("avatar")
                                    //评论时间
                                    val userTime = timeStamp2Date(
                                        commentCard
                                            .getLong("ctime").toString()
                                    )
                                    /*Log.e("GetVideoMessage",
                                        commentCard
                                            .getLong("ctime").toString())*/
                                    //评论内容
                                    val content = "【置顶评论】"+commentCard
                                        .getJSONObject("content")
                                        .getString("message")
                                    //笔记json
                                    val noteObject = commentCard
                                        .getJSONObject("content")
                                        .getJSONObject("rich_text")
                                    val noteImageList: MutableList<String> = mutableListOf()
                                    if(noteObject!=null){
                                        //旧版笔记图片集合
                                        //将笔记图片的json集合转化为string集合
                                        noteObject.getJSONObject("note")
                                            ?.getJSONArray("images")
                                            ?.forEach {
                                                    image->
                                                noteImageList.add(image.toString())
                                            }
                                    }else{
                                        //新版笔记集合
                                        commentCard
                                            .getJSONObject("content")
                                            .getJSONArray("pictures")
                                            ?.forEach { pictures ->
                                                noteImageList.add(
                                                    JSON.parseObject(pictures.toString())
                                                        .getString("img_src")
                                                )
                                            }
                                    }
                                    commentCardListUP.add(
                                        CardItemBean(
                                            userName = userName,
                                            userHead = userHead,
                                            time = userTime,
                                            content = content,
                                            noteImageList = noteImageList,
                                        )
                                    )
                                    /*//只给数据流添加包含笔记图片的评论，需求第一
                                    if(noteImageList.isNotEmpty()){
                                        commentCardList.add(
                                            CardItemBean(
                                                userName = userName,
                                                userHead = userHead,
                                                time = userTime,
                                                content = content,
                                                noteImageList = noteImageList,
                                            )
                                        )
                                    }*/
                                }
                            }
                        }

                        //直接遍历评论集，把每条评论里的重要信息塞进去，一个个打包
                        val replies = commentsData?.getJSONArray("replies")
                        if(replies?.size != 0){
                            replies?.forEach {
                                //评论卡片
                                val commentCard = JSON.parseObject(
                                    it.toString()
                                )
                                //用户名
                                val userName =
                                    commentCard
                                        .getJSONObject("member")
                                        .getString("uname")
                                //用户头像
                                val userHead = commentCard
                                    .getJSONObject("member")
                                    .getString("avatar")
                                //评论时间
                                val userTime = timeStamp2Date(
                                    commentCard
                                        .getLong("ctime").toString()
                                )
                                /*Log.e("GetVideoMessage",
                                    commentCard
                                        .getLong("ctime").toString())*/
                                //评论内容
                                val content = commentCard
                                    .getJSONObject("content")
                                    .getString("message")
                                //笔记json
                                val noteObject = commentCard
                                    .getJSONObject("content")
                                    .getJSONObject("rich_text")
                                val noteImageList: MutableList<String> = mutableListOf()
                                if(noteObject!=null){
                                    //旧版笔记图片集合
                                    //将笔记图片的json集合转化为string集合
                                    noteObject.getJSONObject("note")
                                        ?.getJSONArray("images")
                                        ?.forEach {
                                                image->
                                            noteImageList.add(image.toString())
                                        }
                                }else{
                                    //新版笔记集合
                                    commentCard
                                        .getJSONObject("content")
                                        .getJSONArray("pictures")
                                        ?.forEach { pictures ->
                                            noteImageList.add(
                                                JSON.parseObject(pictures.toString())
                                                    .getString("img_src")
                                            )
                                        }
                                }
                                commentCardList.add(
                                    CardItemBean(
                                        userName = userName,
                                        userHead = userHead,
                                        time = userTime,
                                        content = content,
                                        noteImageList = noteImageList,
                                    )
                                )
                            }
                        }
                        break
                    }
                }
            }else{
                val retrofit = getRetrofit()
                //获取热门接口，后续再加个按时间排列，对应B站的两种获取评论方式
                val getHotComment = retrofit.create(GetVideoInfoService::class.java)
                    .getHotComment(
                        type = i,
                        aid = oid,
                        next = next,
                        mode = mode
                    )  //具体接口详细参数看这个
                //Log.d("牛逼222", oid.toString())
                //https://github.com/SocialSisterYi/bilibili-API-collect/blob/master/docs/comment/list.md
                //到这里已经拿到了热评的JSON，只需要把评论集合提取出来再解析就行
                val json = getHotComment.execute().body()?.string().toString()
                //有数据就添加，没有直接下一轮循环
                if(json.contains("\"code\":-404")){
                    continue
                }else{
                    //数据不为空，则获取笔记列表
                    val commentsData = JSON.parseObject(
                        json
                    ).getJSONObject("data")

                    //2023.5.20 获取置顶评论
                    if(next==1){
                        val repliesUP = commentsData?.getJSONArray("top_replies")
                        if(repliesUP?.size != 0){
                            repliesUP?.forEach {
                                //评论卡片
                                val commentCard = JSON.parseObject(
                                    it.toString()
                                )
                                //用户名
                                val userName =
                                    commentCard
                                        .getJSONObject("member")
                                        .getString("uname")
                                //用户头像
                                val userHead = commentCard
                                    .getJSONObject("member")
                                    .getString("avatar")
                                //评论时间
                                val userTime = timeStamp2Date(
                                    commentCard
                                        .getLong("ctime").toString()
                                )
                                /*Log.e("GetVideoMessage",
                                    commentCard
                                        .getLong("ctime").toString())*/
                                //评论内容
                                val content = "【置顶评论】"+commentCard
                                    .getJSONObject("content")
                                    .getString("message")
                                //笔记json
                                val noteObject = commentCard
                                    .getJSONObject("content")
                                    .getJSONObject("rich_text")
                                val noteImageList: MutableList<String> = mutableListOf()
                                if(noteObject!=null){
                                    //旧版笔记图片集合
                                    //将笔记图片的json集合转化为string集合
                                    noteObject.getJSONObject("note")
                                        ?.getJSONArray("images")
                                        ?.forEach {
                                                image->
                                            noteImageList.add(image.toString())
                                        }
                                }else{
                                    //新版笔记集合
                                    commentCard
                                        .getJSONObject("content")
                                        .getJSONArray("pictures")
                                        ?.forEach { pictures ->
                                            noteImageList.add(
                                                JSON.parseObject(pictures.toString())
                                                    .getString("img_src")
                                            )
                                        }
                                }
                                commentCardListUP.add(
                                    CardItemBean(
                                        userName = userName,
                                        userHead = userHead,
                                        time = userTime,
                                        content = content,
                                        noteImageList = noteImageList,
                                    )
                                )
                                /*//只给数据流添加包含笔记图片的评论，需求第一
                                if(noteImageList.isNotEmpty()){
                                    commentCardList.add(
                                        CardItemBean(
                                            userName = userName,
                                            userHead = userHead,
                                            time = userTime,
                                            content = content,
                                            noteImageList = noteImageList,
                                        )
                                    )
                                }*/
                            }
                        }
                    }

                    //直接遍历评论集，把每条评论里的重要信息塞进去，一个个打包
                    val replies = commentsData?.getJSONArray("replies")
                    if(replies?.size != 0){
                        replies?.forEach {
                            //评论卡片
                            val commentCard = JSON.parseObject(
                                it.toString()
                            )
                            //用户名
                            val userName =
                                commentCard
                                    .getJSONObject("member")
                                    .getString("uname")
                            //用户头像
                            val userHead = commentCard
                                .getJSONObject("member")
                                .getString("avatar")
                            //评论时间
                            val userTime = timeStamp2Date(
                                commentCard
                                    .getLong("ctime").toString()
                            )
                            /*Log.e("GetVideoMessage",
                                commentCard
                                    .getLong("ctime").toString())*/
                            //评论内容
                            val content = commentCard
                                .getJSONObject("content")
                                .getString("message")
                            //笔记json
                            val noteObject = commentCard
                                .getJSONObject("content")
                                .getJSONObject("rich_text")
                            val noteImageList: MutableList<String> = mutableListOf()
                            if(noteObject!=null){
                                //旧版笔记图片集合
                                //将笔记图片的json集合转化为string集合
                                noteObject.getJSONObject("note")
                                    ?.getJSONArray("images")
                                    ?.forEach {
                                            image->
                                        noteImageList.add(image.toString())
                                    }
                            }else{
                                //新版笔记集合
                                commentCard
                                    .getJSONObject("content")
                                    .getJSONArray("pictures")
                                    ?.forEach { pictures ->
                                        noteImageList.add(
                                            JSON.parseObject(pictures.toString())
                                                .getString("img_src")
                                        )
                                    }
                            }
                            commentCardList.add(
                                CardItemBean(
                                    userName = userName,
                                    userHead = userHead,
                                    time = userTime,
                                    content = content,
                                    noteImageList = noteImageList,
                                )
                            )
                        }
                    }
                    break
                }
            }
        }
        //withContext需要一个返回值，到这里评论集合已经打包完了，直接作为返回值即可。
            commentCardListUP.plus(commentCardList).toMutableList()
    }
}

//自定义的Retrofit的Service接口，包含获取视频详细信息和获取热评两个方法，相当于模板，到时候直接调用传参就行
interface GetVideoInfoService {
    //获取视频详细信息
    //构建时指定域名，请求注解中指定接口地址，注意接口后不能有斜杠。
    //构建后的完整url，域名后面要放参数，所以不能有斜杠：
    //https://api.bilibili.com/x/web-interface/view/detail?bvid=BV17d4y1g7fu
    @GET("/x/web-interface/view/detail")
    fun getVideoInfo(
        @Query("bvid") bvid: String, //传入所需bvid参数，获取视频信息json，取出oid参数
    ): Call<ResponseBody>
    //获取动态oid
    @GET("/x/polymer/web-dynamic/v1/detail")
    fun getDynamicOid(
        //请求头，必须加，不加会报错-352
        /*2024.4.10 原先的用户代理user-agent失效了，用浏览器重新
        Get一下，把请求头里的用户代理复制过来就行了。
         */
        @Header("user-agent") userAgent:String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.6045.160 Safari/537.36",
        @Query("id") id: Long,
    ): Call<ResponseBody>
    //获取评论区明细_懒加载
    //https://api.bilibili.com/x/v2/reply/main（GET)
    @GET("/x/v2/reply") //改了一下接口，原来是懒加载，现在是分页加载
    fun getHotComment(
        @Query("type") type: Int,  //必要，评论区类型代码，判断是哪个功能模块下的评论区。
        @Query("oid") aid: Long,  //必要，目标评论区id，用视频详细信息接口获取。
        //分页加载：1是按点赞数，2是按回复数，0是按时间
        @Query("sort") mode: Int = 1,  //懒加载：非必要，排序方式，默认为3，0和3都是仅按热度排序，1是按时间+热度，2是仅按时间排序
        //pn相当于懒加载的next，但是翻页加载对于时间排序下数据的查看更好用，和按热度查询方式一致。
        @Query("pn") next: Int = 0,  //非必要，评论页选择，默认为0，按热度时：热度顺序页码（0 为第一页），按时间时：时间倒序楼层号
        //@Query("ps") ps: Int = 10,  //请求一次的项数，默认20
    ): Call<ResponseBody>
}

//评论区接口示例，这里的oid参数对应上面获取视频详细信息中的aid！
/*
https://api.bilibili.com/x/v2/reply/main
?csrf=c9b2639ca10e5330232a88d82d137605&mode=3&next=0&oid=345725564&plat=1&seek_rpid=&type=1
*/
/*笔记图片json示例：
"rich_text": {
    "note": {
        "summary": "试一下试一下试一下试一下试一下试一下试一下试一下试一下试一下试一下试一下试一下试一下试一下试一下试一下试一下试一下试一下试一下试一下试一下试一下试一下试一...",
        "images": ["https://i0.hdslb.com/bfs/note/2fdf41ba5f0882fd5c405b287b574686508f632f.jpg", "https://i0.hdslb.com/bfs/note/2fdf41ba5f0882fd5c405b287b574686508f632f.jpg", "https://i0.hdslb.com/bfs/note/2fdf41ba5f0882fd5c405b287b574686508f632f.jpg"],
        "click_url": "https://www.bilibili.com/h5/note-app/view?cvid=18641039\u0026pagefrom=comment",
        "last_mtime_text": "2022-09-17"
    }
},
...
*/


//时间戳转日期，亲测有效
@SuppressLint("SimpleDateFormat")
fun timeStamp2Date(seconds: String): String {
    val format = "yyyy年MM月dd日 HH:mm:ss"
    if (seconds.isEmpty() || seconds == "null") {
        return ""
    }
    val sdf = SimpleDateFormat(format)
    return sdf.format(Date((seconds + "000").toLong()))
}
