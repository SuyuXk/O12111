package meow.binary.o123456789;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2fStack;

/**
 * Shared (client side) code for O123456789.
 *
 * <p>1.21.11 port notes:
 * <ul>
 *     <li>{@code ResourceLocation} was renamed to {@link Identifier}, and fonts are described by a
 *     {@link FontDescription} instead of a bare identifier, so {@link Style#withFont} now takes a
 *     {@link FontDescription.Resource}.</li>
 *     <li>GUI drawing is deferred: {@link GuiGraphics#drawString} only submits a
 *     {@code GuiTextRenderState} which is turned into glyph render states at the end of the frame.
 *     Because of that the old 1.21.1 approach (cancelling {@code Font#drawInternal} and calling
 *     {@code Font#drawInBatch8xOutline}) no longer exists. The outline is instead produced by
 *     submitting the glyphs eight more times, offset by one pixel, in a darkened colour - which is
 *     exactly what {@code drawInBatch8xOutline} did internally.</li>
 * </ul>
 */
public final class O123456789 {
    public static final String MOD_ID = "o123456789";

    public static final FontDescription FONT = new FontDescription.Resource(Identifier.fromNamespaceAndPath(O123456789.MOD_ID, "3x5"));
    public static final FontDescription FONT_TINY = new FontDescription.Resource(Identifier.fromNamespaceAndPath(O123456789.MOD_ID, "3x5_tiny"));

    /** Numbers are never allowed to be wider than a 16x16 item slot minus one pixel of padding. */
    private static final float MAX_TEXT_WIDTH = 15.0F;
    /** How much the outline colour is darkened compared to the text colour. */
    private static final float OUTLINE_DARKEN_FACTOR = 0.22F;

    private O123456789() {
    }

    public static void init() {
        // Nothing to do, everything is done through mixins.
    }

    /**
     * Draws an item count using the Create style 3x5 font, right aligned inside the item slot and
     * scaled down when it does not fit, with a one pixel outline around it.
     *
     * @param graphics the gui graphics of the slot being rendered
     * @param font     the font used for the slot
     * @param text     the vanilla count text
     * @param x        the x position vanilla would have used for the text
     * @param y        the y position vanilla would have used for the text
     * @param color    the text colour
     */
    public static void drawItemCount(GuiGraphics graphics, Font font, String text, int x, int y, int color) {
        Component label = Component.literal(text).withStyle(Style.EMPTY.withFont(O123456789.FONT));
        int textWidth = font.width(label);
        if (textWidth <= 0) {
            return;
        }

        // Vanilla draws the count at (itemX + 19 - 2 - font.width(text), itemY + 6 + 3), so the slot
        // position can be recovered from the coordinates handed to drawString.
        int itemX = x + font.width(text) - 17;
        int itemY = y - 9;

        float scale = Math.min(1.0F, O123456789.MAX_TEXT_WIDTH / (float) textWidth);
        int outlineColor = darken(color);

        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate((float) itemX, (float) itemY);
        pose.scale(scale, scale);

        // Right aligned against the right edge of the slot, vertically anchored like the vanilla count.
        int drawX = Mth.floor(16.0F / scale - (float) textWidth);
        int drawY = Mth.floor(16.0F / scale - 7.0F);

        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetY = -1; offsetY <= 1; offsetY++) {
                if (offsetX != 0 || offsetY != 0) {
                    graphics.drawString(font, label, drawX + offsetX, drawY + offsetY, outlineColor, false);
                }
            }
        }

        graphics.drawString(font, label, drawX, drawY, color, false);
        pose.popMatrix();
    }

    /**
     * Builds the outline colour: same alpha, rgb scaled down by {@link #OUTLINE_DARKEN_FACTOR}.
     */
    public static int darken(int color) {
        int alpha = color >>> 24 & 0xFF;
        int red = (int) ((color >> 16 & 0xFF) * O123456789.OUTLINE_DARKEN_FACTOR) & 0xFF;
        int green = (int) ((color >> 8 & 0xFF) * O123456789.OUTLINE_DARKEN_FACTOR) & 0xFF;
        int blue = (int) ((color & 0xFF) * O123456789.OUTLINE_DARKEN_FACTOR) & 0xFF;
        return alpha << 24 | red << 16 | green << 8 | blue;
    }
}
