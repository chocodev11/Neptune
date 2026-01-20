package dev.lrxh.neptune.providers.placeholder;

import dev.lrxh.neptune.API;
import dev.lrxh.neptune.feature.divisions.impl.Division;
import dev.lrxh.neptune.Neptune;
import dev.lrxh.neptune.feature.party.Party;
import dev.lrxh.neptune.feature.queue.QueueEntry;
import dev.lrxh.neptune.feature.queue.QueueService;
import dev.lrxh.neptune.game.kit.impl.KitRule;
import dev.lrxh.neptune.game.match.Match;
import dev.lrxh.neptune.game.match.MatchService;
import dev.lrxh.neptune.game.match.impl.ffa.FfaFightMatch;
import dev.lrxh.neptune.game.match.impl.participant.Participant;
import dev.lrxh.neptune.game.match.impl.solo.SoloFightMatch;
import dev.lrxh.neptune.game.match.impl.team.MatchTeam;
import dev.lrxh.neptune.game.match.impl.team.TeamFightMatch;
import dev.lrxh.neptune.profile.data.GlobalStats;
import dev.lrxh.neptune.profile.data.ProfileState;
import dev.lrxh.neptune.profile.impl.Profile;
import dev.lrxh.neptune.utils.PlayerUtil;
import lombok.experimental.UtilityClass;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@UtilityClass
public class PlaceholderUtil {

    public List<String> format(List<String> lines, Player player) {
        List<String> formattedLines = new ArrayList<>();

        for (String line : lines) {
            formattedLines.add(format(line, player));
        }

        return formattedLines;
    }

    public Component format(Component component, Player player) {
        return component.replaceText(builder -> builder
                .match(Pattern.compile("<.*?>"))
                .replacement((match, builder1) -> {
                    String placeholder = match.group();
                    String replacement = format(placeholder, player);
                    return Component.text(replacement);
                }));
    }

