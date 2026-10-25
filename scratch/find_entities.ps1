$names = @('SmallFireball.java', 'WitherSkull.java', 'ShulkerBullet.java', 'EvokerFangs.java', 'Vex.java', 'Sheep.java', 'Frog.java', 'Goat.java', 'Guardian.java', 'Pufferfish.java', 'Shulker.java', 'WitherBoss.java')
foreach ($n in $names) {
    $f = Get-ChildItem -Path 'C:\Users\gusta\.gemini\antigravity\brain\bf263c0c-fb4f-4dbb-a309-748a369cf583\scratch\mc_26_2_sources\net\minecraft\world\entity' -Filter $n -Recurse
    if ($f) { Write-Host "$n -> $($f.FullName)" }
}
