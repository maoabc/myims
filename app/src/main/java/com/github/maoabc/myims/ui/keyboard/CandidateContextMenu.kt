package com.github.maoabc.myims.ui.keyboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.github.maoabc.librime.RimeCandidate

@Composable
fun CandidateContextMenu(
    candidate: RimeCandidate,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onDeleteCandidate: (RimeCandidate) -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = false),
        modifier = modifier
    ) {
        DropdownMenuItem(
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "忘记当前",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            onClick = {
                onDismissRequest()
                onDeleteCandidate(candidate)
            }
        )
    }
}
