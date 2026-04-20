package com.tubetone.ui.theme

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// xs: 4 · sm: 8 · md: 12 · lg: 20 · xl: 28 · full pill.
// Cards = md, sheets = xl (top corners), pills = full.
val TubeToneShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

val PillShape = RoundedCornerShape(CornerSize(50))
