package com.chuichui.video

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chuichui.video.ui.Categories
import com.chuichui.video.ui.DetailScreen
import com.chuichui.video.ui.PlayScreen
import com.chuichui.video.ui.VodListScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val nav = rememberNavController()
                NavHost(navController = nav, startDestination = "categories") {

                    composable("categories") {
                        Categories(onOpenCategory = { typeId, title ->
                            nav.navigate("vodList?typeId=${Uri.encode(typeId)}&title=${Uri.encode(title)}")
                        })
                    }

                    composable(
                        route = "vodList?typeId={typeId}&title={title}",
                        arguments = listOf(
                            navArgument("typeId") { type = NavType.StringType },
                            navArgument("title") { type = NavType.StringType },
                        )
                    ) { back ->
                        VodListScreen(
                            typeId = back.arguments?.getString("typeId").orEmpty(),
                            title = back.arguments?.getString("title").orEmpty(),
                            onOpenVod = { vodId, name ->
                                nav.navigate("detail?vodId=${Uri.encode(vodId)}&title=${Uri.encode(name)}")
                            }
                        )
                    }

                    composable(
                        route = "detail?vodId={vodId}&title={title}",
                        arguments = listOf(
                            navArgument("vodId") { type = NavType.StringType },
                            navArgument("title") { type = NavType.StringType },
                        )
                    ) { back ->
                        DetailScreen(
                            vodId = back.arguments?.getString("vodId").orEmpty(),
                            title = back.arguments?.getString("title").orEmpty(),
                            onPlay = { url, _ ->
                                nav.navigate("play?url=${Uri.encode(url)}")
                            }
                        )
                    }

                    composable(
                        route = "play?url={url}",
                        arguments = listOf(
                            navArgument("url") { type = NavType.StringType },
                        )
                    ) { back ->
                        PlayScreen(url = back.arguments?.getString("url").orEmpty())
                    }
                }
            }
        }
    }
}
