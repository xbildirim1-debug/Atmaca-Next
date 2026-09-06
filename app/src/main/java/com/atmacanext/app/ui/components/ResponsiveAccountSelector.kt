package com.atmacanext.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.atmacanext.app.domain.model.Account

/**
 * 10-account selector that adapts between phone and tablet widths.
 */
@Composable
fun ResponsiveAccountSelector(
    accounts: List<Account>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val window = rememberAtmacaWindowClass()
    val columns = when (window) {
        AtmacaWindowClass.COMPACT -> 2
        AtmacaWindowClass.MEDIUM -> 3
        AtmacaWindowClass.EXPANDED -> 4
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 360.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = true,
    ) {
        items(
            items = accounts
                .distinctBy { it.username.trim().lowercase() }
                .take(10),
            key = { it.id },
        ) { account ->
            FilterChip(
                selected = account.id in selectedIds,
                onClick = { onToggle(account.id) },
                label = {
                    Column {
                        Text(account.displayName, maxLines = 1)
                        Text(account.username, maxLines = 1)
                    }
                },
            )
        }
    }
}
