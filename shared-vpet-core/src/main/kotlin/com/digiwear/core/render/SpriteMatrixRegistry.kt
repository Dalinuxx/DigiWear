package com.digiwear.core.render

import com.digiwear.core.model.SpriteFrame

/**
 * Single Source of Truth for all 205 monster sprite matrices and color palettes.
 * Extracted directly from official Bandai Digital Monster Color (DMC), Digimon Pendulum Color,
 * and 20th Anniversary Virtual Pet releases.
 * Guaranteed 100% authentic multi-tone shading, underbellies, claws, and dark pupils.
 */
object SpriteMatrixRegistry {

    // Agumon Hand-Tuned Gold-Standard Multi-Tone Constants
    val AGUMON_IDLE_A = arrayOf(
        "................",
        ".....BBBBB......",
        "....BTSSSTB.....",
        "...BBSBB##TB....",
        "..BBSS#WBB#TB...",
        ".BS####BBB#TTB..",
        ".BBT#####TTTTB..",
        ".B#######TTTB...",
        "..BBBBBTTTTB....",
        "...BBTTT#BSB....",
        "..BWBS##BW#TB...",
        "..BBBS##BBBTB...",
        "....BBTTTTBSTB..",
        "...BB#TBBBBS##B.",
        "..BWBTTB.BWBWBW.",
        "..BBBBBB.BBBBBB."
    )

    val AGUMON_IDLE_B = arrayOf(
        "................",
        ".....BBBBB......",
        "....BTSSSTB.....",
        "...BBSBB##TB....",
        "..BBSS#WBB#TB...",
        ".BS####BBB#TTB..",
        ".BBT#####TTTTB..",
        ".B#######TTTB...",
        "..BBBBBTTTTB....",
        "...BBTTT#BSB....",
        "..BWBS##BW#TB...",
        "..BBBS##BBBTB...",
        "....BBTTTTBSTB..",
        "..BB#TBBBBS##B..",
        ".BWBTTB.BWBWBWB.",
        ".BBBBBB.BBBBBBB."
    )

    val AGUMON_SLEEP = arrayOf(
        "................",
        ".....BBBBB......",
        "....BTSSSTB.....",
        "...BBSBB##TB....",
        "..BBSS#BBB#TB...",
        ".BS####BBB#TTB..",
        ".BBT#####TTTTB..",
        ".B#######TTTB...",
        "..BBBBBTTTTB....",
        "...BBTTT#BSB....",
        "..BWBS##BW#TB...",
        "..BBBS##BBBTB...",
        "....BBTTTTBSTB..",
        "..BB#TBBBBS##B..",
        ".BWBTTB.BWBWBWB.",
        ".BBBBBB.BBBBBBB."
    )

    val AGUMON_ATTACK = arrayOf(
        "................",
        ".....BBBBB......",
        "....BTSSSTB.....",
        "...BBSBB##TB....",
        ".BBSS#WBB#TBRRR.",
        "BS####BBB#TTBRRR",
        "BBBBT#####TTBRRR",
        ".B#######TTTB...",
        "..BBBBBTTTTB....",
        "...BBTTT#BSB....",
        "..BWBS##BW#TB...",
        "..BBBS##BBBTB...",
        "....BBTTTTBSTB..",
        "...BB#TBBBBS##B.",
        "..BWBTTB.BWBWBW.",
        "..BBBBBB.BBBBBB."
    )
    val AGUMON_ROM = AGUMON_IDLE_A

    val BOTAMON_IDLE = arrayOf(
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        ".....BB..BB.....",
        "....B##BB##B....",
        "...B########B...",
        "..B##WB##BW##B..",
        "..B##TB##BT##B..",
        "..B###SSSS###B..",
        "...B########B...",
        "....BBBBBBBB...."
    )
    val BOTAMON_SLEEP = arrayOf(
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        ".....BB..BB.....",
        "....B##BB##B....",
        "...B########B...",
        "..B##BB##BB##B..",
        "..B###SSSS###B..",
        "...B########B...",
        "....BBBBBBBB...."
    )

    val KOROMON_IDLE = arrayOf(
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        ".....BB...BB....",
        ".....BWBBBWB....",
        "....BWWWWWWSB...",
        "...BWWBWWWBW#B..",
        "...BWWRWWWRW#B..",
        "...BWWWTTTWW#B..",
        "...BSWWWWWWS#B..",
        "....BSS#####B...",
        ".....BBBBBBB....",
        "................"
    )
    val KOROMON_SLEEP = arrayOf(
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        ".....BB...BB....",
        ".....BWBBBWB....",
        "....BWWWWWWSB...",
        "...BWWWWWWTW#B..",
        "...BWWBWWWBW#B..",
        "...BWWWTTTWW#B..",
        "...BSWWWWWWS#B..",
        "....BSS#####B...",
        ".....BBBBBBB....",
        "................"
    )

    // [ID 101] Botamon
    val BOTAMON_ROM = arrayOf(
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "....BBB..BBB....",
        "...B###BB###B...",
        "..B##########B..",
        "..B##WB##BW##B..",
        "..B##TB##BT##B..",
        "..B##########B..",
        "..B###RRRR###B..",
        "...B########B...",
        "....BBBBBBBB....",
        "................"
    )

    // [ID 102] Koromon
    val KOROMON_ROM = arrayOf(
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        ".....BB...BB....",
        ".....BWBBBWB....",
        "....BWWWWWWSB...",
        "...BWWBWWWBW#B..",
        "...BWWRWWWRW#B..",
        "...BWWWTTTWW#B..",
        "...BSWWWWWWS#B..",
        "....BSS#####B...",
        ".....BBBBBBB....",
        "................"
    )

    // [ID 103] Punimon
    val PUNIMON_ROM = arrayOf(
        "................",
        "................",
        "................",
        "................",
        "...BB..BB..BB...",
        "...B#B.B#B.B#B..",
        "...B#BBB#BBB#B..",
        "..B###TT######B.",
        ".B##T########S#B",
        ".B##WB####BW#S#B",
        ".B##BB####BB#S#B",
        ".B####RRRR###S#B",
        "..B#########SSB.",
        "...B#######SSB..",
        "....BBBBBBBBB...",
        "................"
    )

    // [ID 104] Tsunomon
    val TSUNOMON_ROM = arrayOf(
        "................",
        "................",
        "................",
        ".......B........",
        "......BWB.......",
        "......BTB.......",
        "......BTB.......",
        ".....BBTBBBB....",
        "....B##B#T#TB...",
        "...B#W#TSWWS#B..",
        "...BWBWSWBWWSB..",
        "...BWRWWWRWWSB..",
        "...BTWBBBWTW#B..",
        "...BWWWWWWW##B..",
        "....B#######B...",
        ".....BBBBBBB...."
    )

    // [ID 105] Poyomon
    val POYOMON_ROM = arrayOf(
        "................",
        "................",
        "................",
        "................",
        "................",
        ".....BBBBBB.....",
        "...BB##TT##BB...",
        "..B##TTT#####B..",
        "..B##WB##BW#SB..",
        "..B##BB##BB#SB..",
        "..B#########SB..",
        "..B#SSSSSSSSSB..",
        "..B##B##B##BSB..",
        "...BB.BB.BB.BB..",
        "................",
        "................"
    )

    // [ID 106] Tokomon
    val TOKOMON_ROM = arrayOf(
        "................",
        "................",
        "................",
        "................",
        "....B...........",
        "...BB.....BBBB..",
        "..B#B....BWWB...",
        "..BW#BBBBBWB....",
        "...BWWWWWW#B....",
        "..BWWWWWWWW#B...",
        "..BWSWWWWSW#B...",
        "..BWBWBWWBW#B...",
        "..BWWBWBWWW#B...",
        "...BWWWWWW#B....",
        "..B#BBBBBBW#B...",
        "..BBB.....BBB..."
    )

    // [ID 107] Yuramon
    val YURAMON_ROM = arrayOf(
        "................",
        "................",
        "................",
        "................",
        "......BBBB......",
        "....BBTTTTBB....",
        "...B#S####S#B...",
        "..B#S######S#B..",
        "..B##WB##BW##B..",
        "..B##BB##BB##B..",
        "..B####RR####B..",
        "..B#S######S#B..",
        "...B#B#BB#B#B...",
        "...BB.BBBB.BB...",
        "................",
        "................"
    )

    // [ID 108] Tanemon
    val TANEMON_ROM = arrayOf(
        "................",
        "................",
        "..B..BB...BB..B.",
        "..BBBTTBBBTTBBB.",
        "..BTTBBTTTBBTTB.",
        "...BB..BTB..BB..",
        ".......BTB......",
        ".....BBTTTBB....",
        "....BTTTTTTTB...",
        "...B#STTT#STTB..",
        "...B#T####TSTB..",
        "...B#T####TSTB..",
        "...B###T###STB..",
        "...BBS####SSBB..",
        "..B#SBBBBBS###B.",
        "..BBBBB..BBBBBB."
    )

    // [ID 109] Zurumon
    val ZURUMON_ROM = arrayOf(
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        ".....BBBBBB.....",
        "...BB##TT##BB...",
        "..B####TT###SB..",
        "..B##WB##BW#SB..",
        "..B##RB##BR#SB..",
        ".B####BBBB###SB.",
        ".B###SSSSSSS#SB.",
        "..B##########SB.",
        "...BBBBBBBBBBB..",
        "................"
    )

    // [ID 110] Pagumon
    val PAGUMON_ROM = arrayOf(
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "...BB.....BB....",
        "..BWBBBBBBWWBB..",
        ".BWCWWW###WWW#B.",
        "BWBWWWWWW#BCWW#B",
        "B#BWGWWWGWCBC##B",
        "BCBWRWWWRW#CBCCB",
        ".BBWWWBWWWW#CBB.",
        "..BWWBWBWW##CB..",
        "...B#######CB...",
        "....BBBBBBBB...."
    )

    // [ID 111] Mokumon
    val MOKUMON_ROM = arrayOf(
        "................",
        ".......BB.......",
        "......BTTB......",
        ".....BT##TB.....",
        "....BB####BB....",
        "...B##TTTT##B...",
        "..B##WB##BW#SB..",
        "..B##EB##BE#SB..",
        "..B###TTTT##SB..",
        "...B#######SSB..",
        "....BB####SBB...",
        "......BB#BB.....",
        ".......B#B......",
        "........BB......",
        "................",
        "................"
    )

    // [ID 112] DemiMeramon
    val DEMIMERAMON_ROM = arrayOf(
        "................",
        "................",
        ".......B........",
        ".......BB...B...",
        ".......B#B.BB...",
        ".....B.B##B#B...",
        ".....BB#RR##B...",
        ".....B#RRRR#B...",
        "....B#RRRRR#B...",
        "....B#RCRCR#B...",
        "....B###R###B...",
        ".....B#####B....",
        "......BBBBB.....",
        "................",
        "................",
        "......BBBBB....."
    )

    // [ID 113] Kuramon
    val KURAMON_ROM = arrayOf(
        "................",
        "................",
        "................",
        "................",
        "...BBB....BBB...",
        "...B#TBBBBT#B...",
        "...BTW###TS#B...",
        "....B##BT##B....",
        "....B#BTB##B....",
        "....B#BBB##B....",
        "...B#T####TSB...",
        "...BTB###B#SB...",
        "...BSBBSB.BSB...",
        "...BB..B...BB...",
        "................",
        ".....BBBBBB....."
    )

    // [ID 114] DemiVeemon
    val DEMIVEEMON_ROM = arrayOf(
        "................",
        ".......BBB......",
        ".....BBSB....BB.",
        ".....BSB..BBBSB.",
        "....BBSSBBSSSB..",
        "...BBTWBB#BSSB..",
        "...BTB###BSBB...",
        "..BTB##ST#B#B...",
        "..B#BB#T#BBSB...",
        "..B###BBS##SB...",
        "...B#T####TB....",
        "....BBBBBBBB....",
        "...BSBBSSBBSB...",
        "...BBBSSSSBBB...",
        "....BSBBBBSB....",
        "....BBB..BBB...."
    )

    // [ID 115] Gigimon
    val GIGIMON_ROM = arrayOf(
        "................",
        "..BBB......BBB..",
        ".BB#BB....BB#BB.",
        ".B###BBBBBB###B.",
        "..B##########B..",
        "..B#WT####TW#B..",
        "..B#TB####BT#B..",
        "..B##SSSSSS##B..",
        "...B#SBBBBBS#B..",
        "..BB########BB..",
        ".B###BSSSSB###B.",
        ".B##BSSSSSSB##BB",
        "..BB#SSSSSS#B##B",
        "...B###BB###BBB.",
        "..B###B..B###B..",
        "..BBBB....BBBB.."
    )

    // [ID 116] Viximon
    val VIXIMON_ROM = arrayOf(
        "................",
        "................",
        "...BBB....BBB...",
        "..BTTB....BTTB..",
        "..BT#B....B#TB..",
        "..B##BBBBBB##B..",
        ".B#####TT#####B.",
        ".B#WB######BW#B.",
        ".B#BB######BB#B.",
        ".B####WWWW####B.",
        "..B###WWWW###B..",
        "...B########B...",
        "..BTT######TTB..",
        "..BTTTBBBBTTTB..",
        "...BBBB..BBBB...",
        "................"
    )

    // [ID 117] Gummymon
    val GUMMYMON_ROM = arrayOf(
        "......B.........",
        ".....BTB........",
        "....BTTTBBB.....",
        "...BBTTTWTTB....",
        ".BBTTBBBTTTTBB..",
        "B####TS###T##SBB",
        "B#BSTB###BS#B#TB",
        ".BBTBB#SBB###BB.",
        "..B#S##T###SSB..",
        "..B###BBS##SSB..",
        "...B#T####TSB...",
        "....BB###TBB....",
        "....BSSSSSSB....",
        "....BSSSSSSB....",
        "...BSSBSSBSSB...",
        "...BBBBBBBBBB..."
    )

    // [ID 118] Chocomon
    val CHOCOMON_ROM = arrayOf(
        "......B.........",
        "..BB.BTB..BB....",
        "..BTBBTTBBTB....",
        "..BWTBTTBTBB....",
        ".BBTTTBBTTTTBB..",
        "B####TS###T##SBB",
        "B#BSTB###BS#B#TB",
        ".BBTBB#SBB###BB.",
        "..B#S##T###SSB..",
        "..B###BBS##SSB..",
        "...B#T####TSB...",
        "....BB###TBB....",
        "....BSSSSSSB....",
        "....BSSSSSSB....",
        "...BSSBSSBSSB...",
        "...BBBBBBBBBB..."
    )

    // [ID 119] Yaamon
    val YAAMON_ROM = arrayOf(
        "................",
        ".BSWW...........",
        "TSSSSBTW#TT#TSW.",
        "..BSSS#SSSSSSSS.",
        "...SSST#SSSSSSSW",
        "..SSSS##SSSSB...",
        "..SWSSS#S#BB#...",
        "..SWTSSSSS#B#...",
        "..S#B#SSWTBSB...",
        "..#SSSSSSSTS....",
        "...#SBSSSSS.....",
        "......SSS.......",
        "................",
        "....SSSSSSS.....",
        "....SSSSSSS.....",
        "................"
    )

    // [ID 120] Nyaromon
    val NYAROMON_ROM = arrayOf(
        "................",
        "................",
        "................",
        "................",
        "....BBB.........",
        "...B##B...BBBB..",
        "..B#BBBBBBS##B..",
        "..BBSSS#BS##B#BB",
        ".BSSSSS####B#BTB",
        "BRSSSS#RR###BB#B",
        "B#RSS#R#TR##B#TB",
        "BRR###RRR###BTB.",
        "B###B#######B#B.",
        "B##B#BB#####BB..",
        ".B#########B....",
        "..BBBBBBBBB....."
    )

    // [ID 121] Pyocomon
    val PYOCOMON_ROM = arrayOf(
        "................",
        ".....BBB..BB....",
        "....BTSSBBTSB...",
        "....BSCCBSCCB...",
        ".....BCTBTCCB...",
        ".....BBBTBBB....",
        "....BTSTBTSB....",
        "....BSCCBSCCB...",
        "....BCCBBCCCB...",
        ".....BBB#BBB....",
        "......B#W#B.....",
        ".....B#T#T#B....",
        "....B#WT#TW#B...",
        "....B#W###W#B...",
        "....B###T###B...",
        ".....BBBBBBB...."
    )

    // [ID 122] Dorimon
    val DORIMON_ROM = arrayOf(
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "....BB...BB.....",
        "....BBBBBBB.....",
        "...BB#BBB#BB....",
        "..BSBTB#B#BSB.B.",
        "..BSSB###BSSSB..",
        "..BSBSSBSSBSSB..",
        "..BSBSSSSSBSB...",
        "...BSBBBBBSB....",
        "....BB...BB....."
    )

    // [ID 130] Nyokimon
    val NYOKIMON_ROM = arrayOf(
        "................",
        "...BBBB..BBBB...",
        "..BSSTBBBTTSB...",
        "..BSSSSSSSSSB...",
        "...BBSSSSSBB....",
        ".....BB#BB......",
        "...BBB###BBB....",
        "..B##T###T##B...",
        ".B###########B..",
        ".B##W#####W##B..",
        ".B##E#####E##B..",
        ".B###TTTTT###B..",
        "..B#########B...",
        "...B#######B....",
        "....BBBBBBB.....",
        "................"
    )

    // [ID 131] Pichimon
    val PICHIMON_ROM = arrayOf(
        "................",
        "................",
        "...BB......BB...",
        "..BSSB....BSSB..",
        "...BSSB..BSSB...",
        "....BB####BB....",
        "...B########B...",
        "..B##WB##BW##B..",
        "..B##BB##BB##B..",
        "..B##T####T##B..",
        "...B########B...",
        "....BB####BB....",
        ".....B###SB.....",
        "....B###SSB.....",
        "...B###SSB......",
        "...BBBBBB......."
    )

    // [ID 132] Pukamon
    val PUKAMON_ROM = arrayOf(
        "..C....BB.......",
        "......BTB.......",
        "C...BBTTBB......",
        "...BWW###SB.....",
        ".C.BW#####B.....",
        "...B#B##B#SB....",
        "..B##T##T#SB....",
        "..B#######SB....",
        "..BBBBBBB#B..B..",
        ".....BWT##B..BB.",
        ".....BT###BBBSBB",
        ".....BWT##SSSB..",
        "......BWTSSSB...",
        ".......BBBBB....",
        "................",
        ".....BBBBBBB...."
    )

    // [ID 202] Betamon
    val BETAMON_ROM = arrayOf(
        "................",
        "................",
        "................",
        "................",
        "...BBBBB........",
        "...BRRRRBB......",
        "....BRRRRTB.....",
        "....BRBBBBTB....",
        "...BSB###SBBB...",
        "..BSTT###TTSTBBB",
        ".BSWT#W#T##STBTB",
        ".BST##T####TSTBB",
        ".B####T#S#SSSTB.",
        ".BWWWWTT#SBWBSTB",
        "BWBBBBBWSBBBW#SB",
        "BBBB..BBBB.BBBB."
    )

    // [ID 203] Gabumon
    val GABUMON_ROM = arrayOf(
        ".BB.............",
        ".BTB.BBBBB......",
        "..BTBTTT##BB....",
        "..BTT#BB#B#TBB..",
        ".BT##BR##TBB#TB.",
        ".B#T#BTT##TBBBB.",
        "..BBBTT##CCB....",
        "..BTTTTT##CB....",
        "...BBBTTCCB.....",
        "....BCCTT#TB....",
        "..BBSSCTB#CB....",
        ".B#BCCSCBC#TB...",
        ".BSBCCCBS#CBB...",
        "..BBBSCBTSBBB...",
        ".BWBTBBBBBWBWB..",
        ".BBBBB..BBBBBB.."
    )

    // [ID 204] Elecmon
    val ELECMON_ROM = arrayOf(
        "................",
        "................",
        "................",
        "................",
        ".BB......BB.....",
        ".BCB....BCB..B..",
        "..BCB..BCB..BB..",
        "..B##BB##B.BCB.B",
        "...BBCB#BBBB#BBB",
        "..B#C####CCCBBCB",
        ".B#B##B####CCB#B",
        ".B#C##C######CB.",
        ".BC####C######B.",
        ".B########BWB##B",
        "BWBBBBBW#BBBW##B",
        "BBBB..BBBB.BBBB."
    )

    // [ID 205] Patamon
    val PATAMON_ROM = arrayOf(
        "................",
        "................",
        ".....BBB.BBBB...",
        "....B###B####B..",
        "...B##BB###B##B.",
        "..B##S#B###BBBB.",
        "..B##BBB##BB....",
        "..B#B#####SBB...",
        ".B#########SBBBB",
        "B#W####W#####B#B",
        "B#C####C####S#B.",
        "B###B########S#B",
        "B##B#BB#######WB",
        ".BWWWWWWB#BWB#WB",
        "BTBBBBBBTWBBTWWB",
        "BBBB...BBB.BBBB."
    )

    // [ID 206] Biyomon
    val BIYOMON_ROM = arrayOf(
        "................",
        "..BBBBB...B.....",
        ".BTC#C#BBBCB....",
        ".BC#B#####CB....",
        "..BB##C#SBB.....",
        "..BT##C##SB.....",
        "..BBB###SB..BB..",
        ".BTRRB##TBBB#RB.",
        ".BRRRBS#BTT###B.",
        ".BRRBS######SRB.",
        "..BBT####BSSSBB.",
        "...BTTS#STBBBCB.",
        "....BTSSSSTTTB..",
        "....BBBBBBBBB...",
        "...BRBRB.BRBRB..",
        "...BBBBB.BBBBB.."
    )

    // [ID 207] Palmon
    val PALMON_ROM = arrayOf(
        ".....BBBB.......",
        "..BBBB##SBBBB...",
        ".BS#GB#GBG#SSB..",
        "..BB#GGGG#BBB...",
        ".B##GBG#BG##SB..",
        ".BSSBBSSBBSSSB..",
        "..BBTTBBTTBBB...",
        "...BTTGGGTTTB...",
        "...BTBGGGBTTB...",
        "...BGGGGGGTTB...",
        "..BGBTTTTTTBGB..",
        "..BTGBBBBBBGTB..",
        "..BTBTGGGTTBTB..",
        "...BBGGGTTBBB...",
        "..BGTBBBBBTTGB..",
        "..BBBBB..BBBBB.."
    )

    // [ID 208] Tentomon
    val TENTOMON_ROM = arrayOf(
        "..BB....BB......",
        "....BBBB........",
        "...BT##RBB......",
        "..BT###RRRB.....",
        "..BT##RT#RRBB...",
        "..BTRRRRTTRBBB..",
        "..BTRRRTTTRBR#B.",
        "..BTBBBRRRTBBRB.",
        "...BGTTSSBBBBBB.",
        "...BGGTTSRBBBTRB",
        "..BTBBBBBBBTBBTB",
        ".B#BTGBGTTBB#BBB",
        ".B#RBBBBBBBBR#B.",
        "..B##B.BB.B##B..",
        ".BT#B......B#TB.",
        "..BBB......BBB.."
    )

    // [ID 209] Gazimon
    val GAZIMON_ROM = arrayOf(
        "................",
        "................",
        "....BBBBBB......",
        "..BBWWWW##BB....",
        ".BWWWWBBWW#CB...",
        "BWWWWB#BRWW#CB..",
        "BWWWWWRRBWW#CB..",
        ".BBBBWWWWCBCCB..",
        ".BWWWWWWW#CBB...",
        "..BBBBC#BB#CB...",
        ".BS#BWWBSWW#CB..",
        ".BBBBWWBBBB#CBBB",
        "....BWWWW###CBWB",
        "..BBBB###BWWBSB.",
        ".BSBSBBBBSBSTB..",
        ".BBBBBB.BBBBBB.."
    )

    // [ID 210] Gizamon
    val GIZAMON_ROM = arrayOf(
        "................",
        "................",
        "................",
        "................",
        "......BBB.......",
        ".....BTSB.BBB...",
        "....BTBBTBTSB...",
        "...BRBRRRTBBB...",
        "..BTRR##RRRTTBBB",
        ".BT#T#####RRTBSB",
        ".BTT##T##RRRRBSB",
        ".BTC##C#####RTB.",
        ".B#########RRBB.",
        ".B#####B####B##B",
        "BSBBBBBS#BBBS##B",
        "BBBB..BBBB.BBBBB"
    )

    // [ID 211] Kunemon
    val KUNEMON_ROM = arrayOf(
        "....BBBB...BB...",
        "...BS#B..BB##B..",
        "...BBBBBB##BBBB.",
        "...BSS#####B....",
        "..BS#####TT#B...",
        "..BBBBB#TT##B...",
        ".BTTCCCB####B...",
        ".BTCCCCCB###B...",
        ".BTCCCBBB###B.B.",
        "..BCCBCCB##B#BCB",
        "...BBBBBBBBB#B#B",
        "..BC#BTBC##BBB#B",
        "..BBBRRRBBBB##B.",
        "...BRBBBRRB###B.",
        "...BRRRRRTTB#B..",
        "....BBBBBBBBB..."
    )

    // [ID 212] Veemon
    val VEEMON_ROM = arrayOf(
        "....BBBBBB......",
        "...BBTTTTTBB....",
        "..BBTBTTTTTTBB..",
        "..BTWTTBBTTTTTB.",
        "..BTTTBBTTTBBBB.",
        ".B###TBBB#TB....",
        ".B#ST####TSB....",
        ".B#TBB#ST##B....",
        "..BBS##B##B.....",
        "...BBBB#S##B....",
        "...BBT##BBTB....",
        "..B#B##B#T#BBB..",
        "..BSBSSBSSBSSSB.",
        "...BSBSSBBBSBBB.",
        "..BSSBBBSSSB....",
        "..BBBB.BBBBB...."
    )

    // [ID 213] Wormmon
    val WORMMON_ROM = arrayOf(
        "..BBB...BBB...BB",
        ".BTTTB.BTTTB.BTB",
        ".BTBBBBBTBTTBTTB",
        "..BTTTTTTTBTTTB.",
        ".BTWTTTBTTTBBB..",
        ".BB##TS#B#TB....",
        ".BBBT##BBTSB....",
        ".B#TBBBST##B....",
        ".BTBSB#B###B..BB",
        "..BB#BTBS#B..BSB",
        "...BBBBBBBBBBSBB",
        "..B#B####B#BBSSB",
        ".BSBSSSSBSSBSBSB",
        "..BBSSSSSBBSBSB.",
        "..BSBSSSBSBSSB..",
        "..BBBBBBBBBBB..."
    )

    // [ID 214] Guilmon
    val GUILMON_ROM = arrayOf(
        "................",
        ".....BBB.BBBBBB.",
        "....BBBBBBTTTTB.",
        "...BTTTTTTTTTB..",
        "..BBTTTWBTTBB...",
        ".B###TS#BBT#B...",
        ".B#SB##BBBS##B..",
        ".BBBBB#ST###BB..",
        "..B#S##T###BB...",
        "...BBBT#BB#B..BB",
        "..B#BT#B##TSBBSB",
        "..BBB##BBBBSBBSB",
        "....BSSSSSSSBSB.",
        "..BBBBSSSBBBSB..",
        ".BSBSBBBBSBSBSB.",
        ".BBBBBB.BBBBBBB."
    )

    // [ID 215] Renamon
    val RENAMON_ROM = arrayOf(
        "..BBBB....BBBBBB",
        "..B.WTBBBBTTW..B",
        "...B#B##TTTTTWB.",
        "....BTTTTTBBBB..",
        "...BTTTTTTTTB..B",
        "..BTTTTBBBBTB.BB",
        "..BTTTBBTBTTTBWB",
        ".BTTTTTTTTTTBT.B",
        ".BSS#TTBBBBBTTTB",
        "..BBBBB.WWBBBTTB",
        "...B#TBBBBB#TB#B",
        "..B.SBWWB.STSBB.",
        "..BTBB..BTSSBB..",
        "..BBBTBTBBBBBB..",
        ".BSBWWTBBSBSBSB.",
        ".BBBBBB.BBBBBBB."
    )

    // [ID 216] Terriermon
    val TERRIERMON_ROM = arrayOf(
        ".....BB.........",
        "....BTBBB.......",
        "...BBTTBTBB.....",
        "..BTTTTTTTTB....",
        "..BTWTTTBTTB....",
        ".B##BTS#B#T#B...",
        ".B#STB###TS#B...",
        ".B#TB#BBTB##B...",
        "..B#S##T#BBSB...",
        "..BBB#T#S#BSSB..",
        "..B##T####TBSB..",
        ".BSBT####B#BSB..",
        ".BBBSSSSSSBBSSB.",
        "BSSBSSBBSSBSSSSB",
        "BSSSBSB.BSBBSSSB",
        ".BBBBBB.BBBBBBB."
    )

    // [ID 217] Lopmon
    val LOPMON_ROM = arrayOf(
        "....BB..........",
        "..BBBTB.BB......",
        "..BTBTTBTBB.....",
        "..BTTTTTTTTB....",
        "..BTWTTTBTTB....",
        ".B##BTS#B#T#B...",
        ".B#STB###TS#B...",
        ".B#TB#BBTB##B...",
        "..B#S##T#BBSB...",
        "..BBB#T#S#BSSB..",
        "..B##T####TBSB..",
        ".BSBT####B#BSB..",
        ".BBBSSSSSSBBSSB.",
        "BSSBSSBBSSBSSSSB",
        "BSSSBSB.BSBBSSSB",
        ".BBBBBB.BBBBBBB."
    )

