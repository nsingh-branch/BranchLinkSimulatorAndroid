package io.branch.branchlinksimulator

import android.app.Application
import android.content.Context
import io.branch.referral.Branch
import java.util.UUID

class BranchLinkSimulatorApplication: Application() {
    override fun onCreate() {
        super.onCreate()

        // Branch logging for debugging
        Branch.enableLogging()
        Branch.setAPIUrl("https://protected-api.branch.io/")

        // Branch object initialization
        Branch.getAutoInstance(this)

        // Retrieve or create the bls_session_id
        val sharedPreferences = getSharedPreferences("branch_session_prefs", Context.MODE_PRIVATE)
        val blsSessionId = sharedPreferences.getString("bls_session_id", null) ?: run {
            val newId = UUID.randomUUID().toString()
            sharedPreferences.edit().putString("bls_session_id", newId).apply()
            newId
        }

        // Set the bls_session_id in Branch request metadata
        Branch.getInstance().setRequestMetadata("bls_session_id", blsSessionId)

    }
}