package norivensuu.didimisssomething;

public class DidIMissSomethingQuilt implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        DidIMissSomething.initialize();
    }
}