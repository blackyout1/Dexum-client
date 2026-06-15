package wtf.dexum.base.config;

import com.darkmagician6.eventapi.EventManager;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.Generated;
import ru.nexusguard.protection.annotations.Native;
import wtf.dexum.Dexum;
import wtf.dexum.utility.crypt.CryptUtility;

public class ConfigManager {
   public static final File configDirectory;
   ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
   private final String shifr = "config";

   public ConfigManager() {
      configDirectory.mkdirs();
      this.loadConfig("current_config");
      EventManager.register(this);
      this.scheduler.scheduleAtFixedRate(() -> {
         try {
            this.saveConfig("current_config");
         } catch (Exception var2) {
         }

      }, 5L, 5L, TimeUnit.MINUTES);
   }

   @Native
   public boolean loadConfig(String configName) {
      if (configName == null) {
         return false;
      } else {
         Config config = this.findConfig(configName);
         if (config == null) {
            return false;
         } else {
            try {
               BufferedReader reader = new BufferedReader(new FileReader(config.getFile()));

               boolean var10;
               try {
                  JsonParser parser = new JsonParser();
                  String encryptedDataBase64 = reader.readLine();
                  byte[] encryptedData = Base64.getDecoder().decode(encryptedDataBase64);
                  byte[] decryptedData = CryptUtility.decryptData(encryptedData, "config");
                  String json = new String(decryptedData, StandardCharsets.UTF_8);
                  JsonObject object = (JsonObject)parser.parse(json);
                  config.load(object);
                  var10 = true;
               } catch (Throwable var12) {
                  try {
                     reader.close();
                  } catch (Throwable var11) {
                     var12.addSuppressed(var11);
                  }

                  throw var12;
               }

               reader.close();
               return var10;
            } catch (Exception var13) {

               return false;
            }
         }
      }
   }

   @Native
   public boolean saveConfig(String configName) {
      try {
         if (configName == null) {
            return false;
         } else {
            Config config;
            if ((config = this.findConfig(configName)) == null) {
               config = new Config(configName);
            }

            String contentPrettyPrint = (new GsonBuilder()).setPrettyPrinting().create().toJson(config.save());
            contentPrettyPrint = Base64.getEncoder().encodeToString(CryptUtility.encryptData(contentPrettyPrint.getBytes(), "config"));

            try {
               FileWriter writer = new FileWriter(config.getFile());
               writer.write(contentPrettyPrint);
               writer.close();
               return true;
            } catch (IOException var5) {
               return false;
            }
         }
      } catch (Exception var6) {
         return false;
      }
   }

   @Native
   public Config findConfig(String configName) {
      if (configName == null) {
         return null;
      } else {
         return (new File(configDirectory, configName + "." + "dexum".toLowerCase())).exists() ? new Config(configName) : null;
      }
   }

   @Native
   public List<String> configNames() {
      File[] files = configDirectory.listFiles();
      List<String> names = new ArrayList();
      File[] var3 = files;
      int var4 = files.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         File file = var3[var5];
         names.add(file.getName());
      }

      return names;
   }

   @Native
   public boolean deleteConfig(String configName) {
      if (configName == null) {
         return false;
      } else {
         Config config;
         if ((config = this.findConfig(configName)) == null) {
            return false;
         } else {
            File f = config.getFile();
            return f.exists() && f.delete();
         }
      }
   }

   @Native
   public void save() {
      this.scheduler.shutdown();
      this.saveConfig("current_config");
   }

   @Generated
   public ScheduledExecutorService getScheduler() {
      return this.scheduler;
   }

   @Generated
   public String getShifr() {
      Objects.requireNonNull(this);
      return "config";
   }

   static {
      configDirectory = new File(Dexum.DIRECTORY, "configs");
   }
}