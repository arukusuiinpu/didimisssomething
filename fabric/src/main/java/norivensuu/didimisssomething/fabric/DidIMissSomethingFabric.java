package norivensuu.didimisssomething.fabric;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import norivensuu.didimisssomething.DidIMissSomething;

public class DidIMissSomethingFabric implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        DidIMissSomething.initialize();
    }
}