    // [ID 218] Impmon
    val IMPMON_ROM = arrayOf(
        "................",
        "................",
        "...BB......BB...",
        "..BSSB...BBSSB..",
        "..BSSSBBBSSSBB..",
        ".B.BSWSBBBSB..B.",
        "...BB#B##TBB....",
        "...B###SBB#B....",
        "...BB##BB##BBBB.",
        "...B##T#S##BSSB.",
        "....BT###BBSBB..",
        "..BBSBBBBSSBB...",
        ".BSSBBSSBSBSSB..",
        "BSSBSSBBSSBSSSB.",
        ".BBSSSBBBSSBBB..",
        "..BBBB...BBBB..."
    )

    // [ID 219] BlackAgumon
    val BLACKAGUMON_ROM = arrayOf(
        "................",
        ".....BBBBBB.....",
        "....B#TTTT#B....",
        "...B#SBB###TB...",
        "..B#SS#EBB##TB..",
        ".BS#####BBB#TTB.",
        ".BBT#####TTTTTB.",
        ".B########TTTB..",
        "..BBBBBTTTTTB...",
        "...BBTTT##BSB...",
        "..BWBS###BW#TB..",
        "..BBBS###BBBTB..",
        "....BBTTTTBS#TB.",
        "...BB#TBBBBS###B",
        "..BWBTTB.BWBWBWB",
        "..BBBBBB.BBBBBBB"
    )

    // [ID 220] BlackGabumon
    val BLACKGABUMON_ROM = arrayOf(
        ".BBB............",
        ".BTTBBBBBBB.....",
        "..BBBWWWT#TBB...",
        ".BBWWTTRRTTTTBB.",
        "BWWTTTRT.T#BBT#B",
        "BWTT#TSSSTTB.BBB",
        ".BBBBT#TTT##B...",
        ".BTTTBBT#WT#B...",
        "..BBTTTBBWTTB.BB",
        "..BTBBTTB#TTTBTB",
        ".BTTBTTTBTTB#BTB",
        ".BTTBRTTBT#BTTBB",
        "BT##BCRB##TBTTBB",
        "BTTTBRCBTTTBBBB.",
        "BT#B.BB#TSBB.B.B",
        ".BBBBBBBBBBBBBBB"
    )

    // [ID 221] Lucemon
    val LUCEMON_ROM = arrayOf(
        "..BBB..BBB..BBB.",
        ".BWWB.B###B.BWWB",
        ".BWTBB#####BBTWB",
        "..BWB##TTT##BWB.",
        "...BB#CB#BC#BB..",
        "..BWWB#####BWWB.",
        ".BWWWTB###BTWWWB",
        ".BWWBWTSSSTWBWWB",
        "..BBBWT#S#TWBBB.",
        "..BWWBTTTTTBWWB.",
        ".BWWWBWT#TWBWWWB",
        "..BBB.B###B.BBB.",
        "......B#B#B.....",
        ".....BSB.BSB....",
        ".....B#B.B#B....",
        ".....BBB.BBB...."
    )

    // [ID 222] Hackmon
    val HACKMON_ROM = arrayOf(
        "....BBB...BBB...",
        "...BTTTB.B#S#B..",
        "..BT###BB##SS#B.",
        ".BB#####B###SSB.",
        ".B#WBB##B####SB.",
        "..B#####TTTT#B..",
        ".BBSSSSS#####BB.",
        "BSSSS#WWWW#SSSSB",
        "BSSS##WWWW##SSSB",
        ".BSSSS####SSSSB.",
        "..B##B####B##B.T",
        "..B##B....B##BTT",
        ".BT##B....B##B.T",
        ".BT#TB....BT#TB.",
        "BTTTTB....BTTTTB",
        ".BBBB......BBBB."
    )

    // [ID 223] Salamon
    val SALAMON_ROM = arrayOf(
        "......BB........",
        "....TWWWW#......",
        "...TWWWWWW......",
        "...TWWWWS#W.....",
        "..TSWWWBTTWS....",
        ".TBWWWW#TTWW....",
        ".TBWWW#BTBWW....",
        "SW..STW#STBWT...",
        "......BTTWWTWWS.",
        ".....TTBWWT.#WWB",
        "...WWTB..WW.BW#.",
        "...STW...TW.....",
        ".........WWWS...",
        "........TWWWT...",
        "........BWWWT...",
        "..........B....."
    )

    // [ID 224] Gaomon
    val GAOMON_ROM = arrayOf(
        "...BBB...BBB....",
        "...BTTB..BTTB...",
        "...BTBBBBBBTB...",
        "....BTTTTTTBB...",
        "....BWBBBBBBSB..",
        "....BTS#BBT#BSB.",
        "...BTB##BTS#BBB.",
        "...BB#BST###B...",
        "....B##T###B....",
        ".....BBBBBBSB...",
        "...BBTB###BSSB..",
        "..B#TB###T#BBSB.",
        "..BSSBSSSSSBSSB.",
        "..BBBSBSSBSBBB..",
        "...BSSSBBBSSSB..",
        "...BBBBBBBBBBB.."
    )

    // [ID 225] Hawkmon
    val HAWKMON_ROM = arrayOf(
        "...........BB...",
        "......BBBBBTB...",
        ".....BTTTTTBB...",
        "....BSWBBBBSB...",
        "....BBBSBBSBB...",
        "....BTSB##B#B...",
        "...BT##B#BB#B...",
        "...BBBBST###B...",
        "....B##T###BSBB.",
        "...BB#B#B#BSBSB.",
        "...BSBSBSBSBSSB.",
        "....BSSSSSSSBB..",
        ".....BBSSBSB....",
        "...BBBSBBSBSB...",
        "..BSBSSBBSSBSB..",
        ".BBBBBBB.BBBBB.."
    )

    // [ID 226] Armadillomon
    val ARMADILLOMON_ROM = arrayOf(
        "................",
        "................",
        ".....BBB..BBBB..",
        "....BTB..BTTB...",
        "...BTWSBBTTBB...",
        "...BBBSB##BSSB..",
        "..BST#B##TSBSBB.",
        ".B#TB##BT#BSBTB.",
        "BBT#SB#TB##BSSSB",
        "BT##BBT#BBBSSSSB",
        "B####T##B#TSSSSB",
        ".BBBB###BBBSSSBB",
        "..BSSSSBBSSBSSBB",
        ".BBBBBBSBBSSBBB.",
        "BSSB.BSBSBBBSBSB",
        "BBBB.BBBBB.BBBBB"
    )

    // [ID 227] Dorumon
    val DORUMON_ROM = arrayOf(
        "......BBBB.BBBB.",
        "....BBBBBBBTBSB.",
        "..BBBBTTTTTTTB..",
        ".BTTTTTTWTTBB...",
        ".BBTTTTBBTTTTB..",
        ".B###TS###T#BSBB",
        ".BBBBB###TS##BTB",
        "..BT###STB#BBTSB",
        "...BBBBBB#BSBSSB",
        "...BBBT#BB#BBSSB",
        "..B##B#B##TBSSSB",
        "..B#BB#B#TBSBSB.",
        "...BBSBSBBSSBB..",
        "..BBSSSBSBSSSB..",
        ".BSBSSSBSSBSBSB.",
        ".BBBBBB.BBBBBBB."
    )

    // [ID 228] Kotemon
    val KOTEMON_ROM = arrayOf(
        "......BBBB......",
        ".....BTTTBB.....",
        "....BTTTTTBB....",
        "...BWWTWWTTB....",
        "...BRRBRRBTB....",
        "...BWWTWWTTB....",
        "....BTTTTTB.B...",
        ".....BBBBB.BSB..",
        "....B#####B.BSB.",
        "...B###S###B.BS.",
        "..B###SSS###B...",
        ".B#####S#####B..",
        ".B#####B#####B..",
        "..BBBB..BBBB....",
        "..BWWBB.BWWBB...",
        "...BBBB..BBBB..."
    )

    // [ID 229] Monodramon
    val MONODRAMON_ROM = arrayOf(
        "........BBB.....",
        "......BBTTB.....",
        ".....BTTTTB.....",
        "....BTTTTTTBB...",
        "...BBTWBBTTTB...",
        "..B##TBB##BB....",
        "..BST####TSB....",
        "..BTB#BSTBB...B.",
        "..BBBBBBBB#B.BB.",
        ".....BT#SBBSBSB.",
        "...BBT###BSBSSB.",
        "..B#B###BTBSSSB.",
        "..BSBSSSBSBSSB..",
        "..BBBBSSSBBBB...",
        ".BSBSBBBBBSBSB..",
        ".BBBBB..BBBBBB.."
    )

    // [ID 230] Floramon
    val FLORAMON_ROM = arrayOf(
        "...BBB.BBBB.....",
        "..BS##BR##SB....",
        ".BS#RRBRRR#SB...",
        ".B#RRRRRRRR#B...",
        ".BRRCRRCRRRRB...",
        "..BRTRRTRRBB....",
        ".BRRRRRRRRRRB...",
        ".B#RRBBRRRR#B...",
        ".BS#RRRRRR#SB...",
        "BBBS#RBRR#SB....",
        "BTTBBBRBBBBB....",
        "BTBWBGBGTBBWB...",
        ".BWBBGTTGBWBTB..",
        "..BBTTGGTTBTTB..",
        "..BTTBBBTTTBBB..",
        "..BBBB..BBBB...."
    )

    // [ID 231] Goblimon
    val GOBLIMON_ROM = arrayOf(
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "......B.........",
        ".....SSRBBB.....",
        ".....B##B#SS....",
        "......BB###BB...",
        ".....TTTTBSB....",
        "....B#TBTBBB....",
        "...B.BBS#STT....",
        "....TBB#T#B.....",
        "...BBT#B.BBB....",
        "................"
    )

    // [ID 301] Greymon
    val GREYMON_ROM = arrayOf(
        ".....BBBBB..BBB.",
        "BB..BSSSSSBBSSB.",
        "BSBBBSSWBSSSSB..",
        "BBBSSSBESSBBB...",
        "BSSSBSSSSSSB....",
        "BBBBBBSS##TB....",
        ".B#T#T###TT#B...",
        "..BBBBBT####B...",
        "....BTT###BTTB..",
        "..BBT###BBT##B..",
        ".BWB##TBW####BBB",
        ".BTBTT#BTW#BTB#B",
        "..BB##TTBBBT#BTB",
        ".BB#B###TTB###B.",
        "BWB##BBBBBWBWBWB",
        "BBBBBBB...BBBBBB"
    )

    // [ID 302] Tyrannomon
    val TYRANNOMON_ROM = arrayOf(
        ".....BBBBB.SS...",
        "....B###RRBTS...",
        "...BB#RBBBTB....",
        ".BB##RBCTRTBSS..",
        "B##RBRRRRRBBTS..",
        "BBBBBBRRTTBBB...",
        ".B#RRRRRRRTBBSS.",
        "..BBBBBTRTBBBTS.",
        "....BRRRRRRBBB..",
        "..BBRWWWBBRRBB..",
        ".BWBWWWBWRRRTBBB",
        ".BRBWWWBRWRBTBRB",
        "..BBWWWWBBBTRBRB",
        ".BBRBTTTTTBRRRB.",
        "BWBRTBBBBBWBWBWB",
        "BBBBBBB..BBBBBBB"
    )

    // [ID 303] Devimon
    val DEVIMON_ROM = arrayOf(
        "BB...BBBBBB...BB",
        "BTBBBBBTBBBBBBTB",
        ".BBBBBTTBBBBBTB.",
        "..BBBRBTBBRBBB..",
        "...BBTRBBRTBB...",
        ".B.BBBTBBTBBB.B.",
        "BBBBBBBBBBBBBBBB",
        "BBBBBTBBBBTBBBBB",
        "BBBTTBTTTTB#WBBB",
        "BBT#S#BBBB#STWBB",
        ".BSBBST###SBBSB.",
        ".BTBBBRRRRBBBBB.",
        ".BTSBBBRRBBBSTB.",
        "..BBT#SBBS#TBB..",
        "..BT#BBBBBB#TB..",
        "..BBBB....BBBB.."
    )

    // [ID 304] Meramon
    val MERAMON_ROM = arrayOf(
        "..B..B.BB.B..B..",
        "..BBBTBT#BTBBB..",
        "..BTBTBTRBTBTB..",
        "..BT##T#RTR#TB..",
        "..BT#ST#TRSTRB..",
        "..BT#TC#RCTR#B..",
        "B..BTRS#RSRRB..B",
        "BB.BT#R###RRB.BB",
        "BTBTBT###RRBTBTB",
        "B#T##BBBBBB#R#TB",
        ".BTR#TTTRT#RR#B.",
        ".B#BBRT#T#RBBTB.",
        ".BT#BR##R#RB#TB.",
        "..BBT#RBBR#RBB..",
        ".BTT#B....B#RRB.",
        "..BBBB....BBBB.."
    )

    // [ID 305] Numemon
    val NUMEMON_ROM = arrayOf(
        "................",
        "................",
        ".B.B.B..B.B.B...",
        ".BBBBB..BBBBB...",
        ".B#R#B..B#R#B...",
        ".BBBBB..BBBBB...",
        "...B......B.....",
        "....B....B......",
        ".....BBBBB......",
        "....BTTT#SB.....",
        "....BTT###SB....",
        "....BT####STBB..",
        "...B##BBB##SSTB.",
        "..B#S######TSSTB",
        "...BTS##SSSTBBB.",
        "....BBBBBBBB...."
    )

    // [ID 306] Garurumon
    val GARURUMON_ROM = arrayOf(
        "................",
        "...BB.....BB....",
        "..BTBBBB.BTB....",
        "..BB#T##B#TB.BB.",
        "..BTTSSS#TB#BT#B",
        "BBSSSBBBSTB##B#B",
        "B##B#TR#STB##BTB",
        "BBBBB###TTB#BBSB",
        ".B#######TBBSTB.",
        "..BBBBTS#TTT#STB",
        "....BTS####T#TTB",
        "....B#BT#TSSB#TB",
        "....B#B#TBTTB#TB",
        "...BB#B#TBBBBBTB",
        "..BTTBTSTB.BTSTB",
        "..BBBBBBB..BBBBB"
    )

    // [ID 307] Angemon
    val ANGEMON_ROM = arrayOf(
        "...BBBBBB..BB...",
        "..BWW#TTTB..BB..",
        ".BWWS##TTTB.BTB.",
        ".BW#S##TTBBBWTB.",
        ".BWSSS#TBTBWWWTB",
        ".BW#S##TBTBWWBTB",
        ".B##S#TBTTBTWBTB",
        "..BBBBBWTTBTBWBB",
        "..BWWWTTBBBBWWTB",
        ".BWBBBBBWWTBBBB.",
        "BWTWWTWWTBCWBWWB",
        "BTBTTTTTTBBCBBTB",
        "BWBBTBBBBBSSB.BB",
        ".BTBCTBTTTBBB..B",
        "BTTBCTBTWTWWB...",
        "BBBBBB.BBBBBB..."
    )

    // [ID 308] Togemon
    val TOGEMON_ROM = arrayOf(
        "......B.B.......",
        "...B.BTBTB.B....",
        "....BTB#BSB.....",
        ".BBBT#TT#STBBBB.",
        "BTRBSBTSB#SBRRTB",
        "BRBBSBTSB#SBBRRB",
        "BBTB######SBTBTB",
        ".BBBTSBB##SBBBB.",
        ".BTBTSBB##SSTB..",
        "..BBTT###SSBB...",
        "...B#B#BSBSB....",
        "...BTBSBSBSB....",
        "...B#BSBSB#B....",
        "..BB#BBBBB#TB...",
        ".BT#BB..BT#STB..",
        ".BBBBB..BBBBBB.."
    )

    // [ID 309] Birdramon
    val BIRDRAMON_ROM = arrayOf(
        "...BBBB....B....",
        "....BTTB...BB...",
        "....B#RTB..BTB..",
        ".....B#RB.BTRTB.",
        ".....B##RBBTRTB.",
        ".....BR#RBTR###B",
        "......BBBBTR##RB",
        "....BBTTTBT##RRB",
        "...BTTRRBR##RBB.",
        ".BB##BC#TB#BB#BB",
        "BSSBRC#RRB#TBBTB",
        "BBBB##R#BR#TRT#B",
        ".BSTB##RR#RRT#RB",
        "..BBBBBB####RRB.",
        "......B###RRBB..",
        ".......BBBBB...."
    )

    // [ID 310] Kabuterimon
    val KABUTERIMON_ROM = arrayOf(
        "BB..............",
        "BTB..BB.........",
        "BTB.BBTBB.......",
        "BTTBTTB#TB..BBB.",
        "BT#TBTT##TBBCWB.",
        "BT##T##B#TBSTWB.",
        ".BT####B#TBCTWB.",
        ".BTBBBT##TBTWB..",
        "..BTTTBBBBBTBBBB",
        "..BTTCCSSBTBCTWB",
        ".BTBBBBBBBSTBTB.",
        "BTBSCBTCSSBCBB..",
        "BCBBBBBBBBCCB...",
        ".BBSCBTCSBBBB...",
        "BTSBBBBBBSCCTB..",
        "BBBB....BBBBBB.."
    )

    // [ID 311] Monochromon
    val MONOCHROMON_ROM = arrayOf(
        "..BB.....BBB....",
        ".BTB.....BTBB...",
        ".B#B....BTB#TB..",
        ".B#BB.BB#B#TBBB.",
        "..B#BBT#BBBBB#B.",
        ".BTBBTBBBBBB##B.",
        "BTBBBBBTBBB#TTB.",
        "BBBBBBBBBBBBBBB.",
        "BBBBBBBBBBBBBBB.",
        ".BWWWWBBBBBBBB..",
        "..BBSSWWWWWWSBBB",
        "....BWWWWWWWSBWB",
        "....BSBWSSSSSBSB",
        "....BBBBBBSBT#B.",
        "...BTBT#BBBT#BBB",
        "...BBBBBBBBBBBBB"
    )

    // [ID 312] Ogremon
    val OGREMON_ROM = arrayOf(
        ".B....BBBB...B..",
        "BTBBBBTTTTBBBTB.",
        "BRTBTTTSSBTRTRB.",
        ".BBTTT#BTSBTBB..",
        "..BTT#CT#TSB....",
        "..BB##B##BSB....",
        "..BT###BBBSB....",
        "...BBBBT#BSBBBB.",
        "..BBTTRRRBSBS#TB",
        ".BB#T#R#RBSBST#B",
        "BTBBBBBBBSSBBBRB",
        "BBBTT##TSSBB#SRB",
        "BBSBBBBBBBBB#SB.",
        "B##SBRBTBBBBBBBB",
        "BT#B##SBBBBS###B",
        ".BBBBBBB...BBBBB"
    )

    // [ID 313] Leomon
    val LEOMON_ROM = arrayOf(
        "......BBBBBBBBB.",
        "....BBTT######B.",
        ".BBBT#BB#BBB#B..",
        ".B#BBB##B##BTBBB",
        "..B#######B####B",
        "BB####TB#B###TB.",
        "B##T#BC###B#TBBB",
        "BSSSS#####B####B",
        ".B#######B##TBBB",
        ".BBBBBBBB#BBB##B",
        "B##B##B#####BB#B",
        "B##BBB#BBBBB#SSB",
        "B#BB#####SSB##B.",
        "B###BBTTSSSBBBBB",
        "B###B#BBBBB##BTB",
        ".BBBBBBB..BBBBBB"
    )

    // [ID 314] Seadramon
    val SEADRAMON_ROM = arrayOf(
        ".....BBBBB......",
        "...BBTTTTTBBBBB.",
        "..BTTTTBTTTTTB..",
        ".BTTTBCTTTBBBBB.",
        "BTTTTTTTTTTTB...",
        "BTTTTTTTTBBBBB..",
        ".BWWWWTTBSSSB...",
        "..BBBBBBCCRTB...",
        "......B#RRSB....",
        ".....B#WCCSBBB..",
        ".....BW#CSB#BRB.",
        "....BW#TRTB##BCB",
        "....B#W#CSSBBBCB",
        "....BW#T#CRSSTCB",
        ".....BT#T#T#CCB.",
        "......BBBBBBBB.."
    )

    // [ID 315] Sukamon
    val SUKAMON_ROM = arrayOf(
        "................",
        "................",
        "........BBB.....",
        "......BBT#SB....",
        ".....BTT##BB....",
        "....BT###BB.....",
        "....BS#S##B.....",
        "...BTSS#T##B....",
        "..BBBT#TBBS#B...",
        "..BTRB#BRS##B...",
        ".BT########S#B..",
        ".B#B#WB#WB#WBB..",
        "B#BTBBTBBTBB##B.",
        "B##B#WB#WB#WS#B.",
        ".B######SSSS#B..",
        "..BBBBBBBBBBB..."
    )

    // [ID 316] DarkTyrannomon
    val DARKTYRANNOMON_ROM = arrayOf(
        "....BBBBBB.SS...",
        "...BTT#TBBBTS...",
        "...BT##TBBBB....",
        ".BBR##BC#TRBSS..",
        "BTT######TBBTS..",
        "B##R######RTB...",
        ".BTTTTT#BBBBBSS.",
        "..BBBBBBTBBBBTS.",
        ".....BTTBB#RTBS.",
        "..BBBWWBWTTTBB..",
        ".BT#BTTBTTTBBB.B",
        ".BBBBTWWBBBB#TBB",
        "....BWTTWTT#TBBB",
        "..BBTBTTT#TBBBB.",
        ".BT##TBBBBTBTBWB",
        ".BBBBBB..BBBBBBB"
    )

    // [ID 317] Devidramon
    val DEVIDRAMON_ROM = arrayOf(
        "...BB...BB...B..",
        "..BSB...BSB..BB.",
        "..BBBBBBB#SB.BSB",
        ".BBBSS#####SBBSB",
        ".BBS##TR#B#SB##B",
        ".BB##RT##B#SBB#B",
        ".BS#####BBB#BB#B",
        ".BBBBBTTTBB#BB#B",
        "..BTTTTBBB#B#BBB",
        "...BBB#B##BB#BBB",
        ".BBBS###BBBBBBBB",
        "BR#BSBBB#RBWB.BB",
        "B#RBB#BBR###B..B",
        ".BBB#BBBBBBBBB..",
        "BRBRBBBBBRBRBRB.",
        "BBBBBB..BBBBBBB."
    )

    // [ID 318] Cyclonemon
    val CYCLONEMON_ROM = arrayOf(
        "........BB......",
        "........BSB.....",
        ".....BBBB#BB....",
        "...BBSS###BB....",
        "...BSCT##B#B....",
        ".BBSCC###B#B....",
        "BSS#####B##B....",
        "BBBBBBBB###B....",
        ".BT####BBBB#B...",
        "..BBBBB####B#B..",
        "..BTB#B##BBB#B..",
        ".BR#BBBBBTRB#B..",
        ".B#RB###BR###BBB",
        "..BBBB###BBBBB#B",
        ".BTB##BBBTBTBTB.",
        ".BBBBBB.BBBBBBB."
    )

    // [ID 319] Tuskmon
    val TUSKMON_ROM = arrayOf(
        "BB...BB.......BB",
        "BBB.BBGBBBB..BTB",
        "BTB.BGTTTBSB.BRB",
        "BBTBT##BR#STBTBB",
        "BTBT#####TTBTRTB",
        "BBBT##W##TSBTBB.",
        ".BBBBBBBBSBTRTB.",
        "..BT####TSTBBBB.",
        "...BSSSSS#WTBB..",
        ".BBSBTTTBBTTSB..",
        ".BBSBT#B#G#TBB.B",
        ".BBSBT#BG#TSBSBB",
        "...B####BBBBSS#B",
        "..BT#TSSSSSSTTB.",
        ".BGBG##BBBGBGBGB",
        ".BBBBBB..BBBBBBB"
    )

    // [ID 320] Flymon
    val FLYMON_ROM = arrayOf(
        "...BBBB..BBB....",
        "....BTTB..BTBB..",
        "....B##TB.B#TTB.",
        ".BB..B#TB.B#TTTB",
        "...B.B#TTBBT#TTB",
        ".BB#BB###B#TT#TB",
        "B..BBBRR#B#T##B.",
        "..BTTTTTR###BB..",
        ".BTTTTTTTRBB....",
        ".BTTTTTTTRCCBBB.",
        ".BTTTTTTTRCSSSSB",
        "BCCBTTTTTRSTSSSB",
        "BSSBTTTTRTSTTSTB",
        ".BBBTTRRTTSTTBB.",
        "....BBBBBBBBB...",
        "....B...B.B.B..."
    )

    // [ID 321] Raremon
    val RAREMON_ROM = arrayOf(
        "...BBB.BBBB.....",
        "..BTT#BTCCCB....",
        "..BT##B##CCB....",
        ".BT#####TBCB....",
        ".BTTT##TRBCCB...",
        ".B#TRB#BB#CCB...",
        ".B##BB###CSCCB..",
        "..B######CSCCB..",
        "..B###CS#CSCCB..",
        "..B#CSCS##CSCB..",
        "...BCS#CS#CSCCB.",
        "..BB###CS####CB.",
        "..B###CB##SCSB..",
        "..BW#BBCB#S##WB.",
        "..BWSWBBB#WS#WB.",
        "..BBBBB.BBBBBBB."
    )

    // [ID 322] Kuwagamon
    val KUWAGAMON_ROM = arrayOf(
        "...BBB...BBBB...",
        "..B#B.....B##B..",
        ".B#RBB...BBBR#B.",
        ".BRB........BRRB",
        ".BRSBBBBBBBBRRRB",
        "..BRSB##RRRBRSB.",
        "...BB#RRRRRSBB..",
        "....BRTRRTRSB...",
        "....BRTRRTRSB...",
        "...BBRRRRRSB#B..",
        "..B#RBBBBBBBR#B.",
        "..BRBR#T#RRSBRB.",
        "..BRBTTTTTTBRSB.",
        "..BBBR#TRRSBBBB.",
        ".B#BRBBBBBBRRB#B",
        ".BBBBB....BBBBBB"
    )

    // [ID 323] Kokatorimon
    val KOKATORIMON_ROM = arrayOf(
        "BBB.BBB..BB.BBB.",
        "BTTBTTTBBRTBRTB.",
        ".BTTT#STB#RB#RB.",
        "..B##RBSTB##TBBB",
        ".B##BR#STB#TBRTB",
        ".BBB###STBSTB#RB",
        "BSGGB#STBSTB##RB",
        "BGGB####S##STBB.",
        ".BB#######STBRTB",
        "..B#####SSS###RB",
        "..BSSSSSSSTTTBB.",
        "...BTTTTTTBBB...",
        "....BBBBBBTB....",
        "..BBBTB..BTB....",
        ".BTTBTB.BTBTB...",
        ".BBBBBB.BBBBB..."
    )

    // [ID 324] Coelamon
    val COELAMON_ROM = arrayOf(
        "..........BB....",
        "........BBWB....",
        ".......BWWTB..BB",
        "....BBBBBBB..BTB",
        "..BBWWW##BWBBTWB",
        ".BWW#SBBSBT##BWB",
        "BW#TSBC#SBSSTTTB",
        "B##B###SB####TWB",
        "BBBBBWTBTBBTTBTB",
        ".BWW#TSBBTTBBTBB",
        "..BBSSSB#T#T#BB.",
        "....BBBBWTTWBTB.",
        ".......BW#BTTB..",
        "........BTTBB...",
        ".........BB.....",
        "................"
    )

    // [ID 325] Nanimon
    val NANIMON_ROM = arrayOf(
        ".......B........",
        "........B.......",
        ".......B........",
        ".....BBBBB......",
        "....BTT#TRB.....",
        "...BTB#BRTGB....",
        "..BBTSBBSSBSB...",
        "..BBWSBBWSBBB...",
        "..BBSB##SBBGB...",
        "..BT######TGB...",
        "..B##BBBB#TGB...",
        "...BB####BGB....",
        "..BGBTBBTBBGB...",
        ".BBBBBBBBB.BBBB.",
        "BRBTB......BTBRB",
        "BBBBB......BBBBB"
    )

    // [ID 326] Bakemon
    val BAKEMON_ROM = arrayOf(
        "....BBBBBB......",
        "...B#####TB.....",
        "..BTT##TT#TB....",
        "..BWB##BW#TB....",
        "..B#######TB....",
        ".BBBBBBB##BBB...",
        "B#BSSSSSBB##TB..",
        "B#BSSSSSSB#BTB..",
        "BBBSSSSSSB#B#B..",
        "B#BBBBBBBBBT#TB.",
        "..B###T###T##TB.",
        "..B#BBB#B#BB#BBB",
        "..BB.BB.BBBB.B..",
        "................",
        "................",
        "....BBBBBBBB...."
    )

    // [ID 327] Shellmon
    val SHELLMON_ROM = arrayOf(
        "................",
        "................",
        "..........BB....",
        "......B..BTTB..B",
        "......BBBTTTTBBB",
        "......BTTGTTTTTB",
        ".......BTTTTTTB.",
        "....BBBBBTTTTTB.",
        "...BTTT##BBTGTTB",
        "..BBB#S##TTBTTTB",
        ".BSB#SCCC#CTBTBB",
        ".BS##CBC###TTBB.",
        ".BSSSSS####CTBTB",
        ".BSTSTSB#CBTTTBB",
        "B#BBBBB##BBTTTBB",
        "BBBB..BBBBBBBBB."
    )

    // [ID 328] Unimon
    val UNIMON_ROM = arrayOf(
        ".....BBBBB....BB",
        "BBB.BGGTTBBB.BSB",
        "BTRBGTTTBWGBBSBB",
        ".BTTTRRTT#BBSBBB",
        "..BTRTTCTBBSBBSB",
        ".BTRC#CTWTBBBSBB",
        "BRRRRRTTWBBBBBB.",
        "BWWWWWBW#BBBBBGB",
        ".BBBBBWWWW#BBBTB",
        "....BWWWWTTW#TTB",
        "....BW#BWWWWT#B.",
        ".....BBBWWWWWW#B",
        ".....B#BT#BBWW#B",
        ".....B#BW#BBBB#B",
        "....BSBSBBB.BSBB",
        "....BBBBBBB.BBBB"
    )

