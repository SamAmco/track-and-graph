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

package com.samco.trackandgraph.group

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.compose.ui.cardMarginSmall
import com.samco.trackandgraph.ui.compose.ui.cardPadding
import com.samco.trackandgraph.ui.compose.ui.halfDialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.smallIconSize

@Composable
fun SymlinkIcon(modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(R.drawable.link_2_24px),
        contentDescription = stringResource(R.string.symlink),
        modifier = modifier
            .size(smallIconSize + (halfDialogInputSpacing * 2))
            .padding(halfDialogInputSpacing),
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
