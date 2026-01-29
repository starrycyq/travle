package com.travle.app.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat

object LocationPermissionManager {
    
    /**
     * 检查是否有位置权限
     */
    fun hasLocationPermission(context: Context): Boolean {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        return fineLocationGranted || coarseLocationGranted
    }
    
    /**
     * 检查是否有精确位置权限
     */
    fun hasFineLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 请求位置权限的可组合函数
     * @param onPermissionGranted 权限授予时的回调
     * @param onPermissionDenied 权限被拒绝时的回调
     * @return 是否有权限的布尔值状态
     */
    @Composable
    fun RequestLocationPermission(
        onPermissionGranted: () -> Unit = {},
        onPermissionDenied: () -> Unit = {}
    ): Boolean {
        val hasPermission = remember { mutableStateOf(false) }
        
        val locationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted ->
                hasPermission.value = isGranted
                if (isGranted) {
                    onPermissionGranted()
                } else {
                    onPermissionDenied()
                }
            }
        )
        
        // 如果已经有权限，直接返回true
        // 注意：在Compose中无法直接检查权限，需要Activity或Context
        // 这里的设计需要在调用时传递Context进行检查
        
        return hasPermission.value
    }
    
    /**
     * 请求位置权限（多个权限）
     */
    @Composable
    fun RequestMultipleLocationPermissions(
        onPermissionsGranted: () -> Unit = {},
        onPermissionsDenied: () -> Unit = {}
    ) {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        val locationPermissionsLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
            onResult = { permissionsMap ->
                val fineGranted = permissionsMap[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
                val coarseGranted = permissionsMap[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
                
                if (fineGranted || coarseGranted) {
                    onPermissionsGranted()
                } else {
                    onPermissionsDenied()
                }
            }
        )
        
        // 启动权限请求
        SideEffect {
            locationPermissionsLauncher.launch(permissions)
        }
    }
}