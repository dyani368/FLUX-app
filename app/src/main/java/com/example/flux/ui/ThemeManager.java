package com.example.flux.ui;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.flux.R;

public class ThemeManager {

    private static final String PREF = "flux_prefs";
    private static final String KEY = "selected_theme";

    public static final String DARK_MINIMAL  = "dark_minimal";
    public static final String WARM_NEUTRAL  = "warm_neutral";
    public static final String COOL_CALM     = "cool_calm";
    public static final String FOREST        = "forest";

    public static void applyTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String theme = prefs.getString(KEY, DARK_MINIMAL);
        switch (theme) {
            case WARM_NEUTRAL: context.setTheme(R.style.Theme_Flux_WarmNeutral); break;
            case COOL_CALM:    context.setTheme(R.style.Theme_Flux_CoolCalm);    break;
            case FOREST:       context.setTheme(R.style.Theme_Flux_Forest);      break;
            default:           context.setTheme(R.style.Theme_Flux_DarkMinimal); break;
        }
    }

    public static void saveTheme(Context context, String theme) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
               .edit().putString(KEY, theme).apply();
    }

    public static String getTheme(Context context) {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                      .getString(KEY, DARK_MINIMAL);
    }

    // background color per theme for manual use
    public static int getBackgroundColor(Context context) {
        switch (getTheme(context)) {
            case WARM_NEUTRAL: return 0xFFF8F5F0;
            case COOL_CALM:    return 0xFF101820;
            case FOREST:       return 0xFF0D1A0D;
            default:           return 0xFF121212;
        }
    }

    // card color per theme
    public static int getCardColor(Context context) {
        switch (getTheme(context)) {
            case WARM_NEUTRAL: return 0xFFFFFFFF;
            case COOL_CALM:    return 0xFF1A2030;
            case FOREST:       return 0xFF1A2E1A;
            default:           return 0xFF1E1E1E;
        }
    }

    // accent color per theme
    public static int getAccentColor(Context context) {
        switch (getTheme(context)) {
            case WARM_NEUTRAL: return 0xFFA8D5BA;
            case COOL_CALM:    return 0xFFA2C4D5;
            case FOREST:       return 0xFFA8D5BA;
            default:           return 0xFFB8A2D1;
        }
    }

    // text color per theme
    public static int getTextColor(Context context) {
        switch (getTheme(context)) {
            case WARM_NEUTRAL: return 0xFF2C2C2C;
            case COOL_CALM:    return 0xFFD0E0F0;
            case FOREST:       return 0xFFD0EDD0;
            default:           return 0xFFE0E0E0;
        }
    }
}
