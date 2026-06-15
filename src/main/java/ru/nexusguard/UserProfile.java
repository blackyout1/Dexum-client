package ru.nexusguard;

import lombok.Generated;

public class UserProfile implements IGuard {
   public static UserProfile instance = new UserProfile();
   public UserProfile.IRCProfile irc = new UserProfile.IRCProfile((String)null);
   public static String username = "NexusUser";
   public static String hwid = "123";
   public static int uid = 1337;
   public static String role = "DEV";

   public String username() {
      return username;
   }

   public String hwid() {
      return hwid;
   }

   public String role() {
      return role;
   }

   public int uid() {
      return uid;
   }

   public String roleName() {
      String var1 = role;
      byte var2 = -1;
      switch(var1.hashCode()) {
      case 67573:
         if (var1.equals("DEV")) {
            var2 = 1;
         }
         break;
      case 2035184:
         if (var1.equals("BETA")) {
            var2 = 4;
         }
         break;
      case 2094806:
         if (var1.equals("DEV+")) {
            var2 = 0;
         }
         break;
      case 62130991:
         if (var1.equals("ADMIN")) {
            var2 = 2;
         }
         break;
      case 73234372:
         if (var1.equals("MEDIA")) {
            var2 = 3;
         }
      }

      switch(var2) {
      case 0:
      case 1:
         return "Разработчик";
      case 2:
         return "Администратор";
      case 3:
         return "Медиа";
      case 4:
         return "Бета";
      default:
         return "Пользователь";
      }
   }

   public static class IRCProfile {
      private String prefix;

      public String getPrefix() {
         return this.prefix == null ? this.getRoleName() : this.prefix;
      }

      public void setPrefix(String newPrefix) {
         this.prefix = newPrefix;
      }

      public String getRoleName() {
         String var1 = UserProfile.role;
         byte var2 = -1;
         switch(var1.hashCode()) {
         case 67573:
            if (var1.equals("DEV")) {
               var2 = 1;
            }
            break;
         case 2035184:
            if (var1.equals("BETA")) {
               var2 = 4;
            }
            break;
         case 2094806:
            if (var1.equals("DEV+")) {
               var2 = 0;
            }
            break;
         case 62130991:
            if (var1.equals("ADMIN")) {
               var2 = 2;
            }
            break;
         case 73234372:
            if (var1.equals("MEDIA")) {
               var2 = 3;
            }
         }

         switch(var2) {
         case 0:
         case 1:
            return "§cРазработчик";
         case 2:
            return "§4Администратор";
         case 3:
            return "§cМедиа";
         case 4:
            return "§9Бета";
         default:
            return "§7Пользователь";
         }
      }

      @Generated
      public IRCProfile(String prefix) {
         this.prefix = prefix;
      }
   }
}