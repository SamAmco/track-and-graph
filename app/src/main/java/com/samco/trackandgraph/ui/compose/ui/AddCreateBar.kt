/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Track & Graph is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.samco.trackandgraph.ui.compose.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

        val buttonText =
            if (isUpdateMode) stringResource(id = R.string.update)
            else stringResource(id = R.string.create)

        FilledButton(
            text = buttonText,
            onClick = onCreateUpdateClicked,
            enabled = errorText == null,
            modifier = Modifier.padding(end = cardMarginSmall)
        )
    }
}
