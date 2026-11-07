package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.model.ServiceCategory
import com.example.ui.theme.BlueBrand
import com.example.ui.theme.CyanBrand
import com.example.ui.theme.GreenBrand
import com.example.ui.theme.NavyCardBorder
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import kotlin.math.ceil

/**
 * A responsive Service Category Grid component built with [LazyVerticalGrid]
 * that enables users to browse professional skill sectors across India.
 * Displays a subtle animated shimmer loading skeleton while categories are being fetched.
 */
@Composable
fun ServiceCategoryGrid(
    categories: List<ServiceCategory>,
    selectedCategoryId: String?,
    currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    isLoading: Boolean = categories.isEmpty(),
    skeletonItemCount: Int = 6,
    columns: Int = 2,
    userScrollEnabled: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
    onCategoryClick: (ServiceCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val isHindi = currentLanguage == AppLanguage.HINDI
    val safeColumns = columns.coerceAtLeast(1)
    val showShimmerSkeleton = isLoading || categories.isEmpty()
    val effectiveCount = if (showShimmerSkeleton) {
        skeletonItemCount.coerceAtLeast(safeColumns)
    } else {
        categories.size
    }
    val rowCount = ceil(effectiveCount / safeColumns.toDouble()).toInt().coerceAtLeast(1)
    // Calculate deterministic grid height when nested inside a LazyColumn (userScrollEnabled = false)
    val estimatedGridHeight: Dp = (rowCount * 152).dp + ((rowCount - 1).coerceAtLeast(0) * 12).dp + 12.dp

    val gridModifier = if (userScrollEnabled) {
        modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp, max = 720.dp)
            .testTag("service_category_grid")
    } else {
        modifier
            .fillMaxWidth()
            .height(estimatedGridHeight)
            .testTag("service_category_grid")
    }

    val shimmerBrush = if (showShimmerSkeleton) rememberCategoryShimmerBrush() else null

    LazyVerticalGrid(
        columns = GridCells.Fixed(safeColumns),
        modifier = gridModifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = userScrollEnabled
    ) {
        if (showShimmerSkeleton && shimmerBrush != null) {
            items(
                count = effectiveCount,
                key = { index -> "skeleton_category_$index" }
            ) { index ->
                ServiceCategorySkeletonCard(
                    shimmerBrush = shimmerBrush,
                    index = index
                )
            }
        } else {
            items(
                items = categories,
                key = { it.id }
            ) { category ->
                ServiceCategoryGridItem(
                    category = category,
                    isHindi = isHindi,
                    isSelected = selectedCategoryId == category.id,
                    onClick = { onCategoryClick(category) }
                )
            }
        }
    }
}

@Composable
fun ServiceCategorySkeletonCard(
    shimmerBrush: Brush,
    index: Int = 0,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurfaceVariant.copy(alpha = 0.78f)),
        border = BorderStroke(1.dp, NavyCardBorder.copy(alpha = 0.65f)),
        modifier = modifier
            .fillMaxWidth()
            .height(152.dp)
            .clip(RoundedCornerShape(16.dp))
            .testTag("category_skeleton_card_$index")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Sector Icon & Skill Count Pill Skeleton
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(shimmerBrush)
                )

                Box(
                    modifier = Modifier
                        .width(62.dp)
                        .height(20.dp)
                        .clip(CircleShape)
                        .background(shimmerBrush)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Middle: Category Title & 2-Line Description Skeleton
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.74f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(shimmerBrush)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.94f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(shimmerBrush)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.60f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(shimmerBrush)
                )
            }

            // Bottom Row: Subcategory Tag & Arrow Skeleton
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(86.dp)
                        .height(11.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(shimmerBrush)
                )
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(shimmerBrush)
                )
            }
        }
    }
}

@Composable
private fun rememberCategoryShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "categoryGridShimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = -300f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1250,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "categoryGridShimmerOffset"
    )

    val shimmerColors = listOf(
        NavySurface.copy(alpha = 0.85f),
        CyanBrand.copy(alpha = 0.16f),
        BlueBrand.copy(alpha = 0.14f),
        NavySurface.copy(alpha = 0.85f)
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(x = translateAnim - 240f, y = translateAnim - 240f),
        end = Offset(x = translateAnim, y = translateAnim)
    )
}

@Composable
fun ServiceCategoryGridItem(
    category: ServiceCategory,
    isHindi: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = rememberCategoryAccentColor(category.id)
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            accentColor.copy(alpha = 0.18f)
        } else {
            NavySurfaceVariant
        },
        label = "categoryGridContainerColor"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) accentColor else NavyCardBorder,
        label = "categoryGridBorderColor"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = borderColor
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(152.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("category_card_${category.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Sector Icon & Skill Count Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.28f),
                                    accentColor.copy(alpha = 0.10f)
                                )
                            )
                        )
                        .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIconForName(category.iconName),
                        contentDescription = if (isHindi) category.nameHi else category.nameEn,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.55f))
                        .border(0.8.dp, accentColor.copy(alpha = 0.35f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = accentColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = if (isHindi) "${category.subcategories.size} कौशल" else "${category.subcategories.size} skills",
                        color = accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Middle: Category Title & Description
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isHindi) category.nameHi else category.nameEn,
                    color = TextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = if (isHindi) category.descriptionHi else category.descriptionEn,
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Bottom Row: Top Subcategory Preview + Arrow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val sampleSubcategory = category.subcategories.firstOrNull() ?: "Verified Pros"
                Text(
                    text = sampleSubcategory,
                    color = accentColor.copy(alpha = 0.9f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowOutward,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

private fun categoryIconForName(iconName: String): ImageVector {
    return when (iconName) {
        "Palette" -> Icons.Default.Palette
        "Code" -> Icons.Default.Code
        "Videocam" -> Icons.Default.Videocam
        "TrendingUp" -> Icons.AutoMirrored.Filled.TrendingUp
        "Edit" -> Icons.Default.Edit
        else -> Icons.Default.BusinessCenter
    }
}

private fun rememberCategoryAccentColor(categoryId: String): Color {
    return when (categoryId) {
        "cat_design" -> CyanBrand
        "cat_dev" -> GreenBrand
        "cat_media" -> BlueBrand
        "cat_marketing" -> CyanBrand
        "cat_writing" -> GreenBrand
        "cat_business" -> BlueBrand
        else -> CyanBrand
    }
}
