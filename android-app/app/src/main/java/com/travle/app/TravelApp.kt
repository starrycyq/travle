package com.travle.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TravelApp : Application() {
    // Hilt会自动处理依赖注入初始化
    override fun onCreate() {
        super.onCreate()
        // 可以在这里添加全局初始化代码
    }
}