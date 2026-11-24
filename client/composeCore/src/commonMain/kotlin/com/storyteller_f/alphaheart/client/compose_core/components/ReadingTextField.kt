/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storyteller_f.alphaheart.client.compose_core.theme.fieldColors

/** External labels keep a stable position while the native editor retains selection and IME state. */
@Composable
fun ReadingOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    label: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    prefix: (@Composable () -> Unit)? = null,
    suffix: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = RoundedCornerShape(8.dp),
    colors: TextFieldColors =
        OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
        ),
) {
    val interactions = interactionSource ?: remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    val presentation = fieldColors(colors, enabled, isError, focused)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle.copy(color = presentation.text),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        visualTransformation = visualTransformation,
        interactionSource = interactions,
        cursorBrush =
        SolidColor(
            if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        ),
    ) { editor ->
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (label != null) {
                ProvideTextStyle(
                    MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 20.8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) { label() }
            }
            Box(
                Modifier.fillMaxWidth().heightIn(min = 46.dp).clip(shape)
                    .background(presentation.background).border(1.dp, presentation.border, shape),
            ) {
                ReadingFieldDecoration(
                    value,
                    textStyle,
                    placeholder,
                    leadingIcon,
                    trailingIcon,
                    prefix,
                    suffix,
                    editor,
                )
            }
            supportingText?.invoke()
        }
    }
}

@Composable
private fun ReadingFieldDecoration(
    value: String,
    style: TextStyle,
    placeholder: (@Composable () -> Unit)?,
    leading: (@Composable () -> Unit)?,
    trailing: (@Composable () -> Unit)?,
    prefix: (@Composable () -> Unit)?,
    suffix: (@Composable () -> Unit)?,
    editor: @Composable () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (leading != null) Box(Modifier.sizeIn(maxHeight = 24.dp)) { leading() }
        prefix?.invoke()
        Box(Modifier.weight(1f), propagateMinConstraints = style.textAlign == TextAlign.Center) {
            if (value.isEmpty() && placeholder != null) {
                ProvideTextStyle(style.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)) { placeholder() }
            }
            editor()
        }
        suffix?.invoke()
        if (trailing != null) Box(Modifier.sizeIn(maxHeight = 24.dp)) { trailing() }
    }
}
