package norivensuu.didimisssomething;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public class DidIMissSomethingFabric implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        DidIMissSomething.initialize();
    }
}