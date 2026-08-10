package org.mods.gd656killicon.server.logic.conquest;

import org.mods.gd656killicon.server.logic.spotting.ISpottingHandler;

/** conquest 未加载时的空实现(与 DummySpottingHandler 同构)。 */
public class DummyConquestSpottingHandler implements ISpottingHandler {
    @Override
    public void init() {}
}