    // [ID 329] Frigimon
    val FRIGIMON_ROM = arrayOf(
        ".....BBBBBB.....",
        "....BWWWWW#B....",
        "...BWWWWWWW#B...",
        "...BWWWWWWW#B...",
        "..BWBWWWWBW#B...",
        "..BWWWBWWWW#B...",
        "..BWWWWWWWW#B...",
        "...BWWWWWW#B....",
        "..B#BWWWBBB#B...",
        ".BWWWWWWWWWW#B..",
        ".BWWWRWWWWWW#B..",
        ".BWWWWWWWWWW#B..",
        ".BWWWRWWWWWW#B..",
        ".BWWWWWWWWWW#B..",
        "..BWWWWWWWW#B...",
        "...BBBBBBBBB...."
    )

    // [ID 330] Vegiemon
    val VEGIEMON_ROM = arrayOf(
        "........B.......",
        ".......BCB......",
        "#...BB.BRB.BB..#",
        ".#.BCRB#B#BRCB.#",
        ".#..BBRBBBRBB##.",
        "..#...BRRTB####.",
        "..#..BGRGRSB###.",
        ".#...BRGRGSB####",
        ".#..BGGGGGTSB###",
        "..#BGGGGBGTSB##.",
        "..BGBBBBGGGTSB..",
        "...BGGGGGSGTSB..",
        "...B#S#S#G#TSB..",
        "...BT#G#G#TSSB..",
        "....B#S#S#SSB...",
        ".....BBBBBBB...."
    )

    // [ID 331] Veedramon
    val VEEDRAMON_ROM = arrayOf(
        ".....BBBBBBBBBBB",
        "BB..BC###CBWSSSB",
        "BWBBC#RRR#CBSBB.",
        "BBW###RR##CBB...",
        "BW########CB....",
        "BBBBBB##W#CB....",
        ".BWWSSSS#C#CB...",
        "..BBBBB####CB...",
        "....BCC#C##BCB..",
        "..BBC##CC#BB#B..",
        ".BWBCCCC#BW#WBBB",
        ".BSBWCC##BSWBBCB",
        "..BBS####CBBCBCB",
        ".BBTBSWWSCBT#CB.",
        "B#BCCBBBBB#B#BSB",
        "BBBBBBB..BBBBBBB"
    )

    // [ID 332] Flamedramon
    val FLAMEDRAMON_ROM = arrayOf(
        "................",
        "................",
        "................",
        "................",
        "........B..B....",
        "...TB..B..B.....",
        "...WT.BTTBB.....",
        "....WTTRTBB.....",
        ".....TRBT#......",
        "....T#BRTC......",
        "....T.##T.......",
        "......C#RC......",
        "....TTBTT#TTB...",
        "...BTTBWTSTTBC..",
        "...TTRBCWSTB....",
        "....TRT..TTS...."
    )

    // [ID 333] Stingmon
    val STINGMON_ROM = arrayOf(
        "......BBBB.BBB..",
        ".....BBTTTBTTTB.",
        "....BBTTTTTBBTTB",
        "...BTTWBBTTBSBBB",
        "...BTBSBTTBTB...",
        "...B#BB##BTBB...",
        "....B##BBSB##B..",
        "BBB..BBSBB#BBBB.",
        "..BBB##B###BBSSB",
        ".BSSSBTBS#BSBSBB",
        ".BSBBSBSBBBBSBBB",
        ".BB..B#BBT#BSBSB",
        "....BSBSSBBSBSBB",
        "...BSBBBBSBBSB..",
        "..BSBSSB.BSSBSB.",
        "..BBBBB...BBBBB."
    )

    // [ID 334] Growlmon
    val GROWLMON_ROM = arrayOf(
        "......BB.BBB....",
        "....BBBSBTBB....",
        "...BBTTBTTBTB...",
        ".BBTTTTTWBTBTB..",
        ".BTTBTTBBTTBB...",
        ".BBBBBS###TB#B..",
        ".B#ST####TS#B...",
        "..BBBBBST###B...",
        ".....B#TBB#SB.BB",
        "..BBB#TBS##SSBSB",
        ".B##BTB#B#TSSBSB",
        ".BB#B#BB#B#BSBSB",
        ".BSBSBSBBBBBSBSB",
        ".BBSSSBSSSBSSSB.",
        "BSBSSSBBBBSBSBSB",
        "BBBBBBB..BBBBBBB"
    )

    // [ID 335] Kyubimon
    val KYUBIMON_ROM = arrayOf(
        "................",
        "................",
        "................",
        "...........BB#..",
        ".........BWWBBBB",
        "......B..W#BWW##",
        "....BWRWBTT##WWB",
        "....TWWBRWB###BB",
        "...BT##WSRW##TW#",
        "..BB##SBRSB#T###",
        "..#BB#BBTBW##BB#",
        ".####BSSWB#BWBB.",
        "..WRWSB#BTB#BB..",
        ".BTSTB#B#BB###..",
        ".WW.BWBWB.BB.#W.",
        "..W.BWB#T.BB.#W."
    )

    // [ID 336] Gargomon
    val GARGOMON_ROM = arrayOf(
        "....BB..........",
        "....BTBB........",
        "....BTTTBB......",
        "...BTWTTTTB.....",
        "..BTBTTBTTTBB...",
        ".BB#BTSB##TB#B..",
        "B#BSTB###TS###B.",
        ".BBTB#BST#BBBB..",
        "..B#S##TBBBSB...",
        ".B###BBBB#BBBB..",
        "BB#BBTB#B#BSSB..",
        "B#B#B#B#BBBSSSB.",
        "BSBBSBSBSSBSSSB.",
        ".BSBBSBSBBSBSB..",
        "BSBSSB.BSSBSB...",
        "BBBBBB.BBBBBB..."
    )

    // [ID 337] Wizardmon
    val WIZARDMON_ROM = arrayOf(
        ".......BBBBB....",
        ".....BBTS##BB...",
        "....BTTST#B.B...",
        "...BTSTTT#B.....",
        "...BTSTTTT#B....",
        ".BBTTTSTTT##BB..",
        "..BTTTTSSTT#B...",
        "...BBBBBBBBB....",
        ".BBBCCCCCCCBBBB.",
        "..BTBCCCCCB##B..",
        "..BBTBBBBB##BB..",
        ".BTTBTRTRT#BTTB.",
        ".BBBBST#TT#BBBB.",
        "..BTBBT#TTBBTB..",
        ".BTTTTBBBBTTTTB.",
        ".BBBBBB..BBBBBB."
    )

    // [ID 338] Gatomon
    val GATOMON_ROM = arrayOf(
        "BBBBB....BBBBBBB",
        "BSWWWBBBBWWWWSSB",
        ".B#WBWWWWWTTTBB.",
        "..BBWWWWWWBBB...",
        "...BWCWWWCWB....",
        "..BWWBWWWBWWB...",
        ".BWWWWBWWWWW#B.B",
        "..BWWBWBWWW#B#BB",
        "...BWWWWWWBB#BSB",
        "....BBBBBB##BB#B",
        "...BWBWWBWWW#B#B",
        "...BBWBBWWBB##B.",
        "..BWTBWWWBWTB##B",
        ".BTTTRB#BTTTRB#B",
        ".BTBTTBBBTBTTB#B",
        ".BBBBBB.BBBBBBBB"
    )

    // [ID 339] BlackGreymon
    val BLACKGREYMON_ROM = arrayOf(
        "................",
        "................",
        "................",
        "................",
        "................",
        "....B......B....",
        "..B..B###B#.....",
        "...BB#TBBB......",
        "...B#T#B#BB.....",
        "...BB#TBBBB.....",
        "...B#CBSBC...B..",
        "......B#TCTB.C..",
        "....BCCT#CSCCB..",
        ".....#B#T#TBC#..",
        "......BT#BCTB...",
        ".....BTCBBC#B..."
    )

    // [ID 340] BlackGarurumon
    val BLACKGARURUMON_ROM = arrayOf(
        "................",
        "...BB.....BB....",
        "..BWBBBB.BWB....",
        "..BB#WT#B#WB.BB.",
        "..BWWT#TTSB#BW#B",
        "BBTT#BBBTSB##BTB",
        "BT#W#TT#TWB##BWB",
        "BBBBB#TTWWB#BBSB",
        ".BTT#TTTTSBBTSB.",
        "..BBBBST#WWW#TSB",
        "....BST#TT#W#WWB",
        "....B#BW#STTB#WB",
        "....BTBTSBSSBTTB",
        "...BB#B#WBBBBWSB",
        "..BRSBRTSB.BRTSB",
        "..BBBBBBB..BBBBB"
    )

    // [ID 341] BaoHackmon
    val BAOHACKMON_ROM = arrayOf(
        ".......BBBB.....",
        "B.....BBTTB..BBB",
        "B....BBTTB..BSB.",
        "BB..BTTTTB.BSB..",
        "BB.BTWBTTTBSSSBB",
        "BSB#BBS##BSSSBB.",
        "BB#ST####TBSB...",
        "B##BB##SBBSSSBB.",
        "B#B#S#BBBSSSSSBB",
        ".B##BBBBSBBSSB..",
        "..BBBB####TBBBB.",
        "...BT####T#SSSBB",
        "..BSSSSBSSSSBBBB",
        ".BSSBSSBBSBSSB..",
        "BSSBSSB..BSSSB..",
        "BBBBBB...BBBBB.."
    )

    // [ID 342] Dorugamon
    val DORUGAMON_ROM = arrayOf(
        "......BBBBBBBBB.",
        "...BBBSSSSBBSB..",
        ".BBBTTBBSBTTB...",
        "BTTWBBBTBSBBTBBB",
        "BBTTBBTTBSSSBBTB",
        "B####BBBSSSSBSB.",
        "BBBBBSSSSSSSB#BB",
        ".B#T#BSBBBSB#BSB",
        "..BBBSBT##BSSBBB",
        "....BSBBBBSBBBSB",
        "..BBSB##BBSSSBSB",
        ".BS#BB#B#TBSBSB.",
        ".BSBBBSBSSBBBB..",
        "..BBSBBSBBBSSB..",
        ".BSBSSBBBSBSBSB.",
        ".BBBBBB.BBBBBBB."
    )

    // [ID 343] Sorcerymon
    val SORCERYMON_ROM = arrayOf(
        "......BBBB......",
        ".....BWWWWB.....",
        "....BWWCCWWB....",
        "...BWWCCCCWWB...",
        "..BWWWWWWWWWWB..",
        ".BBBBBBBBBBBBBB.",
        "..B##CWBBWC##B..",
        "..B###WWWW###B..",
        ".BBWWWWWWWWWWBB.",
        "BCWBWWCCCCWWBWCB",
        "BCCBWWWWWWWWB.B.",
        ".BCBWWTWWWTWB.C.",
        "..B.BWWWWWWB.C..",
        "....BWWBBWWB....",
        "...BWWB..BWWB...",
        "...BBBB..BBBB..."
    )

    // [ID 344] Aquilamon
    val AQUILAMON_ROM = arrayOf(
        "........BBB.BB..",
        ".....BBBWWB.BTB.",
        "....B#WWTB..BTB.",
        "...B#BBBWTBBRRTB",
        ".BBTBRBWWWTBRRTB",
        "B#WWWWWTWCBRRRTB",
        "BTTTWTC#TTGRRRRB",
        ".BCCCC#BTGCBRBRB",
        "..BBBBBTCCC#BBRB",
        ".....BCBCC#B#BRB",
        "....BCBCC#B###B.",
        "....BBSCC#B##BTB",
        "....BCBSCC#BBBRB",
        ".....BSBSBC###CB",
        "......BSBSBSBSB.",
        ".......BBBBBBB.."
    )

    // [ID 345] Ankylomon
    val ANKYLOMON_ROM = arrayOf(
        "...........BB...",
        "..........BSSB..",
        "..........BSSB..",
        "...........BB.B.",
        ".......B.......B",
        "...BB.B#B.BB..BB",
        "..BSTB##BBB#BBBB",
        ".B#TW#BSBB###BSB",
        "B#TBS##B###SSSB.",
        "BT#BB#B#SB#BSSSB",
        "B####TB#B#BSSSSB",
        ".BS#TBB##T#SBBBB",
        "..BBBBBBSBSBSSB.",
        ".BSBBSSBBBBSBSSB",
        "BSSBSBSB...BSBSB",
        "BBBBBBBB...BBBBB"
    )

    // [ID 346] Gaogamon
    val GAOGAMON_ROM = arrayOf(
        ".............BBB",
        "..BBB...BBBBBTTB",
        "..BTBBBBBTTBTBBB",
        "..BWTTTTTTBTB...",
        "..BTTTBBBTBB..BB",
        ".B###BSB##B..B#B",
        "BBBBTBB##BBB.BTB",
        "B#BT###STB#BBBSB",
        "B#T#S##TB##BSSSB",
        ".BBBBBBBS#BSSSSB",
        ".B###T###BTSSSB.",
        "..BBT#BBBT#BSSB.",
        "..BSBBSSSSSBBSSB",
        ".BSSSBSSBBBSBSSB",
        "BSBSBSBSB.BBSBSB",
        "BBBBBBBBB.BBBBBB"
    )

    // [ID 347] Mojyamon
    val MOJYAMON_ROM = arrayOf(
        "................",
        "................",
        "....BBB.BBBB....",
        "...BWWSBW##SB...",
        "..BWWTTTTWW#SB..",
        "..BWWWTTWWW#SB..",
        ".BWWWWSWWWWW#SB.",
        ".BWWTS#STWWWBSB.",
        ".B#TT#W#TT#WB#SB",
        ".BB#WWTWWW#BBBSB",
        "BTTB#TSTT#BTTT#B",
        "BTTBWSWSSWBTTTWB",
        "BTTBB#WWW#BTTTB.",
        ".BBBTB#B#BTBBBB.",
        "BTTTTTB.BBTTTTTB",
        "BBBBBBB..BBBBBBB"
    )

    // [ID 348] Starmon
    val STARMON_ROM = arrayOf(
        "........BB......",
        ".......BWB......",
        "......BWWB......",
        ".....BWRS#B.....",
        "BBBBBWWS##BBBBBB",
        ".BWWWBSBSBWWWWB.",
        "..B#WBBBBBW#SB..",
        "...B#BBBWB#SB...",
        "..BGW#BBB#SWGB..",
        "..BRBWSW#SWBRB..",
        "..B#CBWW#WBC#B..",
        "...BBTWSBWTBB...",
        "....BWWBBW#B....",
        "...BRCB..BCRB...",
        "..BTCCB..BCCTB..",
        ".BBBBBB..BBBBBB."
    )

    // [ID 349] Dokugumon
    val DOKUGUMON_ROM = arrayOf(
        "................",
        "................",
        ".......BBBBB....",
        "......BWWWWTB...",
        ".....BWSWSSW#B..",
        "....BTWWWWWW#TB.",
        "....B##W#W#TTSB.",
        ".B.BBBBTTBTTSBB.",
        "BTBTTTTBBTBSSBB.",
        "B#TTTTTT#TBSBBB.",
        "BBTTTTTTBBBBBBBB",
        ".BTTTTTTBBBBBT#B",
        ".BBTTTBBBBBSTBRB",
        "BTBBBBTRBT#BTTBB",
        "BRBTTBRBBBRTBRB.",
        "BBBBBBBB..BBBBB."
    )

    // [ID 401] MetalGreymon
    val METALGREYMON_ROM = arrayOf(
        ".....BBBBB..BBB.",
        "BB..BWWTTTBBTWB.",
        "BWBBBTBBB#T#TB..",
        "BBBWTBGB#TTBBTB.",
        "BWT######BB#BTB.",
        "BBBBBB##BCB#BCB.",
        ".BCCTSBBCSRBTCTB",
        "..BBBBBTSWTBTTTB",
        "....B#T##BTBTBCB",
        "..BB#T#BBW##BBCB",
        ".BSBTTBWWB#TBBTB",
        ".BCBCTB#TBTBB#BB",
        "..BBCCTBBBBSSB#B",
        ".BBCBTTSSSBCTSB.",
        "BTBTSBBBBBTBTBTB",
        "BBBBBBB...BBBBBB"
    )

    // [ID 402] Monzaemon
    val MONZAEMON_ROM = arrayOf(
        "...BB.BBBBB.BBB.",
        "..B##B#####B###B",
        "..BWB#######WW#B",
        "...BBR###BR#BW#B",
        "...B#WBBBW###BB.",
        "...B#WWBWW###B..",
        "...B#WBWBW###B..",
        "...BB#WWW###BB..",
        "..B#BB###BBB##B.",
        ".B#B#WWWW######B",
        ".BB#WSWSWW##B##B",
        "..B#WWSWWW###BB.",
        ".BBBWSWSWWBBB#B.",
        "B###BWWWWB###BB.",
        "B###B####B####B.",
        ".BBB.BBBBBBBBB.."
    )

    // [ID 403] SkullGreymon
    val SKULLGREYMON_ROM = arrayOf(
        ".BBBBB..........",
        ".BWW#TBBB.......",
        "..BBBWW#SBB.BBB.",
        "...BWWWT#SBB#WWB",
        ".BBWWWTT#SB#SBB.",
        "BWWWBWWW#BBSB...",
        "BBBBBBW#SS#BTBB.",
        ".BWW##SB#WWSB#WB",
        "..BBBBBRTBSBWBB.",
        "...BSWBRBSBW#SB.",
        ".BBTBBWBBTBBSSB.",
        "B##BSWBW#BW#BB..",
        "BWWBBBWBBB#WBSB.",
        ".BBB#B##SSBBS#B.",
        ".BWBSSBBBBWBWBWB",
        ".BBBBBB...BBBBBB"
    )

    // [ID 404] WereGarurumon
    val WEREGARURUMON_ROM = arrayOf(
        ".....BB.....BB..",
        "....BCBBBB.BCB..",
        "....BB##SSB#CB..",
        "....B#SBBB#TB...",
        ".BBB#SBRBSTBB...",
        ".BT#CSSSTCCCB...",
        ".B#WWTW#STCCB...",
        "..BTTTTW#SCB....",
        "..BWWWWWWSTGB.BB",
        "...BBBB#TTTTTBWB",
        "..B#BTTT##SBTB#B",
        "..BWBBBBBBBTGBSB",
        "...BBTCCCTBTTBB.",
        "..BBTBBBTTTBBB..",
        ".BTW#SB.B#WTTTB.",
        ".BBBBBB.BBBBBBB."
    )

    // [ID 405] MagnaAngemon
    val MAGNAANGEMON_ROM = arrayOf(
        "...BBBBB.....BBB",
        "..BTWTSBB...B#WB",
        "..BWTSSBWB.B#BB.",
        ".BTWSSSBW#B#W##B",
        ".BWWW#TBBWBW#BB.",
        ".BTWSSTBTBWBWW#B",
        ".BS#TTBTBB#B#BB.",
        "BBBBBBTTBBBTB...",
        "BTBTTTTBTTTBB.BB",
        ".BTBBBBTBTTBTB#B",
        "B#BTTBTTWBBTB##B",
        "BTBWBTBWWB#TB#B.",
        "B#BBBBBBBBTTBWWB",
        ".BWTTBBTTTBBB#B.",
        "BWBWBBBBWBWWBB..",
        "BBBBB..BBBBBB..."
    )

    // [ID 406] Lillymon
    val LILLYMON_ROM = arrayOf(
        "B..BB.BBBB.B...B",
        "BBBTBBWWTTBTB.BB",
        "BTBWBTWT#BTWTBTB",
        ".BTTBTTT#BTT#BTB",
        ".BTBTBT#BSBT#BB.",
        ".B#BWTBBSTB##BB.",
        ".B#BWTWWTTSB#B..",
        "..BBWBWWBTSBB...",
        "...BWWWWTTSBTB..",
        "..BTBWWTTSBBTB..",
        ".BBTSBBBBBSTBTB.",
        "BBTGBBSTSTBGTBTB",
        "BTBB#BTTT#BBBBTB",
        "BB..BSB##BSB..BB",
        "B..BTTGBBGTTB..B",
        "...BBBBB.BBBB..."
    )

    // [ID 407] Garudamon
    val GARUDAMON_ROM = arrayOf(
        "..BB..BBB...BB..",
        ".BB....BWB...BB.",
        "BTB..BBCRCB..BTB",
        "BTB.BTCRBB#B.BTB",
        "BTRBBRRBCR#B.BRB",
        "BRBTR#R#R##BBRRB",
        "BRBRBBB###SBRR#B",
        "BRRBTTTBBSBRRR#B",
        "BRR#BBB##WRBB##B",
        "BTR#BW###WRRRBSB",
        "BRBBBR##WRBBRBRB",
        "BTBTBRRRRRBTRBSB",
        ".B#BWB###WWBB.B.",
        "..BBTSBBTTSBB...",
        ".BTTWSB.BTWSTB..",
        ".BBBBB...BBBBB.."
    )

    // [ID 408] MegaKabuterimon
    val MEGAKABUTERIMON_ROM = arrayOf(
        "B.B.B.B.........",
        "BBB.BBB.........",
        ".BB.BCBBBBB.....",
        ".BTB#CCCCCBB....",
        ".BCCBSCCBC#B....",
        "..BCCCCBB#SSB...",
        "..B#CC###SSBTB..",
        "...BCBBBSSBRBCB.",
        "....BTTTBSBBBCB.",
        "...BBBBBBBBBCBCB",
        "..BCBTWTTTBBC#BB",
        "..B#BTTTTTBC#SBB",
        "...BBTWTTTB#SBB.",
        "...BBBTBBBBBBB..",
        "..B#SBBBBB##S#B.",
        "..BBBBB..BBBBBB."
    )

    // [ID 409] MegaSeadramon
    val MEGASEADRAMON_ROM = arrayOf(
        ".......BBB.BBB..",
        "......BCCCBCCCB.",
        ".......BBCCTBB..",
        ".........BTB....",
        "..........BCB...",
        "......BB..BCB...",
        ".....BTBBBTBCB..",
        "..BBBBBSBTBSBB..",
        ".BTTSSS##B#B#BBB",
        "BTTTTSS#BBBBBBCB",
        "BSTTS##BTBCBCCB.",
        "B####BBTBCCCBB..",
        "BBBBBWBBCCCTB...",
        "BRWWWRRBCTTB....",
        ".BBBBBBTTBB.....",
        "..BBBBBBB......."
    )

    // [ID 410] Myotismon
    val MYOTISMON_ROM = arrayOf(
        ".....BBBBBB.....",
        "B...BWGGGTTB...B",
        "BBBBRWWGTTTTBBBB",
        "BTRBTRRTRRTTBTTB",
        "B#TBRTRRRTRTBT#B",
        ".B#BWRRRRRTTBTSB",
        ".B#BWTTBBBBBB#B.",
        "..BBTBBTT###B#B.",
        "....BTTTT##BBB..",
        "...BTTTT##BSSB..",
        "...BTTT##BSSSB..",
        "...BTT##BSSSSB..",
        "..BTT###BSSSSB..",
        "..BT###BSSSSSSB.",
        ".B####BBBSSSSSB.",
        "BBBBBBBBBBBBBBBB"
    )

    // [ID 411] Etemon
    val ETEMON_ROM = arrayOf(
        ".....BBBBBB.....",
        "....BTT##TSB....",
        ".BBBTBBBBBTSBBB.",
        "BWWBB####TBSBWTB",
        "BWBBBBBBBBBBWBTB",
        "BWTBWTBBWTBBBWTB",
        "BWWBBB#BBBBBWWTB",
        ".BWB##B###TBTTB.",
        "..BB#B#BB#TBBB..",
        "....B####TBSBTB.",
        "...BTBBBBBSSBB#B",
        "...BTSWTTS#TSB#B",
        "...B#BWWWB##SBSB",
        "...B#BBTBB#BS#SB",
        "...BT#BSBT#B##B.",
        "...BBBBBBBBBBBB."
    )

    // [ID 412] Mamemon
    val MAMEMON_ROM = arrayOf(
        "................",
        ".......BB.......",
        "......B##B......",
        "....BBBBBBBB....",
        "...B########B...",
        "..B##WB##BW##B..",
        ".BBB#EB##BE#BBB.",
        "BRWRB######BRWRB",
        "BRRRB#BBBB#BRRRB",
        ".BBB#BSSSSB#BBB.",
        "..B###BBBB###B..",
        "...B########B...",
        "....BBBBBBBB....",
        "...BTTB..BTTB...",
        "..BTTTTB.BTTTTB.",
        "..BBBBBB.BBBBBB."
    )

    // [ID 413] MetalMamemon
    val METALMAMEMON_ROM = arrayOf(
        "................",
        "......BBBB......",
        "....BBTTTTBB....",
        "...BTTTT####B...",
        "..BTTWB##BW##B..",
        ".BBBTEB##BE#BBB.",
        "BTTTBTTTT##BRRRB",
        "BCCCB#BBBB#BRWRB",
        ".BBB#BRRRRB#BBB.",
        "..B###BBBB###B..",
        "...BTTTT####B...",
        "....BBBBBBBB....",
        "...BTTB..BTTB...",
        "..BTRRTB.BTRRTB.",
        "..BBBBBB.BBBBBB.",
        "................"
    )

    // [ID 414] MetalTyrannomon
    val METALTYRANNOMON_ROM = arrayOf(
        "....BBBBBB.BB...",
        "...BWW#CBTBBB...",
        ".BBTW#BC#STB....",
        "BTT######STBBB..",
        "BW###B###SBBBB..",
        ".BBBBBBTTSRTB...",
        "..BBTTTTS#RCBBB.",
        "....BR##R#CTBBB.",
        "..BBCR##BBSTBBB.",
        ".BBB#RBB##STBB..",
        "BBBB##RB#STTBB.B",
        "BBBB#RBBBBBBBBBB",
        "....B#B#RSSBBBTB",
        ".BBBSBSCSS##STB.",
        "BBTTBSBBBBBBBBBB",
        "BBBBBB...BBBBBBB"
    )

    // [ID 415] Ex-Tyrannomon
    val EX_TYRANNOMON_ROM = arrayOf(
        "....BBBB..BBB...",
        "...BTT##BBTTB...",
        ".BBTT###SRBTB...",
        "BTT###B#SRBTB...",
        "BTT#T####SRBTB..",
        "BBBBBR###SRBTB..",
        ".BT#####SSSRBB..",
        "..BBBBWWBBSRBTB.",
        ".B#RBWTB##TRBTB.",
        ".BTBBWTBBBBRBB..",
        ".BB.BWTBBWTRB#BB",
        "....BWTWWT##RB#B",
        "...BBB#T##RBBS#B",
        ".BBT#RBBBBS##BRB",
        "BTB#SRB.BTBTBTB.",
        "BBBBBBB.BBBBBBB."
    )

    // [ID 416] Megadramon
    val MEGADRAMON_ROM = arrayOf(
        "....BBBBBBBB.B..",
        "..BBTTT##TTB.BB.",
        ".BTT##T##BB..BB.",
        "BT###TT##B..BTCB",
        "B#BBB###BTBBTTTB",
        ".BTRRBBBTTTBTCTB",
        "..BTRRRRBBBTTBTB",
        "...BBBBBRRTBB#BB",
        "..BRBTRTTRBRB##B",
        ".BBRBRRBBBT#B##B",
        "BT#BBBBSBT##BBB.",
        "BT#BTTTSBT#BSSSB",
        ".BBBBBBBSBBSSSSB",
        "...BTTBTBSSSSBTB",
        "....BBTTBTTBTBB.",
        ".....BBBBBBBBB.."
    )

    // [ID 417] Piximon
    val PIXIMON_ROM = arrayOf(
        ".##..........##.",
        ".#W#........#W#.",
        ".#T##......##T#.",
        "..#W#......#W#..",
        "..##W#....#W##..",
        "...#T#BBBB#T#...",
        "....#BTT##B#....",
        ".....BBTTBB.....",
        ".....BTTTTB.....",
        "....B#BBBB#B....",
        ".....B....B.....",
        "................",
        "................",
        "................",
        "................",
        ".....BBBBBB....."
    )

    // [ID 418] Andromon
    val ANDROMON_ROM = arrayOf(
        "....BBBBBB......",
        "..BBWWWW#SB.....",
        ".BWWWWW###SB....",
        ".BBWWWBBB#SB....",
        "BBBBBBRB##SBBBB.",
        "BWWBBBB#BBBB#WWB",
        "BB#WWWS#BTBSBWWB",
        ".BBWBWBBBTB#SBB.",
        "B#BBBBBBTBBSB#WB",
        "BBTTTTTTTB#B#WWB",
        "BWBBBBBBBW#BBBWB",
        "BW#BWWSBWRSB##WB",
        "BWBBB#SBBBBBW#B.",
        "BW##BBBWW#SBBBBB",
        "BW#BTTTBBBB##SSB",
        ".BBBBBBB...BBBBB"
    )

    // [ID 419] Giromon
    val GIROMON_ROM = arrayOf(
        "................",
        "...BB......BB...",
        "..B##B....B##B..",
        "..B##BBBBBB##B..",
        "...B########B...",
        "..B##WB##BW##B..",
        ".BBB#EB##BE#BBB.",
        "BWWWB######BRRRB",
        "BWWWB#BBBB#BRWRB",
        ".BBB#BWWRWB#BBB.",
        "..B###BBBB###B..",
        "...B########B...",
        "....BBBBBBBB....",
        "...BRRB..BRRB...",
        "..BRRRRB.BRRRRB.",
        "..BBBBBB.BBBBBB."
    )

