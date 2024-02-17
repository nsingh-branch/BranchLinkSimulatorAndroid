package io.branch.branchlinksimulator

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.branch.referral.Branch
import android.util.Log
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.TextField
import androidx.compose.ui.text.input.TextFieldValue
import io.branch.branchlinksimulator.ui.theme.BranchLinkSimulatorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BranchLinkSimulatorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainContent()
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
                if (branchUniversalObject != null) {
                    Log.i("BranchSDK_Tester", "title " + branchUniversalObject.title)
                    Log.i("BranchSDK_Tester", "CanonicalIdentifier " + branchUniversalObject.canonicalIdentifier)
                    Log.i("BranchSDK_Tester", "metadata " + branchUniversalObject.contentMetadata.convertToJson())
                }
                if (linkProperties != null) {
                    Log.i("BranchSDK_Tester", "Channel " + linkProperties.channel)
                    Log.i("BranchSDK_Tester", "control params " + linkProperties.controlParams)
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
fun MainContent() {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }

    Column(modifier = Modifier.padding(16.dp)) {
        SectionHeader(title = "Deep Link Pages")
        ButtonRow(Modifier.fillMaxWidth())

        SectionHeader(title = "Events")
        FunctionButtonRow(Modifier.fillMaxWidth(), LocalContext.current)

        SectionHeader(title = "Settings")
        Button(            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                onClick = { showDialog = true }) {
            Text("Open Alert")
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Enter API URL") },
                text = {
                    TextField(
                        value = textFieldValue,
                        onValueChange = { textFieldValue = it },
                        label = { Text("https://api3.branch.io") }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        Toast.makeText(context, "Detail: ${textFieldValue.text}", Toast.LENGTH_SHORT).show()
                        showDialog = false
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
    Text(text = title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
fun ButtonRow(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Button(
            onClick = { /* Navigate to detail page A */ },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            Text("Go to Tree")
        }
        Button(
            onClick = { /* Navigate to detail page B */ },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            Text("Go to Twig")
        }
        Button(
            onClick = { /* Navigate to detail page C */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Go to Leaf")
        }
    }
}

@Composable
fun FunctionButtonRow(modifier: Modifier = Modifier, context: android.content.Context) {
    Column(modifier = modifier) {
        Button(
            onClick = { /* Execute function one */ },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            Text("Send Standard Event")
        }
        Button(
            onClick = { /* Execute function two */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Send Custom Event")
        }
    }
}

fun executeFunctionOne(context: android.content.Context) {
    // Execute your specific function here
    Toast.makeText(context, "Executed Function One", Toast.LENGTH_SHORT).show()
}

fun executeFunctionTwo(context: android.content.Context) {
    // Execute your specific function here
    Toast.makeText(context, "Executed Function Two", Toast.LENGTH_SHORT).show()
}
