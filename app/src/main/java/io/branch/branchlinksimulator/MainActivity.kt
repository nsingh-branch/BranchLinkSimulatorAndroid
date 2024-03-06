package io.branch.branchlinksimulator

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.branch.branchlinksimulator.ui.theme.BranchLinkSimulatorTheme
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.Branch
import io.branch.referral.util.BRANCH_STANDARD_EVENT
import io.branch.referral.util.BranchEvent
import io.branch.referral.util.BranchEvent.BranchLogEventCallback
import io.branch.referral.util.LinkProperties
import java.net.URLEncoder
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.sp
import io.branch.referral.PrefHelper
import java.util.UUID

var customerEventAlias = "alias"

class MainActivity : ComponentActivity() {
    private var navController: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BranchLinkSimulatorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    navController = rememberNavController()
                    NavHost(navController = navController!!, startDestination = "main") {
                        composable("main") {
                            MainContent(navController!!)
                        }
                        composable(
                            route = "details/{title}/{params}",
                            arguments = listOf(
                                navArgument("title") { type = NavType.StringType },
                                navArgument("params") {
                                    type = NavType.StringType
                                    defaultValue = ""
                                    nullable = true
                                }
                            )
                        ) { backStackEntry ->
                            val title = backStackEntry.arguments?.getString("title") ?: "Default Title"
                            val params = backStackEntry.arguments?.getString("params") ?: ""
                            DetailScreen(title, parseQueryParams(params))
                        }

                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        Branch.sessionBuilder(this).withCallback { branchUniversalObject, linkProperties, error ->
            if (error != null) {
                Log.e("BranchSDK_Tester", "branch init failed. Caused by -" + error.message)
            } else {
                Log.i("BranchSDK_Tester", "branch init complete!")
                if (branchUniversalObject != null && linkProperties != null) {
                    val title = branchUniversalObject.title ?: "Default Title"


                    val lpQueryString =  convertLinkPropertiesToQueryString(linkProperties)
                    val buoQueryString = convertBUOToQueryString(branchUniversalObject)

                    val combinedQueryString = listOf(buoQueryString, lpQueryString)
                        .filterNot { it.isEmpty() }
                        .joinToString("&")

                    Log.i("BranchSDK_Tester", "Navigating to details/$title/$combinedQueryString")
                    navController?.navigate("details/$title/$combinedQueryString")
                }
            }
        }.withData(this.intent.data).init()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        this.intent = intent
        if (intent != null && intent.hasExtra("branch_force_new_session") && intent.getBooleanExtra("branch_force_new_session",false)) {
            Branch.sessionBuilder(this).withCallback { referringParams, error ->
                if (error != null) {
                    Log.e("BranchSDK_Tester", error.message)
                } else if (referringParams != null) {
                    Log.i("BranchSDK_Tester", referringParams.toString())
                }
            }.reInit()
        }
    }
}

