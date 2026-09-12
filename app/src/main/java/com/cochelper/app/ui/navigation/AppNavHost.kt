package com.cochelper.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.cochelper.app.ui.screens.characters.CharactersScreen
import com.cochelper.app.ui.screens.chase.ChaseScreen
import com.cochelper.app.ui.screens.clue.ClueBoardScreen
import com.cochelper.app.ui.screens.combat.CombatScreen
import com.cochelper.app.ui.screens.dice.DiceScreen
import com.cochelper.app.ui.screens.files.CompanionFilesScreen
import com.cochelper.app.ui.screens.home.HomeScreen
import com.cochelper.app.ui.screens.intro.IntroEditorScreen
import com.cochelper.app.ui.screens.location.LocationDetailScreen
import com.cochelper.app.ui.screens.location.LocationsScreen
import com.cochelper.app.ui.screens.login.LoginScreen
import com.cochelper.app.ui.screens.module.ModuleDetailScreen
import com.cochelper.app.ui.screens.npc.NpcDetailScreen
import com.cochelper.app.ui.screens.npc.NpcListScreen
import com.cochelper.app.ui.screens.original.OriginalDocScreen
import com.cochelper.app.ui.screens.pc.PcDetailScreen
import com.cochelper.app.ui.screens.pc.PcListScreen
import com.cochelper.app.ui.screens.timer.TimerScreen
import com.cochelper.app.ui.screens.timeline.TimelineScreen
import com.cochelper.app.ui.screens.tools.ToolsScreen

private const val DURATION = 320

private val slideEnter: EnterTransition = slideInHorizontally(tween(DURATION)) { it } + fadeIn(tween(DURATION))
private val slideExit: ExitTransition = slideOutHorizontally(tween(DURATION)) { -it / 4 } + fadeOut(tween(DURATION))
private val slidePopEnter: EnterTransition = slideInHorizontally(tween(DURATION)) { -it / 4 } + fadeIn(tween(DURATION))
private val slidePopExit: ExitTransition = slideOutHorizontally(tween(DURATION)) { it } + fadeOut(tween(DURATION))
private val fadeEnter: EnterTransition = fadeIn(tween(DURATION))
private val fadeExit: ExitTransition = fadeOut(tween(DURATION))

