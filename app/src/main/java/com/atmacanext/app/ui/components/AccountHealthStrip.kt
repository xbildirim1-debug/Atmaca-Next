package com.atmacanext.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atmacanext.app.domain.model.Account
import com.atmacanext.app.ui.theme.TextSecondary

/**
 * CP15 account health strip.
 * Uses LazyRow so 10 accounts never get compressed into a fixed Row.
 */
@Composable
fun AccountHealthStrip(
    accounts: List<Account>,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            items = accounts
                .distinctBy { it.username.trim().lowercase() }
                .take(10),
            key = { it.id },
        ) { account ->
            Column(
                modifier = Modifier.widthIn(min = 78.dp, max = 124.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = account.health.toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
                Text(
                    text = account.displayName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 10.sp,
                    color = TextSecondary,
                )
                Text(
                    text = account.username,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 9.sp,
                    color = TextSecondary,
                )
            }
        }
    }
}
