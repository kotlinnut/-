package com.jianguo.bilibili.note.viewmodel

import androidx.compose.runtime.remember
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jianguo.bilibili.note.bean.CardItemBean
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * 类别：分离出评论卡片的数据源
 * 坚果
 * qq 1051888852
 * create 2023/4/11 18:57
 */
class CommentCardViewModel : ViewModel(){
    //var isLast = false
    //将冷流改为stateFlow，通过更改list值改变flow值。
    //新建一个flow数据流，其中包含了所有的列表的卡片数据。
    var commentCardFlow : MutableStateFlow<List<CardItemBean>> = MutableStateFlow(
            emptyList()
    )
    //获取评论总数
    /*fun getCommentCardNumber() : Int{
        //返回用协程异步获取的值
        return runBlocking {
            withContext(viewModelScope.coroutineContext) {
                commentCardFlow.count()
            }
        }
    }*/
}