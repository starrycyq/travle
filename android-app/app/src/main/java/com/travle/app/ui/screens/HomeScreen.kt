package com.travle.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.travle.app.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    // 使用navController参数以避免警告
    val _navController = navController
    val uiState by viewModel.uiState.collectAsState()
    
    // 清除消息的副作用
    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        if (uiState.errorMessage != null || uiState.successMessage != null) {
            // 可以添加自动清除消息的逻辑
        }
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
            Icon(
                imageVector = Icons.Outlined.Explore,
                contentDescription = "Travel Assistant",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "旅行助手",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = "AI 智能生成个性化旅行攻略",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Input Card
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
                OutlinedTextField(
                    value = uiState.destination,
                    onValueChange = viewModel::updateDestination,
                    label = { Text("目的地") },
                    placeholder = { Text("请输入您想去的地方，如：北京、杭州") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = uiState.preferences,
                    onValueChange = viewModel::updatePreferences,
                    label = { Text("旅行偏好（可选）") },
                    placeholder = { Text("如：自然风光、美食、历史遗迹") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = viewModel::clearInputs,
                        modifier = Modifier.weight(1f),
                        enabled = uiState.destination.isNotEmpty() || uiState.preferences.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text("清除")
                    }
                    Button(
                        onClick = viewModel::generateGuide,
                        modifier = Modifier.weight(1f),
                        enabled = uiState.destination.trim().isNotEmpty() && !uiState.isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("生成中...")
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Star,
                                contentDescription = "Generate",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("生成攻略")
                        }
                    }
                }
            }
        }
        
        // Guide Result
        if (uiState.guide != null) {
            Spacer(modifier = Modifier.height(16.dp))
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
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "攻略内容",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row {
                            IconButton(
                                onClick = {
                                    // 复制到剪贴板
                                    // 简化：显示Toast
                                }
                            ) {
                                Icon(Icons.Outlined.ContentCopy, "复制")
                            }
                            IconButton(
                                onClick = {
                                    viewModel.updateShareContent(uiState.guide ?: "")
                                    viewModel.toggleShareModal(true)
                                }
                            ) {
                                Icon(Icons.Outlined.Share, "分享")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.guide ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                    
                    if (uiState.images.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "相关图片",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.images) { imageUrl ->
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(160.dp, 120.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            // 传递攻略数据到详情页面
                            val guide = uiState.guide ?: ""
                            val destination = uiState.destination
                            val preferences = uiState.preferences
                            val images = uiState.images.joinToString(",") // 简单序列化
                            
                            navController.navigate(
                                "guide_detail?destination=${destination}&preferences=${preferences}&guide=${guide}&images=${images}"
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Visibility, "查看详情")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("查看详情")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
    }
    
    // Share Modal
    if (uiState.shareModalOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.toggleShareModal(false) },
            title = { Text("发布旅途攻略") },
            text = {
                Column {
                    OutlinedTextField(
                        value = uiState.shareContent,
                        onValueChange = viewModel::updateShareContent,
                        label = { Text("分享内容") },
                        placeholder = { Text("分享您的旅行心得...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 8
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "分享到发现页，让更多人看到您的精彩旅程",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::publishGuide,
                    enabled = uiState.shareContent.trim().isNotEmpty()
                ) {
                    Text("发布")
                }
            },
            dismissButton = {
                Button(
                    onClick = { viewModel.toggleShareModal(false) },
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
    
    // 显示错误或成功消息 - 添加SnackBar来处理错误
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
        }
    }
    
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
        }
    }
    
    // Snackbar显示
    SnackbarHost(hostState = snackbarHostState)
}