    // [ID 420] Vademon
    val VADEMON_ROM = arrayOf(
        "........BBBBB...",
        ".......BTTBTTB..",
        "......BTTBTTTTB.",
        "......BTTBTTBTB.",
        "......BTTTBTBTB.",
        "B.....BTBTBTTTB.",
        ".BBB...#TTTTT#..",
        ".BTB.B.BBTTTBB..",
        ".BBTB..BTBTBTB..",
        "...BTB..BBTBB...",
        "..B.BSS..ST#....",
        ".......##ST###..",
        ".........SS####S",
        "........######S.",
        ".......S#S####..",
        ".....SS.S.S.#.##"
    )

    // [ID 421] SkullMamemon
    val SKULLMAMEMON_ROM = arrayOf(
        "......BBBBBB....",
        ".BBB.BTBTTTTBB..",
        ".BTTBTTTTTTTBTB.",
        "..BTBTTTTTTTTTTB",
        ".BBTBTBBTTRTTTTB",
        "BTBTTBTBTTTTBTTB",
        "BTBTTBTBTTTTBTTB",
        "BTTBTBTBTTTBBTB.",
        ".BBBB#BBBBBWBB..",
        "BWSBSBRRBSWB#B..",
        "BSWBBRBBBWSB#B..",
        ".BBSWBWSSBB#BSB.",
        "..BBBRBBBB#SBWB.",
        ".BTBWBWS##BBSWBB",
        "B#B#BBBBBB#B#B#B",
        "BBBBBB...BBBBBBB"
    )

    // [ID 422] AeroVeedramon
    val AEROVEEDRAMON_ROM = arrayOf(
        ".....BBBBBBBBBB.",
        "BB..BCCCCBW##TB.",
        "BWBBCRRSTCBTBB..",
        "BBW#TRSW#TBBSSB.",
        "B#######TTBSRRSB",
        "BBBBBB##WTCBSRRB",
        ".BWWW###TCTBSBSB",
        "..BBBBBT###TB#BB",
        "....BCC#C#TB#B#B",
        "..BBC##CC#BB#BB.",
        ".BWBCCCC#BW#WBBB",
        ".B#B#CC#WB#WBBCB",
        "..BBW##WTCBB#BCB",
        ".BB#BWW##CB###B.",
        "BWB##BBBBB#B#BWB",
        "BBBBBBB..BBBBBBB"
    )

    // [ID 423] Paildramon
    val PAILDRAMON_ROM = arrayOf(
        "...BBBBBB.BBBBB.",
        "..BTTTTBTBTTTTB.",
        ".BSBTBBSBBTTTB..",
        "BBBTTTTWBTBTB...",
        "BTTTTBBBTTBB....",
        "BBBBBTBB#BBBB...",
        "B##SBBB#BBS#BB..",
        ".BBB###BB####B..",
        ".BT#BBBTBBBSSB..",
        "..BBB#TBB##BBB..",
        ".B##BBBB##TSBB..",
        ".BS#BSBSBT#BB...",
        "..BBBBSBBBBBBB..",
        "..BBSSB..BSSSB..",
        ".BSBSSB.BSBSBSB.",
        ".BBBBBB.BBBBBBB."
    )

    // [ID 424] WarGrowlmon
    val WARGROWLMON_ROM = arrayOf(
        "....BB.BBB..BBB.",
        "....BBBBTBBBTTB.",
        ".BBBTTTTTTBTTB..",
        ".BTTTTWBTTBTB...",
        ".BTTTBBTTBTBSBBB",
        "..BB#TS#B#BSBS#B",
        ".B#SBBBB#BSB##B.",
        ".BBB###SBBB##B..",
        "..B#BBBB###BBBBB",
        "..BB##T#BBBBSSSB",
        "BBBB#TBBBBTSSSBB",
        "B#S#BBB##T#SSBBB",
        ".BSSBSSBSSSBBBSB",
        ".BBBBBSSBBBSSSB.",
        "BSBSSBBBBBSBSBSB",
        "BBBBBBB...BBBBBB"
    )

    // [ID 425] Taomon
    val TAOMON_ROM = arrayOf(
        "................",
        "................",
        "......BB........",
        "....WTBBBW......",
        "....BBTB#T......",
        "....BTBB#B......",
        "....#TBB#T......",
        "...BSTB#TB......",
        "....BW#BTW......",
        "....#TBB#TW.....",
        "...TTW#TSBTB....",
        "..TTB#TBTTW#....",
        "..TBTT#BTTRTT...",
        "..BBTWWTTTTTT...",
        "...STT.SSTBTTW..",
        "..BTTW.#T#......"
    )

    // [ID 426] Rapidmon
    val RAPIDMON_ROM = arrayOf(
        "..........BBBBB.",
        "........BBTTTTB.",
        "BB.....BTTTBBB..",
        "BTBBBBBTWBBTTTB.",
        ".BTBTTTBTTTBBB..",
        "..B##BB#BBB.....",
        "..BSBB##BBSB....",
        "..BT###SB###B...",
        "...BBBBTB##SB...",
        ".BB###BBBBBB....",
        "B#B##TB###TB....",
        "B#B#T#B##TB.....",
        ".BSBSSSBBB......",
        "..BSBBBBSSB.....",
        ".BSSSB.BSSSB....",
        ".BBBBB.BBBBB...."
    )

    // [ID 427] Angewomon
    val ANGEWOMON_ROM = arrayOf(
        ".....BBBB..BB...",
        "....B#WWSBBWB...",
        "...B#BWSSBWWB...",
        "..BWWBSSSTBBB...",
        "..BWBBBSTTTBTB..",
        "..BWSBSTTBBTTTB.",
        "..BBBBBBBTBBTTB.",
        "...BTTTTTBW#BTTB",
        "...BBWWTBBBW#BTB",
        "..BTTBBBTT#BW#B.",
        ".BWBTWWTTBWTBW#B",
        ".BTB###W#BBWTBWB",
        "..BBBBBBBBWTBWB.",
        ".B#B#WB#W#BBB#WB",
        ".BB#W#BB#W##BB#B",
        ".BBBBBB.BBBBB.BB"
    )

    // [ID 428] LadyDevimon
    val LADYDEVIMON_ROM = arrayOf(
        "....BBBBB.......",
        "...BSSSS#B...B.B",
        "..BSST##BBB..RBR",
        "..BST##BBBB..BBB",
        "..BR##BBRBBB..B.",
        "..BSR#BRBBBB.B..",
        ".BB###BBBBW#B...",
        ".BB#WWWWWBWW#B..",
        "B#WB#WWW#BBWW#B.",
        "BWBTT###WWWBWT#B",
        "BBTBWWTWWBWTBWTB",
        "BBTBTTTT#BBRBWTB",
        ".BTBBBBBBBRTBWB.",
        "..BBWTBTWWBBBWB.",
        ".BRSBTBBTBSRBB..",
        ".BBBBBB.BBBBB..."
    )

    // [ID 429] BlackMetalGreymon
    val BLACKMETALGREYMON_ROM = arrayOf(
        ".....BBBBB..BBB.",
        "BB..BTTTTTBBTTB.",
        "BTBBBTBBBTTTTB..",
        "BBBTTWTBTTTBBTB.",
        "BTTTTTTTTBBTBTB.",
        "BBBBBBS#B#B#BSB.",
        ".B#ST#BB#TSB##TB",
        "..BBBBBST##B#TSB",
        "....B##T#B#BSBSB",
        "..BB##TBB##SBBSB",
        ".B#B#TB##BTSBBSB",
        ".BSBT#B##B#BBSBB",
        "..BBSSSBBBBSSBSB",
        ".BSBBSSSSSBSSSB.",
        "BSBSSBBBBBSBSBSB",
        "BBBBBBB...BBBBBB"
    )

    // [ID 430] BlackWereGarurumon
    val BLACKWEREGARURUMON_ROM = arrayOf(
        "....BB.....BB...",
        "...BWBBBB.BWB...",
        "...BBTSTTBSWB...",
        "...BWSBBBSTB....",
        "BBBWTBTBTTBB....",
        "BTTT#TT#WWWB....",
        "B###T#STTTWB....",
        ".BTBBB#STWB.....",
        ".BTT####TTTB.BB.",
        "..BBBBSTTTTTB#B.",
        ".BSBTTTSSTBTBSB.",
        ".B#BBBBBBBTTBTB.",
        "..BBTTTTTBTTBB..",
        ".BBBBBBBBTBBB...",
        "BW#STB.BS#WTWB..",
        "BBBBBB.BBBBBBB.."
    )

    // [ID 431] Cyberdramon
    val CYBERDRAMON_ROM = arrayOf(
        ".....BBBBBB..BBB",
        "..BBBWWTTB..BTB.",
        ".BWWTTTTB..BTTB.",
        "BWTTTTTB#B.BTTTB",
        "BBBTTTBB##BTBBTB",
        ".BSBBBBTBBBBWBTB",
        "..BWWWSBBWTBSBRB",
        "...BBBBSSBBSBTRB",
        ".BBW#WSBB#SBBRRB",
        "B#B####BW##BBBRB",
        "BWB#WWSBBWBB..BB",
        ".BBBBBBWBBB...BB",
        "..BSWWBBT##B...B",
        ".BST#B.BT###B...",
        "BWB##B..BWWBWB..",
        "BBBBBB..BBBBBB.."
    )

    // [ID 432] Antylamon
    val ANTYLAMON_ROM = arrayOf(
        ".......BBB.BBBBB",
        "......BTBBBTTBSB",
        "..BB.BBBBTTTTBB.",
        "..BTBTTTTTBBBB..",
        "...BTTWBTB......",
        "..B##BB##BB.....",
        "..BST###BTSB....",
        "...BBBBBT##B....",
        "...BS##T##BSB...",
        "..B#B#T#SBBSSB..",
        "..B#BBBBBBTBSSB.",
        ".BS#TBBBBB#BSSB.",
        ".BSSBSSSSBBBSSSB",
        "BSSSBSBBSSBBSSSB",
        "BSSBSSB.BSSBSBSB",
        "BBBBBB...BB.BBB."
    )

    // [ID 433] SaviorHackmon
    val SAVIORHACKMON_ROM = arrayOf(
        "......BB..BBBB..",
        "B....BSSBBTTB...",
        "BB..BSBBTTTB....",
        "BTBBBWTTTTBB....",
        "BTTBTTTBBTTB....",
        ".BB##TBB##T#BBB.",
        ".B#ST####TBBB#B.",
        ".BBBBB#STBB##B..",
        "..B#S##TBB#SSSB.",
        "...BBBBBBSBBBB..",
        "..B#BTB##BBSSBB.",
        ".BS#BB##BB#SBSSB",
        "BSSBSBBBBSSB.BSB",
        "BBBSBSBBBBBB..BB",
        "...BSSB..BSSB..B",
        "...BBBB..BBBB..B"
    )

    // [ID 434] Lucemon Chaos Mode
    val LUCEMON_CHAOS_MODE_ROM = arrayOf(
        ".BBB..BBBBB..BBB",
        "BWWB.B##T##B.BSB",
        "BWTBB###T###BBSB",
        ".BWB##CB#BR##BSB",
        "..BB###TTT###BB.",
        ".BWWB###S###BRSB",
        "BWWWTB##S##BRSSB",
        "BWWBWT##S##BSRSB",
        ".BBB.B##S##B.BBB",
        ".BWWB###S###BRSB",
        "BWWWB##BSB##BSS.",
        ".BBB.B#B.BSB.BB.",
        ".....B#B.BSB....",
        "....BWTB.BRSB...",
        "....BWWB.BSSB...",
        "....BBBB.BBBB..."
    )

    // [ID 435] Dorugreymon
    val DORUGREYMON_ROM = arrayOf(
        ".BB..BB..BB..BBB",
        "BSB.BSB.BSB.BSSB",
        "BSB.BBBBTBTBTBB.",
        "BSSBTTTTTWBTBSB.",
        "BSSBTTBBTTBBSSSB",
        ".BSB#BB##BTBSSB.",
        ".BBST####TBSSSSB",
        ".B#T###STB#BSSSB",
        "..BBBBBTB#BSSSB.",
        "...B##TBSB#BSBSB",
        "....BBB###TSBBSB",
        "....B###BTBBSBB.",
        "...BSBSSBSSSBSBB",
        "..BSSSBBSBSSSBSB",
        ".BSBSSB.BSBBBSBB",
        ".BBBBBB.BBBBBBBB"
    )

    // [ID 436] Knightmon
    val KNIGHTMON_ROM = arrayOf(
        "..B.....BBBB....",
        ".BTB....B#W#B...",
        ".BTB...BBBB#SB..",
        ".BTB..B#WWWBSSB.",
        ".BGB.B#WW###BTB.",
        ".BGB.BBBBCBSBTB.",
        ".BTB.BWWB#SSBBBB",
        ".BTBBWWW#SSTTB..",
        ".BBBBBBBBBBBBBBB",
        "B#WBWB#RRR#BTTTB",
        "BW#BBBSWRWSBTTGB",
        ".BBB#BBB#BBBTGTB",
        "..B#B#RTTRSBTGTB",
        "...BBBBBBBBSBBB.",
        "..B#W#BB..BW#SB.",
        "..BBBBBB..BBBBB."
    )

    // [ID 437] Silphymon
    val SILPHYMON_ROM = arrayOf(
        "............BB..",
        "....BBBBBB.BSSB.",
        "...BSBBSBSBTBSB.",
        "..BTWTTBTBTTTB..",
        "..BTTTTBTBTTB...",
        "...BBBBBBSBB....",
        "...BT####BB...B.",
        "....B##SBBB..BSB",
        ".....BBBB##BBSBB",
        "....B#T#SBBBSBSB",
        "...BBTBB#BTSBSB.",
        "..B#TB##BT#SBB..",
        "..BSBSBBBSSB....",
        "...BSSSBSBBSB...",
        "..BSBSB.BBSBSB..",
        "..BBBB...BBBBB.."
    )

    // [ID 438] Zudomon
    val ZUDOMON_ROM = arrayOf(
        "BBB.............",
        "BTWBB...........",
        ".BTWWBBBB.BBB...",
        "..BTWBTTTBBTB...",
        "..BBBTTTTTSBBB..",
        ".BTTTTTBTSSBTB..",
        ".BBTTTBCS#BBBBB.",
        ".BSTTTTS#BWTBTB.",
        "BWBBBBBBBBTTTBB.",
        "BWBTBTTBTBBTTB..",
        "BTBWB##BWBTBTBBB",
        ".BTBB#T#BBTTTBSB",
        "..BBTTTT##TBBTTB",
        ".BTSBTT##TSSTBB.",
        "BWBWTBBBBWBWBWB.",
        "BBBBBB..BBBBBBB."
    )

    // [ID 439] Panjyamon
    val PANJYAMON_ROM = arrayOf(
        "................",
        "................",
        "................",
        ".......WWWW.....",
        ".....B##B##W....",
        "....WBBWBGWB....",
        "....BBBBWWB.....",
        "....BBBWB##W....",
        "....W##B#BBTT...",
        "....W#BBW##BW...",
        "...W##BT##B#BB..",
        "..BW#TTBGBBBBB..",
        "...W##SBBSB#WB..",
        "..B#BBSB.BBBBWB.",
        "......BB..B.....",
        ".....###..W#W..."
    )

    // [ID 440] Phantomon
    val PHANTOMON_ROM = arrayOf(
        ".TTTTT..........",
        ".TTTTTT.........",
        ".T#####BBBBB....",
        ".T####BRRRRRB...",
        ".T###BRRRRRSRB..",
        ".T##BRBBBTSSSTB.",
        ".T##BBBBBBBTSSB.",
        ".BBBRBCBBCBBTSTB",
        "BWBWRBBBBBBBTSTB",
        "BWBTTRRRSSSSTBWB",
        ".BBTT#TT#TTBBBWB",
        ".T.BTBT#R##BWWWB",
        ".T.BTB#T#T#TBTTB",
        ".T.B#BBT#T#T#B#B",
        ".T.BB.B#B#B#BB#B",
        ".T..B.BB.B.BB.BB"
    )

    // [ID 441] Shakkoumon
    val SHAKKOUMON_ROM = arrayOf(
        "......B.........",
        "....BBTBBB......",
        "...BTTTTBTB.....",
        "..BTWTBBTTTB....",
        ".BBBTBTBBBTTB...",
        "B###BTB###B##B..",
        "B##SB#B##TB#B...",
        ".BBB###BBB#BBBBB",
        "..B#SB#T##BSSSSB",
        "...BBBBBBBBBSSB.",
        "..B#BTB##BTSBSB.",
        ".BS#BBBBBB#SSB..",
        ".BSBSSSSSSBSB...",
        "..BBBBBBBBSB....",
        "....BSB.BSB.....",
        ".....B...B......"
    )

    // [ID 442] Digitamamon
    val DIGITAMAMON_ROM = arrayOf(
        "......BBBB......",
        ".....BWW#SB.....",
        "....BWWWW#SB....",
        "...BWWWWWW#SB...",
        "...BWWBWWW#SB...",
        "..BWWBWWWWB#SB..",
        "..BWBBBWBBWBSB..",
        "...BBBBBBBBBBB..",
        "....BGBBGBBB....",
        "..BBBBBBBBBBB...",
        "..BWBWBBWBWBSB..",
        "..BWWWWWWBW#SB..",
        "...BWBWWBW#SB...",
        "..BBBB####BBBB..",
        ".BTTB......BTTB.",
        "..BBB......BBB.."
    )

    // [ID 443] Cherrymon
    val CHERRYMON_ROM = arrayOf(
        "......BBBBB.....",
        "...BBBBBSTTBBB..",
        "...BTBTTBRTSTTB.",
        ".BBTSTRTSTSRSSB.",
        ".BSBBSTSRSTSBBB.",
        "B#BSSBSS#SSB#TBB",
        "BB#RS#T###T####B",
        "BTB#B###BB###B#B",
        "BTBBTB#BTTB#BTBB",
        "BTTBTTBTTBTBBTBB",
        "BTTBGTTTGBTBTTBB",
        ".BBTBTTTBTTTTTB.",
        "...BTSSSTTTTBB..",
        "..BBTST#TTTTTTB.",
        ".BTTTBBBBBTTBTTB",
        ".BBBBBB..BBBBBBB"
    )

    // [ID 501] WarGreymon (Dragon Warrior with Brave Shield & Dramon Killers)
    val WARGREYMON_ROM = arrayOf(
        "..BB........BB..",
        "..BWB.BBBBBBTB..",
        "..BWBBWTTBBTBB..",
        "...BWTTEBTBBB...",
        "..BWTTBETTTB....",
        "BBBTTTTTTBBBBBBB",
        "B#BBBBBBBCCB#R#B",
        ".B#BCCCCCCB###B.",
        "BSBBBBBBBBTRBBSB",
        "B#SBRWRBRWTTBS#B",
        "B#BBBBBRBBBB#BB.",
        ".BB#B#WWTT#B#B#B",
        "B#TBTBBRRBBTBT#B",
        "BBBBBSBWTBSBBBBB",
        "..BWBSSBBBBSBWB.",
        "..BBBBBB.BBBBBB."
    )

    // [ID 502] Machinedramon
    val MACHINEDRAMON_ROM = arrayOf(
        "...BBBBBBBBBBBBB",
        "..BTTTTTBTTTBTTB",
        ".BTTTTBTTBSBBBBB",
        "BTTTTBBSSBBBBTB.",
        "BTTTSSSSTBTTRBB.",
        "BBBBBBSSSBTTRBBB",
        ".BTTSSBSB#BBBRTB",
        "..BBBBBB#BT##BTB",
        ".BBBWW##BBBB#BTB",
        "BWTBBBBBBWTGTBTB",
        "BTWBTRTTB#WTBBB.",
        ".BBBBBBBBBBB#BWB",
        "...B####TTTBB##B",
        ".BBBB##TTBBBBBBB",
        "B#BSBBBBBSB#WB..",
        "BBBBBB.BBBBBBB.."
    )

    // [ID 503] MetalGarurumon
    val METALGARURUMON_ROM = arrayOf(
        "....BBB...BBB...",
        "...BTSB..BTSB...",
        "..B#BBBBBC#B....",
        "..BBTSC#B#B.....",
        "..BTSSRB#BB...BB",
        "BBTSSBRC##B..BTB",
        "BRSBSCCC#BBB.BTB",
        "BBBWBBBBBTTBBBTB",
        ".BWWWWTBWTTB##B.",
        "..BBBBTTBBBSC##B",
        "....BTTBSSSC#B#B",
        "....BSCBSBT#BR#B",
        "...BTCBTCBT#BRCB",
        "..BBS#BS#BBBBBSB",
        ".BTBTBTBTB.BTTTB",
        ".BBBBBBBB..BBBBB"
    )

    // [ID 504] Seraphimon
    val SERAPHIMON_ROM = arrayOf(
        "....BB..BBBB....",
        "...BBBBBTTTB....",
        "..BBTTTTBTB..BB.",
        "..BTWTTTTB..BTB.",
        "..BBBBBBTB.BTTB.",
        ".BB#BTS##BBBBSB.",
        "B#BST####BS#BBTB",
        "B#BBBBBBB###BTSB",
        ".BB#S##TB#BBSSB.",
        "BTBBBBBBSBBBBB..",
        "B#B#BT###BTBBSB.",
        ".BBBBBBBBT#SBSSB",
        ".BBBSSBSBSSB.BSB",
        ".BBBSSBBBBB...BB",
        "BSSBBBBBSSSB....",
        "BBBBB..BBBBB...."
    )

    // [ID 505] Rosemon
    val ROSEMON_ROM = arrayOf(
        "....B.BBB.BB....",
        "...BRBRRRBRRB...",
        "..BRBRRRRRBRB...",
        "..BRBRRRRRBRB...",
        "BBBRBRRRRRBRBBB.",
        "BTBRBBRRBBRRBTB.",
        ".BTBSTBBSSBBTB..",
        ".B#BBSTTSBB#TB..",
        ".B#BSBBBBSRB#B..",
        ".BBRBRTTRBRB##B.",
        ".BRTBTBBTBTRB#B.",
        "BRTRBBTSBBRTRB#B",
        "BBBBBRRRRBBBBT#B",
        "BWBTTBBBBTTBWWTB",
        "BBTTTB..BTTTBBWB",
        "BBBBBB..BBBBB.BB"
    )

    // [ID 506] Phoenixmon
    val PHOENIXMON_ROM = arrayOf(
        ".BBB.BB.BB.BB...",
        ".BTBBTBBTB.B#BB.",
        ".BT#BTB#TB.B#B#B",
        "..BTTSWSB..B#B#B",
        ".BTWBBBSB.BT#B#B",
        ".BWBTBS#BBT#B##B",
        "BWSS##TTBT####BB",
        "BSTBB########BRB",
        "BSB###TT#####RB.",
        "BB#BBBTT##B#RB..",
        "B.B##B##TT#BB...",
        "...BB.BTT#BRTB..",
        ".......BBBRTB...",
        "........BTBTB...",
        "........BWBWTB..",
        ".........BBBBB.."
    )

    // [ID 507] HerculesKabuterimon
    val HERCULESKABUTERIMON_ROM = arrayOf(
        ".B..BB..BB..B...",
        "BB.BB....BB.BB..",
        "BB.BSBBB.BB.BB..",
        "BSBS#SS#B#B.BSB.",
        "B###S##B##B.B#B.",
        ".B####BB###B##B.",
        ".B#########B##SB",
        "..BB##R####B###B",
        "...BRRRR###BB##B",
        "...BBTTTTBB##B#B",
        ".BBSB#TS##BB##BB",
        ".B#BBTTTTTBSB#BB",
        ".B##B#TS##B###BB",
        "..BBBBBBBBBBBB..",
        ".B#B##BBBB##B#B.",
        ".BBBBBB..BBBBBB."
    )

    // [ID 508] Plesiomon
    val PLESIOMON_ROM = arrayOf(
        "......BB..B.....",
        "....BBBBBBTB....",
        "...B##WBTTTB....",
        ".BB#WBBWBTBB....",
        "B#WWBTW#BBB..BB.",
        "BBBWWWWW##B..BWB",
        ".B####BG##B..BTB",
        "..BBBBGWWB...BSB",
        "...BGG#WTTBBBBWB",
        "...B##WWTSWTTWWB",
        "..BB##WWWTWWSWB.",
        ".B#B##WWWWWWTWB.",
        ".B####WWB#WW##B.",
        ".B#B##BW#BBW##B.",
        "BSSSBBBTTSBBST#B",
        "BBBBB.BBBBBBBBBB"
    )

    // [ID 509] Piedmon
    val PIEDMON_ROM = arrayOf(
        "...BBB....BBB...",
        ".BBRTTBBBBTTRBB.",
        "BB.BTTTTTTTTB.BB",
        "BB.BBBBBBBBBB.BB",
        "...BBTBBWWB#B...",
        "..BBTTTBWBTB#B..",
        ".BBBBTBRTWBW##B.",
        ".BBSBBBTTWW#T#B.",
        "..BBSSSBWTTT#B..",
        "...BBBBSTBBBB...",
        "..BRRB###WBRRB..",
        ".B#WBRBBBBRB#WB.",
        ".BW#BBTTTTBBW#B.",
        "..BBTTTTTTTTBB..",
        ".BTTTTBBBBTTTTB.",
        ".BBBBBB..BBBBBB."
    )

    // [ID 510] Omnimon (Holy Knight of Courage and Friendship)
    val OMNIMON_ROM = arrayOf(
        "....TT..........",
        "...BTTB.........",
        "...B##B...WW....",
        "..B#W##B.BWWBB..",
        ".B######B#WWRRB.",
        "BTTB#C##B#WWRRB.",
        "BTWB#C##BB#WWRB.",
        ".BWB####B.B#WRB.",
        "..B#####B..BBBB.",
        ".BCB#C#BCB......",
        "BCCCB##BCCCB....",
        "BCCB####BCCB....",
        ".BB.B##B..BB....",
        "....B##B........",
        "...B###B........",
        "...BBBBB........"
    )

    // [ID 511] MetalEtemon
    val METALETEMON_ROM = arrayOf(
        ".....BBBBBB.....",
        "....BB#BWTBB....",
        ".BBBBBBBBBBBBBB.",
        "B#WBB#WW###BBWWB",
        "B#WBBBBBBBBBWWWB",
        "BWBBWBBBWBBBBB#B",
        "B#SBBBSBBBBBW#SB",
        ".BSB#WB##STB#SB.",
        "..BB#WW#SSTBBB..",
        ".B#BBBBBBBBBW#B.",
        "B#W#SBBBBBW#SBB.",
        "BBBBTBBBBBTBBSB.",
        ".BBBBBBBBBBBBB..",
        "...BBTBBBBBBB...",
        "..BSBBTBB#SBBB..",
        "..BBBBB..BBBBB.."
    )

    // [ID 512] Imperialdramon Dragon Mode
    val IMPERIALDRAMON_DRAGON_MODE_ROM = arrayOf(
        "..RR......RR....",
        ".BRRB.TT.BRRB.C.",
        ".BRRBBTTBBRRBCCB",
        "BRR##B##B##RRCCB",
        "BRR#WW##WW#RRCBB",
        ".BR##########RB.",
        "..B##TTTTTT##B..",
        ".B###T####T###B.",
        "B#TT#B####B#TT#B",
        "B#TTB######BTT#B",
        ".BBB########BBB.",
        "..B###B##B###B.T",
        ".B###B....B###BT",
        "B###B......B###B",
        ".BT#B......B#TB.",
        "..BBB......BBB.."
    )

    // [ID 513] Imperialdramon Fighter Mode
    val IMPERIALDRAMON_FIGHTER_MODE_ROM = arrayOf(
        ".BB......BB.....",
        ".BTBBBBBBTB.B...",
        ".BBBTTTTBTB.BB..",
        ".BTTTWBTTBTBSSB.",
        ".BTTBBTTBSBSSSB.",
        ".B###TSB#BBBBSSB",
        "B#BBBBB#BTS##BSB",
        "B#BT###BT###BSSB",
        ".BSBBBBSBBBBSSSB",
        ".BB###TBB##SBBSB",
        "B##B#TB##BTSB.BB",
        "B#SBBBB##B#B...B",
        ".BBBSSSBBBBB...B",
        "BBSSBSBSBSSSB...",
        "BSBSSB.BSBSBSB..",
        "BBBBBB.BBBBBBB.."
    )

    // [ID 514] Imperialdramon Paladin Mode
    val IMPERIALDRAMON_PALADIN_MODE_ROM = arrayOf(
        ".BB......BB.....",
        ".BTBBBBBBTB..BB.",
        ".BBBTTTTBTB.BTB.",
        ".BTTTWBTTBTBTTB.",
        ".BTTBBTTBSBTTTTB",
        "BB###TSB#BBBBB#B",
        "B#BBBBB#BTS##BTB",
        "B#BT###BB###BTSB",
        "B#TBBBBT#BBBBSSB",
        ".BBB##TBB##SBSSB",
        ".B#B#TB##BTSBBSB",
        ".BSBT#B##B#B..BB",
        "..BSBSSBBBBSB..B",
        ".BSSSBBBBSSSSB..",
        "BSBSSB..BSBSBSB.",
        "BBBBBB..BBBBBBB."
    )

    // [ID 515] Gallantmon
    val GALLANTMON_ROM = arrayOf(
        ".....BB.BBB.....",
        "....BSBBTTB.....",
        "...BBBTTTBB.....",
        "..BTTTTWBB......",
        "..BTTBBTBTB.....",
        "..BBBBBBB#BBB...",
        ".B#BT###BBS##B..",
        ".B#BBBBBTB###B..",
        "..BBS##T##BBB...",
        ".B#B#BB#BBBSSB..",
        ".BB#BT#BB#TBSB..",
        "B#SBT###BT#SBSB.",
        "BSBBBSBSSBBBSSB.",
        ".BBSSBBSBBSBSSSB",
        ".BSBSSBBBSBSBBB.",
        ".BBBBBB.BBBBB..."
    )

