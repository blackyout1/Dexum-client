package wtf.dexum.base.filemanager.api;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.function.Supplier;
import lombok.Generated;
import wtf.dexum.Dexum;
import wtf.dexum.utility.crypt.CryptUtility;

public class ManagerFileAbstract<T> {
   private Collection<T> items;
   private final String fileName;
   private final String shifr;
   private final Type type;
   private final Supplier<Collection<T>> collectionSupplier;

   public ManagerFileAbstract(String fileName, String shifr, Type type, Supplier<Collection<T>> collectionSupplier) {
      this.fileName = fileName;
      this.shifr = shifr;
      this.type = type;
      this.collectionSupplier = collectionSupplier;
      File file = new File(Dexum.DIRECTORY, fileName);
      if (!file.exists()) {
         try {
            file.createNewFile();
            this.items = (Collection)collectionSupplier.get();
         } catch (Exception var7) {
            this.items = (Collection)collectionSupplier.get();
         }
      } else {
         this.load();
      }

   }

   public void save() {
      Gson gson = new Gson();
      String json = gson.toJson(this.items);

      try {
         FileWriter writer = new FileWriter(new File(Dexum.DIRECTORY, this.fileName));

         try {
            writer.write(this.shifr.isEmpty() ? json : Base64.getEncoder().encodeToString(CryptUtility.encryptData(json.getBytes(), this.shifr)));
         } catch (Throwable var7) {
            try {
               writer.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }

            throw var7;
         }

         writer.close();
      } catch (Exception var8) {
      }

   }

   public void load() {
      try {
         BufferedReader reader = new BufferedReader(new FileReader(new File(Dexum.DIRECTORY, this.fileName)));

         try {
            Gson gson = new Gson();
            if (!this.shifr.isEmpty()) {
               String encryptedDataBase64 = reader.readLine();
               if (encryptedDataBase64 != null && !encryptedDataBase64.isEmpty()) {
                  byte[] encryptedData = Base64.getDecoder().decode(encryptedDataBase64);
                  byte[] decryptedData = CryptUtility.decryptData(encryptedData, this.shifr);
                  String json = new String(decryptedData, StandardCharsets.UTF_8);
                  this.items = (Collection)gson.fromJson(json, this.type);
               } else {
                  this.items = (Collection)this.collectionSupplier.get();
               }
            } else {
               this.items = (Collection)gson.fromJson(reader, this.type);
            }

            if (this.items == null) {
               this.items = (Collection)this.collectionSupplier.get();
            }
         } catch (Throwable var8) {
            try {
               reader.close();
            } catch (Throwable var7) {
               var8.addSuppressed(var7);
            }

            throw var8;
         }

         reader.close();
      } catch (Exception var9) {
         this.items = (Collection)this.collectionSupplier.get();
      }

   }

   public void add(T item) {
      this.items.add(item);
   }

   public void remove(T item) {
      this.items.remove(item);
   }

   @Generated
   public Collection<T> getItems() {
      return this.items;
   }
}