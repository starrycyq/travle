package com.travle.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.res.vectorResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.AlertDialog
import com.travle.app.ui.theme.TravelAppTheme
import com.travle.app.ui.screens.*
import dagger.hilt.android.AndroidEntryPoint

@OptIn(
    ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)
// 底部导航项数据类
data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TravelAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    // 底部导航栏项目定义
                    val bottomNavigationItems = listOf(
                        BottomNavItem(
                            route = "home",
                            label = "首页",
                            icon = Icons.Filled.Home
                        ),
                        BottomNavItem(
                            route = "discover",
                            label = "发现",
                            icon = Icons.Filled.Explore
                        ),
                        BottomNavItem(
                            route = "my_collection",
                            label = "收藏",
                            icon = Icons.Filled.Favorite
                        ),
                        BottomNavItem(
                            route = "profile",
                            label = "我的",
                            icon = Icons.Filled.Person
                        )
                    )
                    
                    // 获取当前路由以高亮底部导航项
                    val navBackStackEntry = navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry.value?.destination
                    
                    // 检查是否应该显示底部导航栏（仅在主要目的地显示）
                    val shouldShowBottomBar = bottomNavigationItems.any { item ->
                        currentDestination?.hierarchy?.any { it.route == item.route } == true
                    }
                    
                    Scaffold(
                        bottomBar = {
                            if (shouldShowBottomBar) {
                                NavigationBar {
                                    bottomNavigationItems.forEach { item ->
                                        NavigationBarItem(
                                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                                            onClick = {
                                                navController.navigate(item.route) {
                                                    // 弹出到起始目的地，避免回退栈堆积
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    // 避免同一目的地的多个副本
                                                    launchSingleTop = true
                                                    // 恢复状态
                                                    restoreState = true
                                                }
                                            },
                                            icon = {
                                                Icon(
                                                    imageVector = item.icon,
                                                    contentDescription = item.label
                                                )
                                            },
                                            label = { Text(text = item.label) }
                                        )
                                    }
                                }
                            }
                        },
                        floatingActionButton = {
                            if (shouldShowBottomBar) {
                                FloatingActionButton(
                                    onClick = { navController.navigate("chat") },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ) {
                                    Icon(Icons.Filled.Chat, "AI助手")
                                }
                            }
                        }
                    ) { paddingValues ->
                        NavHost(
                            navController = navController,
                            startDestination = "home",
                            modifier = Modifier.padding(paddingValues)
                        ) {
                            composable("home") { 
                                HomeScreen(navController = navController)
                            }
                            composable("discover") { 
                                DiscoverScreen(navController = navController)
                            }
                            composable("my_collection") { 
                                MyCollectionScreen(navController = navController)
                            }
                            composable("profile") { 
                                ProfileScreen(navController = navController)
                            }
                            composable("settings") { 
                                SettingsScreen(navController = navController)
                            }
                            composable(
                                route = "guide_detail?destination={destination}&preferences={preferences}&guide={guide}&images={images}",
                                arguments = listOf(
                                    navArgument("destination") { 
                                        defaultValue = ""
                                        type = androidx.navigation.NavType.StringType
                                    },
                                    navArgument("preferences") { 
                                        defaultValue = ""
                                        type = androidx.navigation.NavType.StringType
                                    },
                                    navArgument("guide") { 
                                        defaultValue = ""
                                        type = androidx.navigation.NavType.StringType
                                    },
                                    navArgument("images") { 
                                        defaultValue = "[]"
                                        type = androidx.navigation.NavType.StringType
                                    }
                                )
                            ) { backStackEntry ->
                                val destination = backStackEntry.arguments?.getString("destination") ?: ""
                                val preferences = backStackEntry.arguments?.getString("preferences") ?: ""
                                val guide = backStackEntry.arguments?.getString("guide") ?: ""
                                val imagesJson = backStackEntry.arguments?.getString("images") ?: "[]"
                                // 解析images JSON字符串（简化：使用逗号分隔）
                                val images = if (imagesJson == "[]") emptyList() else imagesJson.split(",")
                                
                                GuideDetailScreen(
                                    navController = navController,
                                    initialDestination = destination,
                                    initialPreferences = preferences,
                                    initialGuide = guide,
                                    initialImages = images
                                )
                            }
                            composable("chat") {
                                ChatScreen(navController = navController)
                            }
                            composable("roadtrip") {
                                RoadTripScreen(navController = navController)
                            }
                        }
                    }
                }
            }
        }
    }
}