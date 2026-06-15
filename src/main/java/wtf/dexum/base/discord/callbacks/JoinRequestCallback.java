package wtf.dexum.base.discord.callbacks;

import com.sun.jna.Callback;
import wtf.dexum.base.discord.utils.DiscordUser;

public interface JoinRequestCallback extends Callback {
   void apply(DiscordUser var1);
}