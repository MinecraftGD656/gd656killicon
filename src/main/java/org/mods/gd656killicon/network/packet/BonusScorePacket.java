package org.mods.gd656killicon.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.mods.gd656killicon.client.util.ClientMessageLogger;
import org.mods.gd656killicon.client.render.HudElementManager;
import org.mods.gd656killicon.client.render.impl.ScoreSubtitleRenderer;
import org.mods.gd656killicon.client.render.impl.BonusListRenderer;
import org.mods.gd656killicon.network.IPacket;

import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BonusScorePacket implements IPacket {
    private final int bonusType;
    private final float score;
    private final String extraData;
    private final int victimId;
    private final String victimName;

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("<(\\w+)>");

    public BonusScorePacket(int bonusType, float score) {
        this(bonusType, score, "", -1, null);
    }

    public BonusScorePacket(int bonusType, float score, String extraData) {
        this(bonusType, score, extraData, -1, null);
    }
    
    public BonusScorePacket(int bonusType, float score, String extraData, int victimId) {
        this(bonusType, score, extraData, victimId, null);
    }

    public BonusScorePacket(int bonusType, float score, String extraData, int victimId, String victimName) {
        this.bonusType = bonusType;
        this.score = score;
        this.extraData = extraData != null ? extraData : "";
        this.victimId = victimId;
        this.victimName = victimName;
    }

    public BonusScorePacket(FriendlyByteBuf buffer) {
        this.bonusType = buffer.readInt();
        this.score = buffer.readFloat();
        this.extraData = buffer.readUtf(32767);
        this.victimId = buffer.readInt();
        if (buffer.readBoolean()) {
            this.victimName = buffer.readUtf(32767);
        } else {
            this.victimName = null;
        }
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(this.bonusType);
        buffer.writeFloat(this.score);
        buffer.writeUtf(this.extraData != null ? this.extraData : "", 32767);
        buffer.writeInt(this.victimId);
        if (this.victimName != null) {
            buffer.writeBoolean(true);
            buffer.writeUtf(this.victimName, 32767);
        } else {
            buffer.writeBoolean(false);
        }
    }

    public int getBonusType() {
        return bonusType;
    }

    public float getScore() {
        return score;
    }

    public String getExtraData() {
        return extraData;
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            // Process chat message
            if (org.mods.gd656killicon.client.config.ClientConfigManager.isShowBonusMessage()) {
                sendBonusChatMessage();
            }

            // Update score subtitle display
            ScoreSubtitleRenderer.getInstance().addScore(this.score);
            
            // Trigger bonus list element with combined data string
            // Format: score|extraData|victimName
            StringBuilder dataBuilder = new StringBuilder();
            dataBuilder.append(this.score);
            if ((this.extraData != null && !this.extraData.isEmpty()) || (this.victimName != null && !this.victimName.isEmpty())) {
                dataBuilder.append("|").append(this.extraData != null ? this.extraData : "");
                if (this.victimName != null && !this.victimName.isEmpty()) {
                    dataBuilder.append("|").append(this.victimName);
                }
            }
            String data = dataBuilder.toString();

            HudElementManager.trigger("subtitle", "bonus_list", 
                org.mods.gd656killicon.client.render.IHudRenderer.TriggerContext.of(this.bonusType, this.victimId, 0, data)
            );
            
            // Record statistics for specific bonus types
            recordStatistics();
        });
        context.get().setPacketHandled(true);
    }
    
    private void recordStatistics() {
        // Record kill for KILL, KILL_EXPLOSION, KILL_HEADSHOT, KILL_CRIT bonus types
        // 注意：基础击杀统计（总击杀数、武器使用等）现在由 KillIconPacket 负责，避免因 Bonus 合并导致漏记。
        // BonusScorePacket 仅负责 ASSIST 和 DAMAGE 等非击杀类统计，或特殊的奖励统计。
        
        if (this.bonusType == org.mods.gd656killicon.common.BonusType.ASSIST) {
            // Record assist for ASSIST bonus type
            org.mods.gd656killicon.client.stats.ClientStatsManager.recordAssist();
        } else if (this.bonusType == org.mods.gd656killicon.common.BonusType.DAMAGE ||
                   this.bonusType == org.mods.gd656killicon.common.BonusType.EXPLOSION ||
                   this.bonusType == org.mods.gd656killicon.common.BonusType.HEADSHOT ||
                   this.bonusType == org.mods.gd656killicon.common.BonusType.CRIT) {
            // Record damage
            org.mods.gd656killicon.client.stats.ClientStatsManager.recordDamage(this.score);
        }
    }

    private void sendBonusChatMessage() {
        String format = BonusListRenderer.getEffectiveFormat(this.bonusType, this.extraData);
        
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(format);
        StringBuilder sb = new StringBuilder();
        
        while (matcher.find()) {
            String type = matcher.group(1);
            String replacement;
            
            switch (type) {
                case "score" -> {
                    if (this.score < 1.0f && this.score > 0.0f) {
                        replacement = String.format("%.1f", this.score);
                    } else {
                        replacement = String.valueOf(Math.round(this.score));
                    }
                }
                case "combo" -> {
                    int val = 0;
                    try { val = Integer.parseInt(this.extraData); } catch (Exception ignored) {}
                    replacement = String.valueOf(val);
                }
                case "multi_kill" -> {
                    int val = 0;
                    try { val = Integer.parseInt(this.extraData); } catch (Exception ignored) {}
                    replacement = BonusListRenderer.getLocalizedNumber(val);
                }
                case "distance" -> replacement = this.extraData + "m";
                case "streak" -> replacement = this.extraData;
                case "extra" -> replacement = this.extraData;
                default -> replacement = "";
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        
        ClientMessageLogger.chatLiteral(sb.toString());
    }
}
