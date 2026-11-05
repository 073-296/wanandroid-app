package com.study.wanandroid.db

/**
 * 收藏记录与远程服务器的同步状态。
 * Room 2.4+ 原生支持枚举，默认会以 name (字符串) 的形式存储在数据库中，
 * 比如 "SYNCED"，这样直接用 SQLite 查看工具调试时非常直观。
 */
enum class RemoteSyncState {
    /**
     * 已同步：本地数据与远程服务器完全一致。
     */
    SYNCED,

    /**
     * 待添加：用户在本地点击了收藏，数据库已记录，但还没成功发送 POST 请求给服务端。
     * 通常发生在：断网时、弱网时、或刚点击还未被 WorkManager 处理时。
     */
    PENDING_ADD,

    /**
     * 待删除：用户在本地点击了取消收藏，数据库将其标记为待删除，但还没发送 DELETE 请求。
     * 注意：处于该状态的记录，UI 层查询时会被过滤掉（用户以为删了，其实在后台等着发请求）。
     */
    PENDING_DELETE
}