package com.unter

import android.annotation.SuppressLint
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.support.v4.app.FragmentActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.unter.model.JourneyInfo
import com.unter.model.DriverInfo
import com.unter.model.UnterAppModel

class JourneyHistoryArrayAdapter(context: Context, private val journeys: List<JourneyInfo>) : ArrayAdapter<JourneyInfo>(context, -1, journeys) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val rowView: View = inflater.inflate(R.layout.journey_history_item, parent, false)

        val model = ViewModelProviders.of(parent.context as FragmentActivity).get(UnterAppModel::class.java)

        // only initialise the model if not already initialised (this activity may be spawned from anywhere)
        if (!model.isInitialised())
            model.initStorage(context.applicationContext)
        model.load()

        val journey: JourneyInfo = journeys[position]
        val driver: DriverInfo = model.getDriver(journey.driverId)

        rowView.findViewById<TextView>(R.id.driver_list_item_name).text = driver.name
        rowView.findViewById<TextView>(R.id.driver_list_item_vehicle).text = driver.vehicle
        rowView.findViewById<RatingBar>(R.id.driver_list_item_rating).rating = journey.rating
        rowView.findViewById<TextView>(R.id.driver_list_item_rate).text = "£${driver.price}/min"

        val statusText = rowView.findViewById<TextView>(R.id.history_item_status)
        val callButton = rowView.findViewById<ImageButton>(R.id.journey_history_call_button)

        setupStatusText(journey, statusText)
        setupCallButton(journey, driver, callButton)

        return rowView
    }

    @SuppressLint("RestrictedApi")
    private fun setupCallButton(journey: JourneyInfo, driver: DriverInfo, button: ImageButton) {

        if (journey.isInProgress || !(journey.isComplete || journey.isCancelled)) {
            button.visibility = View.VISIBLE
            button.setOnClickListener {
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse("tel:${driver.telephoneNo}")
                context.startActivity(intent)
            }
        } else {
            button.visibility = View.INVISIBLE
        }
    }

    private fun setupStatusText(journey: JourneyInfo, textView: TextView) {
        textView.text = when {
            journey.isCancelled -> "Cancelled"
            journey.isComplete -> "Finished"
            else -> "In Progress"
        }

        textView.setTextColor(when {
            journey.isCancelled -> Color.rgb(0xCC, 0, 0)
            journey.isComplete -> Color.rgb(0, 0xCC, 0)
            else -> Color.rgb(0, 0x99, 0xCC)
        })
    }
}