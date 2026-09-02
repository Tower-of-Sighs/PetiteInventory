package com.sighs.petiteinventory.inventory;

import com.sighs.petiteinventory.event.InventoryEvents;
import com.sighs.petiteinventory.service.ItemFootprintRules;
import com.sighs.petiteinventory.spi.ItemRulePort;
import com.sighs.petiteinventory.spi.StackPort;

/**
 * Loader-neutral item rules and footprint calculations.
 *
 * <p>Every target supplies only {@link StackPort} and {@link ItemRulePort};
 * all reusable behavior stays here.</p>
 */
public final class ItemInventoryService<S> {
    private final StackPort<S> stacks;
    private final ItemRulePort<S> rules;

    public ItemInventoryService(StackPort<S> stacks, ItemRulePort<S> rules) {
        if (stacks == null || rules == null) {
            throw new IllegalArgumentException("stack and item rule ports are required");
        }
        this.stacks = stacks;
        this.rules = rules;
    }

    public Area<S> getArea(S stack) {
        if (stack == null || stacks.isEmpty(stack)) {
            return area(1, 1, stack);
        }
        String itemId = rules.itemId(stack);
        String expression = rules.rule(stack, itemId);
        if (expression == null) {
            return area(1, 1, stack);
        }
        try {
            com.sighs.petiteinventory.core.ItemSize size =
                    ItemFootprintRules.parse(expression);
            size = ItemFootprintRules.rotate(size, stacks.isRotated(stack));
            return area(size.width(), size.height(), stack);
        } catch (IllegalArgumentException ignored) {
            return area(1, 1, stack);
        }
    }

    public boolean isSameItemIgnoreRotate(S first, S second) {
        return stacks.sameItemIgnoringRotation(first, second);
    }

    public boolean isRotated(S stack) {
        return stacks.isRotated(stack);
    }

    public void setRotated(S stack, boolean rotated) {
        stacks.setRotated(stack, rotated);
    }

    private Area<S> area(int width, int height, S stack) {
        AreaEvent<S> event = new AreaEvent<>(width, height, stack);
        InventoryEvents.publish(event);
        return new Area<>(event.width, event.height, event.itemStack);
    }
}
