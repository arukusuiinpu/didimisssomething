package norivensuu.didimisssomething.quilt;

import norivensuu.didimisssomething.DidIMissSomething;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.loader.api.entrypoint.PreLaunchEntrypoint;

public class DidIMissSomethingQuilt implements PreLaunchEntrypoint {

    @Override
    public void onPreLaunch(ModContainer mod) {
        DidIMissSomething.initialize();
    }
}