package com.petitjournal.nikki;

import android.content.Context;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/** Receives the widget data from the journal and refreshes every home-screen widget. */
@CapacitorPlugin(name = "NikkiWidget")
public class WidgetPlugin extends Plugin {
    @PluginMethod
    public void update(PluginCall call) {
        String data = call.getString("data", "{}");
        Context ctx = getContext();
        ctx.getSharedPreferences(WidgetData.PREFS, Context.MODE_PRIVATE).edit().putString("data", data).apply();
        WidgetData.refreshAll(ctx);
        call.resolve();
    }
}
