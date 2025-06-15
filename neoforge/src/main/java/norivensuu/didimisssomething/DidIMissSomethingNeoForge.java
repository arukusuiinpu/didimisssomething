package norivensuu.didimisssomething;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.javafmlmod.FMLModContainer;

@Mod(value = DidIMissSomething.MOD_ID, dist = Dist.CLIENT)
public class DidIMissSomethingNeoForge {
    public DidIMissSomethingNeoForge(FMLModContainer container, IEventBus modBus, Dist dist) {
        DidIMissSomething.initialize();
    }
}