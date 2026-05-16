package io.github.mobdev

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

private val ColorOperator = Color(0xFFFF9F0A)
private val ColorFunctionKey = Color(0xFF636366)
private val ColorDigit = Color(0xFF1C1C1E)
private val ColorDisplayBg = Color(0xFF000000)
private val ColorText = Color(0xFFFFFFFF)

@Composable
fun CalculatorScreen(vm: CalculatorViewModel = viewModel()) {
    val state by vm.state.collectAsState()

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ColorDisplayBg)
                .padding(paddingValues)
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            // Display
            Text(
                text = state.display,
                color = ColorText,
                fontSize = 72.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 8.dp, bottom = 8.dp)
            )

            // Buttons — 5 rows × 4 columns
            val rows = listOf(
                listOf(
                    Key(stringResource(R.string.key_ac), KeyType.Function) { vm.onClear() },
                    Key(stringResource(R.string.key_sign), KeyType.Function) { vm.onToggleSign() },
                    Key(stringResource(R.string.key_percent), KeyType.Function) { vm.onPercent() },
                    Key(stringResource(R.string.key_div), KeyType.Operator) { vm.onOperator('÷') }
                ),
                listOf(
                    Key("7", KeyType.Digit) { vm.onDigit("7") },
                    Key("8", KeyType.Digit) { vm.onDigit("8") },
                    Key("9", KeyType.Digit) { vm.onDigit("9") },
                    Key(stringResource(R.string.key_mul), KeyType.Operator) { vm.onOperator('×') }
                ),
                listOf(
                    Key("4", KeyType.Digit) { vm.onDigit("4") },
                    Key("5", KeyType.Digit) { vm.onDigit("5") },
                    Key("6", KeyType.Digit) { vm.onDigit("6") },
                    Key(stringResource(R.string.key_sub), KeyType.Operator) { vm.onOperator('-') }
                ),
                listOf(
                    Key("1", KeyType.Digit) { vm.onDigit("1") },
                    Key("2", KeyType.Digit) { vm.onDigit("2") },
                    Key("3", KeyType.Digit) { vm.onDigit("3") },
                    Key(stringResource(R.string.key_add), KeyType.Operator) { vm.onOperator('+') }
                ),
                listOf(
                    Key("0", KeyType.Digit, span = 2) { vm.onDigit("0") },
                    Key(stringResource(R.string.key_dot), KeyType.Digit) { vm.onDot() },
                    Key(stringResource(R.string.key_eq), KeyType.Operator) { vm.onEquals() }
                )
            )

            rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { key ->
                        CalcButton(
                            key = key,
                            modifier = Modifier.weight(key.span.toFloat())
                        )
                    }
                }
            }

            Spacer(Modifier.padding(bottom = 4.dp))
        }
    }
}

private data class Key(
    val label: String,
    val type: KeyType,
    val span: Int = 1,
    val action: () -> Unit
)

private enum class KeyType { Digit, Operator, Function }

@Composable
private fun CalcButton(key: Key, modifier: Modifier = Modifier) {
    val bgColor = when (key.type) {
        KeyType.Operator -> ColorOperator
        KeyType.Function -> ColorFunctionKey
        KeyType.Digit -> ColorDigit
    }
    val textColor = when (key.type) {
        KeyType.Function -> ColorDisplayBg
        else -> ColorText
    }

    Surface(
        onClick = key.action,
        shape = CircleShape,
        color = bgColor,
        modifier = modifier.aspectRatio(key.span.toFloat())
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = key.label,
                color = textColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
