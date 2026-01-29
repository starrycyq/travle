package com.travle.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext

/**
 * 导航集成工具类
 * 提供调用第三方地图应用进行导航的功能
 */
object NavigationHelper {
    
    /**
     * 检查设备上是否安装了指定的地图应用
     */
    fun isMapAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取可用的地图应用列表
     * @return 可用的地图应用包名列表
     */
    fun getAvailableMapApps(context: Context): List<MapApp> {
        val availableApps = mutableListOf<MapApp>()
        
        MapApp.entries.forEach { mapApp ->
            if (isMapAppInstalled(context, mapApp.packageName)) {
                availableApps.add(mapApp)
            }
        }
        
        return availableApps
    }
    
    /**
     * 打开百度地图进行导航
     * @param start 起点地址或坐标（格式："lat,lng" 或 "地址"）
     * @param destination 终点地址或坐标（格式："lat,lng" 或 "地址"）
     * @param mode 出行方式：driving（驾车）、walking（步行）、transit（公交）、riding（骑行）
     */
    fun openBaiduMap(context: Context, start: String, destination: String, mode: String = "driving") {
        val uri = Uri.parse("baidumap://map/direction?" +
                "origin=$start&" +
                "destination=$destination&" +
                "mode=$mode&" +
                "coord_type=gcj02&" +
                "src=${context.packageName}")
        
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // 如果百度地图未安装，可以尝试其他地图
            openWebMap(context, start, destination)
        }
    }
    
    /**
     * 打开高德地图进行导航
     * @param start 起点地址或坐标（格式："lat,lng" 或 "地址"）
     * @param destination 终点地址或坐标（格式："lat,lng" 或 "地址"）
     * @param mode 出行方式：0（驾车）、1（公交）、2（步行）、3（骑行）、4（火车）
     */
    fun openAmap(context: Context, start: String, destination: String, mode: String = "0") {
        val uri = Uri.parse("amapuri://route/plan/?" +
                "sourceApplication=${context.packageName}&" +
                "slat=${if (start.contains(',')) start.split(',')[0] else ""}&" +
                "slon=${if (start.contains(',')) start.split(',')[1] else ""}&" +
                "sname=${if (!start.contains(',')) start else "起点"}&" +
                "dlat=${if (destination.contains(',')) destination.split(',')[0] else ""}&" +
                "dlon=${if (destination.contains(',')) destination.split(',')[1] else ""}&" +
                "dname=${if (!destination.contains(',')) destination else "终点"}&" +
                "dev=0&" +
                "t=$mode")
        
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.setPackage("com.autonavi.minimap")
        
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // 如果高德地图未安装，可以尝试其他地图
            openWebMap(context, start, destination)
        }
    }
    
    /**
     * 打开腾讯地图进行导航
     */
    fun openTencentMap(context: Context, start: String, destination: String, mode: String = "drive") {
        val uri = Uri.parse("qqmap://map/routeplan?" +
                "type=$mode&" +
                "from=$start&" +
                "to=$destination&" +
                "referer=${context.packageName}")
        
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            openWebMap(context, start, destination)
        }
    }
    
    /**
     * 打开谷歌地图进行导航（如果可用）
     */
    fun openGoogleMap(context: Context, start: String, destination: String, mode: String = "driving") {
        val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&" +
                "origin=$start&" +
                "destination=$destination&" +
                "travelmode=$mode")
        
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.setPackage("com.google.android.apps.maps")
        
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            openWebMap(context, start, destination)
        }
    }
    
    /**
     * 打开网页版地图作为后备方案
     */
    fun openWebMap(context: Context, start: String, destination: String) {
        val uri = Uri.parse("https://maps.google.com/maps?saddr=$start&daddr=$destination")
        
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // 如果连浏览器都没有，显示错误提示
        }
    }
    
    /**
     * 智能选择地图应用打开
     * 按优先级：高德地图 > 百度地图 > 腾讯地图 > 谷歌地图 > 网页地图
     */
    fun openSmartNavigation(context: Context, start: String, destination: String, mode: String = "driving") {
        val availableApps = getAvailableMapApps(context)
        
        when {
            availableApps.any { it == MapApp.AMAP } -> {
                val amapMode = when (mode) {
                    "walking" -> "2"
                    "transit" -> "1"
                    "riding" -> "3"
                    else -> "0" // 驾车
                }
                openAmap(context, start, destination, amapMode)
            }
            availableApps.any { it == MapApp.BAIDU } -> {
                openBaiduMap(context, start, destination, mode)
            }
            availableApps.any { it == MapApp.TENCENT } -> {
                openTencentMap(context, start, destination, mode)
            }
            availableApps.any { it == MapApp.GOOGLE } -> {
                openGoogleMap(context, start, destination, mode)
            }
            else -> {
                openWebMap(context, start, destination)
            }
        }
    }
    
    /**
     * 打开地图选择对话框（供UI调用）
     */
    fun showMapSelection(context: Context, start: String, destination: String) {
        // 这个函数应该由UI层实现，显示对话框让用户选择地图应用
        // 这里返回可用地图列表，UI层可以显示选择对话框
    }
}

/**
 * 支持的地图应用枚举
 */
enum class MapApp(
    val packageName: String,
    val displayName: String,
    val priority: Int
) {
    AMAP("com.autonavi.minimap", "高德地图", 1),
    BAIDU("com.baidu.BaiduMap", "百度地图", 2),
    TENCENT("com.tencent.map", "腾讯地图", 3),
    GOOGLE("com.google.android.apps.maps", "谷歌地图", 4);
}