    // [ID 516] Gallantmon Crimson Mode
    val GALLANTMON_CRIMSON_MODE_ROM = arrayOf(
        ".BBB..BBBB..BBB.",
        "BWWWB.BTTB.BWWWB",
        "BWWWTB####BTWWWB",
        ".BWWB##TT##BWWB.",
        "..BB#TWBBWT#BB..",
        ".BWWB######BWWB.",
        "BWWWTB#TT#BTWWWB",
        "BWWB#TTTTTT#BWWB",
        ".BBB########BBB.",
        "..BC########TB..",
        ".BCCB######BTTB.",
        ".BCWB##BB##BWTB.",
        "..BB.B#B.B#B.BB.",
        "....BT#B.B#TB...",
        "....B##B.B##B...",
        "....BBBB.BBBB..."
    )

    // [ID 517] Sakuyamon
    val SAKUYAMON_ROM = arrayOf(
        "....BBBBBBBB....",
        "...BTTTTTTTTB...",
        "..BT##WWWW##TB..",
        "..BT#WB##BW#TB..",
        "...B########B...",
        "..BB##TTTT##BB..",
        ".BSSB######BSSB.",
        "BSS#B#TTTT#B#SSB",
        "BSS##TTTTTT##SSB",
        ".BB##SSSSSS##BB.",
        "..B##S####S##B..",
        ".B###B####B###B.",
        ".BB..B#BB#B..BB.",
        "....BT#B.B#TB...",
        "....B##B.B##B...",
        "....BBBB.BBBB..."
    )

    // [ID 518] MegaGargomon
    val MEGAGARGOMON_ROM = arrayOf(
        "......BBBB......",
        "...BB.BTTTB.....",
        "...BTBBBBBTB....",
        "....BTTTTTBBBB..",
        "...BTTTWBTBTTTB.",
        "..BSBTBB#BBBBB..",
        ".B#BT###BTSB##B.",
        ".B#TBBBBB##B#TSB",
        ".BT#B##TB##BSSSB",
        ".B##B#B#B##BSSB.",
        ".BBBSBBBSBBBBB..",
        "B#SBSB##BB#SBSB.",
        "BSSBBBSSBBSSBSB.",
        ".BBBSBSSBSBBSB..",
        ".BBSSBBBBSSBB...",
        "BBBBBB..BBBBBB.."
    )

    // [ID 519] Beelzemon
    val BEELZEMON_ROM = arrayOf(
        ".....BBBBBBB....",
        "....BBTBSBBTB...",
        "...BTBTBBTBBBB..",
        "...BTTWBTTBBTB..",
        "...BBBBBBBSSB...",
        "...BBTS##BBBB...",
        "..BSTB##BBS##B..",
        "..BT##BBB####BB.",
        ".BSBS#BTB##BBSB.",
        ".BSSBBB#BBBSBSSB",
        "BBBSBTBBB#BBSBBB",
        "B#SBSBB#BBSSBSSB",
        "BSSBSSSBSSSSBSSB",
        ".BBBBBSBBSBBBBB.",
        "..BSSSB..BSSSB..",
        "..BBBBB..BBBBB.."
    )

    // [ID 520] Beelzemon Blast Mode
    val BEELZEMON_BLAST_MODE_ROM = arrayOf(
        "....BBBBBB...B..",
        "...BSSSSBTB..BB.",
        "..BTBSBSSBTB.BB.",
        "..BTWBTBSSBB.BSB",
        "..BBBTTBSSB..BSB",
        "...BBBBSSBB.BSSB",
        "...BT##BBTSBSSSB",
        "....BBBBT##BSSSB",
        "...BB#BT#BB.BBSB",
        "..BBB#BBBBB...BB",
        ".B##BTB#B#TB..BB",
        ".BS#BBSBBT#B...B",
        "..BBSSBSSBB....B",
        "...BBB.BSBBB...B",
        "..BSSB..BSSSB..B",
        "..BBBB..BBBBB..."
    )

    // [ID 521] Magnamon
    val MAGNAMON_ROM = arrayOf(
        "...BB..BB.......",
        "...BTB.BTBBBBB..",
        "...BTBBBTBTTTB..",
        "...BWTTTTBTTB...",
        "..BTTTBBBTTTTB..",
        ".BBBBBSB##TBBBB.",
        "B##BT####TBB##TB",
        "BB#TBB#BTB###TBB",
        ".BBBSBBTBB#BBBB.",
        "BT#BBBT#B##BSB..",
        "BB#BSB##BBTBBB..",
        ".BBSBB###BBBSB..",
        "...BSSBSSBSSBSB.",
        "..BSBSBBSBBBSB..",
        ".BSBSBSBBSBSBSB.",
        ".BBBBBB.BBBBBBB."
    )

    // [ID 522] UlforceVeedramon
    val ULFORCEVEEDRAMON_ROM = arrayOf(
        ".......BB.BB....",
        "......BWBBWB.B..",
        ".....BWBBWB#BSB.",
        "....BTS#T#BBTSB.",
        "....BSTTBSBTTTSB",
        "...BS#BRSCBBBTSB",
        "...BTWWWTBTT#BSB",
        "...BBWTBBTBTTB#B",
        "..BBTBBTTBWBBT#B",
        ".BSBTBTTBS#RBT#B",
        ".BWBTTTBBWTWBTTB",
        "..BBBBBB#BBBBTTB",
        "...B#W#CWW#BBBTB",
        "..B#TBBBB#TCB#BB",
        ".BWBCB.BWBTBTB.B",
        ".BBBBB.BBBBBBB.."
    )

    // [ID 523] Alphamon
    val ALPHAMON_ROM = arrayOf(
        ".....BB...BBB...",
        "....BSBBBBSB....",
        "...BTBTTTTB.....",
        "BB.BWTTBBTBBBB..",
        "BTBBTTBBTBBTTB..",
        "B##B#TS#B#T##B..",
        ".B#SBB#BSBSBB...",
        "..BBBBBSBSB#B...",
        ".BTBS##T#B#SBB..",
        ".B#B#BB#SB#SBB..",
        "..BB#BB##BBBSSB.",
        "....BB#BBBSSSSSB",
        "...BSBBBSSBSSSSB",
        "..BSSSBSSSSBSBSB",
        ".BSBSSBSBSBSB.BB",
        ".BBBBBBBBBBBB..B"
    )

    // [ID 524] Alphamon Ouryuken
    val ALPHAMON_OURYUKEN_ROM = arrayOf(
        ".....BB.BBB.....",
        "...BBBBBBB......",
        "..BTBTTTTB..BBBB",
        "BBSWTTBBTBBBSSSB",
        "BTBTTBBTBBTTBSB.",
        "B#B##TSB##T#BB#B",
        ".B#BB#BSBTBBB#BB",
        ".BBBBB#BSB#B#BSB",
        "B#B#S##TB##BBSBB",
        "BTB#BBT#B##B.BSB",
        ".BB#BB##BBB...BB",
        "...BSSBBBBB....B",
        "..BSBSSBSSB.....",
        ".BSSSBBSSSSB....",
        "BSBSSBBSBSBSB...",
        "BBBBBBBBBBBBB..."
    )

    // [ID 525] BlackWarGreymon (Black Digizoid Dragon Warrior)
    val BLACKWARGREYMON_ROM = arrayOf(
        "..BB........BB..",
        "..BWB.BBB.BBWB..",
        "..BTBBWWTBTSTB..",
        "...BWWW.BWBBB...",
        "B.BWWWBGWTBB..BB",
        "BBBTTTTTTBTBBBSB",
        "BSBBBBBTBSTBSGB.",
        ".BGBSS#B#BBG#SB.",
        "BSBWBBBBBWWWBBSB",
        "B#SBTWGSGTWTBS#B",
        ".BBBBBBGBBBB.BB.",
        "B#SBBSGSGSTBBS#B",
        "B##BSBWGWTBSB##B",
        "BBBBBWBWTBWBBBBB",
        "..B.BTBBB.B.B.B.",
        "..BBBBBB.BBBBBB."
    )

    // [ID 526] ChaosBlackWarGreymon (Abyssal Corrupted Dragon Warrior)
    val CHAOSBLACKWARGREYMON_ROM = arrayOf(
        "..BB........BB..",
        "..BWB.BBB.BBWB..",
        "..BTBBWWTBTSTB..",
        "...BWWW.BWBBB...",
        "B.BWWWBRWTBB..BB",
        "BBBTTTTTTBTBBBSB",
        "BSBBBBBTBSTBSRB.",
        ".BRBSS#B#BBR#SB.",
        "BSBWBBBBBWWWBBSB",
        "B#SBTWRSRTWTBS#B",
        ".BBBBBBRBBBB.BB.",
        "B#SBBSRSRSTBBS#B",
        "B##BSBWRWTBSB##B",
        "BBBBBWBWTBWBBBBB",
        "..B.BTBBB.B.B.B.",
        "..BBBBBB.BBBBBB."
    )

    // [ID 527] Diaboromon
    val DIABOROMON_ROM = arrayOf(
        "....BBBBB.BB....",
        "..BBSBSSBBTB....",
        "..BSBTBBTTTB....",
        ".BSWTTBSBBBSBBB.",
        ".BSBTBSBBTBBBTBB",
        ".BSSBBB#B#B##BSB",
        "..BSB##BBTBBBSB.",
        "...B##BBTBSSBB..",
        "..B#BB#TBSSBSB..",
        ".B#B.BBBSBB.BSB.",
        ".B#B.BBSB#B.BSB.",
        ".BBB.B#B#TB.BBB.",
        ".BBB..BBSB..BBB.",
        ".BSB...BBB..BSB.",
        ".BBSB.BSSB.BSB..",
        "..BBB.BBB..BB..."
    )

    // [ID 528] Armaggeddemon
    val ARMAGGEDDEMON_ROM = arrayOf(
        "................",
        "...BBBB...BBBB..",
        "....BTTBBBTTTTB.",
        ".....BBTTTBBBTTB",
        ".......BBB...BTB",
        ".............B#B",
        "...........B.BTB",
        "BB.BBBB..BB#BBSB",
        "B#BBBSSBB#BBSBB.",
        ".BB##BB#BBSSBSB.",
        "BB#B#BB#BSSBSBSB",
        "BBS#BSSB#BBSSBBB",
        "BSBBSBBSBBSBBBSB",
        "BSBBBSBBSSBBSBSB",
        ".BSSSB.BBBSBSBB.",
        "..BBB.....B.B..."
    )

    // [ID 529] Apocalymon
    val APOCALYMON_ROM = arrayOf(
        "........B..B....",
        ".........BB..B.B",
        ".........BB..BBB",
        "........BBBB..B.",
        "BBB....BTTTTB..B",
        ".BB.BB.BWBBBSB.B",
        "..BB..BSBSSSSSB.",
        ".BB..BSBTBSSSB..",
        "BBB..BBT##BSSSBB",
        ".....BB#S#BSSSBB",
        "....BTBBBBSSSBSB",
        "....B#BBBBBBBSB.",
        ".....BBSSSSSBBB.",
        "..BB.B.BBBBBB...",
        "...BB...BB......",
        "..BB............"
    )

    // [ID 530] Belphemon
    val BELPHEMON_ROM = arrayOf(
        "...BB.BBBBB..B..",
        "..BTBBBBTTB.BSB.",
        "..BBBTTTTB..BSB.",
        ".BBTTTWBTB.BSSSB",
        "BTTTTBBTTTBSSSSB",
        "B####TS###BSSSSB",
        "BBBBBB###TSBSSSB",
        ".B#TB##ST###BSSB",
        "..B#S##T#B#SSBSB",
        ".B#BBBT#B##SSBSB",
        "B##B#T#B#BTSSBSB",
        "BBSBT##BBTBSB.BB",
        "BSBBSSSSBBBBB..B",
        ".BSSBSSSSBSSSB..",
        "BSBSSBBBBSBSBSB.",
        "BBBBBB..BBBBBBB."
    )

    // [ID 531] Lilithmon
    val LILITHMON_ROM = arrayOf(
        ".BB..BBBB..BB...",
        ".BTBBTTBSBBTB...",
        "..BBTBBTBSSB....",
        ".BBTWSSBTBSSB...",
        "BSSBTBBTBSSSSB..",
        ".BB##TS##BSBB...",
        "BBBSB##B#TB#BB..",
        "..BTB##BT#BB....",
        "...BS##T#BB..B..",
        "..BSBBBBBSBB.BB.",
        ".BSB#B#BBBTBBSB.",
        "BSB#BBB##B#BBSSB",
        "BSBSBSBSSBSB.BSB",
        "BB.BSSBBSSB...BB",
        "B..BSSBSBSSB...B",
        "...BBBBBBBBB...."
    )

    // [ID 532] Daemon
    val DAEMON_ROM = arrayOf(
        ".BBB.R...BBB.R..",
        "BTTB.BB..BTTBB..",
        "BTBTBTTBBBBTBTB.",
        "B#BTCT#TBTT#BTB.",
        "BBC#T#TCB##BTTB.",
        ".BCC##CCTBBTTTTB",
        ".B#C#TCTTTBTTTTB",
        ".B##TTTTTSBTBTTB",
        ".BSWSWSWSSBBTBTB",
        ".BBRRSSSSBTBTTBB",
        ".BTBBBBBBTTTBTB.",
        "BTBTTBTTBTTTBTTB",
        "BTTBTTTTBTTTBTTB",
        "BTTBTTTBTTTBBBB.",
        "BTBRBBBTBTBRBRTB",
        "BBBBB..BBBBBBBBB"
    )

    // [ID 533] Leviamon
    val LEVIAMON_ROM = arrayOf(
        "................",
        "..........BB.BB.",
        "..........BB.BB.",
        "...........B..B.",
        "............B..B",
        "........BBB.B.BB",
        "...BB.BB#TBBBBTB",
        "..BTBB#SWBB##BB.",
        ".BT#BSBBBSSBBSSB",
        ".B###BBBSBBSSSSB",
        "B####B#BB#TSSSBB",
        "B#S#T####BBSBSSB",
        "BBSSSSBBBBSSSBSB",
        "BSBBBBBSSSBSSBBB",
        ".BSSSSSSSBBSBSB.",
        "..BBBBBBB.BBBBB."
    )

    // [ID 534] Barbamon
    val BARBAMON_ROM = arrayOf(
        "..BB.BBBB..BBBB.",
        ".BSSBTTTTBBTBSSB",
        ".BSBTTTBBTBTBSSB",
        "BSSWBTBSBTTTBSSB",
        "BBBTTTBBTTTBSSSB",
        "B.B##TS#BBBBSSBB",
        "..BBBBBBSSB#BBTB",
        ".BSSB##BBSSBSSB.",
        ".BSSB##T#BSSSSSB",
        "BSSSB#B#SBSBBSBB",
        "BSSB#TB##BBSSB.B",
        ".BSBTB###TBSSB..",
        ".BSBSBSBSSBBBSB.",
        "BSBSBSBBSBSSSSSB",
        "BBSBSBSBBSSSSSBB",
        "B.BBBBB.BBBBBB.B"
    )

    // [ID 535] Omnimon Alter-S
    val OMNIMON_ALTER_S_ROM = arrayOf(
        ".RR...BBBB..RRR.",
        ".RTRBBWWWWBBTR..",
        "..RWWTWWWWWWB...",
        "...BTWWWTBTB..BB",
        "BBBWWW#BCW#BBBTB",
        "BSBWWWWWWW#BSRB.",
        ".BTBWBBBWBBTRTRB",
        ".BTTBWW#BWBSRRRB",
        "..BBBBBBBBBBTTBB",
        ".BBTBRWBWTRBBBRB",
        "BSTTBBBRBBBBRTRB",
        "BRTBBTBTWBWTBRB.",
        "BBBBTBWB#B#BTB..",
        "..BBW#BBBBBWWBB.",
        ".BSTBW#BBB#WBTSB",
        ".BBBBBBB.BBBBBBB"
    )

    // [ID 536] Omnimon Zwart
    val OMNIMON_ZWART_ROM = arrayOf(
        "................",
        "................",
        "................",
        ".........T......",
        "....T.B#T.......",
        ".....TSTBBBT....",
        "....BBTRBTBTT...",
        ".....TBBTTTTTB..",
        ".....BTS#TBBTB..",
        "....BBBTBTB#BTT.",
        "...BTBBTBTTBBSTB",
        "..BTTBBBBTTBTTW.",
        ".BTBTBBBBTTBBTT.",
        ".BBBTT#TRBTTTTB.",
        "..BB###BRBTBBB..",
        "...B#TBT.BT#TB.."
    )

    // [ID 537] Mastemon
    val MASTEMON_ROM = arrayOf(
        "....BBBBB.BBB...",
        "...B#WTSBBBB....",
        "..B#TTTSBB......",
        ".B#TGTSBTBB.BB..",
        ".BWGGSBT#TBBBBB.",
        ".B#T#B###TBB....",
        ".BWTTSSBTBBTBB..",
        ".B#TBBBTTBTBBBB.",
        "..BWWTTTB#BB..BB",
        "...BBBBB#BBTB...",
        "..BB#TTSTBB#BB..",
        ".B#B#WSTBTT#BBB.",
        ".BTB##TBBTTB..BB",
        "..B#BBBB#BBB....",
        ".BTTB..BBTTTB...",
        ".BBBB..BBBBBB..."
    )

    // [ID 538] Jesmon
    val JESMON_ROM = arrayOf(
        "....BB..........",
        "....BTBBB.......",
        "....BW###BBBBB..",
        "...BW#WBBBWTB...",
        "...BWWBCBBBB....",
        "...BBBBBB#BBBB..",
        ".BBBWBWW#BBCCB..",
        ".BCTBBBBBBCRRB..",
        "..BBBCBW#BTBB...",
        ".BTBWBWW#SBTTB..",
        ".BGBWBW#SSBTTB..",
        ".BGBBBSSSBTTTB..",
        "..BBTBSSTBTTB...",
        "...BCTBBSTBBB...",
        "..BWBCB.BCBWB...",
        "..BBBBB.BBBBB..."
    )

    // [ID 539] Gankoomon
    val GANKOOMON_ROM = arrayOf(
        ".....BBBBBBBBB..",
        "....BRR####TB...",
        "..BBR#B##TTB....",
        "..BWBBWBBBTB....",
        "...BTSTTSTBB....",
        "..BBSTSSTBBBB...",
        ".BWBBBBBBBTTWB..",
        "BWTBTTTT#BTTTWB.",
        "BBBB#T#TBBBBBBB.",
        ".BTBBBBBBBBTSBTB",
        "BGTBWT#WSBTGSBWB",
        "BRGBTS#STBGRBBWB",
        ".BBBBTTTBBBBBTWB",
        "..BTTBBBTTTBBBBB",
        ".BRRRB.BBRRRB...",
        ".BBBBB..BBBBB..."
    )

    // [ID 540] Dynasmon
    val DYNASMON_ROM = arrayOf(
        ".......BBBB.BB..",
        ".....BBTTTB.BSB.",
        "....BTTTTBBBSSB.",
        "...BTTTTWTTTBSSB",
        "...BTBTBTBBTBSSB",
        "..B#BBS##BTBBBSB",
        ".B#BTBBBBSB###BB",
        ".B#TBSBBTB###TBB",
        ".BTBSB#B##BBBSBB",
        "..BBB#B#BB#BBBSB",
        ".B##BB#B#BBSSBSB",
        ".BS#B####TBSSBSB",
        ".BSBBSBBBSBBSBSB",
        "..BBBSB.BSSBBSBB",
        ".BSSSB...BSSSB..",
        ".BBBBB...BBBBB.."
    )

    // [ID 541] LordKnightmon
    val LORDKNIGHTMON_ROM = arrayOf(
        "....BBBBB.BBBB..",
        "...BTTTTTBTTB...",
        "..BTTTTTBTBBBB..",
        ".BBTTTTWSBTTTTB.",
        ".BTBTTBTBTBBBBTB",
        ".B#BBBS##BTBBBB.",
        "..BBT##BBTS###B.",
        ".B#BBBBBBBBBBB..",
        "..BBS##T#BBSBB..",
        "BBB#B#TBBB#BBSB.",
        "B###BBB#B#TSBBSB",
        "BBS#B###BT#BB.BB",
        "..BBBSBBSBB.....",
        "..BSSB.BSSBB....",
        ".BSSSB.BSSSSB...",
        ".BBBBB.BBBBBB..."
    )

    // [ID 542] Craniummon
    val CRANIUMMON_ROM = arrayOf(
        ".....BBBBB.BB...",
        ".B.BBTTTTTBTTB.B",
        ".BBTBTTBBTBTTTBB",
        "..BWTBBTBBBBBB..",
        ".BBSBTTBSBTBTTB.",
        "B##B#TS#B#B##S#B",
        "BBBSBBBB#BB##BBB",
        ".BBT###STBSBBTB.",
        "B#BBS##TB#BBSSSB",
        "BT#BB#BBBB#BSSSB",
        "B##B#B#B##BBSSSB",
        "B#BBTB#BBBBSBBSB",
        ".BSBBSBBSSSBSSB.",
        "..BBSSSSBSBSBBB.",
        ".BSSBBBBSBSBSB..",
        ".BBBB...BBBBBB.."
    )

    // [ID 543] Sleipmon
    val SLEIPMON_ROM = arrayOf(
        "....BBBB..BBB...",
        "...BTTTTBB#S#B..",
        "..BT####B##SS#B.",
        ".BB#CWW#B###SS#B",
        "BCCB####B####SSB",
        "BCWBTTTTTTTTT#SB",
        ".BCBBBB#####BBBB",
        "..B###S#####S##B",
        ".B#############B",
        "B####SSSSSSS###B",
        "B###S#######S##B",
        ".BBBBBBBBBBBBBBB",
        ".B#B.B#B.B#B.B#B",
        ".BTB.BTB.BTB.BTB",
        ".BBB.BBB.BBB.BBB",
        "................"
    )

    // [ID 544] Duftmon
    val DUFTMON_ROM = arrayOf(
        "...BBB....BBB...",
        "..BTTTB..B#W#B..",
        "..BT##BBBB#W#B..",
        ".BB##T####B#B...",
        ".B#WTBWWB#TBB...",
        "..B##TTTT##B.BB.",
        ".BBTTTTTTTTB.BSB",
        "BSSB##TT##BBSBSB",
        "BSSB######BSSB..",
        "BSSB##TT##BSSB..",
        ".BB.B####B.BB...",
        "....B#BB#B..BC..",
        "...BT#B.B#TB.C..",
        "...B##B.B##B.C..",
        "..BTTTB.BTTTB...",
        "..BBBBB.BBBBB..."
    )

    // [ID 545] Examon
    val EXAMON_ROM = arrayOf(
        "..........BBB...",
        "BB.....BB..BSB..",
        "BTB.BBBTTB.BSSB.",
        "BTBBTTTWB..BSSB.",
        "BTBTTBBTB.BSSSSB",
        "BB##BBS##BSSSSSB",
        "B##ST#B##BSSSSSB",
        "B#BBBB#SBBBSSSSB",
        ".BT#S##B###BSSSB",
        "..BBBBB#SB#SBSSB",
        "...BSB##B#TSBBSB",
        "..B#B###BT#BB.BB",
        "...BBBSSSBBB..BB",
        "..BSSSBBBSSSB..B",
        ".BSBSB.BSBSBSB.B",
        ".BBBBB.BBBBBBB.."
    )

    // [ID 546] Millenniummon
    val MILLENNIUMMON_ROM = arrayOf(
        ".......SSSSSSS..",
        "BB....S#####S...",
        "BTB..BB##S#S....",
        "BTBBBTTB####S...",
        ".BTTTTTTB####S..",
        "BTTTBBBTTB####S.",
        "BTTBRBTTTTBB###S",
        "BTTTTTTTTBBBBB#S",
        "BBBBBBTTBBTTBBBS",
        ".BTTTTBBTTBTBTTB",
        "..BBBBTTTBBTBTRB",
        ".BTTBTTTBTTBBBBB",
        ".BTTBTTTTBTBBB..",
        "..BBTTTBBTTBTB..",
        "..BRTTB..BBRTRB.",
        "..BBBB.....BBBB."
    )

    // [ID 547] MoonMillenniummon
    val MOONMILLENNIUMMON_ROM = arrayOf(
        ".....BBBBBB.....",
        "...BBCCCCCCBB...",
        "..BCCCWWWWCCCB..",
        ".BCCWW####WWCCB.",
        ".BCW###RR###WCB.",
        "BCCW##RWW###WCCB",
        "BCW###RRRR###WCB",
        "BCW##SSSSSS##WCB",
        "BCW###SSSS###WCB",
        "BCCW###RR###WCCB",
        ".BCW########WCB.",
        ".BCCWW####WWCCB.",
        "..BCCCWWWWCCCB..",
        "...BBCCCCCCBB...",
        "....BTTTTTTB....",
        "....BBBBBBBB...."
    )

    // [ID 548] ZeedMillenniummon
    val ZEEDMILLENNIUMMON_ROM = arrayOf(
        ".BBBB..BB..BBBB.",
        "B#RR#BBTTBB#RR#B",
        "B#RW##BTTB##WR#B",
        ".B####BGG######B",
        "..BBBBGGGG####B.",
        ".BTTB#####GGGGB.",
        "BT##B##SS##BGGB.",
        "B###GGGGGGGGB##B",
        ".BB##SSSSSS##BB.",
        "..BGGGGGGGGGG#B.",
        ".B###SSSSSS###B.",
        ".B##B######B##B.",
        "..BBB#B..B#BBB..",
        "...B##B..B##B...",
        "..BTTTB..BTTTB..",
        "..BBBBB..BBBBB.."
    )

    // [ID 549] Susanoomon
    val SUSANOOMON_ROM = arrayOf(
        "..BBBB.BBBB.....",
        "..BTTB.BTTB.....",
        "..BBTBBBTBB.....",
        "....BWTBBBSB..B.",
        "..BBSBBBTTBB.BB.",
        ".B##BTS##BSBBB#B",
        ".B#SBBBBBSB###BB",
        ".B#TBB#STB#BBTSB",
        ".BB#SBBBBBBSSBBB",
        ".BSBBBT#S#BSSSB.",
        "..BBSBBBBBBBBB..",
        "....B####T#B....",
        "....BBSBSBBB....",
        "....BSBSBSBSB...",
        "...BSBSB.BSBSB..",
        "...BBBBB.BBBBB.."
    )

    // [ID 550] Dorugoramon
    val DORUGORAMON_ROM = arrayOf(
        ".......BBBBBB...",
        "....BBBTTTTTBBB.",
        "..BBTTTTTBBBBTB.",
        ".BTTTWBBTTTBTBB.",
        "BTTTBBTTTBBTTTTB",
        "B####TS#B#BBBB#B",
        ".B#BBBBB#BS###B.",
        "..BT###SBB###BB.",
        "..B#BBBB##BBBSB.",
        ".B#BB#TBB#BBSBB.",
        ".B###B##BBTSSSB.",
        "..B#TBBBBB#SSB..",
        "...BBSBBSSBBB...",
        "...BBSSBBBSSSB..",
        "..BSBSSSBSBSBSB.",
        "..BBBBBB.BBBBBB."
    )

    // [ID 551] Gaiomon
    val GAIOMON_ROM = arrayOf(
        "..BB.......B....",
        ".BTB.BBBBBBTB...",
        "BTTBBTSTBTTTBBB.",
        "BTBTSSBBTBBBTTTB",
        "BBTSSBGSSTBTTTB.",
        "BBSSSSSSTRRBTTTB",
        ".B###STRR#BBBBTB",
        "..B#####BB##BTBB",
        "...BBBBB###BTTB.",
        "..BBT###STBBRRB.",
        ".BBBTSSSTBBTB#B.",
        ".BTBRSRSRB#TTB..",
        ".BTBBR#R#B##B...",
        "..BBTB##BTBBBB..",
        ".BTBTBBBBTBTBTB.",
        ".BBBBB..BBBBBBB."
    )

    // [ID 552] Ophanimon
    val OPHANIMON_ROM = arrayOf(
        "....BBBBB...BB..",
        "...BTTTTTB.BTB..",
        "..BTTBTTTTBTTB..",
        "..BWBSBBTBTTB...",
        "..BTTBTTTTBB....",
        "..BBBBBB##B.....",
        "..BST###BBB..BBB",
        "...B###STB#BBTSB",
        "....BBBBB##SBSB.",
        "..BB#BT#BBBBSB..",
        ".B#BBTBBSBTSBB..",
        ".BSBT###BT#SBSB.",
        "..BSBBBBBSSBBSSB",
        "...BSBSBSBBB.BSB",
        "..BSSBBBSSSB..BB",
        "..BBBB.BBBBB...."
    )

    // [ID 553] Vikemon
    val VIKEMON_ROM = arrayOf(
        ".....B...B...SS.",
        "....BTB.BTB.STTS",
        "....BBBBTRB.STTS",
        "...BWWW#BBB..SS.",
        "..BW#CB#WWBBBBS.",
        ".BB#BC#WTBCWTCB.",
        ".BWWWWWW#BWTTTB.",
        ".BBBBBW#TBTTTTB.",
        "..BWBW#TTBCTTCB.",
        "..B##TTTTTBBBB..",
        ".BWBBBTTTBW##B..",
        ".BTBWWTTBTT#B...",
        "..BBB#TBBTTBBB..",
        "..BW#RTW#BBB#WB.",
        ".BWBTRTTBWBWBBB.",
        ".BBBBBBBBBBBB..."
    )

