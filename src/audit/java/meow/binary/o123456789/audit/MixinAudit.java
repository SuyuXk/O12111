package meow.binary.o123456789.audit;

import net.fabricmc.api.EnvType;

/**
 * Development only helper: boots the Fabric Knot class loader in a client environment, loads
 * {@code net.minecraft.client.gui.GuiGraphics} (the mixin target) and reports whether the mixin was
 * applied, without opening a window or downloading assets.
 *
 * <p>This class is not part of the released mod; it only exists to smoke test mixin application.
 */
public final class MixinAudit {
    public static void main(String[] args) throws Exception {
        // Same bootstrap that KnotClient does, but without invoking the Minecraft entrypoint.
        Class<?> knotClass = Class.forName("net.fabricmc.loader.impl.launch.knot.Knot");
        Object knot = knotClass.getConstructor(EnvType.class).newInstance(EnvType.CLIENT);
        ClassLoader loader = (ClassLoader) knotClass.getMethod("init", String[].class).invoke(knot, (Object) new String[0]);
        System.out.println("[audit] knot loader = " + loader);
        System.out.println("[audit] environment = " + org.spongepowered.asm.mixin.MixinEnvironment.getCurrentEnvironment());

        Class<?> guiGraphics = Class.forName("net.minecraft.client.gui.GuiGraphics", false, loader);
        System.out.println("[audit] loaded " + guiGraphics.getName() + " via " + guiGraphics.getClassLoader());

        boolean found = false;
        for (java.lang.reflect.Method method : guiGraphics.getDeclaredMethods()) {
            if (method.getName().startsWith("renderItemCount") || method.getName().startsWith("method_64859")) {
                found = true;
                System.out.println("[audit] target method = " + method.getName() + java.util.Arrays.toString(method.getParameterTypes()));
            }
        }

        Boolean handlerPresent = null;
        for (java.lang.reflect.Method method : guiGraphics.getDeclaredMethods()) {
            if (method.getName().contains("drawItemCount")) {
                handlerPresent = true;
                System.out.println("[audit] merged redirect handler = " + method.getName());
            }
        }

        System.out.println("[audit] target method present = " + found);
        if (!found || handlerPresent == null) {
            throw new IllegalStateException("mixin was not applied: target=" + found + " handler=" + handlerPresent);
        }

        System.out.println("[audit] outline colour for 0xFFFFFFFF = 0x"
                + Integer.toHexString(meow.binary.o123456789.O123456789.darken(-1)).toUpperCase());
        System.out.println("[audit] OK");
    }
}
