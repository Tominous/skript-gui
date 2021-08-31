package io.github.apickledwalrus.skriptgui.gui.events;

import ch.njol.skript.SkriptEventHandler;
import io.github.apickledwalrus.skriptgui.SkriptGUI;
import io.github.apickledwalrus.skriptgui.gui.GUI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class GUIEvents implements Listener {

	public GUIEvents() {
		// We still want these events to be processed by Skript
		SkriptEventHandler.listenCancelled.add(InventoryClickEvent.class);
		SkriptEventHandler.listenCancelled.add(InventoryDragEvent.class);
		SkriptEventHandler.listenCancelled.add(InventoryOpenEvent.class);
		SkriptEventHandler.listenCancelled.add(InventoryCloseEvent.class);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onInventoryClick(InventoryClickEvent event) {
		// Don't handle this event if it's from an unsupported click type
		switch (event.getClick()) {
			case WINDOW_BORDER_RIGHT:
			case WINDOW_BORDER_LEFT:
			case CREATIVE:
				return;
		}

		// No inventory was clicked
		Inventory clickedInventory = event.getClickedInventory();
		if (clickedInventory == null) {
			return;
		}

		// Don't handle this event if there isn't a matching GUI for it
		GUI gui = SkriptGUI.getGUIManager().getGUI(event.getInventory());
		if (gui == null) {
			return;
		}

		// Don't process unknown clicks for safety reasons - cancel them to prevent unwanted GUI changes
		if (event.getClick() == ClickType.UNKNOWN) {
			event.setCancelled(true);
			return;
		}

		// Don't handle this event if the clicked inventory is the bottom inventory, as we want users to be able to interact with their inventory
		// However, there are some cases where interaction with the bottom inventory may cause changes to the top inventory
		// Because of this, we will cancel the event for some click types
		if (clickedInventory.equals(event.getView().getBottomInventory())) {
			switch (event.getClick()) {
				case SHIFT_LEFT:
				case SHIFT_RIGHT:
					event.setCancelled(true);
					return;
				case DOUBLE_CLICK:
					// Only cancel if this will cause a change to the GUI itself
					// We are checking if our GUI contains an item that could be merged with the event item
					// If that item is mergeable but it isn't stealable, we will cancel the event now
					Inventory guiInventory = gui.getInventory();
					int size = guiInventory.getSize();
					ItemStack cursor = event.getWhoClicked().getItemOnCursor();
					for (int slot = 0; slot < size; slot++) {
						ItemStack item = guiInventory.getItem(slot);
						if (item != null && item.isSimilar(cursor) && !gui.isStealable(gui.convert(slot))) {
							event.setCancelled(true);
							break;
						}
					}
					return;
				default:
					return;
			}
		}

		gui.getEventHandler().onClick(event);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onInventoryDrag(InventoryDragEvent e) {
		GUI gui = SkriptGUI.getGUIManager().getGUI(e.getInventory());
		if (gui != null) {
			// Check if any slots in the actual GUI were changed. We don't care if only the player's inventory was changed.
			int lastSlotIndex = gui.getInventory().getSize() - 1;
			for (int slot : e.getRawSlots()) {
				if (slot <= lastSlotIndex) { // A slot in the actual GUI was interacted with
					gui.getEventHandler().onDrag(e);
					break;
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onInventoryOpen(InventoryOpenEvent e) {
		GUI gui = SkriptGUI.getGUIManager().getGUI(e.getInventory());
		if (gui != null) {
			gui.getEventHandler().onOpen(e);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onInventoryClose(InventoryCloseEvent e) {
		GUI gui = SkriptGUI.getGUIManager().getGUI(e.getInventory());
		if (gui != null) {
			gui.getEventHandler().onClose(e);
		}
	}

}