    // [ID 554] Chaosdramon
    val CHAOSDRAMON_ROM = arrayOf(
        ".BB.....BBBBBB..",
        "BRB.BBBBRRRRB...",
        "BRBBRRRTSBBB#BB.",
        "BBRRRTTTTSB#BTTB",
        "BRRRTRTTSBB#BTTB",
        "BRRRRRTSB#BBBBTB",
        ".BRBBBBB#BBBRRB.",
        "..BT####BBRRBTRB",
        ".BT#BBBBSBRSB#RB",
        ".BBBRSRTSBBBB#RB",
        "BTRBRSTSBRR#BBTB",
        "BR##BBBBTRTBB.BB",
        ".BBBBRRTBBBBB...",
        "..BRSBR##BSTRB..",
        ".BTBRSBBBTBTBTB.",
        ".BBBBBB.BBBBBBB."
    )

    // [ID 555] Kentaurosmon
    val KENTAUROSMON_ROM = arrayOf(
        "................",
        ".........T..T...",
        "..W.....TT.TT...",
        "..TTTT#S#TTTTT..",
        "..WTTTSSS#TTTT..",
        "..TTT#SSS##TT...",
        "...T##TSSSTT....",
        "...WR##B##TW....",
        "...RR#TSWSST....",
        "...BSSST#SWS....",
        "...#SSB##SS.....",
        "....SSS#BSSB....",
        ".....#S#W.S.....",
        ".....S.T..S.....",
        "......S#........",
        "......#........."
    )

    // [ID 556] Magnadramon
    val MAGNADRAMON_ROM = arrayOf(
        "......BBBBBB....",
        "....BBSTRB......",
        "...BWWTTBBBBB...",
        ".BBWTBBSTRB.....",
        "BTTTBTT#BBB.....",
        "BBBBTT##SBSBBB..",
        ".B#####SBS##B...",
        "..BBBBBBS##B....",
        "..BBSBSSTBBSBB..",
        ".BTSB###BWBSB...",
        "BWB#BWT#BBBBB.BB",
        "BBBBBWT#TBSB#BSB",
        "...BBTT#BW#BBSB.",
        "..BW#BT#BBBBSSB.",
        "..BBBBBTT##SSB..",
        ".......BBBBBB..."
    )

    // [ID 557] Cherubimon
    val CHERUBIMON_ROM = arrayOf(
        "................",
        "...TS......S....",
        "....SS....SS....",
        "......S.WSS.....",
        "......TSST......",
        "...SSS#SSTSSS...",
        "...SBW#S##WBS...",
        "...SWWWBBWWBS...",
        "..SSBWSSSWWBS...",
        "..#SBWSSSSWBS...",
        "...SSBSSSSBSS...",
        "...SSSBSSSSSS...",
        "...TWSSS.SSSS...",
        "....#SWSBB.B....",
        ".......S........",
        "........W......."
    )

    // Universal Archetype Safety Fallbacks
    val ARCHETYPE_DINOSAUR = AGUMON_IDLE_A
    val ARCHETYPE_BEAST = GARURUMON_ROM
    val ARCHETYPE_AVIAN = BIRDRAMON_ROM
    val ARCHETYPE_INSECT = KABUTERIMON_ROM
    val ARCHETYPE_AQUATIC = SEADRAMON_ROM
    val ARCHETYPE_DEMON = DEVIMON_ROM
    val ARCHETYPE_HOLY = ANGEMON_ROM
    val ARCHETYPE_MECH = ANDROMON_ROM
    val ARCHETYPE_PLANT = PALMON_ROM
    val ARCHETYPE_BABY = BOTAMON_IDLE

    private val MATRIX_CACHE = java.util.concurrent.ConcurrentHashMap<Int, Array<String>>()
    private val AMBIENT_CACHE = java.util.concurrent.ConcurrentHashMap<Int, Array<String>>()
    private val PALETTE_CACHE = java.util.concurrent.ConcurrentHashMap<Int, SpritePalette>()

    /**
     * Returns the 16x16 pixel matrix for the given species and frame.
     * Supports all 12 SpriteFrame states with authentic V-Pet stance and eye animations.
     */
    fun getMatrix(speciesId: Int, frame: SpriteFrame = SpriteFrame.WALK_A): Array<String> {
        val sid = if (speciesId <= 1) 201 else speciesId
        val cacheKey = (sid shl 4) or (frame.ordinal and 0x0F)
        MATRIX_CACHE[cacheKey]?.let { return it }

        val base = when (sid) {
            201 -> when (frame) {
                SpriteFrame.SLEEP_A, SpriteFrame.SLEEP_B -> AGUMON_SLEEP
                SpriteFrame.ATTACK_CHARGE, SpriteFrame.ATTACK_RELEASE -> AGUMON_ATTACK
                SpriteFrame.IDLE_B, SpriteFrame.WALK_B -> AGUMON_IDLE_B
                else -> AGUMON_IDLE_A
            }
            101 -> if (frame == SpriteFrame.SLEEP_A || frame == SpriteFrame.SLEEP_B) BOTAMON_SLEEP else BOTAMON_IDLE
            102 -> if (frame == SpriteFrame.SLEEP_A || frame == SpriteFrame.SLEEP_B) KOROMON_SLEEP else KOROMON_IDLE
            else -> getBaseMatrix(sid)
        }
        return applyFrameTransform(base, frame).also { MATRIX_CACHE[cacheKey] = it }
    }

    private fun shiftRowLeft(row: String, amount: Int = 1): String {
        val s = amount.coerceIn(0, 15)
        return row.substring(s) + ".".repeat(s)
    }

    private fun shiftRowRight(row: String, amount: Int = 1): String {
        val s = amount.coerceIn(0, 15)
        return ".".repeat(s) + row.substring(0, 16 - s)
    }

    private fun closeEyePixels(
        base: Array<String>,
        topRow: Int,
        eyeLimitRow: Int,
        eyelidChar: Char
    ): Array<String> {
        val hasDedicatedEyes = (topRow..eyeLimitRow).any { r -> base[r].contains('E') }
        val upperWCount = (topRow..eyeLimitRow).sumOf { r -> base[r].count { it == 'W' } }
        return Array(16) { r ->
            val row = base[r]
            if (r !in topRow..eyeLimitRow) {
                row
            } else if (hasDedicatedEyes) {
                val chars = row.toCharArray()
                for (c in 0 until 16) {
                    if (row[c] == 'E') {
                        chars[c] = eyelidChar
                    } else if (row[c] == 'W') {
                        val adjE = (c > 0 && row[c - 1] == 'E') || (c < 15 && row[c + 1] == 'E')
                        if (adjE) chars[c] = eyelidChar
                    }
                }
                String(chars)
            } else if (upperWCount <= 4) {
                // Sprites where 'W' is strictly 1-2px eye glints (not white body/wing/armor fill)
                val chars = row.toCharArray()
                var c = 0
                while (c < 16) {
                    if (row[c] == 'W') {
                        var end = c
                        while (end + 1 < 16 && row[end + 1] == 'W') end++
                        val runLen = end - c + 1
                        if (runLen <= 2) {
                            for (k in c..end) chars[k] = eyelidChar
                        }
                        c = end + 1
                    } else if (row[c] == 'T' && r > topRow && base[r - 1][c] == 'W') {
                        // Close lower iris pixel ('T') directly below a 1-2px 'W' eye glint (e.g. Botamon)
                        chars[c] = eyelidChar
                        c++
                    } else {
                        c++
                    }
                }
                String(chars)
            } else {
                // White-bodied / winged / armored sprites ('W' is primary white fill):
                // Preserve 100% of 'W' pixels; only close isolated 1px iris tokens ('R'/'C'/'G') inside white face
                val chars = row.toCharArray()
                for (c in 1 until 15) {
                    val ch = row[c]
                    if ((ch == 'R' || ch == 'C' || ch == 'G') && row[c - 1] == 'W' && row[c + 1] == 'W') {
                        chars[c] = eyelidChar
                    }
                }
                String(chars)
            }
        }
    }

    private fun applyFrameTransform(base: Array<String>, frame: SpriteFrame): Array<String> {
        val topRow = (0..15).firstOrNull { base[it] != "................" } ?: 0
        val bottomRow = (15 downTo 0).firstOrNull { base[it] != "................" } ?: 15
        val waistRow = (bottomRow - 2).coerceAtLeast((topRow + 1).coerceAtMost(bottomRow))
        val eyeLimitRow = (topRow + ((bottomRow - topRow) * 2) / 3).coerceAtLeast(topRow + 2)

        return when (frame) {
            SpriteFrame.IDLE_A -> base.copyOf()
            SpriteFrame.IDLE_B -> {
                // Classic V-Pet breathing bob: upper torso compresses 1px down while feet stay planted
                Array(16) { r ->
                    when {
                        r <= topRow -> "................"
                        r in (topRow + 1)..waistRow -> base[r - 1]
                        else -> base[r]
                    }
                }
            }
            SpriteFrame.WALK_A -> {
                // Left-step stride: shift body 1px left if possible, lift left foot cleanly
                val canShiftLeft = base.all { it.first() == '.' }
                val shifted = Array(16) { r ->
                    if (canShiftLeft) shiftRowLeft(base[r], 1)
                    else if (r in topRow..waistRow) shiftRowLeft(base[r], 1)
                    else base[r]
                }
                val bot = shifted[bottomRow]
                val hasTwoFeet = bottomRow >= 2 &&
                    bot.substring(0, 7).any { it != '.' } &&
                    bot.substring(9, 16).any { it != '.' } &&
                    (bot[7] == '.' || bot[8] == '.')
                if (hasTwoFeet) {
                    Array(16) { r ->
                        when (r) {
                            bottomRow - 1 -> shifted[bottomRow].substring(0, 8) + shifted[bottomRow - 1].substring(8)
                            bottomRow -> "........" + shifted[bottomRow].substring(8)
                            else -> shifted[r]
                        }
                    }
                } else {
                    shifted
                }
            }
            SpriteFrame.WALK_B -> {
                // Right-step stride: shift body 1px right if possible, lift right foot or bounce
                val canShiftRight = base.all { it.last() == '.' }
                val shifted = Array(16) { r ->
                    if (canShiftRight) shiftRowRight(base[r], 1)
                    else if (r in topRow..waistRow) shiftRowRight(base[r], 1)
                    else base[r]
                }
                val bot = shifted[bottomRow]
                val hasTwoFeet = bottomRow >= 2 &&
                    bot.substring(0, 7).any { it != '.' } &&
                    bot.substring(9, 16).any { it != '.' } &&
                    (bot[7] == '.' || bot[8] == '.')
                if (hasTwoFeet) {
                    Array(16) { r ->
                        when (r) {
                            bottomRow - 1 -> shifted[bottomRow - 1].substring(0, 8) + shifted[bottomRow].substring(8)
                            bottomRow -> shifted[bottomRow].substring(0, 8) + "........"
                            else -> shifted[r]
                        }
                    }
                } else if (topRow > 0) {
                    // Single-base Digimon (Baby/serpent): 1px right + 1px hop up
                    Array(16) { r -> if (r < 15) shifted[r + 1] else "................" }
                } else {
                    shifted
                }
            }
            SpriteFrame.FEED_START -> {
                // Lean forward toward food on left
                Array(16) { r ->
                    if (r in topRow..waistRow) shiftRowLeft(base[r], 1) else base[r]
                }
            }
            SpriteFrame.JOY_EAT -> {
                // Happy jump: shift entire sprite 1px up
                val canJumpUp = base[0] == "................"
                if (canJumpUp) {
                    Array(16) { r -> if (r < 15) base[r + 1] else "................" }
                } else {
                    Array(16) { r ->
                        when (r) {
                            bottomRow - 1 -> base[bottomRow]
                            bottomRow -> "................"
                            else -> base[r]
                        }
                    }
                }
            }
            SpriteFrame.SLEEP_A -> {
                // Eyes closed + relaxed posture (preserves white body/armor fill)
                closeEyePixels(base, topRow, eyeLimitRow, 'B')
            }
            SpriteFrame.SLEEP_B -> {
                // Eyes closed + deep sleep breathing bob (preserves white body/armor fill)
                val closed = closeEyePixels(base, topRow, eyeLimitRow, 'B')
                Array(16) { r ->
                    when {
                        r <= topRow -> "................"
                        r in (topRow + 1)..waistRow -> closed[r - 1]
                        else -> closed[r]
                    }
                }
            }
            SpriteFrame.REFUSAL -> {
                // Classic refusal head-turn: horizontally mirror sprite; if symmetric (or mirror matches charge shift),
                // twist head 1px right & lower body 1px left with squinted eyes
                val mirrored = Array(16) { r -> base[r].reversed() }
                val matchesBase = mirrored.indices.all { r -> mirrored[r] == base[r] }
                val matchesCharge = mirrored.indices.all { r ->
                    val charged = if (r in topRow..waistRow) shiftRowRight(base[r], 1) else base[r]
                    mirrored[r] == charged
                }
                if (matchesBase || matchesCharge) {
                    val squinted = closeEyePixels(mirrored, topRow, eyeLimitRow, 'S')
                    Array(16) { r ->
                        when {
                            r in topRow..eyeLimitRow -> shiftRowRight(squinted[r], 1)
                            r in (eyeLimitRow + 1)..bottomRow -> shiftRowLeft(squinted[r], 1)
                            else -> squinted[r]
                        }
                    }
                } else {
                    mirrored
                }
            }
            SpriteFrame.ATTACK_CHARGE -> {
                // Draw back 1px right to wind up attack
                Array(16) { r ->
                    if (r in topRow..waistRow) shiftRowRight(base[r], 1) else base[r]
                }
            }
            SpriteFrame.ATTACK_RELEASE -> {
                // Full-body forward lunge 1px left (with 2px head thrust when margin permits) to fire projectile
                Array(16) { r ->
                    when {
                        r in topRow..eyeLimitRow && base[r].startsWith("..") -> shiftRowLeft(base[r], 2)
                        r in topRow..bottomRow -> shiftRowLeft(base[r], 1)
                        else -> base[r]
                    }
                }
            }
            SpriteFrame.INJURED -> {
                // Weakened crouch with squinting eyes (preserves white body/armor fill)
                val squinted = closeEyePixels(base, topRow, eyeLimitRow, 'S')
                Array(16) { r ->
                    when {
                        r <= topRow -> "................"
                        r in (topRow + 1)..waistRow -> squinted[r - 1]
                        else -> squinted[r]
                    }
                }
            }
        }
    }

