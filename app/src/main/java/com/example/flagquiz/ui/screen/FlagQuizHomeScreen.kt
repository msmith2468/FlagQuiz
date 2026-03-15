package com.example.flagquiz.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.flagquiz.domain.model.GameDifficulty
import com.example.flagquiz.domain.model.GameMode
import com.example.flagquiz.domain.model.GameRegion

private data class RegionCardStyle(
    val containerColor: Color,
    val contentColor: Color,
    val scoreLabel: String,
    val showPerfectBadge: Boolean
)

@Composable
fun FlagQuizHomeScreen(
    savedScores: Map<GameRegion, Int?>,
    regionCounts: Map<GameRegion, Int>,
    selectedGameMode: GameMode,
    selectedDifficulty: GameDifficulty,
    onSelectGameMode: (GameMode) -> Unit,
    onSelectDifficulty: (GameDifficulty) -> Unit,
    onStartGame: (GameRegion) -> Unit,
    onOpenSettings: () -> Unit
) {
    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Open settings"
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Flag Quiz",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Choose a mode, set a difficulty, then pick a region.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        SelectionSection(
                            title = "Game Type",
                            subtitle = "More modes can slot into this row as we add them.",
                            options = GameMode.entries,
                            selectedOption = selectedGameMode,
                            optionLabel = { it.title },
                            onOptionSelected = onSelectGameMode
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        SelectionSection(
                            title = "Difficulty",
                            subtitle = "Easy focuses on larger countries, Expert includes the full pool.",
                            options = GameDifficulty.entries,
                            selectedOption = selectedDifficulty,
                            optionLabel = { it.title },
                            onOptionSelected = onSelectDifficulty
                        )
                    }
                }
            }

            items(GameRegion.entries) { region ->
                val savedScore = savedScores[region]
                val flagCount = regionCounts[region] ?: 0
                val cardStyle = regionCardStyle(savedScore)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = cardStyle.containerColor,
                        contentColor = cardStyle.contentColor
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            GeoJsonRegionBadge(region = region)
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = region.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (cardStyle.showPerfectBadge) {
                                        PerfectScoreBadge()
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = region.subtitle,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "$flagCount countries",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Mode: ${selectedGameMode.title} · ${selectedDifficulty.title}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = cardStyle.scoreLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { onStartGame(region) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Play ${region.title}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> SelectionSection(
    title: String,
    subtitle: String,
    options: List<T>,
    selectedOption: T,
    optionLabel: (T) -> String,
    onOptionSelected: (T) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(options) { option ->
                val isSelected = option == selectedOption
                ElevatedFilterChip(
                    selected = isSelected,
                    onClick = { onOptionSelected(option) },
                    label = {
                        Text(
                            text = optionLabel(option),
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.elevatedFilterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
    }
}

@Composable
private fun PerfectScoreBadge() {
    Text(
        text = "PERFECT",
        modifier = Modifier
            .background(Color(0xFFFFD54F), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        color = Color(0xFF5D4037),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold
    )
}

private fun regionCardStyle(score: Int?): RegionCardStyle {
    return when {
        score == null -> RegionCardStyle(
            containerColor = Color(0xFF1E88E5),
            contentColor = Color.White,
            scoreLabel = "High score: Not played yet",
            showPerfectBadge = false
        )

        score == 100 -> RegionCardStyle(
            containerColor = Color(0xFF2E7D32),
            contentColor = Color.White,
            scoreLabel = "High score: 100%",
            showPerfectBadge = true
        )

        score < 50 -> RegionCardStyle(
            containerColor = Color(0xFFC62828),
            contentColor = Color.White,
            scoreLabel = "High score: $score%",
            showPerfectBadge = false
        )

        score < 80 -> RegionCardStyle(
            containerColor = Color(0xFFF9A825),
            contentColor = Color(0xFF1F1F1F),
            scoreLabel = "High score: $score%",
            showPerfectBadge = false
        )

        else -> RegionCardStyle(
            containerColor = Color(0xFF2E7D32),
            contentColor = Color.White,
            scoreLabel = "High score: $score%",
            showPerfectBadge = false
        )
    }
}
