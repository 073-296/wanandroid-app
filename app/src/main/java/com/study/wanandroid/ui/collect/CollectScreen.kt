package com.study.wanandroid.ui.collect

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.study.wanandroid.ui.component.ContentBottomBar
import com.study.wanandroid.ui.MainViewModel
import com.study.wanandroid.R
import com.study.wanandroid.common.ToastUtils
import com.study.wanandroid.common.customTab
import com.study.wanandroid.data.local.entity.FavoriteArticleEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel,
    currentRoute: String?,
    bottomBarOnClick: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // 抽屉状态
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // 监听 ViewModel 状态
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val favoriteList by viewModel.favoriteList.collectAsStateWithLifecycle()
    val categoryItems by viewModel.sidebarCategories.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    val currentCategory = categoryItems.find { it.id == selectedCategoryId }
        ?: categoryItems.firstOrNull()
        ?: Category(id = null, name = "全部收藏", count = 0, isRealCategory = false)

    // 弹窗控制状态
    var showCreateDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 底部弹窗状态
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedArticleForMenu by remember { mutableStateOf<FavoriteArticleEntity?>(null) }
    val sheetState = rememberModalBottomSheetState()

    // 进入页面时，触发一次全量对账刷新
    LaunchedEffect(Unit) {
        viewModel.refreshFavorites()
    }

    // 外部包裹 ModalNavigationDrawer
    ModalNavigationDrawer(
        modifier = modifier,
        drawerState = drawerState,
        drawerContent = {
            // 抽屉宽度按屏幕宽度的 55% 计算，避免在小屏上占满整个屏幕
            val screenWidth = LocalConfiguration.current.screenWidthDp.dp
            ModalDrawerSheet(
                modifier = Modifier.width(screenWidth * 0.70f)
            ) {
                CategoryDrawerContent(
                    categories = categoryItems,
                    selectedId = selectedCategoryId,
                    onCategoryClick = { catId ->
                        viewModel.selectCategory(catId)
                        coroutineScope.launch { drawerState.close() }
                    },
                    onCreateCategoryClick = {
                        showCreateDialog = true
                        coroutineScope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = currentCategory.name,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "打开分类栏")
                        }
                    },
                    actions = {
                        // 只有真实分类才显示重命名和删除
                        if (currentCategory.isRealCategory) {
                            IconButton(onClick = { showRenameDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "重命名分类")
                            }
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除分类")
                            }
                        }
                    }
                )
            },
            bottomBar = {
                ContentBottomBar(
                    currentRoute = currentRoute,
                    onClick = { route -> bottomBarOnClick(route) }
                )
            }
        ) { paddingValues ->
            // 内容区：文章列表
            CollectScreenContent(
                modifier = Modifier.padding(paddingValues),
                favoriteList = favoriteList,
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refreshFavorites() },
                onMoreClick = { article ->
                    selectedArticleForMenu = article
                    showBottomSheet = true
                }
            )
        }
    }

    //底部弹窗
    selectedArticleForMenu?.let { article ->
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showBottomSheet = false
                    selectedArticleForMenu = null// 释放状态
                },
                sheetState = sheetState
            ) {
                MoveToCategorySheetContent(
                    article = article,
                    categories = categoryItems.filter { it.id != null },//过滤"全部收藏"
                    onMoveTo = { catId ->
                        viewModel.setArticleCategory(article.articleId, catId)
                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showBottomSheet = false
                                selectedArticleForMenu = null
                            }
                        }
                    },
                    onRemoveFavorite = {
                        viewModel.toggleFavorite(article)
                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showBottomSheet = false
                                selectedArticleForMenu = null
                            }
                        }
                    }
                )
            }
        }
    }

    //弹窗处理区
    if (showCreateDialog) {
        CategoryInputDialog(
            title = "新建分类",
            initialText = "",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                viewModel.addCategory(name) { result ->
                    if (result.isSuccess) showCreateDialog = false
                    if (result.isFailure) ToastUtils.show("分类名已存在")
                }
            }
        )
    }

    if (showRenameDialog && currentCategory.id != null) {
        CategoryInputDialog(
            title = "重命名分类",
            initialText = currentCategory.name,
            onDismiss = { showRenameDialog = false },
            onConfirm = { name ->
                viewModel.renameCategory(currentCategory.id, name) { result ->
                    if (result.isSuccess) showRenameDialog = false
                    if (result.isFailure) ToastUtils.show("重命名失败:分类名已存在")
                }
            }
        )
    }

    if (showDeleteDialog && currentCategory.id != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除分类") },
            text = { Text("确定要删除【${currentCategory.name}】吗？该分类下的文章将变为未分类。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCategory(currentCategory.id)
                    viewModel.selectCategory(null) // 删除后切回"全部"
                    showDeleteDialog = false
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }
}

//收藏界面内容部分
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollectScreenContent(
    modifier: Modifier = Modifier,
    favoriteList: List<FavoriteArticleEntity>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onMoreClick: (FavoriteArticleEntity) -> Unit
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        if (favoriteList.isEmpty()) {//没有数据
            if (isRefreshing) {//正在刷新
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "正在同步",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无收藏文章",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            //有数据时正常显示 LazyColumn
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(favoriteList, key = { it.articleId }) { article ->
                    FavoriteArticleItem(
                        article = article,
                        onMoreClick = { onMoreClick(article) }
                    )
                }
            }
        }
    }
}

//侧边栏内容
@Composable
fun CategoryDrawerContent(
    categories: List<Category>,
    selectedId: Long?,
    onCategoryClick: (Long?) -> Unit,
    onCreateCategoryClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部新建按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCreateCategoryClick() }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "新建分类",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        HorizontalDivider()

        // 分类列表
        LazyColumn {
            items(categories, key = { it.id ?: -999L }) { item ->
                val isSelected = item.id == selectedId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCategoryClick(item.id) }
                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.name,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = item.count.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            }
        }
    }
}

//收藏页面的文章Item
@Composable
fun FavoriteArticleItem(
    article: FavoriteArticleEntity,
    onMoreClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable { customTab(context, article.link) }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.author, article.author ?: "佚名"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // 放到其他分类或取消收藏
            IconButton(
                onClick = onMoreClick
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "更多操作",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

//通用输入弹窗 (用于新建和重命名)
@Composable
fun CategoryInputDialog(
    title: String,
    initialText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("分类名称") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (text.isNotBlank()) onConfirm(text) }
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun MoveToCategorySheetContent(
    article: FavoriteArticleEntity,
    categories: List<Category>,
    onMoveTo: (Long?) -> Unit,
    onRemoveFavorite: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        // 标题
        Text(
            text = "移动至",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            fontWeight = FontWeight.Bold
        )

        // 分类列表
        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
            items(categories, key = { it.id ?: -999L }) { item ->
                val isCurrent = if (item.id == -1L) article.categoryId == null
                else item.id == article.categoryId

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onMoveTo(item.id) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 用 Row 把图标和文字包起来，统一视觉
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = item.name,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (isCurrent) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "当前分类",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // 取消收藏按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onRemoveFavorite() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = "取消收藏", color = MaterialTheme.colorScheme.error)
        }
    }
}