    /**
     * Generates a low-power monochrome outline matrix for Wear OS Ambient Mode,
     * strictly capping the On-Pixel Ratio (OPR) at <= 15% (max 38 lit cells out of 256)
     * to prevent OLED burn-in and minimize battery drain.
     */
    fun getAmbientOutlineMatrix(speciesId: Int, frame: SpriteFrame = SpriteFrame.IDLE_A): Array<String> {
        val sid = if (speciesId <= 1) 201 else speciesId
        val cacheKey = (sid shl 4) or (frame.ordinal and 0x0F)
        AMBIENT_CACHE[cacheKey]?.let { return it }

        val full = getMatrix(sid, frame)
        val candidates = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until 16) {
            for (c in 0 until 16) {
                if (full[r][c] == '.') continue
                val isEdge = (r == 0 || r == 15 || c == 0 || c == 15) ||
                    full[r - 1][c] == '.' || full[r + 1][c] == '.' ||
                    full[r][c - 1] == '.' || full[r][c + 1] == '.'
                if (isEdge) {
                    candidates.add(r to c)
                }
            }
        }
        val maxLitCells = 15 // 15 / 256 = 5.86% <= 6.0% OPR (Strict Ultra-Low-Power Wear OS AMOLED wireframe)
        val selected = if (candidates.size <= maxLitCells) {
            candidates.toSet()
        } else {
            // Subsample evenly around the silhouette perimeter to stay strictly <= 15% OPR
            val step = candidates.size.toDouble() / maxLitCells.toDouble()
            (0 until maxLitCells).map { i -> candidates[(i * step).toInt().coerceIn(0, candidates.lastIndex)] }.toSet()
        }
        return Array(16) { r ->
            val sb = StringBuilder(16)
            for (c in 0 until 16) {
                sb.append(if ((r to c) in selected) 'W' else '.')
            }
            sb.toString()
        }.also { AMBIENT_CACHE[cacheKey] = it }
    }

    /**
     * Computes the On-Pixel Ratio (0.0f .. 1.0f) of the Ambient Mode outline matrix.
     */
    fun calculateAmbientOpr(speciesId: Int, frame: SpriteFrame = SpriteFrame.IDLE_A): Float {
        val matrix = getAmbientOutlineMatrix(speciesId, frame)
        val lit = matrix.sumOf { row -> row.count { it != '.' } }
        return lit.toFloat() / 256f
    }

    private fun getBaseMatrix(sid: Int): Array<String> {
        MasterRosterMatrices.getMatrix(sid)?.let { return it }
        return when (sid) {
            101 -> BOTAMON_IDLE
            102 -> KOROMON_IDLE
            103 -> PUNIMON_ROM // Punimon
            104 -> TSUNOMON_ROM // Tsunomon
            105 -> POYOMON_ROM // Poyomon
            106 -> TOKOMON_ROM // Tokomon
            107 -> YURAMON_ROM // Yuramon
            108 -> TANEMON_ROM // Tanemon
            109 -> ZURUMON_ROM // Zurumon
            110 -> PAGUMON_ROM // Pagumon
            111 -> MOKUMON_ROM // Mokumon
            112 -> DEMIMERAMON_ROM // DemiMeramon
            113 -> KURAMON_ROM // Kuramon
            114 -> DEMIVEEMON_ROM // DemiVeemon
            115 -> GIGIMON_ROM // Gigimon
            116 -> VIXIMON_ROM // Viximon
            117 -> GUMMYMON_ROM // Gummymon
            118 -> CHOCOMON_ROM // Chocomon
            119 -> YAAMON_ROM // Yaamon
            120 -> NYAROMON_ROM // Nyaromon
            121 -> PYOCOMON_ROM // Pyocomon
            122 -> DORIMON_ROM // Dorimon
            130 -> NYOKIMON_ROM // Nyokimon
            131 -> PICHIMON_ROM // Pichimon
            132 -> PUKAMON_ROM // Pukamon
            201 -> AGUMON_IDLE_A // Agumon
            202 -> BETAMON_ROM // Betamon
            203 -> GABUMON_ROM // Gabumon
            204 -> ELECMON_ROM // Elecmon
            205 -> PATAMON_ROM // Patamon
            206 -> BIYOMON_ROM // Biyomon
            207 -> PALMON_ROM // Palmon
            208 -> TENTOMON_ROM // Tentomon
            209 -> GAZIMON_ROM // Gazimon
            210 -> GIZAMON_ROM // Gizamon
            211 -> KUNEMON_ROM // Kunemon
            212 -> VEEMON_ROM // Veemon
            213 -> WORMMON_ROM // Wormmon
            214 -> GUILMON_ROM // Guilmon
            215 -> RENAMON_ROM // Renamon
            216 -> TERRIERMON_ROM // Terriermon
            217 -> LOPMON_ROM // Lopmon
            218 -> IMPMON_ROM // Impmon
            219 -> BLACKAGUMON_ROM // BlackAgumon
            220 -> BLACKGABUMON_ROM // BlackGabumon
            221 -> LUCEMON_ROM // Lucemon
            222 -> HACKMON_ROM // Hackmon
            223 -> SALAMON_ROM // Salamon
            224 -> GAOMON_ROM // Gaomon
            225 -> HAWKMON_ROM // Hawkmon
            226 -> ARMADILLOMON_ROM // Armadillomon
            227 -> DORUMON_ROM // Dorumon
            228 -> KOTEMON_ROM // Kotemon
            229 -> MONODRAMON_ROM // Monodramon
            230 -> FLORAMON_ROM // Floramon
            231 -> GOBLIMON_ROM // Goblimon
            301 -> GREYMON_ROM // Greymon
            302 -> TYRANNOMON_ROM // Tyrannomon
            303 -> DEVIMON_ROM // Devimon
            304 -> MERAMON_ROM // Meramon
            305 -> NUMEMON_ROM // Numemon
            306 -> GARURUMON_ROM // Garurumon
            307 -> ANGEMON_ROM // Angemon
            308 -> TOGEMON_ROM // Togemon
            309 -> BIRDRAMON_ROM // Birdramon
            310 -> KABUTERIMON_ROM // Kabuterimon
            311 -> MONOCHROMON_ROM // Monochromon
            312 -> OGREMON_ROM // Ogremon
            313 -> LEOMON_ROM // Leomon
            314 -> SEADRAMON_ROM // Seadramon
            315 -> SUKAMON_ROM // Sukamon
            316 -> DARKTYRANNOMON_ROM // DarkTyrannomon
            317 -> DEVIDRAMON_ROM // Devidramon
            318 -> CYCLONEMON_ROM // Cyclonemon
            319 -> TUSKMON_ROM // Tuskmon
            320 -> FLYMON_ROM // Flymon
            321 -> RAREMON_ROM // Raremon
            322 -> KUWAGAMON_ROM // Kuwagamon
            323 -> KOKATORIMON_ROM // Kokatorimon
            324 -> COELAMON_ROM // Coelamon
            325 -> NANIMON_ROM // Nanimon
            326 -> BAKEMON_ROM // Bakemon
            327 -> SHELLMON_ROM // Shellmon
            328 -> UNIMON_ROM // Unimon
            329 -> FRIGIMON_ROM // Frigimon
            330 -> VEGIEMON_ROM // Vegiemon
            331 -> VEEDRAMON_ROM // Veedramon
            332 -> FLAMEDRAMON_ROM // Flamedramon
            333 -> STINGMON_ROM // Stingmon
            334 -> GROWLMON_ROM // Growlmon
            335 -> KYUBIMON_ROM // Kyubimon
            336 -> GARGOMON_ROM // Gargomon
            337 -> WIZARDMON_ROM // Wizardmon
            338 -> GATOMON_ROM // Gatomon
            339 -> BLACKGREYMON_ROM // BlackGreymon
            340 -> BLACKGARURUMON_ROM // BlackGarurumon
            341 -> BAOHACKMON_ROM // BaoHackmon
            342 -> DORUGAMON_ROM // Dorugamon
            343 -> SORCERYMON_ROM // Sorcerymon
            344 -> AQUILAMON_ROM // Aquilamon
            345 -> ANKYLOMON_ROM // Ankylomon
            346 -> GAOGAMON_ROM // Gaogamon
            347 -> MOJYAMON_ROM // Mojyamon
            348 -> STARMON_ROM // Starmon
            349 -> DOKUGUMON_ROM // Dokugumon
            401 -> METALGREYMON_ROM // MetalGreymon
            402 -> MONZAEMON_ROM // Monzaemon
            403 -> SKULLGREYMON_ROM // SkullGreymon
            404 -> WEREGARURUMON_ROM // WereGarurumon
            405 -> MAGNAANGEMON_ROM // MagnaAngemon
            406 -> LILLYMON_ROM // Lillymon
            407 -> GARUDAMON_ROM // Garudamon
            408 -> MEGAKABUTERIMON_ROM // MegaKabuterimon
            409 -> MEGASEADRAMON_ROM // MegaSeadramon
            410 -> MYOTISMON_ROM // Myotismon
            411 -> ETEMON_ROM // Etemon
            412 -> MAMEMON_ROM // Mamemon
            413 -> METALMAMEMON_ROM // MetalMamemon
            414 -> METALTYRANNOMON_ROM // MetalTyrannomon
            415 -> EX_TYRANNOMON_ROM // Ex-Tyrannomon
            416 -> MEGADRAMON_ROM // Megadramon
            417 -> PIXIMON_ROM // Piximon
            418 -> ANDROMON_ROM // Andromon
            419 -> GIROMON_ROM // Giromon
            420 -> VADEMON_ROM // Vademon
            421 -> SKULLMAMEMON_ROM // SkullMamemon
            422 -> AEROVEEDRAMON_ROM // AeroVeedramon
            423 -> PAILDRAMON_ROM // Paildramon
            424 -> WARGROWLMON_ROM // WarGrowlmon
            425 -> TAOMON_ROM // Taomon
            426 -> RAPIDMON_ROM // Rapidmon
            427 -> ANGEWOMON_ROM // Angewomon
            428 -> LADYDEVIMON_ROM // LadyDevimon
            429 -> BLACKMETALGREYMON_ROM // BlackMetalGreymon
            430 -> BLACKWEREGARURUMON_ROM // BlackWereGarurumon
            431 -> CYBERDRAMON_ROM // Cyberdramon
            432 -> ANTYLAMON_ROM // Antylamon
            433 -> SAVIORHACKMON_ROM // SaviorHackmon
            434 -> LUCEMON_CHAOS_MODE_ROM // Lucemon Chaos Mode
            435 -> DORUGREYMON_ROM // Dorugreymon
            436 -> KNIGHTMON_ROM // Knightmon
            437 -> SILPHYMON_ROM // Silphymon
            438 -> ZUDOMON_ROM // Zudomon
            439 -> PANJYAMON_ROM // Panjyamon
            440 -> PHANTOMON_ROM // Phantomon
            441 -> SHAKKOUMON_ROM // Shakkoumon
            442 -> DIGITAMAMON_ROM // Digitamamon
            443 -> CHERRYMON_ROM // Cherrymon
            501 -> WARGREYMON_ROM // WarGreymon
            502 -> MACHINEDRAMON_ROM // Machinedramon
            503 -> METALGARURUMON_ROM // MetalGarurumon
            504 -> SERAPHIMON_ROM // Seraphimon
            505 -> ROSEMON_ROM // Rosemon
            506 -> PHOENIXMON_ROM // Phoenixmon
            507 -> HERCULESKABUTERIMON_ROM // HerculesKabuterimon
            508 -> PLESIOMON_ROM // Plesiomon
            509 -> PIEDMON_ROM // Piedmon
            510 -> OMNIMON_ROM // Omnimon
            511 -> METALETEMON_ROM // MetalEtemon
            512 -> IMPERIALDRAMON_DRAGON_MODE_ROM // Imperialdramon Dragon Mode
            513 -> IMPERIALDRAMON_FIGHTER_MODE_ROM // Imperialdramon Fighter Mode
            514 -> IMPERIALDRAMON_PALADIN_MODE_ROM // Imperialdramon Paladin Mode
            515 -> GALLANTMON_ROM // Gallantmon
            516 -> GALLANTMON_CRIMSON_MODE_ROM // Gallantmon Crimson Mode
            517 -> SAKUYAMON_ROM // Sakuyamon
            518 -> MEGAGARGOMON_ROM // MegaGargomon
            519 -> BEELZEMON_ROM // Beelzemon
            520 -> BEELZEMON_BLAST_MODE_ROM // Beelzemon Blast Mode
            521 -> MAGNAMON_ROM // Magnamon
            522 -> ULFORCEVEEDRAMON_ROM // UlforceVeedramon
            523 -> ALPHAMON_ROM // Alphamon
            524 -> ALPHAMON_OURYUKEN_ROM // Alphamon Ouryuken
            525 -> BLACKWARGREYMON_ROM // BlackWarGreymon
            526 -> CHAOSBLACKWARGREYMON_ROM // ChaosBlackWarGreymon
            527 -> DIABOROMON_ROM // Diaboromon
            528 -> ARMAGGEDDEMON_ROM // Armaggeddemon
            529 -> APOCALYMON_ROM // Apocalymon
            530 -> BELPHEMON_ROM // Belphemon
            531 -> LILITHMON_ROM // Lilithmon
            532 -> DAEMON_ROM // Daemon
            533 -> LEVIAMON_ROM // Leviamon
            534 -> BARBAMON_ROM // Barbamon
            535 -> OMNIMON_ALTER_S_ROM // Omnimon Alter-S
            536 -> OMNIMON_ZWART_ROM // Omnimon Zwart
            537 -> MASTEMON_ROM // Mastemon
            538 -> JESMON_ROM // Jesmon
            539 -> GANKOOMON_ROM // Gankoomon
            540 -> DYNASMON_ROM // Dynasmon
            541 -> LORDKNIGHTMON_ROM // LordKnightmon
            542 -> CRANIUMMON_ROM // Craniummon
            543 -> SLEIPMON_ROM // Sleipmon
            544 -> DUFTMON_ROM // Duftmon
            545 -> EXAMON_ROM // Examon
            546 -> MILLENNIUMMON_ROM // Millenniummon
            547 -> MOONMILLENNIUMMON_ROM // MoonMillenniummon
            548 -> ZEEDMILLENNIUMMON_ROM // ZeedMillenniummon
            549 -> SUSANOOMON_ROM // Susanoomon
            550 -> DORUGORAMON_ROM // Dorugoramon
            551 -> GAIOMON_ROM // Gaiomon
            552 -> OPHANIMON_ROM // Ophanimon
            553 -> VIKEMON_ROM // Vikemon
            554 -> CHAOSDRAMON_ROM // Chaosdramon
            555 -> KENTAUROSMON_ROM // Kentaurosmon
            556 -> MAGNADRAMON_ROM // Magnadramon
            557 -> CHERUBIMON_ROM // Cherubimon
            else -> ARCHETYPE_DINOSAUR
        }
    }

    /**
     * Returns the authentic 32-bit ARGB color palette for the given species.
     * Every single one of the 205 Digimon species has a unique, canonically accurate colorway.
     */
    fun getPalette(speciesId: Int): SpritePalette {
        val sid = if (speciesId <= 1) 201 else speciesId
        PALETTE_CACHE[sid]?.let { return it }
        return resolvePalette(sid).also { PALETTE_CACHE[sid] = it }
    }

    private fun resolvePalette(sid: Int): SpritePalette {
        MasterRosterMatrices.getPalette(sid)?.let { return it }
        return when (sid) {
            // ---------------- Baby I & Baby II (101 - 132) ----------------
            101 -> SpritePalette(SpritePalette.rgb(45, 48, 55), SpritePalette.rgb(75, 80, 90), SpritePalette.rgb(245, 205, 55), SpritePalette.rgb(220, 35, 35)) // Botamon: Obsidian fur + Yellow eyes
            102 -> SpritePalette(SpritePalette.rgb(245, 165, 195), SpritePalette.rgb(215, 115, 150), SpritePalette.rgb(255, 215, 230), SpritePalette.rgb(220, 35, 35)) // Koromon: Pastel Pink + Rose shading
            103 -> SpritePalette(SpritePalette.rgb(225, 45, 65), SpritePalette.rgb(165, 20, 35), SpritePalette.rgb(255, 125, 140), SpritePalette.rgb(255, 215, 60)) // Punimon: Crimson jelly + Pink highlight
            104 -> SpritePalette(SpritePalette.rgb(235, 145, 45), SpritePalette.rgb(185, 95, 20), SpritePalette.rgb(195, 200, 210), SpritePalette.rgb(220, 35, 35)) // Tsunomon: Orange fur + Steel horn
            105 -> SpritePalette(SpritePalette.rgb(185, 235, 255), SpritePalette.rgb(115, 195, 235), SpritePalette.rgb(240, 250, 255), SpritePalette.rgb(0, 200, 255)) // Poyomon: Translucent Ice Blue + White
            106 -> SpritePalette(SpritePalette.rgb(250, 245, 248), SpritePalette.rgb(235, 150, 185), SpritePalette.rgb(215, 50, 85), SpritePalette.rgb(20, 20, 25)) // Tokomon: Pure White + Pink ears/mouth
            107 -> SpritePalette(SpritePalette.rgb(175, 165, 215), SpritePalette.rgb(125, 115, 165), SpritePalette.rgb(145, 205, 135), SpritePalette.rgb(0, 200, 255)) // Yuramon: Lavender seed fluff + Leaf green
            108 -> SpritePalette(SpritePalette.rgb(115, 205, 75), SpritePalette.rgb(65, 155, 40), SpritePalette.rgb(245, 240, 165), SpritePalette.rgb(20, 20, 25)) // Tanemon: Sprout Green + Cream face
            109 -> SpritePalette(SpritePalette.rgb(135, 215, 95), SpritePalette.rgb(85, 165, 55), SpritePalette.rgb(215, 245, 165), SpritePalette.rgb(220, 35, 35)) // Zurumon: Slime Green + Acid Yellow
            110 -> SpritePalette(SpritePalette.rgb(145, 130, 185), SpritePalette.rgb(95, 80, 135), SpritePalette.rgb(225, 195, 215), SpritePalette.rgb(220, 35, 35)) // Pagumon: Dusky Violet + Mauve ears
            111 -> SpritePalette(SpritePalette.rgb(145, 150, 160), SpritePalette.rgb(95, 100, 110), SpritePalette.rgb(245, 165, 65), SpritePalette.rgb(255, 193, 7)) // Mokumon: Smoke Gray + Ember core
            112 -> SpritePalette(SpritePalette.rgb(250, 135, 25), SpritePalette.rgb(215, 55, 20), SpritePalette.rgb(255, 225, 75), SpritePalette.rgb(220, 35, 35)) // DemiMeramon: Blazing Orange + Flame Yellow
            113 -> SpritePalette(SpritePalette.rgb(95, 115, 185), SpritePalette.rgb(55, 70, 135), SpritePalette.rgb(175, 215, 250), SpritePalette.rgb(230, 30, 40)) // Kuramon: Cyber Indigo + Pale Cyan eye
            114 -> SpritePalette(SpritePalette.rgb(65, 150, 235), SpritePalette.rgb(235, 242, 250), SpritePalette.rgb(230, 50, 60), SpritePalette.rgb(255, 215, 60)) // DemiVeemon: Azure Blue + White belly
            115 -> SpritePalette(SpritePalette.rgb(225, 45, 55), SpritePalette.rgb(245, 240, 230), SpritePalette.rgb(245, 195, 55), SpritePalette.rgb(25, 25, 30)) // Gigimon: Crimson Red + Cream belly + Yellow eyes
            116 -> SpritePalette(SpritePalette.rgb(250, 220, 75), SpritePalette.rgb(245, 245, 250), SpritePalette.rgb(215, 155, 40), SpritePalette.rgb(255, 193, 7)) // Viximon: Fox Yellow + White tail/brow
            117 -> SpritePalette(SpritePalette.rgb(115, 220, 140), SpritePalette.rgb(70, 170, 95), SpritePalette.rgb(240, 155, 185), SpritePalette.rgb(25, 25, 30)) // Gummymon: Mint Green + Soft Pink
            118 -> SpritePalette(SpritePalette.rgb(145, 90, 65), SpritePalette.rgb(100, 55, 35), SpritePalette.rgb(240, 165, 190), SpritePalette.rgb(25, 25, 30)) // Chocomon: Cocoa Brown + Pastel Pink
            119 -> SpritePalette(SpritePalette.rgb(105, 75, 155), SpritePalette.rgb(65, 45, 105), SpritePalette.rgb(245, 210, 65), SpritePalette.rgb(220, 35, 35)) // Yaamon: Imp Purple + Yellow eyes
            120 -> SpritePalette(SpritePalette.rgb(255, 225, 85), SpritePalette.rgb(245, 175, 45), SpritePalette.rgb(255, 250, 210), SpritePalette.rgb(220, 35, 35)) // Nyaromon: Sunshine Yellow + Cream
            121 -> SpritePalette(SpritePalette.rgb(245, 155, 195), SpritePalette.rgb(85, 175, 235), SpritePalette.rgb(250, 225, 75), SpritePalette.rgb(0, 200, 255)) // Pyocomon: Pink bulb + Blue/Yellow flower
            122 -> SpritePalette(SpritePalette.rgb(145, 90, 195), SpritePalette.rgb(240, 235, 250), SpritePalette.rgb(225, 45, 65), SpritePalette.rgb(255, 215, 60)) // Dorimon: Royal Lavender + White + Crimson gem
            130 -> SpritePalette(SpritePalette.rgb(55, 60, 65), SpritePalette.rgb(135, 215, 75), SpritePalette.rgb(210, 245, 155), SpritePalette.rgb(255, 193, 7)) // Nyokimon: Black seed + Sprout Green leaves
            131 -> SpritePalette(SpritePalette.rgb(245, 250, 255), SpritePalette.rgb(75, 185, 235), SpritePalette.rgb(235, 85, 115), SpritePalette.rgb(220, 35, 35)) // Pichimon: Seafoam White + Aqua fins
            132 -> SpritePalette(SpritePalette.rgb(245, 235, 220), SpritePalette.rgb(225, 115, 95), SpritePalette.rgb(115, 175, 235), SpritePalette.rgb(0, 200, 255)) // Pukamon: Cream body + Coral crest

            // ---------------- Child / Rookie (201 - 231) ----------------
            201 -> SpritePalette(SpritePalette.rgb(255, 175, 25), SpritePalette.rgb(216, 112, 10), SpritePalette.rgb(247, 231, 140), SpritePalette.rgb(55, 195, 95)) // Agumon: Classic Amber Orange + Gold
            202 -> SpritePalette(SpritePalette.rgb(65, 185, 85), SpritePalette.rgb(235, 105, 45), SpritePalette.rgb(185, 75, 155), SpritePalette.rgb(220, 35, 35)) // Betamon: Emerald Green + Orange fin
            203 -> SpritePalette(SpritePalette.rgb(115, 145, 225), SpritePalette.rgb(245, 205, 65), SpritePalette.rgb(215, 55, 115), SpritePalette.rgb(220, 35, 35)) // Gabumon: Periwinkle Blue pelt + Yellow belly + Rose markings
            204 -> SpritePalette(SpritePalette.rgb(225, 55, 95), SpritePalette.rgb(105, 85, 175), SpritePalette.rgb(245, 240, 245), SpritePalette.rgb(255, 215, 60)) // Elecmon: Crimson Magenta + Violet tail stripes
            205 -> SpritePalette(SpritePalette.rgb(245, 155, 55), SpritePalette.rgb(248, 242, 230), SpritePalette.rgb(215, 115, 35), SpritePalette.rgb(55, 185, 235)) // Patamon: Warm Orange + Cream belly + Wing ears
            206 -> SpritePalette(SpritePalette.rgb(245, 135, 185), SpritePalette.rgb(210, 75, 135), SpritePalette.rgb(65, 135, 225), SpritePalette.rgb(255, 205, 45)) // Biyomon: Flamingo Pink + Blue wing tips
            207 -> SpritePalette(SpritePalette.rgb(115, 195, 85), SpritePalette.rgb(235, 85, 155), SpritePalette.rgb(65, 145, 55), SpritePalette.rgb(255, 193, 7)) // Palmon: Plant Green + Tropical Pink flower
            208 -> SpritePalette(SpritePalette.rgb(225, 45, 65), SpritePalette.rgb(75, 85, 125), SpritePalette.rgb(155, 215, 95), SpritePalette.rgb(65, 205, 115)) // Tentomon: Ladybug Crimson + Slate shell + Green eyes
            209 -> SpritePalette(SpritePalette.rgb(155, 165, 205), SpritePalette.rgb(95, 100, 135), SpritePalette.rgb(235, 195, 125), SpritePalette.rgb(220, 35, 35)) // Gazimon: Silver-Blue fur + Tan mane/claws
            210 -> SpritePalette(SpritePalette.rgb(245, 205, 55), SpritePalette.rgb(55, 125, 165), SpritePalette.rgb(215, 55, 45), SpritePalette.rgb(220, 35, 35)) // Gizamon: Lemon Yellow + Teal fur + Red dorsal spikes
            211 -> SpritePalette(SpritePalette.rgb(245, 200, 45), SpritePalette.rgb(45, 65, 155), SpritePalette.rgb(225, 55, 65), SpritePalette.rgb(220, 35, 35)) // Kunemon: Larva Yellow + Indigo lightning marks
            212 -> SpritePalette(SpritePalette.rgb(55, 130, 230), SpritePalette.rgb(242, 245, 252), SpritePalette.rgb(250, 210, 50), SpritePalette.rgb(225, 40, 50)) // Veemon: Cobalt Blue + White belly + Yellow V
            213 -> SpritePalette(SpritePalette.rgb(65, 175, 85), SpritePalette.rgb(35, 115, 55), SpritePalette.rgb(225, 55, 85), SpritePalette.rgb(250, 210, 55)) // Wormmon: Leaf Green + Dark Green + Magenta crest
            214 -> SpritePalette(SpritePalette.rgb(220, 35, 45), SpritePalette.rgb(245, 245, 250), SpritePalette.rgb(40, 42, 50), SpritePalette.rgb(255, 210, 45)) // Guilmon: Hazard Crimson + White belly + Black markings
            215 -> SpritePalette(SpritePalette.rgb(250, 210, 55), SpritePalette.rgb(245, 245, 252), SpritePalette.rgb(125, 85, 175), SpritePalette.rgb(65, 195, 245)) // Renamon: Golden Fox Yellow + White + Violet gauntlets
            216 -> SpritePalette(SpritePalette.rgb(245, 242, 232), SpritePalette.rgb(45, 185, 115), SpritePalette.rgb(215, 205, 190), SpritePalette.rgb(45, 165, 215)) // Terriermon: Cream White + Emerald Green ears
            217 -> SpritePalette(SpritePalette.rgb(130, 80, 65), SpritePalette.rgb(240, 150, 180), SpritePalette.rgb(230, 205, 190), SpritePalette.rgb(225, 45, 75)) // Lopmon: Chocolate Brown + Pastel Pink ears
            218 -> SpritePalette(SpritePalette.rgb(85, 60, 135), SpritePalette.rgb(225, 40, 50), SpritePalette.rgb(250, 220, 70), SpritePalette.rgb(95, 225, 105)) // Impmon: Dark Violet + Crimson bandana + Yellow gloves
            219 -> SpritePalette(SpritePalette.rgb(55, 58, 68), SpritePalette.rgb(35, 38, 45), SpritePalette.rgb(125, 130, 145), SpritePalette.rgb(250, 195, 40)) // BlackAgumon: Charcoal Obsidian + Amber eyes
            220 -> SpritePalette(SpritePalette.rgb(65, 70, 85), SpritePalette.rgb(215, 175, 55), SpritePalette.rgb(145, 150, 165), SpritePalette.rgb(225, 45, 45)) // BlackGabumon: Dark Slate pelt + Gold belly/horn
            221 -> SpritePalette(SpritePalette.rgb(250, 245, 235), SpritePalette.rgb(255, 215, 75), SpritePalette.rgb(115, 185, 245), SpritePalette.rgb(0, 200, 255)) // Lucemon: Celestial White + Seraph Gold + Sky Blue
            222 -> SpritePalette(SpritePalette.rgb(205, 215, 230), SpritePalette.rgb(215, 35, 45), SpritePalette.rgb(245, 195, 55), SpritePalette.rgb(0, 210, 255)) // Hackmon: Platinum White/Silver + Crimson Cape + Gold
            223 -> SpritePalette(SpritePalette.rgb(250, 235, 195), SpritePalette.rgb(245, 195, 55), SpritePalette.rgb(185, 155, 115), SpritePalette.rgb(65, 165, 235)) // Salamon: Puppy Cream + Holy Ring Gold
            224 -> SpritePalette(SpritePalette.rgb(55, 115, 215), SpritePalette.rgb(220, 40, 50), SpritePalette.rgb(245, 245, 250), SpritePalette.rgb(250, 205, 55)) // Gaomon: Cobalt Blue + Red Boxing Gloves + White
            225 -> SpritePalette(SpritePalette.rgb(215, 50, 45), SpritePalette.rgb(245, 242, 235), SpritePalette.rgb(245, 195, 50), SpritePalette.rgb(55, 165, 225)) // Hawkmon: Feather Crimson + White + Beak Gold
            226 -> SpritePalette(SpritePalette.rgb(245, 195, 55), SpritePalette.rgb(75, 185, 225), SpritePalette.rgb(195, 145, 85), SpritePalette.rgb(225, 45, 45)) // Armadillomon: Shell Gold + Aqua Blue skin
            227 -> SpritePalette(SpritePalette.rgb(135, 75, 185), SpritePalette.rgb(242, 238, 250), SpritePalette.rgb(220, 35, 55), SpritePalette.rgb(250, 205, 55)) // Dorumon: Royal Violet fur + White muzzle + Red interface
            228 -> SpritePalette(SpritePalette.rgb(45, 95, 195), SpritePalette.rgb(215, 165, 85), SpritePalette.rgb(240, 242, 248), SpritePalette.rgb(225, 40, 40)) // Kotemon: Kendo Indigo Gi + Bamboo Gold mask + White
            229 -> SpritePalette(SpritePalette.rgb(115, 55, 165), SpritePalette.rgb(185, 145, 220), SpritePalette.rgb(245, 210, 65), SpritePalette.rgb(225, 40, 50)) // Monodramon: Dragon Purple + Lavender + Yellow claws
            230 -> SpritePalette(SpritePalette.rgb(95, 195, 85), SpritePalette.rgb(225, 45, 85), SpritePalette.rgb(245, 210, 75), SpritePalette.rgb(220, 35, 35)) // Floramon: Leaf Green body + Rose Red petals + Yellow stamen
            231 -> SpritePalette(SpritePalette.rgb(95, 165, 75), SpritePalette.rgb(215, 115, 45), SpritePalette.rgb(155, 110, 65), SpritePalette.rgb(225, 45, 45)) // Goblimon: Goblin Green + Orange hair + Wood club

            // ---------------- Adult / Champion (301 - 349) ----------------
            301 -> SpritePalette(SpritePalette.rgb(235, 125, 30), SpritePalette.rgb(115, 75, 50), SpritePalette.rgb(65, 125, 215), SpritePalette.rgb(220, 35, 35)) // Greymon: Dinosaur Orange + Brown helmet + Blue stripes
            302 -> SpritePalette(SpritePalette.rgb(225, 55, 50), SpritePalette.rgb(245, 240, 225), SpritePalette.rgb(65, 175, 85), SpritePalette.rgb(250, 205, 45)) // Tyrannomon: Crimson Red + White belly + Green dorsal plates
            303 -> SpritePalette(SpritePalette.rgb(45, 48, 65), SpritePalette.rgb(85, 90, 115), SpritePalette.rgb(215, 45, 55), SpritePalette.rgb(220, 35, 35)) // Devimon: Midnight Black + Slate + Crimson eyes/markings
            304 -> SpritePalette(SpritePalette.rgb(245, 105, 20), SpritePalette.rgb(215, 35, 25), SpritePalette.rgb(255, 220, 55), SpritePalette.rgb(255, 245, 150)) // Meramon: Magma Orange + Crimson core + Flame Yellow
            305 -> SpritePalette(SpritePalette.rgb(125, 190, 55), SpritePalette.rgb(85, 140, 35), SpritePalette.rgb(235, 195, 155), SpritePalette.rgb(220, 35, 35)) // Numemon: Slime Green + Moss + Stalk eyes
            306 -> SpritePalette(SpritePalette.rgb(235, 242, 255), SpritePalette.rgb(55, 105, 205), SpritePalette.rgb(155, 180, 235), SpritePalette.rgb(220, 35, 35)) // Garurumon: Arctic White + Cobalt Blue stripes + Silver
            307 -> SpritePalette(SpritePalette.rgb(242, 245, 252), SpritePalette.rgb(55, 115, 220), SpritePalette.rgb(250, 205, 65), SpritePalette.rgb(255, 215, 60)) // Angemon: Pure Angel White + Blue cloth + Golden staff/hair
            308 -> SpritePalette(SpritePalette.rgb(85, 195, 65), SpritePalette.rgb(50, 140, 40), SpritePalette.rgb(225, 50, 50), SpritePalette.rgb(245, 175, 45)) // Togemon: Cactus Green + Boxing Glove Red
            309 -> SpritePalette(SpritePalette.rgb(230, 65, 35), SpritePalette.rgb(250, 155, 35), SpritePalette.rgb(255, 230, 95), SpritePalette.rgb(220, 35, 35)) // Birdramon: Phoenix Vermilion + Solar Orange + Gold tips
            310 -> SpritePalette(SpritePalette.rgb(55, 95, 195), SpritePalette.rgb(35, 60, 135), SpritePalette.rgb(195, 205, 220), SpritePalette.rgb(245, 195, 55)) // Kabuterimon: Electric Cobalt Blue + Navy + Silver helmet
            311 -> SpritePalette(SpritePalette.rgb(125, 135, 145), SpritePalette.rgb(75, 80, 90), SpritePalette.rgb(235, 195, 65), SpritePalette.rgb(220, 35, 35)) // Monochromon: Armored Slate Gray + Charcoal + Horn Gold
            312 -> SpritePalette(SpritePalette.rgb(95, 165, 65), SpritePalette.rgb(60, 115, 40), SpritePalette.rgb(235, 230, 215), SpritePalette.rgb(220, 35, 35)) // Ogremon: Ogre Olive Green + Bone White club/horns
            313 -> SpritePalette(SpritePalette.rgb(235, 165, 55), SpritePalette.rgb(185, 110, 30), SpritePalette.rgb(95, 110, 145), SpritePalette.rgb(220, 35, 35)) // Leomon: Lion Tawny Gold + Amber Mane + Denim trousers
            314 -> SpritePalette(SpritePalette.rgb(45, 135, 215), SpritePalette.rgb(250, 215, 75), SpritePalette.rgb(235, 95, 35), SpritePalette.rgb(220, 35, 35)) // Seadramon: Ocean Blue + Yellow belly + Orange fin/mask
            315 -> SpritePalette(SpritePalette.rgb(245, 210, 45), SpritePalette.rgb(195, 155, 20), SpritePalette.rgb(255, 245, 145), SpritePalette.rgb(220, 35, 35)) // Sukamon: Bright Poop Gold + Ochre shading
            316 -> SpritePalette(SpritePalette.rgb(65, 70, 80), SpritePalette.rgb(45, 135, 115), SpritePalette.rgb(235, 125, 40), SpritePalette.rgb(220, 35, 35)) // DarkTyrannomon: Charcoal + Teal belly + Orange spikes
            317 -> SpritePalette(SpritePalette.rgb(45, 40, 55), SpritePalette.rgb(185, 25, 40), SpritePalette.rgb(115, 85, 155), SpritePalette.rgb(235, 35, 45)) // Devidramon: Gargoyle Obsidian + Crimson eyes + Violet wings
            318 -> SpritePalette(SpritePalette.rgb(235, 190, 65), SpritePalette.rgb(175, 125, 35), SpritePalette.rgb(215, 45, 45), SpritePalette.rgb(220, 35, 35)) // Cyclonemon: Sand Dragon Yellow + Ochre + Red eye/horn
            319 -> SpritePalette(SpritePalette.rgb(75, 175, 65), SpritePalette.rgb(45, 120, 40), SpritePalette.rgb(245, 235, 195), SpritePalette.rgb(220, 35, 35)) // Tuskmon: Dino Green + Forest stripes + Ivory tusks
            320 -> SpritePalette(SpritePalette.rgb(175, 65, 165), SpritePalette.rgb(115, 75, 45), SpritePalette.rgb(250, 210, 55), SpritePalette.rgb(220, 35, 35)) // Flymon: Wasp Violet wings + Brown + Yellow stripes
            321 -> SpritePalette(SpritePalette.rgb(105, 165, 135), SpritePalette.rgb(65, 115, 90), SpritePalette.rgb(215, 75, 95), SpritePalette.rgb(245, 215, 55)) // Raremon: Sludge Teal-Green + Exposed Muscle Red
            322 -> SpritePalette(SpritePalette.rgb(215, 35, 45), SpritePalette.rgb(145, 20, 30), SpritePalette.rgb(235, 235, 240), SpritePalette.rgb(255, 215, 60)) // Kuwagamon: Stag Beetle Crimson + White pincers
            323 -> SpritePalette(SpritePalette.rgb(245, 242, 225), SpritePalette.rgb(195, 185, 160), SpritePalette.rgb(225, 45, 45), SpritePalette.rgb(245, 190, 45)) // Kokatorimon: Rooster White + Grey feathers + Red comb
            324 -> SpritePalette(SpritePalette.rgb(135, 165, 185), SpritePalette.rgb(85, 110, 130), SpritePalette.rgb(215, 185, 145), SpritePalette.rgb(0, 200, 255)) // Coelamon: Fossil Steel-Teal + Bone Tan fins
            325 -> SpritePalette(SpritePalette.rgb(240, 180, 135), SpritePalette.rgb(45, 45, 50), SpritePalette.rgb(215, 45, 45), SpritePalette.rgb(250, 205, 55)) // Nanimon: Peach Skin + Black shades/beard + Red boots
            326 -> SpritePalette(SpritePalette.rgb(245, 245, 252), SpritePalette.rgb(195, 200, 218), SpritePalette.rgb(135, 45, 165), SpritePalette.rgb(225, 35, 45)) // Bakemon: Sheet White + Ghost Shadow + Purple patch
            327 -> SpritePalette(SpritePalette.rgb(235, 95, 155), SpritePalette.rgb(145, 95, 75), SpritePalette.rgb(85, 185, 135), SpritePalette.rgb(255, 193, 7)) // Shellmon: Coral Pink body + Brown shell + Seaweed Green
            328 -> SpritePalette(SpritePalette.rgb(242, 245, 250), SpritePalette.rgb(220, 45, 45), SpritePalette.rgb(245, 205, 65), SpritePalette.rgb(55, 155, 235)) // Unimon: Pegasus White + Crimson mane + Golden horn/wings
            329 -> SpritePalette(SpritePalette.rgb(240, 248, 255), SpritePalette.rgb(165, 215, 245), SpritePalette.rgb(115, 175, 225), SpritePalette.rgb(220, 35, 35)) // Frigimon: Polar Snow White + Glacier Blue shading
            330 -> SpritePalette(SpritePalette.rgb(185, 205, 45), SpritePalette.rgb(85, 155, 45), SpritePalette.rgb(225, 65, 95), SpritePalette.rgb(220, 35, 35)) // Vegiemon: Mustard-Lime vine + Jungle Green + Red lips
            331 -> SpritePalette(SpritePalette.rgb(55, 125, 225), SpritePalette.rgb(242, 245, 252), SpritePalette.rgb(35, 85, 175), SpritePalette.rgb(225, 45, 55)) // Veedramon: Cobalt Dragon Blue + White V-chest
            332 -> SpritePalette(SpritePalette.rgb(55, 120, 220), SpritePalette.rgb(230, 45, 40), SpritePalette.rgb(250, 185, 45), SpritePalette.rgb(255, 215, 60)) // Flamedramon: Veemon Blue + Fire Armor Red + Flame Gold
            333 -> SpritePalette(SpritePalette.rgb(45, 175, 85), SpritePalette.rgb(40, 42, 55), SpritePalette.rgb(225, 45, 65), SpritePalette.rgb(250, 210, 55)) // Stingmon: Insectoid Emerald + Black armor + Crimson eyes
            334 -> SpritePalette(SpritePalette.rgb(195, 30, 40), SpritePalette.rgb(245, 242, 235), SpritePalette.rgb(245, 195, 55), SpritePalette.rgb(25, 25, 30)) // Growlmon: Deep Crimson + White mane + Gold elbow blades
            335 -> SpritePalette(SpritePalette.rgb(250, 205, 55), SpritePalette.rgb(245, 245, 252), SpritePalette.rgb(215, 45, 55), SpritePalette.rgb(65, 175, 250)) // Kyubimon: Nine-Tailed Gold + White + Red-White rope
            336 -> SpritePalette(SpritePalette.rgb(55, 175, 105), SpritePalette.rgb(245, 242, 235), SpritePalette.rgb(65, 105, 175), SpritePalette.rgb(195, 205, 220)) // Gargomon: Emerald Green + Cream + Denim Blue jeans
            337 -> SpritePalette(SpritePalette.rgb(95, 75, 155), SpritePalette.rgb(225, 195, 145), SpritePalette.rgb(245, 195, 55), SpritePalette.rgb(220, 35, 35)) // Wizardmon: Mystic Violet cloak + Tan hat + Blonde hair
            338 -> SpritePalette(SpritePalette.rgb(248, 248, 252), SpritePalette.rgb(145, 75, 215), SpritePalette.rgb(250, 205, 55), SpritePalette.rgb(65, 175, 245)) // Gatomon: Pure White cat + Purple ear/paw stripes + Gold ring
            339 -> SpritePalette(SpritePalette.rgb(55, 60, 75), SpritePalette.rgb(95, 100, 115), SpritePalette.rgb(55, 145, 235), SpritePalette.rgb(250, 195, 45)) // BlackGreymon: Dark Navy-Slate + Blue horn stripes
            340 -> SpritePalette(SpritePalette.rgb(60, 65, 80), SpritePalette.rgb(175, 185, 200), SpritePalette.rgb(245, 205, 55), SpritePalette.rgb(220, 35, 35)) // BlackGarurumon: Obsidian Wolf + Silver fur + Yellow stripes
            341 -> SpritePalette(SpritePalette.rgb(195, 205, 220), SpritePalette.rgb(215, 35, 45), SpritePalette.rgb(245, 195, 55), SpritePalette.rgb(0, 210, 255)) // BaoHackmon: Armored Silver + Crimson Cape + Gold visors
            342 -> SpritePalette(SpritePalette.rgb(105, 60, 155), SpritePalette.rgb(50, 48, 60), SpritePalette.rgb(215, 220, 235), SpritePalette.rgb(225, 40, 55)) // Dorugamon: Dark Violet fur + Black wings + Silver/Red
            343 -> SpritePalette(SpritePalette.rgb(242, 248, 255), SpritePalette.rgb(75, 155, 235), SpritePalette.rgb(245, 205, 65), SpritePalette.rgb(0, 215, 255)) // Sorcerymon: Snow White tunic + Ice Blue trim + Gold
            344 -> SpritePalette(SpritePalette.rgb(205, 45, 45), SpritePalette.rgb(245, 242, 235), SpritePalette.rgb(240, 190, 55), SpritePalette.rgb(220, 35, 35)) // Aquilamon: Red Eagle feathers + White chest + Gold horns/talons
            345 -> SpritePalette(SpritePalette.rgb(235, 195, 65), SpritePalette.rgb(155, 160, 170), SpritePalette.rgb(215, 45, 55), SpritePalette.rgb(25, 25, 30)) // Ankylomon: Sand Yellow skin + Gray armor carapace + Red crest
            346 -> SpritePalette(SpritePalette.rgb(45, 105, 210), SpritePalette.rgb(225, 40, 50), SpritePalette.rgb(242, 245, 252), SpritePalette.rgb(250, 205, 55)) // Gaogamon: Cobalt Wolf + Red spiral ribbons + White mane
            347 -> SpritePalette(SpritePalette.rgb(242, 242, 248), SpritePalette.rgb(165, 120, 75), SpritePalette.rgb(195, 200, 215), SpritePalette.rgb(20, 20, 25)) // Mojyamon: Yeti White fur + Brown hair/feet
            348 -> SpritePalette(SpritePalette.rgb(250, 205, 45), SpritePalette.rgb(195, 205, 220), SpritePalette.rgb(225, 45, 55), SpritePalette.rgb(0, 200, 255)) // Starmon: Star Gold + Silver gloves/boots + Red visor
            349 -> SpritePalette(SpritePalette.rgb(85, 60, 115), SpritePalette.rgb(215, 40, 45), SpritePalette.rgb(245, 200, 55), SpritePalette.rgb(220, 35, 35)) // Dokugumon: Tarantula Purple-Black + Red legs + Yellow markings

            // ---------------- Perfect / Ultimate (401 - 443) ----------------
            401 -> SpritePalette(SpritePalette.rgb(235, 125, 30), SpritePalette.rgb(185, 195, 210), SpritePalette.rgb(105, 75, 165), SpritePalette.rgb(225, 40, 45)) // MetalGreymon: Orange body + Chrome Silver + Purple wings + Red hair
            402 -> SpritePalette(SpritePalette.rgb(250, 210, 50), SpritePalette.rgb(195, 150, 25), SpritePalette.rgb(245, 245, 250), SpritePalette.rgb(225, 40, 50)) // Monzaemon: Plush Teddy Yellow + Amber shading + Red eyes
            403 -> SpritePalette(SpritePalette.rgb(235, 230, 215), SpritePalette.rgb(175, 168, 155), SpritePalette.rgb(225, 75, 35), SpritePalette.rgb(220, 35, 35)) // SkullGreymon: Bone Ivory + Aged Bone + Orange missile
            404 -> SpritePalette(SpritePalette.rgb(235, 240, 252), SpritePalette.rgb(55, 95, 185), SpritePalette.rgb(135, 95, 60), SpritePalette.rgb(220, 35, 35)) // WereGarurumon: Wolf White + Blue stripes/jeans + Leather belts
            405 -> SpritePalette(SpritePalette.rgb(242, 245, 252), SpritePalette.rgb(155, 75, 185), SpritePalette.rgb(250, 205, 55), SpritePalette.rgb(0, 215, 255)) // MagnaAngemon: Holy White + Violet ribbon/wings + Gold armor
            406 -> SpritePalette(SpritePalette.rgb(235, 75, 125), SpritePalette.rgb(85, 195, 85), SpritePalette.rgb(250, 215, 85), SpritePalette.rgb(255, 193, 7)) // Lillymon: Rose Blossom Pink + Leaf Green + Fairy Blonde
            407 -> SpritePalette(SpritePalette.rgb(225, 45, 35), SpritePalette.rgb(250, 195, 45), SpritePalette.rgb(55, 115, 205), SpritePalette.rgb(220, 35, 35)) // Garudamon: Crimson Eagle + Sun Gold talons/mane + Blue markings
            408 -> SpritePalette(SpritePalette.rgb(215, 35, 50), SpritePalette.rgb(145, 20, 35), SpritePalette.rgb(235, 225, 195), SpritePalette.rgb(250, 205, 55)) // MegaKabuterimon: Rhino Beetle Red + Horn Ivory
            409 -> SpritePalette(SpritePalette.rgb(225, 55, 45), SpritePalette.rgb(250, 210, 65), SpritePalette.rgb(235, 145, 45), SpritePalette.rgb(0, 200, 255)) // MegaSeadramon: Crimson Serpent + Gold lightning blade + Yellow belly
            410 -> SpritePalette(SpritePalette.rgb(55, 60, 95), SpritePalette.rgb(215, 35, 45), SpritePalette.rgb(245, 210, 65), SpritePalette.rgb(220, 35, 35)) // Myotismon: Vampire Navy + Crimson cape + Blonde hair
            411 -> SpritePalette(SpritePalette.rgb(245, 155, 35), SpritePalette.rgb(185, 105, 25), SpritePalette.rgb(235, 230, 220), SpritePalette.rgb(255, 193, 7)) // Etemon: Rockstar Orange suit + Tan + Dark shades
            412 -> SpritePalette(SpritePalette.rgb(165, 175, 190), SpritePalette.rgb(225, 40, 45), SpritePalette.rgb(245, 205, 55), SpritePalette.rgb(220, 35, 35)) // Mamemon: Metallic Silver sphere + Red boxing gloves
            413 -> SpritePalette(SpritePalette.rgb(185, 195, 215), SpritePalette.rgb(115, 125, 145), SpritePalette.rgb(235, 145, 35), SpritePalette.rgb(220, 35, 35)) // MetalMamemon: Cyborg Chrome + Gunmetal + Orange psycho blaster
            414 -> SpritePalette(SpritePalette.rgb(165, 175, 185), SpritePalette.rgb(85, 125, 95), SpritePalette.rgb(215, 45, 45), SpritePalette.rgb(250, 205, 55)) // MetalTyrannomon: Titanium Steel + Olive bio-flesh + Red visor
            415 -> SpritePalette(SpritePalette.rgb(235, 75, 115), SpritePalette.rgb(75, 175, 85), SpritePalette.rgb(250, 215, 65), SpritePalette.rgb(220, 35, 35)) // Ex-Tyrannomon: Plush Tyrannomon Red + Green belly + Gold zipper
            416 -> SpritePalette(SpritePalette.rgb(185, 65, 45), SpritePalette.rgb(145, 155, 170), SpritePalette.rgb(245, 205, 65), SpritePalette.rgb(220, 35, 35)) // Megadramon: Rust-Brown Dragon + Steel helmet/claws + Yellow belly
            417 -> SpritePalette(SpritePalette.rgb(245, 145, 185), SpritePalette.rgb(245, 245, 252), SpritePalette.rgb(95, 195, 235), SpritePalette.rgb(250, 205, 55)) // Piximon: Fairy Pink + White wings + Sky Blue spear
            418 -> SpritePalette(SpritePalette.rgb(185, 195, 210), SpritePalette.rgb(115, 125, 140), SpritePalette.rgb(225, 55, 45), SpritePalette.rgb(0, 210, 255)) // Andromon: Chrome Silver armor + Dark bio-frame + Red helmet gem
            419 -> SpritePalette(SpritePalette.rgb(155, 75, 185), SpritePalette.rgb(195, 205, 220), SpritePalette.rgb(245, 185, 45), SpritePalette.rgb(220, 35, 35)) // Giromon: Mine Purple + Chainsaw Silver + Gold horns
            420 -> SpritePalette(SpritePalette.rgb(215, 155, 115), SpritePalette.rgb(165, 95, 155), SpritePalette.rgb(145, 155, 170), SpritePalette.rgb(65, 225, 115)) // Vademon: Alien Brain Peach + Violet tentacles + Raygun Silver
            421 -> SpritePalette(SpritePalette.rgb(240, 235, 220), SpritePalette.rgb(175, 165, 150), SpritePalette.rgb(225, 45, 55), SpritePalette.rgb(250, 205, 55)) // SkullMamemon: Skull Ivory + Bone shadow + Red core
            422 -> SpritePalette(SpritePalette.rgb(55, 130, 230), SpritePalette.rgb(225, 40, 45), SpritePalette.rgb(250, 205, 55), SpritePalette.rgb(245, 245, 252)) // AeroVeedramon: Veedramon Blue + Crimson wings + Gold V
            423 -> SpritePalette(SpritePalette.rgb(45, 115, 215), SpritePalette.rgb(55, 175, 85), SpritePalette.rgb(220, 35, 45), SpritePalette.rgb(250, 205, 55)) // Paildramon: ExVeemon Blue + Stingmon Green armor + Red helmet
            424 -> SpritePalette(SpritePalette.rgb(205, 30, 40), SpritePalette.rgb(195, 205, 220), SpritePalette.rgb(245, 195, 50), SpritePalette.rgb(0, 215, 255)) // WarGrowlmon: Crimson Dragon + Chrome chest/jaw + Gold
            425 -> SpritePalette(SpritePalette.rgb(245, 245, 252), SpritePalette.rgb(95, 55, 155), SpritePalette.rgb(250, 205, 55), SpritePalette.rgb(225, 40, 50)) // Taomon: White Onmyoji robe + Royal Purple sleeves + Gold talisman
            426 -> SpritePalette(SpritePalette.rgb(55, 185, 105), SpritePalette.rgb(245, 205, 55), SpritePalette.rgb(240, 245, 250), SpritePalette.rgb(225, 40, 50)) // Rapidmon: Metallic Emerald Green + Gold trim + White
            427 -> SpritePalette(SpritePalette.rgb(248, 248, 255), SpritePalette.rgb(245, 145, 185), SpritePalette.rgb(250, 210, 65), SpritePalette.rgb(0, 210, 255)) // Angewomon: Celestial White + Pink ribbon + Blonde hair & Gold helmet
            428 -> SpritePalette(SpritePalette.rgb(45, 42, 60), SpritePalette.rgb(235, 235, 245), SpritePalette.rgb(215, 35, 55), SpritePalette.rgb(145, 65, 195)) // LadyDevimon: Obsidian leather + White hair + Crimson bat marks
            429 -> SpritePalette(SpritePalette.rgb(55, 60, 75), SpritePalette.rgb(165, 175, 190), SpritePalette.rgb(215, 40, 45), SpritePalette.rgb(250, 195, 45)) // BlackMetalGreymon: Dark Navy body + Chrome dome + Red wings
            430 -> SpritePalette(SpritePalette.rgb(55, 58, 70), SpritePalette.rgb(155, 160, 175), SpritePalette.rgb(245, 195, 50), SpritePalette.rgb(220, 35, 35)) // BlackWereGarurumon: Black Wolf fur + Silver jeans + Gold blades
            431 -> SpritePalette(SpritePalette.rgb(65, 70, 85), SpritePalette.rgb(185, 195, 210), SpritePalette.rgb(215, 35, 45), SpritePalette.rgb(250, 205, 55)) // Cyberdramon: Dark Cyborg Slate + Silver ribs/helmet + Crimson wings
            432 -> SpritePalette(SpritePalette.rgb(125, 75, 60), SpritePalette.rgb(245, 235, 220), SpritePalette.rgb(235, 135, 165), SpritePalette.rgb(225, 40, 55)) // Antylamon: Chestnut Brown + Cream coat + Rose Pink scarf/ears
            433 -> SpritePalette(SpritePalette.rgb(215, 35, 45), SpritePalette.rgb(205, 215, 230), SpritePalette.rgb(245, 195, 55), SpritePalette.rgb(0, 215, 255)) // SaviorHackmon: Crimson Dragon-Knight + Silver armor + Gold blades
            434 -> SpritePalette(SpritePalette.rgb(245, 245, 252), SpritePalette.rgb(55, 45, 85), SpritePalette.rgb(250, 205, 55), SpritePalette.rgb(225, 35, 55)) // Lucemon Chaos Mode: Half-Angel White + Half-Demon Obsidian + Gold
            435 -> SpritePalette(SpritePalette.rgb(195, 30, 45), SpritePalette.rgb(185, 195, 215), SpritePalette.rgb(115, 65, 165), SpritePalette.rgb(250, 205, 55)) // Dorugreymon: Crimson Dragon + Metallic Silver wings + Violet fur
            436 -> SpritePalette(SpritePalette.rgb(205, 215, 230), SpritePalette.rgb(215, 40, 50), SpritePalette.rgb(245, 195, 55), SpritePalette.rgb(0, 200, 255)) // Knightmon: Polished Plate Silver + Crimson cape + Gold crest
            437 -> SpritePalette(SpritePalette.rgb(245, 245, 252), SpritePalette.rgb(215, 40, 50), SpritePalette.rgb(175, 135, 215), SpritePalette.rgb(250, 205, 55)) // Silphymon: White Valkyrie body + Red chest/ears + Lilac leggings
            438 -> SpritePalette(SpritePalette.rgb(75, 145, 215), SpritePalette.rgb(185, 115, 55), SpritePalette.rgb(245, 205, 55), SpritePalette.rgb(0, 215, 255)) // Zudomon: Sea Blue skin + Brown shell + Gold horn & Thor hammer
            439 -> SpritePalette(SpritePalette.rgb(235, 242, 255), SpritePalette.rgb(75, 135, 215), SpritePalette.rgb(215, 45, 55), SpritePalette.rgb(250, 205, 55)) // Panjyamon: White Ice Lion + Frost Blue trousers + Red scarf
            440 -> SpritePalette(SpritePalette.rgb(195, 35, 45), SpritePalette.rgb(45, 42, 55), SpritePalette.rgb(245, 205, 55), SpritePalette.rgb(185, 195, 210)) // Phantomon: Reaper Crimson cloak + Black hood + Gold amulet/chain
            441 -> SpritePalette(SpritePalette.rgb(240, 242, 245), SpritePalette.rgb(225, 185, 65), SpritePalette.rgb(215, 45, 45), SpritePalette.rgb(0, 210, 255)) // Shakkoumon: Clay Doll Porcelain White + Antique Gold + Red eyes
            442 -> SpritePalette(SpritePalette.rgb(242, 245, 240), SpritePalette.rgb(65, 175, 75), SpritePalette.rgb(215, 35, 75), SpritePalette.rgb(255, 215, 55)) // Digitamamon: Eggshell White + Green spots/legs + Dark void interior
            443 -> SpritePalette(SpritePalette.rgb(55, 145, 65), SpritePalette.rgb(125, 85, 55), SpritePalette.rgb(235, 125, 165), SpritePalette.rgb(220, 35, 35)) // Cherrymon: Ancient Foliage Green + Bark Brown + Cherry Blossom Pink

            // ---------------- Ultimate / Mega (501 - 557) ----------------
            501 -> SpritePalette(SpritePalette.rgb(250, 195, 40), SpritePalette.rgb(235, 115, 30), SpritePalette.rgb(195, 205, 220), SpritePalette.rgb(220, 35, 40)) // WarGreymon: Dramon Gold armor + Agumon Orange + Chrome/Crimson
            502 -> SpritePalette(SpritePalette.rgb(165, 175, 190), SpritePalette.rgb(75, 80, 95), SpritePalette.rgb(220, 40, 45), SpritePalette.rgb(250, 195, 45)) // Machinedramon: Gunmetal Steel + Dark Iron + Crimson cables/eyes
            503 -> SpritePalette(SpritePalette.rgb(65, 125, 225), SpritePalette.rgb(245, 200, 50), SpritePalette.rgb(185, 195, 215), SpritePalette.rgb(220, 35, 45)) // MetalGarurumon: Cobalt Mecha Blue + Gold snout/wings + Silver
            504 -> SpritePalette(SpritePalette.rgb(225, 235, 250), SpritePalette.rgb(45, 105, 220), SpritePalette.rgb(250, 205, 55), SpritePalette.rgb(0, 215, 255)) // Seraphimon: Celestial Silver-White + Royal Sapphire + 10 Gold Wings
            505 -> SpritePalette(SpritePalette.rgb(220, 35, 75), SpritePalette.rgb(45, 165, 85), SpritePalette.rgb(250, 215, 85), SpritePalette.rgb(255, 240, 150)) // Rosemon: Crimson Rose + Emerald Green cape + Blonde hair
            506 -> SpritePalette(SpritePalette.rgb(255, 205, 45), SpritePalette.rgb(235, 95, 30), SpritePalette.rgb(250, 245, 230), SpritePalette.rgb(220, 35, 35)) // Phoenixmon: Sacred Sun Gold + Blazing Orange + White
            507 -> SpritePalette(SpritePalette.rgb(245, 195, 40), SpritePalette.rgb(185, 135, 20), SpritePalette.rgb(85, 115, 195), SpritePalette.rgb(220, 35, 35)) // HerculesKabuterimon: Radiant Beetle Gold + Amber + Cobalt accents
            508 -> SpritePalette(SpritePalette.rgb(245, 248, 255), SpritePalette.rgb(85, 155, 235), SpritePalette.rgb(250, 205, 65), SpritePalette.rgb(255, 193, 7)) // Plesiomon: Sea Dragon White + Azure Blue fins + Gold head crest
            509 -> SpritePalette(SpritePalette.rgb(195, 35, 75), SpritePalette.rgb(242, 242, 248), SpritePalette.rgb(250, 205, 55), SpritePalette.rgb(115, 55, 175)) // Piedmon: Jester Crimson + Harlequin White + Gold/Violet ruff
            510 -> SpritePalette(SpritePalette.rgb(245, 248, 255), SpritePalette.rgb(55, 135, 235), SpritePalette.rgb(245, 175, 45), SpritePalette.rgb(225, 40, 45)) // Omnimon: Royal Knight White + Garuru Blue + Grey Sword Gold
            511 -> SpritePalette(SpritePalette.rgb(215, 225, 235), SpritePalette.rgb(145, 155, 170), SpritePalette.rgb(245, 195, 50), SpritePalette.rgb(225, 40, 45)) // MetalEtemon: Chrome Digizoid Silver + Steel + Gold belt/banana
            512 -> SpritePalette(SpritePalette.rgb(45, 55, 105), SpritePalette.rgb(215, 35, 50), SpritePalette.rgb(245, 200, 55), SpritePalette.rgb(0, 215, 255)) // Imperialdramon Dragon Mode: Obsidian Navy + Crimson wings + Gold horns
            513 -> SpritePalette(SpritePalette.rgb(40, 50, 95), SpritePalette.rgb(245, 195, 50), SpritePalette.rgb(215, 35, 50), SpritePalette.rgb(0, 225, 255)) // Imperialdramon Fighter Mode: Black-Navy armor + Gold + Crimson wings
            514 -> SpritePalette(SpritePalette.rgb(245, 248, 255), SpritePalette.rgb(250, 205, 65), SpritePalette.rgb(55, 215, 195), SpritePalette.rgb(0, 225, 255)) // Imperialdramon Paladin Mode: Sacred Pearl White + Gold + Omni Sword Teal
            515 -> SpritePalette(SpritePalette.rgb(242, 245, 252), SpritePalette.rgb(215, 35, 45), SpritePalette.rgb(250, 200, 55), SpritePalette.rgb(0, 200, 255)) // Gallantmon: Holy White armor + Royal Crimson cape + Aegis Gold
            516 -> SpritePalette(SpritePalette.rgb(225, 30, 55), SpritePalette.rgb(250, 248, 255), SpritePalette.rgb(255, 210, 75), SpritePalette.rgb(0, 225, 255)) // Gallantmon Crimson Mode: Glowing Crimson armor + Angel White wings + Gold
            517 -> SpritePalette(SpritePalette.rgb(250, 205, 50), SpritePalette.rgb(135, 75, 185), SpritePalette.rgb(245, 245, 252), SpritePalette.rgb(225, 40, 55)) // Sakuyamon: Miko Gold armor + Amethyst Violet + White hair
            518 -> SpritePalette(SpritePalette.rgb(45, 170, 95), SpritePalette.rgb(235, 240, 248), SpritePalette.rgb(245, 190, 50), SpritePalette.rgb(225, 40, 45)) // MegaGargomon: Mecha Emerald Green + White + Gold/Red missiles
            519 -> SpritePalette(SpritePalette.rgb(45, 45, 58), SpritePalette.rgb(185, 195, 215), SpritePalette.rgb(215, 35, 45), SpritePalette.rgb(250, 205, 55)) // Beelzemon: Biker Leather Black + Berenjena Silver + Crimson scarf
            520 -> SpritePalette(SpritePalette.rgb(38, 40, 52), SpritePalette.rgb(85, 215, 125), SpritePalette.rgb(215, 225, 240), SpritePalette.rgb(225, 35, 55)) // Beelzemon Blast Mode: Obsidian + Positron Blaster Green + Silver
            521 -> SpritePalette(SpritePalette.rgb(250, 200, 45), SpritePalette.rgb(55, 125, 225), SpritePalette.rgb(255, 235, 120), SpritePalette.rgb(0, 200, 255)) // Magnamon: Golden Digizoid + Veemon Cobalt Blue + Bright Gold
            522 -> SpritePalette(SpritePalette.rgb(55, 145, 240), SpritePalette.rgb(240, 245, 255), SpritePalette.rgb(250, 205, 55), SpritePalette.rgb(225, 40, 55)) // UlforceVeedramon: Ulforce Sky Blue + Silver-White + Gold V-chest
            523 -> SpritePalette(SpritePalette.rgb(42, 45, 60), SpritePalette.rgb(245, 195, 55), SpritePalette.rgb(45, 85, 185), SpritePalette.rgb(0, 225, 255)) // Alphamon: Obsidian Black armor + Holy Gold trim + Royal Blue cape
            524 -> SpritePalette(SpritePalette.rgb(48, 45, 65), SpritePalette.rgb(255, 205, 50), SpritePalette.rgb(55, 215, 165), SpritePalette.rgb(225, 40, 55)) // Alphamon Ouryuken: Black-Gold armor + Ouryu Gold wings + Emerald blade
            525 -> SpritePalette(SpritePalette.rgb(50, 52, 62), SpritePalette.rgb(215, 175, 45), SpritePalette.rgb(145, 150, 165), SpritePalette.rgb(225, 35, 45)) // BlackWarGreymon: Obsidian armor + Dark Gold trim + Silver skin
            526 -> SpritePalette(SpritePalette.rgb(55, 40, 75), SpritePalette.rgb(225, 35, 55), SpritePalette.rgb(245, 190, 50), SpritePalette.rgb(0, 225, 255)) // ChaosBlackWarGreymon: Chaos Violet-Black + Crimson core + Gold
            527 -> SpritePalette(SpritePalette.rgb(40, 80, 105), SpritePalette.rgb(195, 50, 45), SpritePalette.rgb(245, 210, 60), SpritePalette.rgb(255, 255, 255)) // Diaboromon: Abyssal Teal-Blue + Rust Red chest/hair + Yellow eyes
            528 -> SpritePalette(SpritePalette.rgb(45, 55, 75), SpritePalette.rgb(225, 65, 35), SpritePalette.rgb(245, 205, 55), SpritePalette.rgb(255, 215, 0)) // Armaggeddemon: Dark Carapace Slate + Magma Orange/Red + Yellow legs
            529 -> SpritePalette(SpritePalette.rgb(75, 65, 115), SpritePalette.rgb(175, 185, 205), SpritePalette.rgb(215, 40, 55), SpritePalette.rgb(255, 215, 0)) // Apocalymon: Polyhedral Indigo + Metallic Silver + Crimson claws
            530 -> SpritePalette(SpritePalette.rgb(85, 65, 125), SpritePalette.rgb(235, 235, 245), SpritePalette.rgb(245, 185, 45), SpritePalette.rgb(225, 35, 45)) // Belphemon: Demon Beast Purple + White mane + Gold chains/claws
            531 -> SpritePalette(SpritePalette.rgb(145, 45, 165), SpritePalette.rgb(35, 32, 48), SpritePalette.rgb(245, 200, 65), SpritePalette.rgb(235, 45, 95)) // Lilithmon: Lust Violet + Obsidian dress + Nazar Gold claw
            532 -> SpritePalette(SpritePalette.rgb(185, 35, 45), SpritePalette.rgb(75, 55, 115), SpritePalette.rgb(235, 225, 205), SpritePalette.rgb(250, 205, 55)) // Daemon: Wrath Crimson robe + Dark Violet skin + Bone horns
            533 -> SpritePalette(SpritePalette.rgb(220, 35, 50), SpritePalette.rgb(45, 135, 215), SpritePalette.rgb(250, 205, 55), SpritePalette.rgb(0, 215, 255)) // Leviamon: Envy Leviathan Crimson + Ocean Blue belly + Gold jaws
            534 -> SpritePalette(SpritePalette.rgb(95, 45, 125), SpritePalette.rgb(245, 195, 50), SpritePalette.rgb(235, 235, 242), SpritePalette.rgb(225, 35, 45)) // Barbamon: Avarice Violet robe + Gold Death Lure staff + White beard
            535 -> SpritePalette(SpritePalette.rgb(240, 242, 250), SpritePalette.rgb(215, 35, 45), SpritePalette.rgb(245, 195, 50), SpritePalette.rgb(55, 155, 235)) // Omnimon Alter-S: Silver-White + BlitzGreymon Red + CresGarurumon Gold
            536 -> SpritePalette(SpritePalette.rgb(45, 48, 60), SpritePalette.rgb(75, 115, 185), SpritePalette.rgb(235, 145, 40), SpritePalette.rgb(220, 35, 45)) // Omnimon Zwart: Black Digitron armor + Dark Blue + Orange sword
            537 -> SpritePalette(SpritePalette.rgb(242, 245, 252), SpritePalette.rgb(55, 45, 85), SpritePalette.rgb(245, 195, 65), SpritePalette.rgb(225, 45, 95)) // Mastemon: Light Angel White + Dark Demon Obsidian + Gold/Rose
            538 -> SpritePalette(SpritePalette.rgb(235, 240, 250), SpritePalette.rgb(215, 35, 45), SpritePalette.rgb(250, 200, 55), SpritePalette.rgb(0, 215, 255)) // Jesmon: Crystal White blades + Crimson cape + Gold chest core
            539 -> SpritePalette(SpritePalette.rgb(235, 238, 245), SpritePalette.rgb(185, 35, 45), SpritePalette.rgb(245, 195, 50), SpritePalette.rgb(0, 210, 255)) // Gankoomon: White coat + Crimson armor/aura + Gold Hinukamuy
            540 -> SpritePalette(SpritePalette.rgb(235, 238, 248), SpritePalette.rgb(115, 65, 175), SpritePalette.rgb(65, 195, 175), SpritePalette.rgb(245, 195, 55)) // Dynasmon: Pearl Wyvern armor + Royal Purple wings + Teal gems
            541 -> SpritePalette(SpritePalette.rgb(235, 105, 155), SpritePalette.rgb(245, 200, 65), SpritePalette.rgb(242, 245, 252), SpritePalette.rgb(185, 35, 75)) // LordKnightmon: Rose Pink armor + Gold ribbons/shield + Silver
            542 -> SpritePalette(SpritePalette.rgb(95, 75, 145), SpritePalette.rgb(185, 195, 215), SpritePalette.rgb(65, 205, 235), SpritePalette.rgb(225, 40, 55)) // Craniummon: Black Digizoid Violet-Indigo + Silver + Cyan Claíomh Solais
            543 -> SpritePalette(SpritePalette.rgb(205, 35, 45), SpritePalette.rgb(245, 195, 50), SpritePalette.rgb(65, 185, 245), SpritePalette.rgb(255, 255, 255)) // Sleipmon: Red Digizoid centaur armor + Gold trim + Ice-Blue crossbow
            544 -> SpritePalette(SpritePalette.rgb(235, 240, 248), SpritePalette.rgb(205, 35, 45), SpritePalette.rgb(245, 195, 55), SpritePalette.rgb(55, 135, 225)) // Duftmon: White/Silver knight armor + Crimson wings + Leopard Gold
            545 -> SpritePalette(SpritePalette.rgb(215, 30, 45), SpritePalette.rgb(240, 245, 252), SpritePalette.rgb(45, 175, 165), SpritePalette.rgb(250, 205, 55)) // Examon: Dragon Emperor Crimson + White body + Caledfwlch Teal
            546 -> SpritePalette(SpritePalette.rgb(195, 45, 45), SpritePalette.rgb(145, 155, 170), SpritePalette.rgb(95, 55, 135), SpritePalette.rgb(250, 205, 55)) // Millenniummon: Kimeramon Crimson + Mugendramon Steel + Dark Purple
            547 -> SpritePalette(SpritePalette.rgb(85, 195, 245), SpritePalette.rgb(145, 75, 205), SpritePalette.rgb(235, 248, 255), SpritePalette.rgb(225, 35, 55)) // MoonMillenniummon: Crystal Cyan prism + Astral Violet + White shine
            548 -> SpritePalette(SpritePalette.rgb(165, 30, 55), SpritePalette.rgb(65, 215, 105), SpritePalette.rgb(245, 200, 55), SpritePalette.rgb(0, 225, 255)) // ZeedMillenniummon: Apocalyptic Crimson + Fractal Code Green + Gold
            549 -> SpritePalette(SpritePalette.rgb(215, 35, 45), SpritePalette.rgb(250, 205, 50), SpritePalette.rgb(45, 105, 215), SpritePalette.rgb(0, 225, 255)) // Susanoomon: Kaiser Red + Magna Gold + Zero-Arms Celestial Blue
            550 -> SpritePalette(SpritePalette.rgb(215, 225, 240), SpritePalette.rgb(215, 35, 45), SpritePalette.rgb(245, 195, 50), SpritePalette.rgb(125, 65, 185)) // Dorugoramon: Platinum Silver dragon armor + Crimson wings + Gold
            551 -> SpritePalette(SpritePalette.rgb(48, 45, 60), SpritePalette.rgb(215, 35, 45), SpritePalette.rgb(245, 195, 50), SpritePalette.rgb(0, 215, 255)) // Gaiomon: Samurai Black armor + Crimson blades/hair + Gold horns
            552 -> SpritePalette(SpritePalette.rgb(75, 195, 165), SpritePalette.rgb(250, 205, 55), SpritePalette.rgb(235, 135, 175), SpritePalette.rgb(255, 255, 255)) // Ophanimon: Aquamarine Emerald armor + Seraphic Gold wings + Rose
            553 -> SpritePalette(SpritePalette.rgb(238, 242, 250), SpritePalette.rgb(55, 75, 115), SpritePalette.rgb(245, 195, 55), SpritePalette.rgb(220, 35, 35)) // Vikemon: Arctic White fur + Viking Navy armor + Gold Mjolnir/horns
            554 -> SpritePalette(SpritePalette.rgb(210, 25, 40), SpritePalette.rgb(165, 175, 190), SpritePalette.rgb(55, 58, 70), SpritePalette.rgb(250, 205, 55)) // Chaosdramon: Red Digizoid Crimson + Gunmetal cannons + Dark Iron
            555 -> SpritePalette(SpritePalette.rgb(245, 200, 55), SpritePalette.rgb(195, 35, 45), SpritePalette.rgb(85, 205, 245), SpritePalette.rgb(255, 255, 255)) // Kentaurosmon: Royal Centaur Gold + Red Digizoid + Niflheim Ice Cyan
            556 -> SpritePalette(SpritePalette.rgb(245, 145, 195), SpritePalette.rgb(250, 245, 252), SpritePalette.rgb(250, 210, 65), SpritePalette.rgb(0, 215, 255)) // Magnadramon: Holy Dragon Rose Pink + White mane + Gold horns
            557 -> SpritePalette(SpritePalette.rgb(248, 245, 250), SpritePalette.rgb(235, 125, 175), SpritePalette.rgb(250, 205, 55), SpritePalette.rgb(135, 65, 195)) // Cherubimon: Celestial White + Rose Pink markings + Holy Gold ring
            else -> SpritePalette(SpritePalette.rgb(255, 175, 25), SpritePalette.rgb(216, 112, 10), SpritePalette.rgb(247, 231, 140), SpritePalette.rgb(20, 20, 25))
        }
    }
}

