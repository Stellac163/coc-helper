package com.cochelper.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

/**
 * 把 App 内返回栈与浏览器历史同步：让手机/浏览器的系统返回键在 App 内后退一层，
 * 而不是直接退出整个页面。网页版才有实现，其它平台为空操作。
 */
@Composable
expect fun BrowserBackHandler(navController: NavHostController)
