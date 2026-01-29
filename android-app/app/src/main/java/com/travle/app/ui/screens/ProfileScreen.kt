package com.travle.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.travle.app.ui.viewmodel.ProfileViewModel
import com.travle.app.util.LocationPermissionManager
import com.travle.app.util.NavigationHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.text.input.PasswordVisualTransformation
// import androidx.compose.ui.text.KeyboardOptions
// import androidx.compose.ui.text.KeyboardType
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavHostController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // 导航集成对话框状态
    var showNavigationDialog by remember { mutableStateOf(false) }
    var navigationStart by remember { mutableStateOf("") }
    var navigationDestination by remember { mutableStateOf("北京天安门") }
    var navigationMode by remember { mutableStateOf("driving") }
    var hasLocationPermission by remember { mutableStateOf(false) }
    
    // 位置权限请求launcher
    val locationPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissionsMap ->
            val fineGranted = permissionsMap[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseGranted = permissionsMap[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
            
            hasLocationPermission = fineGranted || coarseGranted
        }
    )
    
    // 初始化时检查权限
    LaunchedEffect(Unit) {
        hasLocationPermission = LocationPermissionManager.hasLocationPermission(context)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Outlined.ArrowBack, "返回")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "个人中心",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        // User Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (uiState.isLoggedIn) Icons.Filled.Person else Icons.Outlined.Person,
                                contentDescription = "用户头像",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = uiState.nickname,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (uiState.isLoggedIn) "已登录，享受个性化旅行体验" else "请登录以保存个人偏好和浏览记录",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (!uiState.isLoggedIn) {
                        Button(
                            onClick = { viewModel.toggleLoginDialog(true) },
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text("登录/注册")
                        }
                    }
                }
                
                Divider(color = MaterialTheme.colorScheme.outline)
                
                if (uiState.isLoggedIn) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = uiState.collectionsCount.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "收藏",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Divider(
                            modifier = Modifier
                                .height(40.dp)
                                .width(1.dp),
                            color = MaterialTheme.colorScheme.outline
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = uiState.sharesCount.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "分享",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Divider(
                            modifier = Modifier
                                .height(40.dp)
                                .width(1.dp),
                            color = MaterialTheme.colorScheme.outline
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = uiState.likesCount.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = "获赞",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    // 未登录时的提示
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "登录后可享受完整功能",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• 保存个人旅行偏好\n• 查看浏览历史\n• 分享攻略到社区\n• 同步收藏内容",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
        }
        
        // 错误消息显示
        uiState.errorMessage?.let { message ->
            LaunchedEffect(message) {
                // 可以显示Snackbar或Toast
                // 暂时清除错误消息
                delay(3000)
                viewModel.clearErrorMessage()
            }
        }
        
        // 登录对话框
        if (uiState.showLoginDialog) {
            var username by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            val focusRequester = remember { FocusRequester() }
            
            AlertDialog(
                onDismissRequest = { viewModel.toggleLoginDialog(false) },
                title = { Text("登录/注册") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("用户名/邮箱") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("密码") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "登录后可保存个人偏好、浏览记录和分享数据",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                val success = viewModel.login(username, password)
                                if (success) {
                                    viewModel.toggleLoginDialog(false)
                                }
                            }
                        },
                        enabled = username.isNotBlank() && password.isNotBlank() && !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("处理中...")
                        } else {
                            Text("登录")
                        }
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { viewModel.toggleLoginDialog(false) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        enabled = !uiState.isLoading
                    ) {
                        Text("取消")
                    }
                }
            )
            
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }
        
        // 小红书授权对话框
        if (uiState.showXiaohongshuDialog) {
            var phoneNumber by remember { mutableStateOf("") }
            var verificationCode by remember { mutableStateOf("") }
            var showVerificationField by remember { mutableStateOf(false) }
            
            AlertDialog(
                onDismissRequest = { viewModel.toggleXiaohongshuDialog(false) },
                title = { Text("小红书授权") },
                text = {
                    Column {
                        if (!showVerificationField) {
                            Text(
                                text = "请输入您的手机号，我们将发送验证码到您的手机",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                label = { Text("手机号") },
                                placeholder = { Text("请输入11位手机号") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                // keyboardOptions = androidx.compose.ui.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.KeyboardType.Phone)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "授权后，应用将可以帮您爬取小红书上的旅行内容",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "请输入收到的验证码",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            OutlinedTextField(
                                value = verificationCode,
                                onValueChange = { verificationCode = it },
                                label = { Text("验证码") },
                                placeholder = { Text("6位验证码") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                // keyboardOptions = androidx.compose.ui.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.KeyboardType.Number)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "验证码已发送到 $phoneNumber",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                confirmButton = {
                    if (!showVerificationField) {
                        Button(
                            onClick = {
                                if (phoneNumber.length == 11) {
                                    scope.launch {
                                        val success = viewModel.sendXiaohongshuVerificationCode(phoneNumber)
                                        if (success) {
                                            showVerificationField = true
                                        }
                                    }
                                }
                            },
                            enabled = phoneNumber.length == 11 && !uiState.isLoading
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("发送中...")
                            } else {
                                Text("发送验证码")
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                if (verificationCode.length == 6) {
                                    scope.launch {
                                        val success = viewModel.authenticateXiaohongshu(phoneNumber, verificationCode)
                                        if (success) {
                                            viewModel.toggleXiaohongshuDialog(false)
                                        }
                                    }
                                }
                            },
                            enabled = verificationCode.length == 6 && !uiState.isLoading
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("验证中...")
                            } else {
                                Text("确认授权")
                            }
                        }
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { 
                            if (showVerificationField) {
                                showVerificationField = false
                            } else {
                                viewModel.toggleXiaohongshuDialog(false)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        enabled = !uiState.isLoading
                    ) {
                        Text(if (showVerificationField) "返回" else "取消")
                    }
                }
            )
        }
    }
}
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Menu Items
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MenuItem(
                icon = Icons.Outlined.Favorite,
                title = "我的收藏",
                description = "查看收藏的旅行攻略",
                onClick = { 
                    if (uiState.isLoggedIn) {
                        navController.navigate("my_collection")
                    } else {
                        viewModel.toggleLoginDialog(true)
                    }
                }
            )
            MenuItem(
                icon = Icons.Outlined.LocalFireDepartment,
                title = "我的分享",
                description = "查看已发布的动态",
                onClick = { 
                    if (uiState.isLoggedIn) {
                        navController.navigate("discover")
                    } else {
                        viewModel.toggleLoginDialog(true)
                    }
                }
            )
            MenuItem(
                icon = Icons.Filled.Chat,
                title = "AI旅行助手",
                description = "实时对话，获取个性化建议",
                onClick = {
                    if (uiState.isLoggedIn) {
                        navController.navigate("chat")
                    } else {
                        viewModel.toggleLoginDialog(true)
                    }
                }
            )
            MenuItem(
                icon = Icons.Filled.Link,
                title = "小红书授权",
                description = "授权爬取小红书旅行内容",
                onClick = { viewModel.toggleXiaohongshuDialog(true) }
            )
            MenuItem(
                icon = Icons.Filled.DirectionsCar,
                title = "自驾游模式",
                description = "输入起点和终点生成自驾攻略",
                onClick = {
                    navController.navigate("roadtrip")
                }
            )
            MenuItem(
                icon = Icons.Filled.Navigation,
                title = "导航集成",
                description = "调用手机导航应用",
                onClick = {
                    showNavigationDialog = true
                }
            )
            MenuItem(
                icon = Icons.Outlined.Settings,
                title = "设置",
                description = "应用设置和偏好",
                onClick = { navController.navigate("settings") }
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Login/Logout Button
        Button(
            onClick = {
                if (uiState.isLoggedIn) {
                    // 退出登录
                    scope.launch {
                        viewModel.logout()
                    }
                } else {
                    // 显示登录对话框
                    viewModel.toggleLoginDialog(true)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (uiState.isLoggedIn) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                contentColor = if (uiState.isLoggedIn) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            if (uiState.isLoggedIn) {
                Icon(Icons.Outlined.Logout, "退出登录", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("退出登录")
            } else {
                Icon(Icons.Outlined.Login, "登录", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("登录/注册")
            }
        }
        
        // 导航集成对话框
        if (showNavigationDialog) {
            var currentStart by remember { mutableStateOf(navigationStart) }
            var currentDestination by remember { mutableStateOf(navigationDestination) }
            var currentMode by remember { mutableStateOf(navigationMode) }
            var availableMaps by remember { mutableStateOf(emptyList<String>()) }
            
            LaunchedEffect(showNavigationDialog) {
                // 获取可用地图应用
                val mapApps = NavigationHelper.getAvailableMapApps(context)
                availableMaps = mapApps.map { it.displayName }
            }
            
            AlertDialog(
                onDismissRequest = { showNavigationDialog = false },
                title = { Text("导航集成") },
                text = {
                    Column {
                        // 权限状态
                        if (!hasLocationPermission) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Outlined.Warning,
                                        "权限提示",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "需要位置权限才能获取当前位置",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                        
                        // 起点输入
                        OutlinedTextField(
                            value = currentStart,
                            onValueChange = { currentStart = it },
                            label = { Text("起点（可选）") },
                            placeholder = { Text("留空将使用当前位置") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Outlined.LocationOn, "起点")
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // 终点输入
                        OutlinedTextField(
                            value = currentDestination,
                            onValueChange = { currentDestination = it },
                            label = { Text("目的地") },
                            placeholder = { Text("请输入目的地地址") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Outlined.Flag, "目的地")
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // 出行方式选择
                        Text(
                            text = "出行方式",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("driving", "walking", "transit", "riding").forEach { mode ->
                                val label = when (mode) {
                                    "driving" -> "驾车"
                                    "walking" -> "步行"
                                    "transit" -> "公交"
                                    "riding" -> "骑行"
                                    else -> mode
                                }
                                
                                FilterChip(
                                    selected = currentMode == mode,
                                    onClick = { currentMode = mode },
                                    label = { Text(label) }
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // 可用地图应用
                        if (availableMaps.isNotEmpty()) {
                            Text(
                                text = "可用地图应用：${availableMaps.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "未检测到地图应用，将使用网页版地图",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                confirmButton = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 权限请求按钮
                        if (!hasLocationPermission) {
                            Button(
                                onClick = {
                                    // 请求位置权限
                                    locationPermissionsLauncher.launch(
                                        arrayOf(
                                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Icon(Icons.Outlined.LocationOn, "位置权限", modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("请求权限")
                            }
                        }
                        
                        Button(
                            onClick = {
                                // 保存设置并打开导航
                                navigationStart = currentStart
                                navigationDestination = currentDestination
                                navigationMode = currentMode
                                
                                // 使用智能导航
                                NavigationHelper.openSmartNavigation(
                                    context = context,
                                    start = if (currentStart.isNotEmpty()) currentStart else "我的位置",
                                    destination = currentDestination,
                                    mode = currentMode
                                )
                                
                                showNavigationDialog = false
                            },
                            enabled = currentDestination.isNotEmpty()
                        ) {
                            Icon(Icons.Outlined.Navigation, "开始导航", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("开始导航")
                        }
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showNavigationDialog = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = "进入",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}