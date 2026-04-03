/*
 * Copyright (C) 2026 Briiqn
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.briiqn.reunion.core.util.container;

import dev.briiqn.reunion.core.data.Recipe;
import dev.briiqn.reunion.core.network.packet.data.ItemInstance;
import dev.briiqn.reunion.core.network.packet.manager.PacketManager;
import dev.briiqn.reunion.core.network.packet.protocol.console.s2c.impl.ConsoleContainerAckS2CPacket;
import dev.briiqn.reunion.core.network.packet.protocol.java.c2s.impl.JavaClickWindowC2SPacket;
import dev.briiqn.reunion.core.registry.impl.RecipeRegistry;
import dev.briiqn.reunion.core.session.ConsoleSession;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class CraftingTranslator {

  private final ConsoleSession session;

  public CraftingTranslator(ConsoleSession session) {
    this.session = session;
  }

  private CraftingGrid buildGrid(Recipe recipe, int gridSize) {
    CraftingGrid grid = new CraftingGrid(gridSize);

    if (recipe.isShaped()) {
      int rows = recipe.gridSize()[0];
      int cols = recipe.gridSize()[1];
      for (int r = 0; r < rows; r++) {
        for (int c = 0; c < cols; c++) {
          grid.setId(r, c, recipe.ingredientAt(r, c));
          grid.setMeta(r, c, recipe.ingredientMetaAt(r, c));
        }
      }
    } else {
      int[] ingredients = recipe.ingredients();
      int[] metas = recipe.ingredientMeta();
      int i = 0;
      outer:
      for (int r = 0; r < gridSize; r++) {
        for (int c = 0; c < gridSize; c++) {
          if (i >= ingredients.length) {
            break outer;
          }
          grid.setId(r, c, ingredients[i]);
          grid.setMeta(r, c, metas[i]);
          i++;
        }
      }
    }

    return grid;
  }

  private Map<Integer, IngredientReq> buildPlacements(CraftingGrid grid, int gridStart) {
    Map<Integer, IngredientReq> placements = new LinkedHashMap<>();
    for (int r = 0; r < grid.size(); r++) {
      for (int c = 0; c < grid.size(); c++) {
        int id = grid.idAt(r, c);
        if (id != 0) {
          placements.put(grid.toSlot(gridStart, r, c), new IngredientReq(id, grid.metaAt(r, c)));
        }
      }
    }
    return placements;
  }

  private void logGrid(CraftingGrid grid) {
    StringBuilder sb = new StringBuilder("Crafting Grid (")
        .append(grid.size()).append('×').append(grid.size()).append(")\n");
    for (int r = 0; r < grid.size(); r++) {
      for (int c = 0; c < grid.size(); c++) {
        int id = grid.idAt(r, c);
        if (id != 0) {
          String meta = grid.metaAt(r, c) == -1 ? "*" : String.valueOf(grid.metaAt(r, c));
          sb.append(String.format("[%4d:%-2s] ", id, meta));
        } else {
          sb.append("[  Empty  ] ");
        }
      }
      sb.append('\n');
    }
    log.debug(sb.toString());
  }

  public void handleCraft(int recipeIndex, short uid) {
    byte windowId = session.getInventoryTracker().getWindowId();

    PacketManager.sendToConsole(session, new ConsoleContainerAckS2CPacket(windowId, uid, true));

    Recipe recipe = RecipeRegistry.getInstance().get(recipeIndex);
    if (recipe == null) {
      return;
    }

    InventoryTracker inv = session.getInventoryTracker();
    boolean isCraftingTable = session.getWindowType(windowId) == 1;
    int gridStart = 1;
    int gridSize = isCraftingTable ? 3 : 2;

    if (recipe.isShaped()) {
      int[] size = recipe.gridSize();
      if (size[0] > gridSize || size[1] > gridSize) {
        return;
      }
    } else {
      if (recipe.ingredients().length > gridSize * gridSize) {
        return;
      }
    }

    CraftingGrid grid = buildGrid(recipe, gridSize);
    Map<Integer, IngredientReq> placements = buildPlacements(grid, gridStart);

    if (log.isDebugEnabled()) {
      logGrid(grid);
    }

    Map<Integer, ItemInstance> simInv = new HashMap<>();
    for (Map.Entry<Integer, ItemInstance> entry : inv.getActiveSlots().entrySet()) {
      simInv.put(entry.getKey(), cloneItem(entry.getValue()));
    }

    ItemInstance cursor = null;
    int cursorSrcSlot = -1;

    for (Map.Entry<Integer, IngredientReq> entry : placements.entrySet()) {
      int gridSlot = entry.getKey();
      IngredientReq req = entry.getValue();

      if (cursor != null && (cursor.id() != req.id()
          || (req.meta() != -1 && cursor.damage() != req.meta()))) {
        int returnSlot = findReturnSlot(simInv, cursor, isCraftingTable, cursorSrcSlot);
        ItemInstance existing = simInv.get(returnSlot);
        click(windowId, returnSlot, 0, 0, existing);
        simInv.put(returnSlot, existing != null
            ? new ItemInstance(cursor.id(), (byte) (existing.count() + cursor.count()),
            cursor.damage(), cursor.nbt())
            : cursor);
        cursor = null;
        cursorSrcSlot = -1;
      }

      if (cursor == null) {
        int sourceSlot = findIngredient(simInv, req.id(), req.meta(), isCraftingTable);
        if (sourceSlot == -1) {
          return;
        }

        ItemInstance sourceItem = simInv.get(sourceSlot);
        click(windowId, sourceSlot, 0, 0, sourceItem);
        cursor = sourceItem;
        cursorSrcSlot = sourceSlot;
        simInv.remove(sourceSlot);
      }

      click(windowId, gridSlot, 1, 0, null);
      cursor = new ItemInstance(cursor.id(), (byte) (cursor.count() - 1), cursor.damage(),
          cursor.nbt());
      if (cursor.count() <= 0) {
        cursor = null;
        cursorSrcSlot = -1;
      }
    }

    if (cursor != null) {
      int returnSlot = findReturnSlot(simInv, cursor, isCraftingTable, cursorSrcSlot);
      ItemInstance existing = simInv.get(returnSlot);
      click(windowId, returnSlot, 0, 0, existing);
      simInv.put(returnSlot, existing != null
          ? new ItemInstance(cursor.id(), (byte) (existing.count() + cursor.count()),
          cursor.damage(), cursor.nbt())
          : cursor);
    }

    ItemInstance craftedItem = new ItemInstance(
        (short) recipe.resultId(), (byte) recipe.resultCount(), (short) recipe.resultMeta(), null);
    click(windowId, 0, 0, 1, craftedItem);
    simulateShiftClick(simInv, recipe, isCraftingTable);

    inv.getActiveSlots().clear();
    for (Map.Entry<Integer, ItemInstance> e : simInv.entrySet()) {
      ItemInstance item = e.getValue();
      if (item != null && item.id() != -1 && item.count() > 0) {
        inv.getActiveSlots().put(e.getKey(), item);
      }
    }
  }


  private void simulateShiftClick(Map<Integer, ItemInstance> simInv, Recipe recipe,
      boolean isCraftingTable) {
    int resultId = recipe.resultId();
    int resultMeta = recipe.resultMeta();
    int count = recipe.resultCount();

    int startIndex = isCraftingTable ? 10 : 9;
    int endIndex = isCraftingTable ? 46 : 45;

    for (int i = endIndex - 1; i >= startIndex; i--) {
      if (count <= 0) {
        break;
      }
      ItemInstance existing = simInv.get(i);
      if (existing != null && existing.id() == resultId && existing.damage() == resultMeta) {
        int space = 64 - existing.count();
        if (space > 0) {
          int add = Math.min(space, count);
          simInv.put(i, new ItemInstance((short) resultId, (byte) (existing.count() + add),
              (short) resultMeta, existing.nbt()));
          count -= add;
        }
      }
    }

    for (int i = endIndex - 1; i >= startIndex; i--) {
      if (count <= 0) {
        break;
      }
      if (!simInv.containsKey(i) || simInv.get(i) == null) {
        int add = Math.min(64, count);
        simInv.put(i, new ItemInstance((short) resultId, (byte) add, (short) resultMeta, null));
        count -= add;
      }
    }
  }


  private ItemInstance cloneItem(ItemInstance item) {
    if (item == null) {
      return null;
    }
    return new ItemInstance(item.id(), item.count(), item.damage(), item.nbt());
  }

  private int findIngredient(Map<Integer, ItemInstance> simInv, int id, int meta,
      boolean isCraftingTable) {
    int invStart = isCraftingTable ? 10 : 9;
    int maxSlots = 36;
    for (Map.Entry<Integer, ItemInstance> entry : simInv.entrySet()) {
      int slot = entry.getKey();
      ItemInstance item = entry.getValue();
      if (slot < invStart || slot >= invStart + maxSlots) {
        continue;
      }
      if (item != null && item.id() == id && (meta == -1 || item.damage() == meta)
          && item.count() > 0) {
        return slot;
      }
    }
    return -1;
  }

  private int findReturnSlot(Map<Integer, ItemInstance> simInv, ItemInstance cursor,
      boolean isCraftingTable, int preferredSlot) {
    int invStart = isCraftingTable ? 10 : 9;
    int maxSlots = 36;

    if (preferredSlot != -1) {
      ItemInstance existing = simInv.get(preferredSlot);
      if (existing == null) {
        return preferredSlot;
      }
      if (existing.id() == cursor.id() && existing.damage() == cursor.damage()
          && existing.count() + cursor.count() <= 64) {
        return preferredSlot;
      }
    }

    for (Map.Entry<Integer, ItemInstance> entry : simInv.entrySet()) {
      int slot = entry.getKey();
      ItemInstance item = entry.getValue();
      if (slot < invStart || slot >= invStart + maxSlots || slot == preferredSlot) {
        continue;
      }
      if (item != null && item.id() == cursor.id() && item.damage() == cursor.damage()
          && item.count() + cursor.count() <= 64) {
        return slot;
      }
    }

    for (int i = invStart + maxSlots - 1; i >= invStart; i--) {
      if (!simInv.containsKey(i) || simInv.get(i) == null) {
        return i;
      }
    }

    return invStart;
  }

  private void click(int windowId, int slot, int button, int mode, ItemInstance clickedItem) {
    short actionId = session.nextActionId();
    log.debug("{} Clicked Inventory Window {} {} {} {} {}",
        session.getPlayerName(), windowId, slot, button, mode, clickedItem);
    PacketManager.sendToJava(session.getJavaSession(),
        new JavaClickWindowC2SPacket(windowId, (short) slot, button, actionId, mode, clickedItem));
  }

  private static final class CraftingGrid {

    private final int size;
    private final int[][] ids;
    private final int[][] metas;

    CraftingGrid(int size) {
      this.size = size;
      this.ids = new int[size][size];
      this.metas = new int[size][size];
    }

    int idAt(int row, int col) {
      return ids[row][col];
    }

    int metaAt(int row, int col) {
      return metas[row][col];
    }

    void setId(int row, int col, int val) {
      ids[row][col] = val;
    }

    void setMeta(int row, int col, int val) {
      metas[row][col] = val;
    }

    int size() {
      return size;
    }

    int toSlot(int gridStart, int row, int col) {
      return gridStart + (row * size) + col;
    }
  }

  private record IngredientReq(int id, int meta) {

  }
}