@Composable
fun MainContent(navController: NavController) {
    val context = LocalContext.current
    var showAPIDialog by remember { mutableStateOf(false) }
    var showAliasDialog by remember { mutableStateOf(false) }
    var showSessionIdDialog by remember { mutableStateOf(false) }

    var textFieldValue by remember { mutableStateOf(PrefHelper.getInstance(context).apiBaseUrl) }
    var aliasValue by remember { mutableStateOf("") }

    val sharedPreferences = context.getSharedPreferences("branch_session_prefs", Context.MODE_PRIVATE)
    val blsSessionId = sharedPreferences.getString("bls_session_id", null) ?: UUID.randomUUID().toString().also {
        sharedPreferences.edit().putString("bls_session_id", it).apply()
    }
    var sessionIdValue by remember { mutableStateOf(blsSessionId) }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.branch_badge_all_white),
                contentDescription = "App Logo",
                modifier = Modifier.size(36.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Branch Link Simulator", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        }

        Spacer(modifier = Modifier.height(16.dp))
        SectionHeader(title = "Deep Link Pages")
        ButtonRow(navController, Modifier.fillMaxWidth())

        SectionHeader(title = "Events")
        FunctionButtonRow(Modifier.fillMaxWidth(), LocalContext.current)

        SectionHeader(title = "Settings")

        RoundedButton(title = "Change Branch API URL", icon = R.drawable.api) {
            showAPIDialog = true
        }

        RoundedButton(title = "Set Customer Event Alias", icon = R.drawable.badge) {
            showAliasDialog = true
        }

        RoundedButton(title = "Change App's Session ID", icon = R.drawable.branch_badge_all_white) {
            showSessionIdDialog = true
        }

        if (showAPIDialog) {
            AlertDialog(
                onDismissRequest = { showAPIDialog = false },
                title = { Text("Enter API URL") },
                text = {
                    TextField(
                        value = textFieldValue,
                        onValueChange = { textFieldValue = it },
                        label = { Text("Ex. https://api2.branch.io/") },
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        Branch.setAPIUrl(textFieldValue)
                        Toast.makeText(context, "Set Branch API URL to $textFieldValue", Toast.LENGTH_SHORT).show()
                        showAPIDialog = false
                    }) {
                        Text("Save")
                    }
                }
            )
        }
        if (showAliasDialog) {

            AlertDialog(
                onDismissRequest = { showAliasDialog = false },
                title = { Text("Enter Customer Event Alias") },
                text = {
                    TextField(
                        value = aliasValue,
                        onValueChange = { aliasValue = it },
                        label = { Text("Ex. mainAlias") },
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        customerEventAlias = aliasValue
                        Toast.makeText(context, "Set Customer Event Alias to $aliasValue", Toast.LENGTH_SHORT).show()
                        showAliasDialog = false
                    }) {
                        Text("Save")
                    }
                }
            )
        }

        if (showSessionIdDialog) {

            AlertDialog(
                onDismissRequest = { showSessionIdDialog = false },
                title = { Text("Enter A Session ID") },
                text = {
                    TextField(
                        value = sessionIdValue,
                        onValueChange = { sessionIdValue = it },
                        label = { Text("Ex. testingSession02") },
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        Branch.getInstance().setRequestMetadata("bls_session_id", sessionIdValue)
                        sharedPreferences.edit().putString("bls_session_id", sessionIdValue).apply()

                        Toast.makeText(context, "Set App's Session ID to $sessionIdValue", Toast.LENGTH_SHORT).show()
                        showSessionIdDialog = false
                    }) {
                        Text("Save")
                    }
                }
            )
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(text = title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onBackground)
}

@Composable
fun ButtonRow(navController: NavController, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(8.dp)) {
        RoundedButton(
            title = "Go to Tree",
            icon = R.drawable.tree
        )
        { navController.navigate("details/Tree/ ") }

        RoundedButton(
            title = "Go to Twig",
            icon = R.drawable.twig
        )
        { navController.navigate("details/Twig/ ") }

        RoundedButton(
            title = "Go to Leaf",
            icon = R.drawable.leaf
        )
        { navController.navigate("details/Leaf/ ") }
    }
}


@Composable
fun FunctionButtonRow(modifier: Modifier = Modifier, context: android.content.Context) {
    Column(modifier = modifier) {
        RoundedButton(
            title = "Send Standard Event",
            icon = R.drawable.send
        )
        { sendStandardEvent(context) }

        RoundedButton(
            title = "Send Custom Event",
            icon = R.drawable.send_custom
        )
        { sendCustomEvent(context) }
    }
}

fun sendStandardEvent(context: android.content.Context) {
    BranchEvent(BRANCH_STANDARD_EVENT.SEARCH).setCustomerEventAlias(customerEventAlias).logEvent(context, object : BranchLogEventCallback {
        override fun onSuccess(responseCode: Int) {
            Toast.makeText(context, "Sent Standard Event!", Toast.LENGTH_SHORT).show()
        }

        override fun onFailure(e: Exception) {
            Toast.makeText(context, "Error Sending Standard Event", Toast.LENGTH_SHORT).show()
        }
    })
}
fun sendCustomEvent(context: android.content.Context) {
    BranchEvent("My Custom Event").setCustomerEventAlias(customerEventAlias).logEvent(context, object : BranchLogEventCallback {
        override fun onSuccess(responseCode: Int) {
            Toast.makeText(context, "Sent Custom Event!", Toast.LENGTH_SHORT).show()
        }

        override fun onFailure(e: Exception) {
            Toast.makeText(context, "Error Sending Custom Event", Toast.LENGTH_SHORT).show()
        }
    })
}

