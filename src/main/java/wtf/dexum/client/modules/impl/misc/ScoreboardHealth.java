package wtf.dexum.client.modules.impl.misc;

import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;

@ModuleAnnotation(
   name = "ScoreboardHealth",
   category = Category.MISC,
   description = "Фиксит хп цели если оно фейк"
)
public class ScoreboardHealth extends Module {
   public static final ScoreboardHealth INSTANCE = new ScoreboardHealth();
}