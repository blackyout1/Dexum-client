package wtf.dexum.base.discord.callbacks;

import com.sun.jna.Callback;
import wtf.dexum.base.discord.utils.DiscordUser;

public interface ReadyCallback extends Callback {
   void apply(DiscordUser var1);
}