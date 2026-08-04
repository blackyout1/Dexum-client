package wtf.dexum.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4d;
import org.joml.Vector4f;
import wtf.dexum.Dexum;
import wtf.dexum.base.events.impl.render.EventRender2D;
import wtf.dexum.base.events.impl.render.EventRender3D;
import wtf.dexum.base.font.Fonts;
import wtf.dexum.client.modules.api.Category;
import wtf.dexum.client.modules.api.Module;
import wtf.dexum.client.modules.api.ModuleAnnotation;
import wtf.dexum.client.modules.api.setting.impl.BooleanSetting;
import wtf.dexum.client.modules.impl.misc.NameProtect;
import wtf.dexum.client.modules.impl.misc.ScoreboardHealth;
import wtf.dexum.utility.game.other.ReplaceUtil;
import wtf.dexum.utility.game.player.PlayerIntersectionUtil;
import wtf.dexum.utility.math.ProjectionUtil;
import wtf.dexum.utility.render.display.base.BorderRadius;
import wtf.dexum.utility.render.display.base.color.ColorRGBA;
import wtf.dexum.utility.render.display.shader.DrawUtil;
import wtf.dexum.utility.render.level.Render3DUtil;

@ModuleAnnotation(
        name = "EntityESP (Deprecated)",
        category = Category.RENDER,
        description = "Устарел - используйте NameTags"
)
public final class EntityESP extends Module {
    public static final EntityESP INSTANCE = new EntityESP();
    private final HashMap<Entity, Vector4f> positions = new HashMap();
    private final BooleanSetting box3D = new BooleanSetting("3D Box", false);

    // Весь рендеринг теперь в NameTags модуле
    // Этот модуль оставлен для совместимости, но ничего не делает

    // Весь рендеринг теперь в NameTags модуле
    // Этот модуль оставлен для совместимости, но ничего не делает
}