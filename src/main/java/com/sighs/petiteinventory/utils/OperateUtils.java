package com.sighs.petiteinventory.utils;

import com.sighs.petiteinventory.init.ContainerGrid;

public class OperateUtils {
    private static boolean isShapeTransposed = false;
    private static ContainerGrid containerGrid;

    public static void transposeShape() {
        isShapeTransposed = !isShapeTransposed;
    }
    public static boolean isShapeTransposed() {
        return isShapeTransposed;
    }

    public static ContainerGrid getContainerGrid() {
        return containerGrid;
    }

    public static void setContainerGrid(ContainerGrid containerGrid) {
        OperateUtils.containerGrid = containerGrid;
    }
}
