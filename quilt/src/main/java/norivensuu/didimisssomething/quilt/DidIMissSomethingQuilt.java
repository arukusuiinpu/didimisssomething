package norivensuu.didimisssomething.quilt;

import norivensuu.didimisssomething.DidIMissSomething;
import org.quiltmc.loader.api.entrypoint.PreLaunchEntrypoint;

public abstract class DidIMissSomethingQuilt implements PreLaunchEntrypoint {

    public void onPreLaunch() {
        DidIMissSomething.initialize();
    }
}