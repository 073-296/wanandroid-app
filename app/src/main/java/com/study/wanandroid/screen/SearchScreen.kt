package com.study.wanandroid.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.study.wanandroid.R
import com.study.wanandroid.ui.theme.Shapes
import com.study.wanandroid.ui.theme.WanAndroidTheme

@Composable
fun SearchScreen(modifier: Modifier = Modifier) {
    var searchWord by remember { mutableStateOf("") }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(10.dp)
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = searchWord,
            onValueChange = { searchWord = it },
            placeholder = { Text(text = "热搜词") },
            shape = Shapes.small,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.search),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )

        Spacer(modifier = Modifier.fillMaxWidth().padding(10.dp))

        LazyColumn {  }
    }

}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SearchScreenPreview() {
    WanAndroidTheme {
        SearchScreen()
    }
}
