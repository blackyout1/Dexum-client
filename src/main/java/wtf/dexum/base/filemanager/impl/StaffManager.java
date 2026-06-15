package wtf.dexum.base.filemanager.impl;

import com.google.common.reflect.TypeToken;
import java.util.HashSet;
import java.util.Set;
import wtf.dexum.base.filemanager.api.ManagerFileAbstract;

public class StaffManager extends ManagerFileAbstract<String> {
   public StaffManager() {
      super("staffName.json", "", (new TypeToken<Set<String>>() {
      }).getType(), HashSet::new);
   }

   public boolean isStaff(String staffName) {
      return this.getItems().contains(staffName);
   }
}