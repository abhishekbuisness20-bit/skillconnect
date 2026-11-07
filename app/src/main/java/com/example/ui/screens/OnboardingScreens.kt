package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AppLanguage
import com.example.model.UserRole
import com.example.ui.components.IndianRupeeText
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(1800)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(NavyBackground, NavySurface, Color(0xFF030712))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Authentic SkillConnect Logo with glowing neon border
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(NavyBackground)
                    .border(2.5.dp, Brush.sweepGradient(listOf(CyanBrand, GreenBrand, BlueBrand, CyanBrand)), CircleShape)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.skillconnect_logo),
                    contentDescription = "SkillConnect Official Logo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Skill",
                    color = TextWhite,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Connect",
                    color = GreenBrand,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "India\'s Multi-Service Skill Marketplace",
                color = TextMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            CircularProgressIndicator(
                color = CyanBrand,
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )
        }

        Text(
            text = "Website: skillconnectofficial.cloud.run",
            color = TextDim,
            fontSize = 11.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }
}

@Composable
fun OnboardingScreen(
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onGetStarted: () -> Unit
) {
    var currentSlide by remember { mutableIntStateOf(0) }

    val slides = listOf(
        Triple(
            "Discover Top Indian Talent",
            "Connect with verified designers, developers, video editors, and digital marketers across India.",
            Icons.Default.Search
        ),
        Triple(
            "Transparent Work Orders in ₹",
            "Post milestone-based work requests with clear budgets in INR (₹) and fixed delivery timelines.",
            Icons.Default.MonetizationOn
        ),
        Triple(
            "Real-Time Messaging & Collaboration",
            "Discuss requirements directly with talented professionals and track your projects end-to-end.",
            Icons.Default.Chat
        ),
        Triple(
            "Grow Your Career or Business",
            "Whether hiring or offering skills, SkillConnect gives you verified badges and trusted reviews.",
            Icons.Default.Verified
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header with authentic logo
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.skillconnect_logo),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SkillConnect",
                        color = TextWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                // Bilingual Prompt as requested
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(NavySurfaceVariant)
                        .border(1.dp, CyanBrand.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    TextButton(
                        onClick = { onLanguageChange(AppLanguage.ENGLISH) },
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = if (currentLanguage == AppLanguage.ENGLISH) CyanBrand else Color.Transparent
                        ),
                        modifier = Modifier.height(30.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text(
                            text = "EN",
                            color = if (currentLanguage == AppLanguage.ENGLISH) Color.Black else TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    TextButton(
                        onClick = { onLanguageChange(AppLanguage.HINDI) },
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = if (currentLanguage == AppLanguage.HINDI) GreenBrand else Color.Transparent
                        ),
                        modifier = Modifier.height(30.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text(
                            text = "हिंदी",
                            color = if (currentLanguage == AppLanguage.HINDI) Color.Black else TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Carousel Slide Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(NavySurfaceVariant)
                        .border(2.dp, CyanBrand, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = slides[currentSlide].third,
                        contentDescription = null,
                        tint = CyanBrand,
                        modifier = Modifier.size(54.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = slides[currentSlide].first,
                    color = TextWhite,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = slides[currentSlide].second,
                    color = TextMuted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Indicators
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    slides.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (index == currentSlide) 24.dp else 6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (index == currentSlide) CyanBrand else NavySurfaceVariant)
                        )
                    }
                }
            }

            // Bottom Actions with properly aligned horizontal buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (currentSlide < slides.size - 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (currentSlide > 0) {
                            OutlinedButton(
                                onClick = { currentSlide-- },
                                border = BorderStroke(1.dp, CyanBrand.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("onboarding_prev_button")
                            ) {
                                Text(
                                    text = "Previous",
                                    color = CyanBrand,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Button(
                            onClick = { currentSlide++ },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanBrand),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(if (currentSlide > 0) 1f else 2f)
                                .height(50.dp)
                                .testTag("onboarding_next_button")
                        ) {
                            Text(
                                text = "Next",
                                color = Color(0xFF001F2B),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    TextButton(
                        onClick = onGetStarted,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = "Skip to Sign In",
                            color = TextMuted,
                            fontSize = 13.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { currentSlide-- },
                            border = BorderStroke(1.dp, CyanBrand.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("onboarding_last_prev_button")
                        ) {
                            Text(
                                text = "Previous",
                                color = CyanBrand,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Button(
                            onClick = onGetStarted,
                            colors = ButtonDefaults.buttonColors(containerColor = GreenBrand),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.5f)
                                .height(50.dp)
                                .testTag("onboarding_get_started_button")
                        ) {
                            Text(
                                text = "Get Started",
                                color = Color(0xFF00210E),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}
