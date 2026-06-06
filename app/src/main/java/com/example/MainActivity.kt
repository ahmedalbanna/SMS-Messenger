package com.example

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ConversationListScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.SmsViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val smsViewModel: SmsViewModel = viewModel()

                // State to monitor Default SMS eligibility
                var isDefaultSmsApp by remember { mutableStateOf(isDefaultSmsApp(context)) }

                // Default SMS App Choice Launcher
                val defaultSmsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { _ ->
                    isDefaultSmsApp = isDefaultSmsApp(context)
                    if (isDefaultSmsApp) {
                        Toast.makeText(context, "تم تعيين التطبيق كافتراضي للرسائل! 🎉", Toast.LENGTH_SHORT).show()
                    }
                }

                // Multiple Runtime Permissions Launcher
                val permissionsToRequest = mutableListOf(
                    Manifest.permission.READ_SMS,
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_CONTACTS
                ).apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }.toTypedArray()

                var permissionsGranted by remember {
                    mutableStateOf(hasAllPermissions(context, permissionsToRequest))
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { resultMap ->
                    val allGranted = resultMap.values.all { it }
                    permissionsGranted = allGranted
                    if (allGranted) {
                        Toast.makeText(context, "تم قبول صلاحيات الرسائل والمزامنة والمخزن التجاري! ✅", Toast.LENGTH_LONG).show()
                        smsViewModel.syncMessages(context)
                    } else {
                        Toast.makeText(context, "لم يتم قبول كافة الصلاحيات. سيعمل التطبيق بوضع المحاكاة الافتراضية.", Toast.LENGTH_LONG).show()
                        smsViewModel.syncMessages(context) // Run fallback prepopulation
                    }
                }

                // Automatically trigger permissions on launch and sync
                LaunchedEffect(Unit) {
                    if (!permissionsGranted) {
                        permissionLauncher.launch(permissionsToRequest)
                    } else {
                        smsViewModel.syncMessages(context)
                    }

                    // Handle intent if tapped from notification
                    handleNotificationIntent(intent, smsViewModel)
                }

                // Force RTL Layout direction on entire layout for standard Arabization guidelines
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        val selectedAddress by smsViewModel.selectedAddress.collectAsState()

                        // State-based navigation inside beautiful animated blocks
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            AnimatedContent(
                                targetState = selectedAddress,
                                transitionSpec = {
                                    // Slide left/right based on navigation action
                                    if (targetState != null) {
                                        (slideInHorizontally { width -> -width } + fadeIn()) togetherWith
                                                (slideOutHorizontally { width -> width } + fadeOut())
                                    } else {
                                        (slideInHorizontally { width -> width } + fadeIn()) togetherWith
                                                (slideOutHorizontally { width -> -width } + fadeOut())
                                    }
                                },
                                label = "ScreenTransition"
                            ) { targetAddress ->
                                if (targetAddress == null) {
                                    ConversationListScreen(
                                        viewModel = smsViewModel,
                                        isDefaultSms = isDefaultSmsApp,
                                        onRequestDefaultSms = {
                                            requestDefaultSmsApp(this@MainActivity, defaultSmsLauncher)
                                        },
                                        onNavigateToChat = { address, name ->
                                            smsViewModel.selectConversation(address, name)
                                        }
                                    )
                                } else {
                                    // Handle physical back button inside Chat to return gracefully
                                    BackHandler {
                                        smsViewModel.deselectConversation()
                                    }

                                    ChatScreen(
                                        viewModel = smsViewModel,
                                        isDefaultSms = isDefaultSmsApp,
                                        onNavigateBack = {
                                            smsViewModel.deselectConversation()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Refresh intent extras check in case user clicked another notification
        val smsViewModel = androidx.lifecycle.ViewModelProvider(this)[SmsViewModel::class.java]
        handleNotificationIntent(intent, smsViewModel)
    }

    private fun handleNotificationIntent(intent: Intent?, viewModel: SmsViewModel) {
        val chatAddress = intent?.getStringExtra("chat_address")
        val chatName = intent?.getStringExtra("chat_name")
        if (chatAddress != null) {
            viewModel.selectConversation(chatAddress, chatName)
        }
    }

    private fun isDefaultSmsApp(context: Context): Boolean {
        // Modern role-held validation or legacy identification check
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
            roleManager?.isRoleHeld(RoleManager.ROLE_SMS) == true
        } else {
            Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
        }
    }

    private fun requestDefaultSmsApp(activity: Activity, launcher: androidx.activity.result.ActivityResultLauncher<Intent>) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = activity.getSystemService(Context.ROLE_SERVICE) as RoleManager
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                launcher.launch(intent)
            } else {
                val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                    putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, activity.packageName)
                }
                launcher.launch(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(activity, "فشل طلب تعيين التطبيق الافتراضي: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hasAllPermissions(context: Context, permissions: Array<String>): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}
