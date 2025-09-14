package com.samco.trackandgraph.functions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.ui.compose.ui.cardPadding
import com.samco.trackandgraph.ui.compose.ui.dialogInputSpacing
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.buttonSize

private val cardWidth = 350.dp

@Composable
fun OutputNode(node: Node.Output) {
    Column(
        Modifier
            .width(cardWidth)
            .padding(horizontal = connectorSize / 2, vertical = cardPadding),
        verticalArrangement = Arrangement.spacedBy(dialogInputSpacing)
    ) {
        Text(stringResource(R.string.output), style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = node.name.value,
            onValueChange = { node.name.value = it },
            label = { Text(stringResource(id = R.string.function_name)) },
            maxLines = Int.MAX_VALUE
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = node.description.value,
            onValueChange = { node.description.value = it },
            label = { Text(stringResource(R.string.add_a_longer_description_optional)) },
            maxLines = Int.MAX_VALUE
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { node.isDuration.value = !node.isDuration.value }
                .heightIn(min = buttonSize)
                .fillMaxWidth()
        ) {
            Checkbox(
                checked = node.isDuration.value,
                onCheckedChange = null
            )
            DialogInputSpacing()
            Text(stringResource(R.string.this_is_a_time_or_duration))
        }
    }
}


