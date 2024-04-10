package com.jianguo.bilibili.note.activity

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.jianguo.bilibili.note.R
import com.permissionx.guolindev.PermissionX

/**
 * 类别：安卓权限申请页面
 * 坚果
 * qq 1051888852
 * create 2023/4/16 23:37
 */
class PermissionActivity : FragmentActivity() {
    private lateinit var mSharedPreferences: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 获取SharedPreferences实例
        mSharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)

        // 检查是否第一次启动应用程序
        val isFirstStart = mSharedPreferences.getBoolean("isFirstStart", true)

        if (isFirstStart) {
            // 如果是第一次启动，显示需要显示的页面
            setContentView(R.layout.permission_layout)
            val button = findViewById<Button>(R.id.button)
            button.setOnClickListener {
                val requestList = ArrayList<String>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestList.add(android.Manifest.permission.READ_MEDIA_IMAGES)
                    requestList.add(android.Manifest.permission.READ_MEDIA_AUDIO)
                    requestList.add(android.Manifest.permission.READ_MEDIA_VIDEO)
                }
                if (requestList.isNotEmpty()) {
                    PermissionX.init(this)
                        .permissions(requestList)
                        .onExplainRequestReason { scope, deniedList ->
                            val message = "下载笔记图片需要存储权限，需要您同意以下权限才能正常使用。"
                            scope.showRequestReasonDialog(deniedList, message, "允许", "拒绝")
                        }
                        .request { allGranted, _, _ ->   //所有权限，通过的权限，未通过的权限->
                            if (allGranted) {
                                //Toast.makeText(this, "所有申请的权限都已通过，您可以正常下载图片了。", Toast.LENGTH_LONG).show()
                                // 将isFirstStart标记设置为false
                                mSharedPreferences.edit().putBoolean("isFirstStart", false).apply()
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            } else {
                                finish()
                            }
                        }
                }else{
                    mSharedPreferences.edit().putBoolean("isFirstStart", false).apply()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            }
        } else {
            // 如果不是第一次启动，关闭当前活动
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}