package org.mods.gd656killicon.client.gui.screens;

import org.mods.gd656killicon.client.gui.tabs.ConfigTabContent;

public interface ElementConfigBuilder {
    /**
     * Builds the configuration interface for a specific element.
     * @param content The ConfigTabContent to add rows to.
     */
    void build(ConfigTabContent content);
}
