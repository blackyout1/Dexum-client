package wtf.dexum.base.macro;

import com.google.common.reflect.TypeToken;
import java.util.HashSet;
import java.util.Set;
import ru.nexusguard.protection.annotations.Native;
import wtf.dexum.base.filemanager.api.ManagerFileAbstract;

public class MacroManager extends ManagerFileAbstract<Macro> {
   public MacroManager() {
      super("macro.json", "", (new TypeToken<Set<Macro>>() {
      }).getType(), HashSet::new);
   }

   @Native
   public boolean removeMacro(Macro macro) {
      return this.getItems().remove(macro);
   }
}