package com.samco.trackandgraph.changelogviewer

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.ui.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.ui.ChangelogDialogContent
import com.samco.trackandgraph.ui.ui.ChangelogReleaseNote
import com.samco.trackandgraph.ui.ui.ContinueCancelButtons
import com.samco.trackandgraph.ui.ui.SelectorButton
import com.samco.trackandgraph.ui.ui.SupportOptionViewData
import com.samco.trackandgraph.ui.ui.SupportOptionsContent
import com.samco.trackandgraph.ui.ui.dialogInputSpacing

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TnGComposeTheme {
                ChangelogViewerApp()
            }
        }
    }
}

@Composable
private fun ChangelogViewerApp() {
    var markdown by remember { mutableStateOf(sampleMarkdown) }
    var showPreview by remember { mutableStateOf(false) }
    var showSupportScreen by remember { mutableStateOf(false) }
    var mockPurchaseComplete by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                text = "Changelog Viewer",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        markdown = context.clipboardText().orEmpty()
                    },
                ) {
                    Text("Paste")
                }

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { markdown = "" },
                ) {
                    Text("Clear")
                }

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        showSupportScreen = false
                        mockPurchaseComplete = false
                        showPreview = true
                    },
                ) {
                    Text("Preview")
                }
            }
        },
    ) { paddingValues ->
        MarkdownEditor(
            markdown = markdown,
            onMarkdownChanged = { markdown = it },
            paddingValues = paddingValues,
        )

        if (showPreview) {
            ChangelogDialogContent(
                releaseNotes = listOf(
                    ChangelogReleaseNote(
                        version = "Preview",
                        markdown = markdown,
                    )
                ),
                onDismissRequest = {
                    if (showSupportScreen) {
                        showSupportScreen = false
                        mockPurchaseComplete = false
                    } else {
                        showPreview = false
                    }
                },
                dismissOnClickOutside = true,
                dismissOnBackPress = true,
                supportContent = {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "If Track & Graph is useful to you, consider supporting its development.",
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                    )
                    SelectorButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Maybe later",
                        onClick = { showPreview = false },
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            mockPurchaseComplete = false
                            showSupportScreen = true
                        },
                    ) {
                        Text("Support development")
                    }
                },
                alternateContent = {
                    MockSupportScreen(
                        purchaseComplete = mockPurchaseComplete,
                        onOptionClicked = { mockPurchaseComplete = true },
                        onBack = {
                            mockPurchaseComplete = false
                            showSupportScreen = false
                        },
                    )
                },
                showAlternateContent = showSupportScreen,
            )
        }
    }
}

@Composable
private fun MockSupportScreen(
    purchaseComplete: Boolean,
    onOptionClicked: (String) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dialogInputSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (purchaseComplete) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Thank you for supporting Track & Graph!",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        } else {
            SupportOptionsContent(
                description = "Support is voluntary and does not unlock any features.",
                options = listOf(
                    SupportOptionViewData("small", "£1.99", highlighted = false),
                    SupportOptionViewData("medium", "£4.99", highlighted = true),
                    SupportOptionViewData("large", "£9.99", highlighted = false),
                ),
                purchaseInProgress = false,
                onOptionClicked = onOptionClicked,
            )
        }

        ContinueCancelButtons(
            cancelVisible = true,
            continueVisible = false,
            onCancel = onBack,
        )
    }
}

@Composable
private fun MarkdownEditor(
    markdown: String,
    onMarkdownChanged: (String) -> Unit,
    paddingValues: PaddingValues,
) {
    OutlinedTextField(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        value = markdown,
        onValueChange = onMarkdownChanged,
        textStyle = MaterialTheme.typography.bodyMedium,
        label = { Text("Markdown") },
    )
}

private fun Context.clipboardText(): String? {
    val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clipData = clipboardManager.primaryClip ?: return null
    if (clipData.itemCount == 0) return null
    return clipData.getItemAt(0).coerceToText(this)?.toString()
}

private val sampleMarkdown = """
    ## New Features
    
    - Paste a changelog here
    - Tap Preview Dialog to see the production dialog
    
    ## Fixes
    
    Inline `code`, **bold**, *italic*, links, images, and tables use the same renderer.
""".trimIndent()