    public String format(String line, Player player) {
        Profile profile = API.getProfile(player);
        if (profile == null)
            return line;
        ProfileState state = profile.getState();

        line = line.replace("<online>", String.valueOf(Bukkit.getServer().getOnlinePlayers().size()));
        line = line.replace("<queued>", String.valueOf(QueueService.get().getQueueSize()));
        line = line.replace("<in-match>", String.valueOf(MatchService.get().matches.size()));
        line = line.replace("<player>", player.getName());
        line = line.replace("<ping>", String.valueOf((PlayerUtil.getPing(player))));
        line = line.replace("<division>", profile.getGameData().getGlobalStats().getDivision() != null
                ? profile.getGameData().getGlobalStats().getDivision().getDisplayName()
                : "N/A");

        GlobalStats globalStats = profile.getGameData().getGlobalStats();
        line = line.replace("<wins>", String.valueOf(globalStats.getWins()));
        line = line.replace("<losses>", String.valueOf(globalStats.getLosses()));
        line = line.replace("<currentStreak>", String.valueOf(globalStats.getCurrentStreak()));
        line = line.replace("<kills>", String.valueOf(globalStats.getKills()));
        line = line.replace("<deaths>", String.valueOf(globalStats.getDeaths()));

        if (state.equals(ProfileState.IN_QUEUE)) {
            QueueEntry queueEntry = QueueService.get().get(player.getUniqueId());
            if (queueEntry == null)
                return line;
            line = line.replace("<kit>", queueEntry.getKit().getDisplayName());
            line = line.replace("<maxPing>", String.valueOf(profile.getSettingData().getMaxPing()));
            line = line.replace("<time>", String.valueOf(queueEntry.getTime().formatTime()));
            Division kitDivision = profile.getGameData().get(queueEntry.getKit()).getDivision();
            line = line.replace("<kit_division>",
                    kitDivision != null ? kitDivision.getDisplayName() : "N/A");
        }

        if (state.equals(ProfileState.IN_KIT_EDITOR)) {
            line = line.replace("<kit>", profile.getGameData().getKitEditor().getDisplayName());
        }

        Party party = profile.getGameData().getParty();
        if (party != null) {
            line = line.replace("<leader>", party.getLeaderName());
            line = line.replace("<size>", String.valueOf(party.getUsers().size()));
            line = line.replace("<party-max>", String.valueOf(party.getMaxUsers()));
        }

        if (profile.getMatch() != null) {
            Match match = profile.getMatch();
            Participant participant = match.getParticipant(player.getUniqueId());

            line = line.replace("<maxPoints>", String.valueOf(match.getRounds()));
            Division matchKitDivision = profile.getGameData().get(profile.getMatch().getKit()).getDivision();
            line = line.replace("<kit_division>",
                    matchKitDivision != null ? matchKitDivision.getDisplayName() : "N/A");
            if (match instanceof SoloFightMatch soloFightMatch) {
                Participant red = soloFightMatch.getParticipantA();
                Participant blue = soloFightMatch.getParticipantB();
                line = line.replace("<red-hits>", String.valueOf(red.getHits()));
                line = line.replace("<blue-hits>", String.valueOf(blue.getHits()));
                line = line.replace("<red-combo>", red.getCombo() > 1 ? "&e(" + red.getCombo() + " Combo)" : "");
                line = line.replace("<blue-combo>", blue.getCombo() > 1 ? "&e(" + blue.getCombo() + " Combo)" : "");
                line = line.replace("<red-points>", String.valueOf(red.getPoints()));
                line = line.replace("<blue-points>", String.valueOf(blue.getPoints()));
                line = line.replace("<red-difference>", String.valueOf(red.getHitsDifference(blue)));
                line = line.replace("<blue-difference>", String.valueOf(blue.getHitsDifference(red)));
                line = line.replace("<playerRed_name>", red.getNameUnColored());
                line = line.replace("<playerBlue_name>", blue.getNameUnColored());

                line = line.replace("<playerRed_ping>",
                        String.valueOf(PlayerUtil.getPing(red.getPlayer())));
                line = line.replace("<playerBlue_ping>",
                        String.valueOf(PlayerUtil.getPing(blue.getPlayer())));

                if (match.getKit().is(KitRule.BED_WARS)) {
                    line = line.replace("<red-bed-status>", !red.isBedBroken() ? "&a✔" : "&c1");
                    line = line.replace("<blue-bed-status>", !blue.isBedBroken() ? "&a✔" : "&c1");
                }

                if (participant != null) {
                    Participant opponent = participant.getOpponent();
                    Player opponentPlayer = participant.getOpponent().getPlayer();

                    line = line.replace("<hits>", String.valueOf(participant.getHits()));
                    line = line.replace("<combo>",
                            participant.getCombo() > 1 ? "&e(" + participant.getCombo() + " Combo)" : "");
                    line = line.replace("<opponent>", participant.getOpponent().getNameUnColored());
                    line = line.replace("<opponent-ping>",
                            String.valueOf(opponentPlayer == null ? 0 : opponentPlayer.getPing()));
                    line = line.replace("<opponent-combo>",
                            opponent.getCombo() > 1 ? "&e(" + opponent.getCombo() + " Combo)" : "");
                    line = line.replace("<opponent-hits>", String.valueOf(opponent.getHits()));
                    line = line.replace("<diffrence>", participant.getHitsDifference(opponent));
                    // fixes the typo
                    line = line.replace("<difference>", participant.getHitsDifference(opponent));
                    line = line.replace("<points>", String.valueOf(participant.getPoints()));
                    line = line.replace("<opponent-points>", String.valueOf(opponent.getPoints()));

                    if (match.getKit().is(KitRule.BED_WARS)) {
                        line = line.replace("<bed-status>", !participant.isBedBroken() ? "&a✔" : "&c1");
                        line = line.replace("<opponent-bed-status>", !opponent.isBedBroken() ? "&a✔" : "&c1");
                    }
                }
            } else if (match instanceof TeamFightMatch teamFightMatch) {
                MatchTeam redTeam = teamFightMatch.getTeamA();
                MatchTeam blueTeam = teamFightMatch.getTeamB();
                line = line.replace("<alive-red>", String.valueOf(redTeam.getAliveParticipants()));
                line = line.replace("<max-red>", String.valueOf(redTeam.getParticipants().size()));
                line = line.replace("<alive-blue>", String.valueOf(blueTeam.getAliveParticipants()));
                line = line.replace("<max-blue>", String.valueOf(blueTeam.getParticipants().size()));

                if (match.getKit().is(KitRule.BED_WARS)) {
                    line = line.replace("<red-bed-status>",
                            !redTeam.isBedBroken() ? "&a✔" : "&c" + redTeam.getAliveParticipants());
                    line = line.replace("<blue-bed-status>",
                            !blueTeam.isBedBroken() ? "&a✔" : "&c" + blueTeam.getAliveParticipants());
                }
                if (participant != null) {
                    MatchTeam matchTeam = teamFightMatch.getParticipantTeam(participant);
                    MatchTeam opponentTeam = teamFightMatch.getParticipantTeam(participant).getOpponentTeam();
                    line = line.replace("<alive>", String.valueOf(matchTeam.getAliveParticipants()));
                    line = line.replace("<max>", String.valueOf(matchTeam.getParticipants().size()));
                    line = line.replace("<alive-opponent>", String.valueOf(opponentTeam.getAliveParticipants()));
                    line = line.replace("<max-opponent>", String.valueOf(opponentTeam.getParticipants().size()));

                    if (match.getRounds() > 1) {
                        line = line.replace("<points>", String.valueOf(matchTeam.getPoints()));
                        line = line.replace("<opponent-points>", String.valueOf(opponentTeam.getPoints()));
                    }
                    if (match.getKit().is(KitRule.BED_WARS)) {
                        line = line.replace("<team-bed-status>",
                                !matchTeam.isBedBroken() ? "&a✔" : "&c" + matchTeam.getAliveParticipants());
                        line = line.replace("<opponent-team-bed-status>",
                                !opponentTeam.isBedBroken() ? "&a✔" : "&c" + opponentTeam.getAliveParticipants());
                    }
                }
            } else if (match instanceof FfaFightMatch ffaFightMatch) {
                line = line.replace("<alive>", String
                        .valueOf(ffaFightMatch.getParticipants().size() - ffaFightMatch.deadParticipants.size()));
                line = line.replace("<max>", String.valueOf(ffaFightMatch.getParticipants().size()));
            }

            line = line.replace("<kit>", match.getKit().getDisplayName());
            line = line.replace("<arena>", match.getArena().getDisplayName());
            line = line.replace("<time>", match.getTime().formatTime());
        }
        if (Neptune.get().isPlaceholder() && PlaceholderAPIPlugin.getInstance().isEnabled()) {
            return PlaceholderAPI.setPlaceholders(player, line);
        }

        return line;
    }
}