@Composable
fun DetailScreen(title: String, deepLinkParams: Map<String, String>) {
    val context = LocalContext.current

    Box(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(16.dp))

            if (deepLinkParams.isEmpty()) {
                Text(
                    "Open Deep Link to View Parameters",
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                Text("Deep Link Parameters", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)

                LazyColumn(modifier = Modifier.weight(1f)) {
                items(deepLinkParams.toList()) { (key, value) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 4.dp
                            )
                        ) {
                            Row(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    key,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    value,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            RoundedButton(title = "Copy Branch Link", icon = R.drawable.content_copy) {
                // Create Link and copy to clipboard
                val buo = BranchUniversalObject().setTitle(title)
                val lp = LinkProperties()
                    .setFeature("Copy Link Button")
                val url = buo.getShortUrl(context, lp)

                val clipboardManager =
                    context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clipData = ClipData.newPlainText("Branch Link", url)
                clipboardManager.setPrimaryClip(clipData)

                Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_LONG).show()
            }
        }
    }
}

@Composable
fun RoundedButton(
    title: String,
    icon: Int,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = title,
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(Modifier.width(8.dp))
            Text(title, fontSize = 18.sp)
        }
    }
}
fun parseQueryParams(query: String): Map<String, String> =
    if (query.isEmpty()) emptyMap() else query.split("&")
        .mapNotNull { it.split("=").takeIf { parts -> parts.size == 2 }?.let { parts -> parts[0] to parts[1] } }
        .toMap()

fun convertLinkPropertiesToQueryString(linkProperties: LinkProperties): String {
    val keyValuePairs = mutableListOf<String>()

    linkProperties.tags?.filterNot { it.isBlank() }?.forEach { tag ->
        keyValuePairs.add("tags=${URLEncoder.encode(tag, "UTF-8")}")
    }
    linkProperties.alias?.takeIf { it.isNotBlank() }?.let { alias ->
        keyValuePairs.add("alias=${URLEncoder.encode(alias, "UTF-8")}")
    }
    linkProperties.channel?.takeIf { it.isNotBlank() }?.let { channel ->
        keyValuePairs.add("channel=${URLEncoder.encode(channel, "UTF-8")}")
    }
    linkProperties.feature?.takeIf { it.isNotBlank() }?.let { feature ->
        keyValuePairs.add("feature=${URLEncoder.encode(feature, "UTF-8")}")
    }
    linkProperties.stage?.takeIf { it.isNotBlank() }?.let { stage ->
        keyValuePairs.add("stage=${URLEncoder.encode(stage, "UTF-8")}")
    }
    linkProperties.campaign?.takeIf { it.isNotBlank() }?.let { campaign ->
        keyValuePairs.add("campaign=${URLEncoder.encode(campaign, "UTF-8")}")
    }
    if (linkProperties.matchDuration > 0) {
        keyValuePairs.add("matchDuration=${linkProperties.matchDuration}")
    }

    linkProperties.controlParams?.takeIf { it.isNotEmpty() }?.let { controlParams ->
        val encodedParams = controlParams.entries.joinToString("&") {
            "${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value.toString(), "UTF-8")}"
        }
        keyValuePairs.add("controlParams=$encodedParams")
    }

    return keyValuePairs.joinToString("&")
}

fun convertBUOToQueryString(buo: BranchUniversalObject): String {
    val keyValuePairs = mutableListOf<String>()

    buo.title?.takeIf { it.isNotBlank() }?.let { title ->
        keyValuePairs.add("title=${URLEncoder.encode(title, "UTF-8")}")
    }
    buo.canonicalIdentifier?.takeIf { it.isNotBlank() }?.let { id ->
        keyValuePairs.add("canonicalIdentifier=${URLEncoder.encode(id, "UTF-8")}")
    }

    buo.canonicalUrl?.takeIf { it.isNotBlank() }?.let { url ->
        keyValuePairs.add("canonicalUrl=${URLEncoder.encode(url, "UTF-8")}")
    }

    buo.description?.takeIf { it.isNotBlank() }?.let { description ->
        keyValuePairs.add("description=${URLEncoder.encode(description, "UTF-8")}")
    }

    buo.imageUrl?.takeIf { it.isNotBlank() }?.let { imageUrl ->
        keyValuePairs.add("imageUrl=${URLEncoder.encode(imageUrl, "UTF-8")}")
    }

    buo.keywords?.filterNot { it.isBlank() }?.forEach { keyword ->
        keyValuePairs.add("keywords=${URLEncoder.encode(keyword, "UTF-8")}")
    }

    buo.expirationTime.takeIf { it > 0 }?.let { expiration ->
        keyValuePairs.add("expiration=${expiration}")
    }

    buo.isLocallyIndexable.let { locallyIndex ->
        keyValuePairs.add("locallyIndex=${locallyIndex}")
    }

    buo.isPublicallyIndexable.let { publiclyIndex ->
        keyValuePairs.add("publiclyIndex=${publiclyIndex}")
    }

    buo.contentMetadata.customMetadata.filterNot { it.key.isBlank() || it.value.toString().isBlank() }.forEach { (key, value) ->
        keyValuePairs.add("${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value.toString(), "UTF-8")}")
    }

    return keyValuePairs.joinToString("&")
}
