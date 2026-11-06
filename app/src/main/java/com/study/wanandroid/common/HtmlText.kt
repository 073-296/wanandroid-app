package com.study.wanandroid.common

import androidx.core.text.HtmlCompat

/**
 * 把接口返回的富文本标题转成纯文本。
 *
 * wanandroid 的搜索接口会把命中的关键词包成 <em class='highlight'>关键词</em> 返回，
 * 标题里还夹杂 &mdash; 这类 HTML 实体。原样塞给 Compose 的 Text 就会把标签直接显示出来。
 *
 * HtmlCompat 会剥掉标签，并把实体还原成字符（&mdash; → —、&amp; → &、&nbsp; → 空格）。
 * 纯文本标题（不含 < 和 &）会直接返回，不产生额外开销。
 */
fun String.toPlainText(): String {
    if (isEmpty() || (indexOf('<') == -1 && indexOf('&') == -1)) return this
    return HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_LEGACY)
        .toString()
        .replace('\u00A0', ' ')//&nbsp; 会转成不换行空格，统一成普通空格
        .trim()
}
