package wtf.dexum.client.modules.impl.render;

import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;

@ModuleAnnotation(
   name = "FullBright",
   category = Category.RENDER,
   description = "Максимальное освещение"
)
public class FullBright extends Module {
   public static final FullBright INSTANCE = new FullBright();
}