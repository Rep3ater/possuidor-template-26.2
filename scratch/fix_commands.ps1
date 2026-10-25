$dir = "c:\Mods developer\possuidor-template-26.2\src\main\java\ladysnake\requiem\common\command"
$files = Get-ChildItem -Path $dir -Filter "*.java"

foreach ($f in $files) {
    $text = [System.IO.File]::ReadAllText($f.FullName)
    
    # Imports
    $text = $text.Replace("import net.minecraft.command.CommandException;", "import net.minecraft.commands.CommandRuntimeException;")
    $text = $text.Replace("import net.minecraft.commands.arguments.EntityArgumentType;", "import net.minecraft.commands.arguments.EntityArgument;")
    $text = $text.Replace("import net.minecraft.commands.arguments.GameProfileArgumentType;", "import net.minecraft.commands.arguments.GameProfileArgument;")
    $text = $text.Replace("import static net.minecraft.commands.arguments.EntityArgumentType.*;", "import static net.minecraft.commands.arguments.EntityArgument.*;")
    $text = $text.Replace("import static net.minecraft.commands.arguments.GameProfileArgumentType.gameProfile;", "import static net.minecraft.commands.arguments.GameProfileArgument.gameProfile;")
    $text = $text.Replace("import static net.minecraft.commands.arguments.GameProfileArgumentType.getProfileArgument;", "")
    $text = $text.Replace("import static net.minecraft.commands.arguments.GameProfileArgumentType.getGameProfiles;", "import static net.minecraft.commands.arguments.GameProfileArgument.getGameProfiles;")

    # Usages
    $text = $text.Replace("EntityArgumentType.player()", "EntityArgument.player()")
    $text = $text.Replace("EntityArgumentType.players()", "EntityArgument.players()")
    $text = $text.Replace("EntityArgumentType.entity()", "EntityArgument.entity()")
    $text = $text.Replace("EntityArgumentType.entities()", "EntityArgument.entities()")
    $text = $text.Replace("EntityArgumentType.getPlayer(", "EntityArgument.getPlayer(")
    $text = $text.Replace("EntityArgumentType.getPlayers(", "EntityArgument.getPlayers(")
    $text = $text.Replace("EntityArgumentType.getEntity(", "EntityArgument.getEntity(")
    $text = $text.Replace("EntityArgumentType.getEntities(", "EntityArgument.getEntities(")
    $text = $text.Replace("GameProfileArgumentType.gameProfile()", "GameProfileArgument.gameProfile()")
    $text = $text.Replace("GameProfileArgumentType.getGameProfiles(", "GameProfileArgument.getGameProfiles(")
    $text = $text.Replace("GameProfileArgumentType.getProfileArgument(", "GameProfileArgument.getGameProfiles(")
    $text = $text.Replace("getProfileArgument(", "getGameProfiles(")
    $text = $text.Replace("new CommandException(", "new CommandRuntimeException(")
    $text = $text.Replace(".getWorld()", ".getLevel()")
    $text = $text.Replace("GameRules.SEND_COMMAND_FEEDBACK", "GameRules.RULE_SENDCOMMANDFEEDBACK")
    $text = [regex]::Replace($text, "source\.sendFeedback\((.+?),\s*(true|false)\);", "source.sendSuccess(() -> `$1, `$2);")

    [System.IO.File]::WriteAllText($f.FullName, $text, (New-Object System.Text.UTF8Encoding($false)))
}
Write-Host "Commands updated!"
