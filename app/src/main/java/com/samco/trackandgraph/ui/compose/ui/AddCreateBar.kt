package com.samco.trackandgraph.ui.compose.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.R

@Composable
fun AddCreateBar(
    errorText: Int?,
    onCreateUpdateClicked: () -> Unit,
    isUpdateMode: Boolean
) = Surface(
    modifier = Modifier.fillMaxWidth()
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {

        Text(
            text = errorText?.let { stringResource(id = it) } ?: "",
            color = MaterialTheme.colorScheme.error,
            fontSize = MaterialTheme.typography.bodyMedium.fontSize,
            fontWeight = MaterialTheme.typography.bodyMedium.fontWeight,
            modifier = Modifier
                .weight(1f)
                .padding(8.dp)
                .fillMaxWidth()
        )

        Button(
            onClick = onCreateUpdateClicked,
            shape = MaterialTheme.shapes.small,
            enabled = errorText == null,
            modifier = Modifier.padding(end = dimensionResource(id = R.dimen.card_margin_small))
        ) {
            val buttonText =
                if (isUpdateMode) stringResource(id = R.string.update)
                else stringResource(id = R.string.create)
            Text(buttonText)
        }
    }
}

