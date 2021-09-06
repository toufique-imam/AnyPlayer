package com.stream.jmxplayer.ui;

import android.view.Menu;

import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.media.widget.ExpandedControllerActivity;
import com.stream.jmxplayer.R;
import com.stream.jmxplayer.castconnect.CastServer;
import com.stream.jmxplayer.utils.GlobalFunctions;

import java.io.IOException;


public class ExpandedControlsActivity extends ExpandedControllerActivity {

    CastServer castServer;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.casty_discovery, menu);
        CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.casty_media_route_menu_item);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCastServer();
    }

    public void startCastServer() {
        if (castServer == null) {
            castServer = new CastServer(this);
        }
        try {
            castServer.start();
        } catch (IOException e) {
            GlobalFunctions.Companion.logger("CastServer", e.getMessage());
        }
    }

}
