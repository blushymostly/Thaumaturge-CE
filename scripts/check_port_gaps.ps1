# Static port-gap checks (research → recipe → model/lang/parents). Exit 1 on hard failures.
$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$Data = Join-Path $Root "src\main\resources\data\thaumaturge"
$Assets = Join-Path $Root "src\main\resources\assets\thaumaturge"
$ResearchDir = Join-Path $Data "thaumaturge\research_entry"
$RecipesDir = Join-Path $Data "recipes"
$LangPath = Join-Path $Assets "lang\en_us.json"
$DeferredPath = Join-Path $PSScriptRoot "port_deferred.json"

function Get-RecipeIndex {
    $map = @{}
    if (Test-Path $RecipesDir) {
        Get-ChildItem $RecipesDir -Recurse -Filter *.json | ForEach-Object {
            $rel = $_.FullName.Substring($RecipesDir.Length + 1).Replace("\", "/") -replace "\.json$", ""
            $map["thaumaturge:$rel"] = $_.FullName
        }
    }
    return $map
}

$recipes = Get-RecipeIndex
$researchIds = @{}
Get-ChildItem $ResearchDir -Filter *.json | ForEach-Object { $researchIds[$_.BaseName] = $true }
$lang = @{}
if (Test-Path $LangPath) {
    (Get-Content $LangPath -Raw | ConvertFrom-Json).PSObject.Properties | ForEach-Object { $lang[$_.Name] = $true }
}
$deferred = @{}
$deferredParents = @{}
if (Test-Path $DeferredPath) {
    $doc = Get-Content $DeferredPath -Raw | ConvertFrom-Json
    $doc.recipes.PSObject.Properties | ForEach-Object { $deferred[$_.Name] = $true }
    if ($doc.parents) {
        $doc.parents.PSObject.Properties | ForEach-Object { $deferredParents[$_.Name] = $true }
    }
}

$hard = New-Object System.Collections.Generic.List[string]
$warn = New-Object System.Collections.Generic.List[string]
$soft = New-Object System.Collections.Generic.List[string]
$bookmarked = 0

Get-ChildItem $ResearchDir -Filter *.json | Sort-Object Name | ForEach-Object {
    $entryId = $_.BaseName
    $entry = Get-Content $_.FullName -Raw | ConvertFrom-Json
    foreach ($parent in @($entry.parents)) {
        if (-not $parent) { continue }
        $base = ([string]$parent).Split("@")[0]
        if (-not $base.StartsWith("thaumaturge:")) { continue }
        $parentPath = $base.Substring("thaumaturge:".Length)
        if ($parentPath.StartsWith("scanned/") -or $parentPath.StartsWith("m_") -or $parentPath.StartsWith("f_")) { continue }
        if (-not $researchIds.ContainsKey($parentPath)) {
            if ($deferredParents.ContainsKey($base)) {
                $soft.Add("DEFERRED parent '$base' ($entryId)")
            } else {
                $hard.Add("PARENT missing research_entry '$base' (from $entryId)")
            }
        }
    }
    if ($entry.name -and -not $lang.ContainsKey([string]$entry.name)) {
        $warn.Add("LANG missing '$($entry.name)' ($entryId)")
    }
    $stageIndex = 0
    foreach ($stage in @($entry.stages)) {
        if ($stage.text -and -not $lang.ContainsKey([string]$stage.text)) {
            $warn.Add("LANG missing '$($stage.text)' ($entryId stage $stageIndex)")
        }
        foreach ($recipeId in @($stage.recipes)) {
            if (-not $recipeId) { continue }
            $rid = [string]$recipeId
            $bookmarked++
            if ($recipes.ContainsKey($rid)) {
                $recipe = Get-Content $recipes[$rid] -Raw | ConvertFrom-Json
                $item = $null
                if ($recipe.result -is [string]) { $item = [string]$recipe.result }
                elseif ($recipe.result.item) { $item = [string]$recipe.result.item }
                if ($item -and $item.StartsWith("thaumaturge:")) {
                    $name = $item.Substring("thaumaturge:".Length)
                    $itemModel = Join-Path $Assets "models\item\$name.json"
                    $blockModel = Join-Path $Assets "models\block\$name.json"
                    $blockState = Join-Path $Assets "blockstates\$name.json"
                    if (-not (Test-Path $itemModel) -and -not (Test-Path $blockModel) -and -not (Test-Path $blockState)) {
                        $hard.Add("MODEL missing for result '$item' (recipe $rid)")
                    }
                    if (-not $lang.ContainsKey("item.thaumaturge.$name") -and -not $lang.ContainsKey("block.thaumaturge.$name")) {
                        $warn.Add("LANG missing item/block name for '$item' ($rid)")
                    }
                }
            }
            elseif ($deferred.ContainsKey($rid)) {
                $soft.Add("DEFERRED recipe '$rid' ($entryId)")
            }
            else {
                $hard.Add("RECIPE missing '$rid' (bookmarked by $entryId)")
            }
        }
        $stageIndex++
    }
}

Write-Host "=== check_port_gaps ==="
Write-Host "research entries: $($researchIds.Count)"
Write-Host "forge recipes:    $($recipes.Count)"
Write-Host "bookmarked:       $bookmarked"
Write-Host "deferred recipes: $($deferred.Count)"
Write-Host ""
if ($soft.Count -gt 0) {
    Write-Host "-- deferred ($($soft.Count)) --"
    $soft | Select-Object -First 20 | ForEach-Object { Write-Host "  $_" }
    if ($soft.Count -gt 20) { Write-Host "  ... +$($soft.Count - 20) more" }
    Write-Host ""
}
if ($warn.Count -gt 0) {
    Write-Host "-- warnings ($($warn.Count)) --"
    $warn | Select-Object -First 40 | ForEach-Object { Write-Host "  WARN $_" }
    if ($warn.Count -gt 40) { Write-Host "  ... +$($warn.Count - 40) more" }
    Write-Host ""
}
if ($hard.Count -gt 0) {
    Write-Host "-- FAILURES ($($hard.Count)) --"
    $hard | ForEach-Object { Write-Host "  FAIL $_" }
    Write-Host ""
    Write-Host "check_port_gaps: FAILED ($($hard.Count) hard, $($warn.Count) warn)"
    exit 1
}
Write-Host "check_port_gaps: OK ($($warn.Count) warn, $($soft.Count) deferred)"
exit 0
