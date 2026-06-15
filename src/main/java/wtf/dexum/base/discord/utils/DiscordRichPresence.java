package wtf.dexum.base.discord.utils;

import com.sun.jna.Structure;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DiscordRichPresence extends Structure {
   public String largeImageKey;
   public String largeImageText;
   public String smallImageText;
   public String partyPrivacy;
   public long startTimestamp;
   public int instance;
   public String partyId;
   public int partySize;
   public long endTimestamp;
   public String details;
   public String joinSecret;
   public String spectateSecret;
   public String smallImageKey;
   public String matchSecret;
   public String state;
   public int partyMax;
   public String button_url_1;
   public String button_label_1;
   public String button_url_2;
   public String button_label_2;

   public DiscordRichPresence() {
      this.setStringEncoding("UTF-8");
   }

   protected List<String> getFieldOrder() {
      return Arrays.asList("state", "details", "startTimestamp", "endTimestamp", "largeImageKey", "largeImageText", "smallImageKey", "smallImageText", "partyId", "partySize", "partyMax", "partyPrivacy", "matchSecret", "joinSecret", "spectateSecret", "button_label_1", "button_url_1", "button_label_2", "button_url_2", "instance");
   }

   public static class Builder {
      private final DiscordRichPresence richPresence = new DiscordRichPresence();

      public DiscordRichPresence.Builder setSmallImage(String key) {
         return this.setSmallImage(key, "");
      }

      public DiscordRichPresence.Builder setDetails(String details) {
         if (details != null && !details.isEmpty()) {
            this.richPresence.details = details.substring(0, Math.min(details.length(), 128));
         }

         return this;
      }

      public DiscordRichPresence.Builder setLargeImage(String key, String text) {
         this.richPresence.largeImageKey = key;
         this.richPresence.largeImageText = text;
         return this;
      }

      public DiscordRichPresence.Builder setState(String state) {
         if (state != null && !state.isEmpty()) {
            this.richPresence.state = state.substring(0, Math.min(state.length(), 128));
         }

         return this;
      }

      public DiscordRichPresence.Builder setInstance(boolean instance) {
         if ((this.richPresence.button_label_1 == null || !this.richPresence.button_label_1.isEmpty()) && (this.richPresence.button_label_2 == null || !this.richPresence.button_label_2.isEmpty())) {
            this.richPresence.instance = instance ? 1 : 0;
         }

         return this;
      }

      public DiscordRichPresence.Builder setButtons(RPCButton button) {
         return this.setButtons(Collections.singletonList(button));
      }

      public DiscordRichPresence.Builder setSmallImage(String key, String text) {
         this.richPresence.smallImageKey = key;
         this.richPresence.smallImageText = text;
         return this;
      }

      public DiscordRichPresence.Builder setButtons(List<RPCButton> buttons) {
         if (buttons != null && !buttons.isEmpty()) {
            int count = Math.min(buttons.size(), 2);
            this.richPresence.button_label_1 = ((RPCButton)buttons.get(0)).getLabel();
            this.richPresence.button_url_1 = ((RPCButton)buttons.get(0)).getUrl();
            if (count == 2) {
               this.richPresence.button_label_2 = ((RPCButton)buttons.get(1)).getLabel();
               this.richPresence.button_url_2 = ((RPCButton)buttons.get(1)).getUrl();
            }
         }

         return this;
      }

      public DiscordRichPresence.Builder setStartTimestamp(OffsetDateTime timestamp) {
         this.richPresence.startTimestamp = timestamp.toEpochSecond();
         return this;
      }

      public DiscordRichPresence.Builder setSecrets(String matchSecret, String joinSecret, String spectateSecret) {
         if ((this.richPresence.button_label_1 == null || !this.richPresence.button_label_1.isEmpty()) && (this.richPresence.button_label_2 == null || !this.richPresence.button_label_2.isEmpty())) {
            this.richPresence.matchSecret = matchSecret;
            this.richPresence.joinSecret = joinSecret;
            this.richPresence.spectateSecret = spectateSecret;
         }

         return this;
      }

      public DiscordRichPresence.Builder setButtons(RPCButton button1, RPCButton button2) {
         this.setButtons(Arrays.asList(button1, button2));
         return this;
      }

      public DiscordRichPresence.Builder setStartTimestamp(long timestamp) {
         this.richPresence.startTimestamp = timestamp;
         return this;
      }

      public DiscordRichPresence.Builder setSecrets(String joinSecret, String spectateSecret) {
         if ((this.richPresence.button_label_1 == null || !this.richPresence.button_label_1.isEmpty()) && (this.richPresence.button_label_2 == null || !this.richPresence.button_label_2.isEmpty())) {
            this.richPresence.joinSecret = joinSecret;
            this.richPresence.spectateSecret = spectateSecret;
         }

         return this;
      }

      public DiscordRichPresence.Builder setEndTimestamp(long timestamp) {
         this.richPresence.endTimestamp = timestamp;
         return this;
      }

      public DiscordRichPresence.Builder setEndTimestamp(OffsetDateTime timestamp) {
         this.richPresence.endTimestamp = timestamp.toEpochSecond();
         return this;
      }

      public DiscordRichPresence.Builder setLargeImage(String key) {
         return this.setLargeImage(key, "");
      }

      public DiscordRichPresence build() {
         return this.richPresence;
      }
   }
}