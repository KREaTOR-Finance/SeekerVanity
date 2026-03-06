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

    NavHost(navController = nav, startDestination = "generator") {
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
