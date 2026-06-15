package wtf.dexum.utility.game.server;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import lombok.Generated;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.StringUtils;
import wtf.dexum.base.events.impl.player.EventUpdate;
import wtf.dexum.base.events.impl.server.EventPacket;
import wtf.dexum.utility.game.player.PlayerIntersectionUtil;
import wtf.dexum.utility.interfaces.IMinecraft;
import wtf.dexum.utility.math.Timer;

public class ServerHandler implements IMinecraft {
   private final Timer pvpWatch = new Timer();
   private String server = "Vanilla";
   private float TPS = 20.0F;
   private long timestamp;
   private boolean serverSprint;
   private int anarchy;
   private boolean pvpEnd;

   public static Instant getTimeFromHttpDate(String urlStr, int timeoutMillis) throws Exception {
      URL url = new URL(urlStr);
      HttpURLConnection conn = (HttpURLConnection)url.openConnection();
      conn.setRequestMethod("HEAD");
      conn.setConnectTimeout(timeoutMillis);
      conn.setReadTimeout(timeoutMillis);
      conn.setInstanceFollowRedirects(true);
      conn.connect();
      String dateHeader = conn.getHeaderField("Date");
      conn.disconnect();
      if (dateHeader == null) {
         throw new RuntimeException("No Date header from " + urlStr);
      } else {
         Instant instant = (Instant)DateTimeFormatter.RFC_1123_DATE_TIME.parse(dateHeader, Instant::from);
         return instant;
      }
   }

   public static boolean isToday24UsingHttp(String url) {
      try {
         Instant t = getTimeFromHttpDate(url, 3000);
         ZoneId zone = ZoneId.of("Europe/Kyiv");
         LocalDate date = LocalDateTime.ofInstant(t, zone).toLocalDate();
         return (date.getDayOfMonth() == 4 || date.getDayOfMonth() == 5) && date.getMonthValue() == 12;
      } catch (Exception var4) {

         return false;
      }
   }

   public ServerHandler() {

      try {
         EventManager.register(this);
      } catch (Exception e) {

      }
   }

   @EventTarget
   public void tick(EventUpdate eventUpdate) {
      this.anarchy = this.getAnarchyMode();
      this.server = this.updateServer();
      this.pvpEnd = this.inPvpEnd();
      if (this.inPvp()) {
         this.pvpWatch.reset();
      }

   }

   @EventTarget
   public void packet(EventPacket e) {
      if (e.getPacket() instanceof WorldTimeUpdateS2CPacket) {
         long nanoTime = System.nanoTime();
         float maxTPS = 20.0F;
         float rawTPS = maxTPS * (1.0E9F / (float)(nanoTime - this.timestamp));
         this.TPS = MathHelper.clamp(rawTPS, 0.0F, maxTPS);
         this.timestamp = nanoTime;
      }

   }

   @EventTarget
   public void onPacket(EventPacket e) {
      Packet var3 = e.getPacket();
      if (var3 instanceof ClientCommandC2SPacket) {
         ClientCommandC2SPacket command = (ClientCommandC2SPacket)var3;
         if (command.getMode().equals(Mode.START_SPRINTING)) {
            e.setCancelled(this.serverSprint);
            this.serverSprint = true;
         } else if (command.getMode().equals(Mode.STOP_SPRINTING)) {
            e.setCancelled(!this.serverSprint);
            this.serverSprint = false;
         }
      }

   }

   private String updateServer() {
      if (!PlayerIntersectionUtil.nullCheck() && mc.getNetworkHandler() != null && mc.getNetworkHandler().getServerInfo() != null && mc.getNetworkHandler().getBrand() != null) {
         String serverIp = mc.getNetworkHandler().getServerInfo().address.toLowerCase();
         String brand = mc.getNetworkHandler().getBrand().toLowerCase();
         if (brand.contains("botfilter")) {
            return "FunTime";
         } else if (!serverIp.contains("funtime") && !serverIp.contains("skytime") && !serverIp.contains("space-times") && !serverIp.contains("funsky")) {
            if (!brand.contains("holyworld") && !brand.contains("leaf") && !brand.contains("vk.com/idwok")) {
               if (serverIp.contains("lonygrief")) {
                  return "LonyGrief";
               } else {
                  return serverIp.contains("reallyworld") ? "ReallyWorld" : "Vanilla";
               }
            } else {
               return "HolyWorld";
            }
         } else {
            return "CopyTime";
         }
      } else {
         return "Vanilla";
      }
   }

   private int getAnarchyMode() {
      Scoreboard scoreboard = mc.world.getScoreboard();
      ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
      String var3 = this.server;
      byte var4 = -1;
      switch(var3.hashCode()) {
      case -495240450:
         if (var3.equals("HolyWorld")) {
            var4 = 1;
         }
         break;
      case 1154553036:
         if (var3.equals("FunTime")) {
            var4 = 0;
         }
      }

      switch(var4) {
      case 0:
         if (objective != null) {
            String[] string = objective.getDisplayName().getString().split("-");
            if (string.length > 1) {
               return Integer.parseInt(string[1]);
            }
         }
         break;
      case 1:
         Iterator var5 = scoreboard.getScoreboardEntries(objective).iterator();

         while(var5.hasNext()) {
            ScoreboardEntry scoreboardEntry = (ScoreboardEntry)var5.next();
            String text = Team.decorateName(scoreboard.getScoreHolderTeam(scoreboardEntry.owner()), scoreboardEntry.name()).getString();
            if (!text.isEmpty()) {
               String string = StringUtils.substringBetween(text, "#", " -◆-");
               if (string != null && !string.isEmpty()) {
                  return Integer.parseInt(string);
               }
            }
         }
      }

      return -1;
   }

   public boolean isPvp() {
      return !this.pvpWatch.finished(250L);
   }

   private boolean inPvp() {
      return mc.inGameHud.getBossBarHud().bossBars.values().stream().map((c) -> {
         return c.getName().getString().toLowerCase();
      }).anyMatch((s) -> {
         return s.contains("pvp") || s.contains("пвп");
      });
   }

   private boolean inPvpEnd() {
      return mc.inGameHud.getBossBarHud().bossBars.values().stream().map((c) -> {
         return c.getName().getString().toLowerCase();
      }).anyMatch((s) -> {
         return (s.contains("pvp") || s.contains("пвп")) && (s.contains("0") || s.contains("1"));
      });
   }

   public String getWorldType() {
      return mc.world.getRegistryKey().getValue().getPath();
   }

   public boolean isCopyTime() {
      return this.server.equals("CopyTime") || this.server.equals("SpookyTime") || this.server.equals("FunTime");
   }

   public boolean isFunTime() {
      return this.server.equals("FunTime");
   }

   public boolean isReallyWorld() {
      return this.server.equals("ReallyWorld");
   }

   public boolean isHolyWorld() {
      return this.server.equals("HolyWorld");
   }

   public boolean isVanilla() {
      return this.server.equals("Vanilla");
   }

   @Generated
   public Timer getPvpWatch() {
      return this.pvpWatch;
   }

   @Generated
   public String getServer() {
      return this.server;
   }

   @Generated
   public float getTPS() {
      return this.TPS;
   }

   @Generated
   public long getTimestamp() {
      return this.timestamp;
   }

   @Generated
   public boolean isServerSprint() {
      return this.serverSprint;
   }

   @Generated
   public int getAnarchy() {
      return this.anarchy;
   }

   @Generated
   public boolean isPvpEnd() {
      return this.pvpEnd;
   }
}