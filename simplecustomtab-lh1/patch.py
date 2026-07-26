from pathlib import Path
import sys

root = Path(sys.argv[1])

pom = root / "pom.xml"
text = pom.read_text(encoding="utf-8")
old = "<version>1.1.1</version>"
new = "<version>1.1.1-LH1</version>"
if old not in text:
    raise SystemExit("Versão original não encontrada no pom.xml")
pom.write_text(text.replace(old, new, 1), encoding="utf-8")

parse_utils = root / "src/main/java/simplexity/simplecustomtab/util/ParseUtils.java"
text = parse_utils.read_text(encoding="utf-8")
if "import org.bukkit.Bukkit;" not in text:
    text = text.replace("import org.bukkit.entity.Player;", "import org.bukkit.Bukkit;\nimport org.bukkit.entity.Player;")
old_block = '''        return TagResolver.resolver(
                Placeholder.component("displayname", player.displayName()),
                Placeholder.unparsed("username", player.getName()),
                Placeholder.parsed("player-skull", "<head:" + player.getName() + ">"));'''
new_block = '''        return TagResolver.resolver(
                Placeholder.component("displayname", player.displayName()),
                Placeholder.unparsed("username", player.getName()),
                Placeholder.unparsed("online", String.valueOf(Bukkit.getOnlinePlayers().size())),
                Placeholder.unparsed("maxplayers", String.valueOf(Bukkit.getMaxPlayers())),
                Placeholder.unparsed("ping", String.valueOf(player.getPing())),
                Placeholder.unparsed("world", player.getWorld().getName()),
                Placeholder.parsed("player-skull", "<head:" + player.getName() + ">"));'''
if old_block not in text:
    raise SystemExit("Bloco defaultTags original não encontrado")
parse_utils.write_text(text.replace(old_block, new_block, 1), encoding="utf-8")

update_tab = root / "src/main/java/simplexity/simplecustomtab/util/UpdateTabList.java"
text = update_tab.read_text(encoding="utf-8")
start = text.index("    public static void updateHeader(String string) {")
end = text.index("    private static String getFormatString(Player player){")
replacement = '''    public static void updateHeaderAndFooter(String headerString, String footerString) {
        SimpleCustomTab.getInstance().getServer().getOnlinePlayers().forEach(player -> {
            Component header;
            Component footer;
            if (SimpleCustomTab.hasPAPI()) {
                header = miniMessage.deserialize(
                        headerString,
                        ParseUtils.papiTag(player),
                        ParseUtils.defaultTags(player));
                footer = miniMessage.deserialize(
                        footerString,
                        ParseUtils.papiTag(player),
                        ParseUtils.defaultTags(player));
            } else {
                header = miniMessage.deserialize(headerString, ParseUtils.defaultTags(player));
                footer = miniMessage.deserialize(footerString, ParseUtils.defaultTags(player));
            }
            player.sendPlayerListHeaderAndFooter(header, footer);
        });
    }

'''
update_tab.write_text(text[:start] + replacement + text[end:], encoding="utf-8")

schedule = root / "src/main/java/simplexity/simplecustomtab/scheduler/ScheduleManager.java"
text = schedule.read_text(encoding="utf-8")
old_calls = '''        UpdateTabList.updateHeader(currentHeader);
        UpdateTabList.updateFooter(currentFooter);'''
new_calls = '''        UpdateTabList.updateHeaderAndFooter(currentHeader, currentFooter);'''
if old_calls not in text:
    raise SystemExit("Chamadas globais de header/footer não encontradas")
schedule.write_text(text.replace(old_calls, new_calls, 1), encoding="utf-8")

config_source = Path(__file__).with_name("config.yml")
config_target = root / "src/main/resources/config.yml"
config_target.write_text(config_source.read_text(encoding="utf-8"), encoding="utf-8")

print("Patch Log Horizon aplicado com sucesso")
