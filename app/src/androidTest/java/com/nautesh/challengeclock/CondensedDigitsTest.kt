package com.nautesh.challengeclock

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.ui.unit.sp
import com.nautesh.challengeclock.ui.clockDigits
import com.nautesh.challengeclock.ui.RobotoFlex
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

// The clock digits rely on Roboto Flex's width and optical-size axes; if those settings get dropped the digits
// silently render full width and stop narrowing when an alarm turns off.
@RunWith(AndroidJUnit4::class)
class CondensedDigitsTest {
    private fun width(family: androidx.compose.ui.text.font.FontFamily, weight: Int): Float {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val typeface = createFontFamilyResolver(context).resolve(family, FontWeight(weight)).value as Typeface
        return Paint().apply { this.typeface = typeface; textSize = 100f }.measureText("0000")
    }

    @Test fun condensedIsNarrower() {
        val regular = width(RobotoFlex, 400)
        val condensed = width(clockDigits(112.sp).fontFamily!!, 400)
        assertTrue("condensed $condensed should be well under regular $regular", condensed < regular * 0.85f)
    }

    @Test fun lighterIsNarrower() {
        val digits = clockDigits(112.sp).fontFamily!!
        val bold = width(digits, 800)
        val light = width(digits, 400)
        assertTrue("400 weight $light should be narrower than 800 weight $bold", light < bold)
    }
}