@Composable
fun AppNavHost(
    navController: NavHostController,
    onThemeModeChange: (com.cochelper.app.ui.theme.ThemeMode) -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = Modifier,
    ) {
        composable(
            route = Routes.HOME,
            enterTransition = { slideEnter },
            exitTransition = { slideExit },
            popEnterTransition = { slidePopEnter },
            popExitTransition = { slidePopExit },
        ) { HomeScreen(navController) }

        composable(
            route = Routes.CHARACTERS,
            enterTransition = { slideEnter },
            exitTransition = { slideExit },
            popEnterTransition = { slidePopEnter },
            popExitTransition = { slidePopExit },
        ) { CharactersScreen(navController) }

        composable(
            route = Routes.DICE,
            enterTransition = { slideEnter },
            exitTransition = { slideExit },
            popEnterTransition = { slidePopEnter },
            popExitTransition = { slidePopExit },
        ) { DiceScreen(navController) }

        composable(
            route = Routes.TOOLS,
            enterTransition = { slideEnter },
            exitTransition = { slideExit },
            popEnterTransition = { slidePopEnter },
            popExitTransition = { slidePopExit },
        ) { ToolsScreen(navController, onThemeModeChange) }

        // 模组详情：卡片用右侧滑入，列表项用淡入。
        composable(
            route = "module/{moduleId}?trans={trans}",
            arguments = listOf(
                navArgument("moduleId") { type = NavType.LongType },
                navArgument("trans") { type = NavType.StringType; defaultValue = "slide" },
            ),
            enterTransition = {
                val fade = initialState.arguments?.getString("trans") == "fade"
                if (fade) fadeEnter else slideEnter
            },
            exitTransition = { if (targetState.arguments?.getString("trans") == "fade") fadeExit else slideExit },
            popEnterTransition = {
                val fade = initialState.arguments?.getString("trans") == "fade"
                if (fade) fadeEnter else slidePopEnter
            },
            popExitTransition = { if (targetState.arguments?.getString("trans") == "fade") fadeExit else slidePopExit },
        ) {
            val id = it.arguments?.getLong("moduleId") ?: 0L
            ModuleDetailScreen(moduleId = id, navController = navController)
        }

        composable(
            route = Routes.ORIGINAL,
            arguments = listOf(navArgument("moduleId") { type = NavType.LongType }),
            enterTransition = { slideEnter }, exitTransition = { slideExit },
            popEnterTransition = { slidePopEnter }, popExitTransition = { slidePopExit },
        ) {
            OriginalDocScreen(it.arguments?.getLong("moduleId") ?: 0L, navController)
        }

        composable(
            route = Routes.FILES,
            arguments = listOf(navArgument("moduleId") { type = NavType.LongType }),
            enterTransition = { slideEnter }, exitTransition = { slideExit },
            popEnterTransition = { slidePopEnter }, popExitTransition = { slidePopExit },
        ) {
            CompanionFilesScreen(it.arguments?.getLong("moduleId") ?: 0L, navController)
        }

        composable(
            route = Routes.INTRO,
            arguments = listOf(navArgument("moduleId") { type = NavType.LongType }),
            enterTransition = { slideEnter }, exitTransition = { slideExit },
            popEnterTransition = { slidePopEnter }, popExitTransition = { slidePopExit },
        ) {
            IntroEditorScreen(it.arguments?.getLong("moduleId") ?: 0L, navController)
        }

        composable(
            route = Routes.TIMELINE,
            arguments = listOf(navArgument("moduleId") { type = NavType.LongType }),
            enterTransition = { slideEnter }, exitTransition = { slideExit },
            popEnterTransition = { slidePopEnter }, popExitTransition = { slidePopExit },
        ) {
            TimelineScreen(it.arguments?.getLong("moduleId") ?: 0L, navController)
        }

        composable(
            route = Routes.LOCATIONS,
            arguments = listOf(navArgument("moduleId") { type = NavType.LongType }),
            enterTransition = { slideEnter }, exitTransition = { slideExit },
            popEnterTransition = { slidePopEnter }, popExitTransition = { slidePopExit },
        ) {
            LocationsScreen(it.arguments?.getLong("moduleId") ?: 0L, navController)
        }

        composable(
            route = Routes.LOCATION,
            arguments = listOf(navArgument("locationId") { type = NavType.LongType }),
            enterTransition = { slideEnter }, exitTransition = { slideExit },
            popEnterTransition = { slidePopEnter }, popExitTransition = { slidePopExit },
        ) {
            LocationDetailScreen(it.arguments?.getLong("locationId") ?: 0L, navController)
        }

        composable(
            route = Routes.NPCS,
            arguments = listOf(navArgument("moduleId") { type = NavType.LongType }),
            enterTransition = { slideEnter }, exitTransition = { slideExit },
            popEnterTransition = { slidePopEnter }, popExitTransition = { slidePopExit },
        ) {
            NpcListScreen(it.arguments?.getLong("moduleId") ?: 0L, navController)
        }

        composable(
            route = Routes.PCS,
            arguments = listOf(navArgument("moduleId") { type = NavType.LongType }),
            enterTransition = { slideEnter }, exitTransition = { slideExit },
            popEnterTransition = { slidePopEnter }, popExitTransition = { slidePopExit },
        ) {
            PcListScreen(it.arguments?.getLong("moduleId") ?: 0L, navController)
        }

        composable(
            route = Routes.PC,
            arguments = listOf(
                navArgument("pcId") { type = NavType.LongType },
                navArgument("avatar") { type = NavType.BoolType; defaultValue = false },
            ),
            enterTransition = { slideEnter }, exitTransition = { slideExit },
            popEnterTransition = { slidePopEnter }, popExitTransition = { slidePopExit },
        ) {
            PcDetailScreen(
                pcId = it.arguments?.getLong("pcId") ?: 0L,
                showAvatar = it.arguments?.getBoolean("avatar") ?: false,
                navController = navController,
            )
        }

        composable(
            route = Routes.NPC,
            arguments = listOf(navArgument("npcId") { type = NavType.LongType }),
            enterTransition = { slideEnter }, exitTransition = { slideExit },
            popEnterTransition = { slidePopEnter }, popExitTransition = { slidePopExit },
        ) {
            NpcDetailScreen(it.arguments?.getLong("npcId") ?: 0L, navController)
        }

        composable(
            route = Routes.CLUES,
            enterTransition = { slideEnter }, exitTransition = { slideExit },
            popEnterTransition = { slidePopEnter }, popExitTransition = { slidePopExit },
        ) { ClueBoardScreen(navController) }

        composable(
            route = Routes.LOGIN,
            enterTransition = { slideEnter }, exitTransition = { slideExit },
            popEnterTransition = { slidePopEnter }, popExitTransition = { slidePopExit },
        ) { LoginScreen(navController) }

        composable(
            route = Routes.COMBAT,
            enterTransition = { slideEnter }, exitTransition = { slideExit },
            popEnterTransition = { slidePopEnter }, popExitTransition = { slidePopExit },
        ) { CombatScreen(navController) }

        composable(
            route = Routes.CHASE,
            enterTransition = { slideEnter }, exitTransition = { slideExit },
            popEnterTransition = { slidePopEnter }, popExitTransition = { slidePopExit },
        ) { ChaseScreen(navController) }

        composable(
            route = Routes.TIMER,
            enterTransition = { slideEnter }, exitTransition = { slideExit },
            popEnterTransition = { slidePopEnter }, popExitTransition = { slidePopExit },
        ) { TimerScreen(navController) }
    }
}
