package com.unter

import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.app.FragmentTabHost
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.unter.model.UnterAppModel
import java.lang.IndexOutOfBoundsException


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [Home.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [Home.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class Home : Fragment() {

    private lateinit var model: UnterAppModel

    private lateinit var tabHost: FragmentTabHost

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProviders.of(activity!!).get(UnterAppModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        tabHost = FragmentTabHost(activity)
        tabHost.setup(activity, childFragmentManager, R.layout.fragment_home)
        tabHost.addTab(tabHost.newTabSpec("Search").setIndicator("Search"), JourneyRequest::class.java, null)
        tabHost.addTab(tabHost.newTabSpec("Account").setIndicator("Account"), Account::class.java, null)
        //return inflater.inflate(R.layout.fragment_home, container, false)
        return tabHost
    }
}
