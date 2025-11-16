package com.sighs.petiteinventory.utils;

import com.sighs.petiteinventory.init.ContainerGrid;

public class OperateUtils {
    private static boolean isShapeTransposed = false;

    public static void transposeShape() {
        isShapeTransposed = !isShapeTransposed;
    }
    public static boolean isShapeTransposed() {
        return isShapeTransposed;
    }
}
