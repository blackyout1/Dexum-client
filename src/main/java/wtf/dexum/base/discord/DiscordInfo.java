package wtf.dexum.base.discord;

public record DiscordInfo(String userName, String avatarUrl, String userId) {
   public DiscordInfo(String userName, String avatarUrl, String userId) {
      this.userName = userName;
      this.avatarUrl = avatarUrl;
      this.userId = userId;
   }

   public String userName() {
      return this.userName;
   }

   public String avatarUrl() {
      return this.avatarUrl;
   }

   public String userId() {
      return this.userId;
   }
}