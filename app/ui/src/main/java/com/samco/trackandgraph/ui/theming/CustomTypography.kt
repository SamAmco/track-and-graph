package com.samco.trackandgraph.ui.theming

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val CustomTypography = Typography(
    displayLarge = Typography().displayLarge.copy(fontSize = 60.sp),
    displayMedium = Typography().displayMedium.copy(fontSize = 45.sp),
    displaySmall = Typography().displaySmall.copy(fontSize = 34.sp),
    headlineLarge = Typography().headlineLarge.copy(
        fontSize = 30.sp,
        fontWeight = FontWeight.Bold,
    ),
    headlineMedium = Typography().headlineMedium.copy(
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 28.sp,
    ),
    headlineSmall = Typography().headlineSmall.copy(
        fontSize = 20.sp,
        fontWeight = FontWeight.W500,
    ),
    titleLarge = Typography().titleLarge.copy(fontSize = 28.sp),
    titleMedium = Typography().titleMedium.copy(fontSize = 18.sp),
    titleSmall = Typography().titleSmall.copy(fontSize = 16.sp),
    bodyLarge = Typography().bodyLarge.copy(fontSize = 15.sp),
    bodyMedium = Typography().bodyMedium.copy(fontSize = 15.sp),
    bodySmall = Typography().bodySmall.copy(fontSize = 13.sp),
    labelLarge = Typography().labelLarge.copy(fontSize = 14.sp),
    labelMedium = Typography().labelMedium.copy(fontSize = 14.sp),
    labelSmall = Typography().labelSmall.copy(fontSize = 10.sp),
)
