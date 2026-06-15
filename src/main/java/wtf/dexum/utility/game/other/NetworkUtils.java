package wtf.dexum.utility.game.other;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.packet.Packet;
import ru.nexusguard.protection.annotations.Native;
import wtf.dexum.utility.interfaces.IMinecraft;

public class NetworkUtils implements IMinecraft {
   private static final List<Packet<?>> silentPackets = new ArrayList();

   @Native
   public static void sendSilentPacket(Packet<?> packet) {
      silentPackets.add(packet);
      mc.getNetworkHandler().sendPacket(packet);
   }

   public static void sendPacket(Packet<?> packet) {
      mc.getNetworkHandler().sendPacket(packet);
   }

   public static List<Packet<?>> getSilentPackets() {
      return silentPackets;
   }

   public static void clearSilentPackets() {
      silentPackets.clear();
   }
}