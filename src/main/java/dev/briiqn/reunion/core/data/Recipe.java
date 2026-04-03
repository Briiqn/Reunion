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

package dev.briiqn.reunion.core.data;

public class Recipe {

  private final Type type;
  private final int resultId;
  private final int resultMeta;
  private final int resultCount;
  private final int[][] inShape;
  private final int[][] inShapeMeta;
  private final int[] ingredients;
  private final int[] ingredientMeta;

  public Recipe(int resultId, int resultMeta, int resultCount,
      int[][] inShape, int[][] inShapeMeta) {
    this.type = Type.SHAPED;
    this.resultId = resultId;
    this.resultMeta = resultMeta;
    this.resultCount = resultCount;
    this.inShape = inShape;
    this.inShapeMeta = inShapeMeta;
    this.ingredients = null;
    this.ingredientMeta = null;
  }

  public Recipe(int resultId, int resultMeta, int resultCount,
      int[] ingredients, int[] ingredientMeta) {
    this.type = Type.SHAPELESS;
    this.resultId = resultId;
    this.resultMeta = resultMeta;
    this.resultCount = resultCount;
    this.inShape = null;
    this.inShapeMeta = null;
    this.ingredients = ingredients;
    this.ingredientMeta = ingredientMeta;
  }

  public Type type() {
    return type;
  }

  public int resultId() {
    return resultId;
  }

  public int resultMeta() {
    return resultMeta;
  }

  public int resultCount() {
    return resultCount;
  }

  public boolean isShaped() {
    return type == Type.SHAPED;
  }

  public boolean isShapeless() {
    return type == Type.SHAPELESS;
  }

  public int[] gridSize() {
    if (inShape == null) {
      return new int[]{0, 0};
    }
    int rows = inShape.length;
    int cols = rows == 0 ? 0 : inShape[0].length;
    return new int[]{rows, cols};
  }

  public int ingredientAt(int row, int col) {
    if (inShape == null || row >= inShape.length || col >= inShape[row].length) {
      return 0;
    }
    return inShape[row][col];
  }

  public int ingredientMetaAt(int row, int col) {
    if (inShapeMeta == null || row >= inShapeMeta.length || col >= inShapeMeta[row].length) {
      return -1;
    }
    return inShapeMeta[row][col];
  }

  public int[] ingredients() {
    return ingredients != null ? ingredients : new int[0];
  }

  public int[] ingredientMeta() {
    return ingredientMeta != null ? ingredientMeta : new int[0];
  }

  @Override
  public String toString() {
    return "Recipe{type=" + type
        + ", result=" + resultId + ":" + resultMeta + "×" + resultCount + "}";
  }

  public enum Type {SHAPED, SHAPELESS}
}