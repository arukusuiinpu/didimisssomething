package norivensuu.didimisssomething;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(DidIMissSomething.MOD_ID)
public class DidIMissSomethingForge {
	public DidIMissSomethingForge(FMLJavaModLoadingContext context) {

		DidIMissSomething.initialize();
	}
}