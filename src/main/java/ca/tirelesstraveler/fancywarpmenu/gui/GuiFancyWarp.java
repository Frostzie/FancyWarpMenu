/*
 * Copyright (c) 2023. TirelessTraveler
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 */

package ca.tirelesstraveler.fancywarpmenu.gui;

import ca.tirelesstraveler.fancywarpmenu.FancyWarpMenu;
import ca.tirelesstraveler.fancywarpmenu.data.Island;
import ca.tirelesstraveler.fancywarpmenu.data.Settings;
import ca.tirelesstraveler.fancywarpmenu.data.Warp;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;

public class GuiFancyWarp extends GuiScreen {
    private ScaledResolution res;
    private float gridUnitWidth;
    private float gridUnitHeight;
    private boolean showDebugOverlay;

    @Override
    public void initGui() {
        res = new ScaledResolution(mc);
        gridUnitWidth = (float) res.getScaledWidth() / Island.GRID_UNIT_WIDTH_FACTOR;
        gridUnitHeight = (float) res.getScaledHeight() / Island.GRID_UNIT_HEIGHT_FACTOR;
        showDebugOverlay = true;
        Warp.initDefaults(res);

        for (Island island: FancyWarpMenu.getInstance().getIslands()) {
            GuiIslandButton islandButton = new GuiIslandButton(this, buttonList.size(), res, island);
            buttonList.add(islandButton);

            for (Warp warp: island.getWarps()) {
                buttonList.add(new GuiWarpButton(buttonList.size(), islandButton, warp));
            }
        }
    }

    /**
     * Draws the screen and all the components in it. Args : mouseX, mouseY, renderPartialTicks
     */
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);

        if (Settings.isDebugModeEnabled() && showDebugOverlay) {
            ArrayList<String> debugStrings = new ArrayList<>();
            int drawX;
            int drawY;
            int nearestX;
            int nearestY;
            boolean tooltipDrawn = false;
            // Draw screen resolution
            drawCenteredString(mc.fontRendererObj, String.format("%d x %d", res.getScaledWidth(), res.getScaledHeight()), width / 2, height - 20, 14737632);
            // Draw version number
            String modName = FancyWarpMenu.getInstance().getModContainer().getName();
            String modVersion = FancyWarpMenu.getInstance().getModContainer().getVersion();
            drawCenteredString(mc.fontRendererObj, modName + " " + modVersion, width / 2, height - 10, 14737632);

            for (GuiButton button:
                 buttonList) {
                //Draw borders
                if (button instanceof GuiButtonExt) {
                    ((GuiButtonExt) button).drawBorders(1, 1);
                }
            }

            // Shift to draw island grid instead of warp grid
            if (!isShiftKeyDown()) {
                for (GuiButton button:
                        buttonList) {
                    // Draw island button coordinate tooltips, draw last to prevent clipping
                    if (!tooltipDrawn && button instanceof GuiIslandButton && button.isMouseOver()) {
                        GuiIslandButton islandButton = (GuiIslandButton) button;
                        debugStrings.add(EnumChatFormatting.GREEN + button.displayString);
                        nearestX = islandButton.findNearestGridX(mouseX);
                        nearestY = islandButton.findNearestGridY(mouseY);
                        drawX = islandButton.getActualX(nearestX);
                        drawY = islandButton.getActualY(nearestY);
                        drawDebugStrings(debugStrings, drawX, drawY, nearestX, nearestY);
                        tooltipDrawn = true;
                    }
                }
            }

            // Draw screen coordinate tooltips
            if (!tooltipDrawn) {
                nearestX = findNearestGridX(mouseX);
                nearestY = findNearestGridY(mouseY);
                drawX = getActualX(nearestX);
                drawY = getActualY(nearestY);
                drawDebugStrings(debugStrings, drawX, drawY, nearestX, nearestY);
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button instanceof GuiWarpButton) {
            sendChatMessage(((GuiWarpButton)button).getWarpCommand());
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        FancyWarpMenu mod = FancyWarpMenu.getInstance();

        if (Settings.isDebugModeEnabled()) {
            if (keyCode == Keyboard.KEY_R) {
                mod.reloadResources();
                buttonList.clear();
                initGui();
            } else if (keyCode == Keyboard.KEY_TAB) {
                showDebugOverlay = !showDebugOverlay;
            }
        }
    }

    int getActualX(int gridX) {
        return Math.round(gridUnitWidth * gridX);
    }

    int getActualY(int gridY) {
        return Math.round(gridUnitHeight * gridY);
    }

    private int findNearestGridX(int mouseX) {
        float quotient = mouseX / gridUnitWidth;
        float remainder = mouseX % gridUnitWidth;

        // Truncate instead of rounding to keep the point left of the cursor
        return (int) (remainder > gridUnitWidth / 2 ? quotient + 1 : quotient);
    }

    private int findNearestGridY(int mouseY) {
        float quotient = mouseY / gridUnitHeight;
        float remainder = mouseY % gridUnitHeight;

        // Truncate instead of rounding to keep the point left of the cursor
        return (int) (remainder > gridUnitHeight / 2 ? quotient + 1 : quotient);
    }

    private void drawDebugStrings(ArrayList<String> debugStrings, int drawX, int drawY, int nearestGridX, int nearestGridY) {
        debugStrings.add("gridX: " + nearestGridX);
        debugStrings.add("gridY: " + nearestGridY);
        drawHoveringText(debugStrings, drawX, drawY);
        drawRect(drawX - 1, drawY - 1, drawX + 1, drawY + 1, Color.RED.getRGB());
    }
}