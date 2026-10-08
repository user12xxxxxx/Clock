package com.nautesh.challengeclock.ui

import android.content.res.AssetManager

/**
 * Set by App.onCreate before any UI. Kept out of Type.kt so assigning it doesn't build the font
 * families early. The font lives in assets/ because Android ignores variationSettings on res/font
 * fonts (only weight got through, via the platform), which left the "condensed" digits at full width.
 */
lateinit var fontAssets: AssetManager
