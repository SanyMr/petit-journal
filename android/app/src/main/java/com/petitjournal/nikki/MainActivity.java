package com.petitjournal.nikki;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(WidgetPlugin.class);
        super.onCreate(savedInstanceState);
        try { MailWorker.schedule(this); } catch (Exception ignored) { }
    }
}
