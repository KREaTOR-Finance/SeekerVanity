package com.kreation.vanity

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val ctx = LocalContext.current

    val generatorVm: GeneratorViewModel = viewModel()

    val start = if (hasAcceptedDisclaimer(ctx)) "generator" else "welcome"

    NavHost(navController = nav, startDestination = start) {
        composable("welcome") {
            WelcomeScreen(
                onContinue = {
                    setAcceptedDisclaimer(ctx, true)
                    nav.navigate("generator") {
                        popUpTo("welcome") { inclusive = true }
                    }
                },
                onExit = {
                    // Close app best-effort
                    (ctx as? android.app.Activity)?.finish()
                }
            )
        }

        composable("generator") {
            GeneratorScreen(
                vm = generatorVm,
                onReveal = { mnemonic ->
                    // Never persist; pass in-memory only.
                    generatorVm.setRevealMnemonic(mnemonic)
                    nav.navigate("reveal")
                },
                onPayments = { /* deprecated */ }
            )
        }
        composable("reveal") {
            RevealScreen(
                vm = generatorVm,
                onDone = {
                    generatorVm.wipeAfterRevealComplete()
                    nav.popBackStack()
                }
            )
        }
    }
}
