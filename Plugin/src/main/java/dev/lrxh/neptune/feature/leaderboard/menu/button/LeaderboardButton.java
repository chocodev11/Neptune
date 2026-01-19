package dev.lrxh.neptune.feature.leaderboard.menu.button;

import dev.lrxh.neptune.configs.impl.MenusLocale;
import dev.lrxh.neptune.feature.leaderboard.LeaderboardService;
import dev.lrxh.neptune.feature.leaderboard.impl.LeaderboardType;
import dev.lrxh.neptune.feature.leaderboard.impl.PlayerEntry;
import dev.lrxh.neptune.game.kit.Kit;
import dev.lrxh.neptune.utils.ItemBuilder;
import dev.lrxh.neptune.utils.menu.Button;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardButton extends Button {
    private final Kit kit;
    private final LeaderboardType leaderboardType;

    public LeaderboardButton(int slot, Kit kit, LeaderboardType leaderboardType) {
        super(slot);
        this.kit = kit;
        this.leaderboardType = leaderboardType;
    }

    @Override
    public ItemStack getItemStack(Player player) {
        List<String> lore = new ArrayList<>();

        List<PlayerEntry> leaderboard = LeaderboardService.get().getPlayerEntries(kit, leaderboardType);

        for (String templateLine : MenusLocale.LEADERBOARD_LORE.getStringList()) {
            String line = templateLine;
            for (int i = 1; i <= 10; i++) {
                // Use the already-fetched leaderboard list directly instead of calling
                // getLeaderboardSlot
                PlayerEntry playerEntry = (i <= leaderboard.size()) ? leaderboard.get(i - 1) : null;

                if (playerEntry == null) {
                    line = line.replace("<player_" + i + ">", "???");
                    line = line.replace("<value_" + i + ">", "???");
                    continue;
                }

                line = line.replace("<player_" + i + ">", playerEntry.getUsername());
                line = line.replace("<value_" + i + ">", String.valueOf(playerEntry.getValue()));
            }

            lore.add(line);
        }

        return new ItemBuilder(kit.getIcon())
                .name(MenusLocale.LEADERBOARD_ITEM_NAME.getString().replace("<kit>", kit.getDisplayName()))
                .lore(lore, player)

                .build();
    }
}
