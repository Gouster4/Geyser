/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.connector.network.translators.java.scoreboard;

import com.github.steveice10.mc.protocol.data.game.scoreboard.ObjectiveAction;
import com.github.steveice10.mc.protocol.packet.ingame.server.scoreboard.ServerScoreboardObjectivePacket;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.GeyserLogger;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.session.cache.WorldCache;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.network.translators.chat.MessageTranslator;
import org.geysermc.connector.scoreboard.Objective;
import org.geysermc.connector.scoreboard.Scoreboard;
import org.geysermc.connector.scoreboard.ScoreboardUpdater;
import org.geysermc.connector.scoreboard.UpdateType;

@Translator(packet = ServerScoreboardObjectivePacket.class)
public class JavaScoreboardObjectiveTranslator extends PacketTranslator<ServerScoreboardObjectivePacket> {
    private final GeyserLogger logger = GeyserConnector.getInstance().getLogger();

    @Override
    public void translate(ServerScoreboardObjectivePacket packet, GeyserSession session) {
        WorldCache worldCache = session.getWorldCache();
        Scoreboard scoreboard = worldCache.getScoreboard();
        int pps = worldCache.increaseAndGetScoreboardPacketsPerSecond();

        Objective objective = scoreboard.getObjective(packet.getName());
        if (objective != null && objective.getUpdateType() != UpdateType.REMOVE && packet.getAction() == ObjectiveAction.ADD) {
            logger.warning("An objective with the same name '" + packet.getName() + "' already exists! Ignoring packet");
            return;
        }

        if ((objective == null || objective.getUpdateType() == UpdateType.REMOVE) && packet.getAction() != ObjectiveAction.REMOVE) {
            objective = scoreboard.registerNewObjective(packet.getName());
        }

        switch (packet.getAction()) {
            case ADD:
            case UPDATE:
                objective.setDisplayName(MessageTranslator.convertMessage(packet.getDisplayName()))
                        .setType(packet.getType().ordinal());
                break;
            case REMOVE:
                scoreboard.unregisterObjective(packet.getName());
                break;
        }

        if (objective == null || !objective.isActive()) {
            return;
        }

        // ScoreboardUpdater will handle it for us if the packets per second
        // (for score and team packets) is higher then the first threshold
        if (pps < ScoreboardUpdater.FIRST_SCORE_PACKETS_PER_SECOND_THRESHOLD) {
            scoreboard.onUpdate();
        }
    }
}
