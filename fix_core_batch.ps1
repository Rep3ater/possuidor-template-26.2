$coreDir = "src\main\java\ladysnake\requiem\core"
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$files = Get-ChildItem -Path $coreDir -Recurse -Filter "*.java"
$count = 0

foreach ($f in $files) {
    $content = [System.IO.File]::ReadAllText($f.FullName, [System.Text.Encoding]::UTF8)
    $orig = $content

    # Mappings
    $content = $content.Replace("net.minecraft.util.ActionResult", "net.minecraft.world.InteractionResult")
    $content = $content.Replace("net.minecraft.world.World", "net.minecraft.world.level.Level")
    $content = $content.Replace("net.minecraft.item.FoodComponent", "net.minecraft.world.food.FoodProperties")
    $content = $content.Replace("net.minecraft.predicate.NumberRange", "net.minecraft.advancements.critereon.MinMaxBounds")
    $content = $content.Replace("net.minecraft.predicate.entity.EntityPredicate", "net.minecraft.advancements.critereon.EntityPredicate")
    $content = $content.Replace("net.minecraft.predicate.item.ItemPredicate", "net.minecraft.advancements.critereon.ItemPredicate")
    $content = $content.Replace("net.minecraft.util.JsonHelper", "net.minecraft.util.GsonHelper")
    $content = $content.Replace("net.minecraft.util.Pair", "com.mojang.datafixers.util.Pair")
    $content = $content.Replace("org.quiltmc.qsl.resource.loader.api.reloader.SimpleResourceReloader", "net.fabricmc.fabric.api.resource.SimpleResourceReloadListener")
    $content = $content.Replace("SimpleResourceReloader", "SimpleResourceReloadListener")

    # Word replacements
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, '\bActionResult\b', 'InteractionResult')
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, '\bWorld\b', 'Level')
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, '\bJsonHelper\b', 'GsonHelper')
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, '\bFoodComponent\b', 'FoodProperties')

    # Ensure Level and InteractionResult imports if referenced
    if ($content.Contains("Level ") -or $content.Contains("Level>") -or $content.Contains("(Level")) {
        if (-not $content.Contains("import net.minecraft.world.level.Level;")) {
            $content = $content.Replace("package ladysnake.requiem.core", "import net.minecraft.world.level.Level;`r`npackage ladysnake.requiem.core")
        }
    }
    if ($content.Contains("InteractionResult ") -or $content.Contains("InteractionResult>") -or $content.Contains("(InteractionResult")) {
        if (-not $content.Contains("import net.minecraft.world.InteractionResult;")) {
            $content = $content.Replace("package ladysnake.requiem.core", "import net.minecraft.world.InteractionResult;`r`npackage ladysnake.requiem.core")
        }
    }

    # Fix package line if it got placed before package
    if ($content.StartsWith("import net.minecraft.")) {
        $lines = $content.Split("`n")
        $pkgLine = ""
        $importLines = @()
        $otherLines = @()
        foreach ($l in $lines) {
            $trimmed = $l.Trim()
            if ($trimmed.StartsWith("package ")) {
                $pkgLine = $l
            } elseif ($trimmed.StartsWith("import net.minecraft.world.level.Level;") -or $trimmed.StartsWith("import net.minecraft.world.InteractionResult;")) {
                $importLines += $l
            } else {
                $otherLines += $l
            }
        }
        if ($pkgLine -ne "") {
            $content = $pkgLine + "`r`n`r`n" + ($importLines -join "`r`n") + "`r`n" + ($otherLines -join "`n")
        }
    }

    if ($content -ne $orig) {
        [System.IO.File]::WriteAllText($f.FullName, $content, $utf8NoBom)
        $count++
    }
}

Write-Host "Updated $count core files."
