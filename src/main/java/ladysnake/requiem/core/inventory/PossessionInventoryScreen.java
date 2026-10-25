/*
 * Requiem
 * Copyright (C) 2017-2023 Ladysnake
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * License for more details.
 *
 * You should have received a copy of the GNU License
 * along with this program. If not, see <https://www.gnu.org/licenses>.
 *
 * Linking this mod statically or dynamically with other
 * modules is making a combined work based on this mod.
 * Thus, the terms and conditions of the GNU General Public License cover the whole combination.
 *
 * In addition, as a special exception, the copyright holders of
 * this mod give you permission to combine this mod
 * with free software programs or libraries that are released under the GNU LGPL
 * and with code included in the standard release of Minecraft under All Rights Reserved (or
 * modified versions of such code, with unchanged license).
 * You may copy and distribute such a system following the terms of the GNU GPL for this mod
 * and the licenses of the other code concerned.
 *
 * Note that people who make modified versions of this mod are not obligated to grant
 * this special exception for their modified versions; it is their choice whether to do so.
 * The GNU General Public License gives permission to release a modified version without this exception;
 * this exception also makes it possible to release a modified version which carries forward this exception.
 */
package ladysnake.requiem.core.inventory;

import com.mojang.blaze3d.systems.RenderSystem;
import ladysnake.requiem.api.v1.entity.InventoryLimiter;
import ladysnake.requiem.api.v1.entity.InventoryShape;
import ladysnake.requiem.api.v1.possession.PossessionComponent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;

public class PossessionInventoryScreen extends AbstractContainerScreen<InventoryMenu> {
    private static final Identifier INVENTORY_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/inventory.png");
    private final Player player;

    public PossessionInventoryScreen(Player player) {
        super(player.inventoryMenu, player.getInventory(), Component.translatable("container.crafting"));
        this.player = player;
    }

    @Override
    protected void init() {
        if (!this.trySwapInventoryScreen()) {
            super.init();
            this.leftPos = (this.width - this.imageWidth) / 2;
        }
    }

    @Override
    public void containerTick() {
        this.trySwapInventoryScreen();
    }

    private boolean trySwapInventoryScreen() {
        assert this.minecraft != null;
        assert this.minecraft.player != null;

        if (this.minecraft.player.hasInfiniteMaterials()) {
            this.minecraft.gui.setScreen(new CreativeModeInventoryScreen(
                this.minecraft.player,
                this.minecraft.player.connection.enabledFeatures(),
                this.minecraft.options.operatorItemsTab().get()
            ));
            return true;
        } else if (InventoryLimiter.instance().getInventoryShape(this.player) != InventoryShape.ALT_LARGE) {
            this.minecraft.gui.setScreen(new InventoryScreen(this.minecraft.player));
            return true;
        }
        return false;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        Mob possessedEntity = PossessionComponent.getHost(this.player);
        Component title = possessedEntity != null ? possessedEntity.getName() : this.title;
        guiGraphics.text(this.font, title, this.titleLabelX, this.titleLabelY, 0x404040, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(guiGraphics, mouseX, mouseY, delta);
        assert this.minecraft != null;
        assert this.minecraft.player != null;

        InventoryShape shape = InventoryLimiter.instance().getInventoryShape(this.player);
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, shape.swapBackground(INVENTORY_LOCATION), x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        shape.setupEntityCrop(guiGraphics, this.leftPos, this.topPos);
        int entityCenterX = (int) shape.shiftEntityX(x + 51);
        int entityCenterY = (int) shape.shiftEntityY(y + 75);
        InventoryScreen.extractEntityInInventoryFollowsMouse(
            guiGraphics,
            entityCenterX - 25,
            entityCenterY - 60,
            entityCenterX + 25,
            entityCenterY,
            30,
            0.0625F,
            (float) mouseX,
            (float) mouseY,
            this.minecraft.player
        );
        shape.tearDownEntityCrop(guiGraphics);
    }
}
