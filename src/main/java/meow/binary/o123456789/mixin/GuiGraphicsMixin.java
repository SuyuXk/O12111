package meow.binary.o123456789.mixin;

import meow.binary.o123456789.O123456789;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    /**
     * Replaces the vanilla item count drawing with the 3x5 font version.
     *
     * <p>1.21.11 moved the count rendering out of {@code renderItemDecorations} into the private
     * {@code renderItemCount} helper, which is therefore the method that now owns the
     * {@code drawString} call that has to be redirected.
     */
    @Redirect(
            method = "renderItemCount",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V"
            )
    )
    private void o123456789$drawItemCount(GuiGraphics graphics, Font font, String text, int x, int y, int color, boolean dropShadow) {
        O123456789.drawItemCount(graphics, font, text, x, y, color);
    }
}
