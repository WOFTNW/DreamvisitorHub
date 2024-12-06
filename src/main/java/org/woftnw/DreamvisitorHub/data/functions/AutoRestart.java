package org.woftnw.DreamvisitorHub.data.functions;

import org.woftnw.DreamvisitorHub.data.SessionData;
import org.woftnw.DreamvisitorHub.data.api.ApiController;

public class AutoRestart {

    public static void setAutoRestart(boolean state) {
        SessionData.autoRestart = !SessionData.autoRestart;
        ApiController.sendActionToPlugin()
    }

    public static boolean isEnabled() {
        return SessionData.autoRestart;
    }

}
