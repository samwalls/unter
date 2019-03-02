package com.unter

import android.annotation.SuppressLint
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log.d
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import com.unter.model.JourneyInfo
import com.unter.model.UnterAppModel
import com.unter.model.UserInfo


@SuppressLint("LogNotTimber")
class Account : Fragment() {

    private val TAG: String = Account::class.java.canonicalName!!

    private lateinit var model: UnterAppModel

    private lateinit var driverListView: ListView

    private lateinit var buttonLogout: Button

    private var user: UserInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProviders.of(activity!!).get(UnterAppModel::class.java)
        if (!model.isInitialised())
            model.initStorage(context!!.applicationContext)
        model.load()

        if (model.lastUserLogin != null)
            user = model.getUser(model.lastUserLogin!!)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        driverListView = view.findViewById(R.id.account_history_list)
        buttonLogout = view.findViewById(R.id.button_account_logout)

        driverListView.emptyView = view.findViewById<TextView>(R.id.account_history_empty_text)

        driverListView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            onDriverListItemClick(parent, view, position, id)
        }

        if (user != null) {
            setupAccountView()
            buttonLogout.setOnClickListener {
                d(TAG, "logging out user with ID '${user!!.id}'")
                model.logout()
                activity!!.finish()
            }
        }
    }

    private fun setupAccountView() {
        val adapter = JourneyHistoryArrayAdapter(context!!, model.getJourneys(user!!).reversed())
        driverListView.adapter = adapter
    }

    private fun onDriverListItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        val journey = parent.adapter.getItem(position) as JourneyInfo
        d(TAG, "user '${user!!.id}' selected journey with id '${journey.id}'")

        if (!(journey.isCancelled || journey.isComplete)) {

            // emit an info intent to view the journey info
            val requestUri: Uri = Uri.parse("unter://ride/info?journeyId=${journey.id}")

            d(TAG, "emitting journey info intent with URI: '$requestUri'")
            startActivity(Intent(Intent.ACTION_VIEW, requestUri))
        }
    }
}
